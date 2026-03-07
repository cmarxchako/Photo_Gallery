package com.droidaio.gallery.databinding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.droidaio.gallery.R

/*
 * Manual view-binding for item_media.xml
*/
class ItemMediaBinding private constructor(
    val root : View,
    val thumb : ImageView,
    val check : ImageView,
) {
    companion object {
        fun inflate(inflater : LayoutInflater, parent : ViewGroup?, attachToParent : Boolean = false) : ItemMediaBinding {
            val root = inflater.inflate(R.layout.item_media, parent, attachToParent)
            val thumb = root.findViewById<ImageView>(R.id.thumb)
            val check = root.findViewById<ImageView>(R.id.check)
            return ItemMediaBinding(root, thumb, check)
        }

        fun bind(view : View) : ItemMediaBinding {
            val inflater = LayoutInflater.from(view.context)
            return inflate(inflater, view as? ViewGroup, false)
        }
    }
}

