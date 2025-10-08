
package com.example.muritin

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
    val fares: Map<String, Map<String, Int>> = emptyMap(),
    val createdAt: Long = 0L
)

@Serializable
data class Schedule(
    val scheduleId: String = "",
    val busId: String = "",
    val conductorId: String = "",
    val startTime: Long = 0L,
    val date: String = "",
    val createdAt: Long = 0L
)

@Serializable
data class Request(
    val id: String = "",
    val riderId: String = "",
    val pickup: String = "",
    val destination: String = "",
    val fare: Int = 0,
    val status: String = "Pending",
    val createdAt: Long = 0L,
    val acceptedBy: String = "",
    val preBookDate: String? = null
)
