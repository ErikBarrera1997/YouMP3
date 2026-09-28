package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.yoump3.appVersion
import com.dev.yoump3.init.ApiException
import com.dev.yoump3.init.YouMp3Api
import com.dev.yoump3.init.networkErrorMessage
import com.dev.yoump3.services.AudioPlayer
import com.dev.yoump3.services.AudioSaver
import com.dev.yoump3.services.ExtractedAudio
import com.dev.yoump3.services.decodeBase64
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
    val resultSizeBytes: Long? = null,
    val hasExtraction: Boolean = false,
    val isDownloading: Boolean = false,
    val isDownloadFailed: Boolean = false,
    val downloadStatus: String? = null,
    val errorMessage: String? = null
)

class SongInputViewModel(
    private val api: YouMp3Api,
    private val audioSaver: AudioSaver,
    val audioPlayer: AudioPlayer
) : ViewModel() {
    var state by mutableStateOf(SongInputUiState())
        private set

    var resultsScrollOffset: Int = 0

    private var searchJob: Job? = null
    private var extractionJob: Job? = null

    private var extractedAudio: ExtractedAudio? = null

    override fun onCleared() {
        purgeExtractedAudio()
        super.onCleared()
    }

    fun onSongQueryChange(value: String) {
        val sanitized = sanitizeSongQuery(value)
        audioPlayer.stop()
        purgeExtractedAudio()
        state = clearExtractionState().copy(songQuery = sanitized, searchResults = emptyList())
    }

    private fun sanitizeSongQuery(query: String): String {
        return query.filter { char ->
            char.isLetter() || char.isDigit() || char == ' ' || char == '-' || char == ','
        }
    }

    fun onSearchClick() {
        val query = state.songQuery.trim()
        if (query.isEmpty()) return

        audioPlayer.stop()
        purgeExtractedAudio()
        state = clearExtractionState().copy(
            lastSearchQuery = query,
            searchRequests = state.searchRequests + 1,
            isLoading = true,
            searchResults = emptyList()
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
                        errorMessage = networkErrorMessage(e) ?: "Service unavailable. Try again later."
                    )
                }
            } finally {
                searchJob = null
            }
        }
    }

    fun onSelectResult(videoId: String, title: String) {
        state = clearExtractionState().copy(
            isExtracting = true,
            selectedTitle = title
        )

        extractionJob = viewModelScope.launch {
            try {
                val response = api.extractAudio(videoName = state.lastSearchQuery, videoId = videoId)
                if (response.success && response.audioBase64 != null) {
                    val resolvedTitle = response.videoTitle ?: title
                    val resolvedFormat = resolveFormat(response.contentType, response.fileName)
                    val audio = ExtractedAudio(decodeBase64(response.audioBase64))

                    purgeExtractedAudio()
                    audioPlayer.load(audio, resolvedTitle)
                    extractedAudio = audio
                    state = state.copy(
                        isExtracting = false,
                        resultTitle = resolvedTitle,
                        resultFormat = resolvedFormat,
                        resultSizeBytes = audio.sizeBytes,
                        hasExtraction = true
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
                        errorMessage = networkErrorMessage(e) ?: "Service unavailable. Try again later."
                    )
                }
            } finally {
                extractionJob = null
            }
        }
    }

    fun onCancelExtraction() {
        cancelInFlightRequests()
        purgeExtractedAudio()
        state = clearExtractionState()
    }

    private fun cancelInFlightRequests() {
        if (!state.isExtracting && extractionJob == null && searchJob == null) return
        searchJob?.cancel()
        searchJob = null
        extractionJob?.cancel()
        extractionJob = null
    }

    private fun purgeExtractedAudio() {
        audioPlayer.release()
        extractedAudio?.wipe()
        extractedAudio = null
    }

    private fun clearExtractionState() = state.copy(
        isExtracting = false,
        isExtractionFailed = false,
        isDownloading = false,
        isDownloadFailed = false,
        hasExtraction = false,
        selectedTitle = null,
        resultTitle = null,
        resultFormat = null,
        resultSizeBytes = null,
        downloadStatus = null,
        errorMessage = null
    )

    fun onDownloadClick() {
        val audio = extractedAudio ?: return
        val fileName = "${sanitizeFileName(state.resultTitle ?: "audio")}.mp3"

        state = state.copy(
            isDownloading = true,
            isDownloadFailed = false,
            downloadStatus = null,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val path = audioSaver.save(fileName, "audio/mpeg", audio.bytes)
                audioPlayer.stop()
                purgeExtractedAudio()
                state = state.copy(
                    isDownloading = false,
                    hasExtraction = false,
                    downloadStatus = "Descargado en: $path"
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

    fun onRetrySearch() {
        val query = state.lastSearchQuery.ifBlank { state.songQuery }
        if (query.isBlank()) {
            onReturnToInput()
            return
        }
        state = state.copy(
            songQuery = query,
            isExtracting = false,
            isExtractionFailed = false
        )
        onSearchClick()
    }

    fun onReturnToResults() {
        cancelInFlightRequests()
        audioPlayer.stop()
        purgeExtractedAudio()
        state = clearExtractionState()
    }

    fun onReturnToInput() {
        cancelInFlightRequests()
        audioPlayer.stop()
        purgeExtractedAudio()
        state = clearExtractionState()
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
