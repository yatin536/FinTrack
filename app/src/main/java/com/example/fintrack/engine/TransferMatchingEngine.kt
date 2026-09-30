package com.example.fintrack.engine

import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.parser.ParsedTransaction
import kotlin.math.abs

data class TransferMatchResult(
    val isTransfer: Boolean,
    val sourceAccount: Account?,
    val destinationAccount: Account?,
    val matchedTransactionId: String? = null
)

/**
 * Intelligent Transfer Matching Engine.
 * Matches bank-to-bank transfers across user accounts to prevent false double-counting
 * of expenses and income when moving money between owned accounts.
 */
class TransferMatchingEngine(private val dbHelper: AppDatabaseHelper) {

    companion object {
        // 10-minute window for matching paired debit/credit transfer alerts
        const val TRANSFER_WINDOW_MS = 10 * 60 * 1000L
    }

    /**
     * Attempts to match an incoming transaction with an existing peer transfer transaction.
     */
    fun matchTransfer(
        parsed: ParsedTransaction,
        targetAccount: Account,
        smsBody: String
    ): TransferMatchResult {
        val allAccounts = dbHelper.getAccounts()
        val bankAccounts = allAccounts.filter { it.accountType != AccountType.CREDIT_CARD }

        // Check if SMS text explicitly mentions another owned account's last 4 digits
        for (other in bankAccounts) {
            if (other.id != targetAccount.id && other.accountNumberLast4.isNotEmpty()) {
                if (smsBody.contains(other.accountNumberLast4)) {
                    val (source, dest) = if (parsed.direction == TransactionDirection.DEBIT) {
                        Pair(targetAccount, other)
                    } else {
                        Pair(other, targetAccount)
                    }
                    return TransferMatchResult(
                        isTransfer = true,
                        sourceAccount = source,
                        destinationAccount = dest
                    )
                }
            }
        }

        // Look for recent pending transfer of the exact same amount in the last 10 minutes
        val recentTxns = dbHelper.getTransactionsWithDetails(limit = 30)
        val oppositeDirection = if (parsed.direction == TransactionDirection.DEBIT) {
            TransactionDirection.CREDIT
        } else {
            TransactionDirection.DEBIT
        }

        val peerCandidate = recentTxns.firstOrNull { item ->
            val txn = item.transaction
            val isDiffAccount = txn.accountId != targetAccount.id
            val isSameAmount = abs(txn.amount - parsed.amount) < 0.01
            val isInWindow = abs(txn.timestamp - parsed.timestamp) <= TRANSFER_WINDOW_MS
            val isOpposite = txn.direction == oppositeDirection
            val refMatch = !parsed.referenceNumber.isNullOrBlank() && txn.referenceNumber == parsed.referenceNumber

            isDiffAccount && isSameAmount && isInWindow && (refMatch || isOpposite)
        }

        if (peerCandidate != null) {
            val peerTxn = peerCandidate.transaction
            val (source, dest) = if (parsed.direction == TransactionDirection.DEBIT) {
                Pair(targetAccount, peerCandidate.account)
            } else {
                Pair(peerCandidate.account, targetAccount)
            }

            return TransferMatchResult(
                isTransfer = true,
                sourceAccount = source,
                destinationAccount = dest,
                matchedTransactionId = peerTxn.id
            )
        }

        return TransferMatchResult(
            isTransfer = false,
            sourceAccount = null,
            destinationAccount = null
        )
    }
}
