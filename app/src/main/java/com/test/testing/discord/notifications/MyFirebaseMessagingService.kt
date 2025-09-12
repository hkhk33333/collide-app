package com.test.testing.discord.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.test.testing.MainActivity
import com.test.testing.R

/**
 * Firebase Cloud Messaging service for handling push notifications
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID_NEARBY = "nearby_notifications"
        private const val CHANNEL_ID_GENERAL = "general_notifications"
        private const val NOTIFICATION_ID_NEARBY = 1001
        private const val NOTIFICATION_ID_GENERAL = 1002
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showGeneralNotification(it.title ?: "Discord Location", it.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // Send token to your backend server
        sendRegistrationToServer(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val title = data["title"] ?: "Discord Location"
        val message = data["message"] ?: ""

        when (type) {
            "nearby_user" -> {
                val userId = data["userId"]
                val distance = data["distance"]
                val username = data["username"] ?: "Someone"

                val nearbyMessage =
                    if (distance != null) {
                        "$username is $distance away from you!"
                    } else {
                        "$username is nearby!"
                    }

                showNearbyNotification(title, nearbyMessage, userId)
            }

            "guild_activity" -> {
                showGeneralNotification(title, message)
            }

            else -> {
                showGeneralNotification(title, message)
            }
        }
    }

    private fun showNearbyNotification(
        title: String,
        message: String,
        userId: String? = null,
    ) {
        createNotificationChannel(CHANNEL_ID_NEARBY, "Nearby Users", NotificationManager.IMPORTANCE_HIGH)

        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("notification_type", "nearby")
                putExtra("user_id", userId)
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID_NEARBY)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 250, 250, 250)) // Vibration pattern
                .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_NEARBY, notification)
    }

    private fun showGeneralNotification(
        title: String,
        message: String,
    ) {
        createNotificationChannel(CHANNEL_ID_GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT)

        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("notification_type", "general")
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                1,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID_GENERAL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_GENERAL, notification)
    }

    private fun createNotificationChannel(
        channelId: String,
        channelName: String,
        importance: Int,
    ) {
        val channel =
            NotificationChannel(channelId, channelName, importance).apply {
                description = "Notifications for $channelName"
                enableVibration(true)

                if (channelId == CHANNEL_ID_NEARBY) {
                    vibrationPattern = longArrayOf(0, 250, 250, 250)
                    setShowBadge(true)
                }
            }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun sendRegistrationToServer(token: String) {
        // The FCM token should be sent to your backend server here.
        // You can use your ApiService to send the FCM token to your backend
        // This token can be associated with the current user for targeted notifications

        Log.d(TAG, "Sending FCM token to server: $token")

        // Example implementation:
        /*
        val currentUser = AuthManager.getInstance(this).currentUser
        if (currentUser != null) {
            // Send token to backend
            // ApiService.updateUserPushToken(token)
        }
         */
    }

    /**
     * Handle notification click actions
     */
    fun handleNotificationClick(intent: Intent) {
        val notificationType = intent.getStringExtra("notification_type")

        when (notificationType) {
            "nearby" -> {
                // Handle nearby user notification click
                val userId = intent.getStringExtra("user_id")
                // Navigate to map with user highlighted or show user details
                Log.d(TAG, "Nearby notification clicked for user: $userId")
            }

            "general" -> {
                // Handle general notification click
                Log.d(TAG, "General notification clicked")
            }
        }
    }
}
