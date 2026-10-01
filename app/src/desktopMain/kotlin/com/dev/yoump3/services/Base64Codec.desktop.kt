package com.dev.yoump3.services

import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun decodeBase64(value: String): ByteArray =
    withContext(Dispatchers.IO) { Base64.getDecoder().decode(value) }