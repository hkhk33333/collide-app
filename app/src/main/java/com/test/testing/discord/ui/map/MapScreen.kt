package com.test.testing.discord.ui.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.request.ImageRequest
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.test.testing.BuildConfig
import com.test.testing.discord.location.LocationManager
import com.test.testing.discord.models.User
import com.test.testing.discord.ui.BorderedCircleCropTransformation
import com.test.testing.discord.ui.CoilImageLoader
import com.test.testing.discord.ui.UiEvent
import com.test.testing.discord.viewmodels.MapViewModel
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    locationManager: LocationManager,
) {
    val uiState by mapViewModel.uiState.collectAsState()
    val currentUserLocation by locationManager.locationUpdates.collectAsState()
    var hasInitiallyCentered by rememberSaveable { mutableStateOf(false) }

    // Debug logging for state changes (only in debug builds)
    LaunchedEffect(uiState) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("MapScreen", "UI state changed: $uiState, isRefreshing: ${uiState.isRefreshing}")
        }
    }

    // Track if the map has finished loading
    var isMapLoaded by remember { mutableStateOf(false) }

    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 10f)
        }

    // This effect will now only run once the map is loaded AND we have a location.
    LaunchedEffect(currentUserLocation, isMapLoaded) {
        if (currentUserLocation != null && isMapLoaded && !hasInitiallyCentered) {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(
                        LatLng(currentUserLocation!!.latitude, currentUserLocation!!.longitude),
                        12f,
                    ),
                ),
            )
            hasInitiallyCentered = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Use a when expression to handle the state
        when (val state = uiState) {
            is MapScreenUiState.Loading -> {
                // Show a full-screen loading indicator
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is MapScreenUiState.Error -> {
                // Show an error message
                Text(
                    text = state.message,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error,
                )

                // The refresh button and other UI can be layered on top even in error state
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isRefreshing) {
                        // Show loading indicator on the refresh button when refreshing
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        // Show refresh button when not refreshing
                        IconButton(onClick = {
                            if (BuildConfig.DEBUG) {
                                android.util.Log.d("MapScreen", "Refresh button clicked from Error state")
                            }
                            mapViewModel.onEvent(UiEvent.RefreshUsers)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Users",
                            )
                        }
                    }
                }
            }
            is MapScreenUiState.Success -> {
                // On success, display the map and markers
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = locationManager.locationPermissionGranted),
                    // Use the onMapLoaded callback to update our state
                    onMapLoaded = {
                        isMapLoaded = true
                    },
                ) {
                    state.users.forEach { user ->
                        user.location?.let { location ->
                            val position = LatLng(location.latitude, location.longitude)
                            UserMarker(
                                user = user,
                                position = position,
                            )
                        }
                    }
                }

                // The refresh button and other UI can be layered on top
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isRefreshing) {
                        // Show loading indicator on the refresh button when refreshing
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        // Show refresh button when not refreshing
                        IconButton(onClick = {
                            if (BuildConfig.DEBUG) {
                                android.util.Log.d("MapScreen", "Refresh button clicked from Success state")
                            }
                            mapViewModel.onEvent(UiEvent.RefreshUsers)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Users",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserMarker(
    user: User,
    position: LatLng,
) {
    val context = LocalContext.current
    var bitmapDescriptor by remember { mutableStateOf<BitmapDescriptor?>(null) }

    // Load and process user avatar
    LaunchedEffect(user.duser.avatarUrl) {
        try {
            val imageLoader = CoilImageLoader.getInstance(context)
            val request =
                ImageRequest
                    .Builder(context)
                    .data(user.duser.avatarUrl)
                    .transformations(BorderedCircleCropTransformation())
                    .size(96, 96) // Smaller size for map markers
                    .allowHardware(false)
                    .target { drawable ->
                        try {
                            val bitmap = drawable.toBitmap()
                            bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("MapScreen", "Failed to process user avatar bitmap", e)
                            // Fallback to default marker if image processing fails
                            bitmapDescriptor = null
                        }
                    }.build()
            imageLoader.enqueue(request)
        } catch (e: Exception) {
            Log.e("MapScreen", "Failed to load user avatar image", e)
            // If image loading fails, use default marker
            bitmapDescriptor = null
        }
    }

    Marker(
        state = remember(position) { MarkerState(position = position) },
        title = user.duser.username,
        snippet = "Accuracy: ${user.location?.accuracy?.roundToInt()}m",
        icon = bitmapDescriptor, // Use custom avatar or default marker
        anchor = Offset(0.5f, 0.45f), // Slightly above center to account for shadow
    )
}
