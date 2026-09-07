package com.dev.yoump3.services

import java.io.ByteArrayInputStream
import java.util.Base64
import javax.sound.sampled.AudioSystem

class DesktopAudioPlayer : AudioPlayer {

    override val state = AudioPlayerState()

    private var clip: javax.sound.sampled.Clip? = null
    private var updateThread: Thread? = null
    private var running = false

    override fun load(audioBase64: String) {
        release()

        val bytes = Base64.getDecoder().decode(audioBase64)
        val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes))
        val loaded = AudioSystem.getClip()
        loaded.open(stream)
        loaded.addLineListener { lineEvent ->
            if (lineEvent.type == javax.sound.sampled.LineEvent.Type.STOP) {
                state.isPlaying = false
                state.positionMs = 0L
                try {
                    loaded.microsecondPosition = 0L
                } catch (_: Exception) {
                }
            }
        }
        clip = loaded
        state.durationMs = loaded.microsecondLength / 1000L
        state.positionMs = 0L
        state.isPlaying = false
    }

    override fun toggle() {
        val c = clip ?: return
        if (state.isPlaying) {
            c.stop()
            state.isPlaying = false
            stopPolling()
        } else {
            c.start()
            state.isPlaying = true
            startPolling()
        }
    }

    override fun seekTo(positionMs: Long) {
        val c = clip ?: return
        val target = positionMs.coerceIn(0L, state.durationMs)
        c.microsecondPosition = target * 1000L
        state.positionMs = target
    }

    override fun stop() {
        clip?.let { c ->
            c.stop()
            c.microsecondPosition = 0L
        }
        state.isPlaying = false
        state.positionMs = 0L
        stopPolling()
    }

    override fun release() {
        stopPolling()
        running = false
        clip?.close()
        clip = null
        state.isPlaying = false
        state.positionMs = 0L
        state.durationMs = 0L
    }

    private fun startPolling() {
        stopPolling()
        running = true
        updateThread = Thread {
            while (running) {
                val c = clip
                if (state.isPlaying && c != null) {
                    state.positionMs = c.microsecondPosition / 1000L
                    if (state.positionMs >= state.durationMs && state.durationMs > 0L) {
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

actual fun createAudioPlayer(): AudioPlayer = DesktopAudioPlayer()