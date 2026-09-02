package com.dev.yoump3.preferences

import java.util.prefs.Preferences

private const val NODE_PATH = "com/dev/yoump3"
private const val KEY_THEME_MODE = "theme_mode"

class DesktopThemeStore : ThemeStore {

    private val prefs = Preferences.userRoot().node(NODE_PATH)

    override fun getThemeMode(): String? =
        prefs.get(KEY_THEME_MODE, null)

    override fun setThemeMode(mode: String) {
        prefs.put(KEY_THEME_MODE, mode)
    }
}
