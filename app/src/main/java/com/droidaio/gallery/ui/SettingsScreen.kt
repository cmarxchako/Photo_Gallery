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
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.droidaio.gallery.R
import com.droidaio.gallery.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var selectedTheme by remember { mutableStateOf(ThemeManager.getSavedTheme(ctx)) }
    var autoBackup by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(id = R.string.tabSettings)) }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Theme Section
            SettingsSectionTitle(stringResource(id = R.string.pref_theme_title))
            ThemeOptionRow(
                stringResource(id = R.string.pref_theme_system),
                selectedTheme == ThemeManager.ThemeChoice.SYSTEM
            ) {
                selectedTheme = ThemeManager.ThemeChoice.SYSTEM
                ThemeManager.applyTheme(ctx, selectedTheme)
            }
            ThemeOptionRow(
                stringResource(id = R.string.pref_theme_light),
                selectedTheme == ThemeManager.ThemeChoice.LIGHT
            ) {
                selectedTheme = ThemeManager.ThemeChoice.LIGHT
                ThemeManager.applyTheme(ctx, selectedTheme)
            }
            ThemeOptionRow(
                stringResource(id = R.string.pref_theme_dark),
                selectedTheme == ThemeManager.ThemeChoice.DARK
            ) {
                selectedTheme = ThemeManager.ThemeChoice.DARK
                ThemeManager.applyTheme(ctx, selectedTheme)
            }
            ThemeOptionRow(
                stringResource(id = R.string.pref_theme_pure_black),
                selectedTheme == ThemeManager.ThemeChoice.BLACK
            ) {
                selectedTheme = ThemeManager.ThemeChoice.BLACK
                ThemeManager.applyTheme(ctx, selectedTheme)
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Cloud Backup Section
            SettingsSectionTitle(stringResource(id = R.string.setting_cloud_backup))
            SettingsClickableRow(
                stringResource(id = R.string.setting_google_drive),
                "Not signed in"
            ) {
                // Trigger Google Sign-in
            }
            SettingsClickableRow(stringResource(id = R.string.setting_one_drive), "Not signed in") {
                // Trigger OneDrive Sign-in
            }
            SettingsSwitchRow(stringResource(id = R.string.setting_auto_backup), autoBackup) {
                autoBackup = it
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Vault Section
            SettingsSectionTitle(stringResource(id = R.string.tabVault))
            SettingsClickableRow(
                stringResource(id = R.string.setting_vault_encryption),
                "AES-256"
            ) {
                // Manage Encryption
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // General Section
            SettingsSectionTitle("General")
            SettingsClickableRow(stringResource(id = R.string.setting_default_app), "") {
                // Set as default
            }
        }
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
        Column {
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
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
