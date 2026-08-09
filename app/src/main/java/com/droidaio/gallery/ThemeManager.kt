package com.droidaio.gallery

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS = "theme_prefs"
    private const val KEY_THEME = "theme_choice"

    enum class ThemeChoice { SYSTEM, LIGHT, DARK, PURE_BLACK }

    fun applySavedTheme(context : Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_THEME, ThemeChoice.SYSTEM.name) ?: ThemeChoice.SYSTEM.name
        applyTheme(context, ThemeChoice.valueOf(name))
    }

    fun applyTheme(context : Context, choice : ThemeChoice) {
        when (choice) {
            ThemeChoice.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            ThemeChoice.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeChoice.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeChoice.PURE_BLACK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, choice.name).apply()
    }

    fun getSavedTheme(context : Context) : ThemeChoice {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_THEME, ThemeChoice.SYSTEM.name) ?: ThemeChoice.SYSTEM.name
        return ThemeChoice.valueOf(name)
    }
}

