package com.test.testing.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://example.com/api/" // TODO: Replace with your actual API URL
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val locationApiService: LocationApiService by lazy {
        retrofit.create(LocationApiService::class.java)
    }
} 