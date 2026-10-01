package com.example.fintrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.TransactionWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ReviewMode {
    YES_CREDIT_CARD,
    NO_BANK_ACCOUNT,
    NOT_RELEVANT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionReviewSheet(
    item: TransactionWithDetails,
    allAccounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirmCreditCard: (cardId: String) -> Unit,
    onConfirmBankAccount: (bankAccountId: String) -> Unit,
    onReject: (reason: String, learnRule: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val txn = item.transaction
    val creditCards = allAccounts.filter { it.isCreditCard }
    val bankAccounts = allAccounts.filter { !it.isCreditCard }

    var selectedCardId by remember {
        mutableStateOf(
            creditCards.firstOrNull { it.id == txn.accountId }?.id
                ?: creditCards.firstOrNull { it.accountNumberLast4.isNotEmpty() && item.account.accountNumberLast4 == it.accountNumberLast4 }?.id
                ?: creditCards.firstOrNull()?.id ?: ""
        )
    }

    // Pre-select best matching bank account
    var selectedBankAccountId by remember {
        mutableStateOf(
            bankAccounts.firstOrNull { it.id == txn.accountId }?.id
                ?: bankAccounts.firstOrNull { it.isPrimary }?.id
                ?: bankAccounts.firstOrNull()?.id ?: ""
        )
    }

    var reviewMode by remember { mutableStateOf(ReviewMode.YES_CREDIT_CARD) }
    var learnFilterRule by remember { mutableStateOf(true) }

    val formattedDate = remember(txn.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(txn.timestamp))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Review Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(AppIcons.Close, contentDescription = "Close")
                }
            }

            // Transaction Detail Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = txn.merchant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formattedDate,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "₹${String.format(Locale.US, "%,.2f", txn.amount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Detection Reasoning
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = txn.reviewReason ?: "AI detected card spending from SMS alert. Verification required.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Raw SMS snippet if available
                    if (!txn.rawSmsBody.isNullOrBlank()) {
                        Text(
                            text = "\"${txn.rawSmsBody}\"",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 3
                        )
                    }
                }
            }

            // Core Question
            Text(
                text = "We believe this transaction was made using your credit card. Is this correct?",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Primary Choice Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = reviewMode == ReviewMode.YES_CREDIT_CARD,
                    onClick = { reviewMode = ReviewMode.YES_CREDIT_CARD },
                    label = { Text("Yes, Credit Card", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = reviewMode == ReviewMode.NO_BANK_ACCOUNT,
                    onClick = { reviewMode = ReviewMode.NO_BANK_ACCOUNT },
                    label = { Text("No, Bank A/c", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = reviewMode == ReviewMode.NOT_RELEVANT,
                    onClick = { reviewMode = ReviewMode.NOT_RELEVANT },
                    label = { Text("Not Relevant", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Sub-actions based on selection
            when (reviewMode) {
                ReviewMode.YES_CREDIT_CARD -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Select Credit Card:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (creditCards.isEmpty()) {
                            Text(
                                text = "No credit cards found in vault. Please add one first.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        } else {
                            creditCards.forEach { card ->
                                val isSelected = card.id == selectedCardId
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCardId = card.id }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = card.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${card.bankName} ••${card.accountNumberLast4}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = AppIcons.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (selectedCardId.isNotBlank()) {
                                    onConfirmCreditCard(selectedCardId)
                                }
                            },
                            enabled = selectedCardId.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm Credit Card Transaction")
                        }
                    }
                }

                ReviewMode.NO_BANK_ACCOUNT -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Assign to Bank Account:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (bankAccounts.isEmpty()) {
                            Text(
                                text = "No bank accounts found in vault.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        } else {
                            bankAccounts.forEach { bank ->
                                val isSelected = bank.id == selectedBankAccountId
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBankAccountId = bank.id }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = bank.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${bank.bankName} ••${bank.accountNumberLast4}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = AppIcons.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (selectedBankAccountId.isNotBlank()) {
                                    onConfirmBankAccount(selectedBankAccountId)
                                }
                            },
                            enabled = selectedBankAccountId.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Confirm as Bank Account Debit")
                        }
                    }
                }

                ReviewMode.NOT_RELEVANT -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Mark as False / Irrelevant",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "This alert will be discarded from your accounts and will not affect any balance, spending, or credit limit.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { learnFilterRule = !learnFilterRule }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = learnFilterRule,
                                onCheckedChange = { learnFilterRule = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Learn safe filter rule: suppress future promotional / non-transaction messages like this from ${txn.smsSender ?: "this sender"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onReject("User rejected transaction as not relevant", learnFilterRule)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Discard & Suppress Future Alerts")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
