package com.example.fintrack.engine

import java.security.MessageDigest

/**
 * Intelligent Duplicate and Reversal Fingerprinting Engine.
 * Generates deterministic hashes for incoming SMS alerts to prevent duplicate insertions
 * while correctly recognizing authentic distinct transactions.
 */
object SmsFingerprintEngine {

    /**
     * Generates a primary transaction fingerprint based on bank, amount, last4, ref number, and timestamp window.
     * Uses 2-minute window buckets so rapid re-deliveries of the same bank SMS map to the same fingerprint.
     */
    fun generateFingerprint(
        bankName: String,
        amount: Double,
        last4: String,
        direction: String,
        referenceNumber: String?,
        timestamp: Long
    ): String {
        // If reference number exists, it is the highest-fidelity unique key
        if (!referenceNumber.isNullOrBlank() && referenceNumber.length >= 4) {
            val key = "REF:${bankName.uppercase()}:$referenceNumber:${String.format("%.2f", amount)}"
            return sha256(key)
        }

        // 2-minute window bucket (120,000 ms)
        val timeBucket = timestamp / 120000L
        val raw = "${bankName.uppercase()}:$last4:${String.format("%.2f", amount)}:$direction:$timeBucket"
        return sha256(raw)
    }

    /**
     * Generates a raw text hash of the normalized SMS body.
     */
    fun generateNormalizedBodyHash(body: String): String {
        val normalized = body.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
        return sha256(normalized)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
