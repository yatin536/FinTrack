package com.example.fintrack.ui.transactions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.fintrack.ui.components.AppIcons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.fintrack.data.model.Category
import com.example.fintrack.data.model.TransactionDirection
import com.example.fintrack.data.model.TransactionKind
import com.example.fintrack.data.model.TransactionWithDetails
import com.example.fintrack.ui.dashboard.TransactionRow

enum class TransactionFilter {
    ALL,
    EXPENSES,
    INCOME,
    TRANSFERS_AND_CARDS,
    NEEDS_REVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionWithDetails>,
    categories: List<Category>,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val needsReviewCount = remember(transactions) {
        transactions.count { it.transaction.needsReview }
    }

    val filteredTransactions = transactions.filter { item ->
        val txn = item.transaction
        val matchesSearch = searchQuery.isBlank() ||
                txn.merchant.contains(searchQuery, ignoreCase = true) ||
                (txn.note?.contains(searchQuery, ignoreCase = true) ?: false) ||
                item.account.name.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            TransactionFilter.ALL -> true
            TransactionFilter.EXPENSES -> txn.direction == TransactionDirection.DEBIT && txn.kind != TransactionKind.BANK_TRANSFER && txn.kind != TransactionKind.CARD_PAYMENT
            TransactionFilter.INCOME -> txn.direction == TransactionDirection.CREDIT && txn.kind != TransactionKind.BANK_TRANSFER && txn.kind != TransactionKind.CARD_PAYMENT
            TransactionFilter.TRANSFERS_AND_CARDS -> txn.kind == TransactionKind.BANK_TRANSFER || txn.kind == TransactionKind.CARD_PAYMENT || txn.kind == TransactionKind.CARD_PURCHASE
            TransactionFilter.NEEDS_REVIEW -> txn.needsReview
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
            text = "Real-time ledger of debits, credits, transfers, and card spends",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by merchant, note, or account...") },
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
                    selected = selectedFilter == TransactionFilter.EXPENSES,
                    onClick = { selectedFilter = TransactionFilter.EXPENSES },
                    label = { Text("Expenses") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == TransactionFilter.INCOME,
                    onClick = { selectedFilter = TransactionFilter.INCOME },
                    label = { Text("Income") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == TransactionFilter.TRANSFERS_AND_CARDS,
                    onClick = { selectedFilter = TransactionFilter.TRANSFERS_AND_CARDS },
                    label = { Text("Transfers & Cards") }
                )
            }
            if (needsReviewCount > 0) {
                item {
                    FilterChip(
                        selected = selectedFilter == TransactionFilter.NEEDS_REVIEW,
                        onClick = { selectedFilter = TransactionFilter.NEEDS_REVIEW },
                        label = { Text("⚠️ Needs Review ($needsReviewCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFFD97706)
                        )
                    )
                }
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
                        onClick = { onTransactionClick(item) }
                    )
                }
            }
        }
    }
}
