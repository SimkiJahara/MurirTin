package com.example.muritin

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String? = null, // Optional for profile completion later
    val phone: String? = null, // Optional for profile completion
    val role: String = "Rider",
    val createdAt: Long = 0L
)