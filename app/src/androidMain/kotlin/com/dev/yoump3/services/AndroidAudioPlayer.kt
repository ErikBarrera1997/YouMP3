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

        activeTitle = title

        val file = File.createTempFile(PREVIEW_PREFIX, PREVIEW_SUFFIX, appContext.cacheDir)
        FileOutputStream(file).use { it.write(audio.bytes) }
        tempFile = file
        loadedAudio = audio

        val mp = MediaPlayer()
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
        player = mp
        state.durationMs = mp.duration.toLong().coerceAtLeast(0L)
        state.positionMs = 0L
        state.isPlaying = false

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
        updateThread?.interrupt()
        updateThread = null
    }
}

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()