package com.example.muritin

// User data class represents a user in the মুড়ির টিন app.
// Why: Stores user information like UID, email, and role for Firebase and UI.
// How: Used in AuthRepository to save/fetch user data and in dashboards for display.
data class User(
    // Unique ID from Firebase Authentication.
    val uid: String = "",
    // User's email address.
    val email: String = "",
    // Role of the user (Rider, Conductor, or Owner). Defaults to "Rider" for safety.
    // Why default "Rider"? Most users are expected to be Riders (passengers),
    // and this prevents null or invalid roles during signup/login failures.
    val role: String = "Rider",
    // Timestamp when the user account was created.
    val createdAt: Long = 0L
)