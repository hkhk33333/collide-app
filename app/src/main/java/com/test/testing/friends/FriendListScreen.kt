package com.test.testing.friends

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FriendListScreen(
    friendRepository: FriendRepository,
    onNavigateToAddFriend: () -> Unit,
    onNavigateBack: () -> Unit,
    onViewFriendLocation: (String) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<FriendshipModel>?>(null) }

    // Load friends on initial composition
    LaunchedEffect(true) {
        friendRepository.getFriendships { friendships ->
            friends = friendships
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            Text(
                text = "Friends",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display current user information
            friendRepository.getCurrentUserId()?.let { userId ->
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "User",
                                modifier = Modifier.padding(end = 8.dp),
                            )

                            Column {
                                Text(
                                    text = "Your User ID:",
                                    style = MaterialTheme.typography.bodySmall,
                                )

                                Text(
                                    text = userId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )

                                Text(
                                    text = "(Share this with friends who want to add you)",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            // Add Friend Button
            Button(
                onClick = onNavigateToAddFriend,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add friend",
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Add Friend")
            }

            if (friends == null) {
                // Loading state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading friends...",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else if (friends!!.isEmpty()) {
                // No friends state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "You don't have any friends yet.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Tap the + button to add friends.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                // Friends list - now organized by direction and status

                // Incoming requests section (can be accepted)
                val incomingRequests =
                    friends!!.filter {
                        it.status == FriendshipStatus.PENDING && it.direction == FriendshipDirection.INCOMING
                    }

                if (incomingRequests.isNotEmpty()) {
                    Text(
                        text = "Incoming Friend Requests",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(0.3f)
                                .fillMaxWidth(),
                    ) {
                        items(incomingRequests) { friend ->
                            IncomingFriendRequestItem(
                                friend = friend,
                                onAccept = {
                                    friendRepository.acceptFriendRequest(friend.userId) { success, message ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                },
                            )
                            Divider()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Outgoing requests section (waiting for approval)
                val outgoingRequests =
                    friends!!.filter {
                        it.status == FriendshipStatus.PENDING && it.direction == FriendshipDirection.OUTGOING
                    }

                if (outgoingRequests.isNotEmpty()) {
                    Text(
                        text = "Pending Requests",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(0.3f)
                                .fillMaxWidth(),
                    ) {
                        items(outgoingRequests) { friend ->
                            OutgoingFriendRequestItem(friend = friend)
                            Divider()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Show message if no pending requests
                if (incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                    Text(
                        text = "No pending friend requests",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                // Accepted friends section
                Text(
                    text = "Your Friends",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                val acceptedFriends = friends!!.filter { it.status == FriendshipStatus.ACCEPTED }
                if (acceptedFriends.isEmpty()) {
                    Text(
                        text = "You don't have any accepted friends yet",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(0.4f)
                                .fillMaxWidth(),
                    ) {
                        items(acceptedFriends) { friend ->
                            FriendItem(friend = friend, onViewLocation = onViewFriendLocation)
                            Divider()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Back to Map")
            }
        }
    }
}

@Composable
fun IncomingFriendRequestItem(
    friend: FriendshipModel,
    onAccept: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = friend.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "Wants to be your friend • ${formatDate(friend.requestedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = onAccept,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Accept",
                modifier = Modifier.padding(end = 4.dp),
            )
            Text("Accept")
        }
    }
}

@Composable
fun OutgoingFriendRequestItem(friend: FriendshipModel) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = friend.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "Waiting for approval • ${formatDate(friend.requestedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
fun FriendItem(
    friend: FriendshipModel,
    onViewLocation: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = friend.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "Friends since: ${formatDate(friend.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Button(
            onClick = { onViewLocation(friend.userId) },
        ) {
            Text("View Location")
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
