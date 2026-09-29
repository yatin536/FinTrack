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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import java.util.UUID

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    onAdjustBalanceClick: (Account) -> Unit,
    onAddAccount: (name: String, bankName: String, type: AccountType, last4: String, initialBalance: Double, creditLimit: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }

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
                    text = "Bank Accounts & Wallets",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track opening balances and automated additions/subtractions",
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
                text = "💡 Enter your actual bank balance under \"Adjust Balance\". As credit and debit SMS arrive, your balance will automatically sync in real-time.",
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
                    onAdjustBalance = { onAdjustBalanceClick(acc) }
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
}

@Composable
fun AccountCard(
    account: Account,
    onAdjustBalance: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Column {
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

                OutlinedButton(
                    onClick = onAdjustBalance,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                ) {
                    Icon(
                        imageVector = AppIcons.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Adjust", fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (isCreditCard) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Limit Used",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                        Text(
                            text = "₹${String.format(java.util.Locale.getDefault(), "%,.2f", account.currentBalance)}",
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
                            text = "₹${String.format(java.util.Locale.getDefault(), "%,.2f", account.limitLeft)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = subTextColor
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Current Balance",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                        Text(
                            text = "₹${String.format(java.util.Locale.getDefault(), "%,.2f", account.currentBalance)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Base Opening Balance",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                        Text(
                            text = "₹${String.format(java.util.Locale.getDefault(), "%,.2f", account.initialBalance)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
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
    onDismiss: () -> Unit,
    onConfirm: (name: String, bankName: String, type: AccountType, last4: String, initialBalance: Double, creditLimit: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Account / Card", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // We mock segmented button by regular row of buttons for simplicity without relying on beta APIs if not available
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { type = AccountType.BANK },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = if (type == AccountType.BANK) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) { Text("Bank", fontSize=12.sp) }
                    
                    OutlinedButton(
                        onClick = { type = AccountType.CREDIT_CARD },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
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
                        label = { Text("Credit Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Amount Owed / Used (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it; error = null },
                        label = { Text("Opening Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = error != null,
                        supportingText = { error?.let { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
