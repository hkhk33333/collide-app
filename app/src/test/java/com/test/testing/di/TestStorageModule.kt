package com.test.testing.di

import android.content.SharedPreferences
import com.test.testing.discord.auth.SecureTokenStorage
import com.test.testing.discord.cache.CacheManager
import com.test.testing.discord.network.NetworkResilience
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.Mockito.mock
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [StorageModule::class],
)
object TestStorageModule {
    @Provides
    @Singleton
    fun provideSecureTokenStorage(): SecureTokenStorage = mock(SecureTokenStorage::class.java)

    @Provides
    @Singleton
    fun provideCacheManager(): CacheManager = mock(CacheManager::class.java)

    @Provides
    @Singleton
    fun provideNetworkResilience(): NetworkResilience = mock(NetworkResilience::class.java)

    @Provides
    @Singleton
    fun provideSharedPreferences(): SharedPreferences = mock(SharedPreferences::class.java)
}
