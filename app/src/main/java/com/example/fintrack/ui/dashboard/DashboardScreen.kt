package com.example.fintrack.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.BillStatus
import com.example.fintrack.data.model.DashboardSummary
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.data.model.UpcomingCreditCardDue
import com.example.fintrack.theme.CyberCyan
import com.example.fintrack.theme.ElectricBlue
import com.example.fintrack.theme.EmeraldGreen
import com.example.fintrack.theme.NeonCoral
import com.example.fintrack.theme.PastelBills
import com.example.fintrack.theme.PastelBillsIcon
import com.example.fintrack.theme.PastelEntertainment
import com.example.fintrack.theme.PastelEntertainmentIcon
import com.example.fintrack.theme.PastelFood
import com.example.fintrack.theme.PastelFoodIcon
import com.example.fintrack.theme.PastelHealth
import com.example.fintrack.theme.PastelHealthIcon
import com.example.fintrack.theme.PastelOther
import com.example.fintrack.theme.PastelOtherIcon
import com.example.fintrack.theme.PastelSalary
import com.example.fintrack.theme.PastelSalaryIcon
import com.example.fintrack.theme.PastelShopping
import com.example.fintrack.theme.PastelShoppingIcon
import com.example.fintrack.theme.PastelTravel
import com.example.fintrack.theme.PastelTravelIcon
import com.example.fintrack.ui.components.AppIcons
import com.example.fintrack.ui.components.AuroraBackground
import com.example.fintrack.ui.components.CategoryBreakdownView
import com.example.fintrack.ui.components.GlassCard
import com.example.fintrack.ui.components.OfflineStatusBadge
import com.example.fintrack.ui.components.SegmentedPeriodSelector
import com.example.fintrack.ui.components.TrendBarChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    selectedPeriod: TimePeriod,
    userName: String = "Yatin Kumar Singh",
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

    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val firstName = userName.trim().split(" ").firstOrNull()?.ifBlank { "User" } ?: "User"
    val cardsList = creditCards.ifEmpty { summary.accounts.filter { it.isCreditCard } }

    AuroraBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
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
                            text = "$greeting,",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$firstName 👋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        OfflineStatusBadge(text = "100% AIR-GAPPED • OFFLINE")
                    }

                    // Accounts Action Button
                    GlassCard(
                        shape = RoundedCornerShape(14.dp),
                        onClick = onManageAccountsClick
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Bank,
                                contentDescription = "Accounts",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Accounts",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                    GlassCard(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onNavigateToVerification() }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33F59E0B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "$pendingVerificationCount Item(s) Require Verification",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Tap to review ambiguous alerts & unlinked card payments",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4-Dot Account Balance Hero Glass Carousel
            item {
                HeroAccountBalanceCarousel(
                    summary = summary,
                    primaryAccount = primaryAccount,
                    creditCards = cardsList,
                    selectedPeriod = selectedPeriod,
                    onAdjustBalanceClick = onAdjustBalanceClick,
                    onMarkBillPaidClick = onMarkBillPaidClick,
                    onManageAccountsClick = onManageAccountsClick,
                    onCardClick = onCardClick
                )
            }

            // Credit Cards Section (Status, Spent vs Limit, Dues, and Mark Paid)
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

            // Segmented Period Switcher (Daily / Weekly / Monthly)
            item {
                SegmentedPeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }

            // Cash Flow Analytics Chart
            item {
                TrendBarChart(trendPoints = summary.trendPoints)
            }

            // Category Breakdown Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))
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
                        text = "Today's Spends & Ledger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.recentTransactions.size} recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Recent Transactions List
            if (summary.recentTransactions.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions recorded yet.\nWaiting for bank SMS or add manually with +.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
}

/**
 * 4-Dot Hero Balance Carousel with page indicator dots:
 * Page 0: Net Worth & Assets
 * Page 1: Primary Bank Account
 * Page 2: Credit Card Liability Hero
 * Page 3: Period Cash Flow Overview
 */
@Composable
fun HeroAccountBalanceCarousel(
    summary: DashboardSummary,
    primaryAccount: Account?,
    creditCards: List<Account>,
    selectedPeriod: TimePeriod,
    onAdjustBalanceClick: (Account) -> Unit,
    onMarkBillPaidClick: (Account) -> Unit,
    onManageAccountsClick: () -> Unit,
    onCardClick: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 4 })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) { page ->
            when (page) {
                0 -> {
                    // Card 0: Net Worth & Liquid Assets
                    GlassCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NET WORTH & ASSETS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { primaryAccount?.let { onAdjustBalanceClick(it) } }
                                ) {
                                    Text(
                                        text = "Adjust",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = summary.netWorthMoney.format(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Bank assets pill
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1A10B981))
                                        .border(1.dp, Color(0x3310B981), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text("Bank Assets", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(summary.totalBalanceMoney.format(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                }

                                // Credit Card liabilities pill
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1AF43F5E))
                                        .border(1.dp, Color(0x33F43F5E), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF43F5E))
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text("Liabilities", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(summary.creditCardOutstandingMoney.format(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Card 1: Primary Bank Account
                    GlassCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        onClick = onManageAccountsClick
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (primaryAccount?.bankName ?: "PRIMARY ACCOUNT").uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (primaryAccount?.lastConfirmedAt != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x1A10B981))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                        Spacer(Modifier.width(4.dp))
                                        Text("SMS Verified", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = primaryAccount?.name ?: "No Bank Account",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = primaryAccount?.currentBalanceMoney?.format() ?: "₹0.00",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (!primaryAccount?.accountNumberLast4.isNullOrEmpty()) "•••• ${primaryAccount?.accountNumberLast4}" else "Savings Ledger",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "View Details →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Card 2: Credit Card Liability Hero
                    val firstCard = creditCards.firstOrNull()
                    GlassCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        onClick = { firstCard?.let { onCardClick(it) } ?: onManageAccountsClick() }
                    ) {
                        if (firstCard != null) {
                            val limit = firstCard.creditLimit
                            val used = firstCard.limitUsed
                            val progress = if (limit > 0) (used / limit).toFloat().coerceIn(0f, 1f) else 0f
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = firstCard.name.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = Color(0xFFFB7185)
                                    )
                                    if (!firstCard.paymentDueDate.isNullOrBlank()) {
                                        Text(
                                            text = "Due: ${firstCard.paymentDueDate}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFF43F5E)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text("Outstanding Debt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            firstCard.outstandingBalanceMoney.format(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (used > 0) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Available Limit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            firstCard.limitLeftMoney.format(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }

                                // Progress bar
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = if (progress > 0.7f) Color(0xFFF43F5E) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("•••• ${firstCard.accountNumberLast4}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Mark Bill Paid →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(AppIcons.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No Credit Cards Added", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Tap to link a credit card to track limits and bill dues", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                3 -> {
                    // Card 3: Period Cash Flow Overview
                    GlassCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${selectedPeriod.name} CASH FLOW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Inflow", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(summary.periodIncomeMoney.format(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Outflow", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(summary.periodExpenseMoney.format(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF43F5E))
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (summary.netSavingsMoney.isNegative) Color(0x1AF43F5E) else Color(0x1A10B981))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Net Period Savings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    summary.netSavingsMoney.formatSigned(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (summary.netSavingsMoney.isNegative) Color(0xFFF43F5E) else Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4-Dot Indicator Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { idx ->
                val isSelected = (pagerState.currentPage == idx)
                val width = if (isSelected) 20.dp else 6.dp
                val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = width, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}

fun getCategoryPastel(categoryId: String): Pair<Color, Color> {
    return when (categoryId.lowercase()) {
        "cat_food", "cat_dining", "food" -> PastelFood to PastelFoodIcon
        "cat_shopping", "shopping" -> PastelShopping to PastelShoppingIcon
        "cat_travel", "cat_fuel", "travel" -> PastelTravel to PastelTravelIcon
        "cat_bills", "cat_utilities", "bills" -> PastelBills to PastelBillsIcon
        "cat_salary", "cat_income", "salary" -> PastelSalary to PastelSalaryIcon
        "cat_transfer", "transfer" -> PastelSalary to PastelSalaryIcon
        "cat_entertainment", "entertainment" -> PastelEntertainment to PastelEntertainmentIcon
        "cat_health", "cat_medical", "health" -> PastelHealth to PastelHealthIcon
        else -> PastelOther to PastelOtherIcon
    }
}

fun getCategoryIcon(categoryId: String): ImageVector {
    return when (categoryId.lowercase()) {
        "cat_food", "cat_dining", "food" -> AppIcons.Restaurant
        "cat_shopping", "shopping" -> AppIcons.ShoppingBag
        "cat_travel", "cat_fuel", "travel" -> AppIcons.Activity
        "cat_bills", "cat_utilities", "bills" -> AppIcons.ReceiptLong
        "cat_salary", "cat_income", "salary" -> AppIcons.TrendingUp
        "cat_transfer", "transfer" -> AppIcons.Bank
        "cat_entertainment", "entertainment" -> AppIcons.Activity
        "cat_health", "cat_medical", "health" -> AppIcons.Shield
        else -> AppIcons.Activity
    }
}

@Composable
fun TransactionRow(
    transactionWithDetails: TransactionWithDetails,
    onClick: () -> Unit
) {
    val txn = transactionWithDetails.transaction
    val isDebit = txn.direction == TransactionDirection.DEBIT
    val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val (pastelBg, pastelIconTint) = getCategoryPastel(txn.categoryId)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pastel Squircle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pastelBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(txn.categoryId),
                        contentDescription = null,
                        tint = pastelIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = txn.merchant,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${transactionWithDetails.account.name} • ${df.format(Date(txn.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = txn.amountMoney.formatSigned(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = if (isDebit) NeonCoral else EmeraldGreen
            )
        }
    }
}

@Composable
fun NeedsReviewSection(
    pendingTransactions: List<TransactionWithDetails>,
    onReviewClick: (TransactionWithDetails) -> Unit
) {
    GlassCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x33F59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Needs Your Review (${pendingTransactions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "Action Needed",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
            }

            Spacer(Modifier.height(12.dp))

            pendingTransactions.take(3).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .clickable { onReviewClick(item) }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.transaction.merchant, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            item.transaction.reviewReason ?: "Ambiguous credit card / bank match",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        item.transaction.amountMoney.format(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun UpcomingDuesCard(
    dues: List<UpcomingCreditCardDue>,
    onMarkBillPaid: (Account) -> Unit
) {
    GlassCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Upcoming Credit Card Dues",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            dues.forEach { due ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(due.account.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Due: ${due.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            due.totalDueMoney.format(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = NeonCoral
                        )
                        Button(
                            onClick = { onMarkBillPaid(due.account) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("Mark Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Credit Cards (${cards.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        cards.forEach { card ->
            val spent = card.limitUsed
            val limit = card.creditLimit
            val available = card.limitLeft
            val progress = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f

            GlassCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { onCardClick(card) }
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(card.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                if (card.accountNumberLast4.isNotEmpty()) "•••• ${card.accountNumberLast4}" else card.bankName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (card.isBillPaid) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x2610B981)
                            ) {
                                Text(
                                    "PAID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (progress > 0.7f) NeonCoral else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Spent / Due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                card.outstandingBalanceMoney.format(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (spent > 0) NeonCoral else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Available Limit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                card.limitLeftMoney.format(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }

                    if (card.totalDue != null && card.totalDue > 0.0 && !card.isBillPaid) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onMarkBillPaid(card) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                            ) {
                                Text("Mark Bill Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
