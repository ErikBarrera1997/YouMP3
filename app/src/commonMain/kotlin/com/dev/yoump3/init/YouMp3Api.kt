package com.dev.yoump3.init

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

@Serializable
private data class ErrorApiResponse(val message: String? = null)

class ApiException(val apiMessage: String) : Exception(apiMessage)

class YouMp3Api(
    private val baseUrl: String
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 200000
            connectTimeoutMillis = 20000
            socketTimeoutMillis = 200000
        }
    }

    suspend fun checkConnection(): Result<Unit> = try {
        client.get(baseUrl)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun searchSongs(videoName: String): AudioSearchResponse {
        val response = client.post("$baseUrl/api/audios/search") {
            contentType(ContentType.Application.Json)
            setBody(AudioSearchRequest(videoName))
        }
        ensureApiSuccess(response)
        return response.body()
    }

    suspend fun extractAudio(videoName: String, videoId: String? = null): AudioExtractionResponse {
        val response = client.post("$baseUrl/api/audios/extract") {
            contentType(ContentType.Application.Json)
            setBody(AudioExtractionRequest(videoName, videoId))
        }
        ensureApiSuccess(response)
        return response.body()
    }

    private suspend fun ensureApiSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val apiMessage = runCatching { response.body<ErrorApiResponse>().message }
            .getOrNull()
            ?: "The service is temporarily unavailable. Try again later."
        throw ApiException(apiMessage)
    }
}
