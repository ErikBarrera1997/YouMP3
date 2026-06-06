package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class YouMp3UiState(
    val appTitle: String = "YOUMP3",
    val title: String = "FIND IT!",
    val footer: String = "BY MSSERVICES",
    val findButtonPresses: Int = 0
)

class YouMp3ViewModel {
    var state by mutableStateOf(YouMp3UiState())
        private set

    fun onFindButtonClick() {
        state = state.copy(findButtonPresses = state.findButtonPresses + 1)
    }
}
