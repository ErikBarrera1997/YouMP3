package com.dev.yoump3.init

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

//Client - Side errors

internal enum class NetworkErrorKind {
    TIMEOUT,
    NO_INTERNET,
    UNKNOWN
}

internal fun classifyNetworkError(e: Throwable): NetworkErrorKind {
    val simpleName = e::class.simpleName ?: ""
    return when {
        e is HttpRequestTimeoutException ||
            e is SocketTimeoutException ||
            simpleName == "SocketTimeoutException" ||
            simpleName == "InterruptedIOException" -> NetworkErrorKind.TIMEOUT

        e is ConnectTimeoutException ||
            simpleName == "ConnectException" ||
            simpleName == "UnknownHostException" ||
            simpleName == "NoRouteToHostException" ||
            simpleName == "PortUnreachableException" ||
            simpleName == "UnresolvedAddressException" -> NetworkErrorKind.NO_INTERNET

        else -> NetworkErrorKind.UNKNOWN
    }
}

internal fun networkErrorMessage(e: Throwable): String? = when (classifyNetworkError(e)) {
    NetworkErrorKind.TIMEOUT -> "Request timed out. Try again later."
    NetworkErrorKind.NO_INTERNET -> "No internet connection. Check your connection."
    NetworkErrorKind.UNKNOWN -> null
}