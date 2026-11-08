package com.dev.yoump3.preferences

interface AppPreferences {
    fun getThemeMode(): String?
    fun setThemeMode(mode: String)

    fun getServerUrl(): String?
    fun setServerUrl(url: String)
    fun clearServerUrl()

    /**
     * Permiso de uso de datos y cookies. `null` significa que todavía no se le preguntó al
     * usuario; `true` que lo concedió y `false` que lo rechazó.
     */
    fun getDataUsageConsent(): Boolean?
    fun setDataUsageConsent(granted: Boolean)
}
