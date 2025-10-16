
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
        date: String
    ): Result<Schedule> {
        return try {
            val scheduleId = database.getReference("schedules").push().key ?: throw Exception("Failed to generate scheduleId")
            val schedule = Schedule(
                scheduleId = scheduleId,
                busId = busId,
                conductorId = conductorId,
                startTime = startTime,
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
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get schedules for bus failed: ${e.message}", e)
            emptyList()
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
        directionsApi: DirectionsApi
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

            val allBuses = getAllBuses()
            val matchingBus = allBuses.firstOrNull { bus: Bus ->
                bus.stops.contains(pickup) && bus.stops.contains(destination) && bus.fares[pickup]?.containsKey(destination) == true
            }
            if (matchingBus == null) {
                return Result.failure(Exception("No matching bus route found"))
            }
            val baseFare = matchingBus.fares[pickup]?.get(destination) ?: fare
            val totalFare = baseFare * seats

            val requestId = database.getReference("requests").push().key ?: throw Exception("Failed to generate requestId")
            val request = Request(
                id = requestId,
                riderId = riderId,
                busId = matchingBus.busId,
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
            val allRequestsSnapshot = database.getReference("requests")
                .orderByChild("status")
                .equalTo("Pending")
                .get().await()
            val pendingRequests = allRequestsSnapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Request::class.java)
            }

            val routeMatching = pendingRequests.filter { req: Request ->
                bus.stops.contains(req.pickup) && bus.stops.contains(req.destination)
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
            nearbyRequests
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get pending requests failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun acceptRequest(requestId: String, conductorId: String): Result<Unit> {
        return try {
            val otp = generateOTP()
            val updates = mapOf(
                "status" to "Accepted",
                "conductorId" to conductorId,
                "otp" to otp,
                "acceptedBy" to conductorId
            )
            database.getReference("requests").child(requestId).updateChildren(updates).await()
            Log.d("AuthRepository", "Request accepted with OTP: $otp")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Accept request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRequestsForUser(userId: String): List<Request> {
        return try {
            val snapshot = database.getReference("requests")
                .orderByChild("riderId")
                .equalTo(userId)
                .get().await()
            snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Request::class.java)
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

    private suspend fun getAllBuses(): List<Bus> {
        return try {
            val snapshot = database.getReference("buses").get().await()
            snapshot.children.mapNotNull { child: DataSnapshot ->
                child.getValue(Bus::class.java)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get all buses failed: ${e.message}", e)
            emptyList()
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

    suspend fun fixUserRole(uid: String) {
        try {
            database.getReference("users").child(uid).child("role").setValue("Owner").await()
            Log.d("AuthRepository", "User role updated to Owner for UID: $uid")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update user role: ${e.message}", e)
        }
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
}

