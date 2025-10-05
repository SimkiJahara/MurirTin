package com.example.muritin

data class Bus(
    val busId: String = "",
    val name: String = "",
    val registrationNumber: String = "",
    val route: String = "",  // Simple string for route/stops (e.g., "Stop1-Stop2-Stop3"); can be expanded later
    val fitnessCertificateExpiry: String = "",  // "YYYY-MM-DD" format, no checks for now
    val taxTokenExpiry: String = "",  // "YYYY-MM-DD" format, no checks for now
    val conductorId: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0L,
    val status: String = "Active"  // "Paused" if expired, but no logic yet
)