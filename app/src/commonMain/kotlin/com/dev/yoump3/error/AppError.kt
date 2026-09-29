package com.dev.yoump3.error

import androidx.compose.runtime.saveable.Saver
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

enum class AppErrorSeverity {
    INLINE,

    FULL_SCREEN
}

/**
 * Cada entrada corresponde a un error que el backend puede emitir, o a un fallo local de la app.
 * [backendMessage] es el literal exacto que se espera en el campo `message` de la respuesta; sirve
 * como contrato verificable contra el backend (`GlobalExceptionHandler` y
 * `YtDlpYoutubeAudioRepository` del proyecto YOUMP3API).
 */
enum class AppErrorKind(
    val httpStatus: Int? = null,
    val backendMessage: String? = null,
    val retryable: Boolean = true,
    val severity: AppErrorSeverity = AppErrorSeverity.INLINE,
    val title: String = "SERVICIO NO DISPONIBLE",
    val detail: String
) {
    // --- 400: petición incorrecta, reintentar no cambia nada ---
    VIDEO_NAME_MISSING(
        httpStatus = 400,
        backendMessage = "Enter the name of a video to search on YouTube.",
        retryable = false,
        detail = "Escribe el nombre de una canción para buscar en YouTube."
    ),
    QUERY_TOO_SHORT(
        httpStatus = 400,
        backendMessage = "The search is too short. Enter at least 3 characters.",
        retryable = false,
        detail = "La búsqueda es muy corta. Escribe al menos 3 caracteres."
    ),
    QUERY_TOO_LONG(
        httpStatus = 400,
        backendMessage = "The search is too long. Try a shorter name.",
        retryable = false,
        detail = "La búsqueda es demasiado larga. Prueba con un nombre más corto."
    ),
    NO_RESULTS(
        httpStatus = 400,
        backendMessage = "We could not find videos with that name. Try a more specific search.",
        retryable = false,
        detail = "No encontramos vídeos con ese nombre. Prueba una búsqueda más específica."
    ),
    MALFORMED_REQUEST(
        httpStatus = 400,
        backendMessage = "Malformed request body.",
        retryable = false,
        severity = AppErrorSeverity.FULL_SCREEN,
        title = "PETICIÓN NO VÁLIDA",
        detail = "El servicio no pudo leer la petición. Actualiza la app e inténtalo de nuevo."
    ),
    INVALID_REQUEST_PARAMETERS(
        httpStatus = 400,
        backendMessage = "Invalid request parameters.",
        retryable = false,
        severity = AppErrorSeverity.FULL_SCREEN,
        title = "PETICIÓN NO VÁLIDA",
        detail = "El servicio recibió parámetros inválidos. Inténtalo de nuevo."
    ),
    /**
     * Prácticamente inalcanzable: el backend lo emite cuando el cliente corta la conexión, así que
     * el cuerpo normalmente no llega. Se mapea igual para que el catálogo sea completo frente al
     * contrato y, si llegara, signifique algo distinto a un rechazo de validación.
     */
    CONNECTION_INTERRUPTED(
        httpStatus = 400,
        backendMessage = "Connection interrupted by the client.",
        detail = "Se interrumpió la conexión con el servidor. Inténtalo de nuevo."
    ),

    // --- 404: URL base mal configurada, no vale la pena reintentar ---
    ENDPOINT_NOT_FOUND(
        httpStatus = 404,
        backendMessage = "Resource not found.",
        retryable = false,
        severity = AppErrorSeverity.FULL_SCREEN,
        title = "DIRECCIÓN DEL SERVICIO NO VÁLIDA",
        detail = "La dirección del servidor no es correcta. Revísala en Ajustes."
    ),

    // --- 408: timeout declarado por el propio backend ---
    REQUEST_TIMEOUT(
        httpStatus = 408,
        backendMessage = "Request timed out.",
        detail = "La petición tardó demasiado. Inténtalo de nuevo."
    ),

    // --- 502: fallo del motor de extracción ---
    SEARCH_TIMEOUT(
        httpStatus = 502,
        backendMessage = "The search is taking too long. Try again.",
        detail = "La búsqueda está tardando demasiado. Inténtalo de nuevo."
    ),
    EXTRACTION_TIMEOUT(
        httpStatus = 502,
        backendMessage = "The extraction is taking too long. Try another video or try again later.",
        detail = "La extracción está tardando demasiado. Prueba otro vídeo o inténtalo más tarde."
    ),
    SEARCH_INTERRUPTED(
        httpStatus = 502,
        backendMessage = "The search was interrupted. Try again.",
        detail = "La búsqueda se interrumpió. Inténtalo de nuevo."
    ),
    EXTRACTION_INTERRUPTED(
        httpStatus = 502,
        backendMessage = "The extraction was interrupted. Try again.",
        detail = "La extracción se interrumpió. Inténtalo de nuevo."
    ),
    SEARCH_RESULTS_UNREADABLE(
        httpStatus = 502,
        backendMessage = "We could not interpret the search results. Try again.",
        detail = "No pudimos interpretar los resultados. Inténtalo de nuevo."
    ),
    EXTRACTION_SETUP_FAILED(
        httpStatus = 502,
        backendMessage = "We could not prepare the audio extraction. Try again.",
        detail = "No pudimos preparar la extracción. Inténtalo de nuevo."
    ),
    NO_AUDIO_AVAILABLE(
        httpStatus = 502,
        backendMessage = "We could not find an audio available for that video.",
        detail = "Ese vídeo no tiene audio disponible. Prueba otro resultado."
    ),
    /** Mismo literal que [NO_RESULTS], pero devuelto desde el endpoint de extracción. */
    VIDEO_HAS_NO_AUDIO(
        httpStatus = 502,
        backendMessage = "We could not find videos with that name. Try a more specific search.",
        detail = "No pudimos extraer el audio de ese vídeo. Prueba otro resultado."
    ),
    YOUTUBE_UNAVAILABLE(
        httpStatus = 502,
        backendMessage = "We could not access YouTube right now. Try again later.",
        detail = "No pudimos acceder a YouTube ahora mismo. Inténtalo más tarde."
    ),
    VIDEO_NOT_PUBLIC(
        httpStatus = 502,
        backendMessage = "The video is not publicly available. Try another result.",
        retryable = false,
        detail = "Ese vídeo no es público. Prueba otro resultado."
    ),
    SEARCH_FAILED(
        httpStatus = 502,
        backendMessage = "We could not perform the search. Try again.",
        detail = "No pudimos realizar la búsqueda. Inténtalo de nuevo."
    ),
    EXTRACTION_FAILED(
        httpStatus = 502,
        backendMessage = "We could not extract the audio from the selected video. Try another video.",
        detail = "No pudimos extraer el audio de ese vídeo. Prueba otro resultado."
    ),

    // --- 502 por infraestructura: no es un problema del usuario ---
    CONVERSION_FAILED(
        httpStatus = 502,
        backendMessage = "We could not convert the audio. Check that ffmpeg is installed on the server.",
        retryable = false,
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "El servidor no puede convertir el audio en este momento. Inténtalo más tarde."
    ),

    // --- 503: yt-dlp / ffmpeg ausentes en el contenedor ---
    EXTRACTION_SERVICE_UNAVAILABLE(
        httpStatus = 503,
        backendMessage = "The extraction service is unavailable. Check that yt-dlp and ffmpeg are installed.",
        retryable = false,
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "El servicio de extracción no está disponible ahora mismo. Inténtalo más tarde."
    ),

    // --- 500: catch-all del backend ---
    UNEXPECTED_SERVER_ERROR(
        httpStatus = 500,
        backendMessage = "We could not process your request right now. Try again later.",
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "El servicio está fallando. Inténtalo más tarde."
    ),

    // --- Fallos de transporte: nunca hubo respuesta HTTP ---
    CLIENT_TIMEOUT(
        detail = "La conexión tardó demasiado. Inténtalo de nuevo."
    ),
    NO_INTERNET(
        detail = "Sin conexión a internet. Revisa tu red e inténtalo de nuevo."
    ),
    UNKNOWN(
        detail = "Ocurrió un error inesperado. Inténtalo más tarde."
    ),

    // --- Fallos del propio dispositivo, ajenos a la API ---
    SAVE_FAILED(
        retryable = false,
        detail = "No se pudo guardar el archivo."
    );

    val blocksScreen: Boolean get() = severity == AppErrorSeverity.FULL_SCREEN
}

/**
 * Error ya resuelto: lleva el [kind] decidido en un único punto, más el dato crudo del backend
 * para diagnóstico. La UI consume [title] y [detail]; nunca vuelve a mirar [statusCode].
 */
data class AppError(
    val kind: AppErrorKind,
    val statusCode: Int? = null,
    val backendMessage: String? = null,
    val cause: Throwable? = null
) {
    val title: String get() = kind.title
    val detail: String get() = kind.detail
    val retryable: Boolean get() = kind.retryable
    val blocksScreen: Boolean get() = kind.blocksScreen

    /** Texto técnico para logs; nunca se muestra en pantalla. */
    fun describe(): String = buildString {
        append(kind.name)
        statusCode?.let { append(" http=").append(it) }
        if (backendMessage != null && backendMessage != kind.backendMessage) {
            append(" msg=\"").append(backendMessage).append('"')
        }
        cause?.let { append(" cause=").append(it::class.simpleName ?: "Throwable") }
    }
}

/**
 * Aplana el error a los tres datos que reconstruyen el [AppErrorKind]. `null` significa
 * "no hay nada que guardar", que es lo que [Saver] espera para no escribir la entrada.
 */
internal fun AppError.toSaveableState(): List<Any?> = listOf(kind.name, statusCode, backendMessage)

/** Inverso de [toSaveableState]. */
internal fun appErrorFromSaveableState(saved: List<Any?>): AppError = AppError(
    kind = AppErrorKind.valueOf(saved[0] as String),
    statusCode = saved[1] as Int?,
    backendMessage = saved[2] as String?
)

/**
 * Permite que el error de conexión sobreviva a un cambio de configuración sin obligar a mover el
 * estado fuera del composable.
 *
 * [AppError.cause] no es saveable (es un `Throwable`) y se pierde al restaurar: solo se usa para
 * diagnóstico dentro de la sesión, así que tras rotar `describe()` pierde el sufijo `cause=`.
 */
val AppErrorSaver: Saver<AppError?, List<Any?>> = Saver(
    save = { error -> error?.toSaveableState() },
    restore = { appErrorFromSaveableState(it) }
)

/**
 * Resolución de errores. Única pieza que conoce los códigos HTTP del backend.
 */
object AppErrorCatalog {

    private const val UNKNOWN_BACKEND_MESSAGE =
        "We could not process your request right now. Try again later."

    /**
     * El backend no manda código de negocio, así que la clave de búsqueda es la pareja
     * (status, mensaje). El status desempata el literal compartido entre 400 y 502.
     */
    private val byStatusAndMessage: Map<Pair<Int, String>, AppErrorKind> = AppErrorKind.entries
        .filter { it.httpStatus != null && it.backendMessage != null }
        .associateBy { (it.httpStatus!! to it.backendMessage!!) }

    /** Respaldo cuando el mensaje no coincide: se decide únicamente por status. */
    private val byStatus: Map<Int, AppErrorKind> = mapOf(
        404 to AppErrorKind.ENDPOINT_NOT_FOUND,
        408 to AppErrorKind.REQUEST_TIMEOUT,
        500 to AppErrorKind.UNEXPECTED_SERVER_ERROR,
        502 to AppErrorKind.EXTRACTION_FAILED,
        503 to AppErrorKind.EXTRACTION_SERVICE_UNAVAILABLE
    )

    /**
     * Resuelve una respuesta HTTP fallida a un [AppErrorKind].
     *
     * @param statusCode status HTTP, o `null` si nunca hubo respuesta.
     * @param message valor crudo del campo `message`; puede ser `null` o ilegible si el cuerpo no
     *   es del backend (por ejemplo el HTML de Cloudflare o el cuerpo por defecto de Spring).
     */
    fun resolve(statusCode: Int?, message: String?): AppErrorKind {
        if (statusCode == null) return AppErrorKind.UNKNOWN

        val normalized = message?.trim()
        if (!normalized.isNullOrEmpty()) {
            byStatusAndMessage[statusCode to normalized]?.let { return it }
        }

        // El catch-all del backend (@ExceptionHandler(Exception.class)) degrada a 500 los casos
        // que sin él serían 404, 405, 415 o 406, y responde siempre con este mismo literal.
        if (normalized == UNKNOWN_BACKEND_MESSAGE && statusCode != 500) {
            return AppErrorKind.UNEXPECTED_SERVER_ERROR
        }

        return byStatus[statusCode] ?: AppErrorKind.UNKNOWN
    }

    /** Resuelve un fallo de transporte: no hubo respuesta HTTP, solo una excepción. */
    fun resolve(cause: Throwable): AppError = AppError(
        kind = classifyTransportError(cause),
        cause = cause
    )

    private fun classifyTransportError(e: Throwable): AppErrorKind {
        val simpleName = e::class.simpleName ?: ""
        return when {
            e is HttpRequestTimeoutException ||
                e is SocketTimeoutException ||
                simpleName == "SocketTimeoutException" ||
                simpleName == "InterruptedIOException" -> AppErrorKind.CLIENT_TIMEOUT

            e is ConnectTimeoutException ||
                simpleName == "ConnectException" ||
                simpleName == "UnknownHostException" ||
                simpleName == "NoRouteToHostException" ||
                simpleName == "PortUnreachableException" ||
                simpleName == "UnresolvedAddressException" -> AppErrorKind.NO_INTERNET

            else -> AppErrorKind.UNKNOWN
        }
    }
}
