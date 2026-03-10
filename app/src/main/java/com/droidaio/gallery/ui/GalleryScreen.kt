package com.droidaio.gallery.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidaio.gallery.models.MediaItem
import com.droidaio.gallery.ui.theme.PhotoGalleryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main gallery screen composable. Displays media items in a grid,
 * allows selection and batch operations.
 * Listens to AppEventBus for progress updates and snackbars.
 * @param onOpenBackup callback to open backup screen
 * @param onOpenSettings callback to open settings screen
 * @param viewModel GalleryViewModel instance (can be injected for testing)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onOpenBackup: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: GalleryViewModel = viewModel(),
) {
    val items by viewModel.items.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Map mediaId -> percent (0..100)
    val itemProgress = remember { mutableStateMapOf<Long, Int>() }

    // Collect AppEventBus events and update per-item progress map
    LaunchedEffect(Unit) {
        AppEventBus.events.collect { ev ->
            when (ev) {
                is AppEventBus.UiEvent.ShowItemProgress -> {
                    val mediaId = ev.mediaId
                    if (mediaId != null) {
                        itemProgress[mediaId] = ev.percent.coerceIn(0, 100)
                        if (ev.percent >= 100) {
                            // keep 100% visible briefly then remove
                            delay(400)
                            itemProgress.remove(mediaId)
                        }
                    }
                }

                is AppEventBus.UiEvent.ShowSnackbar -> {
                    coroutineScope.launch { snackbarHostState.showSnackbar(ev.message) }
                }

                is AppEventBus.UiEvent.ShowUndoableSnackbar -> {
                    coroutineScope.launch {
                        val result =
                            snackbarHostState.showSnackbar(ev.message, actionLabel = ev.actionLabel)
                        if (result == SnackbarResult.ActionPerformed) {
                            (context as? com.droidaio.gallery.MainActivity)?.cancelScheduledOp(ev.id)
                        }
                    }
                }

                else -> { /* other events handled elsewhere */
                }
            }
        }
    }

    GalleryScreenContent(
        items = items,
        selectedIds = selectedIds,
        itemProgress = itemProgress,
        onOpenBackup = onOpenBackup,
        onOpenSettings = onOpenSettings,
        onToggleSelection = { viewModel.toggleSelection(it) },
        onClearSelection = { viewModel.clearSelection() },
        onDelete = {
            val toDelete = viewModel.getSelectedItems()
            viewModel.scheduleDeleteWithUndo(toDelete)
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    "Deleted ${toDelete.size} items",
                    actionLabel = "Undo"
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.cancelDelete(toDelete.map { it.id })
                    snackbarHostState.showSnackbar("Undo successful")
                }
            }
        },
        onMove = {
            val toMove = viewModel.getSelectedItems()
            (context as? com.droidaio.gallery.MainActivity)?.startFolderPickerForOperationWithType(
                com.droidaio.gallery.PendingOperation.Type.MOVE,
                toMove
            )
            viewModel.clearSelection()
        },
        onCopy = {
            val toCopy = viewModel.getSelectedItems()
            (context as? com.droidaio.gallery.MainActivity)?.startFolderPickerForOperationWithType(
                com.droidaio.gallery.PendingOperation.Type.COPY,
                toCopy
            )
            viewModel.clearSelection()
        },
        onBackup = {
            val toBackup = viewModel.getSelectedItems()
            coroutineScope.launch {
                try {
                    performBackup(context, toBackup)
                    snackbarHostState.showSnackbar("Backup started for ${toBackup.size} items")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Backup failed: ${e.localizedMessage}")
                }
            }
            viewModel.clearSelection()
        },
        onVault = {
            val toVault = viewModel.getSelectedItems()
            coroutineScope.launch {
                try {
                    performVault(context, toVault)
                    snackbarHostState.showSnackbar("Moved ${toVault.size} items to vault")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Vault failed: ${e.localizedMessage}")
                }
            }
            viewModel.clearSelection()
        },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryScreenContent(
    items: List<MediaItem>,
    selectedIds: Set<Long>,
    itemProgress: Map<Long, Int>,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSelection: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onBackup: () -> Unit,
    onVault: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    onClear = onClearSelection,
                    onDelete = onDelete,
                    onMove = onMove,
                    onCopy = onCopy,
                    onBackup = onBackup,
                    onVault = onVault
                )
            } else {
                TopAppBar(title = { Text("Gallery") }, actions = {
                    IconButton(onClick = onOpenBackup) {
                        Icon(
                            painterResource(id = android.R.drawable.ic_menu_upload),
                            contentDescription = "Backup"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painterResource(id = android.R.drawable.ic_menu_manage),
                            contentDescription = "Settings"
                        )
                    }
                })
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                Text("No media found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(items) { item ->
                    val percent = itemProgress[item.id]
                    SelectableMediaItem(
                        item = item,
                        selected = selectedIds.contains(item.id),
                        progressPercent = percent,
                        onClick = { onToggleSelection(item.id) },
                        onLongClick = { onToggleSelection(item.id) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GalleryScreenPreview() {
    PhotoGalleryTheme {
        GalleryScreenContent(
            items = listOf(
                MediaItem(1L, Uri.EMPTY, "IMG_001.jpg", "image/jpeg", null, null, false),
                MediaItem(2L, Uri.EMPTY, "IMG_002.jpg", "image/jpeg", null, null, false),
                MediaItem(3L, Uri.EMPTY, "VID_001.mp4", "video/mp4", null, null, true),
            ),
            selectedIds = setOf(1L),
            itemProgress = mapOf(2L to 50),
            onOpenBackup = {},
            onOpenSettings = {},
            onToggleSelection = {},
            onClearSelection = {},
            onDelete = {},
            onMove = {},
            onCopy = {},
            onBackup = {},
            onVault = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
