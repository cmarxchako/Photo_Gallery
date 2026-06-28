package com.droidaio.gallery.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.droidaio.gallery.MainActivity

object Destinations {
    const val MAIN = "main"
    const val BACKUP = "backup"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val BACKUP_FOLDERS = "backup_folders"
    const val TRASH = "trash"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Destinations.MAIN) {
        composable(Destinations.MAIN) {
            MediaTabsScreen(
                onOpenBackup = { navController.navigate(Destinations.BACKUP) },
                onOpenHistory = { navController.navigate(Destinations.HISTORY) },
                onOpenAbout = { navController.navigate(Destinations.ABOUT) },
                onOpenTrash = { navController.navigate(Destinations.TRASH) },
                onOpenBackupFolders = { navController.navigate(Destinations.BACKUP_FOLDERS) }
            )
        }
        composable(Destinations.BACKUP) { BackupScreen() }
        composable(Destinations.HISTORY) {
            val activity = LocalContext.current as? MainActivity
            UndoHistoryScreen(onCancel = { opId ->
                activity?.cancelScheduledOp(opId)
            })
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onOpenBackupFolders = { navController.navigate(Destinations.BACKUP_FOLDERS) }
            )
        }
        composable(Destinations.ABOUT) { AboutAppScreen() }
        composable(Destinations.BACKUP_FOLDERS) {
            BackupFoldersScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.TRASH) {
            TrashScreen(onBack = { navController.popBackStack() })
        }
    }
}
