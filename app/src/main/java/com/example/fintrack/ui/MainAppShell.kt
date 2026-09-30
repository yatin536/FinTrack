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
import com.example.fintrack.ui.components.AppIcons
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
import com.example.fintrack.data.model.SmsAlertStatus
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.data.repository.TransactionRepository
import com.example.fintrack.ui.accounts.AccountsScreen
import com.example.fintrack.ui.accounts.CreditCardDetailsScreen
import com.example.fintrack.ui.alerts.SmsIntelligenceScreen
import com.example.fintrack.ui.auth.AuthScreen
import com.example.fintrack.ui.components.AddTransactionDialog
import com.example.fintrack.ui.components.AdjustBalanceDialog
import com.example.fintrack.ui.components.ChangeCategoryDialog
import com.example.fintrack.ui.components.PayCreditCardDialog
import com.example.fintrack.ui.components.SimulateSmsDialog
import com.example.fintrack.ui.dashboard.DashboardScreen
import com.example.fintrack.ui.transactions.TransactionsScreen
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab {
    DASHBOARD,
    TRANSACTIONS,
    ACCOUNTS
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

    var isAuthenticated by remember { mutableStateOf(!pinManager.isPinSet) }
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTHLY) }

    // Screen Navigation States
    var selectedCreditCard by remember { mutableStateOf<Account?>(null) }
    var payingCreditCard by remember { mutableStateOf<Account?>(null) }
    var showSmsIntelligence by remember { mutableStateOf(false) }

    // Active Dialog States
    var adjustingAccount by remember { mutableStateOf<Account?>(null) }
    var editingCategoryTxn by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var showAddTxnDialog by remember { mutableStateOf(false) }
    var showSimulateSmsDialog by remember { mutableStateOf(false) }

    // Reactive State Flows from Repository
    val summary by repository.getDashboardSummary(selectedPeriod).collectAsState(initial = com.example.fintrack.data.model.DashboardSummary())
    val transactions by repository.getTransactions().collectAsState(initial = emptyList())
    val accounts by repository.getAccounts().collectAsState(initial = emptyList())
    val categories by repository.getCategories().collectAsState(initial = emptyList())
    val alerts by repository.getImportedSmsAlerts().collectAsState(initial = emptyList())

    val pendingAlertCount = remember(alerts) {
        alerts.count { it.status == SmsAlertStatus.NEEDS_REVIEW }
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
                                        text = "V2",
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
                            // SMS Intelligence action with notification badge
                            IconButton(onClick = { showSmsIntelligence = !showSmsIntelligence }) {
                                BadgedBox(
                                    badge = {
                                        if (pendingAlertCount > 0) {
                                            Badge { Text("$pendingAlertCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (pendingAlertCount > 0) AppIcons.Warning else AppIcons.Activity,
                                        contentDescription = "SMS Intelligence",
                                        tint = if (pendingAlertCount > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

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
                            selected = currentTab == AppTab.DASHBOARD && !showSmsIntelligence && selectedCreditCard == null,
                            onClick = {
                                currentTab = AppTab.DASHBOARD
                                showSmsIntelligence = false
                                selectedCreditCard = null
                            },
                            icon = { Icon(AppIcons.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.TRANSACTIONS && !showSmsIntelligence && selectedCreditCard == null,
                            onClick = {
                                currentTab = AppTab.TRANSACTIONS
                                showSmsIntelligence = false
                                selectedCreditCard = null
                            },
                            icon = { Icon(AppIcons.Activity, contentDescription = "Transactions") },
                            label = { Text("Activity") }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.ACCOUNTS && !showSmsIntelligence && selectedCreditCard == null,
                            onClick = {
                                currentTab = AppTab.ACCOUNTS
                                showSmsIntelligence = false
                                selectedCreditCard = null
                            },
                            icon = { Icon(AppIcons.Bank, contentDescription = "Accounts") },
                            label = { Text("Accounts") }
                        )
                    }
                },
                floatingActionButton = {
                    if (!showSmsIntelligence && selectedCreditCard == null) {
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
                        showSmsIntelligence -> {
                            SmsIntelligenceScreen(
                                alerts = alerts,
                                accounts = accounts,
                                onResolveAlert = { alertId, selectedAccountId ->
                                    coroutineScope.launch {
                                        repository.resolveAlert(alertId, selectedAccountId)
                                        snackbarHostState.showSnackbar("Alert resolved and transaction linked!")
                                    }
                                },
                                onBackClick = { showSmsIntelligence = false }
                            )
                        }
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
                                onEditClick = { /* Can edit via accounts tab */ },
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
                        currentTab == AppTab.DASHBOARD -> {
                            DashboardScreen(
                                summary = summary,
                                selectedPeriod = selectedPeriod,
                                onPeriodSelected = { selectedPeriod = it },
                                onAdjustBalanceClick = { adjustingAccount = it },
                                onPayCreditCardClick = { payingCreditCard = it },
                                onTransactionClick = { editingCategoryTxn = it },
                                onSimulateSmsClick = { showSimulateSmsDialog = true },
                                onAddManualClick = { showAddTxnDialog = true }
                            )
                        }
                        currentTab == AppTab.TRANSACTIONS -> {
                            TransactionsScreen(
                                transactions = transactions,
                                categories = categories,
                                onTransactionClick = { editingCategoryTxn = it }
                            )
                        }
                        currentTab == AppTab.ACCOUNTS -> {
                            AccountsScreen(
                                accounts = accounts,
                                onAdjustBalanceClick = { adjustingAccount = it },
                                onCardClick = { selectedCreditCard = it },
                                onPayCardClick = { payingCreditCard = it },
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
                                }
                            )
                        }
                    }
                }
            }

            // Dialogs
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

            payingCreditCard?.let { card ->
                val bankAccounts = accounts.filter { it.accountType == AccountType.BANK_ACCOUNT }
                PayCreditCardDialog(
                    creditCard = card,
                    bankAccounts = bankAccounts,
                    onDismiss = { payingCreditCard = null },
                    onConfirmPayment = { bankId, amount, note ->
                        coroutineScope.launch {
                            repository.payCreditCardBill(
                                sourceBankAccountId = bankId,
                                creditCardAccountId = card.id,
                                amount = amount,
                                notes = note ?: "Credit Card Bill Payment"
                            )
                            payingCreditCard = null
                            snackbarHostState.showSnackbar("Recorded payment of ₹${String.format(java.util.Locale.getDefault(), "%,.2f", amount)} to ${card.name}")
                        }
                    }
                )
            }

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

            if (showSimulateSmsDialog) {
                SimulateSmsDialog(
                    onDismiss = { showSimulateSmsDialog = false },
                    onSimulate = { sender, msg ->
                        coroutineScope.launch {
                            val success = repository.simulateIncomingSms(sender, msg)
                            showSimulateSmsDialog = false
                            if (success) {
                                snackbarHostState.showSnackbar("Financial SMS processed!")
                            } else {
                                snackbarHostState.showSnackbar("Ignored (Spam / OTP or non-financial)")
                            }
                        }
                    }
                )
            }
        }
    }
}
