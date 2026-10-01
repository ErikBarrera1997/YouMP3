package com.dev.yoump3.error

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fija el contrato de errores contra los literales reales del backend:
 * `GlobalExceptionHandler` y `YtDlpYoutubeAudioRepository` en el proyecto YOUMP3API.
 * Si cambia un mensaje en Spring, estos tests lo delatan.
 */
class AppErrorCatalogTest {

    @Test
    fun `resolves the 400 validation errors`() {
        assertEquals(
            AppErrorKind.VIDEO_NAME_MISSING,
            AppErrorCatalog.resolve(400, "Enter the name of a video to search on YouTube.")
        )
        assertEquals(
            AppErrorKind.QUERY_TOO_SHORT,
            AppErrorCatalog.resolve(400, "The search is too short. Enter at least 3 characters.")
        )
        assertEquals(
            AppErrorKind.QUERY_TOO_LONG,
            AppErrorCatalog.resolve(400, "The search is too long. Try a shorter name.")
        )
        assertEquals(
            AppErrorKind.MALFORMED_REQUEST,
            AppErrorCatalog.resolve(400, "Malformed request body.")
        )
        assertEquals(
            AppErrorKind.CONNECTION_INTERRUPTED,
            AppErrorCatalog.resolve(400, "Connection interrupted by the client.")
        )
    }

    @Test
    fun `disambiguates the message shared by 400 and 502 using the status`() {
        val shared = "We could not find videos with that name. Try a more specific search."
        assertEquals(AppErrorKind.NO_RESULTS, AppErrorCatalog.resolve(400, shared))
        assertEquals(AppErrorKind.VIDEO_HAS_NO_AUDIO, AppErrorCatalog.resolve(502, shared))
    }

    @Test
    fun `resolves the 502 extraction errors`() {
        assertEquals(
            AppErrorKind.SEARCH_TIMEOUT,
            AppErrorCatalog.resolve(502, "The search is taking too long. Try again.")
        )
        assertEquals(
            AppErrorKind.VIDEO_NOT_PUBLIC,
            AppErrorCatalog.resolve(502, "The video is not publicly available. Try another result.")
        )
        assertEquals(
            AppErrorKind.YOUTUBE_UNAVAILABLE,
            AppErrorCatalog.resolve(502, "We could not access YouTube right now. Try again later.")
        )
    }

    @Test
    fun `service wide failures block the screen`() {
        assertTrue(AppErrorKind.UNEXPECTED_SERVER_ERROR.blocksScreen)
        assertTrue(AppErrorKind.EXTRACTION_SERVICE_UNAVAILABLE.blocksScreen)
        assertTrue(AppErrorKind.ENDPOINT_NOT_FOUND.blocksScreen)
        assertTrue(!AppErrorKind.QUERY_TOO_SHORT.blocksScreen)
    }

    @Test
    fun `falls back to the status when the body is not the backend shape`() {
        // Cuerpo por defecto de Spring: {timestamp, status, error}, sin campo message.
        assertEquals(
            AppErrorKind.UNEXPECTED_SERVER_ERROR,
            AppErrorCatalog.resolve(500, null)
        )
        // HTML de Cloudflare: el status es lo unico fiable.
        assertEquals(
            AppErrorKind.UNEXPECTED_SERVER_ERROR,
            AppErrorCatalog.resolve(500, "<html><title>500</title></html>")
        )
    }

    @Test
    fun `the catch-all literal degrades any non-500 status to an unexpected server error`() {
        val generic = "We could not process your request right now. Try again later."
        // El catch-all de Spring se come el NoResourceFoundException, que en codigo local es 404.
        assertEquals(AppErrorKind.UNEXPECTED_SERVER_ERROR, AppErrorCatalog.resolve(404, generic))
        assertEquals(AppErrorKind.UNEXPECTED_SERVER_ERROR, AppErrorCatalog.resolve(405, generic))
    }

    @Test
    fun `an unrecognized message falls back to the kind registered for that status`() {
        // El 503 lo emite el backend, no la pasarela, asi que el respaldo es suyo y no del gateway.
        assertEquals(
            AppErrorKind.EXTRACTION_SERVICE_UNAVAILABLE,
            AppErrorCatalog.resolve(503, "un mensaje nuevo que el backend aun no define")
        )
        assertEquals(AppErrorKind.UNEXPECTED_SERVER_ERROR, AppErrorCatalog.resolve(500, "???"))
    }

    @Test
    fun `statuses with a single meaning resolve even without a matching message`() {
        assertEquals(AppErrorKind.ENDPOINT_NOT_FOUND, AppErrorCatalog.resolve(404, null))
        assertEquals(AppErrorKind.REQUEST_TIMEOUT, AppErrorCatalog.resolve(408, null))
        assertEquals(AppErrorKind.GATEWAY_UNAVAILABLE, AppErrorCatalog.resolve(502, null))
    }

    @Test
    fun `a gateway response is not reported as a failed extraction`() {
        // El bug: un 502/52x de la pasarela caia en EXTRACTION_FAILED y la pantalla de arranque
        // pedia "prueba otro resultado" sin que hubiera ningun video seleccionado.
        val cloudflareHtml = "<html><head><title>502</title></head><body>Bad gateway</body></html>"
        assertEquals(
            AppErrorKind.GATEWAY_UNAVAILABLE,
            AppErrorCatalog.resolve(502, cloudflareHtml)
        )
        assertEquals(
            AppErrorKind.GATEWAY_UNAVAILABLE,
            AppErrorCatalog.resolve(502, null)
        )
        listOf(521, 522, 523, 524).forEach { gatewayStatus ->
            assertEquals(
                "el $gatewayStatus de la pasarela debe resolverse como pasarela",
                AppErrorKind.GATEWAY_UNAVAILABLE,
                AppErrorCatalog.resolve(gatewayStatus, null)
            )
        }
    }

    @Test
    fun `the 503 of a backend without youtube cookies is not a gateway failure`() {
        // Verificado contra el servicio desplegado: busqueda y extraccion devuelven esto, con la
        // instancia caliente. Es configuracion del contenedor, no una pasarela levantandose, asi
        // que no puede caer en GATEWAY_UNAVAILABLE ni marcarse como reintentable.
        val cookies = AppErrorCatalog.resolve(
            503,
            "The YouTube session cookies are not available on the server."
        )
        assertEquals(AppErrorKind.YOUTUBE_SESSION_COOKIES_MISSING, cookies)
        assertTrue(!AppErrorKind.YOUTUBE_SESSION_COOKIES_MISSING.retryable)
        assertTrue(AppErrorKind.YOUTUBE_SESSION_COOKIES_MISSING.blocksScreen)
    }

    @Test
    fun `a 503 is only a gateway failure if the backend never spoke`() {
        // 503 no es un codigo de pasarela: la pasarela usa 502 y los 52x.
        assertEquals(
            AppErrorKind.EXTRACTION_SERVICE_UNAVAILABLE,
            AppErrorCatalog.resolve(503, null)
        )
    }

    @Test
    fun `a real backend 503 still means yt-dlp is missing and is not retried`() {
        // El literal manda sobre el respaldo por status: si el backend responde, el problema es suyo.
        assertEquals(
            AppErrorKind.EXTRACTION_SERVICE_UNAVAILABLE,
            AppErrorCatalog.resolve(
                503,
                "The extraction service is unavailable. Check that yt-dlp and ffmpeg are installed."
            )
        )
        assertTrue(!AppErrorKind.EXTRACTION_SERVICE_UNAVAILABLE.retryable)
    }

    @Test
    fun `the 502 and 503 blocked variants stay apart despite a near identical literal`() {
        // El backend emite "We could not perform the search right now." con 502 y con "Try again
        // later." con 503. Lo unico que los separa es la coletilla, asi que se fija a proposito.
        assertEquals(
            AppErrorKind.SEARCH_TEMPORARILY_UNAVAILABLE,
            AppErrorCatalog.resolve(502, "We could not perform the search right now. Try again.")
        )
        assertEquals(
            AppErrorKind.YOUTUBE_BLOCKED_SEARCH,
            AppErrorCatalog.resolve(503, "We could not perform the search right now. Try again later.")
        )
    }

    @Test
    fun `every 503 of the backend resolves to its own kind`() {
        // Todos los 503 del backend, extraidos de GlobalExceptionHandler y los repositorios.
        val expectations = mapOf(
            "The extraction service is unavailable. Check that yt-dlp and ffmpeg are installed." to AppErrorKind.EXTRACTION_SERVICE_UNAVAILABLE,
            "The YouTube session cookies are not available on the server." to AppErrorKind.YOUTUBE_SESSION_COOKIES_MISSING,
            "The YouTube cookies file is not configured. Set YTDLP_COOKIES_PATH." to AppErrorKind.YTDLP_COOKIES_NOT_CONFIGURED,
            "YouTube is blocking this request from our server. Try again later." to AppErrorKind.YOUTUBE_BLOCKED,
            "We could not perform the search right now. Try again later." to AppErrorKind.YOUTUBE_BLOCKED_SEARCH,
            "We could not extract the audio. Try again later." to AppErrorKind.YOUTUBE_BLOCKED_EXTRACTION
        )
        expectations.forEach { (literal, expected) ->
            assertEquals(
                "el 503 '$literal' debe resolverse a $expected",
                expected,
                AppErrorCatalog.resolve(503, literal)
            )
        }
    }

    @Test
    fun `the invalid video id only comes from the extraction endpoint`() {
        assertEquals(
            AppErrorKind.INVALID_VIDEO_ID,
            AppErrorCatalog.resolve(400, "The video identifier is not valid. Try another result.")
        )
        assertTrue(!AppErrorKind.INVALID_VIDEO_ID.retryable)
    }

    @Test
    fun `a gateway failure is retryable and blocks the screen`() {
        // Lo que permite el reintento manual es retryable, asi que esto tiene que quedarse en true.
        assertTrue(AppErrorKind.GATEWAY_UNAVAILABLE.retryable)
        assertTrue(AppErrorKind.GATEWAY_UNAVAILABLE.blocksScreen)
    }

    @Test
    fun `the gateway kind is a fallback, not a backend literal`() {
        // Sin esto el catalogo mentiria: GATEWAY_UNAVAILABLE nunca lo emite el backend.
        assertNull(AppErrorKind.GATEWAY_UNAVAILABLE.httpStatus)
        assertNull(AppErrorKind.GATEWAY_UNAVAILABLE.backendMessage)
    }

    @Test
    fun `a 400 with an unknown message is not guessed as one of the validation errors`() {
        // Hay varios kinds con status 400: sin el literal exacto no se puede saber cual es, asi que
        // no se elige ninguno al azar.
        assertEquals(AppErrorKind.UNKNOWN, AppErrorCatalog.resolve(400, "no lo conozco"))
    }

    @Test
    fun `statuses outside the contract and absent responses degrade to unknown`() {
        assertEquals(AppErrorKind.UNKNOWN, AppErrorCatalog.resolve(418, null))
        assertEquals(AppErrorKind.UNKNOWN, AppErrorCatalog.resolve(null, "lo que sea"))
    }

    @Test
    fun `every kind carries displayable copy`() {
        AppErrorKind.entries.forEach { kind ->
            assertTrue("falta title en ${kind.name}", kind.title.isNotBlank())
            assertTrue("falta detail en ${kind.name}", kind.detail.isNotBlank())
        }
    }

    @Test
    fun `no two kinds share the same status and message pair`() {
        val pairs = AppErrorKind.entries
            .filter { it.httpStatus != null && it.backendMessage != null }
            .map { it.httpStatus to it.backendMessage }
        assertEquals(
            "hay entradas duplicadas en el catálogo: ${pairs.groupingBy { it }.eachCount().filterValues { it > 1 }.keys}",
            pairs.size,
            pairs.toSet().size
        )
    }

    @Test
    fun `every backend error literal is mapped`() {
        // Catálogo cerrado: si el backend añade un literal, tiene que aparecer aquí. Fuente:
        // GlobalExceptionHandler.failure(...) y los throw de YtDlpYoutubeAudioRepository /
        // AudioExtractionService en YOUMP3API.
        val backendErrorLiterals = setOf(
            "Enter the name of a video to search on YouTube.",
            "The search is too short. Enter at least 3 characters.",
            "The search is too long. Try a shorter name.",
            "We could not find videos with that name. Try a more specific search.",
            "Malformed request body.",
            "Invalid request parameters.",
            "The video identifier is not valid. Try another result.",
            "Connection interrupted by the client.",
            "Resource not found.",
            "Request timed out.",
            "We could not process your request right now. Try again later.",
            "The search is taking too long. Try again.",
            "The search was interrupted. Try again.",
            "We could not perform the search. Try again.",
            "We could not interpret the search results. Try again.",
            "The extraction is taking too long. Try another video or try again later.",
            "The extraction was interrupted. Try again.",
            "We could not prepare the audio extraction. Try again.",
            "We could not find an audio available for that video.",
            "The YouTube search quota is exhausted. Try again later.",
            "We could not perform the search right now. Try again.",
            "We could not reach the YouTube search service. Try again.",
            "We could not convert the audio. Check that ffmpeg is installed on the server.",
            "We could not access YouTube right now. Try again later.",
            "The video is not publicly available. Try another result.",
            "We could not extract the audio from the selected video. Try another video.",
            "The extraction service is unavailable. Check that yt-dlp and ffmpeg are installed.",
            "The YouTube session cookies are not available on the server.",
            "The YouTube cookies file is not configured. Set YTDLP_COOKIES_PATH.",
            "YouTube is blocking this request from our server. Try again later.",
            "We could not perform the search right now. Try again later.",
            "We could not extract the audio. Try again later."
        )
        val mapped = AppErrorKind.entries.mapNotNull { it.backendMessage }.toSet()
        assertEquals(
            "literales del backend sin mapear: ${backendErrorLiterals - mapped}",
            emptySet<String>(),
            backendErrorLiterals - mapped
        )
        assertEquals(
            "el catálogo afirma un literal que el backend no emite: ${mapped - backendErrorLiterals}",
            emptySet<String>(),
            mapped - backendErrorLiterals
        )
    }
}
