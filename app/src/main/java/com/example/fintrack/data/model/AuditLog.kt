package com.example.fintrack.data.model

import java.util.UUID

/**
 * Audit log recording critical financial state mutations (e.g. Bill Paid, Reconciled, Verified).
 * Ensures full audibility and user transparency.
 */
data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = User.DEFAULT_USER_ID,
    val entityType: String, // CREDIT_CARD, TRANSACTION, ACCOUNT
    val entityId: String,
    val action: String, // BILL_PAID, PAYMENT_RECONCILED, VERIFIED, REJECTED
    val oldState: String? = null,
    val newState: String? = null,
    val source: String, // "User confirmation", "Detected bank message"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String? = null
)
