package com.example.fintrack

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.fintrack.data.local.ThemeMode
import com.example.fintrack.data.local.ThemePreferenceManager
import com.example.fintrack.theme.FinTrackTheme
import com.example.fintrack.ui.MainAppShell
import com.example.fintrack.ui.components.AppIcons

class MainActivity : FragmentActivity() {

    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeManager = remember { ThemePreferenceManager(this) }
            val themeMode by themeManager.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }

            var showEducationalPermissionDialog by remember {
                mutableStateOf(needsSmsPermission(this))
            }

            FinTrackTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppShell(
                        isDark = isDark,
                        onToggleDarkMode = { themeManager.toggleDarkMode() }
                    )

                    if (showEducationalPermissionDialog) {
                        EducationalPermissionDialog(
                            onConfirm = {
                                showEducationalPermissionDialog = false
                                requestSmsPermissions()
                            },
                            onDismiss = {
                                showEducationalPermissionDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun needsSmsPermission(context: Context): Boolean {
        val receiveGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        return !receiveGranted || !readGranted
    }

    private fun requestSmsPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        requestSmsPermissionLauncher.launch(permissions)
    }
}

@Composable
fun EducationalPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Shield,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "SMS Financial Ingestion",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "FinTrack parses transaction messages from your bank to automatically update your balances and credit limits.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(4.dp))

                Text("• 100% On-Device Processing:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Messages are analyzed entirely on your device with local regular expressions. No third-party services are invoked.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("• Zero Internet Permissions:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("FinTrack cannot transmit your data over the internet because internet permissions are completely absent from this application.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("• OTPs & Personal Chats Ignored:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Verification codes and non-banking personal SMS are automatically filtered and immediately discarded.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Grant SMS Permission")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Continue Manually")
            }
        }
    )
}
