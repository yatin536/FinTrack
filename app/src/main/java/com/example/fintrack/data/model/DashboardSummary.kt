package com.example.fintrack.data.model

enum class TimePeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class CategorySpend(
    val category: Category,
    val totalAmount: Double,
    val percentage: Float
) {
    val totalAmountMoney: Money get() = Money.fromDouble(totalAmount)
}

data class TrendPoint(
    val label: String, // e.g. "Mon", "Sep 28", "May"
    val expense: Double,
    val income: Double
) {
    val expenseMoney: Money get() = Money.fromDouble(expense)
    val incomeMoney: Money get() = Money.fromDouble(income)
}

data class UpcomingCreditCardDue(
    val account: Account,
    val totalDue: Double,
    val minimumDue: Double,
    val dueDate: String,
    val daysRemaining: Int? = null
) {
    val totalDueMoney: Money get() = Money.fromDouble(totalDue)
    val minimumDueMoney: Money get() = Money.fromDouble(minimumDue)
}

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
) {
    val totalBalanceMoney: Money get() = Money.fromDouble(totalBalance)
    val creditCardOutstandingMoney: Money get() = Money.fromDouble(creditCardOutstanding)
    val netWorthMoney: Money get() = Money.fromDouble(netWorth)
    val periodIncomeMoney: Money get() = Money.fromDouble(periodIncome)
    val periodExpenseMoney: Money get() = Money.fromDouble(periodExpense)
    val netSavingsMoney: Money get() = Money.fromDouble(netSavings)
}
