package com.test.testing

import android.app.Application
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

class LocShareApp : Application() {
    private val TAG = "LocShareApp"

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            Log.d(TAG, "Initializing Firebase")
            Firebase.initialize(this)
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }
}
