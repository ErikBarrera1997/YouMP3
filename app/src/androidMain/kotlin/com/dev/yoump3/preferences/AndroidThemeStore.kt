package com.dev.yoump3.preferences

import android.content.Context

private const val PREFS_NAME = "yoump3_prefs"
private const val KEY_THEME_MODE = "theme_mode"

class AndroidThemeStore(
    context: Context
) : ThemeStore {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getThemeMode(): String? =
        prefs.getString(KEY_THEME_MODE, null)

    override fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }
}
