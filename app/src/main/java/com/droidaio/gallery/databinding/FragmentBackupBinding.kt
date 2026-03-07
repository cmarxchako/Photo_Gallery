package com.droidaio.gallery.databinding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.droidaio.gallery.R

/**
 * Manual view-binding for fragment_backup.xml
 */
class FragmentBackupBinding private constructor(
    val root : View,
    val title : TextView,
    val googleDriveButton : Button,
    val oneDriveButton : Button,
    val backupAllButton : Button,
) {
    companion object {
        fun inflate(inflater : LayoutInflater, parent : ViewGroup?, attachToParent : Boolean = false) : FragmentBackupBinding {
            val root = inflater.inflate(R.layout.fragment_backup, parent, attachToParent)
            val title = root.findViewById<TextView>(R.id.title)
            val googleDriveButton = root.findViewById<Button>(R.id.googleDriveButton)
            val oneDriveButton = root.findViewById<Button>(R.id.oneDriveButton)
            val backupAllButton = root.findViewById<Button>(R.id.backupAllButton)
            return FragmentBackupBinding(root, title, googleDriveButton, oneDriveButton, backupAllButton)
        }

        fun bind(view : View) : FragmentBackupBinding {
            val inflater = LayoutInflater.from(view.context)
            return inflate(inflater, view as? ViewGroup, false)
        }
    }
}
