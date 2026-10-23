package com.example.fintrack.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.fintrack.data.model.User
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Handles master PIN hashing, salting, authentication verification,
 * and lockout protection against brute force.
 *
 * Cryptographically upgraded to PBKDF2WithHmacSHA256 (120,000 iterations)
 * with transparent auto-migration from legacy SHA-256 upon successful authentication
 * and monotonic SystemClock.elapsedRealtime() lockout protection.
 */
class PinManager(
    context: Context? = null,
    customPrefs: SharedPreferences? = null
) {
    private val prefs: SharedPreferences = customPrefs
        ?: context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?: throw IllegalArgumentException("Either context or customPrefs must be provided")

    companion object {
        private const val PREFS_NAME = "fintrack_security_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_KDF_VERSION = "pin_kdf_version"
        private const val KEY_IS_PIN_SET = "is_pin_set"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL_ELAPSED = "lockout_until_elapsed"
        private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
        private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds lockout

        const val KDF_VERSION_SHA256 = 1
        const val KDF_VERSION_PBKDF2 = 2
        private const val PBKDF2_ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
    }

    val isPinSet: Boolean
        get() = isPinSet(User.DEFAULT_USER_ID)

    var isBiometricEnabled: Boolean
        get() = isBiometricEnabled(User.DEFAULT_USER_ID)
        set(value) = setBiometricEnabled(value, User.DEFAULT_USER_ID)

    private fun getElapsedRealtime(): Long {
        return try {
            android.os.SystemClock.elapsedRealtime()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }

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

    fun getPinKdfVersion(userId: String = User.DEFAULT_USER_ID): Int {
        val kdfKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_KDF_VERSION else "${KEY_PIN_KDF_VERSION}_$userId"
        return prefs.getInt(kdfKey, KDF_VERSION_SHA256)
    }

    fun setPin(pin: String, userId: String = User.DEFAULT_USER_ID): Boolean {
        if (pin.length < 4) return false

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hash = hashPinPbkdf2(pin, salt)

        val hashKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_HASH else "${KEY_PIN_HASH}_$userId"
        val saltKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_SALT else "${KEY_PIN_SALT}_$userId"
        val kdfKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_KDF_VERSION else "${KEY_PIN_KDF_VERSION}_$userId"
        val isSetKey = if (userId == User.DEFAULT_USER_ID) KEY_IS_PIN_SET else "${KEY_IS_PIN_SET}_$userId"
        val failedKey = if (userId == User.DEFAULT_USER_ID) KEY_FAILED_ATTEMPTS else "${KEY_FAILED_ATTEMPTS}_$userId"
        val lockoutKey = if (userId == User.DEFAULT_USER_ID) KEY_LOCKOUT_UNTIL_ELAPSED else "${KEY_LOCKOUT_UNTIL_ELAPSED}_$userId"

        prefs.edit()
            .putString(saltKey, saltBase64)
            .putString(hashKey, hash)
            .putInt(kdfKey, KDF_VERSION_PBKDF2)
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

    /**
     * Helper to set legacy SHA-256 PIN for testing auto-migration.
     */
    internal fun setLegacySha256PinForTesting(pin: String, userId: String = User.DEFAULT_USER_ID) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hash = hashPinSha256(pin, salt)

        val hashKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_HASH else "${KEY_PIN_HASH}_$userId"
        val saltKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_SALT else "${KEY_PIN_SALT}_$userId"
        val kdfKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_KDF_VERSION else "${KEY_PIN_KDF_VERSION}_$userId"
        val isSetKey = if (userId == User.DEFAULT_USER_ID) KEY_IS_PIN_SET else "${KEY_IS_PIN_SET}_$userId"

        prefs.edit()
            .putString(saltKey, saltBase64)
            .putString(hashKey, hash)
            .putInt(kdfKey, KDF_VERSION_SHA256)
            .putBoolean(isSetKey, true)
            .apply()
    }

    fun verifyPin(pin: String, userId: String = User.DEFAULT_USER_ID): VerificationResult {
        val nowElapsed = getElapsedRealtime()
        val lockoutKey = if (userId == User.DEFAULT_USER_ID) KEY_LOCKOUT_UNTIL_ELAPSED else "${KEY_LOCKOUT_UNTIL_ELAPSED}_$userId"
        val failedKey = if (userId == User.DEFAULT_USER_ID) KEY_FAILED_ATTEMPTS else "${KEY_FAILED_ATTEMPTS}_$userId"
        val saltKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_SALT else "${KEY_PIN_SALT}_$userId"
        val hashKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_HASH else "${KEY_PIN_HASH}_$userId"
        val kdfKey = if (userId == User.DEFAULT_USER_ID) KEY_PIN_KDF_VERSION else "${KEY_PIN_KDF_VERSION}_$userId"

        val lockoutUntilElapsed = prefs.getLong(lockoutKey, 0L)

        if (nowElapsed < lockoutUntilElapsed) {
            val remainingSecs = ((lockoutUntilElapsed - nowElapsed) / 1000).toInt() + 1
            return VerificationResult.LockedOut(remainingSecs)
        }

        var saltBase64 = prefs.getString(saltKey, null)
        var storedHash = prefs.getString(hashKey, null)
        var kdfVersion = prefs.getInt(kdfKey, KDF_VERSION_SHA256)

        // Fallback for default user
        if (userId != User.DEFAULT_USER_ID && (saltBase64 == null || storedHash == null)) {
            saltBase64 = prefs.getString(KEY_PIN_SALT, null)
            storedHash = prefs.getString(KEY_PIN_HASH, null)
            kdfVersion = prefs.getInt(KEY_PIN_KDF_VERSION, KDF_VERSION_SHA256)
        }

        if (saltBase64 == null || storedHash == null) {
            return VerificationResult.Failed(0)
        }

        val salt = Base64.getDecoder().decode(saltBase64)
        val isMatch = if (kdfVersion == KDF_VERSION_PBKDF2) {
            val computedHash = hashPinPbkdf2(pin, salt)
            computedHash == storedHash
        } else {
            // Legacy SHA-256 verification
            val computedHash = hashPinSha256(pin, salt)
            val matched = (computedHash == storedHash)
            if (matched) {
                // Transparent auto-upgrade to PBKDF2
                val newSalt = ByteArray(16)
                SecureRandom().nextBytes(newSalt)
                val newSaltBase64 = Base64.getEncoder().encodeToString(newSalt)
                val newPbkdf2Hash = hashPinPbkdf2(pin, newSalt)

                prefs.edit()
                    .putString(saltKey, newSaltBase64)
                    .putString(hashKey, newPbkdf2Hash)
                    .putInt(kdfKey, KDF_VERSION_PBKDF2)
                    .apply()
            }
            matched
        }

        return if (isMatch) {
            // Reset failed counter & lockout
            prefs.edit()
                .putInt(failedKey, 0)
                .putLong(lockoutKey, 0L)
                .apply()
            VerificationResult.Success
        } else {
            val failed = prefs.getInt(failedKey, 0) + 1
            if (failed >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                val lockUntil = nowElapsed + LOCKOUT_DURATION_MS
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

    private fun hashPinPbkdf2(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = skf.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    private fun hashPinSha256(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val hashBytes = md.digest(pin.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    sealed class VerificationResult {
        object Success : VerificationResult()
        data class Failed(val attemptsRemaining: Int) : VerificationResult()
        data class LockedOut(val waitSeconds: Int) : VerificationResult()
    }
}
