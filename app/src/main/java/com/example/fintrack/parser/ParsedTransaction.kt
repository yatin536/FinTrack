package com.example.fintrack.parser

import com.example.fintrack.data.model.TransactionType

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val merchant: String,
    val bankName: String,
    val accountNumberLast4: String,
    val balanceAfterTxn: Double?,
    val referenceNumber: String?,
    val timestamp: Long = System.currentTimeMillis()
)
