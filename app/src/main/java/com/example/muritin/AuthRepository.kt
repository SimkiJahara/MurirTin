package com.example.muritin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signup(email: String, password: String, role: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID"))

            // Save user role to database
            val user = User(
                uid = userId,
                email = email,
                role = role,
                createdAt = System.currentTimeMillis()
            )

            database.getReference("users").child(userId).setValue(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID"))

            // Fetch user role from database
            val snapshot = database.getReference("users").child(userId).get().await()
            val user = snapshot.getValue(User::class.java)?.copy(
                uid = userId,
                email = result.user?.email ?: ""
            ) ?: User(uid = userId, email = result.user?.email ?: "", role = "Rider")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getUserRole(userId: String, onRoleReceived: (String) -> Unit) {
        database.getReference("users").child(userId).child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.value as? String ?: "Rider"
                onRoleReceived(role)
            }
    }
}