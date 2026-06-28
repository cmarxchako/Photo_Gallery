package com.droidaio.gallery

import android.content.Context

// PrefsManager: stores user settings and preferences.
object PrefsManager {
    private const val PREFS = "photo_gallery_prefs"
    private const val KEY_SELECTED_FOLDERS = "selected_folders"
    private const val KEY_AUTO_BACKUP = "auto_backup"
    private const val KEY_BACKUP_PHOTOS_ONLY = "backup_photos_only"
    private const val KEY_BACKUP_VIDEOS_ONLY = "backup_videos_only"
    private const val KEY_BACKUP_FOLDERS = "backup_folders"
    private const val KEY_TRASH_ENABLED = "trash_enabled"
    private const val KEY_BACKUP_WIFI_ONLY = "backup_wifi_only"
    private const val KEY_VAULT_ENCRYPTION_TYPE = "vault_encryption_type"

    fun saveSelectedFolders(context: Context, folderIds: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SELECTED_FOLDERS, folderIds)
            .apply()
    }

    fun getSelectedFolders(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SELECTED_FOLDERS, emptySet()) ?: emptySet()
    }

    fun setAutoBackup(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    fun getAutoBackup(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_BACKUP, false)

    fun setBackupPhotosOnly(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_BACKUP_PHOTOS_ONLY, enabled).apply()
    }

    fun getBackupPhotosOnly(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BACKUP_PHOTOS_ONLY, false)

    fun setBackupVideosOnly(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_BACKUP_VIDEOS_ONLY, enabled).apply()
    }

    fun getBackupVideosOnly(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BACKUP_VIDEOS_ONLY, false)

    fun setBackupFolders(context: Context, folders: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_BACKUP_FOLDERS, folders).apply()
    }

    fun getBackupFolders(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_BACKUP_FOLDERS, emptySet()) ?: emptySet()

    fun setTrashingMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_TRASH_ENABLED, enabled).apply()
    }

    fun getTrashingMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_TRASH_ENABLED, false)

    fun setBackupWifiOnly(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_BACKUP_WIFI_ONLY, enabled).apply()
    }

    fun getBackupWifiOnly(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BACKUP_WIFI_ONLY, true)

    fun setVaultEncryptionType(context: Context, type: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_VAULT_ENCRYPTION_TYPE, type).apply()
    }

    fun getVaultEncryptionType(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_VAULT_ENCRYPTION_TYPE, "AES") ?: "AES"
}
