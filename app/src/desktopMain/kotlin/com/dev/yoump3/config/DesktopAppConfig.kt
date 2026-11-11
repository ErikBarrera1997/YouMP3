package com.dev.yoump3.config

class DesktopAppConfig : AppConfig {
    override val serverBaseUrl: String =
        System.getProperty("yoump3.serverUrl")
            ?: System.getenv("YOUMP3_SERVER_URL")
            ?: defaultServerBaseUrl

    override val localServerBaseUrl: String =
        System.getProperty("yoump3.localServerUrl")
            ?: System.getenv("YOUMP3_LOCAL_SERVER_URL")
            ?: defaultLocalServerBaseUrl
}
