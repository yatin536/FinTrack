package com.example.fintrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.fintrack.data.model.Category
import com.example.fintrack.data.model.TransactionType
import com.example.fintrack.data.model.TransactionWithDetails

/**
 * Dialog to adjust an account's opening balance.
 * Future credits and debits will automatically add/subtract from this balance.
 */
@Composable
fun AdjustBalanceDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (newBalance: Double) -> Unit
) {
    var balanceText by remember { mutableStateOf(account.currentBalance.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Current Balance", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Set current balance for ${account.name}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Enter the actual balance currently present in your account. The app will immediately update to this balance, and future transactions will adjust from here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = {
                        balanceText = it
                        error = null
                    },
                    label = { Text("Current Balance (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = balanceText.toDoubleOrNull()
                    if (amount == null) {
                        error = "Please enter a valid amount"
                    } else {
                        onConfirm(amount)
                    }
                }
            ) {
                Text("Save Current Balance")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to manually record a transaction (Cash or untracked bank spend).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (
        accountId: String,
        categoryId: String,
        amount: Double,
        type: TransactionType,
        merchant: String,
        note: String?
    ) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }

    var accountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val filteredCategories = categories.filter {
        if (selectedType == TransactionType.CREDIT) it.isIncome else !it.isIncome
    }.ifEmpty { categories }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Transaction", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedType = TransactionType.DEBIT }
                    ) {
                        RadioButton(
                            selected = selectedType == TransactionType.DEBIT,
                            onClick = { selectedType = TransactionType.DEBIT }
                        )
                        Text("Expense")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedType = TransactionType.CREDIT }
                    ) {
                        RadioButton(
                            selected = selectedType == TransactionType.CREDIT,
                            onClick = { selectedType = TransactionType.CREDIT }
                        )
                        Text("Income")
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Amount") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Merchant / Receiver
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(if (selectedType == TransactionType.DEBIT) "Paid To (Merchant/Person)" else "Received From") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    val currentCat = categories.firstOrNull { it.id == selectedCategoryId }
                    OutlinedTextField(
                        value = currentCat?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        filteredCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Account selector
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    val currentAcc = accounts.firstOrNull { it.id == selectedAccountId }
                    OutlinedTextField(
                        value = currentAcc?.displayName ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account / Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.displayName) },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        error = "Enter a valid positive amount"
                    } else {
                        onConfirm(
                            selectedAccountId,
                            selectedCategoryId,
                            amt,
                            selectedType,
                            merchant,
                            note.ifBlank { null }
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dialog to change a transaction's category with optional rule persistence.
 */
@Composable
fun ChangeCategoryDialog(
    transactionWithDetails: TransactionWithDetails,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onCategorySelected: (newCategoryId: String, rememberRule: Boolean) -> Unit
) {
    var rememberRule by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Category", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Assign category for \"${transactionWithDetails.transaction.merchant}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(categories) { cat ->
                        val isSelected = cat.id == transactionWithDetails.category.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    onCategorySelected(cat.id, rememberRule)
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(cat.colorHex))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = cat.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberRule,
                        onCheckedChange = { rememberRule = it }
                    )
                    Text(
                        text = "Remember for future transactions from this merchant",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Built-in Interactive SMS Simulation Dialog.
 * Allows trying Indian bank messages (HDFC, SBI, ICICI, etc.) with 1 tap.
 */
@Composable
fun SimulateSmsDialog(
    onDismiss: () -> Unit,
    onSimulate: (sender: String, message: String) -> Unit
) {
    val presets = listOf(
        Pair(
            "HDFC Bank - Swiggy (₹450)",
            Pair("VK-HDFCBK", "Rs 450.00 debited from HDFC Bank a/c **1234 on 28-SEP-26 to VPA swiggy@icici (UPI Ref: 426189). Avl Bal: INR 12,450.00")
        ),
        Pair(
            "SBI - Zomato (₹1,200)",
            Pair("AD-SBIINB", "Dear SBI User, your A/c ending 5678 debited by Rs.1,200.00 on 28Sep26 transfer to ZOMATO Ref No 426189218920. Avail Bal Rs: 25,600.50")
        ),
        Pair(
            "ICICI Bank - Amazon (₹850)",
            Pair("VM-ICICIB", "Acct XX9012 debited for Rs 850.00 on 28-Sep-26. UPI:426189. Info:AMAZON PAY. Available Balance INR 40,120.00")
        ),
        Pair(
            "Salary Credit (₹75,000)",
            Pair("VK-HDFCBK", "Your a/c no. XX1234 is credited for Rs 75,000.00 on 28-09-26 by salary. Avail Bal Rs: 82,450.00 - HDFC Bank")
        ),
        Pair(
            "ATM Cash Withdrawal (₹2,000)",
            Pair("AD-SBIINB", "Rs 2,000.00 withdrawn at ATM from a/c **4321 on 28-SEP-26. Avl Bal Rs: 10,450.00")
        )
    )

    var customSender by remember { mutableStateOf("VK-HDFCBK") }
    var customMessage by remember { mutableStateOf(presets[0].second.second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate Bank SMS", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pick a sample Indian bank alert or enter your own SMS:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                presets.forEach { (label, data) ->
                    OutlinedButton(
                        onClick = {
                            customSender = data.first
                            customMessage = data.second
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("SMS Content") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSimulate(customSender, customMessage)
                }
            ) {
                Text("Process SMS")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
