package com.test.testing.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "AuthViewModel"

    var currentUser by mutableStateOf<FirebaseUser?>(null)
    var authState by mutableStateOf(AuthState.INITIAL)
    var errorMessage by mutableStateOf<String?>(null)
    
    init {
        // Check if user is already signed in
        currentUser = auth.currentUser
        authState = if (currentUser != null) AuthState.AUTHENTICATED else AuthState.UNAUTHENTICATED
        Log.d(TAG, "Initial auth state: $authState, user: ${currentUser?.email}")
    }
    
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Email and password cannot be empty"
            return
        }
        
        authState = AuthState.LOADING
        errorMessage = null
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signIn:success")
                    currentUser = auth.currentUser
                    authState = AuthState.AUTHENTICATED
                } else {
                    Log.w(TAG, "signIn:failure", task.exception)
                    errorMessage = task.exception?.message ?: "Authentication failed"
                    authState = AuthState.UNAUTHENTICATED
                }
            }
    }
    
    fun signUp(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Email and password cannot be empty"
            return
        }
        
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }
        
        authState = AuthState.LOADING
        errorMessage = null
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUser:success")
                    currentUser = auth.currentUser
                    
                    // Update user profile with display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    
                    currentUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Log.d(TAG, "User profile updated with name: $displayName")
                            } else {
                                Log.w(TAG, "Failed to update profile", profileTask.exception)
                            }
                            authState = AuthState.AUTHENTICATED
                        }
                } else {
                    Log.w(TAG, "createUser:failure", task.exception)
                    errorMessage = task.exception?.message ?: "Registration failed"
                    authState = AuthState.UNAUTHENTICATED
                }
            }
    }
    
    fun signOut() {
        auth.signOut()
        currentUser = null
        authState = AuthState.UNAUTHENTICATED
        Log.d(TAG, "User signed out")
    }
}

enum class AuthState {
    INITIAL,
    LOADING,
    AUTHENTICATED,
    UNAUTHENTICATED
} 