package com.droidaio.gallery.ui

import android.content.Context
import com.droidaio.gallery.BackupManager
import com.droidaio.gallery.VaultManager
import com.droidaio.gallery.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun performBackup(context : Context, items : List<MediaItem>) {
    withContext(Dispatchers.IO) {
        try {
            BackupManager.backupToGoogleDrive(context, items)
        } finally {
            BackupManager.backupToOneDrive(context, items)
        }
    }
}

suspend fun performVault(context : Context, items : List<MediaItem>) {
    withContext(Dispatchers.IO) {
        VaultManager.lockToVault(context, items)
    }
}

