package com.test.testing.discord.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.test.testing.R
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.auth.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "New FCM Token: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val authManager = AuthManager.getInstance(this)
        if (!authManager.isAuthenticated.value) {
            // Store token to send after login if needed
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authToken = "Bearer ${authManager.token.value}"
                val userResponse = ApiClient.apiService.getCurrentUser(authToken)
                if (userResponse.isSuccessful && userResponse.body() != null) {
                    val currentUser = userResponse.body()!!
                    val updatedUser = currentUser.copy(pushToken = token)
                    ApiClient.apiService.updateCurrentUser(authToken, updatedUser)
                    Log.d("FCMService", "FCM token sent to server successfully.")
                }
            } catch (e: Exception) {
                Log.e("FCMService", "Error sending FCM token to server", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let {
            Log.d("FCMService", "Message Notification Body: ${it.body}")
            showNotification(it.title, it.body)
        }
    }

    private fun showNotification(
        title: String?,
        body: String?,
    ) {
        val channelId = "nearby_notifications"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    "Nearby User Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            notificationManager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat
                .Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(1, notification)
    }
}
