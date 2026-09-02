package com.dev.yoump3.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File
import java.io.FileOutputStream

class AndroidAudioSaver(
    private val context: Context
) : AudioSaver {

    override suspend fun save(fileName: String, contentType: String, audioBase64: String): String {
        val appContext = context.applicationContext
        return withContext(Dispatchers.IO) {
            coroutineContext.ensureActive()
            val bytes = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, contentType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = appContext.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: error("No se pudo crear el archivo en Descargas")
                try {
                    coroutineContext.ensureActive()
                    resolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: error("No se pudo abrir el archivo para escritura")
                    coroutineContext.ensureActive()
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
                showDownloadNotification(appContext, fileName)
                return@withContext "Descargas/$fileName"
            }

            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloads.exists() && !downloads.mkdirs()) {
                error("No se pudo crear la carpeta Descargas")
            }
            val file = File(downloads, fileName)
            coroutineContext.ensureActive()
            FileOutputStream(file).use { it.write(bytes) }
            showDownloadNotification(appContext, fileName)
            file.absolutePath
        }
    }

    private fun showDownloadNotification(context: Context, fileName: String) {
        val channelId = "yoump3_downloads"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "YouMp3 descargas",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Descarga completada")
            .setContentText(fileName)
            .setContentInfo("BY CLEVER CLOUD")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
