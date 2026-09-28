package com.dev.yoump3.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File

class DesktopAudioSaver : AudioSaver {

    override suspend fun save(fileName: String, contentType: String, audio: ByteArray): String {
        return withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            val downloads = File(System.getProperty("user.home"), "Downloads")
            if (!downloads.exists() && !downloads.mkdirs()) {
                error("No se pudo crear la carpeta Descargas")
            }
            val file = File(downloads, fileName)
            coroutineContext.ensureActive()
            file.writeBytes(audio)
            file.absolutePath
        }
    }
}
