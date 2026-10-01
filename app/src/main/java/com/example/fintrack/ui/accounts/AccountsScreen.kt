package com.example.fintrack.ui.accounts

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
import androidx.compose.foundation.text.KeyboardOptions
import com.example.fintrack.ui.components.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.BankAccountType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    onAdjustBalanceClick: (Account) -> Unit,
    onCardClick: (Account) -> Unit = {},
    onMarkBillPaidClick: (Account) -> Unit = {},
    onAddAccount: (name: String, bankName: String, type: AccountType, subType: BankAccountType, last4: String, currentBalance: Double, creditLimit: Double, stmtDate: String?, dueDate: String?) -> Unit,
    onEditAccount: (Account, name: String, bankName: String, type: AccountType, subType: BankAccountType, last4: String, currentBalance: Double, creditLimit: Double, stmtDate: String?, dueDate: String?) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    val bankAccounts = accounts.filter { it.accountType == AccountType.BANK_ACCOUNT }
    val creditCards = accounts.filter { it.accountType == AccountType.CREDIT_CARD }
    val otherAccounts = accounts.filter { it.accountType != AccountType.BANK_ACCOUNT && it.accountType != AccountType.CREDIT_CARD }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Accounts & Cards",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bank accounts, credit cards, and local ledger balances",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = { showAddAccountDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(AppIcons.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        // Info Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            )
        ) {
            Text(
                text = "💡 Tap \"Adjust\" to set your actual current bank balance anytime. Incoming financial SMS automatically updates the correct account or card ledger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Bank Accounts
            if (bankAccounts.isNotEmpty()) {
                item {
                    Text(
                        text = "BANK ACCOUNTS (${bankAccounts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                items(bankAccounts, key = { it.id }) { acc ->
                    BankAccountCard(
                        account = acc,
                        onAdjustBalance = { onAdjustBalanceClick(acc) },
                        onEditClick = { accountToEdit = acc },
                        onDeleteClick = { accountToDelete = acc }
                    )
                }
            }

            // Section 2: Credit Cards
            if (creditCards.isNotEmpty()) {
                item {
                    Text(
                        text = "CREDIT CARDS (${creditCards.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEC4899),
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                items(creditCards, key = { it.id }) { card ->
                    CreditCardItem(
                        card = card,
                        onClick = { onCardClick(card) },
                        onPayClick = { onMarkBillPaidClick(card) },
                        onAdjustBalance = { onAdjustBalanceClick(card) },
                        onEditClick = { accountToEdit = card },
                        onDeleteClick = { accountToDelete = card }
                    )
                }
            }

            // Section 3: Cash & Wallets
            if (otherAccounts.isNotEmpty()) {
                item {
                    Text(
                        text = "WALLETS & CASH (${otherAccounts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                items(otherAccounts, key = { it.id }) { acc ->
                    BankAccountCard(
                        account = acc,
                        onAdjustBalance = { onAdjustBalanceClick(acc) },
                        onEditClick = { accountToEdit = acc },
                        onDeleteClick = { accountToDelete = acc }
                    )
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddNewAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, bank, type, subType, last4, balance, limit, stmt, due ->
                onAddAccount(name, bank, type, subType, last4, balance, limit, stmt, due)
                showAddAccountDialog = false
            }
        )
    }

    if (accountToEdit != null) {
        AddNewAccountDialog(
            initialAccount = accountToEdit,
            onDismiss = { accountToEdit = null },
            onConfirm = { name, bank, type, subType, last4, balance, limit, stmt, due ->
                onEditAccount(accountToEdit!!, name, bank, type, subType, last4, balance, limit, stmt, due)
                accountToEdit = null
            },
            onDelete = {
                val acc = accountToEdit!!
                accountToEdit = null
                accountToDelete = acc
            }
        )
    }

    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete ${if (acc.accountType == AccountType.CREDIT_CARD) "Card" else "Account"}?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to remove \"${acc.name}\"? All transactions associated with this instrument will also be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAccount(acc)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BankAccountCard(
    account: Account,
    onAdjustBalance: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(account.colorHex).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Bank,
                            contentDescription = account.name,
                            tint = Color(account.colorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${account.bankAccountType.name.lowercase().replaceFirstChar { it.uppercase() }} A/c",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (account.accountNumberLast4.isNotEmpty()) {
                                Text(
                                    text = " ••${account.accountNumberLast4}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = AppIcons.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = onAdjustBalance,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Adjust", fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Available Balance",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", account.currentBalance)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (account.lastConfirmedBalance != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Verified via SMS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                } else if (account.isPrimary) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "PRIMARY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCardItem(
    card: Account,
    onClick: () -> Unit,
    onPayClick: () -> Unit,
    onAdjustBalance: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val outstanding = card.currentBalance
    val limit = if (card.creditLimit > 0) card.creditLimit else 100000.0
    val usageRatio = (outstanding / limit).toFloat().coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 30.dp, height = 22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFD4AF37))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${card.bankName.uppercase()} ••${card.accountNumberLast4.ifEmpty { "CARD" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp)) {
                        Icon(AppIcons.Edit, contentDescription = "Edit", tint = Color.LightGray, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(30.dp)) {
                        Icon(AppIcons.Delete, contentDescription = "Delete", tint = Color(0xFFF87171), modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("Bill Paid", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Outstanding and Limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current Outstanding",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", outstanding)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (outstanding > 0) Color(0xFFFCA5A5) else Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Available Limit",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", card.limitLeft)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF34D399)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { usageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (usageRatio > 0.7f) Color(0xFFEF4444) else Color(0xFF38BDF8),
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(8.dp))

            // Footer info: Dues & dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Limit: ₹${String.format(Locale.getDefault(), "%,.0f", limit)} (${(usageRatio * 100).toInt()}% used)",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                if (!card.paymentDueDate.isNullOrBlank()) {
                    Text(
                        text = "Due: ${card.paymentDueDate}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

@Composable
fun AddNewAccountDialog(
    initialAccount: Account? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, bankName: String, type: AccountType, subType: BankAccountType, last4: String, currentBalance: Double, creditLimit: Double, stmtDate: String?, dueDate: String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(initialAccount?.accountType ?: AccountType.BANK_ACCOUNT) }
    var subType by remember { mutableStateOf(initialAccount?.bankAccountType ?: BankAccountType.SAVINGS) }
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var bankName by remember { mutableStateOf(initialAccount?.bankName ?: "") }
    var last4 by remember { mutableStateOf(initialAccount?.accountNumberLast4 ?: "") }
    var balanceText by remember {
        mutableStateOf(
            initialAccount?.currentBalance?.let {
                if (it == 0.0) "" else String.format(Locale.US, "%.2f", it)
            } ?: ""
        )
    }
    var limitText by remember {
        mutableStateOf(
            initialAccount?.creditLimit?.let {
                if (it == 0.0) "" else String.format(Locale.US, "%.2f", it)
            } ?: ""
        )
    }
    var dueDateText by remember { mutableStateOf(initialAccount?.paymentDueDate ?: "") }
    var stmtDateText by remember { mutableStateOf(initialAccount?.statementDate ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAccount != null) "Edit Account / Card" else "Add Financial Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { type = AccountType.BANK_ACCOUNT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (type == AccountType.BANK_ACCOUNT) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) { Text("Bank", fontSize = 11.sp) }

                    OutlinedButton(
                        onClick = { type = AccountType.CREDIT_CARD },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (type == AccountType.CREDIT_CARD) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) { Text("Credit Card", fontSize = 11.sp) }

                    OutlinedButton(
                        onClick = { type = AccountType.CASH_WALLET },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (type == AccountType.CASH_WALLET) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) { Text("Cash", fontSize = 11.sp) }
                }

                // If Bank, select Savings vs Current
                if (type == AccountType.BANK_ACCOUNT) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { subType = BankAccountType.SAVINGS },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (subType == BankAccountType.SAVINGS) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                            )
                        ) { Text("Savings", fontSize = 11.sp) }

                        OutlinedButton(
                            onClick = { subType = BankAccountType.CURRENT },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (subType == BankAccountType.CURRENT) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                            )
                        ) { Text("Current", fontSize = 11.sp) }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = {
                        Text(
                            when (type) {
                                AccountType.CREDIT_CARD -> "Card Name (e.g. HDFC Regalia)"
                                AccountType.CASH_WALLET -> "Wallet Name (e.g. Cash in Hand)"
                                else -> "Account Label (e.g. HDFC Salary)"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (type != AccountType.CASH_WALLET) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank / Issuer (HDFC, ICICI, SBI, Axis)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = last4,
                        onValueChange = { if (it.length <= 4) last4 = it },
                        label = { Text("Last 4 digits (e.g. 5678)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (type == AccountType.CREDIT_CARD) {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Total Credit Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Current Outstanding / Used (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dueDateText,
                            onValueChange = { dueDateText = it },
                            label = { Text("Due Date (e.g. 20th)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = stmtDateText,
                            onValueChange = { stmtDateText = it },
                            label = { Text("Statement (e.g. 5th)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it; error = null },
                        label = { Text("Current Balance (₹)") },
                        supportingText = {
                            error?.let { Text(it) } ?: Text("Enter current bank balance. SMS transactions will adjust from here.")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (initialAccount != null && onDelete != null) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = AppIcons.Delete,
                            contentDescription = "Delete Account",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Instrument", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Please enter a name"
                        return@Button
                    }
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    val lim = limitText.toDoubleOrNull() ?: 0.0
                    onConfirm(
                        name,
                        bankName.ifBlank { if (type == AccountType.CASH_WALLET) "Cash" else "Bank" },
                        type,
                        subType,
                        last4,
                        bal,
                        lim,
                        stmtDateText.ifBlank { null },
                        dueDateText.ifBlank { null }
                    )
                }
            ) {
                Text(if (initialAccount != null) "Save" else "Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
