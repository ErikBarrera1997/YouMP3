package com.dev.yoump3.viewModels

import com.dev.yoump3.config.AppConfig
import com.dev.yoump3.dependencies.PlatformDependencies
import com.dev.yoump3.error.AppError
import com.dev.yoump3.error.AppErrorKind
import com.dev.yoump3.preferences.AppPreferences
import com.dev.yoump3.services.AudioPlayer
import com.dev.yoump3.services.AudioPlayerState
import com.dev.yoump3.services.AudioSaver
import com.dev.yoump3.services.ExtractedAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El bug que estos tests cubren: el back se decidía con `currentScreen`, que no cambia cuando la
 * ventana de error toma el pantalla, así que en la ventana de error la app se cerraba en vez de
 * hacer lo que hace su botón.
 */
class YouMp3BackTest {

    @Test
    fun `back during startup is not consumed so the app closes`() {
        val vm = newViewModel()
        assertEquals(AppWindow.Init, vm.activeWindow)
        assertFalse(vm.onBackClick())
    }

    @Test
    fun `back on home is not consumed so the app closes`() {
        val vm = newViewModel(connected = true)
        assertEquals(AppWindow.Main, vm.activeWindow)
        assertFalse(vm.onBackClick())
    }

    @Test
    fun `back on song input closes it and stays in the app`() {
        val vm = newViewModel(connected = true)
        vm.onFindButtonClick()
        assertTrue(vm.onBackClick())
        assertEquals(YouMp3Screen.Home, vm.state.currentScreen)
        assertEquals(AppWindow.Main, vm.activeWindow)
    }

    @Test
    fun `back on settings closes it and stays in the app`() {
        val vm = newViewModel(connected = true)
        vm.onSettingsClick()
        assertTrue(vm.onBackClick())
        assertEquals(YouMp3Screen.Home, vm.state.currentScreen)
    }

    @Test
    fun `back on the connection error window does the same as its button`() {
        val vm = newViewModel(connected = true)
        vm.onConnectionFailed(AppError(AppErrorKind.NO_INTERNET))

        assertEquals(AppWindow.ConnectionError, vm.activeWindow)
        assertTrue("no debe cerrar la app, debe reintentar la conexión", vm.onBackClick())

        // Exactamente lo que deja el botón: error limpio y vuelta al arranque para reintentar.
        assertNull(vm.state.connectionError)
        assertFalse(vm.state.appReady)
        assertEquals(AppWindow.Init, vm.activeWindow)
    }

    @Test
    fun `the connection error button and the back leave the same state`() {
        val viaBack = newViewModel(connected = true).also { it.onConnectionFailed(AppError(AppErrorKind.NO_INTERNET)) }
        viaBack.onBackClick()
        val viaButton = newViewModel(connected = true).also { it.onConnectionFailed(AppError(AppErrorKind.NO_INTERNET)) }
        viaButton.onRecheckConnection()

        assertEquals(viaButton.state, viaBack.state)
    }

    @Test
    fun `rechecking from settings drops the error and returns to the startup window`() {
        val vm = newViewModel(connected = true)
        vm.onConnectionFailed(AppError(AppErrorKind.UNEXPECTED_SERVER_ERROR))
        val before = vm.state.connectionCheckToken

        vm.onRecheckConnection()

        assertEquals(before + 1, vm.state.connectionCheckToken)
        assertNull(vm.activeError)
        assertEquals(AppWindow.Init, vm.activeWindow)
    }

    private fun newViewModel(connected: Boolean = false): YouMp3ViewModel {
        val vm = YouMp3ViewModel(fakeDependencies())
        if (connected) vm.onConnectionEstablished()
        return vm
    }

    private fun fakeDependencies() = PlatformDependencies(
        appConfig = object : AppConfig {
            override val serverBaseUrl: String = "https://fake.test"
        },
        audioSaver = object : AudioSaver {
            override suspend fun save(fileName: String, contentType: String, audio: ByteArray): String = ""
        },
        appPreferences = object : AppPreferences {
            override fun getThemeMode(): String? = null
            override fun setThemeMode(mode: String) = Unit
            override fun getServerUrl(): String? = null
            override fun setServerUrl(url: String) = Unit
            override fun clearServerUrl() = Unit
        },
        audioPlayer = object : AudioPlayer {
            override val state = AudioPlayerState()
            override fun load(audio: ExtractedAudio, title: String) = Unit
            override fun toggle() = Unit
            override fun seekTo(positionMs: Long) = Unit
            override fun stop() = Unit
            override fun release() = Unit
        }
    )
}
