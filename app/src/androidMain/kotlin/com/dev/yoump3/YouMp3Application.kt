package com.dev.yoump3

import android.app.Application
import android.content.Context

class YouMp3Application : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
    }
}