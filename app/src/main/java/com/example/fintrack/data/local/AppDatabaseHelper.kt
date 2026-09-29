package com.example.fintrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.Category
import com.example.fintrack.data.model.CategorySpend
import com.example.fintrack.data.model.DashboardSummary
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionType
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.data.model.TrendPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * SQLite Database Manager with field-level AES-256-GCM hardware encryption.
 * Encrypts sensitive financial data (amounts, merchants, balances, raw SMS)
 * before persisting to device storage.
 */
class AppDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "fintrack_secure.db"
        const val DATABASE_VERSION = 2

        @Volatile
        private var instance: AppDatabaseHelper? = null

        fun getInstance(context: Context): AppDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AppDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }

        // Tables
        private const val TABLE_ACCOUNTS = "accounts"
        private const val TABLE_CATEGORIES = "categories"
        private const val TABLE_TRANSACTIONS = "transactions"
        private const val TABLE_MERCHANT_RULES = "merchant_rules"

        // Accounts Columns
        private const val COL_ACC_ID = "id"
        private const val COL_ACC_NAME = "name"
        private const val COL_ACC_BANK = "bank_name"
                private const val COL_ACC_LAST4 = "account_number_last4"
        private const val COL_ACC_TYPE = "account_type"
        private const val COL_ACC_CREDIT_LIMIT = "credit_limit" // Encrypted
        private const val COL_ACC_INIT_BAL = "initial_balance" // Encrypted
        private const val COL_ACC_COLOR = "color_hex"
        private const val COL_ACC_IS_PRIMARY = "is_primary"
        private const val COL_ACC_CREATED = "created_at"

        // Categories Columns
        private const val COL_CAT_ID = "id"
        private const val COL_CAT_NAME = "name"
        private const val COL_CAT_ICON = "icon_name"
        private const val COL_CAT_COLOR = "color_hex"
        private const val COL_CAT_IS_INCOME = "is_income"
        private const val COL_CAT_KEYWORDS = "keywords"

        // Transactions Columns
        private const val COL_TXN_ID = "id"
        private const val COL_TXN_ACC_ID = "account_id"
        private const val COL_TXN_CAT_ID = "category_id"
        private const val COL_TXN_AMOUNT = "amount" // Encrypted
        private const val COL_TXN_TYPE = "type" // DEBIT / CREDIT
        private const val COL_TXN_TIMESTAMP = "timestamp"
        private const val COL_TXN_MERCHANT = "merchant" // Encrypted
        private const val COL_TXN_RAW_SMS = "raw_sms_body" // Encrypted
        private const val COL_TXN_SMS_SENDER = "sms_sender"
        private const val COL_TXN_REF_NO = "reference_number"
        private const val COL_TXN_BAL_AFTER = "balance_after_txn" // Encrypted
        private const val COL_TXN_IS_MANUAL = "is_manual"
        private const val COL_TXN_NOTE = "note" // Encrypted
        private const val COL_TXN_CREATED = "created_at"

        // Rules Columns
        private const val COL_RULE_ID = "id"
        private const val COL_RULE_KEYWORD = "merchant_keyword"
        private const val COL_RULE_CAT_ID = "category_id"
        private const val COL_RULE_CONFIRMED = "user_confirmed"
    }

    private val _dbChangeSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dbChangeSignal: SharedFlow<Unit> = _dbChangeSignal.asSharedFlow()

    private fun notifyDataChanged() {
        _dbChangeSignal.tryEmit(Unit)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Accounts table
        db.execSQL(
            """
            CREATE TABLE $TABLE_ACCOUNTS (
                $COL_ACC_ID TEXT PRIMARY KEY,
                $COL_ACC_NAME TEXT NOT NULL,
                $COL_ACC_BANK TEXT NOT NULL,
                $COL_ACC_TYPE TEXT NOT NULL DEFAULT 'BANK',
                $COL_ACC_LAST4 TEXT,
                $COL_ACC_INIT_BAL TEXT NOT NULL,
                $COL_ACC_CREDIT_LIMIT TEXT NOT NULL,
                $COL_ACC_COLOR INTEGER NOT NULL,
                $COL_ACC_IS_PRIMARY INTEGER NOT NULL DEFAULT 0,
                $COL_ACC_CREATED INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Categories table
        db.execSQL(
            """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_ID TEXT PRIMARY KEY,
                $COL_CAT_NAME TEXT NOT NULL,
                $COL_CAT_ICON TEXT NOT NULL,
                $COL_CAT_COLOR INTEGER NOT NULL,
                $COL_CAT_IS_INCOME INTEGER NOT NULL DEFAULT 0,
                $COL_CAT_KEYWORDS TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Transactions table with indexes
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_TXN_ID TEXT PRIMARY KEY,
                $COL_TXN_ACC_ID TEXT NOT NULL,
                $COL_TXN_CAT_ID TEXT NOT NULL,
                $COL_TXN_AMOUNT TEXT NOT NULL,
                $COL_TXN_TYPE TEXT NOT NULL,
                $COL_TXN_TIMESTAMP INTEGER NOT NULL,
                $COL_TXN_MERCHANT TEXT NOT NULL,
                $COL_TXN_RAW_SMS TEXT,
                $COL_TXN_SMS_SENDER TEXT,
                $COL_TXN_REF_NO TEXT,
                $COL_TXN_BAL_AFTER TEXT,
                $COL_TXN_IS_MANUAL INTEGER NOT NULL DEFAULT 0,
                $COL_TXN_NOTE TEXT,
                $COL_TXN_CREATED INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_txn_time ON $TABLE_TRANSACTIONS ($COL_TXN_TIMESTAMP DESC)")
        db.execSQL("CREATE INDEX idx_txn_acc ON $TABLE_TRANSACTIONS ($COL_TXN_ACC_ID)")
        db.execSQL("CREATE INDEX idx_txn_cat ON $TABLE_TRANSACTIONS ($COL_TXN_CAT_ID)")

        // Merchant categorization rules
        db.execSQL(
            """
            CREATE TABLE $TABLE_MERCHANT_RULES (
                $COL_RULE_ID TEXT PRIMARY KEY,
                $COL_RULE_KEYWORD TEXT UNIQUE NOT NULL,
                $COL_RULE_CAT_ID TEXT NOT NULL,
                $COL_RULE_CONFIRMED INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        // Seed default categories
        seedDefaultCategories(db)

        // Seed default Primary Account (e.g. Primary Bank)
        seedDefaultAccount(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_ACCOUNTS ADD COLUMN $COL_ACC_TYPE TEXT NOT NULL DEFAULT 'BANK'")
            val encryptedZero = SecurityManager.encryptDouble(0.0)
            db.execSQL("ALTER TABLE $TABLE_ACCOUNTS ADD COLUMN $COL_ACC_CREDIT_LIMIT TEXT NOT NULL DEFAULT '$encryptedZero'")
        }
    }

    private fun seedDefaultCategories(db: SQLiteDatabase) {
        for (cat in Category.DEFAULT_CATEGORIES) {
            val values = ContentValues().apply {
                put(COL_CAT_ID, cat.id)
                put(COL_CAT_NAME, cat.name)
                put(COL_CAT_ICON, cat.iconName)
                put(COL_CAT_COLOR, cat.colorHex)
                put(COL_CAT_IS_INCOME, if (cat.isIncome) 1 else 0)
                put(COL_CAT_KEYWORDS, cat.keywords.joinToString(","))
            }
            db.insert(TABLE_CATEGORIES, null, values)
        }
    }

    private fun seedDefaultAccount(db: SQLiteDatabase) {
        val defaultAccount = Account(
            id = "default_primary_account",
            name = "Primary Bank Account",
            bankName = "General",
            accountType = AccountType.BANK,
            accountNumberLast4 = "",
            creditLimit = 0.0,
            initialBalance = 0.0,
            currentBalance = 0.0,
            colorHex = 0xFF1E88E5,
            isPrimary = true
        )
        val values = ContentValues().apply {
            put(COL_ACC_ID, defaultAccount.id)
            put(COL_ACC_NAME, defaultAccount.name)
            put(COL_ACC_BANK, defaultAccount.bankName)
            put(COL_ACC_TYPE, defaultAccount.accountType.name)
            put(COL_ACC_LAST4, defaultAccount.accountNumberLast4)
            put(COL_ACC_CREDIT_LIMIT, SecurityManager.encryptDouble(defaultAccount.creditLimit))
            put(COL_ACC_INIT_BAL, SecurityManager.encryptDouble(defaultAccount.initialBalance))
            put(COL_ACC_COLOR, defaultAccount.colorHex)
            put(COL_ACC_IS_PRIMARY, 1)
            put(COL_ACC_CREATED, defaultAccount.createdAt)
        }
        db.insert(TABLE_ACCOUNTS, null, values)
    }

    // ==========================================
    // ACCOUNT OPERATIONS
    // ==========================================

    fun insertAccount(account: Account): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ACC_ID, account.id)
            put(COL_ACC_NAME, account.name)
            put(COL_ACC_BANK, account.bankName)
            put(COL_ACC_TYPE, account.accountType.name)
            put(COL_ACC_LAST4, account.accountNumberLast4)
            put(COL_ACC_CREDIT_LIMIT, SecurityManager.encryptDouble(account.creditLimit))
            put(COL_ACC_INIT_BAL, SecurityManager.encryptDouble(account.initialBalance))
            put(COL_ACC_COLOR, account.colorHex)
            put(COL_ACC_IS_PRIMARY, if (account.isPrimary) 1 else 0)
            put(COL_ACC_CREATED, account.createdAt)
        }
        val result = db.insertWithOnConflict(TABLE_ACCOUNTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        notifyDataChanged()
        return result != -1L
    }

    fun updateInitialBalance(accountId: String, newBalance: Double): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ACC_INIT_BAL, SecurityManager.encryptDouble(newBalance))
        }
        val rows = db.update(TABLE_ACCOUNTS, values, "$COL_ACC_ID = ?", arrayOf(accountId))
        notifyDataChanged()
        return rows > 0
    }

    fun getAccounts(): List<Account> {
        val db = readableDatabase
        val accounts = mutableListOf<Account>()
        val cursor: Cursor = db.query(TABLE_ACCOUNTS, null, null, null, null, null, "$COL_ACC_IS_PRIMARY DESC, $COL_ACC_CREATED ASC")
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_ACC_ID))
                val name = c.getString(c.getColumnIndexOrThrow(COL_ACC_NAME))
                val bank = c.getString(c.getColumnIndexOrThrow(COL_ACC_BANK))
                val typeStr = c.getString(c.getColumnIndexOrThrow(COL_ACC_TYPE)) ?: "BANK"
                val accountType = try { AccountType.valueOf(typeStr) } catch(e: Exception) { AccountType.BANK }
                val last4 = c.getString(c.getColumnIndexOrThrow(COL_ACC_LAST4)) ?: ""
                val encryptedInitBal = c.getString(c.getColumnIndexOrThrow(COL_ACC_INIT_BAL))
                val encryptedLimit = c.getString(c.getColumnIndexOrThrow(COL_ACC_CREDIT_LIMIT))
                val creditLimit = if (encryptedLimit != null) SecurityManager.decryptDouble(encryptedLimit, 0.0) else 0.0
                val initBal = SecurityManager.decryptDouble(encryptedInitBal, 0.0)
                val color = c.getLong(c.getColumnIndexOrThrow(COL_ACC_COLOR))
                val isPrimary = c.getInt(c.getColumnIndexOrThrow(COL_ACC_IS_PRIMARY)) == 1
                val created = c.getLong(c.getColumnIndexOrThrow(COL_ACC_CREATED))

                // Calculate current balance: Initial + Sum(Credits) - Sum(Debits)
                val currentBal = calculateAccountBalance(id, initBal)

                accounts.add(
                    Account(
                        id = id,
                        name = name,
                        bankName = bank,
                        accountType = accountType,
                        accountNumberLast4 = last4,
                        creditLimit = creditLimit,
                        initialBalance = initBal,
                        currentBalance = currentBal,
                        colorHex = color,
                        isPrimary = isPrimary,
                        createdAt = created
                    )
                )
            }
        }
        return accounts
    }

    fun getAccountById(accountId: String): Account? {
        return getAccounts().firstOrNull { it.id == accountId }
    }

    fun findAccountByBankAndLast4(bankName: String, last4: String): Account? {
        val accounts = getAccounts()
        if (last4.isNotEmpty()) {
            val match = accounts.firstOrNull { it.accountNumberLast4 == last4 }
            if (match != null) return match
        }
        return accounts.firstOrNull { it.bankName.equals(bankName, ignoreCase = true) }
            ?: accounts.firstOrNull { it.isPrimary }
            ?: accounts.firstOrNull()
    }

    private fun calculateAccountBalance(accountId: String, initialBalance: Double): Double {
        val db = readableDatabase
        var balance = initialBalance
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            arrayOf(COL_TXN_AMOUNT, COL_TXN_TYPE),
            "$COL_TXN_ACC_ID = ?",
            arrayOf(accountId),
            null,
            null,
            null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val encryptedAmount = c.getString(c.getColumnIndexOrThrow(COL_TXN_AMOUNT))
                val type = c.getString(c.getColumnIndexOrThrow(COL_TXN_TYPE))
                val amount = SecurityManager.decryptDouble(encryptedAmount, 0.0)
                if (type == TransactionType.CREDIT.name) {
                    balance += amount
                } else {
                    balance -= amount
                }
            }
        }
        return balance
    }

    // ==========================================
    // CATEGORY OPERATIONS
    // ==========================================

    fun getCategories(): List<Category> {
        val db = readableDatabase
        val categories = mutableListOf<Category>()
        val cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, "$COL_CAT_NAME ASC")
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_CAT_ID))
                val name = c.getString(c.getColumnIndexOrThrow(COL_CAT_NAME))
                val icon = c.getString(c.getColumnIndexOrThrow(COL_CAT_ICON))
                val color = c.getLong(c.getColumnIndexOrThrow(COL_CAT_COLOR))
                val isIncome = c.getInt(c.getColumnIndexOrThrow(COL_CAT_IS_INCOME)) == 1
                val keywordsStr = c.getString(c.getColumnIndexOrThrow(COL_CAT_KEYWORDS)) ?: ""
                val keywords = if (keywordsStr.isEmpty()) emptyList() else keywordsStr.split(",")

                categories.add(Category(id, name, icon, color, isIncome, keywords))
            }
        }
        return categories
    }

    fun insertCategory(category: Category): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CAT_ID, category.id)
            put(COL_CAT_NAME, category.name)
            put(COL_CAT_ICON, category.iconName)
            put(COL_CAT_COLOR, category.colorHex)
            put(COL_CAT_IS_INCOME, if (category.isIncome) 1 else 0)
            put(COL_CAT_KEYWORDS, category.keywords.joinToString(","))
        }
        val result = db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        notifyDataChanged()
        return result != -1L
    }

    // ==========================================
    // TRANSACTION OPERATIONS
    // ==========================================

    fun insertTransaction(transaction: Transaction): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TXN_ID, transaction.id)
            put(COL_TXN_ACC_ID, transaction.accountId)
            put(COL_TXN_CAT_ID, transaction.categoryId)
            put(COL_TXN_AMOUNT, SecurityManager.encryptDouble(transaction.amount))
            put(COL_TXN_TYPE, transaction.type.name)
            put(COL_TXN_TIMESTAMP, transaction.timestamp)
            put(COL_TXN_MERCHANT, SecurityManager.encrypt(transaction.merchant))
            put(COL_TXN_RAW_SMS, SecurityManager.encrypt(transaction.rawSmsBody))
            put(COL_TXN_SMS_SENDER, transaction.smsSender)
            put(COL_TXN_REF_NO, transaction.referenceNumber)
            put(
                COL_TXN_BAL_AFTER,
                transaction.balanceAfterTxn?.let { SecurityManager.encryptDouble(it) }
            )
            put(COL_TXN_IS_MANUAL, if (transaction.isManual) 1 else 0)
            put(COL_TXN_NOTE, SecurityManager.encrypt(transaction.note))
            put(COL_TXN_CREATED, transaction.createdAt)
        }
        val res = db.insert(TABLE_TRANSACTIONS, null, values)
        notifyDataChanged()
        return res != -1L
    }

    fun updateTransactionCategory(transactionId: String, newCategoryId: String, merchant: String? = null): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TXN_CAT_ID, newCategoryId)
        }
        val rows = db.update(TABLE_TRANSACTIONS, values, "$COL_TXN_ID = ?", arrayOf(transactionId))

        // Save self-learning rule for this merchant
        if (!merchant.isNullOrBlank()) {
            saveMerchantRule(merchant.trim(), newCategoryId)
        }

        notifyDataChanged()
        return rows > 0
    }

    fun deleteTransaction(transactionId: String): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_TRANSACTIONS, "$COL_TXN_ID = ?", arrayOf(transactionId))
        notifyDataChanged()
        return rows > 0
    }

    fun getTransactionsWithDetails(limit: Int = 100): List<TransactionWithDetails> {
        val db = readableDatabase
        val accountsMap = getAccounts().associateBy { it.id }
        val categoriesMap = getCategories().associateBy { it.id }
        val defaultCategory = categoriesMap["cat_other"] ?: Category.DEFAULT_CATEGORIES.last()
        val defaultAccount = accountsMap.values.firstOrNull { it.isPrimary } ?: accountsMap.values.first()

        val list = mutableListOf<TransactionWithDetails>()
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TXN_TIMESTAMP DESC",
            limit.toString()
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_TXN_ID))
                val accId = c.getString(c.getColumnIndexOrThrow(COL_TXN_ACC_ID))
                val catId = c.getString(c.getColumnIndexOrThrow(COL_TXN_CAT_ID))
                val encAmount = c.getString(c.getColumnIndexOrThrow(COL_TXN_AMOUNT))
                val amount = SecurityManager.decryptDouble(encAmount, 0.0)
                val typeStr = c.getString(c.getColumnIndexOrThrow(COL_TXN_TYPE))
                val type = if (typeStr == TransactionType.CREDIT.name) TransactionType.CREDIT else TransactionType.DEBIT
                val timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TXN_TIMESTAMP))
                val encMerchant = c.getString(c.getColumnIndexOrThrow(COL_TXN_MERCHANT))
                val merchant = SecurityManager.decrypt(encMerchant) ?: "Unknown"
                val encRawSms = c.getString(c.getColumnIndexOrThrow(COL_TXN_RAW_SMS))
                val rawSms = SecurityManager.decrypt(encRawSms)
                val sender = c.getString(c.getColumnIndexOrThrow(COL_TXN_SMS_SENDER))
                val refNo = c.getString(c.getColumnIndexOrThrow(COL_TXN_REF_NO))
                val encBalAfter = c.getString(c.getColumnIndexOrThrow(COL_TXN_BAL_AFTER))
                val balAfter = encBalAfter?.let { SecurityManager.decryptDouble(it) }
                val isManual = c.getInt(c.getColumnIndexOrThrow(COL_TXN_IS_MANUAL)) == 1
                val encNote = c.getString(c.getColumnIndexOrThrow(COL_TXN_NOTE))
                val note = SecurityManager.decrypt(encNote)
                val created = c.getLong(c.getColumnIndexOrThrow(COL_TXN_CREATED))

                val txn = Transaction(
                    id = id,
                    accountId = accId,
                    categoryId = catId,
                    amount = amount,
                    type = type,
                    timestamp = timestamp,
                    merchant = merchant,
                    rawSmsBody = rawSms,
                    smsSender = sender,
                    referenceNumber = refNo,
                    balanceAfterTxn = balAfter,
                    isManual = isManual,
                    note = note,
                    createdAt = created
                )

                val account = accountsMap[accId] ?: defaultAccount
                val category = categoriesMap[catId] ?: defaultCategory

                list.add(TransactionWithDetails(txn, account, category))
            }
        }
        return list
    }

    // ==========================================
    // MERCHANT RULES (LOCAL SELF-LEARNING AI)
    // ==========================================

    fun saveMerchantRule(merchantKeyword: String, categoryId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_RULE_ID, UUID.randomUUID().toString())
            put(COL_RULE_KEYWORD, merchantKeyword.lowercase().trim())
            put(COL_RULE_CAT_ID, categoryId)
            put(COL_RULE_CONFIRMED, 1)
        }
        db.insertWithOnConflict(TABLE_MERCHANT_RULES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCategoryForMerchantRule(merchant: String): String? {
        val db = readableDatabase
        val clean = merchant.lowercase().trim()
        val cursor = db.query(
            TABLE_MERCHANT_RULES,
            arrayOf(COL_RULE_CAT_ID, COL_RULE_KEYWORD),
            null,
            null,
            null,
            null,
            null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val keyword = c.getString(c.getColumnIndexOrThrow(COL_RULE_KEYWORD))
                if (clean.contains(keyword) || keyword.contains(clean)) {
                    return c.getString(c.getColumnIndexOrThrow(COL_RULE_CAT_ID))
                }
            }
        }
        return null
    }

    // ==========================================
    // DASHBOARD & ANALYTICS
    // ==========================================

    fun getDashboardSummary(period: TimePeriod): DashboardSummary {
        val accounts = getAccounts()
        val totalBalance = accounts.sumOf { it.currentBalance }
        val categories = getCategories().associateBy { it.id }

        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        // Determine start timestamp based on period
        when (period) {
            TimePeriod.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            TimePeriod.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            TimePeriod.YEARLY -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
        }
        val startPeriodTimestamp = cal.timeInMillis

        val allTxns = getTransactionsWithDetails(limit = 500)
        val periodTxns = allTxns.filter { it.transaction.timestamp in startPeriodTimestamp..now }

        var periodIncome = 0.0
        var periodExpense = 0.0
        val categoryTotals = mutableMapOf<String, Double>()

        for (item in periodTxns) {
            val txn = item.transaction
            if (txn.type == TransactionType.CREDIT) {
                periodIncome += txn.amount
            } else {
                periodExpense += txn.amount
                val current = categoryTotals[txn.categoryId] ?: 0.0
                categoryTotals[txn.categoryId] = current + txn.amount
            }
        }

        val categorySpends = categoryTotals.mapNotNull { (catId, sum) ->
            val cat = categories[catId] ?: return@mapNotNull null
            val pct = if (periodExpense > 0) ((sum / periodExpense) * 100).toFloat() else 0f
            CategorySpend(cat, sum, pct)
        }.sortedByDescending { it.totalAmount }

        // Generate Trend Points
        val trendPoints = generateTrendPoints(period, periodTxns)

        return DashboardSummary(
            period = period,
            totalBalance = totalBalance,
            periodIncome = periodIncome,
            periodExpense = periodExpense,
            netSavings = periodIncome - periodExpense,
            categoryBreakdown = categorySpends,
            trendPoints = trendPoints,
            recentTransactions = allTxns.take(15),
            accounts = accounts
        )
    }

    private fun generateTrendPoints(
        period: TimePeriod,
        transactions: List<TransactionWithDetails>
    ): List<TrendPoint> {
        val points = mutableListOf<TrendPoint>()
        val cal = Calendar.getInstance()

        when (period) {
            TimePeriod.DAILY -> {
                // Last 7 days trend
                val df = SimpleDateFormat("EEE", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val targetCal = Calendar.getInstance()
                    targetCal.add(Calendar.DAY_OF_YEAR, -i)
                    val label = df.format(targetCal.time)

                    targetCal.set(Calendar.HOUR_OF_DAY, 0)
                    targetCal.set(Calendar.MINUTE, 0)
                    targetCal.set(Calendar.SECOND, 0)
                    val start = targetCal.timeInMillis
                    targetCal.set(Calendar.HOUR_OF_DAY, 23)
                    targetCal.set(Calendar.MINUTE, 59)
                    targetCal.set(Calendar.SECOND, 59)
                    val end = targetCal.timeInMillis

                    val dayTxns = transactions.filter { it.transaction.timestamp in start..end }
                    val expense = dayTxns.filter { it.transaction.type == TransactionType.DEBIT }.sumOf { it.transaction.amount }
                    val income = dayTxns.filter { it.transaction.type == TransactionType.CREDIT }.sumOf { it.transaction.amount }
                    points.add(TrendPoint(label, expense, income))
                }
            }
            TimePeriod.MONTHLY -> {
                // 4 Weeks of current month
                val df = SimpleDateFormat("dd MMM", Locale.getDefault())
                val now = System.currentTimeMillis()
                val weekMillis = 7 * 24 * 60 * 60 * 1000L
                for (i in 3 downTo 0) {
                    val end = now - (i * weekMillis)
                    val start = end - weekMillis
                    val label = "W${4 - i}"
                    val weekTxns = transactions.filter { it.transaction.timestamp in start..end }
                    val expense = weekTxns.filter { it.transaction.type == TransactionType.DEBIT }.sumOf { it.transaction.amount }
                    val income = weekTxns.filter { it.transaction.type == TransactionType.CREDIT }.sumOf { it.transaction.amount }
                    points.add(TrendPoint(label, expense, income))
                }
            }
            TimePeriod.YEARLY -> {
                // Last 6 months
                val df = SimpleDateFormat("MMM", Locale.getDefault())
                for (i in 5 downTo 0) {
                    val mCal = Calendar.getInstance()
                    mCal.add(Calendar.MONTH, -i)
                    val label = df.format(mCal.time)
                    mCal.set(Calendar.DAY_OF_MONTH, 1)
                    mCal.set(Calendar.HOUR_OF_DAY, 0)
                    val start = mCal.timeInMillis
                    mCal.set(Calendar.DAY_OF_MONTH, mCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    mCal.set(Calendar.HOUR_OF_DAY, 23)
                    val end = mCal.timeInMillis

                    val monthTxns = transactions.filter { it.transaction.timestamp in start..end }
                    val expense = monthTxns.filter { it.transaction.type == TransactionType.DEBIT }.sumOf { it.transaction.amount }
                    val income = monthTxns.filter { it.transaction.type == TransactionType.CREDIT }.sumOf { it.transaction.amount }
                    points.add(TrendPoint(label, expense, income))
                }
            }
        }
        return points
    }
}
