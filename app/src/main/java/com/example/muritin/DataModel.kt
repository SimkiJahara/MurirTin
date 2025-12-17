package com.example.muritin

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val email: String = "",
    val name: String? = null,
    val phone: String? = null,
    val nid: String? = null,
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
    val createdAt: Long = 0L,
    val direction: String = "going",
    val tripRoute: BusRoute?= null
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
    val status: String = "Pending", // Pending, Accepted, Cancelled, Completed
    val conductorId: String = "",
    val otp: String? = null,
    val preBookDate: String? = null,
    val createdAt: Long = 0L,
    val acceptedBy: String = "",
    val estimatedTimeToPickup: Int? = null,
    val acceptedAt: Long = 0L,
    val rating: TripRating? = null,
    val requestedRoute: BusRoute? = null,

    // NEW: Ride tracking fields
    val rideStatus: RideStatus? = null
)

// NEW: Rating model
@Serializable
data class TripRating(
    val conductorRating: Float = 0f,      // Rating for conductor (1-5)
    val busRating: Float = 0f,            // Rating for bus (1-5)
    val overallRating: Float = 0f,        // Overall trip rating (1-5)
    val comment: String = "",              // Rider's comment
    val timestamp: Long = 0L,              // When rating was submitted
    val riderId: String = ""               // Who submitted the rating
)

// NEW: Aggregated ratings for conductor
@Serializable
data class ConductorRatings(
    val conductorId: String = "",
    val totalRatings: Int = 0,
    val averageRating: Float = 0f,
    val totalTrips: Int = 0,
    val reviews: List<ReviewSummary> = emptyList()
)

// NEW: Aggregated ratings for bus
@Serializable
data class BusRatings(
    val busId: String = "",
    val totalRatings: Int = 0,
    val averageRating: Float = 0f,
    val totalTrips: Int = 0,
    val reviews: List<ReviewSummary> = emptyList()
)

// NEW: Summary of a review for display
@Serializable
data class ReviewSummary(
    val requestId: String = "",
    val riderName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Long = 0L,
    val route: String = ""  // e.g., "Mirpur to Uttara"
)

@Serializable
data class RideStatus(
    // OTP and Boarding
    val otpVerified: Boolean = false,
    val otpVerifiedAt: Long = 0L,
    val inBusTravelling: Boolean = false,
    val boardedAt: Long = 0L,

    // Early exit request (before destination)
    val earlyExitRequested: Boolean = false,
    val earlyExitRequestedAt: Long = 0L,
    val earlyExitStop: String? = null,
    val earlyExitLatLng: LatLngData? = null,

    // Late exit (beyond original destination)
    val lateExitRequested: Boolean = false,
    val lateExitStop: String? = null,
    val lateExitLatLng: LatLngData? = null,

    // Arrival confirmation (dual confirmation system)
    val riderArrivedConfirmed: Boolean = false,
    val riderArrivedAt: Long = 0L,
    val conductorArrivedConfirmed: Boolean = false,
    val conductorArrivedAt: Long = 0L,

    // Fare collection
    val fareCollected: Boolean = false,
    val fareCollectedAt: Long = 0L,
    val actualFare: Int = 0, // Updated fare if route changed

    // Trip completion
    val tripCompleted: Boolean = false,
    val tripCompletedAt: Long = 0L
)

@Serializable
data class FareCalculation(
    val originalPickup: String = "",
    val originalDestination: String = "",
    val originalFare: Int = 0,
    val actualPickup: String = "",
    val actualDestination: String = "",
    val actualDistance: Double = 0.0, // in km
    val calculatedFare: Int = 0,
    val fareAdjustmentReason: String = "", // "early_exit", "late_exit", "normal"
    val calculatedAt: Long = 0L
)





