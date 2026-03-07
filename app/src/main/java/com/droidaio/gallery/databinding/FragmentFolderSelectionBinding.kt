package com.droidaio.gallery.databinding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.droidaio.gallery.R

/**
 * Manual view-binding for fragment_folder_selection.xml
 */
class FragmentFolderSelectionBinding private constructor(
    val root : View,
    val title : TextView,
    val selectAllButton : Button,
    val invertButton : Button,
    val clearButton : Button,
    val foldersRecycler : RecyclerView,
    val progress : ProgressBar,
    val saveButton : Button,
    val cancelButton : Button,
) {
    companion object {
        fun inflate(inflater : LayoutInflater, parent : ViewGroup?, attachToParent : Boolean = false) : FragmentFolderSelectionBinding {
            val root = inflater.inflate(R.layout.fragment_folder_selection, parent, attachToParent)
            val title = root.findViewById<TextView>(R.id.title)
            val selectAllButton = root.findViewById<Button>(R.id.selectAllButton)
            val invertButton = root.findViewById<Button>(R.id.invertButton)
            val clearButton = root.findViewById<Button>(R.id.clearButton)
            val foldersRecycler = root.findViewById<RecyclerView>(R.id.foldersRecycler)
            val progress = root.findViewById<ProgressBar>(R.id.progress)
            val saveButton = root.findViewById<Button>(R.id.saveButton)
            val cancelButton = root.findViewById<Button>(R.id.cancelButton)
            return FragmentFolderSelectionBinding(root, title, selectAllButton, invertButton, clearButton, foldersRecycler, progress, saveButton, cancelButton)
        }

        fun bind(view : View) : FragmentFolderSelectionBinding {
            val inflater = LayoutInflater.from(view.context)
            return inflate(inflater, view as? ViewGroup, false)
        }
    }
}
