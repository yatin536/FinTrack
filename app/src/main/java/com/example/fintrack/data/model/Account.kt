package com.example.fintrack.data.model

import java.util.UUID

enum class AccountType {
    BANK, CREDIT_CARD, WALLET
}

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bankName: String,
    val accountType: AccountType = AccountType.BANK,
    val accountNumberLast4: String = "",
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val creditLimit: Double = 0.0, // Used if type is CREDIT_CARD
    val colorHex: Long = 0xFF2563EB, // Primary blue
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val limitLeft: Double
        get() = if (accountType == AccountType.CREDIT_CARD) creditLimit - currentBalance else 0.0

    val displayName: String
        get() = if (accountNumberLast4.isNotEmpty()) {
            "$name (••$accountNumberLast4)"
        } else {
            name
        }
}
