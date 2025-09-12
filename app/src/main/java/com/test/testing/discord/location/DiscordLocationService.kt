package com.test.testing.discord.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
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
            // Note: Services cannot easily use Hilt injection. Consider using a different approach
            // for location updates in services, or create a service-specific LocationManager
            // For now, we'll use a simple approach
            Log.d("DiscordLocationService", "Service started - location updates handled by LocationManager")
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Discord Location Service",
                NotificationManager.IMPORTANCE_LOW,
            )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

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
