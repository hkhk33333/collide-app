package com.test.testing.discord.domain.models

/**
 * Domain model representing a Discord guild/server.
 * Contains guild-specific business logic.
 */
data class DomainGuild(
    val id: GuildId,
    val name: GuildName,
    val iconUrl: String,
    val memberCount: Int = 0,
    val isLocationSharingEnabled: Boolean = true,
) {
    /**
     * Business logic: Get display name for the guild
     */
    val displayName: String
        get() = name.value

    /**
     * Business logic: Check if guild allows location sharing
     */
    fun allowsLocationSharing(): Boolean = isLocationSharingEnabled

    /**
     * Business logic: Get avatar URL with fallback
     */
    fun getAvatarUrl(): String =
        if (iconUrl.isNotBlank()) {
            iconUrl
        } else {
            // Generate fallback avatar URL based on name
            "https://ui-avatars.com/api/?name=${name.value.take(2)}&background=random"
        }
}

/**
 * Value object for Guild name with validation
 */
@JvmInline
value class GuildName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Guild name cannot be blank" }
        require(value.length <= 100) { "Guild name too long" }
    }

    override fun toString(): String = value
}
