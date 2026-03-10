package com.droidaio.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidaio.gallery.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(ThemeManager.getSavedTheme(ctx)) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ThemeOptionRow(
                label = "System default",
                selected = selected == ThemeManager.ThemeChoice.SYSTEM
            ) {
                selected = ThemeManager.ThemeChoice.SYSTEM
                ThemeManager.applyTheme(ctx, selected)
            }
            ThemeOptionRow(label = "Light", selected = selected == ThemeManager.ThemeChoice.LIGHT) {
                selected = ThemeManager.ThemeChoice.LIGHT
                ThemeManager.applyTheme(ctx, selected)
            }
            ThemeOptionRow(label = "Dark", selected = selected == ThemeManager.ThemeChoice.DARK) {
                selected = ThemeManager.ThemeChoice.DARK
                ThemeManager.applyTheme(ctx, selected)
            }
            ThemeOptionRow(
                label = "Pure Black (AMOLED)",
                selected = selected == ThemeManager.ThemeChoice.BLACK
            ) {
                selected = ThemeManager.ThemeChoice.BLACK
                ThemeManager.applyTheme(ctx, selected)
            }
        }
    }
}

@Composable
fun ThemeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        RadioButton(selected = selected, onClick = onClick)
    }
}

