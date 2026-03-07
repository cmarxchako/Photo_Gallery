package com.droidaio.gallery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Backup") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Backup options and status will appear here.")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { /* trigger backup */ }) {
                Text("Start Backup")
            }
        }
    }
}

