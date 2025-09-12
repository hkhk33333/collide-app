package com.test.testing.di

import com.test.testing.discord.models.DomainEventBus
import com.test.testing.discord.models.SimpleEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    @Singleton
    fun provideDomainEventBus(): DomainEventBus = SimpleEventBus()
}
