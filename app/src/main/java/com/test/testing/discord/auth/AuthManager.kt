package com.test.testing.discord.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.test.testing.discord.api.ApiClient
import com.test.testing.discord.models.DiscordTokenResponse
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
    context: Context,
) {
    // Use the new secure storage
    private val secureTokenStorage = SecureTokenStorage(context)
    private var codeVerifier: String? = null

    // StateFlows to hold in-memory state
    private val _isAuthenticated = MutableStateFlow<Boolean>(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token = _token.asStateFlow()

    // Holds the complete token information
    private var currentToken: StoredToken? = null

    init {
        // Load tokens from secure storage on initialization
        currentToken = secureTokenStorage.getTokens()
        if (isTokenValid()) {
            _token.value = currentToken?.accessToken
            _isAuthenticated.value = true
        } else {
            // If the token is expired, clear it
            secureTokenStorage.clearTokens()
        }
    }

    private fun isTokenValid(): Boolean {
        val expiresAt = currentToken?.expiresAt ?: return false
        // Check if the token expires in the next 60 seconds to be safe
        return expiresAt > System.currentTimeMillis() - 60000
    }

    fun login(activityContext: Context) {
        codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier!!)

        val authUrl =
            Uri
                .parse("https://discord.com/api/oauth2/authorize")
                .buildUpon()
                .appendQueryParameter("client_id", com.test.testing.discord.config.AppConfig.discordClientId)
                .appendQueryParameter("redirect_uri", com.test.testing.discord.config.AppConfig.CALLBACK_URL)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "identify guilds")
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
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
                        redirectUri = com.test.testing.discord.config.AppConfig.CALLBACK_URL,
                    )
                val response = ApiClient.apiService.exchangeCodeForToken(request)
                if (response.isSuccessful && response.body() != null) {
                    val tokenResponse = response.body()!!
                    withContext(Dispatchers.Main) {
                        saveToken(tokenResponse)
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

    private fun saveToken(tokenResponse: DiscordTokenResponse) {
        secureTokenStorage.saveTokens(tokenResponse)
        currentToken = secureTokenStorage.getTokens() // Reload from storage to have all computed fields

        _token.value = currentToken?.accessToken
        _isAuthenticated.value = true
    }

    fun logout(onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokenVal = _token.value
                if (tokenVal != null) {
                    val currentTokenHeader = "Bearer $tokenVal"
                    ApiClient.apiService.revokeToken(currentTokenHeader)
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Failed to revoke token", e)
            } finally {
                withContext(Dispatchers.Main) {
                    secureTokenStorage.clearTokens() // Use secure storage to clear
                    currentToken = null
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
            get() = INSTANCE ?: error("AuthManager not initialized")
    }
}
