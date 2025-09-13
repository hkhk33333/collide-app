package com.test.testing.discord.domain.models.mappers

import com.test.testing.discord.domain.models.*
import com.test.testing.discord.models.Guild as DataGuild

/**
 * Mapper for converting between data layer Guild models and domain layer DomainGuild models
 */
class GuildMapper {
    fun toDomain(dataGuild: DataGuild): DomainGuild =
        DomainGuild(
            id = GuildId(dataGuild.id),
            name = GuildName(dataGuild.name),
            iconUrl = dataGuild.iconUrl ?: "",
            memberCount = 0, // Domain logic: not provided in data model
            isLocationSharingEnabled = true, // Domain logic: default to enabled
        )

    fun toDomainList(dataGuilds: List<DataGuild>): List<DomainGuild> = dataGuilds.map { toDomain(it) }

    fun toData(domainGuild: DomainGuild): DataGuild =
        DataGuild(
            id = domainGuild.id.value,
            name = domainGuild.name.value,
            icon = if (domainGuild.iconUrl.isNotBlank()) domainGuild.iconUrl else null,
        )

    fun toDataList(domainGuilds: List<DomainGuild>): List<DataGuild> = domainGuilds.map { toData(it) }
}
