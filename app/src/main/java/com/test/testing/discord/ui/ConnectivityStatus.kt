package com.test.testing.discord.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * Connectivity status indicator that shows network status
 */
@Composable
fun ConnectivityStatus(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val connectivityStatus by produceConnectivityState(context)

    AnimatedVisibility(
        visible = connectivityStatus != ConnectivityState.Available,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier,
    ) {
        ConnectivityStatusBanner(connectivityStatus)
    }
}

/**
 * Banner showing connectivity status
 */
@Composable
private fun ConnectivityStatusBanner(status: ConnectivityState) {
    val (backgroundColor, text, iconTint) =
        when (status) {
            ConnectivityState.Unavailable -> {
                Triple(
                    Color(0xFFD32F2F), // Red
                    "No internet connection",
                    Color.White,
                )
            }

            ConnectivityState.Available -> {
                Triple(
                    Color(0xFF4CAF50), // Green
                    "Back online",
                    Color.White,
                )
            }

            ConnectivityState.Lost -> {
                Triple(
                    Color(0xFFFF9800), // Orange
                    "Connection lost",
                    Color.White,
                )
            }
        }

    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Produces connectivity state as a composable state
 */
@Composable
private fun produceConnectivityState(context: Context): State<ConnectivityState> =
    produceState(initialValue = ConnectivityState.Available) {
        connectivityFlow(context).collectLatest { state ->
            value = state
        }
    }

/**
 * Flow that emits connectivity state changes
 */
private fun connectivityFlow(context: Context) =
    callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(ConnectivityState.Available)
                }

                override fun onLost(network: Network) {
                    trySend(ConnectivityState.Lost)
                }

                override fun onUnavailable() {
                    trySend(ConnectivityState.Unavailable)
                }
            }

        val request =
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        val isConnected = checkCurrentConnectivity(context)
        trySend(if (isConnected) ConnectivityState.Available else ConnectivityState.Unavailable)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

/**
 * Check current connectivity status
 */
private fun checkCurrentConnectivity(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

/**
 * Connectivity state enum
 */
enum class ConnectivityState {
    Available,
    Unavailable,
    Lost,
}

/**
 * Composable that observes connectivity and shows appropriate UI
 */
@Composable
fun ConnectivityAware(
    modifier: Modifier = Modifier,
    offlineContent: @Composable () -> Unit = { DefaultOfflineIndicator() },
    onlineContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val connectivityStatus by produceConnectivityState(context)

    when (connectivityStatus) {
        ConnectivityState.Available -> {
            onlineContent()
        }

        ConnectivityState.Unavailable, ConnectivityState.Lost -> {
            Box(modifier = modifier) {
                onlineContent()
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                ) {
                    offlineContent()
                }
            }
        }
    }
}

/**
 * Default offline indicator
 */
@Composable
private fun DefaultOfflineIndicator() {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Offline - Showing cached data",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
