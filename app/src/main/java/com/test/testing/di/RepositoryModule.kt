package com.test.testing.di

import com.test.testing.discord.api.ApiService
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.cache.CacheManager
import com.test.testing.discord.data.repository.LocationRepositoryImpl
import com.test.testing.discord.data.repository.UserRepositoryImpl
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
    @Provides
    @Singleton
    fun provideUserRepository(
        apiService: ApiService,
        cacheManager: CacheManager,
        networkResilience: NetworkResilience,
    ): UserRepository = UserRepositoryImpl(apiService, cacheManager, networkResilience)

    @Provides
    @Singleton
    fun provideLocationRepository(
        apiService: com.test.testing.discord.api.ApiService,
        locationManager: LocationManager,
        networkResilience: NetworkResilience,
        authManager: AuthManager,
    ): LocationRepository = LocationRepositoryImpl(apiService, locationManager, networkResilience, authManager)
}
