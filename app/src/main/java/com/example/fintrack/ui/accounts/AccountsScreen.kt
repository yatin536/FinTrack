package com.example.fintrack.ui.accounts

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import java.util.Locale

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    onAdjustBalanceClick: (Account) -> Unit,
    onAddAccount: (name: String, bankName: String, type: AccountType, last4: String, currentBalance: Double, creditLimit: Double) -> Unit,
    onEditAccount: (Account, name: String, bankName: String, type: AccountType, last4: String, currentBalance: Double, creditLimit: Double) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

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
                    text = "Bank Accounts & Cards",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage your accounts, cards, and current balances",
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
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Text(
                text = "💡 Tap \"Adjust\" to set your actual current bank balance anytime. Incoming credit & debit SMS will automatically keep your balance in sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts, key = { it.id }) { acc ->
                AccountCard(
                    account = acc,
                    onAdjustBalance = { onAdjustBalanceClick(acc) },
                    onEditClick = { accountToEdit = acc },
                    onDeleteClick = { accountToDelete = acc }
                )
            }
        }
    }

    if (showAddAccountDialog) {
        AddNewAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name, bank, type, last4, balance, limit ->
                onAddAccount(name, bank, type, last4, balance, limit)
                showAddAccountDialog = false
            }
        )
    }

    if (accountToEdit != null) {
        AddNewAccountDialog(
            initialAccount = accountToEdit,
            onDismiss = { accountToEdit = null },
            onConfirm = { name, bank, type, last4, balance, limit ->
                onEditAccount(accountToEdit!!, name, bank, type, last4, balance, limit)
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
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to remove \"${acc.name}\"? All recorded transactions under this account will also be deleted.")
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
fun AccountCard(
    account: Account,
    onAdjustBalance: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val isCreditCard = account.accountType == AccountType.CREDIT_CARD
    
    val cardGradient = if (isCreditCard) {
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        )
    } else {
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        )
    }
    val textColor = if (isCreditCard) Color.White else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isCreditCard) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardGradient)
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha=0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCreditCard) AppIcons.CreditCard else AppIcons.Bank,
                            contentDescription = account.name,
                            tint = if (isCreditCard) Color.White else Color(account.colorHex),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        if (account.accountNumberLast4.isNotEmpty()) {
                            Text(
                                text = (if(isCreditCard) "Card ending ••" else "A/c ending ••") + account.accountNumberLast4,
                                style = MaterialTheme.typography.bodySmall,
                                color = subTextColor
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = "Edit",
                            tint = subTextColor,
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
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    ) {
                        Text("Adjust", fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (isCreditCard) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Limit Used",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", account.currentBalance)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Available Limit",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", account.limitLeft)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = subTextColor
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Balance",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", account.currentBalance)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    if (account.isPrimary) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "PRIMARY A/C",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Bank Account",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddNewAccountDialog(
    initialAccount: Account? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, bankName: String, type: AccountType, last4: String, currentBalance: Double, creditLimit: Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
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
    var type by remember { mutableStateOf(initialAccount?.accountType ?: AccountType.BANK) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAccount != null) "Edit Account / Card" else "Add Account / Card", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { type = AccountType.BANK },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (type == AccountType.BANK) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) { Text("Bank", fontSize=12.sp) }
                    
                    OutlinedButton(
                        onClick = { type = AccountType.CREDIT_CARD },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (type == AccountType.CREDIT_CARD) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) { Text("Credit Card", fontSize=12.sp) }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (type == AccountType.CREDIT_CARD) "Card Label (e.g. HDFC Millennia)" else "Account Label (e.g. HDFC Salary)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name (HDFC, SBI, etc.)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4) last4 = it },
                    label = { Text("Last 4 digits (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
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
                        label = { Text("Limit Used / Owed (₹)") },
                        supportingText = { Text("Current amount spent or billed") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        Text("Delete Account", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Please enter an account name"
                        return@Button
                    }
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    val lim = limitText.toDoubleOrNull() ?: 0.0
                    onConfirm(name, bankName.ifBlank { "Bank" }, type, last4, bal, lim)
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
