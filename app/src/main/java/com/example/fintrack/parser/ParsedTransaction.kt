package com.example.fintrack.parser

import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind

enum class FinancialInstrumentType {
    BANK_ACCOUNT,
    CREDIT_CARD,
    UNKNOWN
}

data class ParsedTransaction(
    val amount: Double,
    val direction: TransactionDirection,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val instrumentType: FinancialInstrumentType = FinancialInstrumentType.BANK_ACCOUNT,
    val merchant: String,
    val bankName: String,
    val accountNumberLast4: String,
    val availableBalance: Double? = null, // For bank accounts
    val availableCredit: Double? = null, // For credit cards
    val outstandingAmount: Double? = null, // For credit cards
    val minimumDue: Double? = null, // For credit cards
    val totalDue: Double? = null, // For credit cards
    val dueDate: String? = null, // For credit cards
    val statementDate: String? = null, // For credit cards
    val referenceNumber: String? = null,
    val confidence: Double = 1.0,
    val timestamp: Long = System.currentTimeMillis(),
    val rawSender: String? = null,
    val rawBody: String? = null
) {
    // Backward-compatibility properties
    val type: TransactionDirection
        get() = direction

    val balanceAfterTxn: Double?
        get() = availableBalance ?: availableCredit
}
