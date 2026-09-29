package com.dev.yoump3.preferences

import java.util.prefs.Preferences

private const val NODE_PATH = "com/dev/yoump3"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_SERVER_URL = "server_url"

class DesktopAppPreferences : AppPreferences {

    private val prefs = Preferences.userRoot().node(NODE_PATH)

    override fun getThemeMode(): String? =
        prefs.get(KEY_THEME_MODE, null)

    override fun setThemeMode(mode: String) {
        prefs.put(KEY_THEME_MODE, mode)
    }

    override fun getServerUrl(): String? =
        prefs.get(KEY_SERVER_URL, null)?.takeIf { it.isNotBlank() }

    override fun setServerUrl(url: String) {
        prefs.put(KEY_SERVER_URL, url)
    }

    override fun clearServerUrl() {
        prefs.remove(KEY_SERVER_URL)
    }
}
