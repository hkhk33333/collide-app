package com.test.testing.discord.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.gms.location.*
import com.test.testing.discord.viewmodels.ApiViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.test.testing.discord.models.Location as ApiLocation

class LocationManager private constructor(
    private val context: Context,
    private val apiViewModel: ApiViewModel,
) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("location_settings", Context.MODE_PRIVATE)
    private var isUpdatingLocation = false

    private val _locationUpdates = MutableStateFlow<android.location.Location?>(null)
    val locationUpdates = _locationUpdates.asStateFlow()

    // --- Persisted Settings ---
    var backgroundUpdatesEnabled by mutableStateOf(prefs.getBoolean("backgroundUpdatesEnabled", true))
        private set
    var updateInterval by mutableLongStateOf(prefs.getLong("updateInterval", 60000L)) // Default 1 minute
        private set
    var minimumMovementThreshold by mutableFloatStateOf(prefs.getFloat("minimumMovementThreshold", 1000f)) // Default 1km
        private set
    var desiredAccuracy by mutableFloatStateOf(prefs.getFloat("desiredAccuracy", 0f)) // Default Full Accuracy
        private set

    fun updateBackgroundUpdates(enabled: Boolean) {
        backgroundUpdatesEnabled = enabled
        prefs.edit { putBoolean("backgroundUpdatesEnabled", enabled) }
        handleSettingsChange()
    }

    fun updateInterval(interval: Long) {
        updateInterval = interval
        prefs.edit { putLong("updateInterval", interval) }
        handleSettingsChange()
    }

    fun updateMinimumMovement(threshold: Float) {
        minimumMovementThreshold = threshold
        prefs.edit { putFloat("minimumMovementThreshold", threshold) }
        handleSettingsChange()
    }

    fun updateDesiredAccuracy(accuracy: Float) {
        desiredAccuracy = accuracy
        prefs.edit { putFloat("desiredAccuracy", accuracy) }
        // No need to restart location updates for this, it's used when sending data
        // Trigger a single refresh to reflect privacy change
        triggerUserRefresh()
    }
    // --- End Persisted Settings ---

    val locationPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val backgroundPermissionGranted: Boolean
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

    private val locationCallback =
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    _locationUpdates.value = location
                    sendLocationUpdate(location)
                }
            }
        }

    private fun handleSettingsChange() {
        if (isUpdatingLocation) {
            stopLocationUpdates()
            startLocationUpdates()
        }
    }

    fun startLocationUpdates() {
        if (!locationPermissionGranted || isUpdatingLocation) return

        try {
            val locationRequest =
                LocationRequest
                    .Builder(Priority.PRIORITY_HIGH_ACCURACY, updateInterval)
                    .setMinUpdateIntervalMillis(updateInterval / 2)
                    .setMinUpdateDistanceMeters(minimumMovementThreshold)
                    .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isUpdatingLocation = true
            Log.d("LocationManager", "Started location updates with interval: $updateInterval ms, threshold: $minimumMovementThreshold m")

            if (backgroundUpdatesEnabled && backgroundPermissionGranted) {
                val serviceIntent = Intent(context, DiscordLocationService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        } catch (e: SecurityException) {
            Log.e("LocationManager", "SecurityException: Cannot start location updates", e)
        }
    }

    fun stopLocationUpdates() {
        if (!isUpdatingLocation) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isUpdatingLocation = false
        Log.d("LocationManager", "Stopped location updates")

        val serviceIntent = Intent(context, DiscordLocationService::class.java)
        context.stopService(serviceIntent)
    }

    private fun sendLocationUpdate(location: android.location.Location) {
        // This function now runs on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            apiViewModel.currentUser.value?.let { currentUser ->
                val newLocation =
                    ApiLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy.toDouble(),
                        desiredAccuracy = desiredAccuracy.toDouble(),
                        lastUpdated = System.currentTimeMillis().toDouble(),
                    )

                val updatedUser = currentUser.copy(location = newLocation)
                // We don't need the onComplete lambda here for a background task
                apiViewModel.updateCurrentUser(updatedUser) {}
            }
        }
    }

    // New function to trigger a one-off user list refresh
    fun triggerUserRefresh() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("LocationManager", "Triggering manual user refresh.")
            apiViewModel.refreshUsers()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LocationManager? = null

        fun getInstance(
            context: Context,
            apiViewModel: ApiViewModel,
        ): LocationManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationManager(context.applicationContext, apiViewModel).also { INSTANCE = it }
            }

        val instance: LocationManager
            get() = INSTANCE ?: throw IllegalStateException("LocationManager not initialized")
    }
}
