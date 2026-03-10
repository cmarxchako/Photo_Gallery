package com.droidaio.gallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidaio.gallery.VaultFile
import com.droidaio.gallery.VaultManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vaultFiles = remember { mutableStateListOf<VaultFile>() }
    var selectedFiles by remember { mutableStateOf(setOf<String>()) }

    fun loadVault() {
        scope.launch {
            val files = VaultManager.listVaultFiles(context)
            vaultFiles.clear()
            vaultFiles.addAll(files.map { VaultFile(it.name, it.absolutePath) })
        }
    }

    LaunchedEffect(Unit) {
        loadVault()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault") },
                actions = {
                    if (selectedFiles.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                // Restoration logic would go here
                                selectedFiles = emptySet()
                                loadVault()
                            }
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                selectedFiles.forEach { path ->
                                    val vf = vaultFiles.find { it.filePath == path }
                                    if (vf != null) VaultManager.deleteVaultFile(context, vf)
                                }
                                selectedFiles = emptySet()
                                loadVault()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (vaultFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Vault is empty", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(vaultFiles) { file ->
                    selectedFiles.contains(file.filePath)
                    // Simplified item representation for vault
                    Box(modifier = Modifier.padding(4.dp)) {
                        Text(file.name, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}
