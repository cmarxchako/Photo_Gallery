package com.droidaio.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidaio.gallery.PrefsManager
import com.droidaio.gallery.R
import com.droidaio.gallery.ThemeManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenBackupFolders: () -> Unit = {}
) {
    val ctx = LocalContext.current
    var selectedTheme by remember { mutableStateOf(ThemeManager.getSavedTheme(ctx)) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showEncryptionDialog by remember { mutableStateOf(false) }
    var showDevOptionsDialog by remember { mutableStateOf(false) }

    var autoBackup by remember { mutableStateOf(PrefsManager.getAutoBackup(ctx)) }
    var wifiOnly by remember { mutableStateOf(PrefsManager.getBackupWifiOnly(ctx)) }
    var trashEnabled by remember { mutableStateOf(PrefsManager.getTrashingMode(ctx)) }
    var encryptionType by remember { mutableStateOf(PrefsManager.getVaultEncryptionType(ctx)) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(id = R.string.tabSettings)) }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Theme Section
            SettingsSectionTitle(stringResource(id = R.string.pref_theme_title))
            SettingsClickableRow(
                label = "App Theme",
                subtitle = selectedTheme.name.lowercase(Locale.ROOT).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                },
                onClick = { showThemeDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Cloud Backup Section
            SettingsSectionTitle("Backup")
            SettingsClickableRow(
                label = "Backup Settings",
                subtitle = if (autoBackup) "Enabled" else "Disabled",
                onClick = { showBackupDialog = true }
            )
            SettingsClickableRow(
                stringResource(id = R.string.setting_google_drive),
                "Sign in to Google"
            ) {
                // Trigger Google Sign-in flow
            }
            SettingsClickableRow(stringResource(id = R.string.setting_one_drive), "Sign in to OneDrive") {
                // Trigger OneDrive Sign-in flow
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Vault Section
            SettingsSectionTitle(stringResource(id = R.string.tabVault))
            SettingsClickableRow(
                stringResource(id = R.string.setting_vault_encryption),
                encryptionType
            ) {
                showEncryptionDialog = true
            }
            SettingsSwitchRow(stringResource(id = R.string.setting_enable_trash), trashEnabled) {
                trashEnabled = it
                PrefsManager.setTrashingMode(ctx, it)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // General Section
            SettingsSectionTitle("General")
            SettingsClickableRow(stringResource(id = R.string.setting_use_as_default), "") {
                // Logic to set as default app
            }
            SettingsClickableRow(stringResource(id = R.string.export_import_data), "Export/Import config") {
                // Implement export/import
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Developer Section
            SettingsSectionTitle(stringResource(id = R.string.developer_options))
            SettingsClickableRow("Developer Tools", "Logcat, Reset Data") {
                showDevOptionsDialog = true
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    ThemeOptionRow("System Default", selectedTheme == ThemeManager.ThemeChoice.SYSTEM) {
                        selectedTheme = ThemeManager.ThemeChoice.SYSTEM
                        ThemeManager.applyTheme(ctx, selectedTheme)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Light", selectedTheme == ThemeManager.ThemeChoice.LIGHT) {
                        selectedTheme = ThemeManager.ThemeChoice.LIGHT
                        ThemeManager.applyTheme(ctx, selectedTheme)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Dark", selectedTheme == ThemeManager.ThemeChoice.DARK) {
                        selectedTheme = ThemeManager.ThemeChoice.DARK
                        ThemeManager.applyTheme(ctx, selectedTheme)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Pure Black", selectedTheme == ThemeManager.ThemeChoice.BLACK) {
                        selectedTheme = ThemeManager.ThemeChoice.BLACK
                        ThemeManager.applyTheme(ctx, selectedTheme)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") } }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup Settings") },
            text = {
                Column {
                    SettingsSwitchRow(stringResource(id = R.string.setting_auto_backup), autoBackup) {
                        autoBackup = it
                        PrefsManager.setAutoBackup(ctx, it)
                    }
                    SettingsSwitchRow(stringResource(id = R.string.backup_wifi_only), wifiOnly) {
                        wifiOnly = it
                        PrefsManager.setBackupWifiOnly(ctx, it)
                    }
                    SettingsClickableRow(stringResource(id = R.string.backup_folders_title), "Manage folders") {
                        showBackupDialog = false
                        onOpenBackupFolders()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBackupDialog = false }) { Text("Done") } }
        )
    }

    if (showEncryptionDialog) {
        AlertDialog(
            onDismissRequest = { showEncryptionDialog = false },
            title = { Text("Vault Encryption") },
            text = {
                Column {
                    ThemeOptionRow("AES-256 (App Password)", encryptionType == "AES") {
                        encryptionType = "AES"
                        PrefsManager.setVaultEncryptionType(ctx, "AES")
                        showEncryptionDialog = false
                    }
                    ThemeOptionRow("Device Lock (Biometric/PIN)", encryptionType == "DEVICE") {
                        encryptionType = "DEVICE"
                        PrefsManager.setVaultEncryptionType(ctx, "DEVICE")
                        showEncryptionDialog = false
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showEncryptionDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDevOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showDevOptionsDialog = false },
            title = { Text("Developer Options") },
            text = {
                Column {
                    SettingsClickableRow(stringResource(id = R.string.view_logcat), "Show app logs") {
                        // Show Logcat sub-window/activity
                    }
                    SettingsClickableRow(stringResource(id = R.string.reset_app_data), "Clear all settings and cache") {
                        // Reset app data logic
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDevOptionsDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ThemeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
fun SettingsClickableRow(label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Icon(
            Icons.Default.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp),
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
