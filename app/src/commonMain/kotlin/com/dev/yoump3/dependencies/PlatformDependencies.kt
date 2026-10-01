package com.dev.yoump3.dependencies

import com.dev.yoump3.config.AppConfig
import com.dev.yoump3.init.YouMp3Api
import com.dev.yoump3.preferences.AppPreferences
import com.dev.yoump3.services.AudioPlayer
import com.dev.yoump3.services.AudioSaver
import com.dev.yoump3.services.createAudioPlayer

data class PlatformDependencies(
    val appConfig: AppConfig,
    val audioSaver: AudioSaver,
    val appPreferences: AppPreferences,
    // Inyectable para que los tests puedan construir los ViewModel sin el reproductor real, que en
    // Android necesita un Context.
    val audioPlayer: AudioPlayer = createAudioPlayer()
) {
    fun createApi(baseUrl: () -> String): YouMp3Api = YouMp3Api(baseUrl)
}
