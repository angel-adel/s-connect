package com.adel.s_connect

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "s_connect_settings"
    private const val KEY_THEME = "theme"
    private const val KEY_LANGUAGE = "language"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Тема: "light", "dark", "system"
    fun getTheme(context: Context): String {
        return getPrefs(context).getString(KEY_THEME, "system") ?: "system"
    }

    fun setTheme(context: Context, theme: String) {
        getPrefs(context).edit().putString(KEY_THEME, theme).apply()
    }

    // Язык: "ru", "en", "uk", "es", "de"
    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, "ru") ?: "ru"
    }

    fun setLanguage(context: Context, language: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, language).apply()
    }
}