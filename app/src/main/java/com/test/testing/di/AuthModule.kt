package com.test.testing.di

import com.test.testing.discord.api.ApiService
import com.test.testing.discord.auth.AuthManager
import com.test.testing.discord.auth.SecureTokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideAuthManager(
        secureTokenStorage: SecureTokenStorage,
        apiService: ApiService,
    ): AuthManager =
        AuthManager(secureTokenStorage).apply {
            setApiService(apiService)
        }
}
