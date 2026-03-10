package com.droidaio.gallery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidaio.gallery.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen() {
    var rootEnabled by remember { mutableStateOf(false) }
    var unlimitedStorage by remember { mutableStateOf(false) }
    var enhancedNotifications by remember { mutableStateOf(false) }
    var deepScan by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Advanced Options") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSectionTitle("System & Root")
            SettingsSwitchRow(
                label = stringResource(id = R.string.enableRoot),
                checked = rootEnabled,
                onCheckedChange = { rootEnabled = it }
            )
            SettingsSwitchRow(
                label = "Enhanced Notification Modes",
                checked = enhancedNotifications,
                onCheckedChange = { enhancedNotifications = it }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SettingsSectionTitle("Storage")
            SettingsSwitchRow(
                label = "Unlimited External Storage Access",
                checked = unlimitedStorage,
                onCheckedChange = { unlimitedStorage = it }
            )
            SettingsSwitchRow(
                label = "Deep Media Scan",
                checked = deepScan,
                onCheckedChange = { deepScan = it }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SettingsSectionTitle("Developer")
            SettingsClickableRow(label = "View Logcat", subtitle = "View internal app logs") {
                // Action to view logs
            }
            SettingsClickableRow(
                label = "Reset App Data",
                subtitle = "Clear all cache and preferences"
            ) {
                // Action to reset
            }
        }
    }
}
