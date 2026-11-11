package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dev.yoump3.config.AppConfig
import com.dev.yoump3.interfaces.DarkThemeColors
import com.dev.yoump3.interfaces.LightThemeColors
import com.dev.yoump3.interfaces.YouMp3Colors
import com.dev.yoump3.preferences.AppPreferences

enum class ThemeMode(
    val title: String,
    val description: String
) {
    DARK("Modo Oscuro", "Tema oscuro predeterminado"),
    LIGHT("Modo Claro", "Tema claro de alto contraste");

    val label: String
        get() = title
}

enum class ServerTarget(
    val title: String,
    val description: String
) {
    ONLINE("Servidor online", "API desplegada en la nube"),
    LOCAL("Servidor local", "API en la red local");
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val serverTarget: ServerTarget = ServerTarget.ONLINE,
    val serverUrl: String = "",
    val dataUsageConsent: Boolean = false,
    val dataUsageConsentConfigured: Boolean = false
)

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    appConfig: AppConfig,
    private val onServerUrlCommitted: () -> Unit = {}
) : ViewModel() {
    private val onlineServerUrl: String = normalizeServerUrl(appConfig.serverBaseUrl)
    private val localServerUrl: String = normalizeServerUrl(appConfig.localServerBaseUrl)

    var state by mutableStateOf(
        SettingsUiState(
            themeMode = initialThemeMode(),
            serverTarget = initialServerTarget(),
            serverUrl = initialServerUrl(),
            dataUsageConsent = appPreferences.getDataUsageConsent() == true,
            dataUsageConsentConfigured = appPreferences.getDataUsageConsent() != null
        )
    )
        private set

    val currentColors: YouMp3Colors
        get() = when (state.themeMode) {
            ThemeMode.DARK -> DarkThemeColors
            ThemeMode.LIGHT -> LightThemeColors
        }

    val effectiveServerUrl: String
        get() = state.serverUrl

    fun onThemeModeChange(mode: ThemeMode) {
        state = state.copy(themeMode = mode)
        appPreferences.setThemeMode(mode.name)
    }

    /**
     * Relee el permiso de las preferencias. El aviso de la búsqueda y Ajustes son dos pantallas
     * distintas y comparten el mismo valor persistido; al abrir Ajustes se refresca para no mostrar
     * una decisión vieja.
     */
    fun refreshDataUsageConsent() {
        state = state.copy(
            dataUsageConsent = appPreferences.getDataUsageConsent() == true,
            dataUsageConsentConfigured = appPreferences.getDataUsageConsent() != null
        )
    }

    fun onDataUsageConsentChange(granted: Boolean) {
        appPreferences.setDataUsageConsent(granted)
        state = state.copy(
            dataUsageConsent = granted,
            dataUsageConsentConfigured = true
        )
    }

    fun onServerTargetChange(target: ServerTarget) {
        if (target == state.serverTarget) return
        commitServerTarget(target)
    }

    private fun commitServerTarget(target: ServerTarget) {
        val url = urlFor(target)
        val changed = url != state.serverUrl
        if (target == ServerTarget.ONLINE) {
            appPreferences.clearServerUrl()
        } else {
            appPreferences.setServerUrl(url)
        }
        state = state.copy(
            serverTarget = target,
            serverUrl = url
        )
        if (changed) onServerUrlCommitted()
    }

    private fun urlFor(target: ServerTarget): String = when (target) {
        ServerTarget.ONLINE -> onlineServerUrl
        ServerTarget.LOCAL -> localServerUrl
    }

    private fun initialThemeMode(): ThemeMode {
        val stored = appPreferences.getThemeMode() ?: return ThemeMode.DARK
        return runCatching { ThemeMode.valueOf(stored) }.getOrNull() ?: ThemeMode.DARK
    }

    private fun initialServerTarget(): ServerTarget {
        val stored = appPreferences.getServerUrl()?.let(::normalizeServerUrl)
        return if (stored == localServerUrl) ServerTarget.LOCAL else ServerTarget.ONLINE
    }

    private fun initialServerUrl(): String = urlFor(initialServerTarget())
}

private fun normalizeServerUrl(url: String): String = url.trim().trimEnd('/')
