package com.test.testing.discord.domain.models

import java.time.Instant

/**
 * Domain model representing a user in the system.
 * Contains business logic and domain-specific behavior.
 */
data class DomainUser(
    val id: UserId,
    val username: Username,
    val avatarUrl: String,
    val location: DomainLocation?,
    val isOnline: Boolean,
    val lastSeen: Instant,
    val privacySettings: UserPrivacySettings,
) {
    /**
     * Business logic: Check if this user is nearby another user
     */
    fun isNearby(
        other: DomainUser,
        maxDistance: Distance = Distance(1000.0), // 1km default
    ): Boolean {
        if (location == null || other.location == null) return false

        val distance = location.distanceTo(other.location)
        return distance <= maxDistance
    }

    /**
     * Business logic: Check if user can be seen by current user based on privacy settings
     */
    fun isVisibleTo(currentUser: DomainUser): Boolean {
        // Check if current user is blocked
        if (privacySettings.blockedUsers.contains(currentUser.id)) {
            return false
        }

        // Check if user is in enabled guilds - for now always return true
        // In a real implementation, this would check guild membership
        return true
    }

    /**
     * Business logic: Get user's display name
     */
    val displayName: String
        get() = username.value

    /**
     * Business logic: Check if user is active (recently seen)
     */
    fun isActive(): Boolean {
        val fiveMinutesAgo = Instant.now().minusSeconds(300)
        return lastSeen.isAfter(fiveMinutesAgo)
    }
}

/**
 * Value object for User ID with validation
 */
@JvmInline
value class UserId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "User ID cannot be blank" }
        require(value.length <= 50) { "User ID too long" }
    }

    override fun toString(): String = value
}

/**
 * Value object for Username with validation
 */
@JvmInline
value class Username(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Username cannot be blank" }
        require(value.length <= 32) { "Username too long" }
        require(value.matches(Regex("^[a-zA-Z0-9_-]+$"))) { "Username contains invalid characters" }
    }

    override fun toString(): String = value
}

/**
 * Domain model for user privacy settings
 */
data class UserPrivacySettings(
    val enabledGuilds: Set<GuildId>,
    val blockedUsers: Set<UserId>,
    val locationSharingEnabled: Boolean = true,
    val nearbyNotificationsEnabled: Boolean = true,
)

/**
 * Value object for Guild ID
 */
@JvmInline
value class GuildId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Guild ID cannot be blank" }
    }

    override fun toString(): String = value
}
