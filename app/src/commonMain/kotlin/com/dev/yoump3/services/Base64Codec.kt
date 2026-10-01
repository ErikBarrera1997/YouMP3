package com.dev.yoump3.services

/**
 * Decodifica el audio que el backend devuelve en base64 dentro del cuerpo JSON.
 *
 * Es `suspend` porque decodificar es trabajo de CPU y de asignación, no de main thread: la
 * cadena de entrada pesa varias veces el tamaño del MP3 y hacerlo en el hilo de la UI es justo lo
 * que dispara un `OutOfMemoryError` con el proceso en primer plano. Cada plataforma se encarga de
 * moverlo a su propio dispatcher de E/S.
 */
expect suspend fun decodeBase64(value: String): ByteArray