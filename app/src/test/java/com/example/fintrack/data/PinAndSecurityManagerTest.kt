package com.example.fintrack.data

import android.content.SharedPreferences
import com.example.fintrack.data.local.PinManager
import com.example.fintrack.data.local.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class PinAndSecurityManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var pinManager: PinManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        pinManager = PinManager(customPrefs = fakePrefs)
    }

    @Test
    fun testPbkdf2PinCreationAndVerification() {
        assertFalse(pinManager.isPinSet())

        // Set PIN
        val success = pinManager.setPin("4321")
        assertTrue(success)
        assertTrue(pinManager.isPinSet())
        assertEquals(PinManager.KDF_VERSION_PBKDF2, pinManager.getPinKdfVersion())

        // Verify with correct PIN
        val correctResult = pinManager.verifyPin("4321")
        assertTrue("Verification should succeed with correct PIN", correctResult is PinManager.VerificationResult.Success)

        // Verify with incorrect PIN
        val wrongResult = pinManager.verifyPin("0000")
        assertTrue("Verification should fail with wrong PIN", wrongResult is PinManager.VerificationResult.Failed)
        assertEquals(4, (wrongResult as PinManager.VerificationResult.Failed).attemptsRemaining)
    }

    @Test
    fun testLegacySha256TransparentAutoMigration() {
        // Seed legacy SHA-256 PIN
        pinManager.setLegacySha256PinForTesting("9876")
        assertEquals(PinManager.KDF_VERSION_SHA256, pinManager.getPinKdfVersion())

        // Verify with correct PIN -> triggers auto-migration to PBKDF2
        val result = pinManager.verifyPin("9876")
        assertTrue(result is PinManager.VerificationResult.Success)

        // Verify KDF version upgraded to PBKDF2
        assertEquals(PinManager.KDF_VERSION_PBKDF2, pinManager.getPinKdfVersion())

        // Subsequent verifications succeed seamlessly with PBKDF2
        val nextResult = pinManager.verifyPin("9876")
        assertTrue(nextResult is PinManager.VerificationResult.Success)
    }

    @Test
    fun testLockoutEnforcementAfterFiveFailedAttempts() {
        pinManager.setPin("5555")

        // 4 failed attempts
        for (i in 1..4) {
            val res = pinManager.verifyPin("9999")
            assertTrue(res is PinManager.VerificationResult.Failed)
            assertEquals(5 - i, (res as PinManager.VerificationResult.Failed).attemptsRemaining)
        }

        // 5th failed attempt triggers lockout
        val lockoutResult = pinManager.verifyPin("9999")
        assertTrue("5th failed attempt must trigger lockout", lockoutResult is PinManager.VerificationResult.LockedOut)
        val waitSecs = (lockoutResult as PinManager.VerificationResult.LockedOut).waitSeconds
        assertTrue("Wait seconds should be around 30", waitSecs in 1..30)

        // While locked out, even the correct PIN must be rejected with LockedOut
        val lockedAttempt = pinManager.verifyPin("5555")
        assertTrue("Should still be locked out", lockedAttempt is PinManager.VerificationResult.LockedOut)
    }

    @Test
    fun testPerUserPinIsolation() {
        val user1 = "user_default"
        val user2 = "user_sister_id"

        pinManager.setPin("1111", user1)
        pinManager.setPin("2222", user2)

        assertTrue(pinManager.verifyPin("1111", user1) is PinManager.VerificationResult.Success)
        assertTrue(pinManager.verifyPin("2222", user1) is PinManager.VerificationResult.Failed)

        assertTrue(pinManager.verifyPin("2222", user2) is PinManager.VerificationResult.Success)
        assertTrue(pinManager.verifyPin("1111", user2) is PinManager.VerificationResult.Failed)
    }

    @Test
    fun testSecurityManagerAes256GcmEncryptionAndDecryption() {
        val plainText = "HDFC Bank Ref #99214: ₹45,000 transferred"

        val cipherText = SecurityManager.encrypt(plainText)
        assertTrue("Ciphertext must not be null", cipherText != null)
        assertTrue("Ciphertext must contain IV separator ':'", cipherText!!.contains(":"))
        assertNotEquals(plainText, cipherText)

        val decrypted = SecurityManager.decrypt(cipherText)
        assertEquals(plainText, decrypted)
    }

    @Test
    fun testSecurityManagerDoubleEncryption() {
        val amount = 125000.50
        val encrypted = SecurityManager.encryptDouble(amount)
        assertTrue(encrypted.contains(":"))

        val decrypted = SecurityManager.decryptDouble(encrypted)
        assertEquals(amount, decrypted, 0.001)
    }

    @Test
    fun testSecurityManagerLegacyPlaintextFallback() {
        // String without colon is treated as legacy unencrypted string
        val legacyString = "HDFC_SALARY_ACCOUNT_UNENCRYPTED"
        val result = SecurityManager.decrypt(legacyString)
        assertEquals(legacyString, result)
    }

    @Test
    fun testSecurityManagerCorruptedCiphertextThrowsSecurityException() {
        val corrupted = "dGVzdF9pdg==:Y29ycnVwdGVkX2NpcGhlcnRleHQ="
        try {
            SecurityManager.decrypt(corrupted)
            fail("Decryption of corrupted ciphertext must throw SecurityException")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("SecurityManager decryption failed") == true)
        }
    }
}

/**
 * Lightweight, fully thread-safe in-memory SharedPreferences for unit testing.
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String> ?: defValues)

    override fun getInt(key: String?, defValue: Int): Int =
        data[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        data[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        data[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    inner class FakeEditor : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var shouldClear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            shouldClear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (shouldClear) data.clear()
            temp.forEach { (k, v) ->
                if (v == null) data.remove(k) else data[k] = v
            }
        }
    }
}
