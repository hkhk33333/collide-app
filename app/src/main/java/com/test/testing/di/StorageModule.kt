package com.test.testing.di

import android.content.Context
import android.content.SharedPreferences
import com.test.testing.discord.auth.SecureTokenStorage
import com.test.testing.discord.cache.CacheManager
import com.test.testing.discord.network.NetworkResilience
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideSecureTokenStorage(
        @ApplicationContext context: Context,
    ): SecureTokenStorage = SecureTokenStorage(context)

    @Provides
    @Singleton
    fun provideCacheManager(
        @ApplicationContext context: Context,
    ): CacheManager = CacheManager(context)

    @Provides
    @Singleton
    fun provideNetworkResilience(
        @ApplicationContext context: Context,
    ): NetworkResilience = NetworkResilience(context)

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
}
