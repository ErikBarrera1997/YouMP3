package com.dev.yoump3

/**
 * Aviso de que la app entera ha salido a segundo plano.
 *
 * Existe porque ningún hook del ciclo de vida de `Activity` sirve para eso: cuando el usuario cierra
 * la app, Android mata el proceso sin ejecutar `onDestroy`, y un `onTerminate` de `Application` no se
 * llama nunca en un dispositivo real. Lo único que sí se puede observar es cuándo desaparece la
 * última pantalla visible, que es justo el momento en que el proceso pasa a ser ajusticible por el
 * sistema y en el que soltar memoria evita que lo mate él primero.
 *
 * Es una pieza mínima a propósito. Cada plataforma alimenta la señal: Android contando las
 * Activities visibles, desktop al cerrar la ventana. Solo se usa desde el hilo principal.
 */
object AppBackgroundSignal {

    fun interface Listener {
        fun onAppBackgrounded()
    }

    private val listeners = mutableListOf<Listener>()

    fun add(listener: Listener) {
        listeners += listener
    }

    fun remove(listener: Listener) {
        listeners -= listener
    }

    /** Lo invoca la plataforma, nunca el código común. */
    fun onAppBackgrounded() {
        // Copia antes de recorrer: un listener puede desuscribirse al ser notificado, y modificar la
        // lista mientras se itera lanzaría `ConcurrentModificationException`.
        listeners.toList().forEach { it.onAppBackgrounded() }
    }
}