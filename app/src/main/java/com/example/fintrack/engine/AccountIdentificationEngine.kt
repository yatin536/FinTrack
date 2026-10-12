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
     * Resolves the matching account for a parsed financial SMS with an explicit list of accounts.
     */
    fun identifyAccount(
        parsed: ParsedTransaction,
        sender: String?,
        smsBody: String,
        accounts: List<Account>,
        aliases: List<AccountAlias> = emptyList()
    ): AccountMatchResult = identifyAccount(
        parsed = parsed,
        sender = sender,
        smsBody = smsBody,
        userId = com.example.fintrack.data.model.User.DEFAULT_USER_ID,
        accounts = accounts,
        aliases = aliases
    )

    /**
     * Resolves the matching account for a parsed financial SMS.
     */
    fun identifyAccount(
        parsed: ParsedTransaction,
        sender: String?,
        smsBody: String,
        userId: String = com.example.fintrack.data.model.User.DEFAULT_USER_ID,
        accounts: List<Account> = dbHelper?.getAccounts(userId) ?: emptyList(),
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
            } else {
                // SMS specifies last4, but no account in vault has this account number last4.
                // Check if this is a Debit Card transaction belonging to a Bank Account
                val isDebitCardSms = parsed.isDebitCard || lowerBody.contains("debit card") || lowerBody.contains("debitcard") || lowerBody.contains("dc ending") || lowerBody.contains("dc no")
                if (isDebitCardSms) {
                    val matchingBankAccounts = accounts.filter { !it.isCreditCard && isBankMatch(it.bankName, bankName) }
                    if (matchingBankAccounts.size == 1) {
                        val bankAcc = matchingBankAccounts.first()
                        // Auto-learn debit card alias for future instant matching
                        dbHelper?.addAccountAlias(
                            AccountAlias(
                                accountId = bankAcc.id,
                                aliasPattern = last4,
                                senderPattern = sender
                            )
                        )
                        return AccountMatchResult(
                            account = bankAcc,
                            confidence = 0.95,
                            matchReason = "Matched Debit Card ••$last4 to ${bankAcc.bankName} account ••${bankAcc.accountNumberLast4}",
                            needsReview = false
                        )
                    } else if (matchingBankAccounts.size > 1) {
                        val primaryChoice = matchingBankAccounts.firstOrNull { it.isPrimary } ?: matchingBankAccounts.first()
                        return AccountMatchResult(
                            account = primaryChoice,
                            confidence = 0.85,
                            matchReason = "Matched Debit Card ••$last4 to primary ${primaryChoice.bankName} account",
                            needsReview = false
                        )
                    } else if (bankName == "Bank") {
                        // User has only 1 bank account in vault overall and SMS didn't name a bank
                        val allBankAccounts = accounts.filter { !it.isCreditCard }
                        if (allBankAccounts.size == 1) {
                            val singleBankAcc = allBankAccounts.first()
                            return AccountMatchResult(
                                account = singleBankAcc,
                                confidence = 0.90,
                                matchReason = "Matched Debit Card ••$last4 to bank account ${singleBankAcc.name}",
                                needsReview = false
                            )
                        }
                    }
                }

                // If target was a Credit Card or no bank account could be correlated:
                return AccountMatchResult(
                    account = null,
                    confidence = CONFIDENCE_UNKNOWN,
                    matchReason = "SMS mentions ${if (targetIsCard) "Credit Card" else "Account"} ending ••$last4, which does not match any configured account.",
                    needsReview = true
                )
            }
        }

        // 3. Match by Bank/Issuer only (when last 4 digits are not mentioned in SMS)
        val matchingBank = accounts.filter { isBankMatch(it.bankName, bankName) }
        if (matchingBank.size == 1) {
            val singleBankAcc = matchingBank.first()
            val typeMatches = if (targetIsCard) singleBankAcc.isCreditCard else !singleBankAcc.isCreditCard
            if (typeMatches) {
                return AccountMatchResult(
                    account = singleBankAcc,
                    confidence = 0.92,
                    matchReason = "Inferred single ${if (targetIsCard) "Card" else "Account"} for $bankName",
                    needsReview = false
                )
            }
        } else if (matchingBank.size > 1) {
            val filteredByType = matchingBank.filter { if (targetIsCard) it.isCreditCard else !it.isCreditCard }
            if (filteredByType.size == 1) {
                return AccountMatchResult(
                    account = filteredByType.first(),
                    confidence = 0.90,
                    matchReason = "Inferred unique matching ${if (targetIsCard) "Card" else "Account"} for $bankName",
                    needsReview = false
                )
            } else if (filteredByType.size > 1) {
                val defaultChoice = filteredByType.firstOrNull { it.isPrimary } ?: filteredByType.first()
                return AccountMatchResult(
                    account = defaultChoice,
                    confidence = CONFIDENCE_BANK_ONLY,
                    matchReason = "Multiple $bankName accounts found; last 4 digits missing in SMS",
                    needsReview = true
                )
            }
        }

        // 4. If user only has a single Bank Account (or single Credit Card) in total, match it directly!
        val singleTypeAccounts = accounts.filter { if (targetIsCard) it.isCreditCard else !it.isCreditCard }
        if (singleTypeAccounts.size == 1 && bankName != "Unknown Bank" && bankName != "Unknown") {
            val soleAcc = singleTypeAccounts.first()
            if (isBankMatch(soleAcc.bankName, bankName) || bankName == "Bank") {
                return AccountMatchResult(
                    account = soleAcc,
                    confidence = 0.92,
                    matchReason = "Matched user's sole configured ${if (targetIsCard) "Credit Card" else "Bank Account"} (${soleAcc.name})",
                    needsReview = false
                )
            }
        }

        // 5. Ambiguous or completely unknown
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
