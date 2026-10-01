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
    val isBiometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() {
            if (id == DEFAULT_USER_ID && (name.isBlank() || name == "Primary User" || name == "User" || name == "Default User")) {
                return DEFAULT_USER.name
            }
            return name.ifBlank { "User" }
        }

    val firstName: String
        get() = displayName.trim().split("\\s+".toRegex()).firstOrNull()?.ifBlank { "User" } ?: "User"

    companion object {
        const val DEFAULT_USER_ID = "user_default"
        val DEFAULT_USER = User(
            id = DEFAULT_USER_ID,
            name = "Yatin Kumar Singh",
            email = "yatin@fintrack.local",
            phoneNumber = null,
            colorHex = 0xFF2563EB,
            isActive = true,
            isBiometricEnabled = false,
            createdAt = 1759276800000L, // 30 September 2026
            updatedAt = 1759276800000L
        )
    }
}
