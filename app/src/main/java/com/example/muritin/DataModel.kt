package com.example.muritin

// Data models for storing bus schedules and booking requests in মুড়ির টিন.
// structured data for Firebase storage and UI display.

// Represents a bus schedule created by a Conductor.
//  Stores details like route, seats, and time for Riders to book.
//  Saved in Firebase at /schedules/<scheduleId> for real-time sync.
data class Schedule(
    // Unique ID for the bus (e.g., "Blue Minibus").
    val busId: String = "",
    // Route name (e.g., "Dhaka-Chittagong").
    val route: String = "",
    // Pickup location (e.g., "Gabtali").
    val pickup: String = "",
    // Destination location (e.g., "Chittagong").
    val destination: String = "",
    // Departure timestamp in milliseconds.
    val departureTime: Long = 0L,
    // Number of available seats on the bus.
    val availableSeats: Int = 0,
    // Date of the trip (e.g., "2025-09-27").
    val date: String = "",
    // UID of the Conductor who created the schedule.
    val conductorId: String = ""
)

// Represents a booking request made by a Rider.
// Tracks user bookings, including status and fare, for real-time updates.
//  Stored in Firebase at /requests/<requestId> for sync across devices.
data class Request(
    // Unique ID for the booking request.
    val id: String = "",
    // UID of the Rider making the request.
    val riderId: String = "",
    // Pickup location for the booking.
    val pickup: String = "",
    // Destination location for the booking.
    val destination: String = "",
    // Fare amount in BDT.
    val fare: Int = 0,
    // Status of the request (e.g., "Pending", "Accepted", "Rejected").
    val status: String = "Pending",
    // Timestamp when the request was created.
    val createdAt: Long = 0L,
    // UID of the Conductor who accepted the request.
    val acceptedBy: String = "",
    // Optional pre-booking date for future trips.
    val preBookDate: String? = null
)