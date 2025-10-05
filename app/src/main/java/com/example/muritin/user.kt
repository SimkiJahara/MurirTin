package com.example.muritin

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String? = null,
    val phone: String? = null,
    val age: Int? = null,
    val role: String = "Rider",
    val createdAt: Long = 0L,
    val ownerUid: String? = null,  // For conductors, links to owner
    val assignedBusId: String? = null  // For conductors, assigned bus
)

data class Bus(
    val busId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val regNumber: String = "",
    val route: List<Stop> = emptyList(),
    val fitnessUrl: String? = null,  // URL for fitness certificate
    val taxUrl: String? = null,      // URL for tax token
    val fitnessExpiry: Long? = null, // Expiry timestamp (for documentation)
    val taxExpiry: Long? = null,     // Expiry timestamp (for documentation)
    val conductorUid: String? = null // Assigned conductor
)

data class Stop(
    val lat: Double = 0.0,
    val lng: Double = 0.0
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