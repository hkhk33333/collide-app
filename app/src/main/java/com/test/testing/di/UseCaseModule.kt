package com.test.testing.di

import com.test.testing.discord.domain.repository.UserRepository
import com.test.testing.discord.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideGetCurrentUserUseCase(repository: UserRepository): GetCurrentUserUseCase = GetCurrentUserUseCase(repository)

    @Provides
    @Singleton
    fun provideGetUsersUseCase(repository: UserRepository): GetUsersUseCase = GetUsersUseCase(repository)

    @Provides
    @Singleton
    fun provideGetGuildsUseCase(repository: UserRepository): GetGuildsUseCase = GetGuildsUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateCurrentUserUseCase(repository: UserRepository): UpdateCurrentUserUseCase = UpdateCurrentUserUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteUserDataUseCase(repository: UserRepository): DeleteUserDataUseCase = DeleteUserDataUseCase(repository)
}
