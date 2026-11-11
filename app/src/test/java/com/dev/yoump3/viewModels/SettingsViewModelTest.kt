package com.dev.yoump3.viewModels

import com.dev.yoump3.config.AppConfig
import com.dev.yoump3.preferences.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun `defaults to the online server from app config`() {
        val settings = SettingsViewModel(FakePreferences(), FakeAppConfig)

        assertEquals(ServerTarget.ONLINE, settings.state.serverTarget)
        assertEquals("https://online.test", settings.effectiveServerUrl)
    }

    @Test
    fun `restores the local server when it was persisted`() {
        val preferences = FakePreferences().apply {
            storedServerUrl = "http://127.0.0.1:8088"
        }
        val settings = SettingsViewModel(preferences, FakeAppConfig)

        assertEquals(ServerTarget.LOCAL, settings.state.serverTarget)
        assertEquals("http://127.0.0.1:8088", settings.effectiveServerUrl)
    }

    @Test
    fun `switching to local persists the url and notifies listeners`() {
        val preferences = FakePreferences()
        var committed = 0
        val settings = SettingsViewModel(preferences, FakeAppConfig) { committed += 1 }

        settings.onServerTargetChange(ServerTarget.LOCAL)

        assertEquals(ServerTarget.LOCAL, settings.state.serverTarget)
        assertEquals("http://127.0.0.1:8088", preferences.storedServerUrl)
        assertEquals("http://127.0.0.1:8088", settings.effectiveServerUrl)
        assertEquals(1, committed)
    }

    @Test
    fun `switching back to online clears the override`() {
        val preferences = FakePreferences().apply {
            storedServerUrl = "http://127.0.0.1:8088"
        }
        val settings = SettingsViewModel(preferences, FakeAppConfig)

        settings.onServerTargetChange(ServerTarget.ONLINE)

        assertEquals(ServerTarget.ONLINE, settings.state.serverTarget)
        assertNull(preferences.storedServerUrl)
        assertEquals("https://online.test", settings.effectiveServerUrl)
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
        override val serverBaseUrl: String = "https://online.test"
        override val localServerBaseUrl: String = "http://127.0.0.1:8088"
    }
}
