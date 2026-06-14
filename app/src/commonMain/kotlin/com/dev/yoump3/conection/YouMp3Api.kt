package com.dev.yoump3.conection

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AudioExtractionRequest(val videoName: String)

@Serializable
data class AudioExtractionResponse(
    val success: Boolean,
    val message: String,
    val videoTitle: String? = null,
    val fileName: String? = null,
    val contentType: String? = null,
    val audioBase64: String? = null
)

class YouMp3Api {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun extractAudio(videoName: String): AudioExtractionResponse {
        return client.post("${YouMp3ApiConfig.baseUrl}/api/audios/extract") {
            contentType(ContentType.Application.Json)
            setBody(AudioExtractionRequest(videoName))
        }.body()
    }
}
