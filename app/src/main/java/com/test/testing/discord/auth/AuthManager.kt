package com.test.testing.discord.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.test.testing.discord.Constants
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.models.TokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

class AuthManager private constructor(
    private val context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("discord_auth", Context.MODE_PRIVATE)
    private var codeVerifier: String? = null

    private val _isAuthenticated = MutableStateFlow<Boolean>(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token = _token.asStateFlow()

    init {
        val storedToken = prefs.getString(Constants.TOKEN_KEY, null)
        if (!storedToken.isNullOrBlank()) {
            _token.value = storedToken
            _isAuthenticated.value = true
        }
    }

    // CHANGE IS HERE: The function now accepts a context parameter
    fun login(activityContext: Context) {
        codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier!!)

        val authUrl =
            Uri
                .parse("https://discord.com/api/oauth2/authorize")
                .buildUpon()
                .appendQueryParameter("client_id", Constants.DISCORD_CLIENT_ID)
                .appendQueryParameter("redirect_uri", Constants.CALLBACK_URL)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "identify guilds")
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
        // CHANGE IS HERE: Use the passed-in activityContext to launch the URL
        customTabsIntent.launchUrl(activityContext, authUrl)
    }

    fun handleAuthCallback(code: String) {
        if (codeVerifier == null) {
            Log.e("AuthManager", "Code verifier is null. Cannot exchange token.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request =
                    TokenRequest(
                        code = code,
                        codeVerifier = codeVerifier!!,
                        redirectUri = Constants.CALLBACK_URL,
                    )
                val response = ApiClient.apiService.exchangeCodeForToken(request)
                if (response.isSuccessful && response.body() != null) {
                    val accessToken = response.body()!!.accessToken
                    withContext(Dispatchers.Main) {
                        saveToken(accessToken)
                    }
                } else {
                    Log.e("AuthManager", "Token exchange failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Exception during token exchange", e)
            } finally {
                codeVerifier = null
            }
        }
    }

    private fun saveToken(token: String) {
        prefs.edit().putString(Constants.TOKEN_KEY, token).apply()
        _token.value = token
        _isAuthenticated.value = true
    }

    fun logout(onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentToken = "Bearer ${_token.value}"
                ApiClient.apiService.revokeToken(currentToken)
            } catch (e: Exception) {
                Log.e("AuthManager", "Failed to revoke token", e)
            } finally {
                withContext(Dispatchers.Main) {
                    prefs.edit().remove(Constants.TOKEN_KEY).apply()
                    _token.value = null
                    _isAuthenticated.value = false
                    onComplete()
                }
            }
        }
    }

    private fun generateCodeVerifier(): String {
        val sr = SecureRandom()
        val code = ByteArray(32)
        sr.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }

        val instance: AuthManager
            get() = INSTANCE ?: throw IllegalStateException("AuthManager not initialized")
    }
}
