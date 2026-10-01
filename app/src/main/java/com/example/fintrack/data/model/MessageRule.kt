package com.example.fintrack.data.model

import java.util.UUID

enum class MessageRuleAction {
    IGNORE,
    DISMISS,
    SUPPRESS
}

enum class MessageRuleStatus {
    ACTIVE,
    DISABLED
}

/**
 * Learned safe classification rule created when a user marks an SMS or transaction as false / not relevant.
 * FinTrack uses these rules to prevent future similar promotional or irrelevant messages from creating pending reviews.
 * Strictly sender- and classification-specific to prevent legitimate financial alerts from being dropped.
 */
data class MessageRule(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = User.DEFAULT_USER_ID,
    val senderPattern: String,
    val bodyPattern: String,
    val classification: String,
    val action: MessageRuleAction = MessageRuleAction.IGNORE,
    val status: MessageRuleStatus = MessageRuleStatus.ACTIVE,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
