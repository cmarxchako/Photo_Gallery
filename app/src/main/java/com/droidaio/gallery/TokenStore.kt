package com.droidaio.gallery

import android.content.Context

object TokenStore {
    private const val PREFS = "token_store"
    private const val KEY_GOOGLE_TOKEN = "google_token"
    private const val KEY_ONEDRIVE_TOKEN = "onedrive_token"

    fun saveGoogleToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_GOOGLE_TOKEN, token).apply()
    }

    fun getGoogleToken(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_GOOGLE_TOKEN, null)
    }

    fun clearGoogleToken(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_GOOGLE_TOKEN)
            .apply()
    }

    fun saveOneDriveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ONEDRIVE_TOKEN, token).apply()
    }

    fun getOneDriveToken(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ONEDRIVE_TOKEN, null)
    }

    fun clearOneDriveToken(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ONEDRIVE_TOKEN)
            .apply()
    }
}

