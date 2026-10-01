package com.dev.yoump3

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.dev.yoump3.services.AndroidAudioPlayer

class YouMp3Application : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        AndroidAudioPlayer.sweepStalePreviews(applicationContext)
        registerActivityLifecycleCallbacks(AppBackgroundWatcher())
    }

    /**
     * Cuenta las Activities visibles para saber cuándo la app entera pasa a segundo plano.
     *
     * Contarlas evita traer `ProcessLifecycleOwner`, que resolvería lo mismo con otra dependencia.
     * En un cambio de configuración la nueva Activity arranca antes de que la anterior se detenga, así
     * que la cuenta va de 1 a 2 y vuelve a 1 sin tocar cero: una rotación no suelta nada por error.
     */
    private class AppBackgroundWatcher : Application.ActivityLifecycleCallbacks {
        private var visible = 0

        override fun onActivityStarted(activity: Activity) {
            visible++
        }

        override fun onActivityStopped(activity: Activity) {
            visible = (visible - 1).coerceAtLeast(0)
            if (visible == 0) AppBackgroundSignal.onAppBackgrounded()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    companion object {
        lateinit var appContext: Context
    }
}