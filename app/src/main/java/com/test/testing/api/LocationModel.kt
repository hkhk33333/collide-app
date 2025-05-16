package com.test.testing.api

data class LocationModel(
    val latitude: Double,
    val longitude: Double,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val displayName: String = "Unknown User"
)

data class LocationResponse(
    val success: Boolean,
    val message: String
) 