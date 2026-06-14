package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class SongInputUiState(
    val appTitle: String = "YOUMP3",
    val placeholder: String = "TYPE THE SONG",
    val footer: String = "BY MSSERVICES",
    val songQuery: String = "",
    val lastSearchQuery: String = "",
    val searchRequests: Int = 0
)

class SongInputViewModel {
    var state by mutableStateOf(SongInputUiState())
        private set

    fun onSongQueryChange(value: String) {
        state = state.copy(songQuery = value)
    }

    fun onSearchClick() {
        state = state.copy(
            lastSearchQuery = state.songQuery.trim(),
            searchRequests = state.searchRequests + 1
        )
    }
}
