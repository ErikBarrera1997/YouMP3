package com.dev.yoump3.services

actual fun decodeBase64(value: String): ByteArray =
    android.util.Base64.decode(value, android.util.Base64.DEFAULT)
