package com.example.muritin

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.snap
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.text.replace

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")

    suspend fun signup(
        email: String,
        nid: String,
        password: String,
        role: String,
        name: String,
        phone: String,
        age: Int,
        ownerPassword: String? = null
    ): Result<User> {
        return try {
            val currentUser = auth.currentUser
            var ownerId: String? = null
            if (role == "Conductor" && ownerPassword != null && currentUser != null) {
                val email = currentUser.email ?: throw Exception("Owner email is null")
                val credential = EmailAuthProvider.getCredential(email, ownerPassword)
                currentUser.reauthenticate(credential).await()
                ownerId = currentUser.uid
            }
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                name = name,
                phone = phone,
                nid = nid,
                age = age,
                role = role,
                createdAt = System.currentTimeMillis(),
                ownerId = ownerId
            )
            database.getReference("users").child(firebaseUser.uid).setValue(user).await()
            Log.d("AuthRepository", "User data saved to database: $role")
            firebaseUser.sendEmailVerification().await()
            Log.d("AuthRepository", "Verification email sent to $email")
            if (ownerPassword == null) {
                auth.signOut()
            }
            if (role == "Conductor" && currentUser != null) {
                val email = currentUser.email ?: throw Exception("Owner email is null")
                val ownerCredential = EmailAuthProvider.getCredential(email, ownerPassword!!)
                auth.signInWithCredential(ownerCredential).await()
                Log.d("AuthRepository", "Signed back in as owner: $email")
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Signup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Login failed")
            if (!firebaseUser.isEmailVerified) {
                auth.signOut()
                throw Exception("আপনার ইমেইল যাচাই করা হয়নি। দয়া করে আপনার ইমেইল চেক করুন এবং যাচাই লিঙ্কে ক্লিক করুন।")
            }
            try {

                cleanExpiredSchedules()
                cleanExpiredRequests()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Cleanup failed: ${e.message}")
            }
            val snapshot = database.getReference("users").child(firebaseUser.uid).get().await()
            val user = snapshot.getValue(User::class.java) ?: throw Exception("User data not found")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = database.getReference("users").child(uid).get().await()
            val user = snapshot.getValue(User::class.java) ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get user failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRole(uid: String): String? {
        return try {
            val snapshot = database.getReference("users").child(uid).get().await()
            snapshot.getValue(User::class.java)?.role
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get user role failed: ${e.message}", e)
            null
        }
    }

    suspend fun updateUserProfile(uid: String, name: String, phone: String, age: Int, email: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "phone" to phone,
                "age" to age,
                "email" to email
            )
            database.getReference("users").child(uid).updateChildren(updates).await()
            Log.d("AuthRepository", "User profile updated for $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Update user profile failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUserData(uid: String): Result<Unit> {
        return try {
            database.getReference("users").child(uid).removeValue().await()
            Log.d("AuthRepository", "User data deleted for $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Delete user data failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteOwnerData(uid: String): Result<Unit> {
        return try {

            //Get all buses owned by this user
            val busesSnapshot = database.getReference("buses")
                .orderByChild("ownerId")
                .equalTo(uid)
                .get()
                .await()

            busesSnapshot.children.forEach { busSnapshot ->
                val busId = busSnapshot.key ?: return@forEach

                // Delete all "Accepted" requests for this owner's buses
                database.getReference("requests").orderByChild("status").equalTo("Accepted").get()
                    .await()
                    .children.forEach { reqSnapshot ->
                        val request = reqSnapshot.getValue(Request::class.java)
                        if (request?.busId == busId) {
                            reqSnapshot.ref.removeValue().await()
                        }
                    }

                deleteBus(busId).getOrThrow() // This deletes the bus, bus assignmnet and schedules for this bus
            }

            //Delete user
            database.getReference("users").child(uid).removeValue().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConductorData(uid: String): Result<Unit> {

        return try {

            val currentTime = System.currentTimeMillis()
            val snapshotSchedule = database.getReference("schedules").orderByChild("conductorId").equalTo(uid).get().await()

            for (childSnapshot in snapshotSchedule.children) {
                val schedule = childSnapshot.getValue(Schedule::class.java)
                if (schedule != null && schedule.startTime < currentTime && schedule.endTime >currentTime) {
                    return Result.failure(Exception("Can't delete account, a trip schedule is running now"))
                }
                if (schedule != null && schedule.startTime > currentTime) {
                    childSnapshot.ref.removeValue().await()
                }
            }

            //Removing location of this conductor
            database.getReference("conductorLocations").child(uid).removeValue().await()

            //Removing/Replacing requests accepted by this conductor?? Confused!!
//            val snapshot = database.getReference("requests").orderByChild("conductorId").equalTo(uid).get().await()
//
//            if (snapshot.exists()) {
//                val updates = mapOf<String, Any>(
//                    "acceptedAt"   to 0L,
//                    "acceptedBy"   to "",
//                    "busId"        to "",
//                    "conductorId"  to "",
//                    "otp"          to "",
//                    "status"       to "Pending"
//                )
//
//                for (child in snapshot.children) {
//                    child.ref.updateChildren(updates).await()
//                }
//            }

            //Remove this conductor from assigned bus
            database.getReference("busAssignments").orderByChild("conductorId").equalTo(uid).get().await()
                .children.forEach { it.ref.removeValue().await() }

            //Delete user
            database.getReference("users").child(uid).removeValue().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun registerBus(
        ownerId: String,
        name: String,
        number: String,
        fitnessCertificate: String,
        taxToken: String,
        stops: List<String>,
        route: BusRoute,
        fares: Map<String, Map<String, Int>>
    ): Result<Bus> {
        return try {
            val busId = database.getReference("buses").push().key ?: throw Exception("Failed to generate busId")
            val bus = Bus(
                busId = busId,
                ownerId = ownerId,
                name = name,
                number = number,
                fitnessCertificate = fitnessCertificate,
                taxToken = taxToken,
                stops = stops,
                route = route,
                fares = fares,
                createdAt = System.currentTimeMillis()
            )
            database.getReference("buses").child(busId).setValue(bus).await()
            Result.success(bus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBusesForOwner(ownerId: String): List<Bus> {
        return try {
            val snapshot = database.getReference("buses")
                .orderByChild("ownerId")
                .equalTo(ownerId)
                .get()
                .await()
            snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Bus::class.java)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get buses failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getBus(busId: String): Bus? {
        return try {
            val snapshot = database.getReference("buses").child(busId).get().await()
            snapshot.getValue(Bus::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get bus failed: ${e.message}", e)
            null
        }
    }

    suspend fun deleteBus(busId: String): Result<Unit> {
        return try {
            database.getReference("schedules").orderByChild("busId").equalTo(busId).get().await()
                .children.forEach { it.ref.removeValue().await() }
            database.getReference("busAssignments").child(busId).removeValue().await()
            database.getReference("buses").child(busId).removeValue().await()
            Log.d("AuthRepository", "Bus deleted: $busId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Delete bus failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    suspend fun getAnalyticForBusConductor(busId: String): Map<Date, Pair<Int, Double>> {
        val todayStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val threeDaysAgoStart = todayStart - (3 * 24 * 60 * 60 * 1000)
        val snapshot = try {
            database.getReference("requests")
                .orderByChild("status")
                .equalTo("Accepted")
                .get()
                .await()
        } catch (e: Exception) {
            println("Firebase error: ${e.message}")
            return emptyMap()
        }

        val dailyReport = mutableMapOf<Date, Pair<Int, Double>>()

        snapshot.children.forEach { child ->
            val req = child.getValue(Request::class.java) ?: return@forEach
            if (req.busId != busId) return@forEach
            if (req.acceptedAt <= 0) return@forEach
            if (req.acceptedAt !in threeDaysAgoStart until todayStart) return@forEach

            val fare = (req.fare ?: 0).toDouble()

            val dayStart = (req.acceptedAt / 86400000) * 86400000
            val dayKey = Date(dayStart)

            val (count, income) = dailyReport.getOrDefault(dayKey, 0 to 0.0)
            dailyReport[dayKey] = (count + 1) to (income + fare)
        }

        val dhakaFmt = SimpleDateFormat("dd MMM yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }

        return dailyReport.toSortedMap()
    }
    suspend fun getAnalyticForBus(busId: String): Map<Date, Pair<Int, Double>> {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: return emptyMap()

        val snapshot = try {
            database.getReference("requests")
                .orderByChild("status")
                .equalTo("Accepted")
                .get()
                .await()
        } catch (e: Exception) {
            println("Firebase error: ${e.message}")
            return emptyMap()
        }

        val dailyReport = mutableMapOf<Date, Pair<Int, Double>>()

        snapshot.children.forEach { child ->
            val req = child.getValue(Request::class.java) ?: return@forEach

            // Filter by busId in code
            if (req.busId != busId) return@forEach
            if (req.acceptedAt <= 0) return@forEach

            val fare = (req.fare ?: 0).toDouble()

            val day = Calendar.getInstance().apply {
                timeInMillis = req.acceptedAt
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val (count, income) = dailyReport.getOrDefault(day, 0 to 0.0)
            dailyReport[day] = (count + 1) to (income + fare)
        }

        return dailyReport.toSortedMap()
    }

    suspend fun getConductorsForOwner(ownerId: String): List<User> {
        return try {
            val snapshot = database.getReference("users")
                .orderByChild("ownerId")
                .equalTo(ownerId)
                .get()
                .await()
            snapshot.children.mapNotNull { child: DataSnapshot ->
                val conductor = child.getValue(User::class.java)
                if (conductor?.role == "Conductor") conductor else null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get conductors failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getAssignedConductorForBus(busId: String): String? {
        return try {
            val snapshot = database.getReference("busAssignments").child(busId).child("conductorId").get().await()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get assigned conductor failed: ${e.message}", e)
            null
        }
    }

    suspend fun assignConductorToBus(busId: String, conductorId: String): Result<Unit> {
        return try {
            val existingAssignment = suspendCoroutine<DatabaseReference?> { cont ->
                database.getReference("busAssignments").orderByChild("conductorId").equalTo(conductorId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                cont.resume(snapshot.children.first().ref)
                            } else {
                                cont.resume(null)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            cont.resumeWith(Result.failure(error.toException()))
                        }
                    })
            }
            existingAssignment?.removeValue()?.await()
            val assignment = BusAssignment(
                busId = busId,
                conductorId = conductorId
            )
            database.getReference("busAssignments").child(busId).setValue(assignment).await()
            Log.d("AuthRepository", "Conductor $conductorId assigned to bus $busId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Assign conductor failed: ${e.message}", e)
            Result.failure(Exception("কন্ডাক্টর অ্যাসাইন করতে ব্যর্থ: ${e.message}"))
        }
    }

    suspend fun removeConductorFromBus(busId: String): Result<Unit> {
        return try {
            database.getReference("busAssignments").child(busId).removeValue().await()
            Log.d("AuthRepository", "Conductor removed from bus $busId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Remove conductor failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun  getBusRouteForSchedule(busId: String): BusRoute? {
        return try {
            val snapshot = database.getReference("buses")
                .orderByChild("busId")
                .equalTo(busId)
                .get().await()
            if (snapshot.exists()) {
                val busSnapshot = snapshot.children.firstOrNull()
                val routeSnapshot = busSnapshot?.child("route")

                if (routeSnapshot != null && routeSnapshot.exists()) {
                    return routeSnapshot.getValue(BusRoute::class.java)
                } else {
                    return null
                }
            } else {
                return null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get bus route for schedule failed: ${e.message}", e)
            null
        }
    }

    suspend fun createSchedule(
        busId: String,
        conductorId: String,
        startTime: Long,
        endTime: Long,
        date: String,
        direction: String = "going"
    ): Result<Schedule> {
        return try {
            val assignedBusId = getBusForConductor(conductorId)
            if (assignedBusId != busId) throw Exception("Conductor is not assigned to bus $busId")
            val busRoute = getBusRouteForSchedule(busId)
            var tripRoute: BusRoute? = BusRoute()
            if (direction == "going"){
                tripRoute = busRoute
            } else if(direction == "returning"){
                tripRoute?.originLoc = busRoute?.destinationLoc
                tripRoute?.destinationLoc = busRoute?.originLoc
                tripRoute?.stopPointsLoc = busRoute?.stopPointsLoc?.reversed()?.toMutableList()!!
            }
            val scheduleId = database.getReference("schedules").push().key ?: throw Exception("Failed to generate scheduleId")
            val schedule = Schedule(
                scheduleId = scheduleId,
                busId = busId,
                conductorId = conductorId,
                startTime = startTime,
                endTime = endTime,
                date = date,
                createdAt = System.currentTimeMillis(),
                direction = direction,
                tripRoute = tripRoute
            )
            database.getReference("schedules")
                .child(scheduleId)
                .setValue(schedule)
                .await()
            Log.d("AuthRepository", "Schedule created: $scheduleId direction=$direction")
            Result.success(schedule)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Create schedule failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getSchedulesForConductor(conductorId: String): List<Schedule> {
        return try {
            val snapshot = database.getReference("schedules")
                .orderByChild("conductorId")
                .equalTo(conductorId)
                .get()
                .await()
            snapshot.children.mapNotNull { child ->
                child.getValue(Schedule::class.java)?.copy(scheduleId = child.key ?: "")
            }.filter { it.endTime >= System.currentTimeMillis() }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get schedules failed", e)
            emptyList()
        }
    }

    suspend fun getSchedulesForBus(busId: String): List<Schedule> {
        return try {
            val snapshot = database.getReference("schedules")
                .orderByChild("busId")
                .equalTo(busId)
                .get()
                .await()
            snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Schedule::class.java)
            }.filter { schedule ->
                val assignedConductorId = getAssignedConductorForBus(busId)
                assignedConductorId == schedule.conductorId && schedule.endTime >= System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get schedules for bus failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun cleanExpiredSchedules(): Result<Unit> {
        return try {
            val currentTime = System.currentTimeMillis()
            val snapshot = database.getReference("schedules").get().await()
            snapshot.children.forEach { child ->
                val schedule = child.getValue(Schedule::class.java) ?: return@forEach
                if ((currentTime - schedule.endTime) > 259200000 ) {    //Remove schedule after 5 days
                    child.ref.removeValue().await()
                    Log.d("AuthRepository", "Removed expired schedule: ${schedule.scheduleId}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Clean expired schedules failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cleanExpiredRequests(): Result<Unit> {
        return try {
            val currentTime = System.currentTimeMillis()
            val snapshot = database.getReference("requests").orderByChild("status").equalTo("Pending").get().await()
            snapshot.children.forEach { child ->
                val req = child.getValue(Request::class.java) ?: return@forEach
                if ((currentTime - req.createdAt) > 180000 ) {
                    child.ref.removeValue().await()
                    Log.d("AuthRepository", "Removed expired request: ${req.id}")
                }
            }
            val snapshot2 = database.getReference("requests").orderByChild("status").equalTo("Cancelled").get().await()
            snapshot2.children.forEach { child ->
                val req = child.getValue(Request::class.java) ?: return@forEach
                     child.ref.removeValue().await()
                    Log.d("AuthRepository", "Removed cancelled request: ${req.id}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Clean expired requests failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun submitTripRequest(
        riderId: String,
        pickup: String,
        destination: String,
        seats: Int,
        pickupLatLng: LatLng,
        destinationLatLng: LatLng,
        apiKey: String,
        directionsApi: DirectionsApi,
        busId: String? = null,
        route: BusRoute
    ): Result<Request> {
        return try {
            val origin = "${pickupLatLng.latitude},${pickupLatLng.longitude}"
            val dest = "${destinationLatLng.latitude},${destinationLatLng.longitude}"
            val directions = withContext(Dispatchers.IO) {
                directionsApi.getRoute(origin, dest, null, apiKey)
            }
            val fare = if (directions.status == "OK" && directions.routes.isNotEmpty()) {
                val distance = directions.routes[0].legs[0].distance.value
                (distance / 1000) * 10
            } else {
                100
            }
            val totalFare = fare * seats
            val requestId = database.getReference("requests").push().key ?: throw Exception("Failed to generate requestId")
            val request = Request(
                id = requestId,
                riderId = riderId,
                busId = busId,
                pickup = pickup,
                destination = destination,
                pickupLatLng = LatLngData(pickupLatLng.latitude, pickupLatLng.longitude),
                destinationLatLng = LatLngData(destinationLatLng.latitude, destinationLatLng.longitude),
                seats = seats,
                fare = totalFare,
                status = "Pending",
                createdAt = System.currentTimeMillis(),
                requestedRoute = route
            )
            database.getReference("requests").child(requestId).setValue(request).await()
            Log.d("AuthRepository", "Trip request submitted: $requestId")
            Result.success(request)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Submit trip request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingRequestsForConductor(
        conductorId: String,
        currentLocation: LatLng,
        bus: Bus,
        apiKey: String,
        directionsApi: DirectionsApi
    ): List<Request> {
        return try {
            val busId = getBusForConductor(conductorId) ?: return emptyList()
            var allRequests = mutableListOf<Request>()

            val now = System.currentTimeMillis()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            val schedules = getSchedulesForBus(busId)
            val activeSchedule = schedules.firstOrNull { it.date == today && it.startTime <= now && it.endTime >= now }?: return emptyList()

            val requestsSnapshot = database.getReference("requests").orderByChild("status").equalTo("Pending").get().await()

            //For returning, the route will be reversed
            val busReverseRoute = BusRoute(
                originLoc = bus.route?.destinationLoc,
                destinationLoc = bus.route?.originLoc,
                stopPointsLoc = bus.route?.stopPointsLoc?.reversed()?.toMutableList() ?: mutableListOf()
            )

            //Finding the correct request
            for (snapshot in requestsSnapshot.children) {
                val request = snapshot.getValue(Request::class.java)
                if (request != null && request.requestedRoute != null) {
                    // Check if the requested route matches either the bus's route or its reverse route
                    if (activeSchedule.direction == "going" && request.requestedRoute == bus.route ) {
                        allRequests.add(request)
                    } else if (activeSchedule.direction == "returning" && request.requestedRoute == busReverseRoute){
                        allRequests.add(request)
                    }
                }
            }
            val filteredRequests = allRequests.filter { req ->
                try {
                    val pickupLatLng = LatLng(
                        req.pickupLatLng!!.lat,
                        req.pickupLatLng.lng
                    )
                    val origin = "${currentLocation.latitude},${currentLocation.longitude}"
                    val dest = "${pickupLatLng.latitude},${pickupLatLng.longitude}"

                    val resp = withContext(Dispatchers.IO) {
                        directionsApi.getRoute(origin, dest, null, apiKey)
                    }

                    if (resp.status != "OK" || resp.routes.isEmpty()) return@filter false

                    val seconds = resp.routes[0].legs[0].duration.value

                    seconds <= 1800   // If conductor is now within 30 minutes range of that pickup station
                } catch (e: Exception) {
                    false
                }
            }
            filteredRequests
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun getLLofPickupDestofBusForRider(requestId: String): List<PointLocation> {
        try {
            var snapshot = database.getReference("requests")
                .child(requestId)
                .get()
                .await()
            val req = snapshot.getValue(Request::class.java)
            snapshot = req?.busId?.let {
                database.getReference("buses").child(it)
            }?.get()?.await()
            val bus = snapshot.getValue(Bus::class.java)
            val riderPickupGH = GeoFireUtils.getGeoHashForLocation(
                GeoLocation(req?.pickupLatLng?.lat ?: 0.00, req?.pickupLatLng?.lng ?: 0.00), 5
            )
            val riderDestGH = GeoFireUtils.getGeoHashForLocation(
                GeoLocation(req?.destinationLatLng?.lat ?: 0.00, req?.destinationLatLng?.lng ?: 0.00), 5
            )
            var pll: PointLocation? = null
            var dll: PointLocation? = null
            bus?.route?.let { route ->
                if (route.originLoc?.geohash == riderPickupGH) {
                    pll = route.originLoc
                } else if (route.destinationLoc?.geohash == riderPickupGH) {
                    pll = route.destinationLoc
                }
                for (stopPoint in route.stopPointsLoc ?: emptyList()) {
                    if (stopPoint.geohash == riderPickupGH) {
                        pll = stopPoint
                        break
                    }
                }
                if (route.originLoc?.geohash == riderDestGH) {
                    dll = route.originLoc
                } else if (route.destinationLoc?.geohash == riderDestGH) {
                    dll = route.destinationLoc
                }
                for (stopPoint in route.stopPointsLoc ?: emptyList()) {
                    if (stopPoint.geohash == riderDestGH) {
                        dll = stopPoint
                        break
                    }
                }
            }
            return listOfNotNull(pll, dll)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get request failed: ${e.message}", e)
            return emptyList()
        }
    }

    suspend fun acceptRequest(requestId: String, conductorId: String): Result<Unit> {
        return try {
            val snapshot = database.getReference("requests").child(requestId).get().await()
            val req = snapshot.getValue(Request::class.java) ?: throw Exception("রিকোয়েস্ট পাওয়া যায়নি")
            if (req.status != "Pending") throw Exception("রিকোয়েস্ট টি পেন্ডিং নেই")
            val busId = getBusForConductor(conductorId) ?: throw Exception("আপনার কোনো বাস অ্যাসাইন্ড নেই")
            val bus = getBus(busId) ?: throw Exception("বাস পাওয়া যায়নি")
            val schedules = getSchedulesForConductor(conductorId)
            val currentTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val currentDate = dateFormat.format(Date(currentTime))
            val activeSchedule = schedules.firstOrNull { it.date == currentDate && it.startTime <= currentTime && it.endTime >= currentTime }
                ?: throw Exception("আপনার এখন কোনো চলমান শিডিউল নেই")
            val otp = generateOTP()
            var updates = mapOf(
                "status" to "Accepted",
                "conductorId" to conductorId,
                "otp" to otp,
                "acceptedBy" to conductorId,
                "busId" to busId,
                "scheduleId" to activeSchedule.scheduleId,
                "acceptedAt" to System.currentTimeMillis()
            )
            val customBaseFare = bus.fares[req.pickup]?.get(req.destination)
            if (customBaseFare != null) {
                val totalFare = customBaseFare * req.seats
                updates = updates + ("fare" to totalFare)
            }
            database.getReference("requests").child(requestId).updateChildren(updates).await()
            Log.d("AuthRepository", "Request accepted with OTP: $otp for bus: $busId")
            val res = AuthRepository().updateFareForAcceptedRequest(req.id, busId)
            Log.d("AuthRepository", "Fare updated!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Accept request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateFareForAcceptedRequest(reqId: String, busId: String): Result<Unit> {
        return try {
            val reqSnapshot = database.getReference("requests")
                .child(reqId)
                .get()
                .await()

            val request = reqSnapshot.getValue(Request::class.java)
                ?: return Result.failure(Exception("Request not found"))

            val origin = request.pickup.replace(Regex("[^A-Za-z0-9 ]"), "")
            val destination = request.destination.replace(Regex("[^A-Za-z0-9 ]"), "")

            val busSnapshot = database.getReference("buses")
                .child(busId)
                .get()
                .await()

            val bus = busSnapshot.getValue(Bus::class.java)

            val fare = bus?.fares[origin]?.get(destination)
                ?: (-15)
            Log.d("AuthRepository", "The fetched fare is: $fare")
            if (fare != (-15)){
                val updatedRequest = request.copy(fare = fare)
                database.getReference("requests")
                    .child(reqId)
                    .setValue(updatedRequest)
                    .await()

                Result.success(Unit)
            } else {
                Result.success(Unit)
            }


        } catch (e: Exception) {
            Log.e("AuthRepository", "Update fare failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    suspend fun updateConductorLocation(conductorId: String, location: LatLng): Result<Unit> {
        return try {
            val locData = ConductorLocation(
                conductorId = conductorId,
                lat = location.latitude,
                lng = location.longitude,
                timestamp = System.currentTimeMillis()
            )
            database.getReference("conductorLocations").child(conductorId).setValue(locData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Update location failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getConductorLocation(conductorId: String): ConductorLocation? {
        return try {
            val snapshot = database.getReference("conductorLocations").child(conductorId).get().await()
            snapshot.getValue(ConductorLocation::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get location failed: ${e.message}", e)
            null
        }
    }

    suspend fun debugSaveTestData() {
        try {
            val testUser = User(
                uid = "test_conductor_uid",
                email = "testconductor@example.com",
                name = "Test Conductor",
                phone = "+8801712345678",
                age = 25,
                role = "Conductor",
                createdAt = System.currentTimeMillis(),
                ownerId = "test_owner_uid"
            )
            database.getReference("users").child("test_conductor_uid").setValue(testUser).await()
            Log.d("AuthRepository", "Test data saved")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Debug save test data failed: ${e.message}", e)
        }
    }

    private fun generateOTP(): String {
        return Random.nextInt(1000, 9999).toString()
    }

    suspend fun ensureOwnerRole(uid: String) {
        try {
            val userSnapshot = database.getReference("users").child(uid).get().await()
            val role = userSnapshot.child("role").getValue(String::class.java) ?: ""
            Log.d("AuthRepository", "User role for UID $uid: $role")
            if (role != "Owner") {
                database.getReference("users").child(uid).child("role").setValue("Owner").await()
                Log.d("AuthRepository", "User role updated to Owner for UID: $uid")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to ensure owner role: ${e.message}", e)
            throw e
        }
    }

    suspend fun getBusForConductor(conductorId: String): String? {
        return try {
            val snapshot = database.getReference("busAssignments")
                .orderByChild("conductorId")
                .equalTo(conductorId)
                .get().await()
            if (snapshot.exists()) {
                snapshot.children.first().key
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get bus for conductor failed: ${e.message}", e)
            null
        }
    }

    suspend fun getAcceptedRequestsForConductor(conductorId: String): List<Request> {
        return try {
            val currentTime = System.currentTimeMillis()
            val snapshot = database.getReference("requests")
                .orderByChild("acceptedBy")
                .equalTo(conductorId)
                .get().await()
            val requests = snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Request::class.java)
            }
            requests.filter { request ->
                if (request.scheduleId != null) {
                    val schedule = getSchedule(request.scheduleId) ?: return@filter false
                    schedule.endTime >= currentTime
                } else {
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get accepted requests failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getSchedule(scheduleId: String): Schedule? {
        return try {
            val snapshot = database.getReference("schedules").child(scheduleId).get().await()
            snapshot.getValue(Schedule::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get schedule failed: ${e.message}", e)
            null
        }
    }

    suspend fun checkDuplicateSchedule(startTime: Long, endTime: Long, conductId: String): Boolean {
        try {
            val snapshot = database.getReference("schedules").orderByChild("conductorId")
                .equalTo(conductId).get().await()
            val schedules = snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Schedule::class.java)
            }
            for (sch in schedules) {
                val existingStartTime = sch.startTime
                val existingEndTime = sch.endTime

                val endsBefore = endTime <= existingStartTime
                val startsAfter = startTime >= existingEndTime

                if (!(endsBefore || startsAfter)) {
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            Log.e("AuthRepository", "Checking schedule failed: ${e.message}", e)
            return false
        }
    }

    suspend fun sendMessage(requestId: String, text: String): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val messageId = database.getReference("messages").child(requestId).child("messages").push().key
                ?: throw Exception("Failed to generate messageId")
            val message = Message(
                senderId = currentUid,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            database.getReference("messages").child(requestId).child("messages").child(messageId).setValue(message).await()
            Log.d("AuthRepository", "Message sent for request $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Send message failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun listenToMessages(requestId: String, onMessagesChanged: (List<Message>) -> Unit) {
        database.getReference("messages").child(requestId).child("messages")
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                    onMessagesChanged(messages)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("AuthRepository", "Listen to messages cancelled: ${error.message}")
                }
            })
    }



    suspend fun confirmConductorArrivalAndFare(
        requestId: String,
        conductorId: String,
        fareCollected: Boolean = true
    ): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Conductor confirming arrival for request: $requestId")

            val requestRef = database.getReference("requests").child(requestId)
            val snapshot = requestRef.get().await()

            if (!snapshot.exists()) {
                throw Exception("রিকোয়েস্ট পাওয়া যায়নি")
            }

            val request = snapshot.getValue(Request::class.java)
                ?: throw Exception("রিকোয়েস্ট ডেটা পড়তে ব্যর্থ")

            if (request.conductorId != conductorId) {
                throw Exception("আপনার এই রিকোয়েস্টের অনুমতি নেই")
            }

            if (request.rideStatus?.inBusTravelling != true) {
                throw Exception("যাত্রী এখনো বাসে উঠেননি")
            }

            val currentTime = System.currentTimeMillis()
            val updates = mutableMapOf<String, Any>(
                "rideStatus/conductorArrivedConfirmed" to true,
                "rideStatus/conductorArrivedAt" to currentTime,
                "rideStatus/fareCollected" to fareCollected,
                "rideStatus/fareCollectedAt" to currentTime
            )

            // If rider also confirmed, mark trip as completed
            if (request.rideStatus?.riderArrivedConfirmed == true && fareCollected) {
                updates["rideStatus/tripCompleted"] = true
                updates["rideStatus/tripCompletedAt"] = currentTime
                updates["status"] = "Completed" // THIS IS KEY - Update main status
            }

            requestRef.updateChildren(updates).await()

            Log.d("AuthRepository", "Conductor confirmed arrival and fare")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Conductor confirmation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getNearbyPickup(
        location: LatLng
    ): List<Pair<BusRoute, PointLocation>> {
        return try{
            val currentTime = System.currentTimeMillis()
            val snapshot = database.getReference("schedules").orderByChild("startTime").get().await()

            val schedules = mutableListOf<Schedule>()
            for (scheduleSnapshot in snapshot.children) {
                val schedule = scheduleSnapshot.getValue(Schedule::class.java)
                if (schedule != null && schedule.startTime < currentTime && schedule.endTime > currentTime) {
                    schedules.add(schedule)
                }
            }
            val geoLocation = GeoLocation(location?.latitude ?: 0.00 , location?.longitude ?: 0.00)
            val RiderChosenGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)

            val returnRoutes = mutableListOf<Pair<BusRoute, PointLocation>>()

            //Checking if any route's origin or stoppoints matches with rider's chosen pickup point
            for (schedule in schedules) {
                val tripRoute = schedule.tripRoute

                if (tripRoute?.originLoc?.geohash == RiderChosenGeoHash) {
                      returnRoutes.add(Pair(tripRoute, tripRoute.originLoc!!))
                }

                tripRoute?.stopPointsLoc?.forEach { stopPoint ->
                    if (stopPoint.geohash == RiderChosenGeoHash) {
                        returnRoutes.add(Pair(tripRoute, stopPoint))
                    }
                }
            }
            return returnRoutes
        }catch (e: Exception) {
            Log.e("AuthRepository", "getNearbyBusStops failed: ${e.message}", e)
            emptyList()
        }
//        return try {
//            val snapshot = database.getReference("buses").get().await()
//            val riderLoc = GeoLocation(location.latitude, location.longitude)
//            val stopsWithDist = mutableListOf<StopWithDistance>()
//            for (child in snapshot.children) {
//                val bus = child.getValue(Bus::class.java) ?: continue
//                bus.route?.let { route ->
//                    fun addStopIfNear(loc: PointLocation?) {
//                        loc ?: return
//                        val stopLoc = GeoLocation(loc.latitude, loc.longitude)
//                        val distanceMeters = GeoFireUtils.getDistanceBetween(riderLoc, stopLoc)
//                        if (distanceMeters <= radiusKm * 1000) {
//                            stopsWithDist.add(StopWithDistance(loc, distanceMeters / 1000.0))
//                        }
//                    }
//                    addStopIfNear(route.originLoc)
//                    addStopIfNear(route.destinationLoc)
//                    route.stopPointsLoc.forEach { addStopIfNear(it) }
//                }
//            }
//            val result = stopsWithDist
//                .sortedBy { it.distanceKm }
//                .take(10)
//            Log.d(
//                "NearbyStops",
//                "Found ${result.size} stops within ${radiusKm}km of (${location.latitude}, ${location.longitude})"
//            )
//            result
//        } catch (e: Exception) {
//            Log.e("AuthRepository", "getNearbyBusStops failed: ${e.message}", e)
//            emptyList()
//        }
    }

    suspend fun getNearbyDestStops(selectedRoute: BusRoute, selectedPickup: PointLocation?, riderchosenlocation: LatLng): List<Pair<BusRoute, PointLocation>>{
        val returnRoutes = mutableListOf<Pair<BusRoute, PointLocation>>()

        //Geohash for rider chosen destination
        val geoLocation = GeoLocation(riderchosenlocation?.latitude ?: 0.00 , riderchosenlocation?.longitude ?: 0.00)
        val RiderChosenGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)

        // Check if selectedPickup is the origin
        if (selectedPickup == selectedRoute.originLoc) {

            selectedRoute.stopPointsLoc.forEach { stop ->
                if (stop.geohash == RiderChosenGeoHash) {
                    returnRoutes.add(Pair(selectedRoute, stop))
                }
            }

            if (selectedRoute.destinationLoc?.geohash == RiderChosenGeoHash) {
                returnRoutes.add(Pair(selectedRoute, selectedRoute.destinationLoc!!))
            }
            return  returnRoutes
        } else {
            // If selectedPickup is in stopPointsLoc, find later stops
            val pickupIndex = selectedRoute.stopPointsLoc.indexOf(selectedPickup)

            if (pickupIndex != -1) {
                // Check next stops
                for (i in pickupIndex + 1 until selectedRoute.stopPointsLoc.size) {
                    val stop = selectedRoute.stopPointsLoc[i]
                    if (stop.geohash == RiderChosenGeoHash) {
                        returnRoutes.add(Pair(selectedRoute, stop))
                    }
                }

                // Check if riderchosenlocation is at destination
                if (selectedRoute.destinationLoc?.geohash == RiderChosenGeoHash) {
                    returnRoutes.add(Pair(selectedRoute, selectedRoute.destinationLoc!!))
                }
            }
            return  returnRoutes
        }
    }

    suspend fun cancelTripRequest(requestId: String, riderId: String): Result<Unit> {
        return try {
            val snapshot = database.getReference("requests").child(requestId).get().await()
            val request = snapshot.getValue(Request::class.java) ?: throw Exception("Request not found")
            Log.d("AuthRepository", "Request debug: $request")
            Log.d("AuthRepository", "Cancel request: requestId=$requestId, riderId=$riderId, request.riderId=${request.riderId}, status=${request.status}")
            if (request.riderId != riderId) throw Exception("Unauthorized: You can only cancel your own requests")
            if (request.status != "Pending") throw Exception("Cannot cancel: Request is no longer pending")
            val updates = mapOf("status" to "Cancelled")
            database.getReference("requests").child(requestId).updateChildren(updates).await()
            Log.d("AuthRepository", "Request $requestId cancelled successfully by rider $riderId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Cancel request failed for requestId=$requestId: ${e.message}", e)
            Result.failure(e)
        }
    }



    suspend fun getConductorRatings(conductorId: String): ConductorRatings? {
        return try {
            val snapshot = database.getReference("conductorRatings")
                .child(conductorId)
                .get()
                .await()
            val ratings = snapshot.getValue(ConductorRatings::class.java)

            // If no ratings exist yet, try to generate them
            if (ratings == null) {
                Log.d("AuthRepository", "No conductor ratings found, attempting to generate for $conductorId")
                updateConductorRatings(conductorId)
                // Try to fetch again
                val newSnapshot = database.getReference("conductorRatings")
                    .child(conductorId)
                    .get()
                    .await()
                newSnapshot.getValue(ConductorRatings::class.java)
            } else {
                ratings
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get conductor ratings failed: ${e.message}", e)
            null
        }
    }

    suspend fun getBusRatings(busId: String): BusRatings? {
        return try {
            val snapshot = database.getReference("busRatings")
                .child(busId)
                .get()
                .await()
            val ratings = snapshot.getValue(BusRatings::class.java)

            // If no ratings exist yet, try to generate them
            if (ratings == null) {
                Log.d("AuthRepository", "No bus ratings found, attempting to generate for $busId")
                updateBusRatings(busId)
                // Try to fetch again
                val newSnapshot = database.getReference("busRatings")
                    .child(busId)
                    .get()
                    .await()
                newSnapshot.getValue(BusRatings::class.java)
            } else {
                ratings
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get bus ratings failed: ${e.message}", e)
            null
        }
    }


    suspend fun submitTripRating(
        requestId: String,
        riderId: String,
        conductorRating: Float,
        busRating: Float,
        overallRating: Float,
        comment: String
    ): Result<Unit> {
        return try {
            // Verify the request exists and belongs to the rider
            val snapshot = database.getReference("requests").child(requestId).get().await()
            val request = snapshot.getValue(Request::class.java)
                ?: throw Exception("Request not found")

            if (request.riderId != riderId) {
                throw Exception("Unauthorized: You can only rate your own trips")
            }

            // Check if trip is completed (either status is "Completed" or tripCompleted flag is true)
            val isCompleted = request.status == "Completed" ||
                    (request.status == "Accepted" && request.rideStatus?.tripCompleted == true)

            if (!isCompleted) {
                throw Exception("Can only rate completed trips")
            }

            if (request.rating != null) {
                throw Exception("You have already rated this trip")
            }

            val rating = TripRating(
                conductorRating = conductorRating,
                busRating = busRating,
                overallRating = overallRating,
                comment = comment,
                timestamp = System.currentTimeMillis(),
                riderId = riderId
            )

            // Save rating to the request
            database.getReference("requests")
                .child(requestId)
                .child("rating")
                .setValue(rating)
                .await()

            // Update conductor's aggregate ratings
            request.conductorId.takeIf { it.isNotEmpty() }?.let { conductorId ->
                updateConductorRatings(conductorId)
            }

            // Update bus's aggregate ratings
            request.busId?.let { busId ->
                updateBusRatings(busId)
            }

            Log.d("AuthRepository", "Rating submitted for request $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Submit rating failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun verifyOtpAndBoardRider(
        requestId: String,
        enteredOtp: String,
        conductorId: String
    ): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Verifying OTP for request: $requestId")

            val requestRef = database.getReference("requests").child(requestId)
            val snapshot = requestRef.get().await()

            if (!snapshot.exists()) {
                throw Exception("রিকোয়েস্ট পাওয়া যায়নি")
            }

            val request = snapshot.getValue(Request::class.java)
                ?: throw Exception("রিকোয়েস্ট ডেটা পড়তে ব্যর্থ")

            if (request.conductorId != conductorId) {
                throw Exception("আপনার এই রিকোয়েস্টের অনুমতি নেই")
            }

            if (request.otp != enteredOtp) {
                throw Exception("ভুল OTP। আবার চেষ্টা করুন।")
            }

            val currentTime = System.currentTimeMillis()
            val rideStatus = RideStatus(
                otpVerified = true,
                otpVerifiedAt = currentTime,
                inBusTravelling = true,
                boardedAt = currentTime
            )

            requestRef.child("rideStatus").setValue(rideStatus).await()

            Log.d("AuthRepository", "OTP verified, rider boarded successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("AuthRepository", "OTP verification failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun requestEarlyExit(
        requestId: String,
        riderId: String,
        earlyExitStop: String,
        earlyExitLatLng: LatLngData
    ): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Requesting early exit for request: $requestId")

            val requestRef = database.getReference("requests").child(requestId)
            val snapshot = requestRef.get().await()

            if (!snapshot.exists()) {
                throw Exception("রিকোয়েস্ট পাওয়া যায়নি")
            }

            val request = snapshot.getValue(Request::class.java)
                ?: throw Exception("রিকোয়েস্ট ডেটা পড়তে ব্যর্থ")

            if (request.riderId != riderId) {
                throw Exception("আপনার এই রিকোয়েস্টের অনুমতি নেই")
            }

            if (request.rideStatus?.inBusTravelling != true) {
                throw Exception("আপনি এখনো বাসে উঠেননি")
            }

            val updates = mapOf(
                "rideStatus/earlyExitRequested" to true,
                "rideStatus/earlyExitRequestedAt" to System.currentTimeMillis(),
                "rideStatus/earlyExitStop" to earlyExitStop,
                "rideStatus/earlyExitLatLng" to earlyExitLatLng
            )

            requestRef.updateChildren(updates).await()

            Log.d("AuthRepository", "Early exit requested successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Early exit request failed: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun requestLateExit(
        requestId: String,
        riderId: String,
        lateExitStop: String,
        lateExitLatLng: LatLngData
    ): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Requesting late exit for request: $requestId")

            val requestRef = database.getReference("requests").child(requestId)
            val snapshot = requestRef.get().await()

            if (!snapshot.exists()) {
                throw Exception("রিকোয়েস্ট পাওয়া যায়নি")
            }

            val request = snapshot.getValue(Request::class.java)
                ?: throw Exception("রিকোয়েস্ট ডেটা পড়তে ব্যর্থ")

            if (request.riderId != riderId) {
                throw Exception("আপনার এই রিকোয়েস্টের অনুমতি নেই")
            }

            if (request.rideStatus?.inBusTravelling != true) {
                throw Exception("আপনি এখনো বাসে উঠেননি")
            }

            val updates = mapOf(
                "rideStatus/lateExitRequested" to true,
                "rideStatus/lateExitStop" to lateExitStop,
                "rideStatus/lateExitLatLng" to lateExitLatLng
            )

            requestRef.updateChildren(updates).await()

            Log.d("AuthRepository", "Late exit requested successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Late exit request failed: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun calculateAndUpdateFare(
        requestId: String,
        actualDestination: String,
        actualDestinationLatLng: LatLngData,
        busId: String
    ): Result<Int> {
        return try {
            Log.d("AuthRepository", "Calculating fare for request: $requestId")

            val requestRef = database.getReference("requests").child(requestId)
            val requestSnapshot = requestRef.get().await()

            if (!requestSnapshot.exists()) {
                throw Exception("রিকোয়েস্ট পাওয়া যায়নি")
            }

            val request = requestSnapshot.getValue(Request::class.java)
                ?: throw Exception("রিকোয়েস্ট ডেটা পড়তে ব্যর্থ")

            // Get bus fare structure
            val busRef = database.getReference("buses").child(busId)
            val busSnapshot = busRef.get().await()

            if (!busSnapshot.exists()) {
                throw Exception("বাসের তথ্য পাওয়া যায়নি")
            }

            val bus = busSnapshot.getValue(Bus::class.java)
                ?: throw Exception("বাসের ডেটা পড়তে ব্যর্থ")

            // Try to get exact fare from bus fare structure first
            val exactFare = try {
                bus.fares[request.pickup]?.get(actualDestination)
            } catch (e: Exception) {
                null
            }

            val actualFare = if (exactFare != null) {
                // Use predefined fare if available
                exactFare * request.seats
            } else {
                // Calculate fare based on distance
                val pickupLat = request.pickupLatLng?.lat ?: 0.0
                val pickupLng = request.pickupLatLng?.lng ?: 0.0
                val destLat = actualDestinationLatLng.lat
                val destLng = actualDestinationLatLng.lng

                val distance = calculateDistance(pickupLat, pickupLng, destLat, destLng)

                // Base fare: 10 taka per km, minimum 20 taka
                val baseFare = (distance * 10).toInt().coerceAtLeast(20)
                baseFare * request.seats
            }

            // Update fare in request
            val updates = mapOf(
                "rideStatus/actualFare" to actualFare
            )

            requestRef.updateChildren(updates).await()

            Log.d("AuthRepository", "Fare calculated: $actualFare (was: ${request.fare})")
            Result.success(actualFare)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Fare calculation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun approveEarlyExit(
        requestId: String,
        conductorId: String
    ): Result<Unit> {
        return try {
            val requestRef = database.getReference("requests").child(requestId)
            val snapshot = requestRef.get().await()

            if (!snapshot.exists()) {
                throw Exception("রিকোয়েস্ট পাওয়া যায়নি")
            }

            val request = snapshot.getValue(Request::class.java)
                ?: throw Exception("রিকোয়েস্ট ডেটা পড়তে ব্যর্থ")

            if (request.conductorId != conductorId) {
                throw Exception("আপনার এই রিকোয়েস্টের অনুমতি নেই")
            }

            if (request.rideStatus?.earlyExitRequested != true) {
                throw Exception("যাত্রী আগাম নামার অনুরোধ করেননি")
            }

            // Calculate new fare based on early exit location
            val earlyExitLatLng = request.rideStatus.earlyExitLatLng
                ?: throw Exception("আগাম নামার স্থান পাওয়া যায়নি")

            val fareResult = calculateAndUpdateFare(
                requestId,
                request.rideStatus.earlyExitStop ?: "",
                earlyExitLatLng,
                request.busId ?: ""
            )

            if (fareResult.isFailure) {
                throw fareResult.exceptionOrNull() ?: Exception("ভাড়া গণনা ব্যর্থ")
            }

            // Mark early exit as approved by updating destination
            val updates = mapOf(
                "destination" to request.rideStatus.earlyExitStop,
                "destinationLatLng" to earlyExitLatLng
            )

            requestRef.updateChildren(updates).await()

            Log.d("AuthRepository", "Early exit approved with new fare: ${fareResult.getOrNull()}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Early exit approval failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun approveLateExit(
        requestId: String,
        conductorId: String
    ): Result<Unit> {
        return try {
            val requestRef = database.getReference("requests").child(requestId)
            val snapshot = requestRef.get().await()

            if (!snapshot.exists()) {
                throw Exception("রিকোয়েস্ট পাওয়া যায়নি")
            }

            val request = snapshot.getValue(Request::class.java)
                ?: throw Exception("রিকোয়েস্ট ডেটা পড়তে ব্যর্থ")

            if (request.conductorId != conductorId) {
                throw Exception("আপনার এই রিকোয়েস্টের অনুমতি নেই")
            }

            if (request.rideStatus?.lateExitRequested != true) {
                throw Exception("যাত্রী দেরিতে নামার অনুরোধ করেননি")
            }

            // Calculate new fare based on late exit location
            val lateExitLatLng = request.rideStatus.lateExitLatLng
                ?: throw Exception("দেরিতে নামার স্থান পাওয়া যায়নি")

            val fareResult = calculateAndUpdateFare(
                requestId,
                request.rideStatus.lateExitStop ?: "",
                lateExitLatLng,
                request.busId ?: ""
            )

            if (fareResult.isFailure) {
                throw fareResult.exceptionOrNull() ?: Exception("ভাড়া গণনা ব্যর্থ")
            }

            // Mark late exit as approved by updating destination
            val updates = mapOf(
                "destination" to request.rideStatus.lateExitStop,
                "destinationLatLng" to lateExitLatLng
            )

            requestRef.updateChildren(updates).await()

            Log.d("AuthRepository", "Late exit approved with new fare: ${fareResult.getOrNull()}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Late exit approval failed: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun getRequestsForUser(userId: String): List<Request> {
        return try {
            val snapshot = database.getReference("requests")
                .orderByChild("riderId")
                .equalTo(userId)
                .get().await()

            val requests = snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Request::class.java)
            }

            // Filter: Show ONLY active requests (Pending or Accepted but NOT completed)
            requests.filter { request ->
                when {
                    request.status == "Cancelled" -> false
                    request.status == "Pending" -> true
                    request.status == "Accepted" -> {
                        // Show accepted requests that are NOT yet completed
                        request.rideStatus?.tripCompleted != true
                    }
                    request.status == "Completed" -> false
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get requests failed: ${e.message}", e)
            emptyList()
        }
    }


    suspend fun getAllRequestsForUser(userId: String): List<Request> {
        return try {
            val snapshot = database.getReference("requests")
                .orderByChild("riderId")
                .equalTo(userId)
                .get().await()

            snapshot.children.mapNotNull { child ->
                child.getValue(Request::class.java)
            }
            // Return all requests - PastTripsScreen will handle filtering
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get all requests failed: ${e.message}", e)
            emptyList()
        }
    }


    suspend fun isChatEnabled(requestId: String): Boolean {
        return try {
            val snapshot = database.getReference("requests").child(requestId).get().await()
            val request = snapshot.getValue(Request::class.java) ?: return false

            // Chat is enabled if:
            // 1. Request is Accepted (or Completed status)
            // 2. Has a scheduleId
            // 3. Schedule ended within the last 5 days (432000000ms = 5 days)
            if ((request.status != "Accepted" && request.status != "Completed") ||
                request.scheduleId == null) return false

            val scheduleSnapshot = database.getReference("schedules")
                .child(request.scheduleId)
                .get()
                .await()
            val schedule = scheduleSnapshot.getValue(Schedule::class.java) ?: return false

            val currentTime = System.currentTimeMillis()
            val fiveDaysInMs = 432000000L // 5 days

            // Chat available if schedule ended within last 5 days
            currentTime <= (schedule.endTime + fiveDaysInMs)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Check chat enabled failed: ${e.message}", e)
            false
        }
    }



    suspend fun confirmRiderArrival(
        requestId: String,
        riderId: String
    ): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Rider confirming arrival for request: $requestId")

            val requestRef = database.getReference("requests").child(requestId)
            val snapshot = requestRef.get().await()

            if (!snapshot.exists()) {
                throw Exception("রিকোয়েস্ট পাওয়া যায়নি")
            }

            val request = snapshot.getValue(Request::class.java)
                ?: throw Exception("রিকোয়েস্ট ডেটা পড়তে ব্যর্থ")

            if (request.riderId != riderId) {
                throw Exception("আপনার এই রিকোয়েস্টের অনুমতি নেই")
            }

            if (request.rideStatus?.inBusTravelling != true) {
                throw Exception("আপনি এখনো বাসে উঠেননি")
            }

            val currentTime = System.currentTimeMillis()
            val updates = mutableMapOf<String, Any>(
                "rideStatus/riderArrivedConfirmed" to true,
                "rideStatus/riderArrivedAt" to currentTime
            )

            // If conductor also confirmed and fare collected, mark trip as completed
            if (request.rideStatus?.conductorArrivedConfirmed == true &&
                request.rideStatus.fareCollected == true) {
                updates["rideStatus/tripCompleted"] = true
                updates["rideStatus/tripCompletedAt"] = currentTime
                updates["status"] = "Completed" // THIS IS KEY - Update main status
            }

            requestRef.updateChildren(updates).await()

            Log.d("AuthRepository", "Rider arrival confirmed")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Rider arrival confirmation failed: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun canRateTrip(requestId: String, riderId: String): Boolean {
        return try {
            val snapshot = database.getReference("requests").child(requestId).get().await()
            val request = snapshot.getValue(Request::class.java) ?: return false

            Log.d("AuthRepository", "canRateTrip - requestId: $requestId, status: ${request.status}, tripCompleted: ${request.rideStatus?.tripCompleted}, hasRating: ${request.rating != null}")

            // Check if rider owns this request
            if (request.riderId != riderId) {
                Log.d("AuthRepository", "canRateTrip - Rider mismatch")
                return false
            }

            // Check if trip is completed (either Completed status or tripCompleted flag)
            val isCompleted = request.status == "Completed" ||
                    (request.status == "Accepted" && request.rideStatus?.tripCompleted == true)

            if (!isCompleted) {
                Log.d("AuthRepository", "canRateTrip - Trip not completed")
                return false
            }

            // Check if already rated
            if (request.rating != null) {
                Log.d("AuthRepository", "canRateTrip - Already rated")
                return false
            }

            Log.d("AuthRepository", "canRateTrip - Can rate: true")
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Check can rate failed: ${e.message}", e)
            false
        }
    }



    suspend fun getAllAcceptedRequestsForConductor(conductorId: String): List<Request> {
        return try {
            val snapshot = database.getReference("requests")
                .orderByChild("acceptedBy")
                .equalTo(conductorId)
                .get().await()

            val requests = snapshot.children.mapNotNull { child ->
                child.getValue(Request::class.java)
            }

            // Return ALL accepted requests (both active and completed)
            // Filter by status: "Accepted" or "Completed"
            val result = requests.filter {
                it.status == "Accepted" || it.status == "Completed"
            }

            Log.d("AuthRepository", "getAllAcceptedRequestsForConductor: Found ${result.size} requests for conductor $conductorId")

            result
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get all accepted requests failed: ${e.message}", e)
            emptyList()
        }
    }

    // Add these new functions to AuthRepository.kt

    /**
     * Automatically detect if rider has reached destination and complete trip
     * This should be called periodically (every 10-15 seconds) while rider is traveling
     */
    suspend fun checkAndAutoCompleteTrip(
        requestId: String,
        riderCurrentLocation: LatLng
    ): Result<Boolean> {
        return try {
            val requestRef = database.getReference("requests").child(requestId)
            val snapshot = requestRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Request not found"))
            }

            val request = snapshot.getValue(Request::class.java)
                ?: return Result.failure(Exception("Failed to parse request"))

            // Only auto-complete if rider is traveling and trip not already completed
            if (request.rideStatus?.inBusTravelling != true ||
                request.rideStatus.tripCompleted == true) {
                return Result.success(false)
            }

            // Get destination coordinates
            val destLat = request.destinationLatLng?.lat ?: return Result.success(false)
            val destLng = request.destinationLatLng?.lng ?: return Result.success(false)

            // Calculate distance to destination
            val distanceToDestination = calculateDistance(
                riderCurrentLocation.latitude,
                riderCurrentLocation.longitude,
                destLat,
                destLng
            )

            // If within 50 meters of destination, auto-complete
            if (distanceToDestination <= 0.05) { // 50 meters = 0.05 km
                Log.d("AutoComplete", "Rider reached destination, auto-completing trip")

                val currentTime = System.currentTimeMillis()
                val updates = mapOf(
                    "rideStatus/riderArrivedConfirmed" to true,
                    "rideStatus/riderArrivedAt" to currentTime,
                    "rideStatus/conductorArrivedConfirmed" to true,
                    "rideStatus/conductorArrivedAt" to currentTime,
                    "rideStatus/fareCollected" to true,
                    "rideStatus/fareCollectedAt" to currentTime,
                    "rideStatus/tripCompleted" to true,
                    "rideStatus/tripCompletedAt" to currentTime,
                    "status" to "Completed"
                )

                requestRef.updateChildren(updates).await()

                Log.d("AutoComplete", "Trip auto-completed successfully")
                return Result.success(true)
            }

            Result.success(false)
        } catch (e: Exception) {
            Log.e("AutoComplete", "Auto-complete check failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Automatically detect and handle early/late exit requests based on rider location
     * Returns: "early" if rider approaching early stop, "late" if past destination, null otherwise
     */
    suspend fun detectAndHandleExitChange(
        requestId: String,
        riderCurrentLocation: LatLng
    ): Result<String?> {
        return try {
            val requestRef = database.getReference("requests").child(requestId)
            val snapshot = requestRef.get().await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("Request not found"))
            }

            val request = snapshot.getValue(Request::class.java)
                ?: return Result.failure(Exception("Failed to parse request"))

            // Only check if rider is traveling
            if (request.rideStatus?.inBusTravelling != true) {
                return Result.success(null)
            }

            // Skip if already requested exit change
            if (request.rideStatus.earlyExitRequested || request.rideStatus.lateExitRequested) {
                return Result.success(null)
            }

            val busId = request.busId ?: return Result.success(null)
            val bus = getBus(busId) ?: return Result.success(null)
            val route = bus.route ?: return Result.success(null)

            // Get destination coordinates
            val destLat = request.destinationLatLng?.lat ?: return Result.success(null)
            val destLng = request.destinationLatLng?.lng ?: return Result.success(null)

            val distanceToOriginalDest = calculateDistance(
                riderCurrentLocation.latitude,
                riderCurrentLocation.longitude,
                destLat,
                destLng
            )

            // Check for EARLY EXIT (approaching a stop before destination)
            val allStops = mutableListOf<PointLocation>()
            route.originLoc?.let { allStops.add(it) }
            allStops.addAll(route.stopPointsLoc)
            route.destinationLoc?.let { allStops.add(it) }

            // Find destination index
            val destGeoHash = GeoFireUtils.getGeoHashForLocation(
                GeoLocation(destLat, destLng), 5
            )
            val destIndex = allStops.indexOfFirst { it.geohash == destGeoHash }

            // Check stops before destination
            for (i in 0 until destIndex) {
                val stop = allStops[i]
                val distanceToStop = calculateDistance(
                    riderCurrentLocation.latitude,
                    riderCurrentLocation.longitude,
                    stop.latitude,
                    stop.longitude
                )

                // If within 100 meters of a stop before destination
                if (distanceToStop <= 0.1) { // 100 meters
                    Log.d("AutoExitDetect", "Rider approaching early stop: ${stop.address}")

                    // Auto-request early exit and recalculate fare
                    val result = requestEarlyExit(
                        requestId,
                        request.riderId,
                        stop.address,
                        LatLngData(stop.latitude, stop.longitude)
                    )

                    if (result.isSuccess) {
                        // Auto-approve and recalculate fare
                        approveEarlyExit(requestId, request.conductorId)
                        return Result.success("early")
                    }
                }
            }

            // Check for LATE EXIT (passed destination, approaching next stop)
            if (distanceToOriginalDest > 0.2) { // More than 200m past destination
                // Check stops after destination
                for (i in (destIndex + 1) until allStops.size) {
                    val stop = allStops[i]
                    val distanceToStop = calculateDistance(
                        riderCurrentLocation.latitude,
                        riderCurrentLocation.longitude,
                        stop.latitude,
                        stop.longitude
                    )

                    // If within 100 meters of a stop after destination
                    if (distanceToStop <= 0.1) {
                        Log.d("AutoExitDetect", "Rider approaching late stop: ${stop.address}")

                        // Auto-request late exit and recalculate fare
                        val result = requestLateExit(
                            requestId,
                            request.riderId,
                            stop.address,
                            LatLngData(stop.latitude, stop.longitude)
                        )

                        if (result.isSuccess) {
                            // Auto-approve and recalculate fare
                            approveLateExit(requestId, request.conductorId)
                            return Result.success("late")
                        }
                    }
                }
            }

            Result.success(null)
        } catch (e: Exception) {
            Log.e("AutoExitDetect", "Exit detection failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Background monitoring service - call this periodically while rider is traveling
     * Combines auto-completion and exit detection
     */
    suspend fun monitorRiderTrip(
        requestId: String,
        riderCurrentLocation: LatLng
    ): Result<String> {
        return try {
            // First check for exit changes (early/late)
            val exitChange = detectAndHandleExitChange(requestId, riderCurrentLocation)
            if (exitChange.isSuccess && exitChange.getOrNull() != null) {
                return Result.success("exit_changed_${exitChange.getOrNull()}")
            }

            // Then check for trip completion
            val completed = checkAndAutoCompleteTrip(requestId, riderCurrentLocation)
            if (completed.isSuccess && completed.getOrNull() == true) {
                return Result.success("completed")
            }

            Result.success("monitoring")
        } catch (e: Exception) {
            Log.e("TripMonitor", "Monitoring failed: ${e.message}", e)
            Result.failure(e)
        }
    }


    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius of Earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }



    private suspend fun updateConductorRatings(conductorId: String) {
        try {
            // Get ALL requests and filter by conductorId in code
            val snapshot = database.getReference("requests").get().await()

            val ratedRequests = snapshot.children.mapNotNull { child ->
                val req = child.getValue(Request::class.java)
                // Filter: must match conductorId, be Accepted OR Completed, and have a rating
                if (req?.conductorId == conductorId &&
                    (req.status == "Accepted" || req.status == "Completed") &&  // ✅ FIXED
                    req.rating != null) {
                    req
                } else {
                    null
                }
            }

            Log.d("AuthRepository", "Found ${ratedRequests.size} rated requests for conductor $conductorId")

            if (ratedRequests.isEmpty()) {
                // Still create an empty rating object so owner knows there are no ratings yet
                val conductorRatings = ConductorRatings(
                    conductorId = conductorId,
                    totalRatings = 0,
                    averageRating = 0f,
                    totalTrips = 0,
                    reviews = emptyList()
                )
                database.getReference("conductorRatings")
                    .child(conductorId)
                    .setValue(conductorRatings)
                    .await()
                return
            }

            val totalRatings = ratedRequests.size
            val averageRating = ratedRequests.map { it.rating!!.conductorRating }.average().toFloat()

            // Get recent reviews (last 10)
            val reviews = ratedRequests
                .sortedByDescending { it.rating!!.timestamp }
                .take(10)
                .mapNotNull { req ->
                    val riderData = getUser(req.riderId).getOrNull()
                    ReviewSummary(
                        requestId = req.id,
                        riderName = riderData?.name ?: "Anonymous",
                        rating = req.rating!!.conductorRating,
                        comment = req.rating!!.comment,
                        timestamp = req.rating!!.timestamp,
                        route = "${req.pickup} to ${req.destination}"
                    )
                }

            val conductorRatings = ConductorRatings(
                conductorId = conductorId,
                totalRatings = totalRatings,
                averageRating = averageRating,
                totalTrips = ratedRequests.size,
                reviews = reviews
            )

            database.getReference("conductorRatings")
                .child(conductorId)
                .setValue(conductorRatings)
                .await()

            Log.d("AuthRepository", "Updated conductor ratings for $conductorId: avg=${averageRating}, total=${totalRatings}")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Update conductor ratings failed: ${e.message}", e)
        }
    }

    private suspend fun updateBusRatings(busId: String) {
        try {
            // Get ALL requests and filter by busId in code
            val snapshot = database.getReference("requests").get().await()

            val ratedRequests = snapshot.children.mapNotNull { child ->
                val req = child.getValue(Request::class.java)
                // Filter: must match busId, be Accepted OR Completed, and have a rating
                if (req?.busId == busId &&
                    (req.status == "Accepted" || req.status == "Completed") &&  // ✅ FIXED
                    req.rating != null) {
                    req
                } else {
                    null
                }
            }

            Log.d("AuthRepository", "Found ${ratedRequests.size} rated requests for bus $busId")

            if (ratedRequests.isEmpty()) {
                // Still create an empty rating object so owner knows there are no ratings yet
                val busRatings = BusRatings(
                    busId = busId,
                    totalRatings = 0,
                    averageRating = 0f,
                    totalTrips = 0,
                    reviews = emptyList()
                )
                database.getReference("busRatings")
                    .child(busId)
                    .setValue(busRatings)
                    .await()
                return
            }

            val totalRatings = ratedRequests.size
            val averageRating = ratedRequests.map { it.rating!!.busRating }.average().toFloat()

            // Get recent reviews (last 10)
            val reviews = ratedRequests
                .sortedByDescending { it.rating!!.timestamp }
                .take(10)
                .mapNotNull { req ->
                    val riderData = getUser(req.riderId).getOrNull()
                    ReviewSummary(
                        requestId = req.id,
                        riderName = riderData?.name ?: "Anonymous",
                        rating = req.rating!!.busRating,
                        comment = req.rating!!.comment,
                        timestamp = req.rating!!.timestamp,
                        route = "${req.pickup} to ${req.destination}"
                    )
                }

            val busRatings = BusRatings(
                busId = busId,
                totalRatings = totalRatings,
                averageRating = averageRating,
                totalTrips = ratedRequests.size,
                reviews = reviews
            )

            database.getReference("busRatings")
                .child(busId)
                .setValue(busRatings)
                .await()

            Log.d("AuthRepository", "Updated bus ratings for $busId: avg=${averageRating}, total=${totalRatings}")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Update bus ratings failed: ${e.message}", e)
        }
    }
}