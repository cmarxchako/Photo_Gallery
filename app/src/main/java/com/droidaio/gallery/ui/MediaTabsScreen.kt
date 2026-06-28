package com.droidaio.gallery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidaio.gallery.R

@Composable
fun MediaTabsScreen(
    onOpenBackup: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenBackupFolders: () -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem(stringResource(id = R.string.tabPhotos), Icons.Default.Photo),
        TabItem(stringResource(id = R.string.tabVideos), Icons.Default.VideoLibrary),
        TabItem(stringResource(id = R.string.tabVault), Icons.Default.Lock),
        TabItem(stringResource(id = R.string.tabSettings), Icons.Default.Settings),
        TabItem(stringResource(id = R.string.tabAdvanced), Icons.Default.Build),
        TabItem(stringResource(id = R.string.tabAbout), Icons.Default.Info)
    )

    val items by viewModel.items.collectAsState()
    val filteredItems = remember(items, selectedTab) {
        when (selectedTab) {
            0 -> items.filter { !it.isVideo }
            1 -> items.filter { it.isVideo }
            else -> items
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    divider = {},
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tab.title, style = MaterialTheme.typography.labelSmall) },
                            icon = { Icon(tab.icon, contentDescription = tab.title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0, 1 -> GalleryScreen(
                    onOpenBackup = onOpenBackup,
                    onOpenTrash = onOpenTrash,
                    viewModel = viewModel,
                    displayItems = filteredItems
                )

                2 -> VaultScreen(onOpenTrash = onOpenTrash)
                3 -> SettingsScreen(onOpenBackupFolders = onOpenBackupFolders)
                4 -> AdvancedScreen()
                5 -> AboutAppScreen()
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)
