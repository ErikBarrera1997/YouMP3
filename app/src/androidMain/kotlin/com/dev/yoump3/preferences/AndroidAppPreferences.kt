package com.dev.yoump3.preferences

import android.content.Context

private const val PREFS_NAME = "yoump3_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_SERVER_URL = "server_url"

class AndroidAppPreferences(
    context: Context
) : AppPreferences {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getThemeMode(): String? =
        prefs.getString(KEY_THEME_MODE, null)

    override fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    override fun getServerUrl(): String? =
        prefs.getString(KEY_SERVER_URL, null)?.takeIf { it.isNotBlank() }

    override fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    override fun clearServerUrl() {
        prefs.edit().remove(KEY_SERVER_URL).apply()
    }
}
