package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dev.yoump3.appVersion
import com.dev.yoump3.init.ApiException
import com.dev.yoump3.init.YouMp3Api
import com.dev.yoump3.services.saveAudioToDownloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class SearchResultUi(
    val videoId: String,
    val title: String,
    val author: String,
    val durationSeconds: Long?
)

data class SongInputUiState(
    val appTitle: String = "YOUMP3",
    val placeholder: String = "Search for song, artist....",
    val footer: String = "BY CLEVER CLOUD · v$appVersion",
    val songQuery: String = "",
    val lastSearchQuery: String = "",
    val searchRequests: Int = 0,
    val isLoading: Boolean = false,
    val searchResults: List<SearchResultUi> = emptyList(),
    val isExtracting: Boolean = false,
    val isExtractionFailed: Boolean = false,
    val selectedTitle: String? = null,
    val resultTitle: String? = null,
    val resultFormat: String? = null,
    val resultAudioBase64: String? = null,
    val isDownloading: Boolean = false,
    val isDownloadFailed: Boolean = false,
    val downloadStatus: String? = null,
    val errorMessage: String? = null
)

class SongInputViewModel {
    var state by mutableStateOf(SongInputUiState())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onSongQueryChange(value: String) {
        state = state.copy(
            songQuery = value,
            searchResults = emptyList(),
            resultTitle = null,
            resultFormat = null,
            resultAudioBase64 = null,
            isExtractionFailed = false,
            isDownloadFailed = false,
            downloadStatus = null,
            errorMessage = null
        )
    }

    fun onSearchClick() {
        val query = state.songQuery.trim()
        if (query.isEmpty()) return

        state = state.copy(
            lastSearchQuery = query,
            searchRequests = state.searchRequests + 1,
            isLoading = true,
            searchResults = emptyList(),
            selectedTitle = null,
            resultTitle = null,
            resultFormat = null,
            resultAudioBase64 = null,
            isExtractionFailed = false,
            isDownloadFailed = false,
            downloadStatus = null,
            errorMessage = null
        )

        scope.launch {
            try {
                val response = YouMp3Api.searchSongs(query)
                if (response.success) {
                    state = state.copy(
                        isLoading = false,
                        searchResults = response.results.mapNotNull { result ->
                            val videoId = result.videoId
                            val title = result.title
                            if (videoId.isNullOrBlank() || title.isNullOrBlank()) null
                            else SearchResultUi(
                                videoId = videoId,
                                title = title,
                                author = result.author ?: "Desconocido",
                                durationSeconds = result.durationSeconds
                            )
                        }
                    )
                } else {
                    state = state.copy(
                        isLoading = false,
                        errorMessage = response.message
                    )
                }
            } catch (e: ApiException) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = e.apiMessage
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    errorMessage = "Service unavailable. Try again later."
                )
            }
        }
    }

    fun onSelectResult(videoId: String, title: String) {
        state = state.copy(
            isExtracting = true,
            isExtractionFailed = false,
            selectedTitle = title,
            resultTitle = null,
            resultFormat = null,
            resultAudioBase64 = null,
            isDownloadFailed = false,
            downloadStatus = null,
            errorMessage = null
        )

        scope.launch {
            try {
                val response = YouMp3Api.extractAudio(videoName = state.lastSearchQuery, videoId = videoId)
                if (response.success) {
                    state = state.copy(
                        isExtracting = false,
                        resultTitle = response.videoTitle ?: title,
                        resultFormat = resolveFormat(response.contentType, response.fileName),
                        resultAudioBase64 = response.audioBase64
                    )
                } else {
                    state = state.copy(
                        isExtracting = false,
                        isExtractionFailed = true,
                        errorMessage = response.message
                    )
                }
            } catch (e: ApiException) {
                state = state.copy(
                    isExtracting = false,
                    isExtractionFailed = true,
                    errorMessage = e.apiMessage
                )
            } catch (e: Exception) {
                state = state.copy(
                    isExtracting = false,
                    isExtractionFailed = true,
                    errorMessage = "Service unavailable. Try again later."
                )
            }
        }
    }

    fun onDownloadClick() {
        val base64 = state.resultAudioBase64 ?: return

        state = state.copy(
            isDownloading = true,
            isDownloadFailed = false,
            downloadStatus = null,
            errorMessage = null
        )

        scope.launch {
            try {
                val fileName = "${sanitizeFileName(state.resultTitle ?: "audio")}.mp3"
                val path = saveAudioToDownloads(fileName, "audio/mpeg", base64)
                state = state.copy(
                    isDownloading = false,
                    downloadStatus = "Descargado en: $path"
                )
            } catch (e: Exception) {
                state = state.copy(
                    isDownloading = false,
                    isDownloadFailed = true,
                    errorMessage = "No se pudo guardar el archivo."
                )
            }
        }
    }

    fun onReturnToResults() {
        state = state.copy(
            isExtracting = false,
            isExtractionFailed = false,
            isDownloading = false,
            isDownloadFailed = false,
            selectedTitle = null,
            resultTitle = null,
            resultFormat = null,
            resultAudioBase64 = null,
            downloadStatus = null,
            errorMessage = null
        )
    }

    fun onReturnToInput() {
        state = state.copy(
            isExtracting = false,
            isExtractionFailed = false,
            isDownloading = false,
            isDownloadFailed = false,
            selectedTitle = null,
            resultTitle = null,
            resultFormat = null,
            resultAudioBase64 = null,
            downloadStatus = null,
            errorMessage = null
        )
    }

    private fun resolveFormat(contentType: String?, fileName: String?): String {
        val extension = fileName?.substringAfterLast('.', "")?.trim()?.uppercase()
        if (!extension.isNullOrBlank() && extension.length in 2..5) return extension

        return when {
            contentType == null -> "MP3"
            contentType.contains("mpeg") -> "MP3"
            contentType.contains("mp4") -> "M4A"
            contentType.contains("webm") -> "WEBM"
            contentType.contains("ogg") -> "OGG"
            contentType.contains("wav") -> "WAV"
            else -> contentType.substringAfter('/').uppercase()
        }
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.trim()
            .replace(Regex("""[\\/:*?"<>|]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return cleaned.ifEmpty { "audio" }.take(80)
    }
}
