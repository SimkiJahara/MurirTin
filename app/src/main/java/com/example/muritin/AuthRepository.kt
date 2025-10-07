package com.example.muritin

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.EmailAuthProvider

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
) {
    init {
        Log.d("AuthRepository", "Initializing AuthRepository")
        Log.d("AuthRepository", "FirebaseApp: ${FirebaseApp.getInstance().name}")
        Log.d("AuthRepository", "Database URL: ${database.reference.toString()}")
    }

    suspend fun signup(
        email: String,
        password: String,
        role: String,
        name: String,
        phone: String,
        age: Int,
        ownerPassword: String? = null // Owner's password for re-authentication
    ): Result<User> {
        Log.d("AuthRepository", "Attempting signup for email: $email, role: $role, name: $name, phone: $phone, age: $age")
        if (role !in listOf("Rider", "Conductor", "Owner")) {
            Log.e("AuthRepository", "Invalid role: $role")
            return Result.failure(Exception("Invalid role"))
        }

        // Check if the current user is an Owner when registering a Conductor
        var ownerId: String? = null
        var ownerEmail: String? = null
        if (role == "Conductor") {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val currentUserRole = getUserRole(currentUser.uid)
                if (currentUserRole != "Owner") {
                    Log.e("AuthRepository", "Only Owners can register Conductors")
                    return Result.failure(Exception("Only Bus Owners can register Conductors"))
                }
                if (ownerPassword == null) {
                    Log.e("AuthRepository", "Owner password required to register Conductor")
                    return Result.failure(Exception("Owner password required"))
                }
                // Re-authenticate the owner
                try {
                    val credential = EmailAuthProvider.getCredential(currentUser.email!!, ownerPassword)
                    currentUser.reauthenticate(credential).await()
                    ownerId = currentUser.uid
                    ownerEmail = currentUser.email
                    Log.d("AuthRepository", "Owner re-authenticated: ${currentUser.email}")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Owner re-authentication failed: ${e.message}", e)
                    return Result.failure(Exception("Invalid owner credentials"))
                }
            } else {
                Log.e("AuthRepository", "No authenticated user, cannot register Conductor")
                return Result.failure(Exception("Must be logged in as a Bus Owner to register Conductors"))
            }
        }

        return try {
            // Create the new user
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID"))
            Log.d("AuthRepository", "Signup successful, userId: $userId")

            val user = User(
                uid = userId,
                email = email,
                name = name,
                phone = phone,
                age = age,
                role = role,
                createdAt = System.currentTimeMillis(),
                ownerId = if (role == "Conductor") ownerId else null
            )
            Log.d("AuthRepository", "Attempting to save user: $user to path: users/$userId")
            database.getReference("users").child(userId).setValue(user).await()
            Log.d("AuthRepository", "User data saved to database: $role")

            // If conductor, sign out the new user and sign back in as owner
            if (role == "Conductor" && ownerId != null && ownerPassword != null && ownerEmail != null) {
                try {
                    auth.signOut()
                    auth.signInWithEmailAndPassword(ownerEmail, ownerPassword).await()
                    Log.d("AuthRepository", "Signed back in as owner: $ownerEmail")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Failed to sign back in as owner: ${e.message}", e)
                    return Result.failure(Exception("Failed to restore owner session"))
                }
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Signup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        Log.d("AuthRepository", "Attempting login for email: $email")
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID"))
            Log.d("AuthRepository", "Login successful, userId: $userId")
            val snapshot = database.getReference("users").child(userId).get().await()
            Log.d("AuthRepository", "Snapshot value: ${snapshot.value}")
            val user = snapshot.getValue(User::class.java)?.copy(
                uid = userId,
                email = result.user?.email ?: ""
            ) ?: User(uid = userId, email = result.user?.email ?: "", role = "Rider")
            if (user.role !in listOf("Rider", "Conductor", "Owner")) {
                Log.e("AuthRepository", "Invalid role found: ${user.role}")
                return Result.failure(Exception("Invalid role"))
            }
            Log.d("AuthRepository", "User data fetched: ${user.email}, role: ${user.role}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User> {
        Log.d("AuthRepository", "Fetching user data for userId: $userId")
        return try {
            val snapshot = database.getReference("users").child(userId).get().await()
            Log.d("AuthRepository", "Snapshot value: ${snapshot.value}")
            val user = snapshot.getValue(User::class.java)?.copy(
                uid = userId
            ) ?: return Result.failure(Exception("User not found"))
            Log.d("AuthRepository", "User fetched: ${user.email}, role: ${user.role}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching user: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRole(userId: String): String {
        Log.d("AuthRepository", "Fetching role for userId: $userId")
        return try {
            val snapshot = database.getReference("users").child(userId).child("role").get().await()
            Log.d("AuthRepository", "Role snapshot value: ${snapshot.value}")
            val role = snapshot.getValue(String::class.java) ?: "Rider"
            if (role !in listOf("Rider", "Conductor", "Owner")) {
                Log.e("AuthRepository", "Invalid role fetched: $role, defaulting to Rider")
                return "Rider"
            }
            Log.d("AuthRepository", "Role fetched: $role")
            role
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching role: ${e.message}", e)
            "Rider"
        }
    }

    suspend fun updateUserProfile(userId: String, name: String, phone: String, age: Int, email: String): Result<Boolean> {
        Log.d("AuthRepository", "Updating user profile for userId: $userId")
        return try {
            val userRef = database.getReference("users").child(userId)
            val updates = mapOf(
                "name" to name,
                "phone" to phone,
                "age" to age,
                "email" to email
            )
            userRef.updateChildren(updates).await()
            Log.d("AuthRepository", "User profile updated")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error updating user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun debugSaveTestData() {
        try {
            val testUser = User(
                uid = "test123",
                email = "test@example.com",
                name = "Test User",
                phone = "+8801712345678",
                age = 25,
                role = "Conductor",
                createdAt = System.currentTimeMillis()
            )
            Log.d("AuthRepository", "Attempting to save test user: $testUser to path: users/test123")
            database.getReference("users").child("test123").setValue(testUser).await()
            Log.d("AuthRepository", "Test data saved successfully")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to save test data: ${e.message}", e)
        }
    }

    suspend fun deleteUserData(userId: String): Result<Boolean> {
        Log.d("AuthRepository", "Deleting user data for userId: $userId")
        return try {
            database.getReference("users").child(userId).removeValue().await()
            Log.d("AuthRepository", "User data deleted successfully")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error deleting user data: ${e.message}", e)
            Result.failure(e)
        }
    }

    // New functions for bus registration and management

    suspend fun registerBus(
        ownerId: String,
        name: String,
        number: String,
        fitnessCertificate: String,
        taxToken: String,
        stops: List<String>
    ): Result<Bus> {
        return try {
            val busId = database.getReference("buses").push().key ?: return Result.failure(Exception("Failed to generate bus ID"))
            val bus = Bus(
                busId = busId,
                ownerId = ownerId,
                name = name,
                number = number,
                fitnessCertificate = fitnessCertificate,
                taxToken = taxToken,
                stops = stops,
                createdAt = System.currentTimeMillis()
            )
            database.getReference("buses").child(busId).setValue(bus).await()
            Log.d("AuthRepository", "Bus registered: $busId for owner: $ownerId")
            Result.success(bus)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Bus registration failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getBusesForOwner(ownerId: String): List<Bus> {
        try {
            val snapshot = database.getReference("buses")
                .orderByChild("ownerId")
                .equalTo(ownerId)
                .get()
                .await()
            val buses = mutableListOf<Bus>()
            for (child in snapshot.children) {
                val bus = child.getValue(Bus::class.java)
                if (bus != null) {
                    buses.add(bus)
                }
            }
            Log.d("AuthRepository", "Fetched ${buses.size} buses for owner: $ownerId")
            return buses
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching buses: ${e.message}", e)
            return emptyList()
        }
    }

    suspend fun deleteBus(busId: String): Result<Boolean> {
        return try {
            database.getReference("buses").child(busId).removeValue().await()
            database.getReference("busAssignments").child(busId).removeValue().await() // Remove assignments
            Log.d("AuthRepository", "Bus deleted: $busId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error deleting bus: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun assignConductorToBus(busId: String, conductorId: String): Result<Boolean> {
        return try {
            database.getReference("busAssignments").child(busId).setValue(conductorId).await()
            Log.d("AuthRepository", "Assigned conductor $conductorId to bus $busId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error assigning conductor: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun removeConductorFromBus(busId: String): Result<Boolean> {
        return try {
            database.getReference("busAssignments").child(busId).removeValue().await()
            Log.d("AuthRepository", "Removed conductor from bus $busId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error removing conductor: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAssignedConductorForBus(busId: String): String? {
        return try {
            val snapshot = database.getReference("busAssignments").child(busId).get().await()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching assigned conductor: ${e.message}", e)
            null
        }
    }

    suspend fun getConductorsForOwner(ownerId: String): List<User> {
        try {
            val snapshot = database.getReference("users")
                .orderByChild("ownerId")
                .equalTo(ownerId)
                .get()
                .await()
            val conductors = mutableListOf<User>()
            for (child in snapshot.children) {
                val user = child.getValue(User::class.java)
                if (user != null && user.role == "Conductor") {
                    conductors.add(user)
                }
            }
            Log.d("AuthRepository", "Fetched ${conductors.size} conductors for owner: $ownerId")
            return conductors
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching conductors: ${e.message}", e)
            return emptyList()
        }
    }
}
