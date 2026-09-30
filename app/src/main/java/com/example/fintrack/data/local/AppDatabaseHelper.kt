package com.example.fintrack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountAlias
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.BankAccountType
import com.example.fintrack.data.model.Category
import com.example.fintrack.data.model.CategorySpend
import com.example.fintrack.data.model.DashboardSummary
import com.example.fintrack.data.model.ImportedSmsAlert
import com.example.fintrack.data.model.ReconciliationLog
import com.example.fintrack.data.model.SmsAlertStatus
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.data.model.TrendPoint
import com.example.fintrack.data.model.UpcomingCreditCardDue
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
 * Supports multi-account ledger for Bank Accounts & Credit Cards, atomic payments,
 * transfers, duplicate detection, and balance reconciliation.
 */
class AppDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "fintrack_secure.db"
        const val DATABASE_VERSION = 3

        @Volatile
        private var instance: AppDatabaseHelper? = null

        fun getInstance(context: Context): AppDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AppDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }

        // Tables
        const val TABLE_ACCOUNTS = "accounts"
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_TRANSACTIONS = "transactions"
        const val TABLE_MERCHANT_RULES = "merchant_rules"
        const val TABLE_RECONCILIATION_LOGS = "reconciliation_logs"
        const val TABLE_IMPORTED_SMS = "imported_sms"
        const val TABLE_ACCOUNT_ALIASES = "account_aliases"

        // Accounts Columns
        const val COL_ACC_ID = "id"
        const val COL_ACC_NAME = "name"
        const val COL_ACC_BANK = "bank_name"
        const val COL_ACC_TYPE = "account_type"
        const val COL_ACC_BANK_SUBTYPE = "bank_subtype"
        const val COL_ACC_LAST4 = "account_number_last4"
        const val COL_ACC_INIT_BAL = "initial_balance" // Encrypted
        const val COL_ACC_CREDIT_LIMIT = "credit_limit" // Encrypted
        const val COL_ACC_AVAIL_CREDIT = "available_credit" // Encrypted
        const val COL_ACC_STATEMENT_DATE = "statement_date"
        const val COL_ACC_DUE_DATE = "payment_due_date"
        const val COL_ACC_MIN_DUE = "minimum_due" // Encrypted
        const val COL_ACC_TOTAL_DUE = "total_due" // Encrypted
        const val COL_ACC_LINKED_ACCOUNTS = "linked_accounts"
        const val COL_ACC_LAST_CONFIRMED_BAL = "last_confirmed_bal" // Encrypted
        const val COL_ACC_LAST_CONFIRMED_AT = "last_confirmed_at"
        const val COL_ACC_COLOR = "color_hex"
        const val COL_ACC_IS_PRIMARY = "is_primary"
        const val COL_ACC_IS_ACTIVE = "is_active"
        const val COL_ACC_CREATED = "created_at"

        // Categories Columns
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_ICON = "icon_name"
        const val COL_CAT_COLOR = "color_hex"
        const val COL_CAT_IS_INCOME = "is_income"
        const val COL_CAT_KEYWORDS = "keywords"

        // Transactions Columns
        const val COL_TXN_ID = "id"
        const val COL_TXN_ACC_ID = "account_id"
        const val COL_TXN_SOURCE_ACC_ID = "source_account_id"
        const val COL_TXN_DEST_ACC_ID = "destination_account_id"
        const val COL_TXN_CAT_ID = "category_id"
        const val COL_TXN_AMOUNT = "amount" // Encrypted
        const val COL_TXN_TYPE = "type" // DEBIT / CREDIT
        const val COL_TXN_KIND = "kind" // EXPENSE, INCOME, CARD_PURCHASE, CARD_PAYMENT, BANK_TRANSFER, etc.
        const val COL_TXN_TIMESTAMP = "timestamp"
        const val COL_TXN_MERCHANT = "merchant" // Encrypted
        const val COL_TXN_RAW_SMS = "raw_sms_body" // Encrypted
        const val COL_TXN_SMS_SENDER = "sms_sender"
        const val COL_TXN_REF_NO = "reference_number"
        const val COL_TXN_BAL_AFTER = "balance_after_txn" // Encrypted
        const val COL_TXN_AVAIL_CREDIT_AFTER = "available_credit_after" // Encrypted
        const val COL_TXN_IS_MANUAL = "is_manual"
        const val COL_TXN_NOTE = "note" // Encrypted
        const val COL_TXN_NEEDS_REVIEW = "needs_review"
        const val COL_TXN_REVIEW_REASON = "review_reason"
        const val COL_TXN_FINGERPRINT = "fingerprint"
        const val COL_TXN_LINKED_TXN_ID = "linked_transaction_id"
        const val COL_TXN_CREATED = "created_at"

        // Rules Columns
        const val COL_RULE_ID = "id"
        const val COL_RULE_KEYWORD = "merchant_keyword"
        const val COL_RULE_CAT_ID = "category_id"
        const val COL_RULE_CONFIRMED = "user_confirmed"

        // Reconciliation Columns
        const val COL_REC_ID = "id"
        const val COL_REC_ACC_ID = "account_id"
        const val COL_REC_LEDGER_BAL = "ledger_balance"
        const val COL_REC_CONFIRMED_BAL = "confirmed_balance"
        const val COL_REC_DISCREPANCY = "discrepancy"
        const val COL_REC_ADJUSTMENT = "adjustment_amount"
        const val COL_REC_TIMESTAMP = "timestamp"
        const val COL_REC_NOTE = "note"

        // Imported SMS Columns
        const val COL_SMS_ID = "id"
        const val COL_SMS_SENDER = "sender"
        const val COL_SMS_BODY = "body"
        const val COL_SMS_TIMESTAMP = "timestamp"
        const val COL_SMS_STATUS = "status"
        const val COL_SMS_TXN_ID = "transaction_id"
        const val COL_SMS_ACC_ID = "account_id"
        const val COL_SMS_CONFIDENCE = "confidence"
        const val COL_SMS_REASON = "reason"

        // Account Aliases Columns
        const val COL_ALIAS_ID = "id"
        const val COL_ALIAS_ACC_ID = "account_id"
        const val COL_ALIAS_PATTERN = "alias_pattern"
        const val COL_ALIAS_SENDER = "sender_pattern"
    }

    private val _dbChangeSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dbChangeSignal: SharedFlow<Unit> = _dbChangeSignal.asSharedFlow()

    fun notifyDataChanged() {
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
                $COL_ACC_TYPE TEXT NOT NULL DEFAULT 'BANK_ACCOUNT',
                $COL_ACC_BANK_SUBTYPE TEXT NOT NULL DEFAULT 'SAVINGS',
                $COL_ACC_LAST4 TEXT,
                $COL_ACC_INIT_BAL TEXT NOT NULL,
                $COL_ACC_CREDIT_LIMIT TEXT NOT NULL,
                $COL_ACC_AVAIL_CREDIT TEXT,
                $COL_ACC_STATEMENT_DATE TEXT,
                $COL_ACC_DUE_DATE TEXT,
                $COL_ACC_MIN_DUE TEXT,
                $COL_ACC_TOTAL_DUE TEXT,
                $COL_ACC_LINKED_ACCOUNTS TEXT,
                $COL_ACC_LAST_CONFIRMED_BAL TEXT,
                $COL_ACC_LAST_CONFIRMED_AT INTEGER,
                $COL_ACC_COLOR INTEGER NOT NULL,
                $COL_ACC_IS_PRIMARY INTEGER NOT NULL DEFAULT 0,
                $COL_ACC_IS_ACTIVE INTEGER NOT NULL DEFAULT 1,
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
                $COL_TXN_SOURCE_ACC_ID TEXT,
                $COL_TXN_DEST_ACC_ID TEXT,
                $COL_TXN_CAT_ID TEXT NOT NULL,
                $COL_TXN_AMOUNT TEXT NOT NULL,
                $COL_TXN_TYPE TEXT NOT NULL,
                $COL_TXN_KIND TEXT NOT NULL DEFAULT 'EXPENSE',
                $COL_TXN_TIMESTAMP INTEGER NOT NULL,
                $COL_TXN_MERCHANT TEXT NOT NULL,
                $COL_TXN_RAW_SMS TEXT,
                $COL_TXN_SMS_SENDER TEXT,
                $COL_TXN_REF_NO TEXT,
                $COL_TXN_BAL_AFTER TEXT,
                $COL_TXN_AVAIL_CREDIT_AFTER TEXT,
                $COL_TXN_IS_MANUAL INTEGER NOT NULL DEFAULT 0,
                $COL_TXN_NOTE TEXT,
                $COL_TXN_NEEDS_REVIEW INTEGER NOT NULL DEFAULT 0,
                $COL_TXN_REVIEW_REASON TEXT,
                $COL_TXN_FINGERPRINT TEXT,
                $COL_TXN_LINKED_TXN_ID TEXT,
                $COL_TXN_CREATED INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_txn_time ON $TABLE_TRANSACTIONS ($COL_TXN_TIMESTAMP DESC)")
        db.execSQL("CREATE INDEX idx_txn_acc ON $TABLE_TRANSACTIONS ($COL_TXN_ACC_ID)")
        db.execSQL("CREATE INDEX idx_txn_cat ON $TABLE_TRANSACTIONS ($COL_TXN_CAT_ID)")
        db.execSQL("CREATE INDEX idx_txn_source ON $TABLE_TRANSACTIONS ($COL_TXN_SOURCE_ACC_ID)")
        db.execSQL("CREATE INDEX idx_txn_dest ON $TABLE_TRANSACTIONS ($COL_TXN_DEST_ACC_ID)")
        db.execSQL("CREATE INDEX idx_txn_kind ON $TABLE_TRANSACTIONS ($COL_TXN_KIND)")
        db.execSQL("CREATE INDEX idx_txn_fingerprint ON $TABLE_TRANSACTIONS ($COL_TXN_FINGERPRINT)")
        db.execSQL("CREATE INDEX idx_txn_ref ON $TABLE_TRANSACTIONS ($COL_TXN_REF_NO)")

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

        // Reconciliation logs
        db.execSQL(
            """
            CREATE TABLE $TABLE_RECONCILIATION_LOGS (
                $COL_REC_ID TEXT PRIMARY KEY,
                $COL_REC_ACC_ID TEXT NOT NULL,
                $COL_REC_LEDGER_BAL TEXT NOT NULL,
                $COL_REC_CONFIRMED_BAL TEXT NOT NULL,
                $COL_REC_DISCREPANCY TEXT NOT NULL,
                $COL_REC_ADJUSTMENT TEXT NOT NULL,
                $COL_REC_TIMESTAMP INTEGER NOT NULL,
                $COL_REC_NOTE TEXT
            )
            """.trimIndent()
        )

        // Imported SMS alerts table
        db.execSQL(
            """
            CREATE TABLE $TABLE_IMPORTED_SMS (
                $COL_SMS_ID TEXT PRIMARY KEY,
                $COL_SMS_SENDER TEXT NOT NULL,
                $COL_SMS_BODY TEXT NOT NULL,
                $COL_SMS_TIMESTAMP INTEGER NOT NULL,
                $COL_SMS_STATUS TEXT NOT NULL,
                $COL_SMS_TXN_ID TEXT,
                $COL_SMS_ACC_ID TEXT,
                $COL_SMS_CONFIDENCE REAL NOT NULL,
                $COL_SMS_REASON TEXT
            )
            """.trimIndent()
        )

        // Account Aliases table
        db.execSQL(
            """
            CREATE TABLE $TABLE_ACCOUNT_ALIASES (
                $COL_ALIAS_ID TEXT PRIMARY KEY,
                $COL_ALIAS_ACC_ID TEXT NOT NULL,
                $COL_ALIAS_PATTERN TEXT NOT NULL,
                $COL_ALIAS_SENDER TEXT
            )
            """.trimIndent()
        )

        // Seed default categories
        seedDefaultCategories(db)

        // Seed default Primary Account
        seedDefaultAccount(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.beginTransaction()
        try {
            if (oldVersion < 2) {
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_TYPE, "TEXT NOT NULL DEFAULT 'BANK'")
                val encryptedZero = SecurityManager.encryptDouble(0.0) ?: "0.0"
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_CREDIT_LIMIT, "TEXT NOT NULL DEFAULT '$encryptedZero'")
            }

            if (oldVersion < 3) {
                // 1. Upgrade accounts table
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_BANK_SUBTYPE, "TEXT NOT NULL DEFAULT 'SAVINGS'")
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_AVAIL_CREDIT, "TEXT")
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_STATEMENT_DATE, "TEXT")
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_DUE_DATE, "TEXT")
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_MIN_DUE, "TEXT")
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_TOTAL_DUE, "TEXT")
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_LINKED_ACCOUNTS, "TEXT")
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_LAST_CONFIRMED_BAL, "TEXT")
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_LAST_CONFIRMED_AT, "INTEGER")
                addColumnIfNotExists(db, TABLE_ACCOUNTS, COL_ACC_IS_ACTIVE, "INTEGER NOT NULL DEFAULT 1")

                // Map legacy account_type strings
                db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_TYPE = 'BANK_ACCOUNT' WHERE $COL_ACC_TYPE = 'BANK'")
                db.execSQL("UPDATE $TABLE_ACCOUNTS SET $COL_ACC_TYPE = 'CASH_WALLET' WHERE $COL_ACC_TYPE = 'WALLET'")

                // 2. Upgrade transactions table
                addColumnIfNotExists(db, TABLE_TRANSACTIONS, COL_TXN_SOURCE_ACC_ID, "TEXT")
                addColumnIfNotExists(db, TABLE_TRANSACTIONS, COL_TXN_DEST_ACC_ID, "TEXT")
                addColumnIfNotExists(db, TABLE_TRANSACTIONS, COL_TXN_KIND, "TEXT NOT NULL DEFAULT 'EXPENSE'")
                addColumnIfNotExists(db, TABLE_TRANSACTIONS, COL_TXN_AVAIL_CREDIT_AFTER, "TEXT")
                addColumnIfNotExists(db, TABLE_TRANSACTIONS, COL_TXN_NEEDS_REVIEW, "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, TABLE_TRANSACTIONS, COL_TXN_REVIEW_REASON, "TEXT")
                addColumnIfNotExists(db, TABLE_TRANSACTIONS, COL_TXN_FINGERPRINT, "TEXT")
                addColumnIfNotExists(db, TABLE_TRANSACTIONS, COL_TXN_LINKED_TXN_ID, "TEXT")

                // Backfill source_account_id
                db.execSQL("UPDATE $TABLE_TRANSACTIONS SET $COL_TXN_SOURCE_ACC_ID = $COL_TXN_ACC_ID WHERE $COL_TXN_SOURCE_ACC_ID IS NULL")

                // Backfill transaction kind from legacy type
                db.execSQL("UPDATE $TABLE_TRANSACTIONS SET $COL_TXN_KIND = 'INCOME' WHERE $COL_TXN_TYPE = 'CREDIT'")
                db.execSQL("UPDATE $TABLE_TRANSACTIONS SET $COL_TXN_KIND = 'EXPENSE' WHERE $COL_TXN_TYPE = 'DEBIT' AND $COL_TXN_KIND = 'EXPENSE'")

                // Create new indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_source ON $TABLE_TRANSACTIONS ($COL_TXN_SOURCE_ACC_ID)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_dest ON $TABLE_TRANSACTIONS ($COL_TXN_DEST_ACC_ID)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_kind ON $TABLE_TRANSACTIONS ($COL_TXN_KIND)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_fingerprint ON $TABLE_TRANSACTIONS ($COL_TXN_FINGERPRINT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_txn_ref ON $TABLE_TRANSACTIONS ($COL_TXN_REF_NO)")

                // 3. Create new tables
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $TABLE_RECONCILIATION_LOGS (
                        $COL_REC_ID TEXT PRIMARY KEY,
                        $COL_REC_ACC_ID TEXT NOT NULL,
                        $COL_REC_LEDGER_BAL TEXT NOT NULL,
                        $COL_REC_CONFIRMED_BAL TEXT NOT NULL,
                        $COL_REC_DISCREPANCY TEXT NOT NULL,
                        $COL_REC_ADJUSTMENT TEXT NOT NULL,
                        $COL_REC_TIMESTAMP INTEGER NOT NULL,
                        $COL_REC_NOTE TEXT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $TABLE_IMPORTED_SMS (
                        $COL_SMS_ID TEXT PRIMARY KEY,
                        $COL_SMS_SENDER TEXT NOT NULL,
                        $COL_SMS_BODY TEXT NOT NULL,
                        $COL_SMS_TIMESTAMP INTEGER NOT NULL,
                        $COL_SMS_STATUS TEXT NOT NULL,
                        $COL_SMS_TXN_ID TEXT,
                        $COL_SMS_ACC_ID TEXT,
                        $COL_SMS_CONFIDENCE REAL NOT NULL,
                        $COL_SMS_REASON TEXT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $TABLE_ACCOUNT_ALIASES (
                        $COL_ALIAS_ID TEXT PRIMARY KEY,
                        $COL_ALIAS_ACC_ID TEXT NOT NULL,
                        $COL_ALIAS_PATTERN TEXT NOT NULL,
                        $COL_ALIAS_SENDER TEXT
                    )
                    """.trimIndent()
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun addColumnIfNotExists(db: SQLiteDatabase, table: String, column: String, definition: String) {
        val cursor = db.rawQuery("PRAGMA table_info($table)", null)
        var exists = false
        cursor.use { c ->
            val nameIndex = c.getColumnIndex("name")
            while (c.moveToNext()) {
                if (c.getString(nameIndex).equals(column, ignoreCase = true)) {
                    exists = true
                    break
                }
            }
        }
        if (!exists) {
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
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
            db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun seedDefaultAccount(db: SQLiteDatabase) {
        val defaultAccount = Account(
            id = "default_primary_account",
            name = "Primary Bank Account",
            bankName = "General",
            accountType = AccountType.BANK_ACCOUNT,
            bankAccountType = BankAccountType.SAVINGS,
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
            put(COL_ACC_BANK_SUBTYPE, defaultAccount.bankAccountType.name)
            put(COL_ACC_LAST4, defaultAccount.accountNumberLast4)
            put(COL_ACC_CREDIT_LIMIT, SecurityManager.encryptDouble(defaultAccount.creditLimit))
            put(COL_ACC_INIT_BAL, SecurityManager.encryptDouble(defaultAccount.initialBalance))
            put(COL_ACC_COLOR, defaultAccount.colorHex)
            put(COL_ACC_IS_PRIMARY, 1)
            put(COL_ACC_IS_ACTIVE, 1)
            put(COL_ACC_CREATED, defaultAccount.createdAt)
        }
        db.insertWithOnConflict(TABLE_ACCOUNTS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
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
            put(COL_ACC_BANK_SUBTYPE, account.bankAccountType.name)
            put(COL_ACC_LAST4, account.accountNumberLast4)
            put(COL_ACC_CREDIT_LIMIT, SecurityManager.encryptDouble(account.creditLimit))
            put(COL_ACC_INIT_BAL, SecurityManager.encryptDouble(account.initialBalance))
            put(COL_ACC_AVAIL_CREDIT, account.availableCredit?.let { SecurityManager.encryptDouble(it) })
            put(COL_ACC_STATEMENT_DATE, account.statementDate)
            put(COL_ACC_DUE_DATE, account.paymentDueDate)
            put(COL_ACC_MIN_DUE, account.minimumDue?.let { SecurityManager.encryptDouble(it) })
            put(COL_ACC_TOTAL_DUE, account.totalDue?.let { SecurityManager.encryptDouble(it) })
            put(COL_ACC_LINKED_ACCOUNTS, account.linkedPaymentAccountIds.joinToString(","))
            put(COL_ACC_LAST_CONFIRMED_BAL, account.lastConfirmedBalance?.let { SecurityManager.encryptDouble(it) })
            put(COL_ACC_LAST_CONFIRMED_AT, account.lastConfirmedAt)
            put(COL_ACC_COLOR, account.colorHex)
            put(COL_ACC_IS_PRIMARY, if (account.isPrimary) 1 else 0)
            put(COL_ACC_IS_ACTIVE, if (account.isActive) 1 else 0)
            put(COL_ACC_CREATED, account.createdAt)
        }
        val result = db.insertWithOnConflict(TABLE_ACCOUNTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        notifyDataChanged()
        return result != -1L
    }

    fun deleteAccount(accountId: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Delete all transactions under this account
            db.delete(TABLE_TRANSACTIONS, "$COL_TXN_ACC_ID = ? OR $COL_TXN_SOURCE_ACC_ID = ? OR $COL_TXN_DEST_ACC_ID = ?", arrayOf(accountId, accountId, accountId))
            // Delete aliases
            db.delete(TABLE_ACCOUNT_ALIASES, "$COL_ALIAS_ACC_ID = ?", arrayOf(accountId))
            // Delete logs
            db.delete(TABLE_RECONCILIATION_LOGS, "$COL_REC_ACC_ID = ?", arrayOf(accountId))
            // Delete account
            val rows = db.delete(TABLE_ACCOUNTS, "$COL_ACC_ID = ?", arrayOf(accountId))

            // Re-elect primary if needed
            val remaining = getAccounts()
            if (remaining.isNotEmpty() && remaining.none { it.isPrimary }) {
                val first = remaining.first()
                val values = ContentValues().apply {
                    put(COL_ACC_IS_PRIMARY, 1)
                }
                db.update(TABLE_ACCOUNTS, values, "$COL_ACC_ID = ?", arrayOf(first.id))
            }

            db.setTransactionSuccessful()
            notifyDataChanged()
            rows > 0
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Calculates net financial effect of transactions on an account.
     * For Bank Accounts: Credits increase (+), Debits decrease (-),
     *   Transfers in (+), Transfers out (-), CC bill payments out (-).
     * For Credit Cards: Spends/purchases increase outstanding (+),
     *   Bill payments and refunds decrease outstanding (-).
     */
    fun getNetTransactions(accountId: String, accountType: AccountType): Double {
        val db = readableDatabase
        var net = 0.0
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            arrayOf(COL_TXN_AMOUNT, COL_TXN_TYPE, COL_TXN_KIND, COL_TXN_SOURCE_ACC_ID, COL_TXN_DEST_ACC_ID, COL_TXN_ACC_ID),
            "$COL_TXN_ACC_ID = ? OR $COL_TXN_SOURCE_ACC_ID = ? OR $COL_TXN_DEST_ACC_ID = ?",
            arrayOf(accountId, accountId, accountId),
            null,
            null,
            null
        )
        cursor.use { c ->
            val encAmountIdx = c.getColumnIndexOrThrow(COL_TXN_AMOUNT)
            val typeIdx = c.getColumnIndexOrThrow(COL_TXN_TYPE)
            val kindIdx = c.getColumnIndexOrThrow(COL_TXN_KIND)
            val sourceIdx = c.getColumnIndexOrThrow(COL_TXN_SOURCE_ACC_ID)
            val destIdx = c.getColumnIndexOrThrow(COL_TXN_DEST_ACC_ID)
            val accIdx = c.getColumnIndexOrThrow(COL_TXN_ACC_ID)

            while (c.moveToNext()) {
                val encryptedAmount = c.getString(encAmountIdx)
                val type = c.getString(typeIdx)
                val kindStr = c.getString(kindIdx)
                val kind = TransactionKind.fromString(kindStr)
                val sourceAcc = c.getString(sourceIdx)
                val destAcc = c.getString(destIdx)
                val primaryAcc = c.getString(accIdx)
                val amount = SecurityManager.decryptDouble(encryptedAmount, 0.0)

                if (accountType == AccountType.CREDIT_CARD) {
                    when (kind) {
                        TransactionKind.CARD_PURCHASE, TransactionKind.EXPENSE -> {
                            if (primaryAcc == accountId || destAcc == accountId || sourceAcc == accountId) {
                                net += amount // Increases outstanding
                            }
                        }
                        TransactionKind.CARD_PAYMENT -> {
                            // Payment towards this credit card decreases outstanding
                            if (destAcc == accountId || primaryAcc == accountId) {
                                net -= amount
                            }
                        }
                        TransactionKind.REFUND, TransactionKind.REVERSAL -> {
                            net -= amount // Reduces outstanding
                        }
                        else -> {
                            if (type == TransactionDirection.DEBIT.name) net += amount else net -= amount
                        }
                    }
                } else {
                    // Bank Account / Cash Wallet
                    when (kind) {
                        TransactionKind.BANK_TRANSFER -> {
                            if (sourceAcc == accountId) {
                                net -= amount // Money sent out
                            } else if (destAcc == accountId) {
                                net += amount // Money received
                            }
                        }
                        TransactionKind.CARD_PAYMENT -> {
                            if (sourceAcc == accountId) {
                                net -= amount // Bank paid CC bill
                            }
                        }
                        TransactionKind.INCOME, TransactionKind.CASH_DEPOSIT, TransactionKind.REFUND -> {
                            net += amount
                        }
                        TransactionKind.EXPENSE, TransactionKind.ATM_WITHDRAWAL, TransactionKind.CASH_WITHDRAWAL -> {
                            net -= amount
                        }
                        TransactionKind.REVERSAL -> {
                            if (type == TransactionDirection.CREDIT.name) net += amount else net -= amount
                        }
                        else -> {
                            if (type == TransactionDirection.CREDIT.name) net += amount else net -= amount
                        }
                    }
                }
            }
        }
        return net
    }

    /**
     * Updates current balance to target number directly by recalculating base ledger balance.
     */
    fun updateCurrentBalance(accountId: String, newCurrentBalance: Double): Boolean {
        val db = writableDatabase
        val acc = getAccountById(accountId) ?: return false
        val net = getNetTransactions(accountId, acc.accountType)
        val calculatedInitial = newCurrentBalance - net
        val values = ContentValues().apply {
            put(COL_ACC_INIT_BAL, SecurityManager.encryptDouble(calculatedInitial))
            put(COL_ACC_LAST_CONFIRMED_BAL, SecurityManager.encryptDouble(newCurrentBalance))
            put(COL_ACC_LAST_CONFIRMED_AT, System.currentTimeMillis())
        }
        val rows = db.update(TABLE_ACCOUNTS, values, "$COL_ACC_ID = ?", arrayOf(accountId))
        notifyDataChanged()
        return rows > 0
    }

    fun updateInitialBalance(accountId: String, newBalance: Double): Boolean {
        return updateCurrentBalance(accountId, newBalance)
    }

    fun updateAccount(account: Account, targetCurrentBalance: Double? = null): Boolean {
        val db = writableDatabase
        val targetBal = targetCurrentBalance ?: account.currentBalance
        val net = getNetTransactions(account.id, account.accountType)
        val calculatedInitial = targetBal - net
        val values = ContentValues().apply {
            put(COL_ACC_NAME, account.name)
            put(COL_ACC_BANK, account.bankName)
            put(COL_ACC_TYPE, account.accountType.name)
            put(COL_ACC_BANK_SUBTYPE, account.bankAccountType.name)
            put(COL_ACC_LAST4, account.accountNumberLast4)
            put(COL_ACC_INIT_BAL, SecurityManager.encryptDouble(calculatedInitial))
            put(COL_ACC_CREDIT_LIMIT, SecurityManager.encryptDouble(account.creditLimit))
            put(COL_ACC_AVAIL_CREDIT, account.availableCredit?.let { SecurityManager.encryptDouble(it) })
            put(COL_ACC_STATEMENT_DATE, account.statementDate)
            put(COL_ACC_DUE_DATE, account.paymentDueDate)
            put(COL_ACC_MIN_DUE, account.minimumDue?.let { SecurityManager.encryptDouble(it) })
            put(COL_ACC_TOTAL_DUE, account.totalDue?.let { SecurityManager.encryptDouble(it) })
            put(COL_ACC_LINKED_ACCOUNTS, account.linkedPaymentAccountIds.joinToString(","))
            put(COL_ACC_IS_ACTIVE, if (account.isActive) 1 else 0)
        }
        val rows = db.update(TABLE_ACCOUNTS, values, "$COL_ACC_ID = ?", arrayOf(account.id))
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
                val typeStr = c.getString(c.getColumnIndexOrThrow(COL_ACC_TYPE)) ?: "BANK_ACCOUNT"
                val accountType = AccountType.fromString(typeStr)

                val subtypeIdx = c.getColumnIndex(COL_ACC_BANK_SUBTYPE)
                val subtypeStr = if (subtypeIdx != -1) c.getString(subtypeIdx) ?: "SAVINGS" else "SAVINGS"
                val bankSubtype = try { BankAccountType.valueOf(subtypeStr) } catch (_: Exception) { BankAccountType.SAVINGS }

                val last4 = c.getString(c.getColumnIndexOrThrow(COL_ACC_LAST4)) ?: ""
                val encryptedInitBal = c.getString(c.getColumnIndexOrThrow(COL_ACC_INIT_BAL))
                val encryptedLimit = c.getString(c.getColumnIndexOrThrow(COL_ACC_CREDIT_LIMIT))
                val creditLimit = if (encryptedLimit != null) SecurityManager.decryptDouble(encryptedLimit, 0.0) else 0.0
                val initBal = SecurityManager.decryptDouble(encryptedInitBal, 0.0)

                val availCreditIdx = c.getColumnIndex(COL_ACC_AVAIL_CREDIT)
                val availCreditEnc = if (availCreditIdx != -1) c.getString(availCreditIdx) else null
                val availCredit = availCreditEnc?.let { SecurityManager.decryptDouble(it) }

                val stmtDateIdx = c.getColumnIndex(COL_ACC_STATEMENT_DATE)
                val stmtDate = if (stmtDateIdx != -1) c.getString(stmtDateIdx) else null

                val dueDateIdx = c.getColumnIndex(COL_ACC_DUE_DATE)
                val dueDate = if (dueDateIdx != -1) c.getString(dueDateIdx) else null

                val minDueIdx = c.getColumnIndex(COL_ACC_MIN_DUE)
                val minDueEnc = if (minDueIdx != -1) c.getString(minDueIdx) else null
                val minDue = minDueEnc?.let { SecurityManager.decryptDouble(it) }

                val totDueIdx = c.getColumnIndex(COL_ACC_TOTAL_DUE)
                val totDueEnc = if (totDueIdx != -1) c.getString(totDueIdx) else null
                val totDue = totDueEnc?.let { SecurityManager.decryptDouble(it) }

                val linkedIdx = c.getColumnIndex(COL_ACC_LINKED_ACCOUNTS)
                val linkedStr = if (linkedIdx != -1) c.getString(linkedIdx) ?: "" else ""
                val linkedIds = if (linkedStr.isBlank()) emptyList() else linkedStr.split(",")

                val confBalIdx = c.getColumnIndex(COL_ACC_LAST_CONFIRMED_BAL)
                val confBalEnc = if (confBalIdx != -1) c.getString(confBalIdx) else null
                val confBal = confBalEnc?.let { SecurityManager.decryptDouble(it) }

                val confAtIdx = c.getColumnIndex(COL_ACC_LAST_CONFIRMED_AT)
                val confAt = if (confAtIdx != -1 && !c.isNull(confAtIdx)) c.getLong(confAtIdx) else null

                val color = c.getLong(c.getColumnIndexOrThrow(COL_ACC_COLOR))
                val isPrimary = c.getInt(c.getColumnIndexOrThrow(COL_ACC_IS_PRIMARY)) == 1

                val activeIdx = c.getColumnIndex(COL_ACC_IS_ACTIVE)
                val isActive = if (activeIdx != -1) c.getInt(activeIdx) == 1 else true

                val created = c.getLong(c.getColumnIndexOrThrow(COL_ACC_CREATED))

                val currentBal = calculateAccountBalance(id, accountType, initBal)

                accounts.add(
                    Account(
                        id = id,
                        name = name,
                        bankName = bank,
                        accountType = accountType,
                        bankAccountType = bankSubtype,
                        accountNumberLast4 = last4,
                        creditLimit = creditLimit,
                        initialBalance = initBal,
                        currentBalance = currentBal,
                        availableCredit = availCredit,
                        statementDate = stmtDate,
                        paymentDueDate = dueDate,
                        minimumDue = minDue,
                        totalDue = totDue,
                        linkedPaymentAccountIds = linkedIds,
                        lastConfirmedBalance = confBal,
                        lastConfirmedAt = confAt,
                        colorHex = color,
                        isPrimary = isPrimary,
                        isActive = isActive,
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

    private fun calculateAccountBalance(accountId: String, accountType: AccountType, initialBalance: Double): Double {
        val net = getNetTransactions(accountId, accountType)
        return initialBalance + net
    }

    // ==========================================
    // RECONCILIATION OPERATIONS
    // ==========================================

    fun recordReconciliation(log: ReconciliationLog): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_REC_ID, log.id)
            put(COL_REC_ACC_ID, log.accountId)
            put(COL_REC_LEDGER_BAL, SecurityManager.encryptDouble(log.ledgerBalance))
            put(COL_REC_CONFIRMED_BAL, SecurityManager.encryptDouble(log.confirmedBalance))
            put(COL_REC_DISCREPANCY, SecurityManager.encryptDouble(log.discrepancy))
            put(COL_REC_ADJUSTMENT, SecurityManager.encryptDouble(log.adjustmentAmount))
            put(COL_REC_TIMESTAMP, log.timestamp)
            put(COL_REC_NOTE, log.note)
        }
        val result = db.insert(TABLE_RECONCILIATION_LOGS, null, values)
        notifyDataChanged()
        return result != -1L
    }

    fun getReconciliationLogs(accountId: String? = null): List<ReconciliationLog> {
        val db = readableDatabase
        val logs = mutableListOf<ReconciliationLog>()
        val selection = if (accountId != null) "$COL_REC_ACC_ID = ?" else null
        val selectionArgs = if (accountId != null) arrayOf(accountId) else null
        val cursor = db.query(
            TABLE_RECONCILIATION_LOGS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COL_REC_TIMESTAMP DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_REC_ID))
                val accId = c.getString(c.getColumnIndexOrThrow(COL_REC_ACC_ID))
                val encLedger = c.getString(c.getColumnIndexOrThrow(COL_REC_LEDGER_BAL))
                val encConfirmed = c.getString(c.getColumnIndexOrThrow(COL_REC_CONFIRMED_BAL))
                val encDiscrepancy = c.getString(c.getColumnIndexOrThrow(COL_REC_DISCREPANCY))
                val encAdjustment = c.getString(c.getColumnIndexOrThrow(COL_REC_ADJUSTMENT))
                val timestamp = c.getLong(c.getColumnIndexOrThrow(COL_REC_TIMESTAMP))
                val note = c.getString(c.getColumnIndexOrThrow(COL_REC_NOTE))

                logs.add(
                    ReconciliationLog(
                        id = id,
                        accountId = accId,
                        ledgerBalance = SecurityManager.decryptDouble(encLedger, 0.0),
                        confirmedBalance = SecurityManager.decryptDouble(encConfirmed, 0.0),
                        discrepancy = SecurityManager.decryptDouble(encDiscrepancy, 0.0),
                        adjustmentAmount = SecurityManager.decryptDouble(encAdjustment, 0.0),
                        timestamp = timestamp,
                        note = note
                    )
                )
            }
        }
        return logs
    }

    // ==========================================
    // IMPORTED SMS ALERTS
    // ==========================================

    fun insertImportedSms(alert: ImportedSmsAlert): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_SMS_ID, alert.id)
            put(COL_SMS_SENDER, alert.sender)
            put(COL_SMS_BODY, alert.body)
            put(COL_SMS_TIMESTAMP, alert.timestamp)
            put(COL_SMS_STATUS, alert.status.name)
            put(COL_SMS_TXN_ID, alert.transactionId)
            put(COL_SMS_ACC_ID, alert.accountId)
            put(COL_SMS_CONFIDENCE, alert.confidence)
            put(COL_SMS_REASON, alert.reason)
        }
        val result = db.insertWithOnConflict(TABLE_IMPORTED_SMS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        notifyDataChanged()
        return result != -1L
    }

    fun getImportedSmsAlerts(): List<ImportedSmsAlert> {
        val db = readableDatabase
        val alerts = mutableListOf<ImportedSmsAlert>()
        val cursor = db.query(TABLE_IMPORTED_SMS, null, null, null, null, null, "$COL_SMS_TIMESTAMP DESC", "200")
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_SMS_ID))
                val sender = c.getString(c.getColumnIndexOrThrow(COL_SMS_SENDER))
                val body = c.getString(c.getColumnIndexOrThrow(COL_SMS_BODY))
                val timestamp = c.getLong(c.getColumnIndexOrThrow(COL_SMS_TIMESTAMP))
                val statusStr = c.getString(c.getColumnIndexOrThrow(COL_SMS_STATUS))
                val status = try { SmsAlertStatus.valueOf(statusStr) } catch (_: Exception) { SmsAlertStatus.PROCESSED }
                val txnId = c.getString(c.getColumnIndexOrThrow(COL_SMS_TXN_ID))
                val accId = c.getString(c.getColumnIndexOrThrow(COL_SMS_ACC_ID))
                val confidence = c.getDouble(c.getColumnIndexOrThrow(COL_SMS_CONFIDENCE))
                val reason = c.getString(c.getColumnIndexOrThrow(COL_SMS_REASON))

                alerts.add(ImportedSmsAlert(id, sender, body, timestamp, status, txnId, accId, confidence, reason))
            }
        }
        return alerts
    }

    fun resolveImportedSmsAlert(alertId: String, selectedAccountId: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val alert = getImportedSmsAlerts().firstOrNull { it.id == alertId } ?: return false
            val values = ContentValues().apply {
                put(COL_SMS_STATUS, SmsAlertStatus.PROCESSED.name)
                put(COL_SMS_ACC_ID, selectedAccountId)
                put(COL_SMS_CONFIDENCE, 1.0)
                put(COL_SMS_REASON, "Resolved by user")
            }
            val rows = db.update(TABLE_IMPORTED_SMS, values, "$COL_SMS_ID = ?", arrayOf(alertId))

            // Update associated transaction if present
            alert.transactionId?.let { txnId ->
                val txnValues = ContentValues().apply {
                    put(COL_TXN_ACC_ID, selectedAccountId)
                    put(COL_TXN_SOURCE_ACC_ID, selectedAccountId)
                    put(COL_TXN_NEEDS_REVIEW, 0)
                    putNull(COL_TXN_REVIEW_REASON)
                }
                db.update(TABLE_TRANSACTIONS, txnValues, "$COL_TXN_ID = ?", arrayOf(txnId))
            }

            // Learn alias from SMS pattern if 4 digits found
            val last4Regex = Regex("""(?:\b|X+|\*+)(\d{4})\b""")
            val last4Match = last4Regex.find(alert.body)
            if (last4Match != null) {
                val alias = AccountAlias(
                    id = UUID.randomUUID().toString(),
                    accountId = selectedAccountId,
                    aliasPattern = last4Match.groupValues[1],
                    senderPattern = alert.sender
                )
                addAccountAlias(alias)
            }

            db.setTransactionSuccessful()
            notifyDataChanged()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    // ==========================================
    // ACCOUNT ALIASES (LEARNING)
    // ==========================================

    fun addAccountAlias(alias: AccountAlias): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ALIAS_ID, alias.id)
            put(COL_ALIAS_ACC_ID, alias.accountId)
            put(COL_ALIAS_PATTERN, alias.aliasPattern.lowercase().trim())
            put(COL_ALIAS_SENDER, alias.senderPattern?.lowercase()?.trim())
        }
        val result = db.insertWithOnConflict(TABLE_ACCOUNT_ALIASES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        notifyDataChanged()
        return result != -1L
    }

    fun getAccountAliases(): List<AccountAlias> {
        val db = readableDatabase
        val list = mutableListOf<AccountAlias>()
        val cursor = db.query(TABLE_ACCOUNT_ALIASES, null, null, null, null, null, null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_ALIAS_ID))
                val accId = c.getString(c.getColumnIndexOrThrow(COL_ALIAS_ACC_ID))
                val pattern = c.getString(c.getColumnIndexOrThrow(COL_ALIAS_PATTERN))
                val sender = c.getString(c.getColumnIndexOrThrow(COL_ALIAS_SENDER))
                list.add(AccountAlias(id, accId, pattern, sender))
            }
        }
        return list
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

    fun findTransactionByFingerprint(fingerprint: String): Transaction? {
        if (fingerprint.isBlank()) return null
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TXN_FINGERPRINT = ?",
            arrayOf(fingerprint),
            null, null, null, "1"
        )
        cursor.use { c ->
            if (c.moveToNext()) {
                return parseTransactionCursor(c)
            }
        }
        return null
    }

    fun findTransactionByRefNumber(refNumber: String): Transaction? {
        if (refNumber.isBlank()) return null
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TXN_REF_NO = ?",
            arrayOf(refNumber),
            null, null, null, "1"
        )
        cursor.use { c ->
            if (c.moveToNext()) {
                return parseTransactionCursor(c)
            }
        }
        return null
    }

    fun insertTransaction(transaction: Transaction): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TXN_ID, transaction.id)
            put(COL_TXN_ACC_ID, transaction.accountId)
            put(COL_TXN_SOURCE_ACC_ID, transaction.sourceAccountId ?: transaction.accountId)
            put(COL_TXN_DEST_ACC_ID, transaction.destinationAccountId)
            put(COL_TXN_CAT_ID, transaction.categoryId)
            put(COL_TXN_AMOUNT, SecurityManager.encryptDouble(transaction.amount))
            put(COL_TXN_TYPE, transaction.direction.name)
            put(COL_TXN_KIND, transaction.kind.name)
            put(COL_TXN_TIMESTAMP, transaction.timestamp)
            put(COL_TXN_MERCHANT, SecurityManager.encrypt(transaction.merchant))
            put(COL_TXN_RAW_SMS, SecurityManager.encrypt(transaction.rawSmsBody))
            put(COL_TXN_SMS_SENDER, transaction.smsSender)
            put(COL_TXN_REF_NO, transaction.referenceNumber)
            put(
                COL_TXN_BAL_AFTER,
                transaction.balanceAfterTxn?.let { SecurityManager.encryptDouble(it) }
            )
            put(
                COL_TXN_AVAIL_CREDIT_AFTER,
                transaction.availableCreditAfterTxn?.let { SecurityManager.encryptDouble(it) }
            )
            put(COL_TXN_IS_MANUAL, if (transaction.isManual) 1 else 0)
            put(COL_TXN_NOTE, SecurityManager.encrypt(transaction.note))
            put(COL_TXN_NEEDS_REVIEW, if (transaction.needsReview) 1 else 0)
            put(COL_TXN_REVIEW_REASON, transaction.reviewReason)
            put(COL_TXN_FINGERPRINT, transaction.fingerprint)
            put(COL_TXN_LINKED_TXN_ID, transaction.linkedTransactionId)
            put(COL_TXN_CREATED, transaction.createdAt)
        }
        val res = db.insert(TABLE_TRANSACTIONS, null, values)
        notifyDataChanged()
        return res != -1L
    }

    /**
     * Atomic Credit Card Bill Payment:
     * - Records CARD_PAYMENT transaction
     * - Linked from bank account (source) to credit card (destination)
     * - In a single atomic SQLite transaction
     */
    fun recordCreditCardPayment(
        sourceBankAccountId: String,
        creditCardId: String,
        amount: Double,
        referenceNumber: String? = null,
        note: String? = null
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val paymentTxn = Transaction(
                id = UUID.randomUUID().toString(),
                accountId = sourceBankAccountId,
                sourceAccountId = sourceBankAccountId,
                destinationAccountId = creditCardId,
                categoryId = "cat_bills",
                amount = amount,
                direction = TransactionDirection.DEBIT,
                kind = TransactionKind.CARD_PAYMENT,
                timestamp = System.currentTimeMillis(),
                merchant = "Credit Card Bill Payment",
                referenceNumber = referenceNumber,
                isManual = true,
                note = note ?: "Payment to credit card"
            )

            val values = ContentValues().apply {
                put(COL_TXN_ID, paymentTxn.id)
                put(COL_TXN_ACC_ID, paymentTxn.accountId)
                put(COL_TXN_SOURCE_ACC_ID, paymentTxn.sourceAccountId)
                put(COL_TXN_DEST_ACC_ID, paymentTxn.destinationAccountId)
                put(COL_TXN_CAT_ID, paymentTxn.categoryId)
                put(COL_TXN_AMOUNT, SecurityManager.encryptDouble(paymentTxn.amount))
                put(COL_TXN_TYPE, paymentTxn.direction.name)
                put(COL_TXN_KIND, paymentTxn.kind.name)
                put(COL_TXN_TIMESTAMP, paymentTxn.timestamp)
                put(COL_TXN_MERCHANT, SecurityManager.encrypt(paymentTxn.merchant))
                put(COL_TXN_REF_NO, paymentTxn.referenceNumber)
                put(COL_TXN_IS_MANUAL, 1)
                put(COL_TXN_NOTE, SecurityManager.encrypt(paymentTxn.note))
                put(COL_TXN_CREATED, paymentTxn.createdAt)
            }
            db.insert(TABLE_TRANSACTIONS, null, values)

            db.setTransactionSuccessful()
            notifyDataChanged()
            true
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Atomic Bank-to-Bank Transfer:
     * - Records BANK_TRANSFER transaction
     * - Linked from source bank account to destination bank account
     */
    fun recordBankTransfer(
        sourceBankAccountId: String,
        destinationBankAccountId: String,
        amount: Double,
        referenceNumber: String? = null,
        note: String? = null
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val transferTxn = Transaction(
                id = UUID.randomUUID().toString(),
                accountId = sourceBankAccountId,
                sourceAccountId = sourceBankAccountId,
                destinationAccountId = destinationBankAccountId,
                categoryId = "cat_transfer",
                amount = amount,
                direction = TransactionDirection.DEBIT,
                kind = TransactionKind.BANK_TRANSFER,
                timestamp = System.currentTimeMillis(),
                merchant = "Bank Transfer",
                referenceNumber = referenceNumber,
                isManual = true,
                note = note ?: "Account to Account Transfer"
            )

            val values = ContentValues().apply {
                put(COL_TXN_ID, transferTxn.id)
                put(COL_TXN_ACC_ID, transferTxn.accountId)
                put(COL_TXN_SOURCE_ACC_ID, transferTxn.sourceAccountId)
                put(COL_TXN_DEST_ACC_ID, transferTxn.destinationAccountId)
                put(COL_TXN_CAT_ID, transferTxn.categoryId)
                put(COL_TXN_AMOUNT, SecurityManager.encryptDouble(transferTxn.amount))
                put(COL_TXN_TYPE, transferTxn.direction.name)
                put(COL_TXN_KIND, transferTxn.kind.name)
                put(COL_TXN_TIMESTAMP, transferTxn.timestamp)
                put(COL_TXN_MERCHANT, SecurityManager.encrypt(transferTxn.merchant))
                put(COL_TXN_REF_NO, transferTxn.referenceNumber)
                put(COL_TXN_IS_MANUAL, 1)
                put(COL_TXN_NOTE, SecurityManager.encrypt(transferTxn.note))
                put(COL_TXN_CREATED, transferTxn.createdAt)
            }
            db.insert(TABLE_TRANSACTIONS, null, values)

            db.setTransactionSuccessful()
            notifyDataChanged()
            true
        } finally {
            db.endTransaction()
        }
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

    fun updateTransactionAccount(transactionId: String, newAccountId: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TXN_ACC_ID, newAccountId)
            put(COL_TXN_SOURCE_ACC_ID, newAccountId)
            put(COL_TXN_NEEDS_REVIEW, 0)
            put(COL_TXN_REVIEW_REASON, null as String?)
        }
        val rows = db.update(TABLE_TRANSACTIONS, values, "$COL_TXN_ID = ?", arrayOf(transactionId))
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
        val defaultAccount = accountsMap.values.firstOrNull { it.isPrimary } ?: accountsMap.values.firstOrNull() ?: Account(name = "Account", bankName = "Bank")

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
                val txn = parseTransactionCursor(c)
                val account = accountsMap[txn.accountId] ?: defaultAccount
                val category = categoriesMap[txn.categoryId] ?: defaultCategory
                val source = txn.sourceAccountId?.let { accountsMap[it] }
                val dest = txn.destinationAccountId?.let { accountsMap[it] }

                list.add(TransactionWithDetails(txn, account, category, source, dest))
            }
        }
        return list
    }

    private fun parseTransactionCursor(c: Cursor): Transaction {
        val id = c.getString(c.getColumnIndexOrThrow(COL_TXN_ID))
        val accId = c.getString(c.getColumnIndexOrThrow(COL_TXN_ACC_ID))

        val srcIdx = c.getColumnIndex(COL_TXN_SOURCE_ACC_ID)
        val sourceAccId = if (srcIdx != -1) c.getString(srcIdx) else accId

        val dstIdx = c.getColumnIndex(COL_TXN_DEST_ACC_ID)
        val destAccId = if (dstIdx != -1) c.getString(dstIdx) else null

        val catId = c.getString(c.getColumnIndexOrThrow(COL_TXN_CAT_ID))
        val encAmount = c.getString(c.getColumnIndexOrThrow(COL_TXN_AMOUNT))
        val amount = SecurityManager.decryptDouble(encAmount, 0.0)

        val typeStr = c.getString(c.getColumnIndexOrThrow(COL_TXN_TYPE))
        val direction = if (typeStr == TransactionDirection.CREDIT.name) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val kindIdx = c.getColumnIndex(COL_TXN_KIND)
        val kindStr = if (kindIdx != -1) c.getString(kindIdx) else null
        val kind = TransactionKind.fromString(kindStr)

        val timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TXN_TIMESTAMP))
        val encMerchant = c.getString(c.getColumnIndexOrThrow(COL_TXN_MERCHANT))
        val merchant = SecurityManager.decrypt(encMerchant) ?: "Unknown"

        val encRawSms = c.getString(c.getColumnIndexOrThrow(COL_TXN_RAW_SMS))
        val rawSms = SecurityManager.decrypt(encRawSms)
        val sender = c.getString(c.getColumnIndexOrThrow(COL_TXN_SMS_SENDER))
        val refNo = c.getString(c.getColumnIndexOrThrow(COL_TXN_REF_NO))

        val encBalAfter = c.getString(c.getColumnIndexOrThrow(COL_TXN_BAL_AFTER))
        val balAfter = encBalAfter?.let { SecurityManager.decryptDouble(it) }

        val availCrIdx = c.getColumnIndex(COL_TXN_AVAIL_CREDIT_AFTER)
        val availCrEnc = if (availCrIdx != -1) c.getString(availCrIdx) else null
        val availCreditAfter = availCrEnc?.let { SecurityManager.decryptDouble(it) }

        val isManual = c.getInt(c.getColumnIndexOrThrow(COL_TXN_IS_MANUAL)) == 1
        val encNote = c.getString(c.getColumnIndexOrThrow(COL_TXN_NOTE))
        val note = SecurityManager.decrypt(encNote)

        val reviewIdx = c.getColumnIndex(COL_TXN_NEEDS_REVIEW)
        val needsReview = if (reviewIdx != -1) c.getInt(reviewIdx) == 1 else false

        val reasonIdx = c.getColumnIndex(COL_TXN_REVIEW_REASON)
        val reviewReason = if (reasonIdx != -1) c.getString(reasonIdx) else null

        val fpIdx = c.getColumnIndex(COL_TXN_FINGERPRINT)
        val fingerprint = if (fpIdx != -1) c.getString(fpIdx) else null

        val linkIdx = c.getColumnIndex(COL_TXN_LINKED_TXN_ID)
        val linkedTxnId = if (linkIdx != -1) c.getString(linkIdx) else null

        val created = c.getLong(c.getColumnIndexOrThrow(COL_TXN_CREATED))

        return Transaction(
            id = id,
            accountId = accId,
            sourceAccountId = sourceAccId,
            destinationAccountId = destAccId,
            categoryId = catId,
            amount = amount,
            direction = direction,
            kind = kind,
            timestamp = timestamp,
            merchant = merchant,
            rawSmsBody = rawSms,
            smsSender = sender,
            referenceNumber = refNo,
            balanceAfterTxn = balAfter,
            availableCreditAfterTxn = availCreditAfter,
            isManual = isManual,
            note = note,
            needsReview = needsReview,
            reviewReason = reviewReason,
            fingerprint = fingerprint,
            linkedTransactionId = linkedTxnId,
            createdAt = created
        )
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
        // Bank/Cash Assets
        val totalBankBalance = accounts.filter { it.accountType != AccountType.CREDIT_CARD }.sumOf { it.currentBalance }
        // Credit Card Liabilities (Outstanding)
        val totalCreditCardOutstanding = accounts.filter { it.accountType == AccountType.CREDIT_CARD }.sumOf { it.currentBalance }
        // Net Worth = Assets - Liabilities
        val netWorth = totalBankBalance - totalCreditCardOutstanding

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
            // Bank transfers and CC bill payments are balance transfers, NOT expenses or income!
            if (txn.kind == TransactionKind.BANK_TRANSFER || txn.kind == TransactionKind.CARD_PAYMENT) {
                continue
            }

            if (txn.direction == TransactionDirection.CREDIT && txn.kind != TransactionKind.REFUND && txn.kind != TransactionKind.REVERSAL) {
                periodIncome += txn.amount
            } else if (txn.kind == TransactionKind.EXPENSE || txn.kind == TransactionKind.CARD_PURCHASE || txn.kind == TransactionKind.ATM_WITHDRAWAL) {
                periodExpense += txn.amount
                val current = categoryTotals[txn.categoryId] ?: 0.0
                categoryTotals[txn.categoryId] = current + txn.amount
            } else if (txn.kind == TransactionKind.REFUND || txn.kind == TransactionKind.REVERSAL) {
                // Refunds/reversals reduce expenses
                periodExpense = maxOf(0.0, periodExpense - txn.amount)
            }
        }

        val categorySpends = categoryTotals.mapNotNull { (catId, sum) ->
            val cat = categories[catId] ?: return@mapNotNull null
            val pct = if (periodExpense > 0) ((sum / periodExpense) * 100).toFloat() else 0f
            CategorySpend(cat, sum, pct)
        }.sortedByDescending { it.totalAmount }

        // Upcoming Credit Card Dues
        val upcomingDues = accounts
            .filter { it.accountType == AccountType.CREDIT_CARD && (it.totalDue != null || it.currentBalance > 0) }
            .map { cc ->
                val tot = cc.totalDue ?: cc.currentBalance
                val min = cc.minimumDue ?: (tot * 0.05).coerceAtLeast(0.0)
                UpcomingCreditCardDue(
                    account = cc,
                    totalDue = tot,
                    minimumDue = min,
                    dueDate = cc.paymentDueDate ?: "Due Soon",
                    daysRemaining = null
                )
            }

        // Generate Trend Points
        val trendPoints = generateTrendPoints(period, periodTxns)

        return DashboardSummary(
            period = period,
            totalBalance = totalBankBalance,
            creditCardOutstanding = totalCreditCardOutstanding,
            netWorth = netWorth,
            periodIncome = periodIncome,
            periodExpense = periodExpense,
            netSavings = periodIncome - periodExpense,
            categoryBreakdown = categorySpends,
            trendPoints = trendPoints,
            recentTransactions = allTxns.take(20),
            accounts = accounts,
            upcomingCreditCardDues = upcomingDues
        )
    }

    private fun generateTrendPoints(
        period: TimePeriod,
        transactions: List<TransactionWithDetails>
    ): List<TrendPoint> {
        val points = mutableListOf<TrendPoint>()

        when (period) {
            TimePeriod.DAILY -> {
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

                    val dayTxns = transactions.filter {
                        it.transaction.timestamp in start..end &&
                                it.transaction.kind != TransactionKind.BANK_TRANSFER &&
                                it.transaction.kind != TransactionKind.CARD_PAYMENT
                    }
                    val expense = dayTxns.filter { it.transaction.direction == TransactionDirection.DEBIT }.sumOf { it.transaction.amount }
                    val income = dayTxns.filter { it.transaction.direction == TransactionDirection.CREDIT }.sumOf { it.transaction.amount }
                    points.add(TrendPoint(label, expense, income))
                }
            }
            TimePeriod.MONTHLY -> {
                val now = System.currentTimeMillis()
                val weekMillis = 7 * 24 * 60 * 60 * 1000L
                for (i in 3 downTo 0) {
                    val end = now - (i * weekMillis)
                    val start = end - weekMillis
                    val label = "W${4 - i}"
                    val weekTxns = transactions.filter {
                        it.transaction.timestamp in start..end &&
                                it.transaction.kind != TransactionKind.BANK_TRANSFER &&
                                it.transaction.kind != TransactionKind.CARD_PAYMENT
                    }
                    val expense = weekTxns.filter { it.transaction.direction == TransactionDirection.DEBIT }.sumOf { it.transaction.amount }
                    val income = weekTxns.filter { it.transaction.direction == TransactionDirection.CREDIT }.sumOf { it.transaction.amount }
                    points.add(TrendPoint(label, expense, income))
                }
            }
            TimePeriod.YEARLY -> {
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

                    val monthTxns = transactions.filter {
                        it.transaction.timestamp in start..end &&
                                it.transaction.kind != TransactionKind.BANK_TRANSFER &&
                                it.transaction.kind != TransactionKind.CARD_PAYMENT
                    }
                    val expense = monthTxns.filter { it.transaction.direction == TransactionDirection.DEBIT }.sumOf { it.transaction.amount }
                    val income = monthTxns.filter { it.transaction.direction == TransactionDirection.CREDIT }.sumOf { it.transaction.amount }
                    points.add(TrendPoint(label, expense, income))
                }
            }
        }
        return points
    }
}
