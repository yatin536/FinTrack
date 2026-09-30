package com.example.fintrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SmsFingerprintEngineTest {

    @Test
    fun testIdenticalSmsProducesIdenticalFingerprint() {
        val fp1 = SmsFingerprintEngine.generateFingerprint("HDFC", 450.0, "1234", "DEBIT", "426189", 1000000L)
        val fp2 = SmsFingerprintEngine.generateFingerprint("HDFC", 450.0, "1234", "DEBIT", "426189", 1000000L)

        assertEquals("Identical parameters must generate same fingerprint", fp1, fp2)
    }

    @Test
    fun testTimeBucketWindowing() {
        // Within the same 2-minute bucket
        val fp1 = SmsFingerprintEngine.generateFingerprint("HDFC", 450.0, "1234", "DEBIT", null, 120000L)
        val fp2 = SmsFingerprintEngine.generateFingerprint("HDFC", 450.0, "1234", "DEBIT", null, 125000L)

        assertEquals("Same time bucket without ref number should produce same fingerprint", fp1, fp2)
    }

    @Test
    fun testDifferentAmountsProduceDifferentFingerprints() {
        val fp1 = SmsFingerprintEngine.generateFingerprint("HDFC", 450.0, "1234", "DEBIT", null, 1000000L)
        val fp2 = SmsFingerprintEngine.generateFingerprint("HDFC", 451.0, "1234", "DEBIT", null, 1000000L)

        assertNotEquals("Different amounts must generate distinct fingerprints", fp1, fp2)
    }

    @Test
    fun testNormalizedBodyHash() {
        val body1 = "Rs 450.00 debited from HDFC Bank a/c **1234"
        val body2 = "  RS 450.00  debited from HDFC bank  a/c **1234  "

        val hash1 = SmsFingerprintEngine.generateNormalizedBodyHash(body1)
        val hash2 = SmsFingerprintEngine.generateNormalizedBodyHash(body2)

        assertNotNull(hash1)
        assertEquals("Normalized SMS strings must have identical hash", hash1, hash2)
    }
}
