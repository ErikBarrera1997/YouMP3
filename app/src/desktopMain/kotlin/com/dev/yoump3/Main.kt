package com.dev.yoump3

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.dev.yoump3.interfaces.YouMp3App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "YouMp3"
    ) {
        YouMp3App()
    }
}
