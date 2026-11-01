package com.example.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 16

    private const val PREFS_NAME = "vaultkeep_security_prefs"
    private const val KEY_VAULT_ENABLED = "vault_enabled"
    private const val KEY_VAULT_HASH = "vault_password_hash"
    private const val KEY_VAULT_SALT = "vault_password_salt"
    private const val KEY_VAULT_HINT = "vault_password_hint"

    private var activeSessionPassphrase: String? = null
    private var lastUnlockTime: Long = 0L
    private var autoLockTimeoutMs: Long = 5 * 60 * 1000L // 5 minutes default

    fun isVaultConfigured(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_VAULT_ENABLED, false) && prefs.getString(KEY_VAULT_HASH, null) != null
    }

    fun getPasswordHint(context: Context): String {
        return getPrefs(context).getString(KEY_VAULT_HINT, "") ?: ""
    }

    fun setupVault(context: Context, passphrase: String, hint: String = ""): Boolean {
        if (passphrase.length < 4) return false
        val salt = generateRandomBytes(SALT_LENGTH)
        val hash = hashPassphrase(passphrase, salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

        getPrefs(context).edit()
            .putBoolean(KEY_VAULT_ENABLED, true)
            .putString(KEY_VAULT_HASH, hash)
            .putString(KEY_VAULT_SALT, saltBase64)
            .putString(KEY_VAULT_HINT, hint)
            .apply()

        activeSessionPassphrase = passphrase
        lastUnlockTime = System.currentTimeMillis()
        return true
    }

    fun unlockVault(context: Context, passphrase: String): Boolean {
        val prefs = getPrefs(context)
        val storedHash = prefs.getString(KEY_VAULT_HASH, null) ?: return false
        val storedSaltBase64 = prefs.getString(KEY_VAULT_SALT, null) ?: return false
        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)

        val computedHash = hashPassphrase(passphrase, salt)
        return if (computedHash == storedHash) {
            activeSessionPassphrase = passphrase
            lastUnlockTime = System.currentTimeMillis()
            true
        } else {
            false
        }
    }

    fun isVaultUnlocked(): Boolean {
        if (activeSessionPassphrase == null) return false
        if (autoLockTimeoutMs > 0 && System.currentTimeMillis() - lastUnlockTime > autoLockTimeoutMs) {
            lockVault()
            return false
        }
        return true
    }

    fun refreshSession() {
        if (activeSessionPassphrase != null) {
            lastUnlockTime = System.currentTimeMillis()
        }
    }

    fun lockVault() {
        activeSessionPassphrase = null
        lastUnlockTime = 0L
    }

    fun getActivePassphrase(): String? {
        return if (isVaultUnlocked()) activeSessionPassphrase else null
    }

    fun setAutoLockTimeout(timeoutMs: Long) {
        autoLockTimeoutMs = timeoutMs
    }

    fun getAutoLockTimeout(): Long = autoLockTimeoutMs

    fun encrypt(plainText: String, passphrase: String): String {
        val salt = generateRandomBytes(SALT_LENGTH)
        val iv = generateRandomBytes(IV_LENGTH)
        val secretKey = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val saltStr = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivStr = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherStr = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)

        return "VK1:$saltStr:$ivStr:$cipherStr"
    }

    fun decrypt(encryptedPayload: String, passphrase: String): Result<String> {
        return try {
            if (!encryptedPayload.startsWith("VK1:")) {
                // If not in VK1 format, return as-is
                return Result.success(encryptedPayload)
            }
            val parts = encryptedPayload.split(":")
            if (parts.size != 4) {
                return Result.failure(IllegalArgumentException("Malformed encrypted payload"))
            }
            val salt = Base64.decode(parts[1], Base64.NO_WRAP)
            val iv = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[3], Base64.NO_WRAP)

            val secretKey = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plainBytes = cipher.doFinal(cipherBytes)

            Result.success(String(plainBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKey {
        val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(DERIVATION_ALGORITHM)
        val keyBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    private fun hashPassphrase(passphrase: String, salt: ByteArray): String {
        val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, 256)
        val factory = SecretKeyFactory.getInstance(DERIVATION_ALGORITHM)
        val hashBytes = factory.generateSecret(keySpec).encoded
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    private fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
