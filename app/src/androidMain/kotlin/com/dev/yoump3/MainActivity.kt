package com.dev.yoump3

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dev.yoump3.interfaces.YouMp3App
import com.dev.yoump3.services.AppContextProvider
import com.dev.yoump3.viewModels.YouMp3ViewModel

class MainActivity : ComponentActivity() {
    private val viewModel = YouMp3ViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextProvider.context = applicationContext
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
            YouMp3App(viewModel = viewModel, onExit = { finishAffinity() })
        }
    }
}
