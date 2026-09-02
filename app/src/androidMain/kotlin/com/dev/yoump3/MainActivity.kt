package com.dev.yoump3

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dev.yoump3.config.AndroidAppConfig
import com.dev.yoump3.dependencies.PlatformDependencies
import com.dev.yoump3.interfaces.YouMp3App
import com.dev.yoump3.preferences.AndroidThemeStore
import com.dev.yoump3.services.AndroidAudioSaver
import com.dev.yoump3.viewModels.YouMp3ViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { YouMp3ViewModel(AndroidThemeStore(applicationContext)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!viewModel.onBackClick()) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        )
        setContent {
            YouMp3App(
                dependencies = PlatformDependencies(
                    appConfig = AndroidAppConfig(),
                    audioSaver = AndroidAudioSaver(applicationContext),
                    themeStore = AndroidThemeStore(applicationContext)
                ),
                viewModel = viewModel,
                onExit = { finishAffinity() }
            )
        }
    }
}
