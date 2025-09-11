package com.test.testing.discord.domain.repository

import android.location.Location
import com.test.testing.discord.models.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for location-related operations
 */
interface LocationRepository {
    /**
     * Get the current user location
     */
    fun getCurrentLocation(): Flow<Result<Location?>>

    /**
     * Start location updates
     */
    suspend fun startLocationUpdates(): Result<Unit>

    /**
     * Stop location updates
     */
    suspend fun stopLocationUpdates(): Result<Unit>

    /**
     * Check if location permission is granted
     */
    fun isLocationPermissionGranted(): Boolean

    /**
     * Check if background location permission is granted
     */
    fun isBackgroundPermissionGranted(): Boolean

    /**
     * Send location update to server
     */
    suspend fun sendLocationUpdate(location: Location): Result<Unit>

    /**
     * Get location settings
     */
    fun getLocationSettings(): LocationSettings

    /**
     * Update location settings
     */
    suspend fun updateSettings(settings: LocationSettings): Result<Unit>

    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(): Boolean

    /**
     * Request location services to be enabled
     */
    suspend fun requestLocationEnable(): Result<Unit>

    /**
     * Location settings data class
     */
    data class LocationSettings(
        val backgroundUpdatesEnabled: Boolean = true,
        val updateIntervalMs: Long = 60000L, // 1 minute
        val minimumMovementMeters: Float = 1000f, // 1km
        val desiredAccuracy: Float = 0f, // Full accuracy
    )
}
