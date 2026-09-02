package com.dev.yoump3.preferences

interface ThemeStore {
    fun getThemeMode(): String?
    fun setThemeMode(mode: String)
}
