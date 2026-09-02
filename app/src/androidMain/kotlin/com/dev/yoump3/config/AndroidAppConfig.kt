package com.dev.yoump3.config

import com.dev.yoump3.BuildConfig

class AndroidAppConfig : AppConfig {
    override val serverBaseUrl: String = BuildConfig.SERVER_BASE_URL
}
