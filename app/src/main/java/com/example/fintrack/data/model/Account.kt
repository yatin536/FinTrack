package com.example.fintrack.data.model

import java.util.UUID

enum class AccountType {
    BANK_ACCOUNT,
    CREDIT_CARD,
    CASH_WALLET,
    UPI_WALLET,
    OTHER;

    companion object {
        // Backward-compatibility mapping from legacy string representation
        fun fromString(typeStr: String?): AccountType {
            if (typeStr == null) return BANK_ACCOUNT
            return when (typeStr.uppercase().trim()) {
                "BANK", "BANK_ACCOUNT" -> BANK_ACCOUNT
                "CREDIT_CARD" -> CREDIT_CARD
                "WALLET", "CASH_WALLET" -> CASH_WALLET
                "UPI_WALLET" -> UPI_WALLET
                else -> try {
                    valueOf(typeStr)
                } catch (_: Exception) {
                    OTHER
                }
            }
        }
    }
}

enum class BankAccountType {
    SAVINGS,
    CURRENT
}

enum class BillStatus {
    UPCOMING,
    GENERATED,
    DUE_SOON,
    PAID,
    OVERDUE,
    UNKNOWN;

    companion object {
        fun fromString(statusStr: String?): BillStatus {
            if (statusStr == null) return UNKNOWN
            return try {
                valueOf(statusStr.uppercase().trim())
            } catch (_: Exception) {
                UNKNOWN
            }
        }
    }
}

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = User.DEFAULT_USER_ID,
    val name: String,
    val bankName: String, // Bank or Card Issuer (e.g. HDFC, ICICI, SBI)
    val accountType: AccountType = AccountType.BANK_ACCOUNT,
    val bankAccountType: BankAccountType = BankAccountType.SAVINGS,
    val accountNumberLast4: String = "",
    val initialBalance: Double = 0.0, // Opening ledger balance for bank/wallet
    val currentBalance: Double = 0.0, // For bank: current balance. For credit card: current outstanding!
    val creditLimit: Double = 0.0, // Total credit limit (for credit card)
    val availableCredit: Double? = null, // Available credit reported by SMS
    val statementDate: String? = null, // Statement generation date if known
    val paymentDueDate: String? = null, // Payment due date if known
    val minimumDue: Double? = null, // Minimum amount due
    val totalDue: Double? = null, // Total amount due
    val billStatus: BillStatus = BillStatus.UNKNOWN, // Bill payment status (PAID, DUE_SOON, etc.)
    val linkedPaymentAccountIds: List<String> = emptyList(), // Bank account IDs configured to pay this CC
    val lastConfirmedBalance: Double? = null, // Official balance confirmed from SMS
    val lastConfirmedAt: Long? = null, // Timestamp when official balance was confirmed
    val colorHex: Long = 0xFF2563EB, // Accent color
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isCreditCard: Boolean
        get() = accountType == AccountType.CREDIT_CARD

    // Outstanding debt for credit cards
    val outstandingBalance: Double
        get() = if (isCreditCard) currentBalance else 0.0

    // Available credit limit left
    val limitLeft: Double
        get() = if (isCreditCard) {
            availableCredit ?: maxOf(0.0, creditLimit - currentBalance)
        } else {
            0.0
        }

    // Limit used
    val limitUsed: Double
        get() = if (isCreditCard) currentBalance else 0.0

    val isBillPaid: Boolean
        get() = billStatus == BillStatus.PAID || (totalDue != null && totalDue <= 0.0)

    val displayName: String
        get() = if (accountNumberLast4.isNotEmpty()) {
            "$name (••$accountNumberLast4)"
        } else {
            name
        }
}
