package com.test.testing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.test.testing.api.FirebaseLocationRepository
import java.util.*

class LocationService : Service() {
    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "LocationServiceChannel"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val locationRepository = FirebaseLocationRepository()
    private var timer: Timer? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.d(TAG, "LocationService started")
        startForeground(NOTIFICATION_ID, createNotification())
        startPeriodicLocationUpdates()
        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Location Service",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Keeps location sharing active in background"
                    setShowBadge(false)
                }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Location Sharing Active")
            .setContentText("Sharing location with friends")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun setupLocationCallback() {
        locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        Log.d(TAG, "Background location update: ${location.latitude}, ${location.longitude}")
                        sendLocationToServer(location)
                    }
                }
            }
    }

    private fun startPeriodicLocationUpdates() {
        try {
            // Get location every 1 minute for testing (60,000 milliseconds)
            val locationRequest =
                LocationRequest
                    .Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        60000, // 1 minute for testing
                    ).apply {
                        setMinUpdateIntervalMillis(60000) // Minimum 1 minute between updates
                        setMaxUpdateDelayMillis(120000) // Maximum 2 minutes delay
                    }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper(),
            )

            Log.d(TAG, "Started periodic location updates (every 15 minutes)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        }
    }

    private fun sendLocationToServer(location: Location) {
        locationRepository.sendLocationUpdate(location) { success, message ->
            Log.d(TAG, "Background location update result: success=$success, message=$message")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationService destroyed")
        timer?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
