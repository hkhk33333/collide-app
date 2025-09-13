package com.test.testing.discord.domain.models.mappers

import com.test.testing.discord.domain.models.*
import com.test.testing.discord.models.Location as DataLocation

/**
 * Mapper for converting between data layer Location models and domain layer DomainLocation models
 */
class LocationMapper {
    fun toDomain(dataLocation: DataLocation): DomainLocation =
        DomainLocation(
            latitude = Latitude(dataLocation.latitude),
            longitude = Longitude(dataLocation.longitude),
            accuracy = Distance(dataLocation.accuracy),
            timestamp = java.time.Instant.ofEpochMilli(dataLocation.lastUpdated.toLong()),
        )

    fun toData(domainLocation: DomainLocation): DataLocation =
        DataLocation(
            latitude = domainLocation.latitude.value,
            longitude = domainLocation.longitude.value,
            accuracy = domainLocation.accuracy.meters,
            desiredAccuracy = domainLocation.accuracy.meters,
            lastUpdated = domainLocation.timestamp.toEpochMilli().toDouble(),
        )
}
