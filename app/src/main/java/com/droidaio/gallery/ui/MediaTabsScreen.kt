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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidaio.gallery.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaTabsScreen(
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        TabItem(stringResource(id = R.string.tabPhotos), Icons.Default.Photo),
        TabItem(stringResource(id = R.string.tabVideos), Icons.Default.VideoLibrary),
        TabItem(stringResource(id = R.string.tabVault), Icons.Default.Lock),
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
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = onOpenSettings
                )
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
                    viewModel = viewModel,
                    onOpenBackup = onOpenBackup,
                    onOpenSettings = onOpenSettings,
                    displayItems = filteredItems
                )

                2 -> VaultScreen()
                3 -> AdvancedScreen()
                4 -> AboutAppScreen()
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)
