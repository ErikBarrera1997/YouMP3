package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.yoump3.AppBackgroundSignal
import com.dev.yoump3.appVersion
import com.dev.yoump3.consent.DataUsageConsent
import com.dev.yoump3.error.AppError
import com.dev.yoump3.error.AppErrorCatalog
import com.dev.yoump3.error.AppErrorKind
import com.dev.yoump3.init.ApiException
import com.dev.yoump3.init.YouMp3Api
import com.dev.yoump3.preferences.AppPreferences
import com.dev.yoump3.services.AudioPlayer
import com.dev.yoump3.services.AudioSaver
import com.dev.yoump3.services.ExtractedAudio
import com.dev.yoump3.services.ExtractionNotifier
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
    val serviceError: AppError? = null,
    // El aviso se muestra en la pantalla de búsqueda y bloquea el arranque de la petición.
    val showDataUsageConsent: Boolean = false,
    // Distingue "todavía no preguntado" de "rechazado", para explicar por qué sigue bloqueada.
    val dataUsageConsentDeclined: Boolean = false
)

class SongInputViewModel(
    private val api: YouMp3Api,
    private val audioSaver: AudioSaver,
    val audioPlayer: AudioPlayer,
    private val appPreferences: AppPreferences,
    private val extractionNotifier: ExtractionNotifier
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
        // `cancelInFlightRequests` ya la retira si había extracción; el stop explícito cubre el
        // caso de que el servicio haya quedado vivo sin job (por ejemplo, tras un fallo raro).
        extractionNotifier.stop()
        purgeExtractedAudio()
    }

    override fun onCleared() {
        dispose()
        super.onCleared()
    }

    /**
     * Suelta memoria al pasar a segundo plano, pero ya no cancela la extracción.
     *
     * La extracción en curso la sostiene un servicio en primer plano, así que el proceso no es
     * ajusticible mientras dura: cancelarla aquí era justo lo que devolvía al usuario a la lista de
     * resultados al volver. Cuando termina, el notificador publica el aviso si la app sigue en
     * segundo plano.
     *
     * Con la música en marcha no se toca nada: hay otro servicio en primer plano sosteniendo el
     * proceso a propósito. Si ya hay una extracción terminada y sin reproducir, se suelta para no
     * conservar el buffer decodificado mientras la app no se usa. La descarga se respeta aunque no
     * haya reproducción: está escribiendo ese mismo buffer a disco y limpiarlo a mitad corrompería
     * el archivo.
     */
    private fun onAppBackgrounded() {
        if (audioPlayer.state.isPlaying) return
        if (state.isExtracting) return
        if (state.isDownloading) return
        if (!state.hasExtraction) return

        purgeExtractedAudio()
        state = clearExtractionState()
    }

    var resultsScrollOffset: Int = 0

    private var searchJob: Job? = null
    private var extractionJob: Job? = null

    // Cada extracción tiene su generación. Sin esto, el `finally` de una extracción cancelada
    // podría cerrar la notificación de la extracción que el usuario acaba de iniciar.
    private var extractionGeneration = 0

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
        if (state.showDataUsageConsent) return

        val query = state.songQuery.trim()
        if (query.isEmpty()) return

        when (appPreferences.getDataUsageConsent()) {
            true -> startSearch(query)
            null -> state = state.copy(
                showDataUsageConsent = true,
                dataUsageConsentDeclined = false
            )
            false -> state = state.copy(
                showDataUsageConsent = true,
                dataUsageConsentDeclined = true
            )
        }
    }

    /**
     * El usuario concedió el permiso desde el aviso de la búsqueda. Se guarda para no volver a
     * preguntar y, si había una búsqueda pendiente, se ejecuta ahora.
     */
    fun onDataUsageConsentAccepted() {
        appPreferences.setDataUsageConsent(true)
        val query = state.songQuery.trim()
        state = state.copy(
            showDataUsageConsent = false,
            dataUsageConsentDeclined = false,
            errorMessage = null
        )
        if (query.isNotEmpty()) startSearch(query)
    }

    /**
     * El usuario rechazó el permiso. La decisión se persiste y la búsqueda queda bloqueada hasta
     * que lo habilite desde Ajustes o acepte el aviso en un nuevo intento.
     */
    fun onDataUsageConsentDeclined() {
        appPreferences.setDataUsageConsent(false)
        state = state.copy(
            showDataUsageConsent = false,
            dataUsageConsentDeclined = true,
            errorMessage = DataUsageConsent.BLOCKED_MESSAGE
        )
    }

    private fun startSearch(query: String) {
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
                val response = api.searchSongs(
                    videoName = query,
                    dataUsageConsent = appPreferences.getDataUsageConsent()
                )
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
        // Si venía sonando otra canción, se suelta antes de abrir el servicio de extracción: dos
        // notificaciones en primer plano a la vez solo confunden y no hay razón para mantener el
        // audio anterior mientras se extrae el nuevo.
        purgeExtractedAudio()
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
        val generation = ++extractionGeneration
        // El aviso se arranca antes de la petición para que el proceso ya esté sostenido cuando la
        // app pase a segundo plano. La acción de cancelar de la notificación reutiliza el mismo
        // camino que el botón de la pantalla, para que el estado no se quede a medias.
        extractionNotifier.start(title) { onCancelExtraction() }

        extractionJob = viewModelScope.launch {
            // El buffer decodificado todavía no es del ViewModel: solo pasa a serlo después de que
            // `load` lo acepte. `wipe` en el `finally` cubre ese hueco, para que un fallo de
            // reproducción no deje el MP3 en el heap hasta que pase el recolector.
            var audio: ExtractedAudio? = null
            var success = false
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
                success = true
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
                // Solo la extracción vigente cierra su notificación. Una cancelada ya la retiró
                // `cancelInFlightRequests`, y una reemplazada tiene otra generación.
                if (generation == extractionGeneration) {
                    extractionJob = null
                    extractionNotifier.finish(title, success)
                }
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
        val response = api.extractAudio(
            videoName = state.lastSearchQuery,
            videoId = videoId,
            dataUsageConsent = appPreferences.getDataUsageConsent()
        )
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

    /**
     * Decisión única del back dentro de la pantalla de búsqueda, para que el botón en pantalla y el
     * back del sistema no hagan cosas distintas.
     *
     * Devuelve `true` si consumió el back (canceló la extracción o volvió de la tarjeta a los
     * resultados) y `false` si el padre debe navegar a Home.
     */
    fun onBackFromSongInput(): Boolean = when {
        state.isExtracting -> {
            onCancelExtraction()
            true
        }
        state.resultTitle != null && state.searchResults.isNotEmpty() -> {
            onReturnToResults()
            true
        }
        else -> false
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
        val hadExtraction = extractionJob != null || state.isExtracting
        if (!hadExtraction && searchJob == null) return

        if (hadExtraction) {
            // Invalida el `finally` de la extracción cancelada para que no cierre la notificación de
            // una extracción posterior, y retira el aviso de progreso.
            extractionGeneration++
            extractionNotifier.stop()
        }

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
        errorMessage = null,
        showDataUsageConsent = false,
        dataUsageConsentDeclined = false
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
