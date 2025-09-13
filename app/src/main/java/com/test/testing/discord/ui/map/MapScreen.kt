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
import com.test.testing.discord.viewmodels.MapViewModel
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    locationManager: LocationManager,
) {
    val state by mapViewModel.state.collectAsState()
    val currentUserLocation by locationManager.locationUpdates.collectAsState()
    var hasInitiallyCentered by rememberSaveable { mutableStateOf(false) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 10f)
        }

    HandleMapEffects(mapViewModel)
    HandleMapStateLogging(state)
    HandleMapCentering(currentUserLocation, isMapLoaded, hasInitiallyCentered, cameraPositionState) { hasInitiallyCentered = true }

    MapScreenContent(
        state = state,
        cameraPositionState = cameraPositionState,
        locationManager = locationManager,
        mapViewModel = mapViewModel,
        onMapLoaded = { isMapLoaded = true },
    )
}

@Composable
private fun HandleMapEffects(mapViewModel: MapViewModel) {
    LaunchedEffect(mapViewModel) {
        mapViewModel.effects.collect { effect ->
            when (effect) {
                is MapEffect.ShowSnackbar -> {
                    Log.d("MapScreen", "Show snackbar: ${effect.message}")
                }

                is MapEffect.NavigateToUser -> {
                    Log.d("MapScreen", "Navigate to user: ${effect.userId}")
                }

                is MapEffect.RequestLocationPermission -> {
                    Log.d("MapScreen", "Request location permission")
                }
            }
        }
    }
}

@Composable
private fun HandleMapStateLogging(state: MapScreenState) {
    LaunchedEffect(state) {
        if (BuildConfig.DEBUG) {
            val isRefreshing =
                when (state) {
                    is MapScreenState.Content -> state.isRefreshing
                    is MapScreenState.Error -> state.isRefreshing
                    MapScreenState.Loading -> false
                }
            android.util.Log.d("MapScreen", "UI state changed: $state, isRefreshing: $isRefreshing")
        }
    }
}

@Composable
private fun HandleMapCentering(
    currentUserLocation: android.location.Location?,
    isMapLoaded: Boolean,
    hasInitiallyCentered: Boolean,
    cameraPositionState: CameraPositionState,
    onCentered: () -> Unit,
) {
    LaunchedEffect(currentUserLocation, isMapLoaded) {
        if (currentUserLocation != null && isMapLoaded && !hasInitiallyCentered) {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(
                        LatLng(currentUserLocation.latitude, currentUserLocation.longitude),
                        12f,
                    ),
                ),
            )
            onCentered()
        }
    }
}

@Composable
private fun MapScreenContent(
    state: MapScreenState,
    cameraPositionState: CameraPositionState,
    locationManager: LocationManager,
    mapViewModel: MapViewModel,
    onMapLoaded: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is MapScreenState.Loading -> LoadingContent()
            is MapScreenState.Error -> ErrorContent(state, mapViewModel)
            is MapScreenState.Content -> SuccessContent(state, cameraPositionState, locationManager, mapViewModel, onMapLoaded)
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ErrorContent(
    state: MapScreenState.Error,
    mapViewModel: MapViewModel,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = state.message,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.error,
        )

        RefreshButtonOverlay(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = state.isRefreshing,
            onRefreshClick = {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("MapScreen", "Refresh button clicked from Error state")
                }
                mapViewModel.processIntent(MapIntent.RefreshUsers)
            },
        )
    }
}

@Composable
private fun SuccessContent(
    state: MapScreenState.Content,
    cameraPositionState: CameraPositionState,
    locationManager: LocationManager,
    mapViewModel: MapViewModel,
    onMapLoaded: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationManager.locationPermissionGranted),
            onMapLoaded = onMapLoaded,
        ) {
            state.users.forEach { user ->
                user.location?.let { location ->
                    val position = LatLng(location.latitude, location.longitude)
                    UserMarker(
                        user = user,
                        position = position,
                        onClick = { mapViewModel.processIntent(MapIntent.UserSelected(user.id)) },
                    )
                }
            }
        }

        RefreshButtonOverlay(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = state.isRefreshing,
            onRefreshClick = {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("MapScreen", "Refresh button clicked from Content state")
                }
                mapViewModel.processIntent(MapIntent.RefreshUsers)
            },
        )
    }
}

@Composable
private fun RefreshButtonOverlay(
    modifier: Modifier,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .padding(top = 16.dp)
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Users",
                )
            }
        }
    }
}

@Composable
fun UserMarker(
    user: User,
    position: LatLng,
    onClick: () -> Unit = {},
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

    val markerState = remember { MarkerState(position = position) }

    Marker(
        state = markerState,
        title = user.duser.username,
        snippet = "Accuracy: ${user.location?.accuracy?.roundToInt()}m",
        icon = bitmapDescriptor, // Use custom avatar or default marker
        anchor = Offset(0.5f, 0.45f), // Slightly above center to account for shadow
        onClick = {
            onClick()
            false // Don't consume the click to allow default behavior
        },
    )
}
