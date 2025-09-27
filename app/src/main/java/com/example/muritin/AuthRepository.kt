package com.example.muritin

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
) {
    init {
        Log.d("AuthRepository", "Initializing AuthRepository")
        Log.d("AuthRepository", "FirebaseApp: ${FirebaseApp.getInstance().name}")
        Log.d("AuthRepository", "Database URL: ${database.reference.toString()}")
    }

    fun getCurrentUser(): FirebaseUser? {
        val user = auth.currentUser
        Log.d("AuthRepository", "getCurrentUser: ${user?.email ?: "null"}, uid: ${user?.uid ?: "null"}")
        return user
    }

    suspend fun signup(email: String, password: String, role: String): Result<User> {
        Log.d("AuthRepository", "Attempting signup for email: $email, role: $role")
        // Validate role
        if (role !in listOf("Rider", "Conductor", "Owner")) {
            Log.e("AuthRepository", "Invalid role: $role")
            return Result.failure(Exception("Invalid role"))
        }
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID"))
            Log.d("AuthRepository", "Signup successful, userId: $userId")
            val user = User(
                uid = userId,
                email = email,
                role = role,
                createdAt = System.currentTimeMillis()
            )
            Log.d("AuthRepository", "Attempting to save user: $user to path: users/$userId")
            database.getReference("users").child(userId).setValue(user).await()
            Log.d("AuthRepository", "User data saved to database: $role")
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
            // Validate role
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

    fun logout() {
        Log.d("AuthRepository", "Logging out")
        auth.signOut()
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

    suspend fun debugSaveTestData() {
        try {
            val testUser = User(
                uid = "test123",
                email = "test@example.com",
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
}