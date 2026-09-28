package com.example.fintrack.parser

import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.model.Category
import com.example.fintrack.data.model.TransactionType

/**
 * On-Device Heuristic & Self-Learning Categorization Engine.
 * 100% private, runs entirely offline on the CPU with zero cloud calls.
 */
class ExpenseCategorizer(private val dbHelper: AppDatabaseHelper) {

    /**
     * Categorizes a transaction based on:
     * 1. User's previously saved merchant rules (self-learning)
     * 2. Semantic keyword matching against Indian merchant names & handles
     * 3. Transaction type defaults
     */
    fun categorize(
        merchant: String,
        smsBody: String,
        type: TransactionType
    ): String {
        // Priority 1: Learned user rules (e.g. user previously mapped "Chai Point" to Food)
        val learnedCategoryId = dbHelper.getCategoryForMerchantRule(merchant)
        if (learnedCategoryId != null) {
            return learnedCategoryId
        }

        // Priority 2: Keyword matching
        val combinedText = "$merchant $smsBody".lowercase()
        val categories = dbHelper.getCategories()

        for (category in categories) {
            for (keyword in category.keywords) {
                if (keyword.isNotBlank() && combinedText.contains(keyword.lowercase().trim())) {
                    return category.id
                }
            }
        }

        // Priority 3: Fallback based on type
        return if (type == TransactionType.CREDIT) {
            if (combinedText.contains("salary") || combinedText.contains("payroll")) {
                "cat_salary"
            } else {
                "cat_transfer"
            }
        } else {
            "cat_other"
        }
    }
}
