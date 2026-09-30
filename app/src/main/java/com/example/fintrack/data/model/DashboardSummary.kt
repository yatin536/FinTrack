package com.example.fintrack.data.model

enum class TimePeriod {
    DAILY,
    MONTHLY,
    YEARLY
}

data class CategorySpend(
    val category: Category,
    val totalAmount: Double,
    val percentage: Float
)

data class TrendPoint(
    val label: String, // e.g. "Mon", "Sep 28", "May"
    val expense: Double,
    val income: Double
)

data class UpcomingCreditCardDue(
    val account: Account,
    val totalDue: Double,
    val minimumDue: Double,
    val dueDate: String,
    val daysRemaining: Int? = null
)

data class DashboardSummary(
    val period: TimePeriod = TimePeriod.MONTHLY,
    val totalBalance: Double = 0.0, // Bank / Cash Assets
    val creditCardOutstanding: Double = 0.0, // Credit Card Liabilities
    val netWorth: Double = 0.0, // Assets - Liabilities
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val trendPoints: List<TrendPoint> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val upcomingCreditCardDues: List<UpcomingCreditCardDue> = emptyList()
)
