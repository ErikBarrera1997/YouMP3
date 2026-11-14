package com.dev.yoump3

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.dev.yoump3.services.AndroidAudioPlayer
import com.dev.yoump3.services.ExtractionService

class YouMp3Application : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        AndroidAudioPlayer.sweepStalePreviews(applicationContext)
        // Un proceso que murió con una extracción en curso puede dejar su notificación huérfana.
        ExtractionService.cleanupStale(applicationContext)
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
            AppVisibility.update(visible)
            // El aviso de resultado solo sirve mientras el usuario está fuera. Al volver, el
            // resultado ya está en pantalla y la notificación sobra.
            ExtractionService.dismissFinished(activity.applicationContext)
        }

        override fun onActivityStopped(activity: Activity) {
            visible = (visible - 1).coerceAtLeast(0)
            AppVisibility.update(visible)
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

/**
 * Visibilidad de la app para las piezas que no viven en una Activity.
 *
 * El notificador la consulta al terminar la extracción: si el usuario ya está dentro de la app no
 * tiene sentido publicar una notificación de "completado".
 */
object AppVisibility {
    @Volatile
    var isInForeground: Boolean = false
        private set

    fun update(visibleActivities: Int) {
        isInForeground = visibleActivities > 0
    }
}