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
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Destinations.MAIN) {
        composable(Destinations.MAIN) {
            MediaTabsScreen(
                onOpenBackup = { navController.navigate(Destinations.BACKUP) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                onOpenAbout = { navController.navigate(Destinations.ABOUT) },
                onOpenHistory = { navController.navigate(Destinations.HISTORY) }
            )
        }
        composable(Destinations.BACKUP) { BackupScreen() }
        composable(Destinations.HISTORY) {
            val activity = LocalContext.current as? MainActivity
            UndoHistoryScreen(onCancel = { opId ->
                activity?.cancelScheduledOp(opId)
            })
        }
        composable(Destinations.SETTINGS) { SettingsScreen() }
        composable(Destinations.ABOUT) { AboutAppScreen() }
    }
}
