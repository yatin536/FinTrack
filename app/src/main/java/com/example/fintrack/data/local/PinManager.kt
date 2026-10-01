package com.example.fintrack.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.fintrack.data.model.User
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
        get() = isPinSet(User.DEFAULT_USER_ID)

    var isBiometricEnabled: Boolean
        get() = isBiometricEnabled(User.DEFAULT_USER_ID)
        set(value) = setBiometricEnabled(value, User.DEFAULT_USER_ID)

    fun isPinSet(userId: String = User.DEFAULT_USER_ID): Boolean {
        val key = if (userId == User.DEFAULT_USER_ID) KEY_IS_PIN_SET else "${KEY_IS_PIN_SET}_$userId"
        return prefs.getBoolean(key, false) || (userId == User.DEFAULT_USER_ID && prefs.getBoolean(KEY_IS_PIN_SET, false))
    }

    fun isBiometricEnabled(userId: String = User.DEFAULT_USER_ID): Boolean {
        val key = if (userId == User.DEFAULT_USER_ID) KEY_BIOMETRIC_ENABLED else "${KEY_BIOMETRIC_ENABLED}_$userId"
        return prefs.getBoolean(key, false)
    }

    fun setBiometricEnabled(enabled: Boolean, userId: String = User.DEFAULT_USER_ID) {
        val key = if (userId == User.DEFAULT_USER_ID) KEY_BIOMETRIC_ENABLED else "${KEY_BIOMETRIC_ENABLED}_$userId"
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun setPin(pin: String, userId: String = User.DEFAULT_USER_ID): Boolean {
        if (pin.length < 4) return false

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPin(pin, salt)

        val hashKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_HASH else "${KEY_PIN_HASH}_$userId"
        val saltKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_SALT else "${KEY_PIN_SALT}_$userId"
        val isSetKey = if (userId == User.DEFAULT_USER_ID) KEY_IS_PIN_SET else "${KEY_IS_PIN_SET}_$userId"
        val failedKey = if (userId == User.DEFAULT_USER_ID) KEY_FAILED_ATTEMPTS else "${KEY_FAILED_ATTEMPTS}_$userId"
        val lockoutKey = if (userId == User.DEFAULT_USER_ID) KEY_LOCKOUT_UNTIL else "${KEY_LOCKOUT_UNTIL}_$userId"

        prefs.edit()
            .putString(saltKey, saltBase64)
            .putString(hashKey, hash)
            .putBoolean(isSetKey, true)
            .putInt(failedKey, 0)
            .putLong(lockoutKey, 0L)
            .apply()

        // Also update default key if primary user for maximum compatibility
        if (userId == User.DEFAULT_USER_ID) {
            prefs.edit().putBoolean(KEY_IS_PIN_SET, true).apply()
        }
        return true
    }

    fun verifyPin(pin: String, userId: String = User.DEFAULT_USER_ID): VerificationResult {
        val now = System.currentTimeMillis()
        val lockoutKey = if (userId == User.DEFAULT_USER_ID) KEY_LOCKOUT_UNTIL else "${KEY_LOCKOUT_UNTIL}_$userId"
        val failedKey = if (userId == User.DEFAULT_USER_ID) KEY_FAILED_ATTEMPTS else "${KEY_FAILED_ATTEMPTS}_$userId"
        val saltKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_SALT else "${KEY_PIN_SALT}_$userId"
        val hashKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_HASH else "${KEY_PIN_HASH}_$userId"

        val lockoutUntil = prefs.getLong(lockoutKey, 0L)

        if (now < lockoutUntil) {
            val remainingSecs = ((lockoutUntil - now) / 1000).toInt() + 1
            return VerificationResult.LockedOut(remainingSecs)
        }

        var saltBase64 = prefs.getString(saltKey, null)
        var storedHash = prefs.getString(hashKey, null)

        // Fallback for default user
        if (userId != User.DEFAULT_USER_ID && (saltBase64 == null || storedHash == null)) {
            // Check fallback to default user key only if this is the only user
            saltBase64 = prefs.getString(KEY_PIN_SALT, null)
            storedHash = prefs.getString(KEY_PIN_HASH, null)
        }

        if (saltBase64 == null || storedHash == null) {
            return VerificationResult.Failed(0)
        }

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val computedHash = hashPin(pin, salt)

        return if (computedHash == storedHash) {
            // Reset failed counter
            prefs.edit()
                .putInt(failedKey, 0)
                .putLong(lockoutKey, 0L)
                .apply()
            VerificationResult.Success
        } else {
            val failed = prefs.getInt(failedKey, 0) + 1
            if (failed >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                val lockUntil = now + LOCKOUT_DURATION_MS
                prefs.edit()
                    .putInt(failedKey, 0)
                    .putLong(lockoutKey, lockUntil)
                    .apply()
                VerificationResult.LockedOut((LOCKOUT_DURATION_MS / 1000).toInt())
            } else {
                prefs.edit().putInt(failedKey, failed).apply()
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
