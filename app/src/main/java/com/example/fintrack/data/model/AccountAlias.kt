package com.example.fintrack.data.model

import java.util.UUID

data class AccountAlias(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = User.DEFAULT_USER_ID,
    val accountId: String,
    val aliasPattern: String, // e.g. "my salary", "hdfc millennia", "card ending 5678"
    val senderPattern: String? = null // e.g. "HDFCBK", "ICICIB"
)
