package com.test.testing

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltAndroidApp
class LocShareApp : Application() {
    companion object {
        private const val TAG = "LocShareApp"
    }

    // TODO Delete when https://github.com/google/dagger/issues/3601 is resolved.
    @Inject
    @ApplicationContext
    lateinit var context: Context

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
