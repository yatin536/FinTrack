package com.example.fintrack.data.model

import java.util.UUID

enum class TransactionDirection {
    DEBIT,
    CREDIT
}

// Backward-compatible alias for existing code
typealias TransactionType = TransactionDirection

enum class TransactionStatus {
    CONFIRMED,
    PENDING_REVIEW,
    REJECTED,
    DISMISSED;

    companion object {
        fun fromString(statusStr: String?): TransactionStatus {
            if (statusStr == null) return CONFIRMED
            return try {
                valueOf(statusStr.uppercase().trim())
            } catch (_: Exception) {
                CONFIRMED
            }
        }
    }
}

enum class TransactionKind {
    EXPENSE,
    INCOME,
    BANK_TRANSFER,
    CARD_PURCHASE,
    CARD_PAYMENT,
    CARD_PAYMENT_SUCCESS,
    CARD_PAYMENT_FAILED,
    CARD_PAYMENT_REVERSED,
    CARD_BILL_GENERATED,
    CARD_BILL_DUE,
    CARD_BILL_OVERDUE,
    REFUND,
    REVERSAL,
    ATM_WITHDRAWAL,
    CASH_DEPOSIT,
    CASH_WITHDRAWAL,
    BANK_DEBIT,
    BANK_CREDIT,
    TRANSFER,
    PROMOTIONAL,
    OTP,
    ADJUSTMENT,
    UNKNOWN;

    companion object {
        fun fromString(kindStr: String?): TransactionKind {
            if (kindStr == null) return EXPENSE
            return try {
                valueOf(kindStr.uppercase().trim())
            } catch (_: Exception) {
                EXPENSE
            }
        }
    }
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = User.DEFAULT_USER_ID,
    val accountId: String, // Primary financial instrument
    val sourceAccountId: String? = null, // In transfers/payments: source account
    val destinationAccountId: String? = null, // In transfers/payments: destination account
    val categoryId: String,
    val amount: Double,
    val direction: TransactionDirection = TransactionDirection.DEBIT,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val status: TransactionStatus = TransactionStatus.CONFIRMED,
    val timestamp: Long = System.currentTimeMillis(),
    val merchant: String,
    val rawSmsBody: String? = null,
    val smsSender: String? = null,
    val referenceNumber: String? = null,
    val balanceAfterTxn: Double? = null, // Available bank balance
    val availableCreditAfterTxn: Double? = null, // Available credit card limit
    val isManual: Boolean = false,
    val note: String? = null,
    val needsReview: Boolean = false,
    val reviewReason: String? = null,
    val fingerprint: String? = null, // Duplicate detection hash
    val linkedTransactionId: String? = null, // ID of related transaction (e.g. reversal, transfer peer, refund)
    val createdAt: Long = System.currentTimeMillis()
) {
    // Backward-compatible property alias
    val type: TransactionDirection
        get() = direction
}

data class TransactionWithDetails(
    val transaction: Transaction,
    val account: Account,
    val category: Category,
    val sourceAccount: Account? = null,
    val destinationAccount: Account? = null
)
