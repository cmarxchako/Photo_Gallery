package com.droidaio.gallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidaio.gallery.R
import com.droidaio.gallery.models.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val trashedItems = remember { mutableStateListOf<MediaItem>() }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.trash)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (trashedItems.isNotEmpty()) {
                            IconButton(onClick = { /* Empty trash logic */ }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Empty Trash")
                            }
                        }
                    }
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        if (trashedItems.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                Text("Trash is empty", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(trashedItems) { item ->
                    Box(modifier = Modifier.padding(4.dp)) {
                        Column {
                            Text(item.displayName ?: "Unknown", style = MaterialTheme.typography.bodySmall)
                            Row {
                                IconButton(onClick = { /* Restore logic */ }) {
                                    Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore")
                                }
                                IconButton(onClick = { /* Delete Forever logic */ }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
