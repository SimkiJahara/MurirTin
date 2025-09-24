package com.example.muritin

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "Rider",
    val createdAt: Long = 0L
)