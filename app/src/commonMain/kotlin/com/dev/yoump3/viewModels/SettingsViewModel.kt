package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dev.yoump3.interfaces.DarkThemeColors
import com.dev.yoump3.interfaces.LightThemeColors
import com.dev.yoump3.interfaces.YouMp3Colors

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

class SettingsViewModel {
    var state by mutableStateOf(SettingsUiState())
        private set

    val currentColors: YouMp3Colors
        get() = when (state.themeMode) {
            ThemeMode.DARK -> DarkThemeColors
            ThemeMode.LIGHT -> LightThemeColors
        }

    fun onThemeModeChange(mode: ThemeMode) {
        state = state.copy(themeMode = mode)
    }
}