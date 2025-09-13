package com.test.testing.discord.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.test.testing.discord.location.LocationManager
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.User
import com.test.testing.discord.viewmodels.UserViewModel

@Composable
fun SettingsScreen(
    userViewModel: UserViewModel,
    users: List<User>,
    locationManager: LocationManager,
) {
    val state by userViewModel.state.collectAsState()

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            ServerSettingsSection(
                state = state,
                onSaveSettings = { updatedUser ->
                    userViewModel.processIntent(UserIntent.UpdateUser(updatedUser))
                },
            )
        }

        item {
            UserSettingsSection(
                users = users,
                state = state,
                onSaveSettings = { updatedUser ->
                    userViewModel.processIntent(UserIntent.UpdateUser(updatedUser))
                },
            )
        }

        item {
            LocationSettingsSection(locationManager = locationManager)
        }

        item {
            NotificationSettingsSection(
                state = state,
                onSaveSettings = { updatedUser ->
                    userViewModel.processIntent(UserIntent.UpdateUser(updatedUser))
                },
            )
        }

        item {
            AccountSettingsSection(userViewModel = userViewModel)
        }
    }
}

@Composable
fun ServerSettingsSection(
    state: UserScreenState,
    onSaveSettings: (User) -> Unit,
) {
    // Local UI state - preserved during configuration changes
    var selectedGuilds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var guildSearchText by rememberSaveable { mutableStateOf("") }

    // Sync local state with data from the view model
    LaunchedEffect(state) {
        val currentState = state
        when (currentState) {
            is UserScreenState.Content -> {
                val currentUser = currentState.currentUser
                currentUser?.let { user ->
                    selectedGuilds = user.privacy.enabledGuilds.toSet()
                }
            }

            else -> {
                // Handle other states if needed
            }
        }
    }

    val saveSettings = {
        val currentState = state
        when (currentState) {
            is UserScreenState.Content -> {
                val currentUser = currentState.currentUser
                currentUser?.let { user ->
                    val updatedUser =
                        user.copy(
                            privacy = user.privacy.copy(enabledGuilds = selectedGuilds.toList()),
                        )
                    onSaveSettings(updatedUser)
                }
            }

            else -> {
                // Handle other states if needed
            }
        }
    }

    Column {
        SectionHeader("Discord Servers")
        ServerListView(
            guilds =
                when (val currentState = state) {
                    is UserScreenState.Content -> currentState.guilds
                    else -> emptyList()
                },
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
}

@Composable
fun UserSettingsSection(
    users: List<User>,
    state: UserScreenState,
    onSaveSettings: (User) -> Unit,
) {
    // Local UI state - preserved during configuration changes
    var blockedUsers by rememberSaveable { mutableStateOf(listOf<String>()) }
    var userSearchText by rememberSaveable { mutableStateOf("") }

    // Sync local state with data from the view model
    LaunchedEffect(state) {
        val currentState = state
        when (currentState) {
            is UserScreenState.Content -> {
                val currentUser = currentState.currentUser
                currentUser?.let { user ->
                    blockedUsers = user.privacy.blockedUsers
                }
            }

            else -> {
                // Handle other states if needed
            }
        }
    }

    val saveSettings = {
        val currentState = state
        when (currentState) {
            is UserScreenState.Content -> {
                val currentUser = currentState.currentUser
                currentUser?.let { user ->
                    val updatedUser =
                        user.copy(
                            privacy = user.privacy.copy(blockedUsers = blockedUsers),
                        )
                    onSaveSettings(updatedUser)
                }
            }

            else -> {
                // Handle other states if needed
            }
        }
    }

    Column {
        SectionHeader("Users")
        UserListView(
            users = users,
            currentUser =
                when (val currentState = state) {
                    is UserScreenState.Content -> currentState.currentUser
                    else -> null
                },
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
}

@Composable
fun LocationSettingsSection(locationManager: LocationManager) {
    Column {
        SectionHeader("Location Settings")
        LocationSettingsView(locationManager = locationManager)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun NotificationSettingsSection(
    state: UserScreenState,
    onSaveSettings: (User) -> Unit,
) {
    // Local UI state - preserved during configuration changes
    var receiveNearbyNotifications by rememberSaveable { mutableStateOf(true) }
    var allowNearbyNotifications by rememberSaveable { mutableStateOf(true) }
    var nearbyNotificationDistance by rememberSaveable { mutableDoubleStateOf(500.0) }
    var allowNearbyNotificationDistance by rememberSaveable { mutableDoubleStateOf(500.0) }

    // Sync local state with data from the view model
    LaunchedEffect(state) {
        val currentState = state
        when (currentState) {
            is UserScreenState.Content -> {
                val currentUser = currentState.currentUser
                currentUser?.let { user ->
                    receiveNearbyNotifications = user.receiveNearbyNotifications ?: true
                    allowNearbyNotifications = user.allowNearbyNotifications ?: true
                    nearbyNotificationDistance = user.nearbyNotificationDistance ?: 500.0
                    allowNearbyNotificationDistance = user.allowNearbyNotificationDistance ?: 500.0
                }
            }

            else -> {
                // Handle other states if needed
            }
        }
    }

    val saveSettings = {
        val currentState = state
        when (currentState) {
            is UserScreenState.Content -> {
                val currentUser = currentState.currentUser
                currentUser?.let { user ->
                    val updatedUser =
                        user.copy(
                            receiveNearbyNotifications = receiveNearbyNotifications,
                            allowNearbyNotifications = allowNearbyNotifications,
                            nearbyNotificationDistance = nearbyNotificationDistance,
                            allowNearbyNotificationDistance = allowNearbyNotificationDistance,
                        )
                    onSaveSettings(updatedUser)
                }
            }

            else -> {
                // Handle other states if needed
            }
        }
    }

    Column {
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
}

@Composable
fun AccountSettingsSection(userViewModel: UserViewModel) {
    Column {
        SectionHeader("Account")
        AccountActionsView(
            userViewModel = userViewModel,
            onIntent = { intent -> userViewModel.processIntent(intent) },
        )
        Spacer(modifier = Modifier.height(16.dp))
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
        keySelector = { guild: Guild -> guild.id },
        itemContent = { guild ->
            ListItem(
                headlineContent = { Text(guild.name) },
                leadingContent = {
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
        },
    )
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
        keySelector = { user: User -> user.id },
        itemContent = { user ->
            ListItem(
                headlineContent = { Text(user.duser.username) },
                supportingContent = { if (user.id == currentUser?.id) Text("You") },
                leadingContent = {
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
                                    containerColor =
                                        if (isBlocked) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                ),
                        ) {
                            Text(if (isBlocked) "Unblock" else "Block")
                        }
                    }
                },
            )
        },
    )
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
fun AccountActionsView(
    userViewModel: UserViewModel,
    onIntent: (UserIntent) -> Unit,
) {
    // AuthManager is now injected into UserViewModel, no need to manually instantiate

    ListItem(
        headlineContent = { Text("Logout", color = MaterialTheme.colorScheme.error) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error) },
        modifier =
            Modifier.clickable {
                userViewModel.logout {
                    // The change in auth state will trigger recomposition and data clearing via DomainEvent
                }
            },
    )
    ListItem(
        headlineContent = { Text("Delete My Data", color = MaterialTheme.colorScheme.error) },
        trailingContent = { Icon(Icons.Default.Delete, contentDescription = "Delete Data", tint = MaterialTheme.colorScheme.error) },
        modifier =
            Modifier.clickable {
                onIntent(UserIntent.DeleteUserData)
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
