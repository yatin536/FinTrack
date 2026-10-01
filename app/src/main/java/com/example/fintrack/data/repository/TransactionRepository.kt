package com.example.fintrack.data.repository

import android.content.Context
import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.local.SessionManager
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.AuditLog
import com.example.fintrack.data.model.BankAccountType
import com.example.fintrack.data.model.BillStatus
import com.example.fintrack.data.model.Category
import com.example.fintrack.data.model.DashboardSummary
import com.example.fintrack.data.model.ImportedSmsAlert
import com.example.fintrack.data.model.MessageRule
import com.example.fintrack.data.model.MessageRuleAction
import com.example.fintrack.data.model.ReconciliationLog
import com.example.fintrack.data.model.SmsAlertStatus
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.data.model.TransactionStatus
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.data.model.User
import com.example.fintrack.data.model.VerificationEvent
import com.example.fintrack.data.model.VerificationStatus
import com.example.fintrack.engine.AccountIdentificationEngine
import com.example.fintrack.engine.BalanceReconciliationEngine
import com.example.fintrack.engine.CreditCardPaymentEngine
import com.example.fintrack.engine.FinancialInsight
import com.example.fintrack.engine.FinancialIntelligenceEngine
import com.example.fintrack.engine.IntelligenceAnswer
import com.example.fintrack.engine.ReconciliationResult
import com.example.fintrack.engine.SmsFingerprintEngine
import com.example.fintrack.engine.TransferMatchingEngine
import com.example.fintrack.parser.ExpenseCategorizer
import com.example.fintrack.parser.FinancialInstrumentType
import com.example.fintrack.parser.IndianBankSmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class TransactionRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)
    val sessionManager = SessionManager(context)
    val intelligenceEngine = FinancialIntelligenceEngine(dbHelper)

    private val categorizer = ExpenseCategorizer(dbHelper)
    private val accountEngine = AccountIdentificationEngine(dbHelper)
    private val reconciliationEngine = BalanceReconciliationEngine(dbHelper)
    private val ccPaymentEngine = CreditCardPaymentEngine(dbHelper)
    private val transferEngine = TransferMatchingEngine(dbHelper)

    fun getDashboardSummary(period: TimePeriod, userId: String = sessionManager.getActiveUserId()): Flow<DashboardSummary> = flow {
        emit(dbHelper.getDashboardSummary(period, userId))
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getDashboardSummary(period, userId))
        }
    }.flowOn(Dispatchers.IO)

    fun getTransactions(userId: String = sessionManager.getActiveUserId()): Flow<List<TransactionWithDetails>> = flow {
        emit(dbHelper.getTransactionsWithDetails().filter { it.transaction.userId == userId })
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getTransactionsWithDetails().filter { it.transaction.userId == userId })
        }
    }.flowOn(Dispatchers.IO)

    fun getAccounts(userId: String = sessionManager.getActiveUserId()): Flow<List<Account>> = flow {
        emit(dbHelper.getAccounts(userId))
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getAccounts(userId))
        }
    }.flowOn(Dispatchers.IO)

    fun getCategories(): Flow<List<Category>> = flow {
        emit(dbHelper.getCategories())
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getCategories())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getReconciliationLogs(accountId: String? = null, userId: String = sessionManager.getActiveUserId()): List<ReconciliationLog> =
        withContext(Dispatchers.IO) {
            dbHelper.getReconciliationLogs(accountId).filter { it.userId == userId }
        }

    fun getImportedSmsAlerts(userId: String = sessionManager.getActiveUserId()): Flow<List<ImportedSmsAlert>> = flow {
        emit(dbHelper.getImportedSmsAlerts(userId))
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getImportedSmsAlerts(userId))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun resolveAlert(alertId: String, selectedAccountId: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.resolveImportedSmsAlert(alertId, selectedAccountId)
    }

    // --- Verification System ---
    fun getVerificationEvents(userId: String = sessionManager.getActiveUserId()): Flow<List<VerificationEvent>> = flow {
        emit(dbHelper.getVerificationEvents(userId))
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getVerificationEvents(userId))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun resolveVerificationEvent(
        eventId: String,
        status: VerificationStatus,
        resolvedAccountId: String? = null,
        userId: String = sessionManager.getActiveUserId()
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.resolveVerificationEvent(eventId, status, userId)
    }

    // --- Audit Logs ---
    suspend fun getAuditLogs(userId: String = sessionManager.getActiveUserId()): List<AuditLog> =
        withContext(Dispatchers.IO) {
            dbHelper.getAuditLogs(userId)
        }

    // --- Financial Intelligence ---
    fun getFinancialInsights(userId: String = sessionManager.getActiveUserId()): Flow<List<FinancialInsight>> = flow {
        emit(intelligenceEngine.generateInsights(userId))
        dbHelper.dbChangeSignal.collect {
            emit(intelligenceEngine.generateInsights(userId))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun queryFinancialIntelligence(query: String, userId: String = sessionManager.getActiveUserId()): IntelligenceAnswer =
        withContext(Dispatchers.IO) {
            intelligenceEngine.answerFinancialQuestion(query, userId)
        }

    // --- Multi-User Management ---
    suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
        dbHelper.getUsers()
    }

    suspend fun createUser(name: String, email: String? = null): User = withContext(Dispatchers.IO) {
        val user = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            createdAt = System.currentTimeMillis()
        )
        dbHelper.insertUser(user)
        user
    }

    fun switchUser(user: User) {
        sessionManager.switchUser(user.id, user.name)
    }

    // --- Manual Transactions & Bill Paid Operations ---
    suspend fun addManualTransaction(
        accountId: String,
        categoryId: String,
        amount: Double,
        direction: TransactionDirection,
        kind: TransactionKind = if (direction == TransactionDirection.DEBIT) TransactionKind.EXPENSE else TransactionKind.INCOME,
        merchant: String,
        destinationAccountId: String? = null,
        note: String? = null,
        userId: String = sessionManager.getActiveUserId()
    ): Boolean = withContext(Dispatchers.IO) {
        val txn = Transaction(
            id = UUID.randomUUID().toString(),
            userId = userId,
            accountId = accountId,
            sourceAccountId = accountId,
            destinationAccountId = destinationAccountId,
            categoryId = categoryId,
            amount = amount,
            direction = direction,
            kind = kind,
            timestamp = System.currentTimeMillis(),
            merchant = merchant.ifBlank { "Manual ${kind.name.lowercase().replaceFirstChar { it.uppercase() }}" },
            isManual = true,
            note = note
        )
        dbHelper.insertTransaction(txn)
    }

    /**
     * Mark a credit card bill as Paid.
     * Intelligently recalculates available credit and liability, records transaction, and logs audit record.
     */
    suspend fun confirmBillPaid(
        creditCardId: String,
        amountPaid: Double,
        sourceBankAccountId: String? = null,
        notes: String? = null,
        userId: String = sessionManager.getActiveUserId()
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.confirmBillPaid(
            creditCardId = creditCardId,
            amountPaid = amountPaid,
            payingBankAccountId = sourceBankAccountId,
            note = notes,
            userId = userId
        )
    }

    @Deprecated("Replaced with confirmBillPaid() to reflect user recording instead of gateway processing")
    suspend fun payCreditCardBill(
        sourceBankAccountId: String,
        creditCardAccountId: String,
        amount: Double,
        notes: String? = null
    ): Boolean = confirmBillPaid(
        creditCardId = creditCardAccountId,
        amountPaid = amount,
        sourceBankAccountId = sourceBankAccountId,
        notes = notes
    )

    suspend fun recordBankTransfer(
        sourceBankAccountId: String,
        destinationBankAccountId: String,
        amount: Double,
        referenceNumber: String? = null,
        note: String? = null,
        userId: String = sessionManager.getActiveUserId()
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.recordBankTransfer(sourceBankAccountId, destinationBankAccountId, amount, referenceNumber, note)
    }

    suspend fun reconcileAccount(account: Account, officialBalance: Double): ReconciliationResult =
        withContext(Dispatchers.IO) {
            reconciliationEngine.reconcile(account, officialBalance)
        }

    suspend fun resolveNeedsReviewTransaction(transactionId: String, correctAccountId: String): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.updateTransactionAccount(transactionId, correctAccountId)
        }

    suspend fun updateTransactionCategory(
        transactionId: String,
        newCategoryId: String,
        merchant: String?
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateTransactionCategory(transactionId, newCategoryId, merchant)
    }

    suspend fun deleteTransaction(transactionId: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteTransaction(transactionId)
    }

    suspend fun updateCurrentBalance(accountId: String, newBalance: Double): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.updateCurrentBalance(accountId, newBalance)
        }

    suspend fun updateAccount(account: Account, targetCurrentBalance: Double? = null): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.updateAccount(account, targetCurrentBalance)
        }

    suspend fun deleteAccount(accountId: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteAccount(accountId)
    }

    suspend fun addAccount(account: Account): Boolean = withContext(Dispatchers.IO) {
        dbHelper.insertAccount(account)
    }

    suspend fun addCategory(category: Category): Boolean = withContext(Dispatchers.IO) {
        dbHelper.insertCategory(category)
    }

    // --- Transaction Review & Message Rules ---
    fun getPendingReviewTransactions(userId: String = sessionManager.getActiveUserId()): Flow<List<TransactionWithDetails>> = flow {
        emit(dbHelper.getPendingReviewTransactions(userId))
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getPendingReviewTransactions(userId))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun confirmCreditCardTransaction(
        transactionId: String,
        creditCardId: String,
        userId: String = sessionManager.getActiveUserId()
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.confirmCreditCardTransaction(transactionId, creditCardId, userId)
    }

    suspend fun confirmCreditCardPayment(
        transactionId: String,
        creditCardId: String,
        payingBankAccountId: String?,
        amount: Double,
        userId: String = sessionManager.getActiveUserId()
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.confirmCreditCardPayment(transactionId, creditCardId, payingBankAccountId, amount, userId)
    }

    suspend fun rejectTransaction(
        transactionId: String,
        reason: String = "Marked as not relevant by user",
        learnRule: Boolean = false,
        userId: String = sessionManager.getActiveUserId()
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.rejectTransaction(transactionId, reason, learnRule, userId)
    }

    suspend fun reverseTransaction(
        transactionId: String,
        reason: String = "Reversed by user",
        userId: String = sessionManager.getActiveUserId()
    ): Boolean = withContext(Dispatchers.IO) {
        dbHelper.reverseTransaction(transactionId, reason, userId)
    }

    fun getMessageRules(userId: String = sessionManager.getActiveUserId()): Flow<List<MessageRule>> = flow {
        emit(dbHelper.getMessageRules(userId))
        dbHelper.dbChangeSignal.collect {
            emit(dbHelper.getMessageRules(userId))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteMessageRule(ruleId: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteMessageRule(ruleId)
    }

    /**
     * Production Battery-efficient SMS Processing Pipeline.
     * Invoked exclusively by SmsBroadcastReceiver upon receiving real incoming financial SMS messages.
     * Adheres to: DETECTED -> UNDERSTOOD -> REVIEW REQUIRED -> USER CONFIRMED -> FINANCIAL STATE UPDATED
     */
    suspend fun processIncomingSms(sender: String?, fullBody: String): Boolean = withContext(Dispatchers.IO) {
        if (fullBody.isBlank()) return@withContext false
        val activeUserId = sessionManager.getActiveUserId()

        // 0. Check User-Learned Message Rules (safe false-positive filter suppression)
        val matchedRule = dbHelper.findMatchingMessageRule(sender, fullBody)
        if (matchedRule != null && matchedRule.action == MessageRuleAction.IGNORE) {
            dbHelper.insertImportedSms(
                ImportedSmsAlert(
                    sender = sender ?: "Unknown",
                    body = fullBody,
                    status = SmsAlertStatus.IGNORED,
                    confidence = 1.0,
                    reason = "Suppressed by learned user rule: ${matchedRule.description.ifBlank { matchedRule.senderPattern }}",
                    userId = activeUserId
                )
            )
            return@withContext true
        }

        // 1. Regex Parsing & Fraud/OTP filtering
        val parsed = IndianBankSmsParser.parse(sender, fullBody)
        if (parsed == null) {
            dbHelper.insertImportedSms(
                ImportedSmsAlert(
                    sender = sender ?: "Unknown",
                    body = fullBody,
                    status = SmsAlertStatus.IGNORED,
                    confidence = 0.0,
                    reason = "Non-financial, OTP, or promotional message",
                    userId = activeUserId
                )
            )
            return@withContext false
        }

        // 2. Duplicate Detection via Fingerprinting & UTR
        val fingerprint = SmsFingerprintEngine.generateFingerprint(
            bankName = parsed.bankName,
            amount = parsed.amount,
            last4 = parsed.accountNumberLast4,
            direction = parsed.direction.name,
            referenceNumber = parsed.referenceNumber,
            timestamp = parsed.timestamp
        )

        val duplicateByFp = dbHelper.findTransactionByFingerprint(fingerprint)
        val duplicateByRef = if (!parsed.referenceNumber.isNullOrBlank()) {
            dbHelper.findTransactionByRefNumber(parsed.referenceNumber)
        } else {
            null
        }

        if (duplicateByFp != null || duplicateByRef != null) {
            dbHelper.insertImportedSms(
                ImportedSmsAlert(
                    sender = sender ?: "Unknown",
                    body = fullBody,
                    status = SmsAlertStatus.DUPLICATE,
                    transactionId = duplicateByFp?.id ?: duplicateByRef?.id,
                    confidence = 1.0,
                    reason = "Duplicate alert dropped by fingerprint / UTR check",
                    userId = activeUserId
                )
            )
            return@withContext true
        }

        // 3. Intelligent Account Identification
        val matchResult = accountEngine.identifyAccount(parsed, sender, fullBody, activeUserId)
        val account = matchResult.account
        val needsReview = matchResult.needsReview || account == null
        val targetAccountId = account?.id ?: ""
        val initialStatus = if (needsReview) TransactionStatus.PENDING_REVIEW else TransactionStatus.CONFIRMED

        // 4. Handle Credit Card Bill Statement / Due notice (Informational only, not an expense, NEVER marked as PAID)
        if (parsed.kind == TransactionKind.CARD_BILL_DUE || parsed.kind == TransactionKind.CARD_BILL_GENERATED) {
            val newBillStatus = if (parsed.kind == TransactionKind.CARD_BILL_GENERATED) BillStatus.GENERATED else BillStatus.DUE

            if (account != null && account.isCreditCard) {
                val updatedCc = account.copy(
                    totalDue = parsed.totalDue ?: parsed.amount,
                    minimumDue = parsed.minimumDue ?: account.minimumDue,
                    paymentDueDate = parsed.dueDate ?: account.paymentDueDate,
                    billStatus = newBillStatus
                )
                dbHelper.updateAccount(updatedCc)
            }

            val stmtTxn = Transaction(
                id = UUID.randomUUID().toString(),
                userId = activeUserId,
                accountId = targetAccountId,
                sourceAccountId = targetAccountId,
                categoryId = "cat_bills",
                amount = parsed.amount,
                direction = TransactionDirection.DEBIT,
                kind = parsed.kind,
                timestamp = parsed.timestamp,
                merchant = parsed.merchant,
                rawSmsBody = fullBody,
                smsSender = sender,
                referenceNumber = parsed.referenceNumber,
                isManual = false,
                needsReview = needsReview,
                reviewReason = if (needsReview) matchResult.matchReason else null,
                status = initialStatus,
                fingerprint = fingerprint
            )
            dbHelper.insertTransaction(stmtTxn)

            dbHelper.insertImportedSms(
                ImportedSmsAlert(
                    sender = sender ?: "Unknown",
                    body = fullBody,
                    status = if (needsReview) SmsAlertStatus.NEEDS_REVIEW else SmsAlertStatus.PROCESSED,
                    transactionId = stmtTxn.id,
                    accountId = account?.id,
                    confidence = matchResult.confidence,
                    reason = if (needsReview) matchResult.matchReason else "Credit card bill statement / due notice updated",
                    userId = activeUserId
                )
            )
            return@withContext true
        }

        // 5. Handle Credit Card Bill Payment detection & reconciliation
        if (parsed.kind == TransactionKind.CARD_PAYMENT && account != null && account.isCreditCard) {
            val paymentResult = ccPaymentEngine.processPaymentSms(parsed, account, fullBody)
            val paymentTxnId = if (!paymentResult.wasReconciledWithManual) {
                val paymentTxn = paymentResult.transaction.copy(
                    fingerprint = fingerprint,
                    userId = activeUserId,
                    status = if (paymentResult.needsSourceConfirmation) TransactionStatus.PENDING_REVIEW else TransactionStatus.CONFIRMED
                )
                dbHelper.insertTransaction(paymentTxn)
                paymentTxn.id
            } else {
                paymentResult.transaction.id
            }

            // Update card billStatus based on verification confidence
            val newBillStatus = if (paymentResult.wasReconciledWithManual || !paymentResult.needsSourceConfirmation) {
                BillStatus.PAID
            } else {
                BillStatus.PAYMENT_DETECTED
            }
            val updatedCc = account.copy(
                billStatus = newBillStatus,
                availableCredit = parsed.availableCredit ?: account.availableCredit
            )
            dbHelper.updateAccount(updatedCc)

            dbHelper.insertImportedSms(
                ImportedSmsAlert(
                    sender = sender ?: "Unknown",
                    body = fullBody,
                    status = if (paymentResult.needsSourceConfirmation) SmsAlertStatus.NEEDS_REVIEW else SmsAlertStatus.PROCESSED,
                    transactionId = paymentTxnId,
                    accountId = account.id,
                    confidence = matchResult.confidence,
                    reason = if (paymentResult.wasReconciledWithManual) {
                        paymentResult.reconciliationMessage ?: "Payment reconciled with previous manual bill confirmation"
                    } else if (paymentResult.needsSourceConfirmation) {
                        "Payment detected towards ${account.name}; source bank account needs confirmation"
                    } else {
                        "Credit Card bill payment processed"
                    },
                    userId = activeUserId
                )
            )

            if (!paymentResult.needsSourceConfirmation && parsed.availableCredit != null && account.creditLimit > 0) {
                val newOutstanding = maxOf(0.0, account.creditLimit - parsed.availableCredit)
                reconciliationEngine.reconcile(account, newOutstanding, parsed.timestamp)
            }
            return@withContext true
        }

        // 6. Handle Transfer Matching
        var sourceAccId = targetAccountId
        var destAccId: String? = null
        var txnKind = parsed.kind

        if (parsed.kind == TransactionKind.BANK_TRANSFER && account != null && !account.isCreditCard) {
            val transferMatch = transferEngine.matchTransfer(parsed, account, fullBody)
            if (transferMatch.isTransfer) {
                sourceAccId = transferMatch.sourceAccount?.id ?: account.id
                destAccId = transferMatch.destinationAccount?.id
                txnKind = TransactionKind.BANK_TRANSFER
            }
        }

        // 7. Categorization
        val catId = categorizer.categorize(parsed.merchant, fullBody, parsed.direction)

        // 8. Assemble and Insert Transaction
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            userId = activeUserId,
            accountId = targetAccountId,
            sourceAccountId = sourceAccId,
            destinationAccountId = destAccId,
            categoryId = catId,
            amount = parsed.amount,
            direction = parsed.direction,
            kind = txnKind,
            timestamp = parsed.timestamp,
            merchant = parsed.merchant,
            rawSmsBody = fullBody,
            smsSender = sender,
            referenceNumber = parsed.referenceNumber,
            balanceAfterTxn = parsed.availableBalance,
            availableCreditAfterTxn = parsed.availableCredit,
            isManual = false,
            needsReview = needsReview,
            reviewReason = if (needsReview) matchResult.matchReason else null,
            status = initialStatus,
            fingerprint = fingerprint
        )

        dbHelper.insertTransaction(transaction)

        // 9. Reconcile Balance / Credit ONLY if CONFIRMED and account is known
        if (!needsReview) {
            if (parsed.availableBalance != null && !account.isCreditCard) {
                reconciliationEngine.reconcile(account, parsed.availableBalance, parsed.timestamp)
            } else if (parsed.availableCredit != null && account.isCreditCard) {
                val updatedCc = account.copy(
                    availableCredit = parsed.availableCredit,
                    lastConfirmedBalance = parsed.availableCredit,
                    lastConfirmedAt = parsed.timestamp
                )
                dbHelper.updateAccount(updatedCc)
                if (account.creditLimit > 0) {
                    val confirmedOutstanding = maxOf(0.0, account.creditLimit - parsed.availableCredit)
                    reconciliationEngine.reconcile(account, confirmedOutstanding, parsed.timestamp)
                }
            }

            // Update Dues / Due Dates on Credit Card if present
            if (account.isCreditCard && (parsed.totalDue != null || parsed.minimumDue != null || parsed.dueDate != null)) {
                val updatedCc = account.copy(
                    totalDue = parsed.totalDue ?: account.totalDue,
                    minimumDue = parsed.minimumDue ?: account.minimumDue,
                    paymentDueDate = parsed.dueDate ?: account.paymentDueDate
                )
                dbHelper.updateAccount(updatedCc)
            }
        }

        // 10. Record in Imported SMS Audit Table
        dbHelper.insertImportedSms(
            ImportedSmsAlert(
                sender = sender ?: "Unknown",
                body = fullBody,
                status = if (needsReview) SmsAlertStatus.NEEDS_REVIEW else SmsAlertStatus.PROCESSED,
                transactionId = transaction.id,
                accountId = account?.id,
                confidence = matchResult.confidence,
                reason = if (needsReview) matchResult.matchReason else "Transaction recorded successfully",
                userId = activeUserId
            )
        )

        return@withContext true
    }
}
