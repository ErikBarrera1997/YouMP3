package com.dev.yoump3.config

interface AppConfig {
    /** URL del servidor online (`YouMp3ServerUrl`), valor por defecto al arrancar. */
    val serverBaseUrl: String

    /** URL del servidor local (`YouMp3LocalServer`), seleccionable en Ajustes. */
    val localServerBaseUrl: String
}
