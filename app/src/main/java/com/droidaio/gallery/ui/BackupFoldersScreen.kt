package com.droidaio.gallery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidaio.gallery.MediaRepository
import com.droidaio.gallery.PrefsManager
import com.droidaio.gallery.R
import com.droidaio.gallery.models.FolderInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupFoldersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }
    val folders = remember { mutableStateListOf<FolderInfo>() }
    val selectedFolders = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        folders.addAll(repository.queryFolders())
        selectedFolders.addAll(PrefsManager.getBackupFolders(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.backup_folders_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(folders) { folder ->
                val bucketId = folder.bucketId ?: ""
                val isSelected = selectedFolders.contains(bucketId)
                FolderBackupRow(
                    folder = folder,
                    isSelected = isSelected,
                    onToggle = { checked ->
                        if (checked) {
                            if (bucketId.isNotEmpty()) selectedFolders.add(bucketId)
                        } else {
                            selectedFolders.remove(bucketId)
                        }
                        PrefsManager.setBackupFolders(context, selectedFolders.toSet())
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun FolderBackupRow(folder: FolderInfo, isSelected: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = folder.bucketName ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${folder.itemCount} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Switch(checked = isSelected, onCheckedChange = onToggle)
    }
}
