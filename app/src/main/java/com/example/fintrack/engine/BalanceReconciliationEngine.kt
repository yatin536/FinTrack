package com.example.fintrack.engine

import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.ReconciliationLog
import java.util.UUID
import kotlin.math.abs

data class ReconciliationResult(
    val accountId: String,
    val ledgerBalance: Double,
    val confirmedBalance: Double,
    val discrepancy: Double,
    val wasAdjusted: Boolean,
    val log: ReconciliationLog?
)

/**
 * Intelligent Balance & Credit Limit Reconciliation Engine.
 * Verifies ledger calculations against official SMS-confirmed figures.
 * When discrepancies occur, creates an auditable record and reconciles the account
 * without altering historical ledger transactions.
 */
class BalanceReconciliationEngine(private val dbHelper: AppDatabaseHelper) {

    /**
     * Reconciles a bank account or credit card against an official SMS confirmed balance/credit.
     */
    fun reconcile(
        account: Account,
        officialBalance: Double,
        smsTimestamp: Long = System.currentTimeMillis()
    ): ReconciliationResult {
        val ledgerBalance = account.currentBalance
        val discrepancy = officialBalance - ledgerBalance

        // If discrepancy is within 1 paisa (0.01), treat as in sync
        if (abs(discrepancy) < 0.01) {
            return ReconciliationResult(
                accountId = account.id,
                ledgerBalance = ledgerBalance,
                confirmedBalance = officialBalance,
                discrepancy = 0.0,
                wasAdjusted = false,
                log = null
            )
        }

        // Create an audit log record
        val log = ReconciliationLog(
            id = UUID.randomUUID().toString(),
            accountId = account.id,
            ledgerBalance = ledgerBalance,
            confirmedBalance = officialBalance,
            discrepancy = discrepancy,
            adjustmentAmount = discrepancy,
            timestamp = smsTimestamp,
            note = if (account.isCreditCard) {
                "Reconciled card outstanding from ₹${String.format("%.2f", ledgerBalance)} to confirmed ₹${String.format("%.2f", officialBalance)}"
            } else {
                "Reconciled bank balance from ₹${String.format("%.2f", ledgerBalance)} to official Avl Bal ₹${String.format("%.2f", officialBalance)}"
            }
        )

        // Record in database and update account's current balance
        dbHelper.recordReconciliation(log)
        dbHelper.updateCurrentBalance(account.id, officialBalance)

        return ReconciliationResult(
            accountId = account.id,
            ledgerBalance = ledgerBalance,
            confirmedBalance = officialBalance,
            discrepancy = discrepancy,
            wasAdjusted = true,
            log = log
        )
    }
}
