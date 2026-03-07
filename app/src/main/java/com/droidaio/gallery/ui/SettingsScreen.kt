package com.droidaio.gallery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
            ThemeOptionRow(label = "System default", selected = selected == ThemeManager.ThemeChoice.SYSTEM) {
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
            ThemeOptionRow(label = "Pure Black (AMOLED)", selected = selected == ThemeManager.ThemeChoice.PURE_BLACK) {
                selected = ThemeManager.ThemeChoice.PURE_BLACK
                ThemeManager.applyTheme(ctx, selected)
            }
        }
    }
}

@Composable
fun ThemeOptionRow(label : String, selected : Boolean, onClick : () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        RadioButton(selected = selected, onClick = onClick)
    }
}

