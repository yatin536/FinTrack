package com.example.fintrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.Account
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayCreditCardDialog(
    creditCard: Account,
    bankAccounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirmPayment: (sourceBankId: String, amount: Double, note: String?) -> Unit
) {
    var selectedBank by remember {
        mutableStateOf(
            bankAccounts.firstOrNull { it.id in creditCard.linkedPaymentAccountIds }
                ?: bankAccounts.firstOrNull { it.isPrimary }
                ?: bankAccounts.firstOrNull()
        )
    }

    val totalDueAmount = creditCard.totalDue ?: creditCard.currentBalance
    val minDueAmount = creditCard.minimumDue ?: (totalDueAmount * 0.05).coerceAtLeast(0.0)

    var amountText by remember {
        mutableStateOf(if (totalDueAmount > 0) String.format(Locale.US, "%.2f", totalDueAmount) else "")
    }
    var noteText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var bankDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = AppIcons.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Pay Credit Card",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Credit Card Summary
                Text(
                    text = "Paying bill for ${creditCard.name} (••${creditCard.accountNumberLast4})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Current Outstanding: ₹${String.format(Locale.getDefault(), "%,.2f", creditCard.currentBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Source Bank Dropdown
                ExposedDropdownMenuBox(
                    expanded = bankDropdownExpanded,
                    onExpandedChange = { bankDropdownExpanded = !bankDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBank?.let { "${it.name} (₹${String.format(Locale.getDefault(), "%,.0f", it.currentBalance)})" } ?: "Select Bank Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pay From (Bank Account)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = bankDropdownExpanded,
                        onDismissRequest = { bankDropdownExpanded = false }
                    ) {
                        bankAccounts.forEach { bank ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(bank.displayName, fontWeight = FontWeight.SemiBold)
                                        Text("Balance: ₹${String.format(Locale.getDefault(), "%,.2f", bank.currentBalance)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedBank = bank
                                    bankDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quick Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (minDueAmount > 0) {
                        FilterChip(
                            selected = amountText == String.format(Locale.US, "%.2f", minDueAmount),
                            onClick = { amountText = String.format(Locale.US, "%.2f", minDueAmount); error = null },
                            label = { Text("Min Due (₹${minDueAmount.toInt()})", fontSize = 11.sp) }
                        )
                    }
                    if (totalDueAmount > 0) {
                        FilterChip(
                            selected = amountText == String.format(Locale.US, "%.2f", totalDueAmount),
                            onClick = { amountText = String.format(Locale.US, "%.2f", totalDueAmount); error = null },
                            label = { Text("Total Due (₹${totalDueAmount.toInt()})", fontSize = 11.sp) }
                        )
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    label = { Text("Payment Amount (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Ref (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedBank == null) {
                        error = "Please select a bank account to pay from"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        error = "Please enter a valid payment amount"
                        return@Button
                    }
                    onConfirmPayment(selectedBank!!.id, amount, noteText.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
