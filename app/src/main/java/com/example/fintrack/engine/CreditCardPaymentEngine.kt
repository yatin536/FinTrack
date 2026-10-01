package com.example.fintrack.engine

import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.data.model.VerificationEvent
import com.example.fintrack.data.model.VerificationEventType
import com.example.fintrack.data.model.VerificationStatus
import com.example.fintrack.parser.ParsedTransaction
import java.util.UUID

data class PaymentMatchResult(
    val creditCard: Account,
    val sourceBankAccount: Account?,
    val needsSourceConfirmation: Boolean,
    val transaction: Transaction,
    val wasReconciledWithManual: Boolean = false,
    val reconciliationMessage: String? = null
)

/**
 * Intelligent Credit Card Bill Payment Engine.
 * Resolves credit card bill payment relationships between bank accounts and cards.
 * Reconciles incoming SMS with manual "Bill Paid" records to eliminate duplicate entries.
 * Prompts verification when source accounts or matching cards are ambiguous.
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
        // Step 1: Check if this SMS reconciles with a recent manual "Bill Paid" action
        val existingManualPayment = dbHelper?.findMatchingRecentManualPayment(creditCard.id, parsed.amount)
        if (existingManualPayment != null) {
            // Reconcile and update the existing manual entry
            dbHelper.reconcilePaymentWithSms(
                manualTxnId = existingManualPayment.id,
                smsRefNo = parsed.referenceNumber,
                rawSms = rawSmsBody,
                newAvailCredit = parsed.availableCredit,
                userId = creditCard.userId
            )

            val reconciledTxn = existingManualPayment.copy(
                rawSmsBody = rawSmsBody,
                referenceNumber = parsed.referenceNumber ?: existingManualPayment.referenceNumber,
                availableCreditAfterTxn = parsed.availableCredit ?: existingManualPayment.availableCreditAfterTxn,
                needsReview = false,
                reviewReason = null
            )

            return PaymentMatchResult(
                creditCard = creditCard,
                sourceBankAccount = allAccounts.firstOrNull { it.id == existingManualPayment.sourceAccountId },
                needsSourceConfirmation = false,
                transaction = reconciledTxn,
                wasReconciledWithManual = true,
                reconciliationMessage = "Payment of ₹${"%.2f".format(parsed.amount)} reconciled with your previous manual bill payment confirmation."
            )
        }

        // Step 2: Resolve source bank account
        val bankAccounts = allAccounts.filter { it.accountType != AccountType.CREDIT_CARD }

        // Check if SMS mentions specific source bank account number
        var sourceAcc: Account? = null
        for (acc in bankAccounts) {
            if (acc.accountNumberLast4.isNotEmpty() && rawSmsBody.contains(acc.accountNumberLast4)) {
                if (acc.accountNumberLast4 != creditCard.accountNumberLast4) {
                    sourceAcc = acc
                    break
                }
            }
        }

        // Check single linked preferred payment account
        if (sourceAcc == null && creditCard.linkedPaymentAccountIds.isNotEmpty()) {
            if (creditCard.linkedPaymentAccountIds.size == 1) {
                sourceAcc = bankAccounts.firstOrNull { it.id == creditCard.linkedPaymentAccountIds.first() }
            }
        }

        // If source account still not identified, generate a VerificationEvent instead of guessing
        val needsConfirmation = sourceAcc == null

        val txnId = UUID.randomUUID().toString()
        val txn = Transaction(
            id = txnId,
            userId = creditCard.userId,
            accountId = creditCard.id,
            sourceAccountId = sourceAcc?.id,
            destinationAccountId = creditCard.id,
            categoryId = "cat_bills",
            amount = parsed.amount,
            direction = TransactionDirection.CREDIT,
            kind = TransactionKind.CARD_PAYMENT,
            timestamp = parsed.timestamp,
            merchant = "Payment Received - ${creditCard.name}",
            rawSmsBody = rawSmsBody,
            referenceNumber = parsed.referenceNumber,
            availableCreditAfterTxn = parsed.availableCredit,
            needsReview = needsConfirmation,
            reviewReason = if (needsConfirmation) "Source bank account needs confirmation" else null
        )

        if (needsConfirmation && dbHelper != null) {
            val verificationEvent = VerificationEvent(
                userId = creditCard.userId,
                eventType = VerificationEventType.CREDIT_CARD_PAYMENT,
                title = "Payment to ${creditCard.name}",
                description = "Received SMS confirmation for payment of ₹${"%.2f".format(parsed.amount)} towards card ending in ${creditCard.accountNumberLast4}. Please confirm which bank account was used.",
                amount = parsed.amount,
                accountLast4 = creditCard.accountNumberLast4,
                status = VerificationStatus.PENDING,
                confidence = 0.5,
                transactionId = txnId,
                accountId = creditCard.id
            )
            dbHelper.insertVerificationEvent(verificationEvent)
        }

        return PaymentMatchResult(
            creditCard = creditCard,
            sourceBankAccount = sourceAcc,
            needsSourceConfirmation = needsConfirmation,
            transaction = txn,
            wasReconciledWithManual = false
        )
    }
}
