package com.test.testing.discord.api

import com.test.testing.discord.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private val okHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor)
            .build()

    private val retrofit: Retrofit =
        Retrofit
            .Builder()
            .baseUrl(Constants.BACKEND_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
