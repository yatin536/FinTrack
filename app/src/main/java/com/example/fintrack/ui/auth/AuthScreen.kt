package com.example.fintrack.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.fintrack.data.local.PinManager
import com.example.fintrack.data.model.User
import com.example.fintrack.data.repository.TransactionRepository
import com.example.fintrack.ui.components.AppIcons
import kotlinx.coroutines.launch

private enum class AuthMode {
    UNLOCK_PIN,
    CREATE_ACCOUNT,
    SWITCH_PROFILE
}

@Composable
fun AuthScreen(
    repository: TransactionRepository,
    pinManager: PinManager,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fragmentActivity = context as? FragmentActivity

    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<User?>(null) }

    // Check if any user account is already configured
    val hasPin = remember { pinManager.isPinSet }
    var authMode by remember {
        mutableStateOf(if (hasPin) AuthMode.UNLOCK_PIN else AuthMode.CREATE_ACCOUNT)
    }

    LaunchedEffect(Unit) {
        val users = repository.getUsers()
        allUsers = users
        val activeId = repository.sessionManager.getActiveUserId()
        selectedUser = users.firstOrNull { it.id == activeId } ?: users.firstOrNull() ?: User.DEFAULT_USER
    }

    Crossfade(targetState = authMode, label = "AuthModeCrossfade") { mode ->
        when (mode) {
            AuthMode.UNLOCK_PIN -> {
                UnlockPinView(
                    user = selectedUser ?: User.DEFAULT_USER,
                    allUsers = allUsers,
                    pinManager = pinManager,
                    fragmentActivity = fragmentActivity,
                    onSuccess = {
                        val u = selectedUser ?: User.DEFAULT_USER
                        repository.sessionManager.login(u.id, u.name)
                        onAuthenticated()
                    },
                    onSwitchProfile = {
                        authMode = AuthMode.SWITCH_PROFILE
                    },
                    onCreateNew = {
                        authMode = AuthMode.CREATE_ACCOUNT
                    }
                )
            }
            AuthMode.CREATE_ACCOUNT -> {
                CreateAccountView(
                    isFirstUser = allUsers.isEmpty() || !hasPin,
                    onCreateAccount = { name, email, phone, pin ->
                        scope.launch {
                            val newUser = repository.createUser(
                                name = name,
                                email = email,
                                phone = phone
                            )
                            pinManager.setPin(pin, newUser.id)
                            pinManager.setPin(pin) // Also set as global default PIN
                            repository.sessionManager.login(newUser.id, newUser.name)
                            onAuthenticated()
                        }
                    },
                    onCancel = if (hasPin) {
                        { authMode = AuthMode.UNLOCK_PIN }
                    } else null
                )
            }
            AuthMode.SWITCH_PROFILE -> {
                SwitchProfileView(
                    users = allUsers,
                    activeUserId = selectedUser?.id ?: "",
                    onSelectUser = { user ->
                        selectedUser = user
                        authMode = AuthMode.UNLOCK_PIN
                    },
                    onCreateNew = {
                        authMode = AuthMode.CREATE_ACCOUNT
                    },
                    onBack = {
                        authMode = AuthMode.UNLOCK_PIN
                    }
                )
            }
        }
    }
}

@Composable
private fun UnlockPinView(
    user: User,
    allUsers: List<User>,
    pinManager: PinManager,
    fragmentActivity: FragmentActivity?,
    onSuccess: () -> Unit,
    onSwitchProfile: () -> Unit,
    onCreateNew: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableStateOf(0) }

    // Lockout countdown timer
    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            lockoutSeconds--
        }
    }

    val isBioEnabled = remember(user.id) {
        pinManager.isBiometricEnabled(user.id) || pinManager.isBiometricEnabled
    }

    val showBiometricPrompt = {
        if (fragmentActivity != null && isBioEnabled) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                fragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Don't show error if user cancelled
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            errorMessage = errString.toString()
                        }
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock FinTrack")
                .setSubtitle("Confirm your identity for ${user.displayName}")
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    // Auto-prompt biometric on launch if enabled
    LaunchedEffect(user.id) {
        if (isBioEnabled) {
            showBiometricPrompt()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Avatar & Greeting
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(user.colorHex),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.name.take(1).uppercase().ifBlank { "U" },
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                val subtitle = when {
                    lockoutSeconds > 0 -> "Too many attempts. Wait $lockoutSeconds seconds"
                    else -> "Enter your 6-digit Master PIN to unlock"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lockoutSeconds > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Error Message
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // 6 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val handleDigit = { digit: String ->
                    if (lockoutSeconds == 0 && enteredPin.length < 6) {
                        enteredPin += digit
                        errorMessage = null

                        if (enteredPin.length == 6) {
                            when (val result = pinManager.verifyPin(enteredPin, user.id)) {
                                is PinManager.VerificationResult.Success -> {
                                    onSuccess()
                                }
                                is PinManager.VerificationResult.Failed -> {
                                    errorMessage = "Incorrect PIN. ${result.attemptsRemaining} attempts left."
                                    enteredPin = ""
                                }
                                is PinManager.VerificationResult.LockedOut -> {
                                    lockoutSeconds = result.waitSeconds
                                    errorMessage = "Vault temporarily locked"
                                    enteredPin = ""
                                }
                            }
                        }
                    }
                }

                val handleBackspace = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        errorMessage = null
                    }
                }

                // Row 1: 1, 2, 3
                KeypadRow(listOf("1", "2", "3"), handleDigit)
                // Row 2: 4, 5, 6
                KeypadRow(listOf("4", "5", "6"), handleDigit)
                // Row 3: 7, 8, 9
                KeypadRow(listOf("7", "8", "9"), handleDigit)

                // Row 4: Biometric icon (or blank), 0, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBioEnabled) {
                        IconButton(
                            onClick = { showBiometricPrompt() },
                            modifier = Modifier.size(68.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Fingerprint,
                                contentDescription = "Biometric Unlock",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(Modifier.size(68.dp))
                    }

                    KeypadButton(text = "0", onClick = { handleDigit("0") })

                    IconButton(
                        onClick = handleBackspace,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Backspace,
                            contentDescription = "Backspace",
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Bottom Footer Options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (allUsers.size > 1) {
                        TextButton(onClick = onSwitchProfile) {
                            Text(
                                text = "Switch Profile",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    TextButton(onClick = onCreateNew) {
                        Text(
                            text = "+ New Vault",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAccountView(
    isFirstUser: Boolean,
    onCreateAccount: (name: String, email: String?, phone: String?, pin: String) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Shield Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.ShieldCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isFirstUser) "Welcome to FinTrack" else "Create New Financial Vault",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Privacy-first personal finance. 100% offline & local.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            errorMessage = null
                        },
                        label = { Text("Full Name *") },
                        placeholder = { Text("e.g. Yatin Kumar Singh") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address (Optional)") },
                        placeholder = { Text("yatin@example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pin = it },
                        label = { Text("6-Digit Master Security PIN *") },
                        placeholder = { Text("••••••") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) confirmPin = it },
                        label = { Text("Confirm 6-Digit PIN *") },
                        placeholder = { Text("••••••") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "Please enter your full name."
                        return@Button
                    }
                    if (pin.length != 6) {
                        errorMessage = "Master PIN must be exactly 6 digits."
                        return@Button
                    }
                    if (pin != confirmPin) {
                        errorMessage = "PINs do not match. Please verify."
                        return@Button
                    }
                    onCreateAccount(
                        fullName.trim(),
                        email.trim().ifBlank { null },
                        phone.trim().ifBlank { null },
                        pin
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Create Financial Vault",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (onCancel != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Back to Unlock")
                }
            }
        }
    }
}

@Composable
private fun SwitchProfileView(
    users: List<User>,
    activeUserId: String,
    onSelectUser: (User) -> Unit,
    onCreateNew: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(AppIcons.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Select Profile Vault",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            users.forEach { user ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (user.id == activeUserId)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSelectUser(user) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(user.colorHex),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user.email ?: "Local Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCreateNew,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("+ Add Another Vault User")
            }
        }
    }
}

@Composable
private fun KeypadRow(
    digits: List<String>,
    onDigitClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        digits.forEach { digit ->
            KeypadButton(text = digit, onClick = { onDigitClick(digit) })
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.size(68.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
