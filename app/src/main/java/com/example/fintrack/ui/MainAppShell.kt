package com.example.fintrack.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.local.PinManager
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.BankAccountType
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.data.model.VerificationStatus
import com.example.fintrack.data.repository.TransactionRepository
import com.example.fintrack.ui.accounts.AccountsScreen
import com.example.fintrack.ui.accounts.CreditCardDetailsScreen
import com.example.fintrack.ui.auth.AuthScreen
import com.example.fintrack.ui.components.AddTransactionDialog
import com.example.fintrack.ui.components.AdjustBalanceDialog
import com.example.fintrack.ui.components.AppIcons
import com.example.fintrack.ui.components.BillPaidDialog
import com.example.fintrack.ui.components.ChangeCategoryDialog
import com.example.fintrack.ui.components.TransactionReviewSheet
import com.example.fintrack.ui.dashboard.DashboardScreen
import com.example.fintrack.ui.intelligence.IntelligenceCenterScreen
import com.example.fintrack.ui.profile.ProfileSettingsScreen
import com.example.fintrack.ui.splash.SplashScreen
import com.example.fintrack.ui.transactions.TransactionsScreen
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab {
    HOME,
    ACTIVITY,
    INSIGHTS,
    PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    isDark: Boolean = true,
    onToggleDarkMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { TransactionRepository(context) }
    val pinManager = remember { PinManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSplash by remember { mutableStateOf(true) }
    var isAuthenticated by remember { mutableStateOf(!pinManager.isPinSet) }
    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTHLY) }

    // Screen Navigation States
    var selectedCreditCard by remember { mutableStateOf<Account?>(null) }
    var payingCreditCard by remember { mutableStateOf<Account?>(null) }
    var showAccountsScreen by remember { mutableStateOf(false) }

    // Active Dialog States
    var adjustingAccount by remember { mutableStateOf<Account?>(null) }
    var editingCategoryTxn by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var reviewingTxn by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var showAddTxnDialog by remember { mutableStateOf(false) }

    // Multi-User active key for refreshing
    var userRefreshTrigger by remember { mutableStateOf(0) }

    // Reactive State Flows from Repository
    val summary by repository.getDashboardSummary(selectedPeriod).collectAsState(initial = com.example.fintrack.data.model.DashboardSummary())
    val transactions by repository.getTransactions().collectAsState(initial = emptyList())
    val accounts by repository.getAccounts().collectAsState(initial = emptyList())
    val categories by repository.getCategories().collectAsState(initial = emptyList())
    val verificationEvents by repository.getVerificationEvents().collectAsState(initial = emptyList())
    val insights by repository.getFinancialInsights().collectAsState(initial = emptyList())
    val pendingReviewTxns by repository.getPendingReviewTransactions().collectAsState(initial = emptyList())
    val creditCards = remember(accounts) { accounts.filter { it.isCreditCard } }

    val pendingVerificationCount = remember(verificationEvents) {
        verificationEvents.count { it.status == VerificationStatus.PENDING }
    }

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    Crossfade(targetState = isAuthenticated, label = "AuthCrossfade") { authenticated ->
        if (!authenticated) {
            AuthScreen(
                pinManager = pinManager,
                onAuthenticated = { isAuthenticated = true }
            )
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FinTrack",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "V2 PRO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        actions = {
                            IconButton(onClick = onToggleDarkMode) {
                                Icon(
                                    imageVector = if (isDark) AppIcons.LightMode else AppIcons.DarkMode,
                                    contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { isAuthenticated = false }) {
                                Icon(
                                    imageVector = AppIcons.Lock,
                                    contentDescription = "Lock Vault",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.HOME && selectedCreditCard == null && !showAccountsScreen,
                            onClick = {
                                currentTab = AppTab.HOME
                                selectedCreditCard = null
                                showAccountsScreen = false
                            },
                            icon = {
                                if (pendingReviewTxns.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge { Text("${pendingReviewTxns.size}") }
                                        }
                                    ) {
                                        Icon(AppIcons.Dashboard, contentDescription = "Home")
                                    }
                                } else {
                                    Icon(AppIcons.Dashboard, contentDescription = "Home")
                                }
                            },
                            label = { Text("Home", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.ACTIVITY && selectedCreditCard == null && !showAccountsScreen,
                            onClick = {
                                currentTab = AppTab.ACTIVITY
                                selectedCreditCard = null
                                showAccountsScreen = false
                            },
                            icon = { Icon(AppIcons.Activity, contentDescription = "Activity") },
                            label = { Text("Activity", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.INSIGHTS && selectedCreditCard == null && !showAccountsScreen,
                            onClick = {
                                currentTab = AppTab.INSIGHTS
                                selectedCreditCard = null
                                showAccountsScreen = false
                            },
                            icon = { Icon(AppIcons.TrendingUp, contentDescription = "Insights") },
                            label = { Text("Insights", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.PROFILE && selectedCreditCard == null && !showAccountsScreen,
                            onClick = {
                                currentTab = AppTab.PROFILE
                                selectedCreditCard = null
                                showAccountsScreen = false
                            },
                            icon = { Icon(AppIcons.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontSize = 11.sp) }
                        )
                    }
                },
                floatingActionButton = {
                    if (selectedCreditCard == null && !showAccountsScreen && (currentTab == AppTab.HOME || currentTab == AppTab.ACTIVITY)) {
                        FloatingActionButton(
                            onClick = { showAddTxnDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(AppIcons.Add, contentDescription = "Add Transaction")
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    when {
                        selectedCreditCard != null -> {
                            val activeCard = accounts.firstOrNull { it.id == selectedCreditCard!!.id } ?: selectedCreditCard!!
                            val cardTxns = transactions.filter {
                                it.transaction.accountId == activeCard.id ||
                                        it.transaction.sourceAccountId == activeCard.id ||
                                        it.transaction.destinationAccountId == activeCard.id
                            }
                            CreditCardDetailsScreen(
                                card = activeCard,
                                allAccounts = accounts,
                                cardTransactions = cardTxns,
                                onBackClick = { selectedCreditCard = null },
                                onPayCardClick = { payingCreditCard = activeCard },
                                onAdjustBalanceClick = { adjustingAccount = activeCard },
                                onEditClick = { /* Handled in accounts tab */ },
                                onDeleteClick = {
                                    coroutineScope.launch {
                                        repository.deleteAccount(activeCard.id)
                                        selectedCreditCard = null
                                        snackbarHostState.showSnackbar("Credit Card deleted")
                                    }
                                },
                                onTransactionClick = { editingCategoryTxn = it }
                            )
                        }
                        showAccountsScreen -> {
                            AccountsScreen(
                                accounts = accounts,
                                onAdjustBalanceClick = { adjustingAccount = it },
                                onCardClick = {
                                    selectedCreditCard = it
                                    showAccountsScreen = false
                                },
                                onMarkBillPaidClick = { payingCreditCard = it },
                                onAddAccount = { name, bank, type, subType, last4, initBal, limit, stmt, due ->
                                    coroutineScope.launch {
                                        val newAcc = Account(
                                            id = UUID.randomUUID().toString(),
                                            name = name,
                                            bankName = bank,
                                            accountType = type,
                                            bankAccountType = subType,
                                            accountNumberLast4 = last4,
                                            initialBalance = initBal,
                                            currentBalance = initBal,
                                            creditLimit = limit,
                                            statementDate = stmt,
                                            paymentDueDate = due,
                                            colorHex = if (type == AccountType.CREDIT_CARD) 0xFFEC4899 else 0xFF2563EB
                                        )
                                        repository.addAccount(newAcc)
                                        snackbarHostState.showSnackbar("Added: $name")
                                    }
                                },
                                onEditAccount = { account, name, bank, type, subType, last4, curBal, limit, stmt, due ->
                                    coroutineScope.launch {
                                        val updated = account.copy(
                                            name = name,
                                            bankName = bank,
                                            accountType = type,
                                            bankAccountType = subType,
                                            accountNumberLast4 = last4,
                                            creditLimit = limit,
                                            statementDate = stmt,
                                            paymentDueDate = due
                                        )
                                        repository.updateAccount(updated, curBal)
                                        snackbarHostState.showSnackbar("Updated: $name")
                                    }
                                },
                                onDeleteAccount = { account ->
                                    coroutineScope.launch {
                                        repository.deleteAccount(account.id)
                                        snackbarHostState.showSnackbar("Deleted: ${account.name}")
                                    }
                                },
                                onBackClick = { showAccountsScreen = false }
                            )
                        }
                        currentTab == AppTab.HOME -> {
                            DashboardScreen(
                                summary = summary,
                                selectedPeriod = selectedPeriod,
                                onPeriodSelected = { selectedPeriod = it },
                                onAdjustBalanceClick = { adjustingAccount = it },
                                onMarkBillPaidClick = { payingCreditCard = it },
                                onTransactionClick = { editingCategoryTxn = it },
                                onAddManualClick = { showAddTxnDialog = true },
                                pendingVerificationCount = pendingVerificationCount,
                                onNavigateToVerification = { /* Handled inline */ },
                                pendingReviewTransactions = pendingReviewTxns,
                                onReviewTransaction = { reviewingTxn = it },
                                creditCards = creditCards,
                                onCardClick = { selectedCreditCard = it },
                                onManageAccountsClick = { showAccountsScreen = true }
                            )
                        }
                        currentTab == AppTab.ACTIVITY -> {
                            TransactionsScreen(
                                transactions = transactions,
                                categories = categories,
                                onTransactionClick = { editingCategoryTxn = it },
                                onReviewClick = { reviewingTxn = it },
                                onReverseClick = { item ->
                                    coroutineScope.launch {
                                        repository.reverseTransaction(item.transaction.id)
                                        snackbarHostState.showSnackbar("Transaction reversed and ledger recalculated")
                                    }
                                },
                                onDeleteClick = { item ->
                                    coroutineScope.launch {
                                        repository.deleteTransaction(item.transaction.id)
                                        snackbarHostState.showSnackbar("Transaction deleted")
                                    }
                                }
                            )
                        }
                        currentTab == AppTab.INSIGHTS -> {
                            IntelligenceCenterScreen(
                                repository = repository,
                                insights = insights,
                                onNavigateToVerification = { currentTab = AppTab.HOME },
                                onNavigateToAccounts = { showAccountsScreen = true }
                            )
                        }
                        currentTab == AppTab.PROFILE -> {
                            ProfileSettingsScreen(
                                repository = repository,
                                onUserSwitched = {
                                    userRefreshTrigger++
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Active profile switched!")
                                    }
                                },
                                onNavigateToAccounts = { showAccountsScreen = true }
                            )
                        }
                    }
                }
            }

            // Bottom Sheet: Fast Review Flow
            reviewingTxn?.let { item ->
                TransactionReviewSheet(
                    item = item,
                    allAccounts = accounts,
                    onDismiss = { reviewingTxn = null },
                    onConfirmCreditCard = { cardId ->
                        coroutineScope.launch {
                            val ok = repository.confirmCreditCardTransaction(item.transaction.id, cardId)
                            reviewingTxn = null
                            if (ok) {
                                snackbarHostState.showSnackbar("Confirmed as Credit Card purchase! Balance updated.")
                            }
                        }
                    },
                    onConfirmBankAccount = { bankAccountId ->
                        coroutineScope.launch {
                            val ok = repository.resolveNeedsReviewTransaction(item.transaction.id, bankAccountId)
                            reviewingTxn = null
                            if (ok) {
                                snackbarHostState.showSnackbar("Assigned to Bank Account!")
                            }
                        }
                    },
                    onReject = { reason, learnRule ->
                        coroutineScope.launch {
                            val ok = repository.rejectTransaction(item.transaction.id, reason, learnRule)
                            reviewingTxn = null
                            if (ok) {
                                snackbarHostState.showSnackbar(
                                    if (learnRule) "Alert discarded & filter rule learned!" else "Transaction discarded"
                                )
                            }
                        }
                    }
                )
            }

            // Dialog: Adjust Balance
            adjustingAccount?.let { account ->
                AdjustBalanceDialog(
                    account = account,
                    onDismiss = { adjustingAccount = null },
                    onConfirm = { newBal ->
                        coroutineScope.launch {
                            repository.updateCurrentBalance(account.id, newBal)
                            adjustingAccount = null
                            snackbarHostState.showSnackbar("Balance updated for ${account.name}")
                        }
                    }
                )
            }

            // Dialog: Mark Credit Card Bill Paid
            payingCreditCard?.let { card ->
                val bankAccounts = accounts.filter { it.accountType == AccountType.BANK_ACCOUNT }
                BillPaidDialog(
                    creditCard = card,
                    bankAccounts = bankAccounts,
                    onDismiss = { payingCreditCard = null },
                    onConfirmBillPaid = { bankId, amount, note ->
                        coroutineScope.launch {
                            val success = repository.confirmBillPaid(
                                creditCardId = card.id,
                                amountPaid = amount,
                                sourceBankAccountId = bankId,
                                notes = note
                            )
                            payingCreditCard = null
                            if (success) {
                                snackbarHostState.showSnackbar("Bill marked as paid! Available credit limit updated.")
                            } else {
                                snackbarHostState.showSnackbar("Failed to record bill payment.")
                            }
                        }
                    }
                )
            }

            // Dialog: Change Category
            editingCategoryTxn?.let { item ->
                ChangeCategoryDialog(
                    transactionWithDetails = item,
                    categories = categories,
                    onDismiss = { editingCategoryTxn = null },
                    onCategorySelected = { newCatId, rememberRule ->
                        coroutineScope.launch {
                            repository.updateTransactionCategory(
                                transactionId = item.transaction.id,
                                newCategoryId = newCatId,
                                merchant = if (rememberRule) item.transaction.merchant else null
                            )
                            editingCategoryTxn = null
                            snackbarHostState.showSnackbar("Category updated!")
                        }
                    }
                )
            }

            // Dialog: Add Manual Transaction
            if (showAddTxnDialog) {
                AddTransactionDialog(
                    accounts = accounts,
                    categories = categories,
                    onDismiss = { showAddTxnDialog = false },
                    onConfirm = { accId, catId, amount, type, merchant, note ->
                        coroutineScope.launch {
                            repository.addManualTransaction(
                                accountId = accId,
                                categoryId = catId,
                                amount = amount,
                                direction = type,
                                merchant = merchant,
                                note = note
                            )
                            showAddTxnDialog = false
                            snackbarHostState.showSnackbar("Transaction recorded")
                        }
                    }
                )
            }
        }
    }
}
