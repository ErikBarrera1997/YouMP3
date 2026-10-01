package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.yoump3.AppBackgroundSignal
import com.dev.yoump3.appVersion
import com.dev.yoump3.error.AppError
import com.dev.yoump3.error.AppErrorCatalog
import com.dev.yoump3.error.AppErrorKind
import com.dev.yoump3.init.ApiException
import com.dev.yoump3.init.YouMp3Api
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
    val selectedVideoId: String? = null,
    val resultTitle: String? = null,
    val resultFormat: String? = null,
    val resultSizeBytes: Long? = null,
    val hasExtraction: Boolean = false,
    val isDownloading: Boolean = false,
    val isDownloadFailed: Boolean = false,
    val downloadStatus: String? = null,
    val errorMessage: String? = null,
    val serviceError: AppError? = null
)

class SongInputViewModel(
    private val api: YouMp3Api,
    private val audioSaver: AudioSaver,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    /**
     * Extracción terminada con sus metadatos. Existe para que [extractAndDecode] pueda devolver
     * todo de golpe y dejar de vivir en su marco la respuesta del backend, que lleva la cadena de
     * base64.
     */
    private class ExtractionOutcome(
        val audio: ExtractedAudio,
        val title: String,
        val format: String
    )

    var state by mutableStateOf(SongInputUiState())
        private set

    private val backgroundListener = AppBackgroundSignal.Listener { onAppBackgrounded() }

    init {
        AppBackgroundSignal.add(backgroundListener)
    }

    /**
     * Suelta todo lo que este ViewModel tiene abierto.
     *
     * Es público, y no un simple `override` de `onCleared`, porque este ViewModel se construye a
     * mano dentro de [YouMp3ViewModel] y por tanto no entra en ningún `ViewModelStore`: el framework
     * nunca le llamaría `onCleared`, así que sin esta puerta explícita ni el audio se purga ni la
     * suscripción a [AppBackgroundSignal] se retira al cerrar la app.
     */
    fun dispose() {
        AppBackgroundSignal.remove(backgroundListener)
        cancelInFlightRequests()
        purgeExtractedAudio()
    }

    override fun onCleared() {
        dispose()
        super.onCleared()
    }

    /**
     * Suelta la extracción al salir de la app, que es justo cuando el proceso pasa a ser ajusticible.
     *
     * Con la música en marcha no se toca nada: hay un servicio en primer plano sosteniendo el proceso
     * a propósito, y ni la reproducción ni una extracción que va a terminar en unos segundos son
     * memoria que sobre. Liberar ahí sería cortarle al usuario la canción que se fue a escuchar.
     *
     * Una extracción en curso es el peor caso: sostiene a la vez la cadena de base64 y el buffer
     * decodificado. Se corta en lugar de dejarla rematar sola en un proceso que el sistema ya puede
     * matar, y el usuario vuelve a los resultados para reintentarla si quiere. Si ya había audio
     * cargado, también se suelta, porque la tarjeta y su botón de descarga leen de ese mismo buffer.
     */
    private fun onAppBackgrounded() {
        if (audioPlayer.state.isPlaying) return

        if (state.isExtracting) {
            cancelInFlightRequests()
            purgeExtractedAudio()
            state = clearExtractionState()
            return
        }

        if (!state.hasExtraction) return
        purgeExtractedAudio()
        state = clearExtractionState()
    }

    var resultsScrollOffset: Int = 0

    private var searchJob: Job? = null
    private var extractionJob: Job? = null

    private var extractedAudio: ExtractedAudio? = null

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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isActive) {
                    val error = toAppError(e)
                    if (error.blocksScreen) {
                        raiseServiceUnavailable(error)
                    } else {
                        state = state.copy(
                            isLoading = false,
                            errorMessage = error.detail
                        )
                    }
                }
            } finally {
                searchJob = null
            }
        }
    }

    fun onSelectResult(videoId: String, title: String) {
        state = clearExtractionState().copy(
            isExtracting = true,
            selectedTitle = title,
            selectedVideoId = videoId
        )
        runExtraction(videoId, title)
    }

    /**
     * Reintenta la extracción del mismo video, sin volver a buscar. El fallo estaba en la
     * extracción, así que repetir la búsqueda solo gastaría una llamada que ya funcionó.
     */
    fun onRetryExtraction() {
        val videoId = state.selectedVideoId
        val title = state.selectedTitle
        if (videoId.isNullOrBlank() || title == null) {
            // Sin selección guardada no hay nada que reintentar: la búsqueda es el único camino.
            onRetrySearch()
            return
        }
        state = state.copy(
            isExtracting = true,
            isExtractionFailed = false,
            errorMessage = null
        )
        runExtraction(videoId, title)
    }

    private fun runExtraction(videoId: String, title: String) {
        extractionJob = viewModelScope.launch {
            // El buffer decodificado todavía no es del ViewModel: solo pasa a serlo después de que
            // `load` lo acepte. `wipe` en el `finally` cubre ese hueco, para que un fallo de
            // reproducción no deje el MP3 en el heap hasta que pase el recolector.
            var audio: ExtractedAudio? = null
            try {
                val outcome = extractAndDecode(videoId, title)
                audio = outcome.audio

                purgeExtractedAudio()
                audioPlayer.load(outcome.audio, outcome.title)
                extractedAudio = outcome.audio
                audio = null
                state = state.copy(
                    isExtracting = false,
                    resultTitle = outcome.title,
                    resultFormat = outcome.format,
                    resultSizeBytes = outcome.audio.sizeBytes,
                    hasExtraction = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isActive) {
                    val error = toAppError(e)
                    if (error.blocksScreen) {
                        raiseServiceUnavailable(error)
                    } else {
                        state = state.copy(
                            isExtracting = false,
                            isExtractionFailed = true,
                            errorMessage = error.detail
                        )
                    }
                }
            } finally {
                audio?.wipe()
                extractionJob = null
            }
        }
    }

    /**
     * Extrae y decodifica en un ámbito propio para que la cadena de base64 quede fuera de alcance en
     * cuanto existe el `ByteArray`. Devolverla desde aquí es lo que permite que el recolector la
     * libere antes de que `load` reserve el archivo temporal y el `MediaPlayer`: si la respuesta
     * viviera en el ámbito de [runExtraction], sus varias veces el tamaño del audio se sumarían al
     * pico del `load` en lugar de solaparse solo con la decodificación.
     */
    private suspend fun extractAndDecode(videoId: String, title: String): ExtractionOutcome {
        val response = api.extractAudio(videoName = state.lastSearchQuery, videoId = videoId)
        return ExtractionOutcome(
            audio = ExtractedAudio(decodeBase64(response.audioBase64)),
            title = response.videoTitle ?: title,
            format = resolveFormat(response.contentType, response.fileName)
        )
    }

    fun onCancelExtraction() {
        cancelInFlightRequests()
        purgeExtractedAudio()
        state = clearExtractionState()
    }

    fun onServiceErrorDismissed() {
        cancelInFlightRequests()
        purgeExtractedAudio()
        state = clearExtractionState().copy(
            serviceError = null
        )
    }

    private fun raiseServiceUnavailable(error: AppError) {
        purgeExtractedAudio()
        state = SongInputUiState(
            searchResults = emptyList(),
            serviceError = error
        )
    }

    /** Toda excepcion se traduce aqui a un [AppError] ya resuelto por el catalogo. */
    private fun toAppError(e: Throwable): AppError = when (e) {
        is ApiException -> e.error
        else -> AppErrorCatalog.resolve(e)
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
        // `cancelInFlightRequests()` puede matar un `searchJob`, y un `CancellationException` sale
        // por encima sin pasar por los `catch` que bajan `isLoading`. Sin esto, cancelar una búsqueda
        // dejaba el spinner girando sobre una pantalla que ya no va a recibir respuesta.
        isLoading = false,
        isExtractionFailed = false,
        isDownloading = false,
        isDownloadFailed = false,
        hasExtraction = false,
        selectedTitle = null,
        selectedVideoId = null,
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
                    errorMessage = AppErrorKind.SAVE_FAILED.detail
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
