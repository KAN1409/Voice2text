package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStorage(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_transcriber_keys", Context.MODE_PRIVATE)

    private val keyAlias = "VoiceTranscriberKeyStoreAlias"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    init {
        createKeyStoreKeyIfNeeded()
    }

    private fun createKeyStoreKeyIfNeeded() {
        try {
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = keyStore.getKey(keyAlias, null) as? SecretKey ?: return plainText
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    private fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val secretKey = keyStore.getKey(keyAlias, null) as? SecretKey ?: return encryptedBase64
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, combined, 0, 12)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val plainBytes = cipher.doFinal(combined, 12, combined.size - 12)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedBase64
        }
    }

    fun saveGeminiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API, encrypt(key.trim())).apply()
    }

    fun getGeminiKey(): String {
        val storedEncrypted = prefs.getString(KEY_GEMINI_API, null)
        if (!storedEncrypted.isNullOrEmpty()) {
            val decrypted = decrypt(storedEncrypted)
            if (decrypted.isNotEmpty() && !decrypted.equals("MY_GEMINI_API_KEY", ignoreCase = true)) {
                return decrypted
            }
        }

        // Fallback to BuildConfig if provided
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotEmpty() && !buildKey.equals("MY_GEMINI_API_KEY", ignoreCase = true)) {
                buildKey
            } else ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun saveGroqKey(key: String) {
        prefs.edit().putString(KEY_GROQ_API, encrypt(key.trim())).apply()
    }

    fun getGroqKey(): String {
        val storedEncrypted = prefs.getString(KEY_GROQ_API, null)
        if (!storedEncrypted.isNullOrEmpty()) {
            val decrypted = decrypt(storedEncrypted)
            if (decrypted.isNotEmpty() && !decrypted.equals("MY_GROQ_API_KEY", ignoreCase = true)) {
                return decrypted
            }
        }

        return try {
            val buildKey = BuildConfig.GROQ_API_KEY
            if (buildKey.isNotEmpty() && !buildKey.equals("MY_GROQ_API_KEY", ignoreCase = true)) {
                buildKey
            } else ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun getMaskedGeminiKey(): String = maskKey(getGeminiKey())

    fun getMaskedGroqKey(): String = maskKey(getGroqKey())

    private fun maskKey(key: String): String {
        if (key.isBlank()) return "Not configured"
        if (key.length <= 4) return "••••"
        val suffix = key.takeLast(4)
        return "••••••••••••••••$suffix"
    }

    companion object {
        private const val KEY_GEMINI_API = "encrypted_gemini_api_key"
        private const val KEY_GROQ_API = "encrypted_groq_api_key"
    }
}
