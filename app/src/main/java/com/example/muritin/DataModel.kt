package com.example.muritin

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val email: String = "",
    val name: String? = null,
    val phone: String? = null,
    val age: Int? = null,
    val role: String = "Rider",
    val createdAt: Long = 0L,
    val ownerId: String? = null
)

@Serializable
data class Bus(
    val busId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val number: String = "",
    val fitnessCertificate: String = "",
    val taxToken: String = "",
    val stops: List<String> = emptyList(),
    val route: BusRoute? = null,
    val fares: Map<String, Map<String, Int>> = emptyMap(),
    val createdAt: Long = 0L
)

@Serializable
data class BusRoute(
    var originLoc: PointLocation ?= null,
    var stopPointsLoc: MutableList<PointLocation> = mutableListOf(),
    var destinationLoc: PointLocation ?= null
){
    fun clear() {
        originLoc = null
        destinationLoc = null
        stopPointsLoc.clear()
    }
}

@Serializable
data class PointLocation(
    var address: String = "",
    var latitude: Double = 0.00,
    var longitude: Double = 0.00,
    var geohash: String = ""
)

@Serializable
data class BusAssignment(
    val busId: String? = null,
    val conductorId: String? = null,
    val createdAt: Long? = System.currentTimeMillis()
)

@Serializable
data class Schedule(
    val scheduleId: String = "",
    val busId: String = "",
    val conductorId: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val date: String = "",
    val createdAt: Long = 0L
)

@Serializable
data class Request(
    val id: String = "",
    val riderId: String = "",
    val busId: String? = null,
    val scheduleId: String? = null,
    val pickup: String = "",
    val destination: String = "",
    val pickupLatLng: LatLngData? = null,
    val destinationLatLng: LatLngData? = null,
    var pickupGeoHash: String = "",
    var destinationGeoHash: String = "",
    val seats: Int = 1,
    val fare: Int = 0,
    val status: String = "Pending",
    val conductorId: String = "",
    val otp: String? = null,
    val preBookDate: String? = null,
    val createdAt: Long = 0L,
    val acceptedBy: String = "",
    val estimatedTimeToPickup: Int? = null,
    val acceptedAt: Long = 0L  // NEW: Timestamp when request was accepted
)

@Serializable
data class LatLngData(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

@Serializable
data class ConductorLocation(
    val conductorId: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Long = 0L
)

@Serializable  // NEW: Message model for chat
data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
@Serializable
data class StopWithDistance(
    val stop: PointLocation,
    val distanceKm: Double
)


