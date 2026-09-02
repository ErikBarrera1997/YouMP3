package com.dev.yoump3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dev.yoump3.config.DesktopAppConfig
import com.dev.yoump3.dependencies.PlatformDependencies
import com.dev.yoump3.interfaces.YouMp3App
import com.dev.yoump3.preferences.DesktopThemeStore
import com.dev.yoump3.services.DesktopAudioSaver
import com.dev.yoump3.viewModels.YouMp3ViewModel

private val appViewModelStore = ViewModelStore()

@Composable
private fun ProvideViewModelStoreOwner(content: @Composable () -> Unit) {
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = appViewModelStore
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "YouMp3"
    ) {
        ProvideViewModelStoreOwner {
            val deps = remember {
                PlatformDependencies(
                    appConfig = DesktopAppConfig(),
                    audioSaver = DesktopAudioSaver(),
                    themeStore = DesktopThemeStore()
                )
            }
            val viewModel = viewModel { YouMp3ViewModel(deps.themeStore) }
            YouMp3App(
                dependencies = deps,
                viewModel = viewModel,
                onExit = ::exitApplication
            )
        }
    }
}
