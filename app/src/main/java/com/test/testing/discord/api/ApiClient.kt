package com.test.testing.discord.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient private constructor() {
    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private val resilienceInterceptor =
        object : okhttp3.Interceptor {
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

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(resilienceInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit
            .Builder()
            .baseUrl(com.test.testing.discord.config.AppConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    companion object {
        @Volatile
        private var instance: ApiClient? = null

        fun getInstance(): ApiClient =
            instance ?: synchronized(this) {
                instance ?: ApiClient().also { instance = it }
            }
    }
}
