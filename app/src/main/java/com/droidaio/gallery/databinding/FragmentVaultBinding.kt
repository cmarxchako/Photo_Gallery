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
 * Manual view-binding for fragment_vault.xml
 */
class FragmentVaultBinding private constructor(
    val root : View,
    val title : TextView,
    val vaultRecycler : RecyclerView,
    val refreshVaultButton : Button,
    val restoreVaultButton : Button,
    val deleteVaultButton : Button,
    val progress : ProgressBar,
) {
    companion object {
        fun inflate(inflater : LayoutInflater, parent : ViewGroup?, attachToParent : Boolean = false) : FragmentVaultBinding {
            val root = inflater.inflate(R.layout.fragment_vault, parent, attachToParent)
            val title = root.findViewById<TextView>(R.id.title)
            val vaultRecycler = root.findViewById<RecyclerView>(R.id.vaultRecycler)
            val refreshVaultButton = root.findViewById<Button>(R.id.refreshVaultButton)
            val restoreVaultButton = root.findViewById<Button>(R.id.restoreVaultButton)
            val deleteVaultButton = root.findViewById<Button>(R.id.deleteVaultButton)
            val progress = root.findViewById<ProgressBar>(R.id.vaultProgress)
            return FragmentVaultBinding(root, title, vaultRecycler, refreshVaultButton, restoreVaultButton, deleteVaultButton, progress)
        }

        fun bind(view : View) : FragmentVaultBinding {
            val inflater = LayoutInflater.from(view.context)
            return inflate(inflater, view as? ViewGroup, false)
        }
    }
}
