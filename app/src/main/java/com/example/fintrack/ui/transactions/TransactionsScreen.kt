package com.example.fintrack.ui.transactions

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.fintrack.ui.components.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.Category
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.data.model.TransactionStatus
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.ui.dashboard.TransactionRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransactionFilter {
    ALL,
    CREDIT_CARDS,
    BANK_ACCOUNTS,
    PAYMENTS,
    PENDING_REVIEW,
    REJECTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionWithDetails>,
    categories: List<Category>,
    onTransactionClick: (TransactionWithDetails) -> Unit = {},
    onReviewClick: (TransactionWithDetails) -> Unit = {},
    onReverseClick: (TransactionWithDetails) -> Unit = {},
    onDeleteClick: (TransactionWithDetails) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedDetailTxn by remember { mutableStateOf<TransactionWithDetails?>(null) }

    val needsReviewCount = remember(transactions) {
        transactions.count { it.transaction.needsReview || it.transaction.status == TransactionStatus.PENDING_REVIEW }
    }

    val filteredTransactions = transactions.filter { item ->
        val txn = item.transaction
        val matchesSearch = searchQuery.isBlank() ||
                txn.merchant.contains(searchQuery, ignoreCase = true) ||
                (txn.note?.contains(searchQuery, ignoreCase = true) ?: false) ||
                item.account.name.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            TransactionFilter.ALL -> txn.status != TransactionStatus.REJECTED
            TransactionFilter.CREDIT_CARDS -> (item.account.isCreditCard || txn.kind == TransactionKind.CARD_PURCHASE) && txn.status != TransactionStatus.REJECTED
            TransactionFilter.BANK_ACCOUNTS -> !item.account.isCreditCard && txn.kind != TransactionKind.CARD_PURCHASE && txn.status != TransactionStatus.REJECTED
            TransactionFilter.PAYMENTS -> (txn.kind == TransactionKind.CARD_PAYMENT || txn.kind == TransactionKind.BANK_TRANSFER) && txn.status != TransactionStatus.REJECTED
            TransactionFilter.PENDING_REVIEW -> txn.needsReview || txn.status == TransactionStatus.PENDING_REVIEW
            TransactionFilter.REJECTED -> txn.status == TransactionStatus.REJECTED
        }

        val matchesCategory = selectedCategoryId == null || item.category.id == selectedCategoryId

        matchesSearch && matchesFilter && matchesCategory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Activity Ledger",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text = "Full financial activity across bank accounts, credit cards, and pending reviews",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search merchant, note, or account...") },
            leadingIcon = { Icon(AppIcons.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Kind / Type Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == TransactionFilter.ALL,
                    onClick = { selectedFilter = TransactionFilter.ALL },
                    label = { Text("All") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == TransactionFilter.CREDIT_CARDS,
                    onClick = { selectedFilter = TransactionFilter.CREDIT_CARDS },
                    label = { Text("Credit Cards") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == TransactionFilter.BANK_ACCOUNTS,
                    onClick = { selectedFilter = TransactionFilter.BANK_ACCOUNTS },
                    label = { Text("Bank Accounts") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == TransactionFilter.PAYMENTS,
                    onClick = { selectedFilter = TransactionFilter.PAYMENTS },
                    label = { Text("Payments") }
                )
            }
            if (needsReviewCount > 0) {
                item {
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.PENDING_REVIEW,
                        onClick = { selectedFilter = TransactionFilter.PENDING_REVIEW },
                        label = { Text("⚠️ Pending Review ($needsReviewCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFFD97706)
                        )
                    )
                }
            }
            item {
                FilterChip(
                    selected = selectedFilter == TransactionFilter.REJECTED,
                    onClick = { selectedFilter = TransactionFilter.REJECTED },
                    label = { Text("Rejected") }
                )
            }
        }

        // Category Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text("All Categories") }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = {
                        selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                    },
                    label = { Text(cat.name) }
                )
            }
        }

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching transactions found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTransactions, key = { it.transaction.id }) { item ->
                    TransactionRow(
                        transactionWithDetails = item,
                        onClick = { selectedDetailTxn = item }
                    )
                }
            }
        }
    }

    // Transaction Details & Actions Dialog
    selectedDetailTxn?.let { item ->
        val txn = item.transaction
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date(txn.timestamp))

        AlertDialog(
            onDismissRequest = { selectedDetailTxn = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = txn.merchant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (txn.status) {
                            TransactionStatus.CONFIRMED -> Color(0xFF10B981).copy(alpha = 0.15f)
                            TransactionStatus.PENDING_REVIEW -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            TransactionStatus.REJECTED -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = txn.status.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (txn.status) {
                                TransactionStatus.CONFIRMED -> Color(0xFF059669)
                                TransactionStatus.PENDING_REVIEW -> Color(0xFFD97706)
                                TransactionStatus.REJECTED -> Color(0xFFDC2626)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", txn.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = if (txn.direction == TransactionDirection.DEBIT) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Account: ${item.account.name} (${if (item.account.isCreditCard) "Credit Card" else "Bank Account"})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Category: ${item.category.name}",
                        fontSize = 13.sp
                    )
                    if (!txn.referenceNumber.isNullOrBlank()) {
                        Text(
                            text = "Reference / UTR: ${txn.referenceNumber}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!txn.reviewReason.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Note: ${txn.reviewReason}",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (txn.needsReview || txn.status == TransactionStatus.PENDING_REVIEW) {
                        Button(
                            onClick = {
                                val target = selectedDetailTxn
                                selectedDetailTxn = null
                                target?.let { onReviewClick(it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Review Now")
                        }
                    } else if (txn.status == TransactionStatus.CONFIRMED) {
                        OutlinedButton(
                            onClick = {
                                val target = selectedDetailTxn
                                selectedDetailTxn = null
                                target?.let { onReverseClick(it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706))
                        ) {
                            Text("Mark Incorrect / Reverse")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                val target = selectedDetailTxn
                                selectedDetailTxn = null
                                target?.let { onTransactionClick(it) }
                            }
                        ) {
                            Text("Edit Category")
                        }

                        TextButton(
                            onClick = {
                                val target = selectedDetailTxn
                                selectedDetailTxn = null
                                target?.let { onDeleteClick(it) }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDetailTxn = null }) {
                    Text("Close")
                }
            }
        )
    }
}
