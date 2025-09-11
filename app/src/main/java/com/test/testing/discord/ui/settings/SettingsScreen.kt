package com.test.testing.discord.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.location.LocationManager
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.User
import com.test.testing.discord.viewmodels.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    appViewModel: AppViewModel,
    locationManager: LocationManager,
) {
    val currentUser by appViewModel.currentUser.collectAsState()
    val guilds by appViewModel.guilds.collectAsState()
    val users by appViewModel.users.collectAsState()

    // Local UI state
    var selectedGuilds by remember { mutableStateOf(setOf<String>()) }
    var blockedUsers by remember { mutableStateOf(listOf<String>()) }
    var guildSearchText by remember { mutableStateOf("") }
    var userSearchText by remember { mutableStateOf("") }
    var receiveNearbyNotifications by remember { mutableStateOf(true) }
    var allowNearbyNotifications by remember { mutableStateOf(true) }
    var nearbyNotificationDistance by remember { mutableStateOf(500.0) }
    var allowNearbyNotificationDistance by remember { mutableStateOf(500.0) }

    // Sync local state with data from the view model
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            selectedGuilds = user.privacy.enabledGuilds.toSet()
            blockedUsers = user.privacy.blockedUsers
            receiveNearbyNotifications = user.receiveNearbyNotifications ?: true
            allowNearbyNotifications = user.allowNearbyNotifications ?: true
            nearbyNotificationDistance = user.nearbyNotificationDistance ?: 500.0
            allowNearbyNotificationDistance = user.allowNearbyNotificationDistance ?: 500.0
        }
    }

    val saveSettings = {
        currentUser?.let { user: com.test.testing.discord.models.User ->
            val updatedUser =
                user.copy(
                    privacy =
                        user.privacy.copy(
                            enabledGuilds = selectedGuilds.toList(),
                            blockedUsers = blockedUsers,
                        ),
                    receiveNearbyNotifications = receiveNearbyNotifications,
                    allowNearbyNotifications = allowNearbyNotifications,
                    nearbyNotificationDistance = nearbyNotificationDistance,
                    allowNearbyNotificationDistance = allowNearbyNotificationDistance,
                )
            appViewModel.updateCurrentUser(updatedUser) {}
        }
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            SectionHeader("Discord Servers")
            ServerListView(
                guilds = guilds,
                selectedGuilds = selectedGuilds,
                searchText = guildSearchText,
                onSearchTextChanged = { guildSearchText = it },
                onToggle = { guildId, isEnabled ->
                    selectedGuilds =
                        if (isEnabled) {
                            selectedGuilds + guildId
                        } else {
                            selectedGuilds - guildId
                        }
                    saveSettings()
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SectionHeader("Users")
            UserListView(
                users = users,
                currentUser = currentUser,
                blockedUsers = blockedUsers,
                searchText = userSearchText,
                onSearchTextChanged = { userSearchText = it },
                onToggleBlock = { userId ->
                    blockedUsers =
                        if (blockedUsers.contains(userId)) {
                            blockedUsers - userId
                        } else {
                            blockedUsers + userId
                        }
                    saveSettings()
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SectionHeader("Location Settings")
            LocationSettingsView(locationManager = locationManager)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SectionHeader("Nearby Notifications")
            NotificationSettingsView(
                receiveNearbyNotifications = receiveNearbyNotifications,
                onReceiveNearbyChanged = {
                    receiveNearbyNotifications = it
                    saveSettings()
                },
                allowNearbyNotifications = allowNearbyNotifications,
                onAllowNearbyChanged = {
                    allowNearbyNotifications = it
                    saveSettings()
                },
                nearbyNotificationDistance = nearbyNotificationDistance,
                onNearbyDistanceChanged = {
                    nearbyNotificationDistance = it
                    saveSettings()
                },
                allowNearbyNotificationDistance = allowNearbyNotificationDistance,
                onAllowNearbyDistanceChanged = {
                    allowNearbyNotificationDistance = it
                    saveSettings()
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SectionHeader("Account")
            AccountActionsView(appViewModel = appViewModel)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- All sub-composables below for a clean, structured screen ---

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun ServerListView(
    guilds: List<Guild>,
    selectedGuilds: Set<String>,
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    val filteredGuilds = guilds.filter { it.name.contains(searchText, ignoreCase = true) }

    FilterableListView(
        items = filteredGuilds,
        searchText = searchText,
        onSearchTextChanged = onSearchTextChanged,
        searchPlaceholder = "Search servers...",
    ) { guild ->
        ListItem(
            headlineContent = { Text(guild.name) },
            leadingContent = {
                // THE FIX IS HERE
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = guild.iconUrl,
                        contentDescription = guild.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
            trailingContent = {
                Switch(
                    checked = selectedGuilds.contains(guild.id),
                    onCheckedChange = { onToggle(guild.id, it) },
                )
            },
        )
    }
}

@Composable
fun UserListView(
    users: List<User>,
    currentUser: User?,
    blockedUsers: List<String>,
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onToggleBlock: (String) -> Unit,
) {
    val filteredUsers = users.filter { it.duser.username.contains(searchText, ignoreCase = true) }

    FilterableListView(
        items = filteredUsers,
        searchText = searchText,
        onSearchTextChanged = onSearchTextChanged,
        searchPlaceholder = "Search users...",
    ) { user ->
        ListItem(
            headlineContent = { Text(user.duser.username) },
            supportingContent = { if (user.id == currentUser?.id) Text("You") },
            leadingContent = {
                // THE FIX IS HERE
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = user.duser.avatarUrl,
                        contentDescription = user.duser.username,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
            trailingContent = {
                if (user.id != currentUser?.id) {
                    val isBlocked = blockedUsers.contains(user.id)
                    Button(
                        onClick = { onToggleBlock(user.id) },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = if (isBlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text(if (isBlocked) "Unblock" else "Block")
                    }
                }
            },
        )
    }
}

@Composable
fun LocationSettingsView(locationManager: LocationManager) {
    val intervalOptions =
        mapOf(
            30000L to "30 seconds",
            60000L to "1 minute",
            300000L to "5 minutes",
            900000L to "15 minutes",
        )
    val movementOptions =
        mapOf(
            100f to "100m",
            500f to "500m",
            1000f to "1km",
            5000f to "5km",
        )
    val privacyOptions =
        mapOf(
            0f to "Full Accuracy",
            1000f to "1km",
            5000f to "5km",
            10000f to "10km",
        )

    Column {
        ListItem(
            headlineContent = { Text("Background Updates") },
            trailingContent = {
                Switch(
                    checked = locationManager.backgroundUpdatesEnabled,
                    onCheckedChange = { locationManager.updateBackgroundUpdates(it) },
                )
            },
        )
        MenuPicker(
            label = "Update Interval",
            options = intervalOptions,
            selectedValue = locationManager.updateInterval,
            onValueChange = { locationManager.updateInterval(it) },
        )
        MenuPicker(
            label = "Minimum Movement",
            options = movementOptions,
            selectedValue = locationManager.minimumMovementThreshold,
            onValueChange = { locationManager.updateMinimumMovement(it) },
        )
        MenuPicker(
            label = "Location Privacy",
            options = privacyOptions,
            selectedValue = locationManager.desiredAccuracy,
            onValueChange = { locationManager.updateDesiredAccuracy(it) },
        )
    }
}

@Composable
fun NotificationSettingsView(
    receiveNearbyNotifications: Boolean,
    onReceiveNearbyChanged: (Boolean) -> Unit,
    allowNearbyNotifications: Boolean,
    onAllowNearbyChanged: (Boolean) -> Unit,
    nearbyNotificationDistance: Double,
    onNearbyDistanceChanged: (Double) -> Unit,
    allowNearbyNotificationDistance: Double,
    onAllowNearbyDistanceChanged: (Double) -> Unit,
) {
    val distanceOptions =
        mapOf(
            50.0 to "50 meters",
            100.0 to "100 meters",
            250.0 to "250 meters",
            500.0 to "500 meters",
            1000.0 to "1 kilometer",
        )
    Column {
        ListItem(
            headlineContent = { Text("Notify me when I'm near someone") },
            trailingContent = { Switch(checked = receiveNearbyNotifications, onCheckedChange = onReceiveNearbyChanged) },
        )
        if (receiveNearbyNotifications) {
            MenuPicker(
                label = "Notification Distance",
                options = distanceOptions,
                selectedValue = nearbyNotificationDistance,
                onValueChange = onNearbyDistanceChanged,
            )
        }
        ListItem(
            headlineContent = { Text("Notify others when they are near me") },
            trailingContent = { Switch(checked = allowNearbyNotifications, onCheckedChange = onAllowNearbyChanged) },
        )
        if (allowNearbyNotifications) {
            MenuPicker(
                label = "Notify Others Within",
                options = distanceOptions,
                selectedValue = allowNearbyNotificationDistance,
                onValueChange = onAllowNearbyDistanceChanged,
            )
        }
    }
}

@Composable
fun AccountActionsView(appViewModel: AppViewModel) {
    val context = LocalContext.current
    val authManager = AuthManager.getInstance(context)
    val coroutineScope = rememberCoroutineScope()

    ListItem(
        headlineContent = { Text("Logout", color = MaterialTheme.colorScheme.error) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error) },
        modifier =
            Modifier.clickable {
                authManager.logout {
                    coroutineScope.launch { appViewModel.logout() }
                }
            },
    )
    ListItem(
        headlineContent = { Text("Delete My Data", color = MaterialTheme.colorScheme.error) },
        trailingContent = { Icon(Icons.Default.Delete, contentDescription = "Delete Data", tint = MaterialTheme.colorScheme.error) },
        modifier =
            Modifier.clickable {
                appViewModel.deleteUserData {
                    authManager.logout {
                        coroutineScope.launch { appViewModel.logout() }
                    }
                }
            },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MenuPicker(
    label: String,
    options: Map<T, String>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Box {
                Text(
                    text = options[selectedValue] ?: "",
                    modifier =
                        Modifier
                            .clickable { expanded = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { (value, displayText) ->
                        DropdownMenuItem(
                            text = { Text(displayText) },
                            onClick = {
                                onValueChange(value)
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}
