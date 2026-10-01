package com.example.fintrack.ui.alerts

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.data.model.Account
import com.example.fintrack.data.model.ImportedSmsAlert
import com.example.fintrack.data.model.SmsAlertStatus
import com.example.fintrack.ui.components.AppIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsIntelligenceScreen(
    alerts: List<ImportedSmsAlert>,
    accounts: List<Account>,
    onResolveAlert: (alertId: String, selectedAccountId: String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<SmsAlertStatus?>(null) }
    var resolvingAlert by remember { mutableStateOf<ImportedSmsAlert?>(null) }

    val filteredAlerts = alerts.filter { alert ->
        selectedFilter == null || alert.status == selectedFilter
    }

    val accountsMap = accounts.associateBy { it.id }
    val df = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(AppIcons.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = "SMS Intelligence & Alerts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "On-device audit of financial SMS alerts and account classification",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All (${alerts.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == SmsAlertStatus.NEEDS_REVIEW,
                onClick = { selectedFilter = SmsAlertStatus.NEEDS_REVIEW },
                label = {
                    Text(
                        "Needs Review (${alerts.count { it.status == SmsAlertStatus.NEEDS_REVIEW }})",
                        fontSize = 11.sp,
                        color = if (alerts.any { it.status == SmsAlertStatus.NEEDS_REVIEW }) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
            FilterChip(
                selected = selectedFilter == SmsAlertStatus.PROCESSED,
                onClick = { selectedFilter = SmsAlertStatus.PROCESSED },
                label = { Text("Processed", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == SmsAlertStatus.DUPLICATE,
                onClick = { selectedFilter = SmsAlertStatus.DUPLICATE },
                label = { Text("Duplicates", fontSize = 11.sp) }
            )
        }

        if (filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No alerts found for this filter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAlerts, key = { it.id }) { alert ->
                    val linkedAcc = alert.accountId?.let { accountsMap[it] }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (alert.status) {
                                SmsAlertStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                SmsAlertStatus.DUPLICATE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                SmsAlertStatus.IGNORED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                SmsAlertStatus.PROCESSED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (alert.status) {
                                                    SmsAlertStatus.PROCESSED -> Color(0xFF10B981)
                                                    SmsAlertStatus.NEEDS_REVIEW -> Color(0xFFEF4444)
                                                    SmsAlertStatus.DUPLICATE -> Color(0xFFF59E0B)
                                                    SmsAlertStatus.IGNORED -> Color(0xFF94A3B8)
                                                }
                                            )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = alert.sender,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (alert.status) {
                                        SmsAlertStatus.PROCESSED -> Color(0xFF10B981).copy(alpha = 0.15f)
                                        SmsAlertStatus.NEEDS_REVIEW -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                        SmsAlertStatus.DUPLICATE -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        SmsAlertStatus.IGNORED -> Color(0xFF94A3B8).copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = alert.status.name.replace("_", " "),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (alert.status) {
                                            SmsAlertStatus.PROCESSED -> Color(0xFF10B981)
                                            SmsAlertStatus.NEEDS_REVIEW -> Color(0xFFEF4444)
                                            SmsAlertStatus.DUPLICATE -> Color(0xFFF59E0B)
                                            SmsAlertStatus.IGNORED -> Color(0xFF94A3B8)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = alert.body,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3
                            )

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = df.format(Date(alert.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (linkedAcc != null) {
                                    Text(
                                        text = "Account: ${linkedAcc.displayName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (alert.status == SmsAlertStatus.NEEDS_REVIEW) {
                                    OutlinedButton(
                                        onClick = { resolvingAlert = alert },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Select Account", fontSize = 11.sp)
                                    }
                                }
                            }

                            if (!alert.reason.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = alert.reason,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog to resolve Needs Review alert
    resolvingAlert?.let { alert ->
        var chosenAccount by remember { mutableStateOf(accounts.firstOrNull()) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { resolvingAlert = null },
            title = { Text("Assign Financial Instrument", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose which Bank Account or Credit Card this SMS belongs to:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = chosenAccount?.displayName ?: "Select Account",
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
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.displayName) },
                                    onClick = {
                                        chosenAccount = acc
                                        dropdownExpanded = false
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
                        chosenAccount?.let { acc ->
                            onResolveAlert(alert.id, acc.id)
                        }
                        resolvingAlert = null
                    }
                ) {
                    Text("Confirm & Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { resolvingAlert = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
