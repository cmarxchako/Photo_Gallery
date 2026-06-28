package com.droidaio.gallery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.droidaio.gallery.models.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BackupManager - coordinates backing up media to cloud providers.
 */
object BackupManager {

    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "app_prefs"
    private const val PREF_GOOGLE_ACCESS_TOKEN = "google_access_token"

    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun canBackup(context: Context): Boolean {
        val wifiOnly = PrefsManager.getBackupWifiOnly(context)
        if (wifiOnly && !isWifiConnected(context)) {
            Log.w(TAG, "Backup skipped: Wi-Fi only mode enabled and Wi-Fi not connected.")
            return false
        }
        return true
    }

    fun backupToGoogleDrive(context: Context, items: List<MediaItem>) {
        if (!canBackup(context)) {
            Toast.makeText(context, "Backup waiting for Wi-Fi", Toast.LENGTH_SHORT).show()
            return
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(PREF_GOOGLE_ACCESS_TOKEN, null)

        if (accessToken.isNullOrBlank()) {
            Log.w(TAG, "No Google access token available. Skipping Google Drive backup.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                GoogleDriveManager.uploadFiles(context, items, accessToken)
                Log.i(TAG, "Upload to Google Drive finished for ${items.size} items")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to upload to Google Drive", ex)
            }
        }
    }

    fun backupToOneDrive(context: Context, items: List<MediaItem>) {
        if (!canBackup(context)) {
            Toast.makeText(context, "Backup waiting for Wi-Fi", Toast.LENGTH_SHORT).show()
            return
        }
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
