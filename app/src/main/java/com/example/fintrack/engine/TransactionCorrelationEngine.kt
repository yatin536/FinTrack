package com.example.fintrack.engine

import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.parser.IndianBankSmsParser
import com.example.fintrack.parser.ParsedTransaction
import kotlin.math.abs

/**
 * Intelligent Multi-Message Transaction Correlation Engine.
 * Correlates dual SMS alerts arriving for the same financial purchase
 * (e.g., an Amazon/Swiggy order notification + a Kotak/HDFC Bank debit alert),
 * fuses them into a single ledger transaction, prevents double deductions,
 * merges rich merchant metadata, and reconciles official closing balances.
 */
object TransactionCorrelationEngine {

    const val CORRELATION_WINDOW_MS = 15 * 60 * 1000L // 15 minutes

    fun isCorrelated(
        existing: Transaction,
        incoming: ParsedTransaction,
        incomingSender: String?,
        incomingBody: String
    ): Boolean {
        // 1. Same direction (DEBIT with DEBIT, CREDIT with CREDIT)
        if (existing.direction != incoming.direction) return false

        // 2. Exact same amount (within 1 paisa)
        if (abs(existing.amount - incoming.amount) >= 0.01) return false

        // 3. Informational bill notices shouldn't merge with real expenses
        if (existing.kind == TransactionKind.CARD_BILL_DUE || existing.kind == TransactionKind.CARD_BILL_GENERATED) {
            return false
        }

        // 4. Timestamp window check (within 15 minutes)
        if (abs(existing.timestamp - incoming.timestamp) > CORRELATION_WINDOW_MS) {
            return false
        }

        val existingBody = (existing.rawSmsBody ?: "").lowercase()
        val existingMerchant = existing.merchant.lowercase()

        val incomingIsMerchant = IndianBankSmsParser.isMerchantSender(incomingSender) ||
                                (IndianBankSmsParser.hasMerchantKeyword(incomingBody) && incomingBody.contains("order", ignoreCase = true))
        val existingIsMerchant = IndianBankSmsParser.isMerchantSender(existing.smsSender) ||
                                (IndianBankSmsParser.hasMerchantKeyword(existingBody) && existingBody.contains("order", ignoreCase = true))

        val incomingIsBank = IndianBankSmsParser.isBankSender(incomingSender) || incoming.availableBalance != null || !incoming.referenceNumber.isNullOrBlank()
        val existingIsBank = IndianBankSmsParser.isBankSender(existing.smsSender) || existing.balanceAfterTxn != null || !existing.referenceNumber.isNullOrBlank()

        // Condition A: One alert is from a Merchant and the other is from a Bank
        if ((incomingIsMerchant && existingIsBank) || (incomingIsBank && existingIsMerchant)) {
            val merchantName = if (incomingIsMerchant) incoming.merchant.lowercase() else existingMerchant
            val bankText = if (incomingIsBank) incomingBody.lowercase() else existingBody
            val merchantText = if (incomingIsMerchant) incomingBody.lowercase() else existingBody

            val merchantFoundInBankText = merchantName.isNotBlank() && bankText.contains(merchantName)
            val bankFoundInMerchantText = incoming.bankName.isNotBlank() && merchantText.contains(incoming.bankName.lowercase())
            val commonKeywords = listOf("amazon", "swiggy", "zomato", "flipkart", "uber", "paytm", "zepto", "blinkit", "myntra")
            val keywordMatch = commonKeywords.any { kw ->
                (merchantText.contains(kw) || existingMerchant.contains(kw)) && (bankText.contains(kw) || incoming.merchant.lowercase().contains(kw))
            }

            if (merchantFoundInBankText || bankFoundInMerchantText || keywordMatch) {
                return true
            }

            // If within 10 minutes, same amount, one is merchant and other is bank debit
            if (abs(existing.timestamp - incoming.timestamp) <= 10 * 60 * 1000L) {
                return true
            }
        }

        // Condition B: Both reference the same account last-4
        if (incoming.accountNumberLast4.isNotBlank() && existingBody.contains(incoming.accountNumberLast4)) {
            return true
        }

        // Condition C: Shared UTR / Reference number
        if (!incoming.referenceNumber.isNullOrBlank() && !existing.referenceNumber.isNullOrBlank()) {
            if (incoming.referenceNumber.equals(existing.referenceNumber, ignoreCase = true)) {
                return true
            }
        }

        // Condition D: Same amount within 5 minutes on the same bank
        if (abs(existing.timestamp - incoming.timestamp) <= 5 * 60 * 1000L) {
            if (existingBody.contains(incoming.bankName.lowercase()) || incoming.bankName == "Bank") {
                return true
            }
        }

        return false
    }

    /**
     * Resolves the merged merchant name, giving preference to rich merchant SMS titles.
     */
    fun resolveMergedMerchant(
        existingMerchant: String,
        existingSender: String?,
        existingBody: String?,
        incomingMerchant: String,
        incomingSender: String?,
        incomingBody: String
    ): String {
        val incomingIsMerchant = IndianBankSmsParser.isMerchantSender(incomingSender) ||
                                (IndianBankSmsParser.hasMerchantKeyword(incomingBody) && incomingBody.contains("order", ignoreCase = true))
        val existingIsMerchant = IndianBankSmsParser.isMerchantSender(existingSender) ||
                                (IndianBankSmsParser.hasMerchantKeyword(existingBody ?: "") && (existingBody ?: "").contains("order", ignoreCase = true))

        return when {
            incomingIsMerchant && incomingMerchant.isNotBlank() && incomingMerchant != "Bank Payment" && !incomingMerchant.contains("Payment", ignoreCase = true) -> incomingMerchant
            existingIsMerchant && existingMerchant.isNotBlank() && existingMerchant != "Bank Payment" && !existingMerchant.contains("Payment", ignoreCase = true) -> existingMerchant
            incomingIsMerchant && incomingMerchant.isNotBlank() -> incomingMerchant
            existingIsMerchant && existingMerchant.isNotBlank() -> existingMerchant
            else -> existingMerchant.ifBlank { incomingMerchant }
        }
    }
}
