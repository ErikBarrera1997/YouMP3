package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dev.yoump3.appVersion

enum class YouMp3Screen {
    Home,
    SongInput
}

data class YouMp3UiState(
    val appTitle: String = "YOUMP3",
    val title: String = "FIND IT!",
    val footer: String = "BY CLEVER CLOUD · v$appVersion",
    val currentScreen: YouMp3Screen = YouMp3Screen.Home,
    val findButtonPresses: Int = 0
)

class YouMp3ViewModel {
    var state by mutableStateOf(YouMp3UiState())
        private set

    fun onFindButtonClick() {
        state = state.copy(
            currentScreen = YouMp3Screen.SongInput,
            findButtonPresses = state.findButtonPresses + 1
        )
    }

    fun onCloseSongInputClick() {
        state = state.copy(currentScreen = YouMp3Screen.Home)
    }

    fun onBackClick(): Boolean {
        return if (state.currentScreen == YouMp3Screen.SongInput) {
            onCloseSongInputClick()
            true
        } else {
            false
        }
    }
}
