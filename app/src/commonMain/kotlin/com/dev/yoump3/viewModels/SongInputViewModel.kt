package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class SongInputUiState(
    val appTitle: String = "YOUMP3",
    val placeholder: String = "TIPE THE SONG",
    val footer: String = "BY MSSERVICES"
)

class SongInputViewModel {
    var state by mutableStateOf(SongInputUiState())
        private set
}
