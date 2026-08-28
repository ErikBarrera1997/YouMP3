package com.dev.yoump3.services

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object AppContextProvider {
    lateinit var context: Context
}

actual fun saveAudioToDownloads(fileName: String, contentType: String, audioBase64: String): String {
    val bytes = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, contentType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = AppContextProvider.context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: error("No se pudo crear el archivo en Descargas")
        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("No se pudo abrir el archivo para escritura")
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return "Descargas/$fileName"
    }

    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloads.exists() && !downloads.mkdirs()) {
        error("No se pudo crear la carpeta Descargas")
    }
    val file = File(downloads, fileName)
    FileOutputStream(file).use { it.write(bytes) }
    return file.absolutePath
}
