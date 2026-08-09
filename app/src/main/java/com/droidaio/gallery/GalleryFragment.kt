package com.droidaio.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.droidaio.gallery.ui.GalleryScreen
import com.droidaio.gallery.ui.theme.PhotoGalleryTheme

/**
 * Main Fragment hosting the gallery screen. Uses Compose for UI.
 * Handles navigation to backup and settings screens.
 */
class GalleryFragment : Fragment() {

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View {
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                // Use ThemeManager to determine pure-black/dark if needed
                val choice = ThemeManager.getSavedTheme(requireContext())
                val usePureBlack = (choice == ThemeManager.ThemeChoice.PURE_BLACK)
                PhotoGalleryTheme(usePureBlack = usePureBlack, darkTheme = (choice == ThemeManager.ThemeChoice.DARK || usePureBlack)) {
                    GalleryScreen(
                        onOpenBackup = { findNavController().navigate("backup") },
                        onOpenSettings = { findNavController().navigate("settings") }
                    )
                }
            }
        }
        return composeView
    }

    // inside GalleryFragment
    fun refreshWithSelectedFolders() {
        //val selectedFolderIds = PrefsManager.getSelectedFolders(requireContext())
        //val repository = MediaRepository(requireContext())
        //val mediaItems = repository.queryAllMedia().filter { selectedFolderIds.contains(it.bucketId) }
        //repository.loadMediaItems(selectedFolderIds)
    }

}

