package com.example.fintrack.data.repository

import android.content.Context
import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.Category
import com.example.fintrack.data.model.DashboardSummary
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionType
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.parser.ExpenseCategorizer
import com.example.fintrack.parser.IndianBankSmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

class TransactionRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)
    private val categorizer = ExpenseCategorizer(dbHelper)

    fun getDashboardSummary(period: TimePeriod): Flow<DashboardSummary> = flow {
        // Emit initial
        emit(dbHelper.getDashboardSummary(period))
        // Emit whenever database change signal fires
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getDashboardSummary(period))
        }
    }.flowOn(Dispatchers.IO)

    fun getTransactions(): Flow<List<TransactionWithDetails>> = flow {
        emit(dbHelper.getTransactionsWithDetails())
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getTransactionsWithDetails())
        }
    }.flowOn(Dispatchers.IO)

    fun getAccounts(): Flow<List<Account>> = flow {
        emit(dbHelper.getAccounts())
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getAccounts())
        }
    }.flowOn(Dispatchers.IO)

    fun getCategories(): Flow<List<Category>> = flow {
        emit(dbHelper.getCategories())
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getCategories())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun addManualTransaction(
        accountId: String,
        categoryId: String,
        amount: Double,
        type: TransactionType,
        merchant: String,
        note: String? = null
    ): Boolean {
        val txn = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            type = type,
            timestamp = System.currentTimeMillis(),
            merchant = merchant.ifBlank { "Manual ${type.name.lowercase().replaceFirstChar { it.uppercase() }}" },
            isManual = true,
            note = note
        )
        return dbHelper.insertTransaction(txn)
    }

    suspend fun updateInitialBalance(accountId: String, newBalance: Double): Boolean {
        return dbHelper.updateInitialBalance(accountId, newBalance)
    }

    suspend fun updateTransactionCategory(
        transactionId: String,
        newCategoryId: String,
        merchant: String?
    ): Boolean {
        return dbHelper.updateTransactionCategory(transactionId, newCategoryId, merchant)
    }

    suspend fun deleteTransaction(transactionId: String): Boolean {
        return dbHelper.deleteTransaction(transactionId)
    }

    suspend fun updateAccount(account: Account): Boolean {
        return dbHelper.updateAccount(account)
    }

    suspend fun addAccount(account: Account): Boolean {
        return dbHelper.insertAccount(account)
    }

    suspend fun addCategory(category: Category): Boolean {
        return dbHelper.insertCategory(category)
    }

    /**
     * Diagnostic and testing helper: simulates an incoming SMS as if received from the bank.
     */
    suspend fun simulateIncomingSms(sender: String, body: String): Boolean {
        val parsed = IndianBankSmsParser.parse(sender, body) ?: return false
        val catId = categorizer.categorize(parsed.merchant, body, parsed.type)

        var account = dbHelper.findAccountByBankAndLast4(parsed.bankName, parsed.accountNumberLast4)
        if (account == null) {
            val newAcc = Account(
                id = UUID.randomUUID().toString(),
                name = "${parsed.bankName} Account",
                bankName = parsed.bankName,
                accountNumberLast4 = parsed.accountNumberLast4,
                initialBalance = 0.0,
                colorHex = 0xFF1976D2
            )
            dbHelper.insertAccount(newAcc)
            account = newAcc
        }

        val txn = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = account.id,
            categoryId = catId,
            amount = parsed.amount,
            type = parsed.type,
            timestamp = parsed.timestamp,
            merchant = parsed.merchant,
            rawSmsBody = body,
            smsSender = sender,
            referenceNumber = parsed.referenceNumber,
            balanceAfterTxn = parsed.balanceAfterTxn,
            isManual = false
        )
        return dbHelper.insertTransaction(txn)
    }
}
