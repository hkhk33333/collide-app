package com.test.testing

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.initialize

class LocShareApp : Application() {
    companion object {
        private const val TAG = "LocShareApp"
    }

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
