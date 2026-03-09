package com.droidaio.gallery.databinding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.droidaio.gallery.R

/**
 * Manual view-binding for item_folder.xml
*/
class ItemFolderBinding private constructor(
    val root : View,
    val thumb : ImageView,
    val check : ImageView,
) {
    companion object {
        fun inflate(inflater : LayoutInflater, parent : ViewGroup?, attachToParent : Boolean = false) : ItemFolderBinding {
            val root = inflater.inflate(R.layout.item_folder, parent, attachToParent)
            val thumb = root.findViewById<ImageView>(R.id.thumb)
            val check = root.findViewById<ImageView>(R.id.check)
            return ItemFolderBinding(root, thumb, check)
        }

        fun bind(view : View) : ItemFolderBinding {
            val inflater = LayoutInflater.from(view.context)
            return inflate(inflater, view as? ViewGroup, false)
        }
    }
}

