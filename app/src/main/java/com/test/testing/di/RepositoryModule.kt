package com.test.testing.di

import com.test.testing.discord.api.ApiService
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.cache.CacheManager
import com.test.testing.discord.data.datasources.UserLocalDataSource
import com.test.testing.discord.data.datasources.UserRemoteDataSource
import com.test.testing.discord.data.repository.LocationRepositoryImpl
import com.test.testing.discord.data.repository.UserRepositoryImpl
import com.test.testing.discord.domain.models.mappers.GuildMapper
import com.test.testing.discord.domain.models.mappers.LocationMapper
import com.test.testing.discord.domain.models.mappers.UserMapper
import com.test.testing.discord.domain.repository.LocationRepository
import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.location.LocationManager
import com.test.testing.discord.network.NetworkResilience
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Data Sources
    @Provides
    @Singleton
    fun provideUserRemoteDataSource(
        apiService: ApiService,
        networkResilience: NetworkResilience,
    ): UserRemoteDataSource = UserRemoteDataSource(apiService, networkResilience)

    @Provides
    @Singleton
    fun provideUserLocalDataSource(cacheManager: CacheManager): UserLocalDataSource = UserLocalDataSource(cacheManager)

    // Mappers
    @Provides
    @Singleton
    fun provideUserMapper(locationMapper: LocationMapper): UserMapper = UserMapper(locationMapper)

    @Provides
    @Singleton
    fun provideLocationMapper(): LocationMapper = LocationMapper()

    @Provides
    @Singleton
    fun provideGuildMapper(): GuildMapper = GuildMapper()

    // Repositories
    @Provides
    @Singleton
    fun provideUserRepository(
        remoteDataSource: UserRemoteDataSource,
        localDataSource: UserLocalDataSource,
        userMapper: UserMapper,
        guildMapper: GuildMapper,
    ): UserRepository =
        UserRepositoryImpl(
            remoteDataSource,
            localDataSource,
            userMapper,
            guildMapper,
        )

    @Provides
    @Singleton
    fun provideLocationRepository(
        apiService: com.test.testing.discord.api.ApiService,
        locationManager: LocationManager,
        networkResilience: NetworkResilience,
        authManager: AuthManager,
    ): LocationRepository = LocationRepositoryImpl(apiService, locationManager, networkResilience, authManager)
}
