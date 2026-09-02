package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.yoump3.appVersion
import com.dev.yoump3.init.ApiException
import com.dev.yoump3.init.YouMp3Api
import com.dev.yoump3.services.AudioSaver
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
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

class SongInputViewModel(
    private val api: YouMp3Api,
    private val audioSaver: AudioSaver
) : ViewModel() {
    var state by mutableStateOf(SongInputUiState())
        private set

    private var searchJob: Job? = null
    private var extractionJob: Job? = null

    fun onSongQueryChange(value: String) {
        val sanitized = sanitizeSongQuery(value)
        state = state.copy(
            songQuery = sanitized,
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

    private fun sanitizeSongQuery(query: String): String {
        val allowedPunctuation = setOf('-', '\'', '.', '&', '(', ')', ',', '!', '?', '"', '_', ':')
        return buildString {
            for (char in query) {
                val code = char.code
                when {
                    char.isLetterOrDigit() -> append(char)
                    char.isWhitespace() -> append(char)
                    allowedPunctuation.contains(char) -> append(char)
                    code < 32 || code == 127 -> Unit
                }
            }
        }
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

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val response = api.searchSongs(query)
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (isActive) {
                    state = state.copy(
                        isLoading = false,
                        errorMessage = e.apiMessage
                    )
                }
            } catch (e: Exception) {
                if (isActive) {
                    state = state.copy(
                        isLoading = false,
                        errorMessage = "Service unavailable. Try again later."
                    )
                }
            } finally {
                searchJob = null
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

        extractionJob = viewModelScope.launch {
            try {
                val response = api.extractAudio(videoName = state.lastSearchQuery, videoId = videoId)
                if (response.success && response.audioBase64 != null) {
                    val resolvedTitle = response.videoTitle ?: title
                    val resolvedFormat = resolveFormat(response.contentType, response.fileName)
                    val audioBase64 = response.audioBase64

                    state = state.copy(
                        isExtracting = false,
                        resultTitle = resolvedTitle,
                        resultFormat = resolvedFormat,
                        resultAudioBase64 = audioBase64
                    )
                } else {
                    state = state.copy(
                        isExtracting = false,
                        isExtractionFailed = true,
                        errorMessage = response.message
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (isActive) {
                    state = state.copy(
                        isExtracting = false,
                        isExtractionFailed = true,
                        errorMessage = e.apiMessage
                    )
                }
            } catch (e: Exception) {
                if (isActive) {
                    state = state.copy(
                        isExtracting = false,
                        isExtractionFailed = true,
                        errorMessage = "Service unavailable. Try again later."
                    )
                }
            } finally {
                extractionJob = null
            }
        }
    }

    fun onCancelExtraction() {
        cancelInFlightRequests()
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

    private fun cancelInFlightRequests() {
        if (!state.isExtracting && extractionJob == null && searchJob == null) return
        searchJob?.cancel()
        searchJob = null
        extractionJob?.cancel()
        extractionJob = null
    }

    fun onDownloadClick() {
        val base64 = state.resultAudioBase64 ?: return
        val fileName = "${sanitizeFileName(state.resultTitle ?: "audio")}.mp3"

        state = state.copy(
            isDownloading = true,
            isDownloadFailed = false,
            downloadStatus = null,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val path = audioSaver.save(fileName, "audio/mpeg", base64)
                state = state.copy(
                    isDownloading = false,
                    downloadStatus = "Descargado en: $path",
                    resultAudioBase64 = null
                )
            } catch (e: CancellationException) {
                throw e
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
        cancelInFlightRequests()
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
        cancelInFlightRequests()
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
