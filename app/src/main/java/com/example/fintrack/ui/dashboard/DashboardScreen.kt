package com.example.fintrack.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.fintrack.ui.components.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.DashboardSummary
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.data.model.BillStatus
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.data.model.UpcomingCreditCardDue
import com.example.fintrack.ui.components.CategoryBreakdownView
import com.example.fintrack.ui.components.TrendBarChart
import androidx.compose.material3.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    onAdjustBalanceClick: (Account) -> Unit,
    onMarkBillPaidClick: (Account) -> Unit = {},
    onTransactionClick: (TransactionWithDetails) -> Unit,
    onAddManualClick: () -> Unit,
    pendingVerificationCount: Int = 0,
    onNavigateToVerification: () -> Unit = {},
    pendingReviewTransactions: List<TransactionWithDetails> = emptyList(),
    onReviewTransaction: (TransactionWithDetails) -> Unit = {},
    creditCards: List<Account> = emptyList(),
    onCardClick: (Account) -> Unit = {},
    onManageAccountsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryAccount = summary.accounts.firstOrNull { it.isPrimary } ?: summary.accounts.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header / Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FinTrack Ledger",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "100% Local & Encrypted (AES-256)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onManageAccountsClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(AppIcons.Bank, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Accounts", fontSize = 12.sp)
                }
            }
        }

        // Needs Your Review Queue (fast inline review flow)
        if (pendingReviewTransactions.isNotEmpty()) {
            item {
                NeedsReviewSection(
                    pendingTransactions = pendingReviewTransactions,
                    onReviewClick = onReviewTransaction
                )
            }
        }

        // Verification Required Prompt Banner (if any pending alerts)
        if (pendingVerificationCount > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToVerification() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column {
                                Text(
                                    text = "$pendingVerificationCount Item(s) Require Verification",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Tap to review ambiguous alerts & unlinked card payments",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Multi-Account Financial Balance & Net Worth Card
        item {
            BalanceCard(
                totalBalance = summary.totalBalance,
                creditCardOutstanding = summary.creditCardOutstanding,
                netWorth = summary.netWorth,
                account = primaryAccount,
                onAdjustBalance = {
                    primaryAccount?.let { onAdjustBalanceClick(it) }
                }
            )
        }

        // Credit Cards Section (Status, Spent vs Limit, Dues, and Mark Paid)
        val cardsList = creditCards.ifEmpty { summary.accounts.filter { it.isCreditCard } }
        if (cardsList.isNotEmpty()) {
            item {
                CreditCardsSection(
                    cards = cardsList,
                    onCardClick = onCardClick,
                    onMarkBillPaid = onMarkBillPaidClick
                )
            }
        }

        // Upcoming Credit Card Dues (if any)
        if (summary.upcomingCreditCardDues.isNotEmpty()) {
            item {
                UpcomingDuesCard(
                    dues = summary.upcomingCreditCardDues,
                    onMarkBillPaid = onMarkBillPaidClick
                )
            }
        }

        // Time Period Switcher (Daily / Monthly / Yearly)
        item {
            PrimaryTabRow(
                selectedTabIndex = selectedPeriod.ordinal,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                TimePeriod.values().forEach { period ->
                    Tab(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodSelected(period) },
                        text = {
                            Text(
                                text = period.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // Income vs Expense Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMetricCard(
                    title = "Spent",
                    amount = summary.periodExpense,
                    color = Color(0xFFEF4444),
                    isExpense = true,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = "Received",
                    amount = summary.periodIncome,
                    color = Color(0xFF10B981),
                    isExpense = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Trend Chart
        item {
            TrendBarChart(trendPoints = summary.trendPoints)
        }

        // Category Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    CategoryBreakdownView(categorySpends = summary.categoryBreakdown)
                }
            }
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${summary.recentTransactions.size} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Recent Transactions List
        if (summary.recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions yet.\nWaiting for bank SMS or add manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(summary.recentTransactions) { item ->
                TransactionRow(
                    transactionWithDetails = item,
                    onClick = { onTransactionClick(item) }
                )
            }
        }
    }
}

@Composable
fun BalanceCard(
    totalBalance: Double,
    creditCardOutstanding: Double,
    netWorth: Double,
    account: Account?,
    onAdjustBalance: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF1E3A8A))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Top row: Net worth label & Primary adjust button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Net Liquid Worth",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", netWorth)}",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Button(
                        onClick = onAdjustBalance,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = "Adjust",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Set Bank", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Breakdown: Bank Assets vs CC Liabilities
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bank Accounts Asset
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bank Balance",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", totalBalance)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    // Credit Card Outstanding Liability
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF87171))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Cards Due",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", creditCardOutstanding)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (creditCardOutstanding > 0) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingDuesCard(
    dues: List<UpcomingCreditCardDue>,
    onMarkBillPaid: (Account) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.CreditCard,
                        contentDescription = "Credit Card Dues",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Upcoming Credit Card Dues",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            dues.forEach { due ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = due.account.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Due: ${due.dueDate} • Min ₹${String.format(Locale.getDefault(), "%,.0f", due.minimumDue)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", due.totalDue)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { onMarkBillPaid(due.account) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669)
                            )
                        ) {
                            Text("Bill Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryMetricCard(
    title: String,
    amount: Double,
    color: Color,
    isExpense: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpense) AppIcons.ArrowUp else AppIcons.ArrowDown,
                        contentDescription = title,
                        modifier = Modifier.size(14.dp),
                        tint = color
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "₹${String.format(Locale.getDefault(), "%,.2f", amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TransactionRow(
    transactionWithDetails: TransactionWithDetails,
    onClick: () -> Unit
) {
    val txn = transactionWithDetails.transaction
    val category = transactionWithDetails.category
    val isDebit = txn.direction == TransactionDirection.DEBIT
    val isCardPurchase = txn.kind == TransactionKind.CARD_PURCHASE
    val isCardPayment = txn.kind == TransactionKind.CARD_PAYMENT
    val isTransfer = txn.kind == TransactionKind.BANK_TRANSFER
    val isRefund = txn.kind == TransactionKind.REFUND || txn.kind == TransactionKind.REVERSAL

    val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    val kindBadgeColor = when {
        txn.needsReview -> Color(0xFFF59E0B) // Amber
        isCardPurchase -> Color(0xFFEC4899) // Pink / Magenta
        isCardPayment -> Color(0xFF8B5CF6) // Purple
        isTransfer -> Color(0xFF3B82F6) // Blue
        isRefund -> Color(0xFF10B981) // Green
        else -> Color(category.colorHex)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Kind or Category Icon badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(kindBadgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when {
                        isCardPurchase || isCardPayment -> AppIcons.CreditCard
                        isTransfer -> AppIcons.ArrowForward
                        isRefund -> AppIcons.Check
                        else -> AppIcons.Bank
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = txn.kind.name,
                        tint = kindBadgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    // Title
                    val title = when (txn.kind) {
                        TransactionKind.CARD_PURCHASE -> "Card Spend: ${txn.merchant}"
                        TransactionKind.CARD_PAYMENT -> "Credit Card Payment"
                        TransactionKind.BANK_TRANSFER -> "Bank Transfer: ${txn.merchant}"
                        TransactionKind.REFUND -> "Refund: ${txn.merchant}"
                        TransactionKind.REVERSAL -> "Reversal: ${txn.merchant}"
                        TransactionKind.ATM_WITHDRAWAL -> "ATM Cash Withdrawal"
                        else -> {
                            if (isDebit && !txn.isManual && !txn.merchant.contains("Payment", true)) "Paid to: ${txn.merchant}"
                            else if (!isDebit && !txn.isManual) "Received: ${txn.merchant}"
                            else txn.merchant
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    // Subtitle metadata row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (txn.needsReview) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "Needs Review",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            Text(
                                text = category.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(category.colorHex)
                            )
                        }

                        Text(
                            text = " • ${df.format(Date(txn.timestamp))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val amountColor = when {
                    isCardPayment -> Color(0xFF8B5CF6)
                    isTransfer -> Color(0xFF3B82F6)
                    isRefund -> Color(0xFF10B981)
                    isDebit -> Color(0xFFEF4444)
                    else -> Color(0xFF10B981)
                }

                val sign = when {
                    isCardPayment || isTransfer -> ""
                    isDebit -> "-"
                    else -> "+"
                }

                Text(
                    text = "$sign₹${String.format(Locale.getDefault(), "%,.2f", txn.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                val accountInfo = transactionWithDetails.account
                val label = if (accountInfo.accountType == AccountType.CREDIT_CARD) {
                    "Card ••${accountInfo.accountNumberLast4}"
                } else if (txn.isManual) {
                    "Cash / Manual"
                } else if (accountInfo.accountNumberLast4.isNotEmpty()) {
                    "A/c ••${accountInfo.accountNumberLast4}"
                } else {
                    accountInfo.name
                }

                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NeedsReviewSection(
    pendingTransactions: List<TransactionWithDetails>,
    onReviewClick: (TransactionWithDetails) -> Unit
) {
    if (pendingTransactions.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = AppIcons.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Needs Your Review (${pendingTransactions.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = "ACTION REQUIRED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "FinTrack detected card spending from SMS alerts. Verify or reject each transaction before account liabilities update.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            pendingTransactions.take(3).forEach { item ->
                val txn = item.transaction
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = txn.merchant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = txn.reviewReason ?: "Unconfirmed card debit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.2f", txn.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )

                            Button(
                                onClick = { onReviewClick(item) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Review", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (pendingTransactions.size > 3) {
                Text(
                    text = "+ ${pendingTransactions.size - 3} more items pending review",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun CreditCardsSection(
    cards: List<Account>,
    onCardClick: (Account) -> Unit,
    onMarkBillPaid: (Account) -> Unit
) {
    if (cards.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = AppIcons.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Credit Cards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${cards.size} card${if (cards.size > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        cards.forEach { card ->
            val limit = if (card.creditLimit > 0) card.creditLimit else 1.0
            val spent = card.currentBalance.coerceAtLeast(0.0)
            val available = card.availableCredit ?: maxOf(0.0, limit - spent)
            val progress = (spent / limit).toFloat().coerceIn(0f, 1f)

            val (badgeText, badgeBg, badgeFg) = when (card.billStatus) {
                BillStatus.PAID -> Triple("PAID", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF059669))
                BillStatus.DUE -> Triple("BILL DUE", Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFDC2626))
                BillStatus.PAYMENT_DETECTED -> Triple("PAYMENT DETECTED", Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF2563EB))
                BillStatus.UPCOMING -> Triple("UPCOMING DUE", Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFD97706))
                BillStatus.OVERDUE -> Triple("OVERDUE", Color(0xFFDC2626).copy(alpha = 0.2f), Color(0xFFB91C1C))
                BillStatus.GENERATED -> Triple("STATEMENT READY", Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFF7C3AED))
                else -> Triple("ACTIVE", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick(card) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Top Row: Name, Last4 & Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = card.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${card.bankName} ••${card.accountNumberLast4}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (progress > 0.8f) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Limits row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Spent / Outstanding",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.2f", spent)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (spent > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Available Credit",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.2f", available)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    // Due Info and Mark Bill Paid Action
                    val totalDue = card.totalDue ?: 0.0
                    val hasDue = totalDue > 0.0 || card.paymentDueDate != null
                    if (hasDue) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    if (totalDue > 0.0) {
                                        Text(
                                            text = "Due: ₹${String.format(Locale.getDefault(), "%,.2f", totalDue)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    if (!card.paymentDueDate.isNullOrBlank()) {
                                        Text(
                                            text = "Pay by ${card.paymentDueDate}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onMarkBillPaid(card) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Text("Mark Bill Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

