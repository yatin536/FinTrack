package com.example.fintrack.engine

import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountAlias
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.parser.FinancialInstrumentType
import com.example.fintrack.parser.ParsedTransaction

data class AccountMatchResult(
    val account: Account?,
    val confidence: Double,
    val matchReason: String,
    val needsReview: Boolean
)

/**
 * Intelligent Account Identification Engine.
 * Matches incoming SMS alerts to the correct financial instrument (Bank Account vs Credit Card)
 * using a multi-tier confidence priority model.
 * Never guesses an account when confidence is insufficient (< 0.70).
 */
class AccountIdentificationEngine(private val dbHelper: AppDatabaseHelper? = null) {

    companion object {
        const val CONFIDENCE_EXACT = 0.98
        const val CONFIDENCE_BANK_AND_LAST4 = 0.90
        const val CONFIDENCE_ALIAS = 0.85
        const val CONFIDENCE_BANK_ONLY = 0.65 // Triggers review
        const val CONFIDENCE_UNKNOWN = 0.30 // Triggers review
        const val REVIEW_THRESHOLD = 0.70
    }

    /**
     * Resolves the matching account for a parsed financial SMS.
     */
    fun identifyAccount(
        parsed: ParsedTransaction,
        sender: String?,
        smsBody: String,
        accounts: List<Account> = dbHelper?.getAccounts() ?: emptyList(),
        aliases: List<AccountAlias> = dbHelper?.getAccountAliases() ?: emptyList()
    ): AccountMatchResult {
        if (accounts.isEmpty()) {
            return AccountMatchResult(
                account = null,
                confidence = 0.0,
                matchReason = "No accounts configured in vault",
                needsReview = true
            )
        }

        val targetIsCard = parsed.instrumentType == FinancialInstrumentType.CREDIT_CARD
        val last4 = parsed.accountNumberLast4.trim()
        val bankName = parsed.bankName.trim()
        val lowerBody = smsBody.lowercase()
        val lowerSender = (sender ?: "").lowercase()

        // 1. Check user-defined Aliases & Learning Rules
        for (alias in aliases) {
            val aliasMatches = lowerBody.contains(alias.aliasPattern.lowercase()) ||
                    (alias.senderPattern != null && lowerSender.contains(alias.senderPattern.lowercase()))
            if (aliasMatches) {
                val matchedAcc = accounts.firstOrNull { it.id == alias.accountId }
                if (matchedAcc != null) {
                    return AccountMatchResult(
                        account = matchedAcc,
                        confidence = CONFIDENCE_ALIAS,
                        matchReason = "Matched user-learned alias rule: '${alias.aliasPattern}'",
                        needsReview = false
                    )
                }
            }
        }

        // 2. Exact Match: Bank/Issuer + Exact Last4 + Financial Instrument Context (Card vs Bank)
        if (last4.isNotEmpty()) {
            val matchingLast4 = accounts.filter { it.accountNumberLast4 == last4 }

            if (matchingLast4.isNotEmpty()) {
                // If instrument type matches (Card vs Bank) and Bank matches
                val exactMatch = matchingLast4.firstOrNull { acc ->
                    val typeMatches = if (targetIsCard) acc.isCreditCard else !acc.isCreditCard
                    val bankMatches = isBankMatch(acc.bankName, bankName)
                    typeMatches && bankMatches
                }
                if (exactMatch != null) {
                    return AccountMatchResult(
                        account = exactMatch,
                        confidence = CONFIDENCE_EXACT,
                        matchReason = "Exact match on ${if (targetIsCard) "Credit Card" else "Bank Account"} '$bankName' ending ••$last4",
                        needsReview = false
                    )
                }

                // If only last4 and bank match
                val bankAndLast4Match = matchingLast4.firstOrNull { acc ->
                    isBankMatch(acc.bankName, bankName)
                }
                if (bankAndLast4Match != null) {
                    return AccountMatchResult(
                        account = bankAndLast4Match,
                        confidence = CONFIDENCE_BANK_AND_LAST4,
                        matchReason = "Match on Bank '$bankName' and ending ••$last4",
                        needsReview = false
                    )
                }

                // Last 4 matches a single account directly
                if (matchingLast4.size == 1) {
                    return AccountMatchResult(
                        account = matchingLast4.first(),
                        confidence = CONFIDENCE_BANK_AND_LAST4,
                        matchReason = "Match on unique ending ••$last4",
                        needsReview = false
                    )
                }
            }
        }

        // 3. Match by Bank/Issuer only (when last 4 digits are not mentioned in SMS)
        val matchingBank = accounts.filter { isBankMatch(it.bankName, bankName) }
        if (matchingBank.size == 1) {
            val singleBankAcc = matchingBank.first()
            val typeMatches = if (targetIsCard) singleBankAcc.isCreditCard else !singleBankAcc.isCreditCard
            if (typeMatches) {
                // If last4 was missing in SMS, require review unless this is the only account for that bank
                return AccountMatchResult(
                    account = singleBankAcc,
                    confidence = 0.75,
                    matchReason = "Inferred single ${if (targetIsCard) "Card" else "Account"} for $bankName (no last 4 in SMS)",
                    needsReview = false
                )
            }
        } else if (matchingBank.size > 1) {
            // Multiple accounts for the same bank, ambiguous which one was debited/credited
            val defaultChoice = matchingBank.firstOrNull { it.isPrimary } ?: matchingBank.first()
            return AccountMatchResult(
                account = defaultChoice,
                confidence = CONFIDENCE_BANK_ONLY,
                matchReason = "Multiple $bankName accounts found; last 4 digits missing in SMS",
                needsReview = true
            )
        }

        // 4. Ambiguous or completely unknown
        val fallbackAccount = accounts.firstOrNull { it.isPrimary } ?: accounts.first()
        return AccountMatchResult(
            account = fallbackAccount,
            confidence = CONFIDENCE_UNKNOWN,
            matchReason = "Uncertain financial instrument ($bankName). Please verify account.",
            needsReview = true
        )
    }

    private fun isBankMatch(accBank: String, parsedBank: String): Boolean {
        val a = accBank.uppercase().replace("BANK", "").replace(" ", "").trim()
        val b = parsedBank.uppercase().replace("BANK", "").replace(" ", "").trim()
        if (a.isEmpty() || b.isEmpty()) return false
        return a.contains(b) || b.contains(a)
    }
}
