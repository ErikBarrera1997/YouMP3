package com.dev.yoump3.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dev.yoump3.AppVisibility
import com.dev.yoump3.MainActivity
import com.dev.yoump3.R

/**
 * Servicio en primer plano que sostiene la extracción mientras la app está en segundo plano.
 *
 * El trabajo real sigue viviendo en [com.dev.yoump3.viewModels.SongInputViewModel]; este servicio
 * no hace la petición. Su única responsabilidad es mantener el proceso vivo y mostrar el progreso,
 * para que Android no se lleve por delante una extracción de varios minutos al pulsar Home o al
 * apagar la pantalla.
 *
 * No se usa `mediaPlayback` a propósito: aquí no hay reproducción todavía. `dataSync` describe
 * mejor una transferencia de datos acotada y es el tipo que Android 14 espera para este caso.
 */
class ExtractionService : Service() {

    companion object {
        const val CHANNEL_ONGOING = "yoump3_extraction"
        const val CHANNEL_DONE = "yoump3_extraction_done"
        const val NOTIFICATION_ONGOING = 1002
        const val NOTIFICATION_DONE = 1003

        private const val ACTION_CANCEL = "com.dev.yoump3.action.CANCEL_EXTRACTION"
        private const val EXTRA_TITLE = "title"
        private const val WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L

        @Volatile
        private var cancelHandler: (() -> Unit)? = null

        fun start(context: Context, title: String, onCancel: () -> Unit) {
            cancelHandler = onCancel
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ExtractionService::class.java)
                        .putExtra(EXTRA_TITLE, title)
                )
            } catch (_: Exception) {
                // Si el sistema no permite arrancar el FGS, la extracción puede seguir mientras la
                // app esté visible. Mejor eso que abortar la extracción por la notificación.
                cancelHandler = null
            }
        }

        fun finish(context: Context, title: String, success: Boolean) {
            cancelHandler = null
            context.stopService(Intent(context, ExtractionService::class.java))
            if (!AppVisibility.isInForeground) {
                showFinishedNotification(context, title, success)
            }
        }

        fun stop(context: Context) {
            cancelHandler = null
            context.stopService(Intent(context, ExtractionService::class.java))
        }

        /**
         * Retira el aviso de resultado. Se llama cuando el usuario vuelve a la app: el aviso solo
         * tiene sentido mientras el resultado está fuera de la vista.
         */
        fun dismissFinished(context: Context) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_DONE)
        }

        /**
         * Limpia restos de un proceso anterior. Android nunca borra por su cuenta las notificaciones
         * de un servicio que murió con el proceso.
         */
        fun cleanupStale(context: Context) {
            cancelHandler = null
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ONGOING)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_DONE)
            context.stopService(Intent(context, ExtractionService::class.java))
        }

        /**
         * Lint no reconoce el chequeo de [canPostNotifications], así que se silencia aquí y se
         * mantienen las dos salvaguardas reales: la comprobación explícita del permiso y el
         * `try/catch` por si el usuario lo revoca entre ambas llamadas.
         */
        @SuppressLint("MissingPermission")
        private fun showFinishedNotification(context: Context, title: String, success: Boolean) {
            if (!canPostNotifications(context)) return
            createChannels(context)

            val text = title.ifBlank { "YOUMP3" }
            val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
                .setSmallIcon(R.drawable.ic_stat_speaker)
                .setContentTitle(if (success) "Extracción completada" else "No se pudo extraer")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(context))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_DONE, notification)
            } catch (_: SecurityException) {
                // Sin permiso en este instante no hay nada que mostrar; el resultado igualmente
                // está en la app, que es donde el usuario lo va a ver.
            }
        }

        private fun canPostNotifications(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        private fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            val ongoing = NotificationChannel(
                CHANNEL_ONGOING,
                "Extracción de audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de la extracción en segundo plano"
                setShowBadge(false)
            }
            val done = NotificationChannel(
                CHANNEL_DONE,
                "Extracción completada",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resultado de una extracción en segundo plano"
            }
            manager.createNotificationChannels(listOf(ongoing, done))
        }

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                10,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun cancelIntent(context: Context): PendingIntent =
            PendingIntent.getService(
                context,
                11,
                Intent(context, ExtractionService::class.java).setAction(ACTION_CANCEL),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            cancelHandler?.invoke()
            cancelHandler = null
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val notification = buildOngoingNotification(title)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ONGOING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ONGOING, notification)
        }

        acquireWakeLock()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        cancelHandler = null
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ONGOING)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildOngoingNotification(title: String): Notification {
        val text = title.ifBlank { "Preparando la extracción…" }
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_speaker)
            .setContentTitle("Extrayendo audio")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(0, 0, true)
            .setContentIntent(openAppIntent(this))
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancelar",
                cancelIntent(this)
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * La extracción puede durar varios minutos con la pantalla apagada. El FGS mantiene vivo el
     * proceso, pero no impide que el CPU entre en suspensión; el wake lock cubre ese hueco y se
     * libera en cuanto la extracción termina.
     */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "YouMp3:extraction"
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }
}
