package com.test.testing.friends

/**
 * Status of a friendship between users
 */
enum class FriendshipStatus {
    PENDING, // Friend request sent but not accepted
    ACCEPTED, // Both users are friends
    REJECTED, // Friend request was rejected
}

/**
 * Direction of a friendship request from the current user's perspective
 */
enum class FriendshipDirection {
    OUTGOING, // I sent the request to them
    INCOMING, // They sent the request to me
}

/**
 * Model for friendship data
 */
data class FriendshipModel(
    val userId: String = "", // The user ID of the friend
    val displayName: String = "", // Display name of the friend
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val direction: FriendshipDirection = FriendshipDirection.OUTGOING, // Direction of the request
    val requestedAt: Long = System.currentTimeMillis(), // When the request was sent
    val updatedAt: Long = System.currentTimeMillis(), // Last status update
)

/**
 * Model for storing friendship in Firebase
 * This is the structure we'll use in the database
 */
data class FirebaseFriendship(
    val status: String = FriendshipStatus.PENDING.name,
    val direction: String = FriendshipDirection.OUTGOING.name, // Direction of the request
    val requestedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
