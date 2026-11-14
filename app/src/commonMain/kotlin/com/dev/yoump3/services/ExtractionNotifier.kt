package com.dev.yoump3.services

/**
 * Puente entre la extracción (que vive en el ViewModel) y la notificación de plataforma.
 *
 * En Android lo implementa un servicio en primer plano que mantiene vivo el proceso mientras la
 * app está en segundo plano. En Desktop y en los tests es un no-op: allí no hay una extracción en
 * segundo plano que sostener.
 */
interface ExtractionNotifier {
    /**
     * Arranca la notificación de progreso. [onCancel] se invoca si el usuario pulsa "Cancelar" en
     * la notificación; debe dejar el ViewModel en el mismo estado que el botón de cancelar de la
     * pantalla.
     */
    fun start(title: String, onCancel: () -> Unit)

    /**
     * Cierra la notificación de progreso. Si la app está en segundo plano, publica el resultado
     * de la extracción.
     */
    fun finish(title: String, success: Boolean)

    /** Retira la notificación sin publicar ningún resultado. */
    fun stop()
}

object NoOpExtractionNotifier : ExtractionNotifier {
    override fun start(title: String, onCancel: () -> Unit) = Unit
    override fun finish(title: String, success: Boolean) = Unit
    override fun stop() = Unit
}
