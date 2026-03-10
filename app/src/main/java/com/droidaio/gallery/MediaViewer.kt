package com.droidaio.gallery

import android.content.Context
import android.content.Intent
import com.droidaio.gallery.models.MediaItem

object MediaViewer {
    fun open(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(item.uri, item.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    }
}

