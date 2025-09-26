package com.example.muritin

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

// AuthRepository handles user authentication and data storage for মুড়ির টিন.
// It interacts with Firebase Authentication for login/signup and Firebase Realtime Database for user data.
//  Centralize auth logic to keep LoginScreen and dashboards clean.
class AuthRepository(
    // FirebaseAuth for email/password authentication.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    // FirebaseDatabase for storing user data at a specific Asia region URL.
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
) {
    // Initialize AuthRepository, logging setup details for debugging.
    init {
        Log.d("AuthRepository", "Initializing AuthRepository")
        Log.d("AuthRepository", "FirebaseApp: ${FirebaseApp.getInstance().name}")
        Log.d("AuthRepository", "Database URL: ${database.reference.toString()}")
    }

    // Returns the currently logged-in Firebase user, if any.
    //  Used to check if a user is logged in before showing dashboards.
    //  Fetches user from FirebaseAuth and logs their email/UID for debugging.
    fun getCurrentUser(): FirebaseUser? {
        val user = auth.currentUser
        Log.d("AuthRepository", "getCurrentUser: ${user?.email ?: "null"}, uid: ${user?.uid ?: "null"}")
        return user
    }

    // Signs up a new user with email, password, and role (Rider/Conductor).
    //  Allows users to create accounts and set their role for app access.
    //  Creates user in Firebase Auth, saves User object to Firebase Database, returns Result<User>.
    suspend fun signup(email: String, password: String, role: String): Result<User> {
        Log.d("AuthRepository", "Attempting signup for email: $email, role: $role")
        return try {
            // Create user in Firebase Authentication.
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID"))
            Log.d("AuthRepository", "Signup successful, userId: $userId")
            // Create User object with UID, email, role, and creation timestamp.
            val user = User(
                uid = userId,
                email = email,
                role = role,
                createdAt = System.currentTimeMillis()
            )
            // Save user data to Firebase Database at /users/<uid>.
            Log.d("AuthRepository", "Attempting to save user: $user to path: users/$userId")
            database.getReference("users").child(userId).setValue(user).await()
            Log.d("AuthRepository", "User data saved to database: $role")
            Result.success(user)
        } catch (e: Exception) {
            // Log and return failure if signup or database write fails.
            Log.e("AuthRepository", "Signup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Logs in an existing user with email and password.
    //  Authenticates users to access their role-based dashboard.
    //  Verifies credentials with Firebase Auth, fetches user data from Database, returns Result<User>.
    suspend fun login(email: String, password: String): Result<User> {
        Log.d("AuthRepository", "Attempting login for email: $email")
        return try {
            // Authenticate user with Firebase Authentication.
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID"))
            Log.d("AuthRepository", "Login successful, userId: $userId")
            // Fetch user data from Firebase Database.
            val snapshot = database.getReference("users").child(userId).get().await()
            Log.d("AuthRepository", "Snapshot value: ${snapshot.value}")
            // Parse user data, default to Rider role if missing.
            val user = snapshot.getValue(User::class.java)?.copy(
                uid = userId,
                email = result.user?.email ?: ""
            ) ?: User(uid = userId, email = result.user?.email ?: "", role = "Rider")
            Log.d("AuthRepository", "User data fetched: ${user.email}, role: ${user.role}")
            Result.success(user)
        } catch (e: Exception) {
            // Log and return failure if login or data fetch fails.
            Log.e("AuthRepository", "Login failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Logs out the current user.
    //  Allows users to exit the app securely.
    // Calls FirebaseAuth.signOut() to clear session.
    fun logout() {
        Log.d("AuthRepository", "Logging out")
        auth.signOut()
    }

    // Fetches the role of a user by their UID.
    //  Determines whether to show Rider or Conductor dashboard.
    //  Queries /users/<uid>/role in Firebase Database, defaults to Rider if error.
    suspend fun getUserRole(userId: String): String {
        Log.d("AuthRepository", "Fetching role for userId: $userId")
        return try {
            // Query the role field from Firebase Database.
            val snapshot = database.getReference("users").child(userId).child("role").get().await()
            Log.d("AuthRepository", "Role snapshot value: ${snapshot.value}")
            val role = snapshot.getValue(String::class.java) ?: "Rider"
            Log.d("AuthRepository", "Role fetched: $role")
            role
        } catch (e: Exception) {
            // Default to Rider role if fetch fails.
            Log.e("AuthRepository", "Error fetching role: ${e.message}", e)
            "Rider"
        }
    }
    //SKIP THIS
    // Saves test user data to Firebase for debugging.
    // Why: Helps  test Firebase Database without manual signup.
    // How: Writes a hardcoded test user to /users/test123.
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