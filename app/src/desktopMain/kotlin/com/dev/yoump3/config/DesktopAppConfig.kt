package com.dev.yoump3.config

class DesktopAppConfig : AppConfig {
    override val serverBaseUrl: String =
        System.getProperty("yoump3.serverUrl")
            ?: System.getenv("YOUMP3_SERVER_URL")
            ?: "http://192.168.1.9:8088"
}
