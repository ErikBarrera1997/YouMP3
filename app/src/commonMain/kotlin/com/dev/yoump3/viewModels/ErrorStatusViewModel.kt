package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dev.yoump3.error.AppError
import com.dev.yoump3.error.AppErrorKind

data class ErrorStatusUiState(
    val title: String = AppErrorKind.UNEXPECTED_SERVER_ERROR.title,
    val message: String = AppErrorKind.UNEXPECTED_SERVER_ERROR.detail,
    val buttonText: String = "VOLVER AL INICIO"
)

class ErrorStatusViewModel : ViewModel() {
    var state by mutableStateOf(ErrorStatusUiState())
        private set

    /** Todo el copy llega resuelto desde [AppError]; aqui no se interpretan codigos HTTP. */
    fun showError(error: AppError) {
        state = ErrorStatusUiState(title = error.title, message = error.detail)
    }
}
