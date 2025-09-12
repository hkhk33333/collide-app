package com.test.testing.di

import android.content.Context
import com.test.testing.discord.api.ApiService
import com.test.testing.discord.auth.SecureTokenStorage
import com.test.testing.discord.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthInterceptor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ResilienceInterceptor

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (AppConfig.isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    @AuthInterceptor
    fun provideAuthInterceptor(tokenStorage: SecureTokenStorage): Interceptor =
        Interceptor { chain ->
            val token = tokenStorage.getTokens()?.accessToken
            val request =
                if (token != null) {
                    chain
                        .request()
                        .newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
            chain.proceed(request)
        }

    @Provides
    @Singleton
    @ResilienceInterceptor
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
    fun provideCache(
        @ApplicationContext context: Context,
    ): Cache {
        val cacheSize = AppConfig.CACHE_SIZE_BYTES
        return Cache(context.cacheDir, cacheSize)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        @AuthInterceptor authInterceptor: Interceptor,
        @ResilienceInterceptor resilienceInterceptor: Interceptor,
        cache: Cache,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor) // Must be first
            .addInterceptor(loggingInterceptor)
            .addInterceptor(resilienceInterceptor)
            .cache(cache)
            .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
