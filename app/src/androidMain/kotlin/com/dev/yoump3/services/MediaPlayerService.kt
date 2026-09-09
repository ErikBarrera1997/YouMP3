package com.dev.yoump3.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dev.yoump3.MainActivity
import com.dev.yoump3.R

class MediaPlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "yoump3_media_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE = "com.dev.yoump3.action.TOGGLE"
        const val ACTION_STOP = "com.dev.yoump3.action.STOP"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MediaPlayerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MediaPlayerService::class.java))
        }

        fun buildNotification(context: Context): Notification {
            val player = AndroidAudioPlayer.instance
            val isPlaying = player?.state?.isPlaying == true
            val title = AndroidAudioPlayer.activeTitle.ifBlank { "YOUMP3" }

            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val toggleIntent = PendingIntent.getService(
                context,
                1,
                Intent(context, MediaPlayerService::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val stopIntent = PendingIntent.getService(
                context,
                2,
                Intent(context, MediaPlayerService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_speaker)
                .setContentTitle(title)
                .setContentText(if (isPlaying) "Reproduciendo en YOUMP3" else "Pausado")
                .setOngoing(isPlaying)
                .setShowWhen(false)
                .setContentIntent(contentIntent)
                .addAction(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPlaying) "Pausa" else "Reproducir",
                    toggleIntent
                )
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopIntent)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1)
                )
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> AndroidAudioPlayer.instance?.toggle()
            ACTION_STOP -> {
                AndroidAudioPlayer.instance?.stop()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification(this))
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reproducción de audio",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificación del reproductor activo"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}