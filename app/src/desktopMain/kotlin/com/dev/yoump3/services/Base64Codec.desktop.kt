package com.dev.yoump3.services

import java.util.Base64

actual fun decodeBase64(value: String): ByteArray =
    Base64.getDecoder().decode(value)
