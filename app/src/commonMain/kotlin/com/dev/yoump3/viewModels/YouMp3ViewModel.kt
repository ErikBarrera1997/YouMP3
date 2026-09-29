package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dev.yoump3.appVersion
import com.dev.yoump3.config.AppConfig
import com.dev.yoump3.preferences.AppPreferences

enum class YouMp3Screen {
    Home,
    SongInput,
    Settings
}

data class YouMp3UiState(
    val appTitle: String = "YOUMP3",
    val title: String = "FIND IT!",
    val footer: String = "BY CLEVER CLOUD · v$appVersion",
    val currentScreen: YouMp3Screen = YouMp3Screen.Home,
    val findButtonPresses: Int = 0,
    val connectionCheckToken: Int = 0
)

class YouMp3ViewModel(
    appPreferences: AppPreferences,
    appConfig: AppConfig
) : ViewModel() {
    var state by mutableStateOf(YouMp3UiState())
        private set

    val settingsViewModel = SettingsViewModel(
        appPreferences = appPreferences,
        appConfig = appConfig,
        onServerUrlCommitted = { onRecheckConnection() }
    )

    fun onFindButtonClick() {
        state = state.copy(
            currentScreen = YouMp3Screen.SongInput,
            findButtonPresses = state.findButtonPresses + 1
        )
    }

    fun onCloseSongInputClick() {
        state = state.copy(currentScreen = YouMp3Screen.Home)
    }

    fun onSettingsClick() {
        state = state.copy(currentScreen = YouMp3Screen.Settings)
    }

    fun onCloseSettingsClick() {
        state = state.copy(currentScreen = YouMp3Screen.Home)
    }

    /** Vuelve a la pantalla de inicio desde donde sea, sin tocar el estado de la otra VM. */
    fun onReturnToHome() {
        state = state.copy(currentScreen = YouMp3Screen.Home)
    }

    fun onRecheckConnection() {
        state = state.copy(connectionCheckToken = state.connectionCheckToken + 1)
    }

    fun onBackClick(): Boolean {
        return when (state.currentScreen) {
            YouMp3Screen.SongInput -> {
                onCloseSongInputClick()
                true
            }
            YouMp3Screen.Settings -> {
                onCloseSettingsClick()
                true
            }
            YouMp3Screen.Home -> false
        }
    }
}
