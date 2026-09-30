package com.example.fintrack.engine

import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.parser.ParsedTransaction
import java.util.UUID

data class PaymentMatchResult(
    val creditCard: Account,
    val sourceBankAccount: Account?,
    val needsSourceConfirmation: Boolean,
    val transaction: Transaction
)

/**
 * Intelligent Credit Card Bill Payment Engine.
 * Resolves credit card bill payment relationships between bank accounts and cards.
 * Never guesses the source bank account when ambiguous; places in Needs Confirmation.
 */
class CreditCardPaymentEngine(private val dbHelper: AppDatabaseHelper? = null) {

    /**
     * Processes an incoming parsed credit card bill payment SMS.
     */
    fun processPaymentSms(
        parsed: ParsedTransaction,
        creditCard: Account,
        rawSmsBody: String,
        allAccounts: List<Account> = dbHelper?.getAccounts() ?: emptyList()
    ): PaymentMatchResult {
        val bankAccounts = allAccounts.filter { it.accountType != AccountType.CREDIT_CARD }

        // Attempt 1: Check if the SMS body explicitly contains another account number (e.g. from A/c XX1234)
        var sourceAcc: Account? = null
        for (acc in bankAccounts) {
            if (acc.accountNumberLast4.isNotEmpty() && rawSmsBody.contains(acc.accountNumberLast4)) {
                // If it's different from the CC number itself
                if (acc.accountNumberLast4 != creditCard.accountNumberLast4) {
                    sourceAcc = acc
                    break
                }
            }
        }

        // Attempt 2: Check linked preferred payment accounts on the credit card
        if (sourceAcc == null && creditCard.linkedPaymentAccountIds.isNotEmpty()) {
            if (creditCard.linkedPaymentAccountIds.size == 1) {
                // Exactly one preferred payment account configured by user
                sourceAcc = bankAccounts.firstOrNull { it.id == creditCard.linkedPaymentAccountIds.first() }
            }
        }

        // If source account still not identified, do NOT guess. Mark as needing review.
        val needsConfirmation = sourceAcc == null

        val txn = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = creditCard.id,
            sourceAccountId = sourceAcc?.id,
            destinationAccountId = creditCard.id,
            categoryId = "cat_bills",
            amount = parsed.amount,
            direction = TransactionDirection.CREDIT, // Credit to card reducing outstanding
            kind = TransactionKind.CARD_PAYMENT,
            timestamp = parsed.timestamp,
            merchant = "Payment to ${creditCard.name}",
            rawSmsBody = rawSmsBody,
            referenceNumber = parsed.referenceNumber,
            availableCreditAfterTxn = parsed.availableCredit,
            needsReview = needsConfirmation,
            reviewReason = if (needsConfirmation) "Source bank account for card payment needs confirmation" else null
        )

        return PaymentMatchResult(
            creditCard = creditCard,
            sourceBankAccount = sourceAcc,
            needsSourceConfirmation = needsConfirmation,
            transaction = txn
        )
    }
}
