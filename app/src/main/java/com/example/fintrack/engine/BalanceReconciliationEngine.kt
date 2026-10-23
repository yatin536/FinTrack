package com.example.fintrack.engine

import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.Money
import com.example.fintrack.data.model.ReconciliationLog
import java.util.UUID

data class ReconciliationResult(
    val accountId: String,
    val ledgerBalance: Double,
    val confirmedBalance: Double,
    val discrepancy: Double,
    val wasAdjusted: Boolean,
    val log: ReconciliationLog?
) {
    val ledgerBalanceMoney: Money get() = Money.fromDouble(ledgerBalance)
    val confirmedBalanceMoney: Money get() = Money.fromDouble(confirmedBalance)
    val discrepancyMoney: Money get() = Money.fromDouble(discrepancy)
}

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
        val ledgerMoney = account.currentBalanceMoney
        val officialMoney = Money.fromDouble(officialBalance)
        val discrepancyMoney = officialMoney - ledgerMoney

        // If discrepancy is exactly 0 paise, treat as completely in sync
        if (discrepancyMoney.isZero) {
            return ReconciliationResult(
                accountId = account.id,
                ledgerBalance = ledgerMoney.asDouble,
                confirmedBalance = officialMoney.asDouble,
                discrepancy = 0.0,
                wasAdjusted = false,
                log = null
            )
        }

        // Create an audit log record
        val log = ReconciliationLog(
            id = UUID.randomUUID().toString(),
            accountId = account.id,
            ledgerBalance = ledgerMoney.asDouble,
            confirmedBalance = officialMoney.asDouble,
            discrepancy = discrepancyMoney.asDouble,
            adjustmentAmount = discrepancyMoney.asDouble,
            timestamp = smsTimestamp,
            note = if (account.isCreditCard) {
                "Reconciled card outstanding from ${ledgerMoney.format()} to confirmed ${officialMoney.format()}"
            } else {
                "Reconciled bank balance from ${ledgerMoney.format()} to official Avl Bal ${officialMoney.format()}"
            }
        )

        // Record in database and update account's current balance
        dbHelper.recordReconciliation(log)
        dbHelper.updateCurrentBalance(account.id, officialMoney.asDouble)

        return ReconciliationResult(
            accountId = account.id,
            ledgerBalance = ledgerMoney.asDouble,
            confirmedBalance = officialMoney.asDouble,
            discrepancy = discrepancyMoney.asDouble,
            wasAdjusted = true,
            log = log
        )
    }
}
