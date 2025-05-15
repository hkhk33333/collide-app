package com.test.testing.api

data class LocationModel(
    val latitude: Double,
    val longitude: Double,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LocationResponse(
    val success: Boolean,
    val message: String
) 