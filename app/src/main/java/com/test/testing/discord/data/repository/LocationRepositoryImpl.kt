package com.test.testing.discord.data.repository

import android.content.Context
import android.location.Location
import android.util.Log
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.domain.repository.LocationRepository
import com.test.testing.discord.location.LocationManager
import com.test.testing.discord.models.*
import com.test.testing.discord.network.NetworkResilience
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of LocationRepository using LocationManager and network services
 */
class LocationRepositoryImpl(
    private val context: Context,
    private val locationManager: LocationManager = LocationManager.getInstance(context),
    private val networkResilience: NetworkResilience = NetworkResilience.getInstance(context),
) : LocationRepository {
    private val token: String?
        get() =
            AuthManager.instance.token.value
                ?.let { "Bearer $it" }

    override fun getCurrentLocation(): Flow<Result<Location?>> =
        locationManager.locationUpdates
            .map { location -> Result.success(location) }

    override suspend fun startLocationUpdates(): Result<Unit> =
        try {
            locationManager.startLocationUpdates()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }

    override suspend fun stopLocationUpdates(): Result<Unit> =
        try {
            locationManager.stopLocationUpdates()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }

    override fun isLocationPermissionGranted(): Boolean = locationManager.locationPermissionGranted

    override fun isBackgroundPermissionGranted(): Boolean = locationManager.backgroundPermissionGranted

    override suspend fun sendLocationUpdate(location: Location): Result<Unit> {
        if (token == null) {
            return Result.error(
                Exception("Authentication required"),
                errorType = ErrorType.AUTHENTICATION,
            )
        }

        // Create location data for API
        val locationData =
            com.test.testing.discord.models.Location(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy.toDouble(),
                desiredAccuracy = locationManager.desiredAccuracy.toDouble(),
                lastUpdated = System.currentTimeMillis().toDouble(),
            )

        // Use network resilience for the API call
        return networkResilience.executeWithResilience(
            operation = {
                try {
                    // Create a user update with location data
                    val currentUser = getCurrentUser()
                    if (currentUser != null) {
                        val updatedUser = currentUser.copy(location = locationData)
                        val response = ApiClient.getInstance().apiService.updateCurrentUser(token!!, updatedUser)

                        if (response.isSuccessful) {
                            Result.success(Unit)
                        } else {
                            Result.error(
                                Exception("Failed to update location: ${response.code()}"),
                                errorType = ErrorType.SERVER,
                                canRetry = response.code() in 500..599,
                            )
                        }
                    } else {
                        Result.error(
                            Exception("No current user data available"),
                            errorType = ErrorType.CLIENT,
                        )
                    }
                } catch (e: Exception) {
                    Result.error(e, errorType = ErrorType.NETWORK, canRetry = true)
                }
            },
            operationName = "sendLocationUpdate",
        )
    }

    override fun getLocationSettings(): LocationRepository.LocationSettings =
        LocationRepository.LocationSettings(
            backgroundUpdatesEnabled = locationManager.backgroundUpdatesEnabled,
            updateIntervalMs = locationManager.updateInterval,
            minimumMovementMeters = locationManager.minimumMovementThreshold,
            desiredAccuracy = locationManager.desiredAccuracy,
        )

    override suspend fun updateSettings(settings: LocationRepository.LocationSettings): Result<Unit> =
        try {
            locationManager.updateBackgroundUpdates(settings.backgroundUpdatesEnabled)
            locationManager.updateInterval(settings.updateIntervalMs)
            locationManager.updateMinimumMovement(settings.minimumMovementMeters)
            locationManager.updateDesiredAccuracy(settings.desiredAccuracy)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }

    override fun isLocationEnabled(): Boolean {
        // This would need to be implemented in LocationManager
        // For now, return true if we have location updates
        return true
    }

    override suspend fun requestLocationEnable(): Result<Unit> {
        // This would need to be implemented to show location settings dialog
        // For now, return success
        return Result.success(Unit)
    }

    /**
     * Helper method to get current user data
     */
    private suspend fun getCurrentUser(): User? =
        try {
            val response = ApiClient.getInstance().apiService.getCurrentUser(token ?: "")
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("LocationRepositoryImpl", "Failed to get current user", e)
            null
        }
}
