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

    /**
     * Reproduce una extracción ya terminada. Si el mismo buffer ya está cargado no se vuelve a
     * leer ni a decodificar nada: el reproductor solo consume lo que la extracción entregó.
     */
    fun load(audio: ExtractedAudio, title: String = "")
    fun toggle()
    fun seekTo(positionMs: Long)
    fun stop()
    fun release()
}

expect fun createAudioPlayer(): AudioPlayer
