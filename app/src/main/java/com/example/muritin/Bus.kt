package com.example.muritin

data class Bus(
    val busId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val number: String = "",
    val fitnessCertificate: String = "",
    val taxToken: String = "",
    val stops: List<String> = emptyList(),
    val createdAt: Long = 0L
)