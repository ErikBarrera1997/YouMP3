package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dev.yoump3.init.YouMp3Api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class SongInputUiState(
    val appTitle: String = "YOUMP3",
    val placeholder: String = "TYPE THE SONG",
    val footer: String = "BY MSSERVICES",
    val songQuery: String = "",
    val lastSearchQuery: String = "",
    val searchRequests: Int = 0,
    val isLoading: Boolean = false,
    val resultMessage: String? = null,
    val errorMessage: String? = null
)

class SongInputViewModel {
    var state by mutableStateOf(SongInputUiState())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onSongQueryChange(value: String) {
        state = state.copy(songQuery = value, resultMessage = null, errorMessage = null)
    }

    fun onSearchClick() {
        val query = state.songQuery.trim()
        if (query.isEmpty()) return

        state = state.copy(
            lastSearchQuery = query,
            searchRequests = state.searchRequests + 1,
            isLoading = true,
            resultMessage = null,
            errorMessage = null
        )

        scope.launch {
            try {
                val response = YouMp3Api.extractAudio(query)
                state = state.copy(
                    isLoading = false,
                    resultMessage = if (response.success) {
                        "✓ ${response.videoTitle ?: response.message}"
                    } else {
                        "✗ ${response.message}"
                    }
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message ?: "Could not connect to server"}"
                )
            }
        }
    }
}
