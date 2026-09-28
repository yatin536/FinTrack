package com.example.fintrack.data.model

import java.util.UUID

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bankName: String,
    val accountNumberLast4: String = "",
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val colorHex: Long = 0xFF2563EB, // Primary blue
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = if (accountNumberLast4.isNotEmpty()) {
            "$name (••$accountNumberLast4)"
        } else {
            name
        }
}
