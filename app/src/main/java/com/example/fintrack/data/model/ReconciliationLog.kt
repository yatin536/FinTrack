package com.example.fintrack.data.model

import java.util.UUID

data class ReconciliationLog(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = User.DEFAULT_USER_ID,
    val accountId: String,
    val ledgerBalance: Double,
    val confirmedBalance: Double,
    val discrepancy: Double,
    val adjustmentAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)
