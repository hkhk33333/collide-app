package com.test.testing.discord.ui

import android.content.Context
import coil.ImageLoader
import okhttp3.OkHttpClient

// This object provides a custom ImageLoader instance for Coil.
object CoilImageLoader {
    private var instance: ImageLoader? = null

    fun getInstance(context: Context): ImageLoader {
        // Return existing instance if already created (singleton pattern)
        instance?.let { return it }

        // Build a new instance
        return ImageLoader
            .Builder(context)
            .okHttpClient {
                // Configure OkHttpClient to follow redirects, which is needed for Discord's default avatars.
                OkHttpClient
                    .Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
            }.build()
            .also {
                // Store the new instance
                instance = it
            }
    }
}
