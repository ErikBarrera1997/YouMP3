package com.dev.yoump3.error

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
     * Solo llega desde la extracción: `AudioExtractionService` valida el `videoId` que el cliente
     * manda junto al título. Es un resultado viejo o corrupto, no algo que se arregle reintentando.
     */
    INVALID_VIDEO_ID(
        httpStatus = 400,
        backendMessage = "The video identifier is not valid. Try another result.",
        retryable = false,
        detail = "Ese resultado ya no es válido. Prueba con otro."
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
    SEARCH_QUOTA_EXHAUSTED(
        httpStatus = 502,
        backendMessage = "The YouTube search quota is exhausted. Try again later.",
        retryable = true,
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "Se ha agotado la cuota de búsqueda. Inténtalo más tarde."
    ),
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

    // --- 502 del servicio de búsqueda por Data API, antes de caer a yt-dlp ---
    SEARCH_TEMPORARILY_UNAVAILABLE(
        httpStatus = 502,
        backendMessage = "We could not perform the search right now. Try again.",
        detail = "No pudimos buscar en este momento. Inténtalo de nuevo."
    ),
    SEARCH_SERVICE_UNREACHABLE(
        httpStatus = 502,
        backendMessage = "We could not reach the YouTube search service. Try again.",
        detail = "No pudimos contactar con el servicio de búsqueda. Inténtalo de nuevo."
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
    /**
     * El backend responde 503 con este literal cuando yt-dlp sí está instalado pero no puede hablar
     * con YouTube sin cookies de sesión. Rompe búsqueda y extracción por igual, y verificado contra
     * el servicio desplegado, no es transitorio: reintentar no lo arregla, hay que reconfigurar el
     * contenedor.
     */
    YOUTUBE_SESSION_COOKIES_MISSING(
        httpStatus = 503,
        backendMessage = "The YouTube session cookies are not available on the server.",
        retryable = false,
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "El servidor no puede acceder a YouTube en este momento. Es un problema del servicio, no de tu búsqueda. Inténtalo más tarde."
    ),
    /**
     * YouTube bloquea las peticiones desde la IP del servidor. Es el motivo más probable de que
     * cookies, PO token o proxy dejen de servir, y llega cuando las cookies ya están resueltas.
     */
    YOUTUBE_BLOCKED(
        httpStatus = 503,
        backendMessage = "YouTube is blocking this request from our server. Try again later.",
        retryable = true,
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "YouTube está bloqueando las peticiones desde nuestro servidor. Inténtalo más tarde."
    ),
    /**
     * Los dos siguientes son el último recurso de `searchVideos` y `extractAudioByVideoName`: se
     * lanzan cuando todos los player clients fallaron sin que ninguno encajara en un motivo
     * concreto. Cuidado con el literal: se diferencia del 502 de arriba solo por "Try again."
     * frente a "Try again later.", y esa diferencia es lo único que los separa en el catálogo.
     */
    YOUTUBE_BLOCKED_SEARCH(
        httpStatus = 503,
        backendMessage = "We could not perform the search right now. Try again later.",
        retryable = true,
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "YouTube no nos está dejando buscar en este momento. Inténtalo más tarde."
    ),
    YOUTUBE_BLOCKED_EXTRACTION(
        httpStatus = 503,
        backendMessage = "We could not extract the audio. Try again later.",
        retryable = true,
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "YouTube no nos está dejando extraer el audio en este momento. Inténtalo más tarde."
    ),
    /**
     * La otra rama de `resolveCookiesFile`: aquí la ruta ni siquiera está configurada, no es que el
     * archivo falte. Para el usuario es el mismo servicio caído.
     */
    YTDLP_COOKIES_NOT_CONFIGURED(
        httpStatus = 503,
        backendMessage = "The YouTube cookies file is not configured. Set YTDLP_COOKIES_PATH.",
        retryable = false,
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "El servidor no tiene configuradas las cookies de YouTube. Es un problema del servicio."
    ),

    // --- 500: catch-all del backend ---
    UNEXPECTED_SERVER_ERROR(
        httpStatus = 500,
        backendMessage = "We could not process your request right now. Try again later.",
        severity = AppErrorSeverity.FULL_SCREEN,
        detail = "El servicio está fallando. Inténtalo más tarde."
    ),

    // --- Respuesta de pasarela, no del backend: la instancia está levantándose ---
    /**
     * Lo decide lo que hay por delante del backend, no el backend. Cloudflare o el propio Render
     * contestan 502/503/52x mientras levantan una instancia en reposo, y el cuerpo no es JSON del
     * backend: [AppErrorCatalog.byStatusAndMessage] no casa y el error se decide solo por status.
     *
     * Antes ese caso caía en [EXTRACTION_FAILED] y el usuario leía "prueba otro resultado" en la
     * pantalla de arranque, donde no hay ningún vídeo seleccionado.
     *
     * Sin [httpStatus] ni [backendMessage] a propósito: no es un literal del backend, solo un
     * respaldo por status. Declararlos rompería los tests de invariante del catálogo.
     */
    GATEWAY_UNAVAILABLE(
        retryable = true,
        severity = AppErrorSeverity.FULL_SCREEN,
        title = "SERVIDOR NO DISPONIBLE",
        detail = "El servidor no está respondiendo. Puede que esté arrancando; inténtalo en unos segundos."
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
    ),
    /**
     * No lo decide el backend: lo decide el cliente antes de decodificar, cuando la longitud de la
     * cadena de base64 ya permite saber que el audio no cabe en el heap. Reintentar el mismo vídeo
     * devolvería exactamente la misma respuesta, así que no es reintentable.
     */
    AUDIO_TOO_LARGE(
        retryable = false,
        detail = "Ese audio es demasiado largo para este dispositivo. Prueba con otro vídeo."
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
        502 to AppErrorKind.GATEWAY_UNAVAILABLE,
        // 503 no entra aquí: no es un código de pasarela. Lo emite el backend para decir que yt-dlp
        // no es utilizable, y todos sus literales son del backend, así que el respaldo es suyo.
        503 to AppErrorKind.EXTRACTION_SERVICE_UNAVAILABLE,
        // La pasarela no distingue sus propios 52x y aquí no hay cuerpo que los distinga.
        521 to AppErrorKind.GATEWAY_UNAVAILABLE,
        522 to AppErrorKind.GATEWAY_UNAVAILABLE,
        523 to AppErrorKind.GATEWAY_UNAVAILABLE,
        524 to AppErrorKind.GATEWAY_UNAVAILABLE
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
