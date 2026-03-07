package com.droidaio.gallery.databinding

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.droidaio.gallery.R

/**
 * Manual view-binding for fragment_gallery.xml
 * Exposes the views used by GalleryFragment.
 */
class FragmentGalleryBinding private constructor(
    val root : View,
    val viewModeButton : Button,
    val refreshButton : Button,
    val restoreButton : Button,
    val selectAllButton : Button,
    val clearSelectionButton : Button,
    val recycler : RecyclerView,
    val deleteButton : Button,
    val moveButton : Button,
    val backupButton : Button,
    val vaultButton : Button,
    val progress : ProgressBar,
) {
    companion object {
        @SuppressLint("MissingInflatedId")
        fun inflate(inflater : LayoutInflater, parent : ViewGroup?, attachToParent : Boolean = false) : FragmentGalleryBinding {
            val root = inflater.inflate(R.layout.fragment_gallery, parent, attachToParent)
            val viewModeButton = root.findViewById<Button>(R.id.viewModeButton)
            val refreshButton = root.findViewById<Button>(R.id.refreshButton)
            val restoreButton = root.findViewById<Button>(R.id.restoreButton)
            val selectAllButton = root.findViewById<Button>(R.id.selectAllButton)
            val clearSelectionButton = root.findViewById<Button>(R.id.clearSelectionButton)
            val recycler = root.findViewById<RecyclerView>(R.id.recycler)
            val deleteButton = root.findViewById<Button>(R.id.deleteButton)
            val moveButton = root.findViewById<Button>(R.id.moveButton)
            val backupButton = root.findViewById<Button>(R.id.backupButton)
            val vaultButton = root.findViewById<Button>(R.id.vaultButton)
            val progress = root.findViewById<ProgressBar>(R.id.progress)
            return FragmentGalleryBinding(root, viewModeButton, restoreButton, refreshButton, selectAllButton, clearSelectionButton, recycler, deleteButton, moveButton, backupButton, vaultButton, progress)
        }

        fun bind(view : View) : FragmentGalleryBinding {
            val inflater = LayoutInflater.from(view.context)
            return inflate(inflater, view as? ViewGroup, false)
        }
    }
}
