package com.test.testing.discord.domain.models

import java.time.Instant
import kotlin.math.*

/**
 * Domain model representing a geographic location.
 * Contains location-specific business logic.
 */
data class DomainLocation(
    val latitude: Latitude,
    val longitude: Longitude,
    val accuracy: Distance,
    val timestamp: Instant,
) {
    /**
     * Calculate distance to another location using Haversine formula
     */
    fun distanceTo(other: DomainLocation): Distance {
        val lat1 = Math.toRadians(latitude.value)
        val lon1 = Math.toRadians(longitude.value)
        val lat2 = Math.toRadians(other.latitude.value)
        val lon2 = Math.toRadians(other.longitude.value)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = kotlin.math.sin(dLat / 2).pow(2) + kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * kotlin.math.sin(dLon / 2).pow(2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        // Earth's radius in meters
        val earthRadius = 6371000.0

        return Distance(c * earthRadius)
    }

    /**
     * Check if location is recent (within last 5 minutes)
     */
    fun isRecent(): Boolean {
        val fiveMinutesAgo = Instant.now().minusSeconds(300)
        return timestamp.isAfter(fiveMinutesAgo)
    }

    /**
     * Check if location accuracy is acceptable
     */
    fun hasAcceptableAccuracy(maxAccuracy: Distance = Distance(100.0)): Boolean = accuracy <= maxAccuracy
}

/**
 * Value object for latitude with validation
 */
@JvmInline
value class Latitude(
    val value: Double,
) {
    init {
        require(value in -90.0..90.0) { "Latitude must be between -90 and 90 degrees" }
    }

    override fun toString(): String = value.toString()
}

/**
 * Value object for longitude with validation
 */
@JvmInline
value class Longitude(
    val value: Double,
) {
    init {
        require(value in -180.0..180.0) { "Longitude must be between -180 and 180 degrees" }
    }

    override fun toString(): String = value.toString()
}

/**
 * Value object for distance measurements
 */
@JvmInline
value class Distance(
    val meters: Double,
) {
    init {
        require(meters >= 0) { "Distance cannot be negative" }
    }

    operator fun compareTo(other: Distance): Int = meters.compareTo(other.meters)

    operator fun plus(other: Distance): Distance = Distance(meters + other.meters)

    operator fun minus(other: Distance): Distance = Distance(meters - other.meters)

    companion object {
        val INFINITE = Distance(Double.MAX_VALUE)
    }
}
