package com.dev.yoump3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        requestNotificationPermissionIfNeeded()
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
        }
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 100
    }
}