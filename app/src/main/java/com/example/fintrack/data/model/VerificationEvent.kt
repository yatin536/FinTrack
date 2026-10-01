package com.example.fintrack.data.model

import java.util.UUID

enum class VerificationStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    IGNORED,
    EXPIRED
}

enum class VerificationEventType {
    CREDIT_CARD_PAYMENT,
    TRANSACTION_CONFIRMATION,
    BANK_TRANSFER,
    ACCOUNT_LINKING,
    DISCREPANCY_ALERT
}

/**
 * Structured model representing an event requiring user review or confirmation.
 * Powers the dedicated Verification Center screen.
 */
data class VerificationEvent(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = User.DEFAULT_USER_ID,
    val status: VerificationStatus = VerificationStatus.PENDING,
    val eventType: VerificationEventType,
    val title: String,
    val description: String,
    val amount: Double? = null,
    val merchant: String? = null,
    val accountLast4: String? = null,
    val sourceText: String? = null,
    val extractedJson: String? = null,
    val confidence: Double = 0.95,
    val transactionId: String? = null,
    val accountId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val resolvedAt: Long? = null
) {
    val isPending: Boolean
        get() = status == VerificationStatus.PENDING

    val confidencePercent: Int
        get() = (confidence * 100).toInt().coerceIn(0, 100)
}
