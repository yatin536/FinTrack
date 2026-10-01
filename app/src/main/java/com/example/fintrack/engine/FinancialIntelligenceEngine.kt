package com.example.fintrack.engine

import com.example.fintrack.data.local.AppDatabaseHelper
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.BillStatus
import com.example.fintrack.data.model.Transaction
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.User
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class InsightCategory {
    SPENDING_TREND,
    CREDIT_UTILIZATION,
    UPCOMING_BILL,
    CASH_FLOW,
    ANOMALY_DETECTION
}

enum class InsightSeverity {
    INFO,
    WARNING,
    CRITICAL,
    SUCCESS
}

data class FinancialInsight(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val category: InsightCategory,
    val severity: InsightSeverity,
    val actionText: String? = null,
    val groundedMetrics: Map<String, String> = emptyMap()
)

data class IntelligenceAnswer(
    val query: String,
    val answer: String,
    val supportingData: List<Pair<String, String>> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

class FinancialIntelligenceEngine(private val dbHelper: AppDatabaseHelper) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun generateInsights(userId: String = User.DEFAULT_USER_ID): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val accounts = dbHelper.getAccounts(userId)
        val transactions: List<Transaction> = dbHelper.getTransactionsWithDetails(limit = 1000)
            .map { it.transaction }
            .filter { it.userId == userId }
        val now = System.currentTimeMillis()

        // 1. Credit Card Utilization & Dues Insights
        val creditCards = accounts.filter { it.accountType == AccountType.CREDIT_CARD }
        for (card in creditCards) {
            val limit = card.creditLimit
            val used = card.limitUsed
            val utilizationRate = if (limit > 0.0) (used / limit) * 100.0 else 0.0

            if (limit > 0.0) {
                if (utilizationRate > 70.0) {
                    insights.add(
                        FinancialInsight(
                            title = "High Credit Utilization on ${card.name}",
                            summary = "You have used ${"%.1f".format(utilizationRate)}% of your credit limit (${currencyFormat.format(used)} of ${currencyFormat.format(limit)}). Keeping utilization under 30% helps maintain a strong credit profile.",
                            category = InsightCategory.CREDIT_UTILIZATION,
                            severity = InsightSeverity.WARNING,
                            groundedMetrics = mapOf(
                                "Limit" to currencyFormat.format(limit),
                                "Utilized" to currencyFormat.format(used),
                                "Ratio" to "${"%.1f".format(utilizationRate)}%"
                            )
                        )
                    )
                } else if (utilizationRate <= 30.0 && used > 0.0) {
                    insights.add(
                        FinancialInsight(
                            title = "Healthy Credit Utilization on ${card.name}",
                            summary = "Your credit card utilization is at a healthy ${"%.1f".format(utilizationRate)}% (${currencyFormat.format(used)} used).",
                            category = InsightCategory.CREDIT_UTILIZATION,
                            severity = InsightSeverity.SUCCESS,
                            groundedMetrics = mapOf(
                                "Ratio" to "${"%.1f".format(utilizationRate)}%",
                                "Available" to currencyFormat.format(card.availableCredit ?: (limit - used))
                            )
                        )
                    )
                }
            }

            // Bill Due date alerts
            if (!card.paymentDueDate.isNullOrBlank() && (card.totalDue ?: 0.0) > 0.0 && card.billStatus != BillStatus.PAID) {
                val dueDateStr = card.paymentDueDate
                if (card.billStatus == BillStatus.OVERDUE) {
                    insights.add(
                        FinancialInsight(
                            title = "Bill Overdue for ${card.name}",
                            summary = "Your credit card bill of ${currencyFormat.format(card.totalDue)} was reported due on $dueDateStr. Please verify if payment has been made.",
                            category = InsightCategory.UPCOMING_BILL,
                            severity = InsightSeverity.CRITICAL,
                            actionText = "Mark Bill Paid",
                            groundedMetrics = mapOf(
                                "Amount Due" to currencyFormat.format(card.totalDue),
                                "Due Date" to dueDateStr,
                                "Status" to "OVERDUE"
                            )
                        )
                    )
                } else {
                    insights.add(
                        FinancialInsight(
                            title = "Bill Due: ${card.name}",
                            summary = "Total bill amount of ${currencyFormat.format(card.totalDue)} is reported due on $dueDateStr. Ensure sufficient balance in your bank account.",
                            category = InsightCategory.UPCOMING_BILL,
                            severity = InsightSeverity.WARNING,
                            actionText = "Mark Bill Paid",
                            groundedMetrics = mapOf(
                                "Amount Due" to currencyFormat.format(card.totalDue),
                                "Due Date" to dueDateStr,
                                "Status" to card.billStatus.name
                            )
                        )
                    )
                }
            }
        }

        // 2. Spending Trends & Velocity
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        val sixtyDaysAgo = now - (60L * 24 * 60 * 60 * 1000)

        val currentPeriodDebits = transactions.filter {
            it.timestamp in thirtyDaysAgo..now && it.direction == TransactionDirection.DEBIT
        }
        val priorPeriodDebits = transactions.filter {
            it.timestamp in sixtyDaysAgo until thirtyDaysAgo && it.direction == TransactionDirection.DEBIT
        }

        val currentSpend = currentPeriodDebits.sumOf { it.amount }
        val priorSpend = priorPeriodDebits.sumOf { it.amount }

        if (currentPeriodDebits.isNotEmpty()) {
            val topCategory = currentPeriodDebits.groupBy { it.categoryId }
                .maxByOrNull { entry -> entry.value.sumOf { it.amount } }

            val topCategoryName = topCategory?.key?.replace("cat_", "")?.replaceFirstChar { it.uppercase() } ?: "General"
            val topCategoryAmount = topCategory?.value?.sumOf { it.amount } ?: 0.0

            if (priorSpend > 0.0) {
                val diffPercent = ((currentSpend - priorSpend) / priorSpend) * 100.0
                val trend = if (diffPercent > 0) "increased by ${"%.1f".format(diffPercent)}%" else "decreased by ${"%.1f".format(-diffPercent)}%"
                insights.add(
                    FinancialInsight(
                        title = "Monthly Spending Trend",
                        summary = "Your expenses over the last 30 days total ${currencyFormat.format(currentSpend)}, which has $trend compared to the prior 30-day window (${currencyFormat.format(priorSpend)}).",
                        category = InsightCategory.SPENDING_TREND,
                        severity = if (diffPercent > 15) InsightSeverity.WARNING else InsightSeverity.INFO,
                        groundedMetrics = mapOf(
                            "Last 30 Days" to currencyFormat.format(currentSpend),
                            "Prior 30 Days" to currencyFormat.format(priorSpend),
                            "Change" to "${if (diffPercent >= 0) "+" else ""}${"%.1f".format(diffPercent)}%"
                        )
                    )
                )
            }

            insights.add(
                FinancialInsight(
                    title = "Top Spending Category: $topCategoryName",
                    summary = "Your highest spending in the last 30 days was in $topCategoryName, accounting for ${currencyFormat.format(topCategoryAmount)} (${"%.1f".format((topCategoryAmount / currentSpend) * 100)}% of total expenses).",
                    category = InsightCategory.CASH_FLOW,
                    severity = InsightSeverity.INFO,
                    groundedMetrics = mapOf(
                        "Category" to topCategoryName,
                        "Amount" to currencyFormat.format(topCategoryAmount),
                        "Share" to "${"%.1f".format((topCategoryAmount / currentSpend) * 100)}%"
                    )
                )
            )
        }

        // 3. Pending Verification Check
        val pendingEvents = dbHelper.getVerificationEvents(userId).filter { it.status.name == "PENDING" }
        if (pendingEvents.isNotEmpty()) {
            insights.add(
                FinancialInsight(
                    title = "Action Required: ${pendingEvents.size} Unverified Item${if (pendingEvents.size > 1) "s" else ""}",
                    summary = "You have ${pendingEvents.size} pending verification alerts that need confirmation to keep your financial ledger accurate.",
                    category = InsightCategory.ANOMALY_DETECTION,
                    severity = InsightSeverity.WARNING,
                    actionText = "Open Verification Center",
                    groundedMetrics = mapOf(
                        "Pending" to "${pendingEvents.size}",
                        "Status" to "ACTION_NEEDED"
                    )
                )
            )
        }

        return insights
    }

    /**
     * Answers queries strictly grounded in local SQLite financial data.
     */
    fun answerFinancialQuestion(query: String, userId: String = User.DEFAULT_USER_ID): IntelligenceAnswer {
        val q = query.lowercase(Locale.ROOT).trim()
        val accounts = dbHelper.getAccounts(userId)
        val transactions: List<Transaction> = dbHelper.getTransactionsWithDetails(limit = 1000)
            .map { it.transaction }
            .filter { it.userId == userId }

        return when {
            // Net worth / Total balance
            q.contains("net worth") || q.contains("total balance") || q.contains("how much money") -> {
                val bankTotal = accounts.filter { it.accountType != AccountType.CREDIT_CARD }.sumOf { it.currentBalance }
                val ccOutstanding = accounts.filter { it.accountType == AccountType.CREDIT_CARD }.sumOf { it.currentBalance }
                val net = bankTotal - ccOutstanding
                IntelligenceAnswer(
                    query = query,
                    answer = "Your calculated net position is ${currencyFormat.format(net)}. You have ${currencyFormat.format(bankTotal)} in bank & liquid accounts, and ${currencyFormat.format(ccOutstanding)} in credit card liabilities.",
                    supportingData = listOf(
                        "Bank Balances" to currencyFormat.format(bankTotal),
                        "Card Liabilities" to currencyFormat.format(ccOutstanding),
                        "Net Financial Position" to currencyFormat.format(net)
                    )
                )
            }

            // Credit card dues
            q.contains("due") || q.contains("credit card") || q.contains("bill") -> {
                val ccAccounts = accounts.filter { it.accountType == AccountType.CREDIT_CARD }
                if (ccAccounts.isEmpty()) {
                    IntelligenceAnswer(query, "You currently have no credit cards registered in FinTrack.")
                } else {
                    val totalDue = ccAccounts.sumOf { it.totalDue ?: it.currentBalance }
                    val details = ccAccounts.map { card ->
                        val dueStr = card.paymentDueDate ?: "No date"
                        "${card.name}: ${currencyFormat.format(card.totalDue ?: card.currentBalance)} (Due: $dueStr)"
                    }.joinToString("; ")
                    IntelligenceAnswer(
                        query = query,
                        answer = "Total outstanding dues across ${ccAccounts.size} card(s) is ${currencyFormat.format(totalDue)}. Breakdown: $details.",
                        supportingData = ccAccounts.map { it.name to currencyFormat.format(it.totalDue ?: it.currentBalance) }
                    )
                }
            }

            // Spending this month
            q.contains("spent") || q.contains("expense") || q.contains("this month") -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val startOfMonth = cal.timeInMillis
                val monthlyDebits: List<Transaction> = transactions.filter { it.timestamp >= startOfMonth && it.direction == TransactionDirection.DEBIT }
                val totalSpent = monthlyDebits.sumOf { it.amount }
                val topMerchantEntry = monthlyDebits.groupBy { it.merchant ?: "Unknown" }
                    .maxByOrNull { entry -> entry.value.sumOf { t -> t.amount } }

                val topMerchantStr = topMerchantEntry?.let { "${it.key} (${currencyFormat.format(it.value.sumOf { t -> t.amount })})" } ?: "None"
                IntelligenceAnswer(
                    query = query,
                    answer = "You have spent ${currencyFormat.format(totalSpent)} across ${monthlyDebits.size} debit transaction(s) this month. Your top payee is $topMerchantStr.",
                    supportingData = listOf(
                        "Month-to-Date Spend" to currencyFormat.format(totalSpent),
                        "Transaction Count" to "${monthlyDebits.size}",
                        "Top Payee" to topMerchantStr
                    )
                )
            }

            // Highest/Largest transactions
            q.contains("highest") || q.contains("largest") || q.contains("big spend") -> {
                val largest: List<Transaction> = transactions.filter { it.direction == TransactionDirection.DEBIT }
                    .sortedByDescending { it.amount }
                    .take(3)
                if (largest.isEmpty()) {
                    IntelligenceAnswer(query, "No expense transactions found in your records.")
                } else {
                    val summary = largest.mapIndexed { idx: Int, t: Transaction ->
                        val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(t.timestamp))
                        "#${idx + 1}: ${currencyFormat.format(t.amount)} at ${t.merchant ?: "Unknown"} on $date"
                    }.joinToString("\n")
                    IntelligenceAnswer(
                        query = query,
                        answer = "Here are your top 3 largest transactions on record:\n$summary",
                        supportingData = largest.map { (it.merchant ?: "Payment") to currencyFormat.format(it.amount) }
                    )
                }
            }

            // Default fallback with summary stats
            else -> {
                val bankTotal = accounts.filter { it.accountType != AccountType.CREDIT_CARD }.sumOf { it.currentBalance }
                IntelligenceAnswer(
                    query = query,
                    answer = "Based on your local financial ledger: you have ${accounts.size} account(s) tracked with a liquid balance of ${currencyFormat.format(bankTotal)} and ${transactions.size} total recorded transactions. Ask me about your 'credit card dues', 'this month spend', 'net worth', or 'largest expenses'.",
                    supportingData = listOf(
                        "Accounts Count" to "${accounts.size}",
                        "Transactions Count" to "${transactions.size}",
                        "Liquid Balance" to currencyFormat.format(bankTotal)
                    )
                )
            }
        }
    }
}
