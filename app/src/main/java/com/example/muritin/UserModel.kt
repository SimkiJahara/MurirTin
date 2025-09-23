package com.example.muritin

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "", // "Rider" or "Conductor"
    val createdAt: Long = 0L
)

data class Schedule(
    val busId: String = "",
    val route: String = "",
    val pickup: String = "",
    val destination: String = "",
    val departureTime: Long = 0L,
    val availableSeats: Int = 0,
    val date: String = "",
    val conductorId: String = ""
)

data class Request(
    val id: String = "",
    val riderId: String = "",
    val pickup: String = "",
    val destination: String = "",
    val fare: Int = 0,
    val status: String = "Pending", // Pending, Accepted, Rejected
    val createdAt: Long = 0L,
    val acceptedBy: String = "",
    val preBookDate: String? = null
)