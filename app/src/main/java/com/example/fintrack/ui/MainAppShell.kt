package com.example.fintrack.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.example.fintrack.ui.components.AppIcons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.fintrack.data.local.PinManager
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.TimePeriod
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.data.repository.TransactionRepository
import com.example.fintrack.ui.accounts.AccountsScreen
import com.example.fintrack.ui.auth.AuthScreen
import com.example.fintrack.ui.components.AddTransactionDialog
import com.example.fintrack.ui.components.AdjustBalanceDialog
import com.example.fintrack.ui.components.ChangeCategoryDialog
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
                            Text(
                                text = "FinTrack",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                            selected = currentTab == AppTab.DASHBOARD,
                            onClick = { currentTab = AppTab.DASHBOARD },
                            icon = { Icon(AppIcons.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.TRANSACTIONS,
                            onClick = { currentTab = AppTab.TRANSACTIONS },
                            icon = { Icon(AppIcons.Activity, contentDescription = "Transactions") },
                            label = { Text("Activity") }
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.ACCOUNTS,
                            onClick = { currentTab = AppTab.ACCOUNTS },
                            icon = { Icon(AppIcons.Bank, contentDescription = "Accounts") },
                            label = { Text("Accounts") }
                        )
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddTxnDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(AppIcons.Add, contentDescription = "Add Transaction")
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    when (currentTab) {
                        AppTab.DASHBOARD -> {
                            DashboardScreen(
                                summary = summary,
                                selectedPeriod = selectedPeriod,
                                onPeriodSelected = { selectedPeriod = it },
                                onAdjustBalanceClick = { adjustingAccount = it },
                                onTransactionClick = { editingCategoryTxn = it },
                                onSimulateSmsClick = { showSimulateSmsDialog = true },
                                onAddManualClick = { showAddTxnDialog = true }
                            )
                        }
                        AppTab.TRANSACTIONS -> {
                            TransactionsScreen(
                                transactions = transactions,
                                categories = categories,
                                onTransactionClick = { editingCategoryTxn = it }
                            )
                        }
                        AppTab.ACCOUNTS -> {
                            AccountsScreen(
                                accounts = accounts,
                                onAdjustBalanceClick = { adjustingAccount = it },
                                onAddAccount = { name, bank, type, last4, initBal, limit ->
                                    coroutineScope.launch {
                                        val newAcc = Account(
                                            id = UUID.randomUUID().toString(),
                                            name = name,
                                            bankName = bank,
                                            accountType = type,
                                            accountNumberLast4 = last4,
                                            initialBalance = initBal,
                                            creditLimit = limit,
                                            colorHex = 0xFF2563EB
                                        )
                                        repository.addAccount(newAcc)
                                        snackbarHostState.showSnackbar("Account added: $name")
                                    }
                                },
                                onEditAccount = { account, name, bank, type, last4, initBal, limit ->
                                    coroutineScope.launch {
                                        val updated = account.copy(
                                            name = name,
                                            bankName = bank,
                                            accountType = type,
                                            accountNumberLast4 = last4,
                                            initialBalance = initBal,
                                            creditLimit = limit
                                        )
                                        repository.updateAccount(updated)
                                        snackbarHostState.showSnackbar("Account updated")
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
                            repository.updateInitialBalance(account.id, newBal)
                            adjustingAccount = null
                            snackbarHostState.showSnackbar("Balance updated for ${account.name}")
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
                                type = type,
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
                                snackbarHostState.showSnackbar("Bank SMS successfully processed!")
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
