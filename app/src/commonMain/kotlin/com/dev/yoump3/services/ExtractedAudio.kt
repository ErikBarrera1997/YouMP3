package com.dev.yoump3.services

/**
 * Única copia en memoria de una extracción ya terminada.
 *
 * El base64 se decodifica una sola vez, al llegar la respuesta, y este buffer es la única
 * representación que se conserva viva: el reproductor y el guardador leen de aquí, así que
 * ninguno vuelve a decodificar ni a pedir los datos. [wipe] deja el buffer a cero y se invoca
 * en cuanto se sale de la pantalla del reproductor.
 */
class ExtractedAudio(
    val bytes: ByteArray
) {
    val sizeBytes: Long get() = bytes.size.toLong()

    private var wiped = false

    fun wipe() {
        if (!wiped) {
            bytes.fill(0)
            wiped = true
        }
    }
}
