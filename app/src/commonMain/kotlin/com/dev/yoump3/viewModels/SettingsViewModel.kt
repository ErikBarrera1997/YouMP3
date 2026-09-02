package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dev.yoump3.interfaces.DarkThemeColors
import com.dev.yoump3.interfaces.LightThemeColors
import com.dev.yoump3.interfaces.YouMp3Colors
import com.dev.yoump3.preferences.ThemeStore

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
    val themeMode: ThemeMode = ThemeMode.DARK
)

class SettingsViewModel(
    private val themeStore: ThemeStore
) : ViewModel() {
    var state by mutableStateOf(SettingsUiState(themeMode = initialThemeMode()))
        private set

    val currentColors: YouMp3Colors
        get() = when (state.themeMode) {
            ThemeMode.DARK -> DarkThemeColors
            ThemeMode.LIGHT -> LightThemeColors
        }

    fun onThemeModeChange(mode: ThemeMode) {
        state = state.copy(themeMode = mode)
        themeStore.setThemeMode(mode.name)
    }

    private fun initialThemeMode(): ThemeMode {
        val stored = themeStore.getThemeMode() ?: return ThemeMode.DARK
        return runCatching { ThemeMode.valueOf(stored) }.getOrNull() ?: ThemeMode.DARK
    }
}