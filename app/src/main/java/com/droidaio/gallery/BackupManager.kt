package com.droidaio.gallery

import android.content.Context
import android.util.Log
import com.droidaio.gallery.models.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BackupManager - coordinates backing up media to cloud providers.
 *
 * Fix applied:
 *  - GoogleDriveManager.uploadFiles requires an accessToken parameter.
 *    We read the token from SharedPreferences (key "google_access_token") and pass it.
 *  - Calls to suspend upload functions are launched in a CoroutineScope(Dispatchers.IO).
 *
 * You may replace the SharedPreferences access with your own account/token management (if you
 * already store the token somewhere else).
 */
object BackupManager {

    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "app_prefs"
    private const val PREF_GOOGLE_ACCESS_TOKEN = "google_access_token"

    /**
     * Start backup to Google Drive for a list of media items.
     * This function will return immediately (upload runs in background).
     */
    fun backupToGoogleDrive(context: Context, items: List<MediaItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(PREF_GOOGLE_ACCESS_TOKEN, null)

        if (accessToken.isNullOrBlank()) {
            Log.w(TAG, "No Google access token available. Skipping Google Drive backup.")
            // Optionally you could start the sign-in flow here.
            return
        }

        // Launch the suspend upload in an IO coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                GoogleDriveManager.uploadFiles(context, items, accessToken)
                Log.i(TAG, "Upload to Google Drive finished for ${items.size} items")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to upload to Google Drive", ex)
            }
        }
    }

    /**
     * Start backup to OneDrive for a list of media items (OneDriveManager handles MSAL internally).
     */
    fun backupToOneDrive(context: Context, items: List<MediaItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                OneDriveManager.uploadFiles(context, items)
                Log.i(TAG, "Upload to OneDrive finished for ${items.size} items")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to upload to OneDrive", ex)
            }
        }
    }
}

