package com.example.fintrack.data.model

import java.util.UUID

/**
 * Domain model representing an authenticated user profile in FinTrack.
 * Supports multi-user isolation on the same physical device.
 */
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val colorHex: Long = 0xFF2563EB,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_USER_ID = "user_default"
        val DEFAULT_USER = User(
            id = DEFAULT_USER_ID,
            name = "Primary User",
            email = "user@fintrack.local",
            colorHex = 0xFF2563EB
        )
    }
}
