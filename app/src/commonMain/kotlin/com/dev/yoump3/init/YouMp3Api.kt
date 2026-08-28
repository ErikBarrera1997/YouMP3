package com.dev.yoump3.init

import com.dev.yoump3.conection.YouMp3ApiConfig
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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

object YouMp3Api {
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

    suspend fun checkConnection(): Result<Unit> = runCatching {
        client.get(YouMp3ApiConfig.baseUrl)
    }

    suspend fun searchSongs(videoName: String): AudioSearchResponse {
        return client.post("${YouMp3ApiConfig.baseUrl}/api/audios/search") {
            contentType(ContentType.Application.Json)
            setBody(AudioSearchRequest(videoName))
        }.body()
    }

    suspend fun extractAudio(videoName: String, videoId: String? = null): AudioExtractionResponse {
        return client.post("${YouMp3ApiConfig.baseUrl}/api/audios/extract") {
            contentType(ContentType.Application.Json)
            setBody(AudioExtractionRequest(videoName, videoId))
        }.body()
    }
}
