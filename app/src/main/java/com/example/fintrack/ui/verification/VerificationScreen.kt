package com.example.fintrack.ui.verification

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.AccountType
import com.example.fintrack.data.model.VerificationEvent
import com.example.fintrack.data.model.VerificationEventType
import com.example.fintrack.data.model.VerificationStatus
import com.example.fintrack.ui.components.AppIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    verificationEvents: List<VerificationEvent>,
    accounts: List<Account>,
    onResolveEvent: (eventId: String, status: VerificationStatus, selectedAccountId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val pendingEvents = remember(verificationEvents) {
        verificationEvents.filter { it.status == VerificationStatus.PENDING }
    }
    val resolvedEvents = remember(verificationEvents) {
        verificationEvents.filter { it.status != VerificationStatus.PENDING }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Verification Center",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Review ambiguous financial alerts and confirm account associations to keep your ledger pristine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tab Selector
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Action Required")
                        if (pendingEvents.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${pendingEvents.size}",
                                        color = MaterialTheme.colorScheme.onError,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("History (${resolvedEvents.size})") }
            )
        }

        if (selectedTab == 0) {
            if (pendingEvents.isEmpty()) {
                EmptyVerificationState(
                    title = "All Caught Up!",
                    subtitle = "There are no pending alerts requiring manual verification. All ingested transactions are reconciled."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pendingEvents, key = { it.id }) { event ->
                        VerificationEventCard(
                            event = event,
                            accounts = accounts,
                            onConfirm = { accId -> onResolveEvent(event.id, VerificationStatus.CONFIRMED, accId) },
                            onReject = { onResolveEvent(event.id, VerificationStatus.REJECTED, null) },
                            onIgnore = { onResolveEvent(event.id, VerificationStatus.IGNORED, null) }
                        )
                    }
                }
            }
        } else {
            if (resolvedEvents.isEmpty()) {
                EmptyVerificationState(
                    title = "No History Yet",
                    subtitle = "Resolved or verified transactions will appear here for your audit records."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(resolvedEvents, key = { it.id }) { event ->
                        ResolvedVerificationItem(event = event)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerificationEventCard(
    event: VerificationEvent,
    accounts: List<Account>,
    onConfirm: (selectedAccountId: String?) -> Unit,
    onReject: () -> Unit,
    onIgnore: () -> Unit
) {
    val bankAccounts = accounts.filter { it.accountType != AccountType.CREDIT_CARD }
    var selectedBank by remember { mutableStateOf<Account?>(bankAccounts.firstOrNull()) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Type badge & timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (event.eventType) {
                        VerificationEventType.CREDIT_CARD_PAYMENT -> Color(0xFF6366F1).copy(alpha = 0.2f)
                        VerificationEventType.ACCOUNT_LINKING -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        VerificationEventType.DISCREPANCY_ALERT -> Color(0xFFEF4444).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = event.eventType.name.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (event.eventType) {
                            VerificationEventType.CREDIT_CARD_PAYMENT -> Color(0xFF6366F1)
                            VerificationEventType.ACCOUNT_LINKING -> Color(0xFFD97706)
                            VerificationEventType.DISCREPANCY_ALERT -> Color(0xFFDC2626)
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(event.createdAt)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Title & Description
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Account Selector if card payment or account linking
            if (event.eventType == VerificationEventType.CREDIT_CARD_PAYMENT || event.eventType == VerificationEventType.ACCOUNT_LINKING) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Assign to Bank Account:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBank?.displayName ?: "Select Bank Account",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
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
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onConfirm(selectedBank?.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Confirm")
                }

                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ignore")
                }

                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun ResolvedVerificationItem(event: VerificationEvent) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(event.createdAt)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (event.status) {
                    VerificationStatus.CONFIRMED -> Color(0xFF10B981).copy(alpha = 0.2f)
                    VerificationStatus.REJECTED -> Color(0xFFEF4444).copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = event.status.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (event.status) {
                        VerificationStatus.CONFIRMED -> Color(0xFF059669)
                        VerificationStatus.REJECTED -> Color(0xFFDC2626)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyVerificationState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
