package com.test.testing.discord.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.test.testing.discord.location.LocationManager
import com.test.testing.discord.models.User
import com.test.testing.discord.ui.BorderedCircleCropTransformation
import com.test.testing.discord.ui.CoilImageLoader
import com.test.testing.discord.viewmodels.ApiViewModel
import kotlin.math.roundToInt

@Composable
fun MapScreen(
    apiViewModel: ApiViewModel,
    locationManager: LocationManager,
) {
    val uiState by apiViewModel.uiState.collectAsState()
    val currentUserLocation by locationManager.locationUpdates.collectAsState()
    var hasInitiallyCentered by remember { mutableStateOf(false) }

    // THE FIX: Add a state to track if the map has finished loading.
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
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationManager.locationPermissionGranted),
            // THE FIX: Use the onMapLoaded callback to update our state.
            onMapLoaded = {
                isMapLoaded = true
            },
        ) {
            uiState.users.forEach { user ->
                user.location?.let { location ->
                    val position = LatLng(location.latitude, location.longitude)
                    UserMarker(
                        user = user,
                        position = position,
                    )
                }
            }
        }

        // Combined UI for both manual and automatic refresh indicators
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.isLoading) {
                // Show spinner if loading
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                // Show refresh button if not loading
                IconButton(onClick = { apiViewModel.manualRefresh() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Users",
                    )
                }
            }
        }

        // Error message display
        uiState.error?.let { errorMessage ->
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(8.dp),
                        ).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
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

    LaunchedEffect(user.duser.avatarUrl) {
        val imageLoader = CoilImageLoader.getInstance(context)
        val request =
            ImageRequest
                .Builder(context)
                .data(user.duser.avatarUrl)
                .transformations(BorderedCircleCropTransformation())
                .size(128, 128)
                .allowHardware(false)
                .target { drawable ->
                    val bitmap = drawable.toBitmap()
                    bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
                }.build()
        imageLoader.enqueue(request)
    }

    bitmapDescriptor?.let {
        Marker(
            state = MarkerState(position = position),
            title = user.duser.username,
            snippet = "Accuracy: ${user.location?.accuracy?.roundToInt()}m",
            icon = it,
            anchor = Offset(0.5f, 0.5f),
        )
    }
}
