package com.dev.yoump3.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File
import java.util.Base64

class DesktopAudioSaver : AudioSaver {

    override suspend fun save(fileName: String, contentType: String, audioBase64: String): String {
        return withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            val bytes = Base64.getDecoder().decode(audioBase64)
            val downloads = File(System.getProperty("user.home"), "Downloads")
            if (!downloads.exists() && !downloads.mkdirs()) {
                error("No se pudo crear la carpeta Descargas")
            }
            val file = File(downloads, fileName)
            coroutineContext.ensureActive()
            file.writeBytes(bytes)
            file.absolutePath
        }
    }
}
