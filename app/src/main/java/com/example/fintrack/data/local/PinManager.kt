package com.example.fintrack.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Handles master PIN hashing, salting, authentication verification,
 * and lockout protection against brute force.
 */
class PinManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "fintrack_security_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_IS_PIN_SET = "is_pin_set"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
        private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds lockout
    }

    val isPinSet: Boolean
        get() = prefs.getBoolean(KEY_IS_PIN_SET, false)

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPin(pin, salt)

        prefs.edit()
            .putString(KEY_PIN_SALT, saltBase64)
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_IS_PIN_SET, true)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        return true
    }

    fun verifyPin(pin: String): VerificationResult {
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)

        if (now < lockoutUntil) {
            val remainingSecs = ((lockoutUntil - now) / 1000).toInt() + 1
            return VerificationResult.LockedOut(remainingSecs)
        }

        val saltBase64 = prefs.getString(KEY_PIN_SALT, null) ?: return VerificationResult.Failed(0)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return VerificationResult.Failed(0)

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val computedHash = hashPin(pin, salt)

        return if (computedHash == storedHash) {
            // Reset failed counter
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            VerificationResult.Success
        } else {
            val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            if (failed >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                val lockUntil = now + LOCKOUT_DURATION_MS
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCKOUT_UNTIL, lockUntil)
                    .apply()
                VerificationResult.LockedOut((LOCKOUT_DURATION_MS / 1000).toInt())
            } else {
                prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failed).apply()
                VerificationResult.Failed(MAX_ATTEMPTS_BEFORE_LOCKOUT - failed)
            }
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val hashBytes = md.digest(pin.toByteArray())
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    sealed class VerificationResult {
        object Success : VerificationResult()
        data class Failed(val attemptsRemaining: Int) : VerificationResult()
        data class LockedOut(val waitSeconds: Int) : VerificationResult()
    }
}
