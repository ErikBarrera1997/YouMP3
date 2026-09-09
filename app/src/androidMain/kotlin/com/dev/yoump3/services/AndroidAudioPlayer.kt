package com.dev.yoump3.services

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import androidx.core.app.NotificationManagerCompat
import com.dev.yoump3.YouMp3Application
import java.io.File
import java.io.FileOutputStream

class AndroidAudioPlayer : AudioPlayer {

    companion object {
        @Volatile
        var instance: AndroidAudioPlayer? = null
            private set

        @Volatile
        var activeTitle: String = ""
    }

    override val state = AudioPlayerState()

    private val appContext: Context = YouMp3Application.appContext

    private var player: MediaPlayer? = null
    private var tempFile: File? = null
    private var updateThread: Thread? = null
    private var running = false

    init {
        instance = this
    }

    override fun load(audioBase64: String, title: String) {
        release()

        activeTitle = title

        val bytes = Base64.decode(audioBase64, Base64.DEFAULT)
        val file = File.createTempFile("yoump3_preview", ".mp3")
        FileOutputStream(file).use { it.write(bytes) }
        tempFile = file

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
        state.isPlaying = false
        state.positionMs = 0L
        state.durationMs = 0L
        activeTitle = ""
        stopForegroundNotification()
    }

    private fun refreshNotification() {
        if (MediaPlayerService.isRunning) {
            NotificationManagerCompat.from(appContext)
                .notify(MediaPlayerService.NOTIFICATION_ID, MediaPlayerService.buildNotification(appContext))
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