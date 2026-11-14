package com.comfortfriend.app.data.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user's Groq API key encrypted with an AES-256-GCM key that lives inside the
 * Android Keystore (hardware-backed where available). Only ciphertext touches disk.
 *
 * Limitation: the app itself must decrypt the key to call Groq, so a sufficiently capable
 * attacker on a compromised/rooted device could still use it. The key is never logged.
 */
class SecureKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("cf_secure", Context.MODE_PRIVATE)

    fun hasKey(): Boolean = prefs.contains(PREF_KEY)

    fun save(apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val enc = cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))
        val packed = Base64.encodeToString(iv + enc, Base64.NO_WRAP)
        prefs.edit().putString(PREF_KEY, packed).apply()
    }

    fun get(): String? {
        val packed = prefs.getString(PREF_KEY, null) ?: return null
        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, IV_LEN)
            val data = bytes.copyOfRange(IV_LEN, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (e: Exception) {
            // Key invalidated or data corrupted: treat as no key so the user can re-enter it.
            clear()
            null
        }
    }

    fun clear() {
        prefs.edit().remove(PREF_KEY).apply()
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "comfortfriend_groq_key"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val PREF_KEY = "groq_key_enc"
        const val IV_LEN = 12
    }
}
