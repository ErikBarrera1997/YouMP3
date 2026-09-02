package com.dev.yoump3.dependencies

import com.dev.yoump3.config.AppConfig
import com.dev.yoump3.init.YouMp3Api
import com.dev.yoump3.preferences.ThemeStore
import com.dev.yoump3.services.AudioSaver

data class PlatformDependencies(
    val appConfig: AppConfig,
    val audioSaver: AudioSaver,
    val themeStore: ThemeStore
) {
    val api: YouMp3Api by lazy { YouMp3Api(appConfig.serverBaseUrl) }
}
