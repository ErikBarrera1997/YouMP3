package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class ErrorStatusUiState(
    val title: String = "SERVICIO NO DISPONIBLE",
    val message: String = "INTENTA MÁS TARDE",
    val buttonText: String = "VOLVER AL INICIO"
)

class ErrorStatusViewModel : ViewModel() {
    var state by mutableStateOf(ErrorStatusUiState())
        private set
}