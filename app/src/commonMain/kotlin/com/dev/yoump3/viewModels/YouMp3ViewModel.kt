package com.dev.yoump3.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dev.yoump3.appVersion
import com.dev.yoump3.dependencies.PlatformDependencies
import com.dev.yoump3.error.AppError

enum class YouMp3Screen {
    Home,
    SongInput,
    Settings
}

/**
 * Ventana activa de la app. No es navegable: [Init] y las dos ventanas de error las impone el
 * estado, el usuario no las elige. Existe para que el render y el back consulten la misma cosa.
 */
enum class AppWindow {
    Init,
    Main,
    ConnectionError,
    ServiceError
}

data class YouMp3UiState(
    val appTitle: String = "YOUMP3",
    val title: String = "FIND IT!",
    val footer: String = "BY CLEVER CLOUD · v$appVersion",
    val currentScreen: YouMp3Screen = YouMp3Screen.Home,
    val findButtonPresses: Int = 0,
    val connectionCheckToken: Int = 0,
    val appReady: Boolean = false,
    val connectionError: AppError? = null
)

/**
 * Fuente de verdad de la app: dueño del estado de ventanas, de la navegación interna y de los
 * ViewModel hijos. Que el back viva aquí y no en el composable es lo que evita que se comporte
 * distinto de lo que el usuario ve.
 */
class YouMp3ViewModel(
    dependencies: PlatformDependencies
) : ViewModel() {
    var state by mutableStateOf(YouMp3UiState())
        private set

    val settingsViewModel = SettingsViewModel(
        appPreferences = dependencies.appPreferences,
        appConfig = dependencies.appConfig,
        onServerUrlCommitted = { onRecheckConnection() }
    )

    val api = dependencies.createApi { settingsViewModel.effectiveServerUrl }

    val songInputViewModel = SongInputViewModel(
        api = api,
        audioSaver = dependencies.audioSaver,
        audioPlayer = dependencies.audioPlayer
    )

    val errorStatusViewModel = ErrorStatusViewModel()

    /**
     * Único punto donde se desmonta la app.
     *
     * Solo este ViewModel vive en el `ViewModelStore`, así que es el único que el framework limpia al
     * terminar la Activity. Los hijos se construyen a mano y no heredan de esa limpieza: hay que
     * propagarla a mano o el audio se queda cargado, el listener de segundo plano queda suscrito y los
     * clientes HTTP se acumulan cada vez que se abre y cierra la app en el mismo proceso.
     */
    override fun onCleared() {
        songInputViewModel.dispose()
        api.close()
        super.onCleared()
    }

    /** La ventana que el usuario ve ahora mismo. El render y [onBackClick] leen de aquí. */
    val activeWindow: AppWindow
        get() = when {
            !state.appReady && state.connectionError == null -> AppWindow.Init
            state.connectionError != null -> AppWindow.ConnectionError
            songInputViewModel.state.serviceError != null -> AppWindow.ServiceError
            else -> AppWindow.Main
        }

    /**
     * El error de conexión tiene prioridad: solo puede existir durante el arranque, antes de que
     * haya una ventana principal que pueda fallar.
     */
    val activeError: AppError?
        get() = state.connectionError ?: songInputViewModel.state.serviceError

    fun onConnectionEstablished() {
        state = state.copy(appReady = true)
    }

    fun onConnectionFailed(error: AppError) {
        state = state.copy(connectionError = error)
    }

    fun onFindButtonClick() {
        state = state.copy(
            currentScreen = YouMp3Screen.SongInput,
            findButtonPresses = state.findButtonPresses + 1
        )
    }

    fun onCloseSongInputClick() {
        state = state.copy(currentScreen = YouMp3Screen.Home)
    }

    fun onSettingsClick() {
        state = state.copy(currentScreen = YouMp3Screen.Settings)
    }

    fun onCloseSettingsClick() {
        state = state.copy(currentScreen = YouMp3Screen.Home)
    }

    /** Vuelve a la pantalla de inicio desde donde sea, sin tocar el estado de la otra VM. */
    fun onReturnToHome() {
        state = state.copy(currentScreen = YouMp3Screen.Home)
    }

    /**
     * Descarta cualquier error activo y devuelve la app al arranque para volver a comprobar el
     * servidor. Es también la salida de la ventana de error de conexión, de modo que su botón y el
     * back hacen literalmente lo mismo.
     */
    fun onRecheckConnection() {
        songInputViewModel.onServiceErrorDismissed()
        state = state.copy(
            appReady = false,
            connectionError = null,
            connectionCheckToken = state.connectionCheckToken + 1
        )
    }

    /** Salida de la ventana de error de servicio: limpia el error y vuelve al inicio. */
    fun onDismissServiceError() {
        songInputViewModel.onServiceErrorDismissed()
        onReturnToHome()
    }

    /**
     * Único punto de decisión del back. Primero la ventana de error, porque su salida es la del
     * botón; luego la navegación interna; en Home y durante el arranque devuelve `false` para que
     * la plataforma cierre la app.
     */
    fun onBackClick(): Boolean = when (activeWindow) {
        AppWindow.ConnectionError -> {
            onRecheckConnection()
            true
        }
        AppWindow.ServiceError -> {
            onDismissServiceError()
            true
        }
        AppWindow.Init -> false
        AppWindow.Main -> when (state.currentScreen) {
            YouMp3Screen.SongInput -> {
                onCloseSongInputClick()
                true
            }
            YouMp3Screen.Settings -> {
                onCloseSettingsClick()
                true
            }
            YouMp3Screen.Home -> false
        }
    }
}
