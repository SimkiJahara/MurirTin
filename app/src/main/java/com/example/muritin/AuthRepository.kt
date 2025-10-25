package com.example.muritin

import android.util.Log
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

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")

    suspend fun signup(
        email: String,
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
                age = age,
                role = role,
                createdAt = System.currentTimeMillis(),
                ownerId = ownerId
            )
            database.getReference("users").child(firebaseUser.uid).setValue(user).await()
            Log.d("AuthRepository", "User data saved to database: $role")
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
            Log.d("AuthRepository", "Attempting to register bus: $bus")
            database.getReference("buses").child(busId).setValue(bus).await()
            Log.d("AuthRepository", "Bus registered: $busId for owner: $ownerId")
            Result.success(bus)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Bus registration failed: ${e.message}", e)
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
            database.getReference("buses").child(busId).removeValue().await()
            database.getReference("busAssignments").child(busId).removeValue().await()
            database.getReference("schedules").orderByChild("busId").equalTo(busId).get().await()
                .children.forEach { it.ref.removeValue().await() }
            Log.d("AuthRepository", "Bus deleted: $busId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Delete bus failed: ${e.message}", e)
            Result.failure(e)
        }
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

    suspend fun createSchedule(
        busId: String,
        conductorId: String,
        startTime: Long,
        endTime: Long,
        date: String
    ): Result<Schedule> {
        return try {
            // Validate that the conductor is assigned to the bus
            val assignedBusId = getBusForConductor(conductorId)
            if (assignedBusId != busId) {
                throw Exception("Conductor is not assigned to bus $busId")
            }
            val scheduleId = database.getReference("schedules").push().key ?: throw Exception("Failed to generate scheduleId")
            val schedule = Schedule(
                scheduleId = scheduleId,
                busId = busId,
                conductorId = conductorId,
                startTime = startTime,
                endTime = endTime,
                date = date,
                createdAt = System.currentTimeMillis()
            )
            database.getReference("schedules").child(scheduleId).setValue(schedule).await()
            Log.d("AuthRepository", "Schedule created: $scheduleId for bus: $busId")
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
            snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Schedule::class.java)
            }.filter { schedule ->
                // Only include schedules where the conductor is still assigned to the bus
                val assignedBusId = getBusForConductor(conductorId)
                assignedBusId == schedule.busId && schedule.endTime >= System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get schedules failed: ${e.message}", e)
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
                // Only include schedules where the conductor is still assigned to the bus
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
                if (schedule.endTime < currentTime) {
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

    suspend fun submitTripRequest(
        riderId: String,
        pickup: String,
        destination: String,
        seats: Int,
        pickupLatLng: LatLng,
        destinationLatLng: LatLng,
        apiKey: String,
        directionsApi: DirectionsApi,
        busId: String? = null
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
                createdAt = System.currentTimeMillis()
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
            val schedules = getSchedulesForBus(busId)
            val currentTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val currentDate = dateFormat.format(Date(currentTime))
            val geoHashesofThisBus: MutableList<String> = mutableListOf()

            val activeSchedule = schedules.firstOrNull {
                it.date == currentDate && it.startTime <= currentTime && it.endTime >= currentTime
            }
            if (activeSchedule == null) {
                Log.d("AuthRepository", "No active schedule for conductor $conductorId on $currentDate")
                return emptyList()
            }

            val snapshot = database.getReference("requests")
                .orderByChild("status")
                .equalTo("Pending")
                .get()
                .await()
            val pendingRequests = snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Request::class.java)
            }
            //Converting rider's pickup and destination point to geohash
            for (req in pendingRequests) {
                var geoLocation = GeoLocation(
                    req.pickupLatLng?.lat ?: 0.0,
                    req.pickupLatLng?.lng ?: 0.0
                )
                req.pickupGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)
                geoLocation = GeoLocation(
                    req.destinationLatLng?.lat ?: 0.0,
                    req.destinationLatLng?.lng ?: 0.0
                )
                req.destinationGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)
            }
            //Collecting geohashes for the assigned bus route
            if (bus.route != null) {
                bus.route.originLoc?.let { loc ->
                    val originGeohash = loc.geohash
                    if (originGeohash.isNotEmpty()) geoHashesofThisBus.add(originGeohash)
                }
                bus.route.destinationLoc?.let { loc ->
                    val destGeohash = loc.geohash
                    if (destGeohash.isNotEmpty()) geoHashesofThisBus.add(destGeohash)
                }
                for (point in bus.route.stopPointsLoc) {
                    val stopGeohash = point.geohash
                    if (stopGeohash.isNotEmpty()) geoHashesofThisBus.add(stopGeohash)
                }
            }

            val routeMatching = pendingRequests.filter { req: Request ->
                geoHashesofThisBus.contains(req.pickupGeoHash) && geoHashesofThisBus.contains(req.destinationGeoHash)
            }

            val nearbyRequests = routeMatching.filter { req: Request ->
                val pickupLatLng = LatLng(req.pickupLatLng!!.lat, req.pickupLatLng.lng)
                val originStr = "${currentLocation.latitude},${currentLocation.longitude}"
                val destStr = "${pickupLatLng.latitude},${pickupLatLng.longitude}"
                val response = withContext(Dispatchers.IO) {
                    directionsApi.getRoute(originStr, destStr, null, apiKey)
                }
                if (response.status == "OK" && response.routes.isNotEmpty()) {
                    val duration = response.routes[0].legs[0].duration.value
                    (duration / 60) <= 30
                } else {
                    false
                }
            }
            Log.d("AuthRepository", "Fetched ${nearbyRequests.size} pending requests for bus $busId")
            nearbyRequests
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get pending requests failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getLLofPickupDestofBusForRider(requestId: String): List<PointLocation> {
        try{
            var snapshot = database.getReference("requests")
                .child(requestId) // Access the specific requestId node
                .get()
                .await()
            val req = snapshot.getValue(Request::class.java)
            snapshot = req?.busId?.let {
                database.getReference("buses")
                    .child(it)
            } // Access the specific requestId node
                ?.get()
                ?.await()
            val bus = snapshot.getValue(Bus::class.java)

            //Geohash of rider's choice
            val riderPickupGH = GeoFireUtils.getGeoHashForLocation(GeoLocation(req?.pickupLatLng?.lat
                ?: 0.00, req?.pickupLatLng?.lng ?: 0.00), 5)
            val riderDestGH = GeoFireUtils.getGeoHashForLocation(GeoLocation(req?.destinationLatLng?.lat
                ?: 0.00, req?.destinationLatLng?.lng ?: 0.00), 5)
            var pll: PointLocation? = null
            var dll: PointLocation? = null

            //Getting the points of the bus which is near rider's choice
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
                        break // Found destination location, no need to continue
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
            val req = snapshot.getValue(Request::class.java) ?: throw Exception("Request not found")
            if (req.status != "Pending") throw Exception("Request not pending")

            val busId = getBusForConductor(conductorId) ?: throw Exception("No bus assigned")
            val bus = getBus(busId) ?: throw Exception("Bus not found")

            val schedules = getSchedulesForConductor(conductorId)
            val currentTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val currentDate = dateFormat.format(Date(currentTime))
            val activeSchedule = schedules.firstOrNull {
                it.date == currentDate && it.startTime <= currentTime && it.endTime >= currentTime
            }
            if (activeSchedule == null) throw Exception("No active schedule")

//            val pickupIndex = bus.stops.indexOf(req.pickup)
//            val destIndex = bus.stops.indexOf(req.destination)
//            if (pickupIndex == -1 || destIndex == -1 || pickupIndex >= destIndex) throw Exception("Request does not match bus route")

            val otp = generateOTP()
            var updates = mapOf<String, Any?>(
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
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Accept request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRequestsForUser(userId: String): List<Request> {
        return try {
            val currentTime = System.currentTimeMillis()
            val snapshot = database.getReference("requests")
                .orderByChild("riderId")
                .equalTo(userId)
                .get().await()
            val requests = snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Request::class.java)
            }
            requests.filter { request ->
                if (request.status == "Accepted" && request.scheduleId != null) {
                    val schedule = getSchedule(request.scheduleId) ?: return@filter false
                    schedule.endTime >= currentTime
                } else {
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get requests failed: ${e.message}", e)
            emptyList()
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

//    private suspend fun getAllBuses(): List<Bus> {
//        return try {
//            val snapshot = database.getReference("buses").get().await()
//            snapshot.children.mapNotNull { child: DataSnapshot ->
//                child.getValue(Bus::class.java)
//            }
//        } catch (e: Exception) {
//            Log.e("AuthRepository", "Get all buses failed: ${e.message}", e)
//            emptyList()
//        }
//    }

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

//    suspend fun fixUserRole(uid: String) {
//        try {
//            database.getReference("users").child(uid).child("role").setValue("Owner").await()
//            Log.d("AuthRepository", "User role updated to Owner for UID: $uid")
//        } catch (e: Exception) {
//            Log.e("AuthRepository", "Failed to update user role: ${e.message}", e)
//        }
//    }

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

    suspend fun sendMessage(requestId: String, text: String): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val messageId = database.getReference("messages").child(requestId).child("messages").push().key ?: throw Exception("Failed to generate messageId")
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

    suspend fun isChatEnabled(requestId: String): Boolean {
        return try {
            val snapshot = database.getReference("requests").child(requestId).get().await()
            val request = snapshot.getValue(Request::class.java) ?: return false
            if (request.status != "Accepted" || request.scheduleId == null) return false
            val scheduleSnapshot = database.getReference("schedules").child(request.scheduleId).get().await()
            val schedule = scheduleSnapshot.getValue(Schedule::class.java) ?: return false
            val currentTime = System.currentTimeMillis()
            currentTime <= schedule.endTime + 432000000L  // 5 days after endTime
        } catch (e: Exception) {
            Log.e("AuthRepository", "Check chat enabled failed: ${e.message}", e)
            false
        }
    }
}