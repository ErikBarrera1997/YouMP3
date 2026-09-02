package com.dev.yoump3.services

interface AudioSaver {
    suspend fun save(fileName: String, contentType: String, audioBase64: String): String
}
