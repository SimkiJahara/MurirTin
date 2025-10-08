package com.example.muritin

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi


@Serializable
data class User(
    val uid: String = "",
    val email: String = "",
    val name: String? = null,
    val phone: String? = null,
    val age: Int? = null,
    val role: String = "Rider",
    val createdAt: Long = 0L,
    val ownerId: String? = null
)

@Serializable
data class Bus(
    val busId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val number: String = "",
    val fitnessCertificate: String = "",
    val taxToken: String = "",
    val stops: List<String> = emptyList(),
    val createdAt: Long = 0L
)

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

    @OptIn(InternalSerializationApi::class)
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

    @OptIn(InternalSerializationApi::class)
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
        stops: List<String>
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
        return try {
            val snapshot = database.getReference("buses")
                .orderByChild("ownerId")
                .equalTo(ownerId)
                .get()
                .await()
            snapshot.children.mapNotNull { it.getValue(Bus::class.java) }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get buses failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun deleteBus(busId: String): Result<Unit> {
        return try {
            database.getReference("buses").child(busId).removeValue().await()
            database.getReference("busAssignments").child(busId).removeValue().await()
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
            snapshot.children.mapNotNull { child ->
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
            // Check if the conductor is already assigned to another bus
            val snapshot = database.getReference("busAssignments")
                .orderByChild("conductorId")
                .equalTo(conductorId)
                .get()
                .await()
            if (snapshot.exists()) {
                val existingBusId = snapshot.children.first().key
                return Result.failure(Exception("কন্ডাক্টর ইতিমধ্যে বাস $existingBusId এ অ্যাসাইন করা হয়েছে"))
            }
            // Assign conductor to bus
            database.getReference("busAssignments").child(busId).child("conductorId").setValue(conductorId).await()
            Log.d("AuthRepository", "Conductor $conductorId assigned to bus $busId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Assign conductor failed: ${e.message}", e)
            Result.failure(e)
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
}
