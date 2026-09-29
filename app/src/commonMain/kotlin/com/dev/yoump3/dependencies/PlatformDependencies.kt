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
    val appPreferences: AppPreferences
) {
    fun createApi(baseUrl: () -> String): YouMp3Api = YouMp3Api(baseUrl)

    val audioPlayer: AudioPlayer by lazy {
        createAudioPlayer()
    }
}
