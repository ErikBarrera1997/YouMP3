package com.dev.yoump3.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AudioPlayerState {
    var isPlaying by mutableStateOf(false)
    var durationMs by mutableLongStateOf(0L)
    var positionMs by mutableLongStateOf(0L)
}

interface AudioPlayer {
    val state: AudioPlayerState
    fun load(audioBase64: String, title: String = "")
    fun toggle()
    fun seekTo(positionMs: Long)
    fun stop()
    fun release()
}

expect fun createAudioPlayer(): AudioPlayer
