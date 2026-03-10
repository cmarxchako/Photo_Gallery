package com.droidaio.gallery.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.droidaio.gallery.MainActivity

object Destinations {
    const val GALLERY = "gallery"
    const val BACKUP = "backup"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Destinations.GALLERY) {
        composable("gallery") {
            GalleryScreen(
                onOpenBackup = {
                    navController.navigate(Destinations.BACKUP); navController.navigate(
                    Destinations.HISTORY
                )
                },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) })
        }
        composable("backup") { BackupScreen() }
        //composable("history") { UndoHistoryScreen { LocalContext.current as MainActivity } }
        composable(Destinations.HISTORY) {
            val activity = LocalContext.current as? MainActivity
            UndoHistoryScreen(onCancel = { opId ->
                activity?.cancelScheduledOp(opId)
            })
        }
        composable("settings") { SettingsScreen() }
    }
}

