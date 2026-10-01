package com.example.fintrack.data.model

import java.util.UUID

enum class SmsAlertStatus {
    PROCESSED,
    NEEDS_REVIEW,
    DUPLICATE,
    IGNORED
}

data class ImportedSmsAlert(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: SmsAlertStatus = SmsAlertStatus.PROCESSED,
    val transactionId: String? = null,
    val accountId: String? = null,
    val confidence: Double = 1.0,
    val reason: String? = null,
    val userId: String = User.DEFAULT_USER_ID
)
