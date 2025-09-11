package com.test.testing.discord.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.test.testing.discord.models.DiscordTokenResponse
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Data class to hold all parts of the token response together for easy serialization.
data class StoredToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long, // Store the absolute expiry timestamp
)

class SecureTokenStorage(
    context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("discord_secure_auth", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "discord_auth_key"
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"

        private const val ENCRYPTED_TOKEN_KEY = "encrypted_token_data"
        private const val ENCRYPTION_IV_KEY = "encryption_iv"
    }

    private val keyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

    private fun getOrCreateSecretKey(): SecretKey = (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: generateSecretKey()

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEYSTORE)

        val spec =
            KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(ENCRYPTION_BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setRandomizedEncryptionRequired(true) // Recommended for GCM
                .setKeySize(256)
                .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun saveTokens(tokenResponse: DiscordTokenResponse) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        // Calculate when the token will expire in system milliseconds
        val expiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)

        val storedToken =
            StoredToken(
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresAt = expiresAt,
            )
        val json = gson.toJson(storedToken)

        val encryptedData = cipher.doFinal(json.toByteArray(Charset.defaultCharset()))
        val encryptedDataBase64 = Base64.encodeToString(encryptedData, Base64.DEFAULT)
        val ivBase64 = Base64.encodeToString(cipher.iv, Base64.DEFAULT)

        prefs.edit {
            putString(ENCRYPTED_TOKEN_KEY, encryptedDataBase64)
            putString(ENCRYPTION_IV_KEY, ivBase64)
        }
    }

    fun getTokens(): StoredToken? {
        val encryptedDataBase64 = prefs.getString(ENCRYPTED_TOKEN_KEY, null)
        val ivBase64 = prefs.getString(ENCRYPTION_IV_KEY, null)

        if (encryptedDataBase64 == null || ivBase64 == null) return null

        return try {
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)
            val encryptedData = Base64.decode(encryptedDataBase64, Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv) // GCM tag length is 128 bits
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

            val decryptedData = cipher.doFinal(encryptedData)
            val json = String(decryptedData, Charset.defaultCharset())
            gson.fromJson(json, StoredToken::class.java)
        } catch (e: Exception) {
            // If decryption fails, clear the invalid tokens
            Log.e("SecureTokenStorage", "Failed to decrypt tokens", e)
            clearTokens()
            null
        }
    }

    fun clearTokens() {
        prefs.edit {
            remove(ENCRYPTED_TOKEN_KEY)
            remove(ENCRYPTION_IV_KEY)
        }
        // Also remove the key from the keystore for complete cleanup
        keyStore.deleteEntry(KEY_ALIAS)
    }
}
