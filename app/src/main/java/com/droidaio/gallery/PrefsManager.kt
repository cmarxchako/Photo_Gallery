package com.droidaio.gallery

import android.content.Context

// PrefsManager: stores user-selected folder (bucket) ids to include in gallery.
object PrefsManager {
    private const val PREFS = "photo_gallery_prefs"
    private const val KEY_SELECTED_FOLDERS = "selected_folders"

    fun saveSelectedFolders(context : Context, folderIds : Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SELECTED_FOLDERS, folderIds)
            .apply()
    }

    fun getSelectedFolders(context : Context) : Set<String> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SELECTED_FOLDERS, emptySet()) ?: emptySet()
    }

    fun clearSelectedFolders(context : Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_SELECTED_FOLDERS).apply()
    }
}

