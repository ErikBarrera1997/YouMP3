package com.dev.yoump3.consent

/**
 * Texto y claves del permiso de uso de datos. Vive en la app, no en el backend: el aviso se
 * muestra antes de la primera búsqueda y la decisión se guarda en las preferencias del dispositivo.
 *
 * Sube [VERSION] cuando cambie el texto o el alcance del permiso. Con eso se puede volver a pedir
 * el consentimiento a quien aceptó una versión anterior.
 */
object DataUsageConsent {

    const val VERSION = "1"

    const val TITLE = "USO DE DATOS Y COOKIES"

    const val MESSAGE =
        "Para extraer audio de YouTube, la app puede usar las cookies de tu sesión de YouTube. " +
            "Antes de la primera búsqueda necesitamos tu permiso."

    val DETAILS = listOf(
        "Tus cookies se envían solo con la petición que estás haciendo y se usan para " +
            "autenticar la búsqueda o la extracción en YouTube.",
        "No se guardan en la base de datos ni se escriben en los logs.",
        "Se eliminan del servidor al terminar la petición.",
        "Puedes deshabilitar el permiso en Ajustes cuando quieras. Mientras esté deshabilitado " +
            "no se realizarán búsquedas."
    )

    const val ACCEPT_LABEL = "ACEPTAR Y BUSCAR"
    const val DECLINE_LABEL = "AHORA NO"

    const val BLOCKED_MESSAGE =
        "La búsqueda está bloqueada. Habilita el permiso de uso de datos en Ajustes para buscar."

    const val DECLINED_HINT =
        "Sin permiso no se realizarán búsquedas. Puedes aceptarlo ahora o habilitarlo después " +
            "desde Ajustes."

    const val SETTINGS_TITLE = "Permiso de uso de datos"
    const val SETTINGS_ENABLED_DESCRIPTION =
        "Las cookies de tu sesión se usarán al buscar en YouTube."
    const val SETTINGS_NOT_CONFIGURED_DESCRIPTION =
        "Aún no has decidido. Se te pedirá permiso antes de la primera búsqueda."
    const val SETTINGS_DISABLED_DESCRIPTION =
        "No se usarán cookies y la búsqueda queda bloqueada hasta habilitarlo."
    const val SETTINGS_ENABLED_STATUS = "PERMISO CONCEDIDO"
    const val SETTINGS_NOT_CONFIGURED_STATUS =
        "PERMISO SIN CONFIGURAR · SE PEDIRÁ AL BUSCAR"
    const val SETTINGS_DISABLED_STATUS = "PERMISO DESHABILITADO · BÚSQUEDA BLOQUEADA"
    const val SETTINGS_HELP =
        "Puedes cambiar esta decisión cuando quieras. Si lo deshabilitas, la búsqueda no se " +
            "realizará hasta que lo vuelvas a habilitar."
}
