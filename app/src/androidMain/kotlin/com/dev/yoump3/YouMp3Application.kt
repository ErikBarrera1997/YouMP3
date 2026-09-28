package com.dev.yoump3

import android.app.Application
import android.content.Context
import com.dev.yoump3.services.AndroidAudioPlayer

class YouMp3Application : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        AndroidAudioPlayer.sweepStalePreviews(applicationContext)
    }

    companion object {
        lateinit var appContext: Context
    }
}