package com.dev.yoump3.services

import java.io.File
import java.util.Base64

actual fun saveAudioToDownloads(fileName: String, contentType: String, audioBase64: String): String {
    val downloads = File(System.getProperty("user.home"), "Downloads")
    if (!downloads.exists() && !downloads.mkdirs()) {
        error("No se pudo crear la carpeta Descargas")
    }
    val file = File(downloads, fileName)
    file.writeBytes(Base64.getDecoder().decode(audioBase64))
    return file.absolutePath
}
