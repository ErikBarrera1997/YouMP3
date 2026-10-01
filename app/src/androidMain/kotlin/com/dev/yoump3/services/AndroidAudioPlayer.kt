package com.dev.yoump3.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.dev.yoump3.YouMp3Application
import java.io.File
import java.io.FileOutputStream

class AndroidAudioPlayer : AudioPlayer {

    companion object {
        private const val PREVIEW_PREFIX = "yoump3_preview"
        private const val PREVIEW_SUFFIX = ".mp3"
        private const val POLLING_JOIN_TIMEOUT_MS = 500L

        @Volatile
        var instance: AndroidAudioPlayer? = null
            private set

        @Volatile
        var activeTitle: String = ""

        /**
         * Borra los temporales de previsualización que dejó un proceso que murió antes de
         * liberarlos. Android nunca limpia la caché de la app por su cuenta.
         */
        fun sweepStalePreviews(context: Context) {
            val stale = context.cacheDir.listFiles()
                ?.filter { it.isFile && it.name.startsWith(PREVIEW_PREFIX) }
                ?: return
            stale.forEach { it.delete() }
        }
    }

    override val state = AudioPlayerState()

    private val appContext: Context = YouMp3Application.appContext

    private var player: MediaPlayer? = null
    private var tempFile: File? = null
    private var loadedAudio: ExtractedAudio? = null
    private var updateThread: Thread? = null
    private var running = false

    init {
        instance = this
    }

    override fun load(audio: ExtractedAudio, title: String) {
        if (audio === loadedAudio) return

        release()

        // `release()` retira la instancia estática, así que hay que volver a registrarse aquí: es
        // lo que leen la notificación y sus botones para saber qué reproducir. Sin esto, una
        // extracción posterior a haber salido de la app funcionaría pero dejaría la notificación
        // inerte.
        instance = this
        activeTitle = title

        val file = File.createTempFile(PREVIEW_PREFIX, PREVIEW_SUFFIX, appContext.cacheDir)
        tempFile = file
        try {
            FileOutputStream(file).use { it.write(audio.bytes) }
        } catch (e: Exception) {
            // El temporal se registra antes de escribirlo, no después: si el write falla (ENOSPC,
            // IOException) y lo registrásemos al final, el MP3 ya creado no lo referenciaría ningún
            // campo y por tanto `release()` no podría borrarlo. Quedaría en `cacheDir` hasta el
            // siguiente arranque, con `sweepStalePreviews` como única salida.
            release()
            throw e
        }
        loadedAudio = audio

        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            mp.setOnCompletionListener {
                stopPolling()
                state.isPlaying = false
                state.positionMs = 0L
                stopForegroundNotification()
            }
            state.durationMs = mp.duration.toLong().coerceAtLeast(0L)
            state.positionMs = 0L
            state.isPlaying = false
        } catch (e: Exception) {
            // El `MediaPlayer` es un recurso nativo y solo quedaba alcanzable desde el campo
            // `player`, así que un `setDataSource` o un `prepare` fallidos lo filtraban de forma
            // permanente: el decoder nativo se acumulaba en el proceso sin que nada lo liberara.
            // `release()` lo libera y borra el temporal, y la excepción sube para que la pantalla
            // muestre el fallo de la extracción en vez de fingir que hay audio.
            release()
            throw e
        }

        MediaPlayerService.start(appContext)
    }

    override fun toggle() {
        val mp = player ?: return
        if (state.isPlaying) {
            mp.pause()
            state.isPlaying = false
            stopPolling()
        } else {
            mp.start()
            state.isPlaying = true
            startPolling()
        }
        refreshNotification()
    }

    override fun seekTo(positionMs: Long) {
        val mp = player ?: return
        val target = positionMs.coerceIn(0L, state.durationMs)
        mp.seekTo(target.toInt())
        state.positionMs = target
    }

    override fun stop() {
        val mp = player ?: return
        mp.pause()
        mp.seekTo(0)
        state.isPlaying = false
        state.positionMs = 0L
        stopPolling()
        stopForegroundNotification()
    }

    override fun release() {
        stopPolling()
        running = false
        player?.let { mp ->
            try {
                mp.stop()
            } catch (_: IllegalStateException) {
            }
            mp.release()
        }
        player = null
        tempFile?.delete()
        tempFile = null
        loadedAudio = null
        state.isPlaying = false
        state.positionMs = 0L
        state.durationMs = 0L
        activeTitle = ""
        // Se suelta la referencia estática para que el recolector pueda llevarse este reproductor si
        // nada más lo apunta. La comprobación importa: si otra Activity ya creó un reproductor y
        // `instance` es suyo, este `release()` no debe robarle el sitio.
        if (instance === this) instance = null
        stopForegroundNotification()
    }

    private fun refreshNotification() {
        if (MediaPlayerService.isRunning) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(appContext)
                    .notify(MediaPlayerService.NOTIFICATION_ID, MediaPlayerService.buildNotification(appContext))
            }
        } else if (state.isPlaying) {
            MediaPlayerService.start(appContext)
        }
    }

    private fun stopForegroundNotification() {
        NotificationManagerCompat.from(appContext).cancel(MediaPlayerService.NOTIFICATION_ID)
        appContext.stopService(Intent(appContext, MediaPlayerService::class.java))
    }

    private fun startPolling() {
        stopPolling()
        running = true
        updateThread = Thread {
            while (running) {
                val mp = player
                if (state.isPlaying && mp != null) {
                    try {
                        state.positionMs = mp.currentPosition.toLong()
                    } catch (_: IllegalStateException) {
                        state.isPlaying = false
                    }
                }
                try {
                    Thread.sleep(200)
                } catch (_: InterruptedException) {
                    running = false
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun stopPolling() {
        running = false
        val thread = updateThread ?: return
        updateThread = null
        thread.interrupt()
        // `MediaPlayer` no es thread-safe. Sin esta espera, el hilo de sondeo puede estar dentro de
        // `currentPosition` mientras esta misma llamada hace `release()`, y eso no es una excepción
        // que se pueda capturar en un `try`: es un acceso concurrente a un recurso nativo. El
        // `interrupt` rompe el `sleep` de 200 ms, así que la espera es casi siempre inmediata; el
        // plazo es solo para que un sondeo atascado en una llamada nativa no bloquee el hilo principal.
        try {
            thread.join(POLLING_JOIN_TIMEOUT_MS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()