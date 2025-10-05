package com.example.muritin

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String? = null,
    val phone: String? = null,
    val age: Int? = null,
    val role: String = "Rider",
    val createdAt: Long = 0L,
    val ownerId: String? = null  // New field for Conductors
)