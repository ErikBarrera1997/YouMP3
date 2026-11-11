package com.dev.yoump3.viewModels

import com.dev.yoump3.config.AppConfig
import com.dev.yoump3.init.YouMp3Api
import com.dev.yoump3.preferences.AppPreferences
import com.dev.yoump3.services.AudioPlayer
import com.dev.yoump3.services.AudioPlayerState
import com.dev.yoump3.services.AudioSaver
import com.dev.yoump3.services.ExtractedAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El permiso de uso de datos se confirma en la app, se persiste y bloquea la búsqueda mientras
 * esté rechazado. El backend ya no expone un endpoint de aviso: esta es la fuente de verdad.
 */
class DataUsageConsentTest {

    @Test
    fun `a first search shows the notice instead of calling the API`() {
        val harness = newHarness()
        try {
            harness.viewModel.onSongQueryChange("never gonna give you up")
            harness.viewModel.onSearchClick()

            assertTrue(harness.viewModel.state.showDataUsageConsent)
            assertFalse(harness.viewModel.state.dataUsageConsentDeclined)
            assertFalse(harness.viewModel.state.isLoading)
            assertNull(harness.preferences.storedConsent)
        } finally {
            harness.close()
        }
    }

    @Test
    fun `declining persists the rejection and blocks the next search`() {
        val harness = newHarness()
        try {
            harness.viewModel.onSongQueryChange("never gonna give you up")
            harness.viewModel.onSearchClick()
            harness.viewModel.onDataUsageConsentDeclined()

            assertEquals(false, harness.preferences.storedConsent)
            assertFalse(harness.viewModel.state.showDataUsageConsent)
            assertFalse(harness.viewModel.state.isLoading)
            assertNotNull(harness.viewModel.state.errorMessage)
            assertTrue(harness.viewModel.state.errorMessage!!.contains("Ajustes"))

            harness.viewModel.onSearchClick()

            assertTrue(harness.viewModel.state.showDataUsageConsent)
            assertTrue(harness.viewModel.state.dataUsageConsentDeclined)
            assertFalse(harness.viewModel.state.isLoading)
        } finally {
            harness.close()
        }
    }

    @Test
    fun `accepting the notice persists the permission`() {
        val harness = newHarness()
        try {
            harness.viewModel.onDataUsageConsentAccepted()

            assertEquals(true, harness.preferences.storedConsent)
            assertFalse(harness.viewModel.state.showDataUsageConsent)
        } finally {
            harness.close()
        }
    }

    @Test
    fun `the settings toggle persists and refreshes changes made elsewhere`() {
        val preferences = FakePreferences()
        val settings = SettingsViewModel(preferences, FakeAppConfig)

        assertFalse(settings.state.dataUsageConsent)
        assertFalse(settings.state.dataUsageConsentConfigured)

        settings.onDataUsageConsentChange(true)

        assertEquals(true, preferences.storedConsent)
        assertTrue(settings.state.dataUsageConsent)
        assertTrue(settings.state.dataUsageConsentConfigured)

        // El aviso de búsqueda puede cambiar el permiso mientras Ajustes no está visible.
        preferences.storedConsent = false
        settings.refreshDataUsageConsent()

        assertFalse(settings.state.dataUsageConsent)
        assertTrue(settings.state.dataUsageConsentConfigured)
    }

    private fun newHarness(): Harness {
        val preferences = FakePreferences()
        val api = YouMp3Api { "https://fake.test" }
        val viewModel = SongInputViewModel(
            api = api,
            audioSaver = object : AudioSaver {
                override suspend fun save(
                    fileName: String,
                    contentType: String,
                    audio: ByteArray
                ): String = ""
            },
            audioPlayer = object : AudioPlayer {
                override val state = AudioPlayerState()
                override fun load(audio: ExtractedAudio, title: String) = Unit
                override fun toggle() = Unit
                override fun seekTo(positionMs: Long) = Unit
                override fun stop() = Unit
                override fun release() = Unit
            },
            appPreferences = preferences
        )
        return Harness(preferences, api, viewModel)
    }

    private class Harness(
        val preferences: FakePreferences,
        private val api: YouMp3Api,
        val viewModel: SongInputViewModel
    ) {
        fun close() {
            viewModel.dispose()
            api.close()
        }
    }

    private class FakePreferences : AppPreferences {
        var storedThemeMode: String? = null
        var storedServerUrl: String? = null
        var storedConsent: Boolean? = null

        override fun getThemeMode(): String? = storedThemeMode
        override fun setThemeMode(mode: String) {
            storedThemeMode = mode
        }

        override fun getServerUrl(): String? = storedServerUrl
        override fun setServerUrl(url: String) {
            storedServerUrl = url
        }

        override fun clearServerUrl() {
            storedServerUrl = null
        }

        override fun getDataUsageConsent(): Boolean? = storedConsent
        override fun setDataUsageConsent(granted: Boolean) {
            storedConsent = granted
        }
    }

    private object FakeAppConfig : AppConfig {
        override val serverBaseUrl: String = "https://fake.test"
        override val localServerBaseUrl: String = "http://127.0.0.1:8088"
    }
}
