package com.test.testing.discord.domain.models.mappers

import com.test.testing.discord.domain.models.*
import com.test.testing.discord.models.User as DataUser

/**
 * Mapper for converting between data layer User models and domain layer DomainUser models
 */
class UserMapper(
    private val locationMapper: LocationMapper,
) {
    fun toDomain(dataUser: DataUser): DomainUser =
        DomainUser(
            id = UserId(dataUser.id),
            username = Username(dataUser.duser.username),
            avatarUrl = dataUser.duser.avatarUrl ?: "",
            location = dataUser.location?.let { locationMapper.toDomain(it) },
            isOnline = true, // Domain logic: assume online for now
            lastSeen = java.time.Instant.now(), // Domain logic: current time
            privacySettings =
                UserPrivacySettings(
                    enabledGuilds =
                        dataUser.privacy.enabledGuilds
                            .map { GuildId(it) }
                            .toSet(),
                    blockedUsers =
                        dataUser.privacy.blockedUsers
                            .map { UserId(it) }
                            .toSet(),
                    locationSharingEnabled = true, // Domain logic: default to enabled
                    nearbyNotificationsEnabled = dataUser.receiveNearbyNotifications ?: true,
                ),
        )

    fun toDomainList(dataUsers: List<DataUser>): List<DomainUser> = dataUsers.map { toDomain(it) }

    fun toData(domainUser: DomainUser): DataUser {
        // This would be used when sending data back to the server
        // For now, return a minimal implementation
        return DataUser(
            id = domainUser.id.value,
            location = domainUser.location?.let { locationMapper.toData(it) },
            duser =
                com.test.testing.discord.models.DiscordUser(
                    id = domainUser.id.value,
                    username = domainUser.username.value,
                    avatar = null, // Domain logic: avatar handling
                ),
            privacy =
                com.test.testing.discord.models.PrivacySettings(
                    enabledGuilds = domainUser.privacySettings.enabledGuilds.map { it.value },
                    blockedUsers = domainUser.privacySettings.blockedUsers.map { it.value },
                ),
            pushToken = null,
            receiveNearbyNotifications = domainUser.privacySettings.nearbyNotificationsEnabled,
            allowNearbyNotifications = true,
            nearbyNotificationDistance = 500.0,
            allowNearbyNotificationDistance = 500.0,
        )
    }

    fun toDataList(domainUsers: List<DomainUser>): List<DataUser> = domainUsers.map { toData(it) }
}
