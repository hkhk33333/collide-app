package com.test.testing.di

import android.content.Context
import android.content.SharedPreferences
import com.test.testing.discord.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun provideLocationManager(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences,
    ): LocationManager = LocationManager(context, sharedPreferences)
}
