package com.dev.yoump3.preferences

interface AppPreferences {
    fun getThemeMode(): String?
    fun setThemeMode(mode: String)

    fun getServerUrl(): String?
    fun setServerUrl(url: String)
    fun clearServerUrl()
}
