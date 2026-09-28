package com.example.fintrack.data.model

import java.util.UUID

enum class TransactionType {
    DEBIT,
    CREDIT
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val categoryId: String,
    val amount: Double,
    val type: TransactionType,
    val timestamp: Long = System.currentTimeMillis(),
    val merchant: String,
    val rawSmsBody: String? = null,
    val smsSender: String? = null,
    val referenceNumber: String? = null,
    val balanceAfterTxn: Double? = null,
    val isManual: Boolean = false,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class TransactionWithDetails(
    val transaction: Transaction,
    val account: Account,
    val category: Category
)
