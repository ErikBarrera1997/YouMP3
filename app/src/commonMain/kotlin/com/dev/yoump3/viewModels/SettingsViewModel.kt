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

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val serverUrl: String = "",
    val committedServerUrl: String = "",
    val defaultServerUrl: String = "",
    val serverUrlError: String? = null
) {
    val isServerUrlDirty: Boolean
        get() = normalizeServerUrl(serverUrl) != committedServerUrl

    val hasServerUrlOverride: Boolean
        get() = committedServerUrl != defaultServerUrl
}

private fun normalizeServerUrl(url: String): String = url.trim().trimEnd('/')

private fun validateServerUrl(url: String): String? = when {
    url.isEmpty() -> "ESCRIBE UNA URL"
    !url.startsWith("http://") && !url.startsWith("https://") -> "DEBE EMPEZAR POR HTTP:// O HTTPS://"
    url.any { it.isWhitespace() } -> "LA URL NO PUEDE CONTENER ESPACIOS"
    else -> null
}

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    appConfig: AppConfig,
    private val onServerUrlCommitted: () -> Unit = {}
) : ViewModel() {
    private val defaultServerUrl: String = appConfig.serverBaseUrl

    var state by mutableStateOf(
        SettingsUiState(
            themeMode = initialThemeMode(),
            serverUrl = initialServerUrl(),
            committedServerUrl = initialServerUrl(),
            defaultServerUrl = defaultServerUrl
        )
    )
        private set

    val currentColors: YouMp3Colors
        get() = when (state.themeMode) {
            ThemeMode.DARK -> DarkThemeColors
            ThemeMode.LIGHT -> LightThemeColors
        }

    val effectiveServerUrl: String
        get() = state.committedServerUrl

    fun onThemeModeChange(mode: ThemeMode) {
        state = state.copy(themeMode = mode)
        appPreferences.setThemeMode(mode.name)
    }

    fun onServerUrlChange(value: String) {
        state = state.copy(serverUrl = value, serverUrlError = null)
    }

    fun onServerUrlSave() {
        val normalized = normalizeServerUrl(state.serverUrl)
        val error = validateServerUrl(normalized)
        if (error != null) {
            state = state.copy(serverUrlError = error)
            return
        }
        commitServerUrl(normalized)
    }

    fun onServerUrlReset() {
        commitServerUrl(state.defaultServerUrl)
    }

    private fun commitServerUrl(url: String) {
        val changed = url != state.committedServerUrl
        if (url == state.defaultServerUrl) {
            appPreferences.clearServerUrl()
        } else {
            appPreferences.setServerUrl(url)
        }
        state = state.copy(
            serverUrl = url,
            committedServerUrl = url,
            serverUrlError = null
        )
        if (changed) onServerUrlCommitted()
    }

    private fun initialThemeMode(): ThemeMode {
        val stored = appPreferences.getThemeMode() ?: return ThemeMode.DARK
        return runCatching { ThemeMode.valueOf(stored) }.getOrNull() ?: ThemeMode.DARK
    }

    private fun initialServerUrl(): String =
        appPreferences.getServerUrl() ?: defaultServerUrl
}