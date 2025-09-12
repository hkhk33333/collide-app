package com.test.testing.di

import com.test.testing.discord.api.ApiService
import com.test.testing.discord.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideResilienceInterceptor(): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
                val request = chain.request()

                // Add resilience headers
                val newRequest =
                    request
                        .newBuilder()
                        .addHeader("Connection", "keep-alive")
                        .addHeader("Accept", "application/json")
                        .build()

                val startTime = System.currentTimeMillis()

                return try {
                    val response = chain.proceed(newRequest)
                    val duration = System.currentTimeMillis() - startTime

                    // Log slow requests
                    if (duration > 5000) { // 5 seconds
                        android.util.Log.w("NetworkResilience", "Slow request: ${request.url} took ${duration}ms")
                    }

                    response
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    android.util.Log.e("NetworkResilience", "Request failed: ${request.url} after ${duration}ms", e)
                    throw e
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        resilienceInterceptor: Interceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(resilienceInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(AppConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}
