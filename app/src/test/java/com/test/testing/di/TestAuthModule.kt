package com.test.testing.di

import com.test.testing.discord.auth.AuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.Mockito.mock
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AuthModule::class],
)
object TestAuthModule {
    @Provides
    @Singleton
    fun provideAuthManager(): AuthManager = mock(AuthManager::class.java)
}
