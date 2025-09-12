package com.test.testing.di

import com.test.testing.discord.api.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.mockito.Mockito.mock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class],
)
object TestNetworkModule {
    @Provides
    @Singleton
    fun provideMockWebServer(): MockWebServer = MockWebServer()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("http://localhost:8080")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    // Alternative: provide a mock for simpler unit tests
    @Provides
    @Singleton
    fun provideMockApiService(): ApiService = mock(ApiService::class.java)
}
