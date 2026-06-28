package com.droidaio.gallery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidaio.gallery.MainActivity
import com.droidaio.gallery.PendingOperation
import com.droidaio.gallery.PrefsManager
import com.droidaio.gallery.R
import com.droidaio.gallery.models.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onOpenBackup: () -> Unit = {},
    onOpenTrash: () -> Unit = {},
    viewModel: GalleryViewModel = viewModel(),
    displayItems: List<MediaItem>? = null
) {
    val allItems by viewModel.items.collectAsState()
    val items = displayItems ?: allItems
    val selectedIds by viewModel.selectedIds.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var gridColumns by remember { mutableStateOf(3) }
    var showMenu by remember { mutableStateOf(false) }

    val itemProgress = remember { mutableStateMapOf<Long, Int>() }
    val trashEnabled = remember { PrefsManager.getTrashingMode(context) }

    LaunchedEffect(Unit) {
        AppEventBus.events.collect { ev ->
            when (ev) {
                is AppEventBus.UiEvent.ShowItemProgress -> {
                    val mediaId = ev.mediaId
                    if (mediaId != null) {
                        itemProgress[mediaId] = ev.percent.coerceIn(0, 100)
                        if (ev.percent >= 100) {
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
                            (context as? MainActivity)?.cancelScheduledOp(ev.id)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                if (selectedIds.isNotEmpty()) {
                    SelectionTopBar(
                        selectedCount = selectedIds.size,
                        onClear = { viewModel.clearSelection() },
                        onDelete = {
                            val toDelete = viewModel.getSelectedItems()
                            viewModel.scheduleDeleteWithUndo(toDelete)
                        },
                        onMove = {
                            val toMove = viewModel.getSelectedItems()
                            (context as? MainActivity)?.startFolderPickerForOperationWithType(
                                PendingOperation.Type.MOVE,
                                toMove
                            )
                            viewModel.clearSelection()
                        },
                        onCopy = {
                            val toCopy = viewModel.getSelectedItems()
                            (context as? MainActivity)?.startFolderPickerForOperationWithType(
                                PendingOperation.Type.COPY,
                                toCopy
                            )
                            viewModel.clearSelection()
                        },
                        onBackup = { viewModel.clearSelection() },
                        onVault = { viewModel.clearSelection() }
                    )
                } else {
                    TopAppBar(
                        title = { Text("Gallery") },
                        actions = {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Grid Size") },
                                    onClick = {
                                        gridColumns =
                                            if (gridColumns == 3) 4 else if (gridColumns == 4) 2 else 3
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.GridView,
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Backup") },
                                    onClick = {
                                        onOpenBackup()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(id = android.R.drawable.ic_menu_upload),
                                            contentDescription = null
                                        )
                                    }
                                )
                                if (trashEnabled) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.trash)) },
                                        onClick = {
                                            onOpenTrash()
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No media found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val percent = itemProgress[item.id]
                    SelectableMediaItem(
                        item = item,
                        selected = selectedIds.contains(item.id),
                        progressPercent = percent,
                        onClick = { viewModel.toggleSelection(item.id) },
                        onLongClick = { viewModel.toggleSelection(item.id) }
                    )
                }
            }
        }
    }
}
