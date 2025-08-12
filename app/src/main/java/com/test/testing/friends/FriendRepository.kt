package com.test.testing.friends

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * Repository to manage friend relationships in Firebase
 */
class FriendRepository {
    private val TAG = "FriendRepository"
    private val auth = FirebaseAuth.getInstance()
    private val database = Firebase.database("https://locshare-93a7b-default-rtdb.europe-west1.firebasedatabase.app/")
    private val friendsRef = database.getReference("friendships")
    private val usersRef = database.getReference("users")

    /**
     * Send a friend request to another user
     * Uses sequential writes with proper error handling instead of complex transactions
     */
    fun sendFriendRequest(
        targetUserId: String,
        onComplete: (success: Boolean, message: String) -> Unit,
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onComplete(false, "You need to be logged in")
            return
        }

        // Check if user exists
        usersRef
            .child(targetUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onComplete(false, "User does not exist")
                    return@addOnSuccessListener
                }

                val currentUserId = currentUser.uid

                // Don't allow friending yourself
                if (currentUserId == targetUserId) {
                    onComplete(false, "You cannot add yourself as a friend")
                    return@addOnSuccessListener
                }

                // Check if friendship already exists
                friendsRef
                    .child(currentUserId)
                    .child(targetUserId)
                    .get()
                    .addOnSuccessListener { existingSnapshot ->
                        if (existingSnapshot.exists()) {
                            onComplete(false, "Friend request already exists")
                            return@addOnSuccessListener
                        }

                        val timestamp = System.currentTimeMillis()

                        // Create friendship data for requester (outgoing)
                        val outgoingFriendship =
                            mapOf(
                                "status" to FriendshipStatus.PENDING.name,
                                "direction" to FriendshipDirection.OUTGOING.name,
                                "requestedAt" to timestamp,
                                "updatedAt" to timestamp,
                            )

                        // Create friendship data for recipient (incoming)
                        val incomingFriendship =
                            mapOf(
                                "status" to FriendshipStatus.PENDING.name,
                                "direction" to FriendshipDirection.INCOMING.name,
                                "requestedAt" to timestamp,
                                "updatedAt" to timestamp,
                            )

                        // First, set the requester's outgoing friendship
                        friendsRef
                            .child(currentUserId)
                            .child(targetUserId)
                            .setValue(outgoingFriendship)
                            .addOnSuccessListener {
                                Log.d(TAG, "Outgoing friendship created successfully")

                                // Then set the recipient's incoming friendship
                                friendsRef
                                    .child(targetUserId)
                                    .child(currentUserId)
                                    .setValue(incomingFriendship)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Friend request sent successfully")
                                        onComplete(true, "Friend request sent")
                                    }.addOnFailureListener { e ->
                                        Log.e(TAG, "Failed to create incoming friendship", e)
                                        // Rollback: remove the outgoing friendship
                                        friendsRef.child(currentUserId).child(targetUserId).removeValue()
                                        onComplete(false, "Failed to send friend request: ${e.message}")
                                    }
                            }.addOnFailureListener { e ->
                                Log.e(TAG, "Failed to create outgoing friendship", e)
                                onComplete(false, "Failed to send friend request: ${e.message}")
                            }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error checking existing friendship", e)
                        onComplete(false, "Failed to check existing friendship: ${e.message}")
                    }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error checking if user exists", e)
                onComplete(false, "Failed to check if user exists: ${e.message}")
            }
    }

    /**
     * Get all friends and pending friends for the current user
     */
    fun getFriendships(onResult: (friendships: List<FriendshipModel>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(emptyList())
            return
        }

        val currentUserId = currentUser.uid
        friendsRef.child(currentUserId).addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val friendships = mutableListOf<FriendshipModel>()

                    for (friendSnapshot in snapshot.children) {
                        val friendId = friendSnapshot.key ?: continue
                        val status = friendSnapshot.child("status").getValue(String::class.java) ?: continue
                        val direction = friendSnapshot.child("direction").getValue(String::class.java) ?: continue
                        val requestedAt = friendSnapshot.child("requestedAt").getValue(Long::class.java) ?: 0L
                        val updatedAt = friendSnapshot.child("updatedAt").getValue(Long::class.java) ?: 0L

                        // Get friend's display name from users collection
                        usersRef
                            .child(friendId)
                            .child("displayName")
                            .get()
                            .addOnSuccessListener { userSnapshot ->
                                val displayName = userSnapshot.getValue(String::class.java) ?: "Unknown"

                                val friendship =
                                    FriendshipModel(
                                        userId = friendId,
                                        displayName = displayName,
                                        status = FriendshipStatus.valueOf(status),
                                        direction = FriendshipDirection.valueOf(direction),
                                        requestedAt = requestedAt,
                                        updatedAt = updatedAt,
                                    )

                                friendships.add(friendship)

                                // Once we've processed all friends, return the list
                                if (friendships.size == snapshot.childrenCount.toInt()) {
                                    onResult(friendships)
                                }
                            }.addOnFailureListener {
                                Log.e(TAG, "Error getting user display name", it)
                                // Still add the friendship even without display name
                                val friendship =
                                    FriendshipModel(
                                        userId = friendId,
                                        displayName = "Unknown",
                                        status = FriendshipStatus.valueOf(status),
                                        direction = FriendshipDirection.valueOf(direction),
                                        requestedAt = requestedAt,
                                        updatedAt = updatedAt,
                                    )

                                friendships.add(friendship)

                                // Once we've processed all friends, return the list
                                if (friendships.size == snapshot.childrenCount.toInt()) {
                                    onResult(friendships)
                                }
                            }
                    }

                    // Handle case of no friends
                    if (snapshot.childrenCount == 0L) {
                        onResult(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error getting friendships", error.toException())
                    onResult(emptyList())
                }
            },
        )
    }

    /**
     * Accept a friend request
     * Uses sequential writes with proper error handling
     */
    fun acceptFriendRequest(
        friendId: String,
        onComplete: (success: Boolean, message: String) -> Unit,
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onComplete(false, "You need to be logged in")
            return
        }

        val currentUserId = currentUser.uid
        val timestamp = System.currentTimeMillis()

        // First, update the recipient's friendship (INCOMING -> ACCEPTED)
        val recipientUpdates =
            mapOf(
                "status" to FriendshipStatus.ACCEPTED.name,
                "updatedAt" to timestamp,
            )

        friendsRef
            .child(currentUserId)
            .child(friendId)
            .updateChildren(recipientUpdates)
            .addOnSuccessListener {
                Log.d(TAG, "Recipient friendship updated successfully")

                // Then update the requester's friendship (OUTGOING -> ACCEPTED)
                val requesterUpdates =
                    mapOf(
                        "status" to FriendshipStatus.ACCEPTED.name,
                        "updatedAt" to timestamp,
                    )

                friendsRef
                    .child(friendId)
                    .child(currentUserId)
                    .updateChildren(requesterUpdates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Friend request accepted successfully")
                        onComplete(true, "Friend request accepted")
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update requester friendship", e)
                        // Rollback: revert recipient friendship back to PENDING
                        val rollbackUpdates =
                            mapOf(
                                "status" to FriendshipStatus.PENDING.name,
                                "updatedAt" to timestamp,
                            )
                        friendsRef.child(currentUserId).child(friendId).updateChildren(rollbackUpdates)
                        onComplete(false, "Failed to accept friend request: ${e.message}")
                    }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to update recipient friendship", e)
                onComplete(false, "Failed to accept friend request: ${e.message}")
            }
    }

    /**
     * Get the current user's ID
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Get display name for a user ID
     */
    fun getUserDisplayName(
        userId: String,
        onResult: (displayName: String) -> Unit,
    ) {
        usersRef
            .child(userId)
            .child("displayName")
            .get()
            .addOnSuccessListener { snapshot ->
                val displayName = snapshot.getValue(String::class.java) ?: "Unknown User"
                onResult(displayName)
            }.addOnFailureListener {
                onResult("Unknown User")
            }
    }
}
