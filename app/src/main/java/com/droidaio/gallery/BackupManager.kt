package com.droidaio.gallery

import android.content.Context
import com.droidaio.gallery.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BackupManager {

    suspend fun backupToGoogleDrive(context : Context, items : List<MediaItem>) {
        withContext(Dispatchers.IO) {
            GoogleDriveManager.uploadFiles(context, items)
        }
    }

    suspend fun backupToOneDrive(context : Context, items : List<MediaItem>) {
        withContext(Dispatchers.IO) {
            OneDriveManager.uploadFiles(context, items)
        }
    }
}

