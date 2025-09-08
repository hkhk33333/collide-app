package com.test.testing.discord.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.test.testing.R
import kotlinx.coroutines.*

class DiscordLocationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        Log.d("DiscordLocationService", "Service started")

        serviceScope.launch {
            LocationManager.instance.locationUpdates.collect { location ->
                location?.let {
                    Log.d("DiscordLocationService", "Collected location update in service")
                    // The LocationManager is already responsible for sending the update to the server
                }
            }
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Discord Location Service",
                    NotificationManager.IMPORTANCE_LOW,
                )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Location Sharing Active")
            .setContentText("Sharing your location with your Discord communities.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d("DiscordLocationService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "DiscordLocationServiceChannel"
    }
}
