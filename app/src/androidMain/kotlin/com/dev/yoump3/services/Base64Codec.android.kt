package com.dev.yoump3.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun decodeBase64(value: String): ByteArray =
    withContext(Dispatchers.IO) { android.util.Base64.decode(value, android.util.Base64.DEFAULT) }