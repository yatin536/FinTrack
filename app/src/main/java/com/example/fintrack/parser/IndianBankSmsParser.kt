package com.example.fintrack.parser

import com.example.fintrack.data.model.TransactionType
import java.util.regex.Pattern

/**
 * High-performance, offline regex parser for Indian Banking & Financial SMS.
 * Supports HDFC, SBI, ICICI, Axis, Kotak, PNB, BoB, Canara, UPI, Debit/Credit Cards.
 * Filters out OTPs and promotional spam in < 1ms to ensure zero battery waste.
 */
object IndianBankSmsParser {

    // Spam / OTP keywords that must be rejected immediately
    private val OTP_SPAM_PATTERNS = listOf(
        Pattern.compile("\\b(?:otp|one time password|verification code|secret code)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:do not share|don't share|never share)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:pre-approved|apply now|congratulations|click here|claim your)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:loan|insurance policy|credit limit increased)\\b", Pattern.CASE_INSENSITIVE)
    )

    // Debit indicators
    private val DEBIT_PATTERNS = listOf(
        Pattern.compile("\\b(?:debited|spent|paid|transferred to|sent|withdrawn|purchase|deducted)\\b", Pattern.CASE_INSENSITIVE)
    )

    // Credit indicators
    private val CREDIT_PATTERNS = listOf(
        Pattern.compile("\\b(?:credited|received|deposited|refunded|reversed|cashback|salary)\\b", Pattern.CASE_INSENSITIVE)
    )

    // Amount patterns
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?:(?:rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?))|(?:([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr))",
        Pattern.CASE_INSENSITIVE
    )

    // Available Balance patterns (handles "Avl Bal: INR 12,450", "Avail Bal Rs: 25,600", "Bal Rs 100", etc.)
    private val BALANCE_PATTERN = Pattern.compile(
        "(?:avl(?:\\.|\\s*bal)|avail(?:able)?\\s*bal(?:ance)?|bal(?:ance)?)\\s*(?:is|:|–|-)?\\s*(?:inr|rs\\.?|rs)?\\s*(?:is|:|–|-)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Account Last 4 digits pattern
    private val ACCOUNT_LAST4_PATTERN = Pattern.compile(
        "(?:(?:a/c|acct|ac|card|account|ending)\\s*(?:no\\.?)?\\s*(?:ending(?:\\s+with)?|is)?\\s*(?:[xX*]+)?\\s*([0-9]{3,4}))",
        Pattern.CASE_INSENSITIVE
    )

    // Reference / UPI Ref No pattern
    private val REF_PATTERN = Pattern.compile(
        "(?:upi\\s*ref(?:\\s*no)?|ref\\s*(?:no)?|utr|txn(?:\\s*id)?)[:\\s]+([A-Za-z0-9]+)",
        Pattern.CASE_INSENSITIVE
    )

    // Merchant Extraction patterns
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?:to\\s+vpa\\s+|transfer\\s+to\\s+|to\\s+)([A-Za-z0-9@_.-]+)(?:[\\s(\\[]+(?:on|ref|upi|avl|bal|using|dated|\\.)|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:towards|at|info:)\\s*([A-Za-z0-9_.-]+(?:\\s+[A-Za-z0-9_.-]+)?)(?:[\\s(\\[]+(?:on|avail|bal|using|dated|\\.)|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:paid\\s+to\\s+|sent\\s+to\\s+)([A-Za-z0-9_.-]+(?:\\s+[A-Za-z0-9_.-]+)?)(?:[\\s(\\[]+(?:on|via|ref|bal|\\.)|$)", Pattern.CASE_INSENSITIVE)
    )

    /**
     * Parses an incoming SMS message. Returns ParsedTransaction if valid financial alert,
     * or null if OTP / spam / non-banking message.
     */
    fun parse(sender: String?, messageBody: String): ParsedTransaction? {
        if (messageBody.isBlank()) return null

        // 1. Immediately drop OTPs or Promotional spam
        for (pattern in OTP_SPAM_PATTERNS) {
            if (pattern.matcher(messageBody).find()) {
                return null
            }
        }

        // 2. Identify Transaction Type
        var isDebit = false
        var isCredit = false

        for (pattern in DEBIT_PATTERNS) {
            if (pattern.matcher(messageBody).find()) {
                isDebit = true
                break
            }
        }

        for (pattern in CREDIT_PATTERNS) {
            if (pattern.matcher(messageBody).find()) {
                isCredit = true
                break
            }
        }

        if (!isDebit && !isCredit) {
            return null // Not a financial transaction message
        }

        val type = if (isCredit && !isDebit) {
            TransactionType.CREDIT
        } else {
            TransactionType.DEBIT
        }

        // 3. Extract Amount
        val amount = extractAmount(messageBody) ?: return null

        // 4. Extract Available Balance (if present)
        val balanceAfterTxn = extractBalance(messageBody)

        // 5. Extract Account Last 4 digits
        val accountLast4 = extractAccountLast4(messageBody) ?: ""

        // 6. Extract Bank Name from sender or text
        val bankName = identifyBank(sender, messageBody)

        // 7. Extract Merchant / Beneficiary
        val merchant = extractMerchant(messageBody, bankName, type)

        // 8. Extract Reference / UTR
        val refNo = extractRefNumber(messageBody)

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            bankName = bankName,
            accountNumberLast4 = accountLast4,
            balanceAfterTxn = balanceAfterTxn,
            referenceNumber = refNo
        )
    }

    private fun extractAmount(text: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val amountStr = matcher.group(1) ?: matcher.group(2) ?: return null
            val clean = amountStr.replace(",", "").trim()
            return clean.toDoubleOrNull()
        }
        return null
    }

    private fun extractBalance(text: String): Double? {
        val matcher = BALANCE_PATTERN.matcher(text)
        if (matcher.find()) {
            val balStr = matcher.group(1) ?: return null
            val clean = balStr.replace(",", "").trim()
            return clean.toDoubleOrNull()
        }
        return null
    }

    private fun extractAccountLast4(text: String): String? {
        val matcher = ACCOUNT_LAST4_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.takeLast(4)
        }
        return null
    }

    private fun extractRefNumber(text: String): String? {
        val matcher = REF_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    private fun extractMerchant(text: String, bankName: String, type: TransactionType): String {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && candidate.length > 1 && !candidate.equals("INR", true) && !candidate.equals("Rs", true)) {
                    // Clean up VPA handle if needed (e.g. swiggy@icici -> Swiggy)
                    return cleanMerchantName(candidate)
                }
            }
        }

        // Fallbacks based on message context
        return when {
            text.contains("ATM", ignoreCase = true) -> "ATM Cash Withdrawal"
            text.contains("salary", ignoreCase = true) -> "Salary / Payroll"
            type == TransactionType.CREDIT -> "Direct Credit / Transfer"
            else -> "$bankName Payment"
        }
    }

    private fun cleanMerchantName(raw: String): String {
        var clean = raw.trim()
        // If it's a UPI handle (e.g. merchant@paytm), extract merchant
        if (clean.contains("@")) {
            val prefix = clean.substringBefore("@")
            if (prefix.isNotBlank() && prefix.length > 2) {
                clean = prefix
            }
        }
        // Replace underscores and dots with spaces
        clean = clean.replace(Regex("[._]+"), " ")
        // Format to Title Case
        return clean.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    private fun identifyBank(sender: String?, text: String): String {
        val combined = "${sender ?: ""} $text".uppercase()
        return when {
            combined.contains("HDFC") -> "HDFC Bank"
            combined.contains("SBI") || combined.contains("SBIN") -> "State Bank of India"
            combined.contains("ICICI") -> "ICICI Bank"
            combined.contains("AXIS") || combined.contains("UTIB") -> "Axis Bank"
            combined.contains("KOTAK") -> "Kotak Bank"
            combined.contains("PNB") || combined.contains("PUNB") -> "Punjab National Bank"
            combined.contains("BOB") || combined.contains("BARB") -> "Bank of Baroda"
            combined.contains("CANARA") || combined.contains("CNRB") -> "Canara Bank"
            combined.contains("INDUS") || combined.contains("INDB") -> "IndusInd Bank"
            combined.contains("PAYTM") -> "Paytm Payments Bank"
            combined.contains("IDFC") -> "IDFC FIRST Bank"
            combined.contains("YES") || combined.contains("YESB") -> "YES Bank"
            else -> "Bank"
        }
    }
}
