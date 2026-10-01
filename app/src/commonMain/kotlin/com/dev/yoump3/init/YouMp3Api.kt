package com.dev.yoump3.init

import com.dev.yoump3.error.AppError
import com.dev.yoump3.error.AppErrorCatalog
import com.dev.yoump3.error.AppErrorKind
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class AudioExtractionRequest(val videoName: String, val videoId: String? = null)

@Serializable
data class AudioExtractionResponse(
    val success: Boolean,
    val message: String,
    val videoTitle: String? = null,
    val fileName: String? = null,
    val contentType: String? = null,
    val audioBase64: String? = null
)

@Serializable
data class AudioSearchRequest(val videoName: String)

/**
 * Extracción correcta. El tipo codifica la garantía: si este método devuelve, hay audio. Un
 * `success: true` sin `audioBase64` es una violación de contrato del backend y se convierte en
 * [AppErrorKind.UNEXPECTED_SERVER_ERROR], no en un resultado vacío que el ViewModel deba detectar.
 */
data class ExtractedAudioResponse(
    val videoTitle: String?,
    val fileName: String?,
    val contentType: String?,
    val audioBase64: String
)

@Serializable
data class AudioSearchResult(
    val videoId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val durationSeconds: Long? = null
)

@Serializable
data class AudioSearchResponse(
    val success: Boolean,
    val message: String,
    val results: List<AudioSearchResult> = emptyList()
)

/**
 * Unico tipo de excepcion que escapa de [YouMp3Api]. Ya viene resuelto a un [AppError], de modo
 * que ningun ViewModel necesita volver a interpretar codigos HTTP ni el mensaje del backend.
 */
class ApiException(val error: AppError) : Exception(error.describe())

class YouMp3Api(
    private val baseUrl: () -> String
) {
    private val lenientJson = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 600000
            connectTimeoutMillis = 90000
            socketTimeoutMillis = 600000
        }
    }

    /**
     * Cliente separado para el sondeo. No puede compartir timeouts con el de las búsquedas: una
     * extracción puede tardar 180 s legítimamente, pero la pantalla de arranque no debe esperar
     * ni un minuto para saber que no hay nadie escuchando.
     *
     * El `lazy` se guarda aparte para poder distinguir "todavía no se ha construido" de "vale
     * cero": leerlo en [close] crearía un cliente entero para cerrar nada.
     *
     * "no hay nadie escuchando" es cierto para un servidor caído, pero no para una instancia en
     * reposo: en Render el servicio se duerme tras ~15 min de inactividad y al primer request la
     * plataforma tarda 30-60 s en levantarlo. Durante ese rato la pasarela acepta la conexión y
     * no contesta, así que [PROBE_TIMEOUT_MS] se agota antes de tiempo. El síntoma es que el
     * arranque necesita ~3 intentos, y es un coste aceptado a cambio de no dejar la pantalla
     * bloqueada un minuto ante un servidor que sí está caído de verdad.
     *
     * Si algún día se quiere arreglar de raiz, el arreglo es de infraestructura (una instancia sin
     * suspensión, o un ping que la despierte), no subir este timeout.
     */
    private val probeClientDelegate = lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = PROBE_TIMEOUT_MS
                connectTimeoutMillis = PROBE_CONNECT_TIMEOUT_MS
                socketTimeoutMillis = PROBE_TIMEOUT_MS
            }
        }
    }

    private val probeClient by probeClientDelegate

    /**
     * Cierra los dos clientes. Cada `HttpClient` de Ktor arrastra un scope de corrutinas y los hilos
     * de su motor, y no los suelta el recolector por mucho que quede huérfano: sin esto, abrir y
     * cerrar la app en el mismo proceso los deja acumulados.
     */
    fun close() {
        client.close()
        if (probeClientDelegate.isInitialized()) probeClient.close()
    }

    private fun endpoint(path: String): String = baseUrl().trimEnd('/') + path

    /**
     * Decide si se puede entrar en la app. Solo devuelve success si el backend responde con el
     * rechazo esperado del sondeo, es decir, si está realmente sirviendo.
     *
     * La API no expone endpoint raíz ni health check, así que se sondea
     * `POST /api/audios/search` con un body vacío: el backend rechaza la petición con 400
     * `Enter the name of a video to search on YouTube.` de inmediato, sin lanzar yt-dlp. Cualquier
     * otra respuesta (500 genérico, 503, 404 por URL mal configurada, o el body por defecto de
     * Spring) significa que el servicio no sirve, y se propaga como [ApiException] para que la
     * pantalla inicial muestre el error en lugar de avanzar.
     */
    suspend fun checkConnection(): Result<Unit> = try {
        val response = probeClient.post(endpoint("/api/audios/search")) {
            contentType(ContentType.Application.Json)
            setBody(EMPTY_PROBE_BODY)
        }
        val message = response.readMessage()
        val kind = AppErrorCatalog.resolve(response.status.value, message)
        if (kind == AppErrorKind.VIDEO_NAME_MISSING) {
            Result.success(Unit)
        } else {
            Result.failure(
                ApiException(AppError(kind, response.status.value, message))
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(ApiException(AppErrorCatalog.resolve(e)))
    }

    suspend fun searchSongs(videoName: String): AudioSearchResponse {
        val response = client.post(endpoint("/api/audios/search")) {
            contentType(ContentType.Application.Json)
            setBody(AudioSearchRequest(videoName))
        }
        response.requireSuccess()
        val parsed = response.body<AudioSearchResponse>()
        ensureSuccessFlag(parsed.success, parsed.message, response.status.value)
        return parsed
    }

    suspend fun extractAudio(videoName: String, videoId: String? = null): ExtractedAudioResponse {
        val response = client.post(endpoint("/api/audios/extract")) {
            contentType(ContentType.Application.Json)
            setBody(AudioExtractionRequest(videoName, videoId))
        }
        response.requireSuccess()
        val parsed = response.body<AudioExtractionResponse>()
        ensureSuccessFlag(parsed.success, parsed.message, response.status.value)
        val audio = parsed.audioBase64?.takeIf { it.isNotBlank() }
            ?: throw ApiException(
                AppError(
                    kind = AppErrorKind.UNEXPECTED_SERVER_ERROR,
                    statusCode = response.status.value,
                    backendMessage = parsed.message
                )
            )
        requireAudioWithinBudget(audio, response.status.value, parsed.message)
        return ExtractedAudioResponse(
            videoTitle = parsed.videoTitle,
            fileName = parsed.fileName,
            contentType = parsed.contentType,
            audioBase64 = audio
        )
    }

    /**
     * Corta la respuesta **antes** de decodificarla.
     *
     * El audio viaja dentro del cuerpo JSON como cadena de base64, y el pico real es de varias
     * veces el tamaño del MP3: una cadena con el cuerpo entero, otra con el campo ya deserializado
     * y el `ByteArray` decodificado, todas vivas a la vez. Comprobar el tamaño después de decodificar
     * sería tarde, porque el heap ya está comprometido y el `OutOfMemoryError` no es una excepción
     * que este código pueda recuperar.
     *
     * A 4 caracteres de base64 por cada 3 bytes, la longitud de la cadena ya dice cuántos bytes
     * va a pedir el `ByteArray`, sin reservar nada.
     */
    private fun requireAudioWithinBudget(base64: String, statusCode: Int, backendMessage: String?) {
        val decodedBytes = base64.length / 4L * 3L
        if (decodedBytes <= MAX_DECODED_AUDIO_BYTES) return
        throw ApiException(
            AppError(
                kind = AppErrorKind.AUDIO_TOO_LARGE,
                statusCode = statusCode,
                backendMessage = backendMessage
            )
        )
    }

    private suspend fun HttpResponse.requireSuccess() {
        if (status.isSuccess()) return
        throw ApiException(toAppError())
    }

    /**
     * Traduce cualquier respuesta fallida a un [AppError] resuelto. El cuerpo se lee como texto y se
     * parsea a mano porque puede no ser del backend: HTML de Cloudflare, el
     * `{timestamp,status,error}` por defecto de Spring, o vacío.
     */
    private suspend fun HttpResponse.toAppError(): AppError {
        val message = readMessage()
        return AppError(
            kind = AppErrorCatalog.resolve(status.value, message),
            statusCode = status.value,
            backendMessage = message
        )
    }

    /**
     * El cuerpo se lee como texto y se parsea a mano porque puede no ser del backend: HTML de
     * Cloudflare, el `{timestamp,status,error}` por defecto de Spring, o vacío.
     */
    private suspend fun HttpResponse.readMessage(): String? {
        val raw = runCatching { bodyAsText() }.getOrNull()
        return runCatching {
            val json = lenientJson.parseToJsonElement(raw.orEmpty()) as? JsonObject
            (json?.get("message") as? JsonPrimitive)?.takeIf { it.isString }?.content
        }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** El backend puede responder 200 con `success: false`; también es un fallo, no un resultado. */
    private fun ensureSuccessFlag(success: Boolean, message: String, statusCode: Int) {
        if (success) return
        throw ApiException(
            AppError(
                kind = AppErrorCatalog.resolve(statusCode, message),
                statusCode = statusCode,
                backendMessage = message
            )
        )
    }

    private companion object {
        val EMPTY_PROBE_BODY = """{"videoName":""}"""
        const val PROBE_CONNECT_TIMEOUT_MS = 8_000L
        const val PROBE_TIMEOUT_MS = 20_000L

        /**
         * Tope del audio decodificado, en bytes.
         *
         * Una canción normal ocupa alrededor de 19 MB contando el base64 de la respuesta, así que
         * el audio decodificado ronda los 14 MB. El tope va a 32 MB para que ni una canción normal
         * ni una duración larga en un bitrate alto se queden fuera: 32 MB son ~33 min a 128 kbps o
         * ~13 min a 320 kbps.
         *
         * Ojo al precio: el pico transitorio de esa extracción es de ~117 MB (32 MB de `ByteArray`
         * más dos cadenas de 42,7 M caracteres, a 1 byte por carácter con las cadenas compactas de
         * ART) y de ~203 MB si el runtime los ensancha a UTF-16. Ese pico no lo baja este cliente:
         * viene de que el audio viaje dentro del JSON. La única forma de eliminarlo es que el
         * backend sirva el MP3 como binario, porque entonces se puede escribir en `cacheDir` sin
         * pasar por el heap. Para el dispositivo, ese endpoint es lo que hay que hacer antes que
         * subir este número.
         */
        const val MAX_DECODED_AUDIO_BYTES = 32L * 1024 * 1024
    }
}
