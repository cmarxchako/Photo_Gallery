package com.droidaio.gallery.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onBackup: () -> Unit,
    onVault: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = { Text("$selectedCount selected") },
        actions = {
            SelectionActions(
                onClear = onClear,
                onDelete = onDelete,
                onMove = onMove,
                onCopy = onCopy,
                onBackup = onBackup,
                onVault = onVault
            )
        }
    )
}

@Composable
fun RowScope.SelectionActions(
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onBackup: () -> Unit,
    onVault: () -> Unit,
) {
    IconButton(onClick = onBackup) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_upload),
            contentDescription = "Backup"
        )
    }
    IconButton(onClick = onCopy) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_save),
            contentDescription = "Copy"
        )
    }
    IconButton(onClick = onMove) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_directions),
            contentDescription = "Move"
        )
    }
    IconButton(onClick = onVault) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_lock_lock),
            contentDescription = "Vault"
        )
    }
    IconButton(onClick = onDelete) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_delete),
            contentDescription = "Delete"
        )
    }
    IconButton(onClick = onClear) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
            contentDescription = "Clear selection"
        )
    }
}

