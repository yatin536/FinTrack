package com.example.fintrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPaidDialog(
    creditCard: Account,
    bankAccounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirmBillPaid: (sourceBankId: String?, amount: Double, note: String?) -> Unit
) {
    var selectedBank by remember {
        mutableStateOf<Account?>(
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

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val simulatedNewBalance = maxOf(0.0, creditCard.currentBalance - parsedAmount)
    val simulatedAvailableCredit = minOf(creditCard.creditLimit, (creditCard.availableCredit ?: (creditCard.creditLimit - creditCard.currentBalance)) + parsedAmount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Mark Bill as Paid",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Notice banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Record an external bill payment to restore your available credit limit and update liability. FinTrack does not execute bank transfers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Credit Card Summary
                Text(
                    text = "${creditCard.name} (••${creditCard.accountNumberLast4})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Quick Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (totalDueAmount > 0) {
                        FilterChip(
                            selected = amountText == String.format(Locale.US, "%.2f", totalDueAmount),
                            onClick = {
                                amountText = String.format(Locale.US, "%.2f", totalDueAmount)
                                error = null
                            },
                            label = { Text("Total Due (₹${totalDueAmount.toInt()})", fontSize = 11.sp) }
                        )
                    }
                    if (creditCard.currentBalance > 0 && creditCard.currentBalance != totalDueAmount) {
                        FilterChip(
                            selected = amountText == String.format(Locale.US, "%.2f", creditCard.currentBalance),
                            onClick = {
                                amountText = String.format(Locale.US, "%.2f", creditCard.currentBalance)
                                error = null
                            },
                            label = { Text("Full Outstanding (₹${creditCard.currentBalance.toInt()})", fontSize = 11.sp) }
                        )
                    }
                    if (minDueAmount > 0) {
                        FilterChip(
                            selected = amountText == String.format(Locale.US, "%.2f", minDueAmount),
                            onClick = {
                                amountText = String.format(Locale.US, "%.2f", minDueAmount)
                                error = null
                            },
                            label = { Text("Min Due (₹${minDueAmount.toInt()})", fontSize = 11.sp) }
                        )
                    }
                }

                // Amount Paid Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    label = { Text("Amount Paid (₹)") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Bank Account Paid From
                ExposedDropdownMenuBox(
                    expanded = bankDropdownExpanded,
                    onExpandedChange = { bankDropdownExpanded = !bankDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBank?.let { "${it.name} (₹${String.format(Locale.getDefault(), "%,.0f", it.currentBalance)})" } ?: "Paid from External Bank / Cash",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid From (Optional Bank)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = bankDropdownExpanded,
                        onDismissRequest = { bankDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Paid from External Bank / Other", fontWeight = FontWeight.Normal) },
                            onClick = {
                                selectedBank = null
                                bankDropdownExpanded = false
                            }
                        )
                        bankAccounts.forEach { bank ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(bank.displayName, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Balance: ₹${String.format(Locale.getDefault(), "%,.2f", bank.currentBalance)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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

                // Recalculation Impact Preview
                if (parsedAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Recalculation Preview:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("New Outstanding:", fontSize = 12.sp)
                                Text("₹${String.format(Locale.getDefault(), "%,.2f", simulatedNewBalance)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("New Available Credit:", fontSize = 12.sp)
                                Text("₹${String.format(Locale.getDefault(), "%,.2f", simulatedAvailableCredit)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
                            }
                        }
                    }
                }

                // Notes input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / UTR (Optional)") },
                    placeholder = { Text("e.g. Paid via NetBanking") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        error = "Please enter a valid payment amount"
                        return@Button
                    }
                    onConfirmBillPaid(selectedBank?.id, amount, noteText.ifBlank { null })
                }
            ) {
                Text("Confirm Bill Paid")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
