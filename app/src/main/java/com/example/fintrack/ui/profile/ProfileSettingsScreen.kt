package com.example.fintrack.ui.profile

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.fintrack.data.local.PinManager
import com.example.fintrack.data.model.AuditLog
import com.example.fintrack.data.model.MessageRule
import com.example.fintrack.data.model.User
import com.example.fintrack.data.repository.TransactionRepository
import com.example.fintrack.ui.components.AppIcons
import com.example.fintrack.ui.components.UserDetailsSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileSettingsScreen(
    repository: TransactionRepository,
    onUserSwitched: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    val pinManager = remember { PinManager(context) }
    val scope = rememberCoroutineScope()

    val activeUser by repository.getActiveUserFlow().collectAsState(initial = null)
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var activeUserId by remember { mutableStateOf(repository.sessionManager.getActiveUserId()) }
    var activeUserName by remember { mutableStateOf(repository.sessionManager.getActiveUserName()) }
    var auditLogs by remember { mutableStateOf<List<AuditLog>>(emptyList()) }

    var biometricEnabled by remember { mutableStateOf(pinManager.isBiometricEnabled) }
    var showUserSwitchDialog by remember { mutableStateOf(false) }
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showUserDetailsSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val messageRules by repository.getMessageRules(activeUserId).collectAsState(initial = emptyList())

    LaunchedEffect(activeUser) {
        activeUser?.let { u ->
            activeUserId = u.id
            activeUserName = u.name
            biometricEnabled = u.isBiometricEnabled
        }
    }

    fun refreshUsers() {
        scope.launch {
            users = repository.getUsers()
            auditLogs = repository.getAuditLogs(activeUserId)
        }
    }

    LaunchedEffect(activeUserId) {
        refreshUsers()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        item {
            Text(
                text = "Profile & Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage multi-user profiles, security parameters, and data audit logs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Profile Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showUserDetailsSheet = true }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(activeUser?.colorHex ?: 0xFF2563EB),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (activeUser?.displayName ?: activeUserName).take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = activeUser?.displayName ?: activeUserName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeUser?.email ?: "Personal Financial Vault",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Active Profile",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { showUserDetailsSheet = true }) {
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = "View Details",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showUserDetailsSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Edit Details", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showUserSwitchDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Switch Profile", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Personal Details & Identity Row
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showUserDetailsSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Personal Details & Identity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "View and edit full name, email, phone & membership",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Manage Accounts & Cards Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAccounts() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Bank,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Accounts & Credit Cards",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add, edit, adjust balances, and configure cards",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Learned Message Rules Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRulesDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Message Processing & Learned Rules",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${messageRules.size} user-trained filter rule${if (messageRules.size != 1) "s" else ""} active",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Security Settings Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Security & Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Biometric Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Unlock", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Unlock with Fingerprint or Face ID",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (fragmentActivity != null) {
                                        val executor = ContextCompat.getMainExecutor(context)
                                        val prompt = BiometricPrompt(
                                            fragmentActivity,
                                            executor,
                                            object : BiometricPrompt.AuthenticationCallback() {
                                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                    biometricEnabled = true
                                                    pinManager.isBiometricEnabled = true
                                                    activeUser?.let { u ->
                                                        scope.launch {
                                                            repository.updateUser(u.copy(isBiometricEnabled = true))
                                                        }
                                                    }
                                                }
                                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                    biometricEnabled = false
                                                }
                                            }
                                        )
                                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                            .setTitle("Enable Biometric Unlock")
                                            .setSubtitle("Confirm your biometric identity to enable quick unlock")
                                            .setNegativeButtonText("Cancel")
                                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                            .build()
                                        prompt.authenticate(promptInfo)
                                    } else {
                                        biometricEnabled = true
                                        pinManager.isBiometricEnabled = true
                                        activeUser?.let { u ->
                                            scope.launch {
                                                repository.updateUser(u.copy(isBiometricEnabled = true))
                                            }
                                        }
                                    }
                                } else {
                                    biometricEnabled = false
                                    pinManager.isBiometricEnabled = false
                                    activeUser?.let { u ->
                                        scope.launch {
                                            repository.updateUser(u.copy(isBiometricEnabled = false))
                                        }
                                    }
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Change PIN button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showChangePinDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Security PIN", fontWeight = FontWeight.SemiBold)
                            Text("Change your 6-digit master access PIN", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            imageVector = AppIcons.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Privacy & SMS Transparency Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Air-Gapped Privacy Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "FinTrack has ZERO internet permissions in AndroidManifest.xml. All financial data, regex parsing, and analytics remain strictly encrypted on this device using hardware-backed AES-256-GCM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Permission & Security Model")
                    }
                }
            }
        }

        // Audit Trail Section
        item {
            Text(
                text = "Recent Ledger Audit Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (auditLogs.isEmpty()) {
            item {
                Text(
                    text = "No audit log entries yet. Financial operations like manual adjustments and bill confirmations are tracked here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(auditLogs.take(10), key = { it.id }) { log ->
                AuditLogItem(log = log)
            }
        }

        // Session / Log Out Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Log Out of FinTrack",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Lock encrypted vault and return to unlock screen",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Version Footer
        item {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FinTrack v2.0 • Production Ready",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Encrypted Local SQLite • Zero-Cloud Architecture",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // Dialog: Switch User / Multi-User
    if (showUserSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showUserSwitchDialog = false },
            title = { Text("Switch Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    users.forEach { user ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (user.id == activeUserId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.switchUser(user)
                                    activeUserId = user.id
                                    activeUserName = user.name
                                    showUserSwitchDialog = false
                                    onUserSwitched()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(user.name, fontWeight = FontWeight.Bold)
                                    Text(user.email ?: "Local Profile", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (user.id == activeUserId) {
                                    Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            showUserSwitchDialog = false
                            showCreateUserDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Create New Profile")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserSwitchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Create User
    if (showCreateUserDialog) {
        var newUserName by remember { mutableStateOf("") }
        var newUserEmail by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateUserDialog = false },
            title = { Text("Create Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("Profile Name") },
                        placeholder = { Text("e.g. Business, Family") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserEmail,
                        onValueChange = { newUserEmail = it },
                        label = { Text("Label / Email (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserName.isNotBlank()) {
                            scope.launch {
                                val created = repository.createUser(newUserName.trim(), newUserEmail.trim().ifBlank { null })
                                repository.switchUser(created)
                                activeUserId = created.id
                                activeUserName = created.name
                                showCreateUserDialog = false
                                refreshUsers()
                                onUserSwitched()
                            }
                        }
                    }
                ) {
                    Text("Create & Switch")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateUserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Change PIN
    if (showChangePinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Security PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 6) currentPin = it },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6) newPin = it },
                        label = { Text("New 6-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6) confirmPin = it },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError != null,
                        supportingText = { pinError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val verifyResult = pinManager.verifyPin(currentPin)
                        if (verifyResult !is PinManager.VerificationResult.Success) {
                            pinError = "Current PIN is incorrect"
                            return@Button
                        }
                        if (newPin.length < 4) {
                            pinError = "New PIN must be at least 4 digits"
                            return@Button
                        }
                        if (newPin != confirmPin) {
                            pinError = "New PINs do not match"
                            return@Button
                        }
                        pinManager.setPin(newPin)
                        showChangePinDialog = false
                    }
                ) {
                    Text("Update PIN")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Privacy Details
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("FinTrack Privacy & Security Guarantee", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Zero Cloud Connections:", fontWeight = FontWeight.SemiBold)
                    Text("The application does not include android.permission.INTERNET in its manifest, guaranteeing that your financial records never leave this hardware.", fontSize = 12.sp)

                    Text("2. Hardware Keystore Encryption:", fontWeight = FontWeight.SemiBold)
                    Text("Your SQLite database files and sensitive tokens are encrypted using AES-256-GCM backed by the AndroidKeyStore.", fontSize = 12.sp)

                    Text("3. Transparent SMS Processing:", fontWeight = FontWeight.SemiBold)
                    Text("SMS messages are processed locally using regular expressions. OTPs and personal messages are discarded and never retained.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    // Dialog: Learned Message Rules
    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = {
                Text(
                    text = "Learned SMS Filter Rules",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "These rules were trained when you rejected false/promotional alerts. They safely suppress matching marketing texts without dropping genuine banking transactions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (messageRules.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No learned rules yet.\nWhen you mark an SMS as not relevant, you can choose to ignore similar messages.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        messageRules.forEach { rule ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rule.senderPattern.ifBlank { "All Senders" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Pattern: \"${rule.bodyPattern}\"",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteMessageRule(rule.id)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Close,
                                            contentDescription = "Delete Rule",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Dialog: Log Out Confirmation
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out of FinTrack?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Logging out will lock your local financial vault and return to the unlock screen. All encrypted data remains completely intact on this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        repository.sessionManager.logout()
                        onLogout()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sheet: User Details & Profile Editor
    if (showUserDetailsSheet && activeUser != null) {
        UserDetailsSheet(
            user = activeUser!!,
            onDismiss = { showUserDetailsSheet = false },
            onSaveProfile = { updated ->
                scope.launch {
                    repository.updateUser(updated)
                    showUserDetailsSheet = false
                    refreshUsers()
                }
            }
        )
    }
}

@Composable
private fun AuditLogItem(log: AuditLog) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.action.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = log.details ?: "",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
