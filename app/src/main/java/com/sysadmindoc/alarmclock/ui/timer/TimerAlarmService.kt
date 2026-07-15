package com.sysadmindoc.alarmclock.ui.timer

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.service.AlarmAudioRouting
import com.sysadmindoc.alarmclock.service.AlarmService

internal data class TimerAlert(val id: Int, val label: String)

/** Small deterministic state holder so duplicate and simultaneous expiry
 * deliveries can be tested without constructing audio hardware. */
internal class TimerAlertBatch {
    private val alerts = linkedMapOf<Int, TimerAlert>()

    val count: Int get() = alerts.size
    val isEmpty: Boolean get() = alerts.isEmpty()

    fun add(id: Int, label: String): Boolean {
        if (id <= 0) return false
        val isNew = id !in alerts
        alerts[id] = TimerAlert(id, label)
        return isNew
    }

    fun remove(id: Int): TimerAlert? = alerts.remove(id)

    fun snapshot(): List<TimerAlert> = alerts.values.toList()

    fun clear(): List<TimerAlert> = snapshot().also { alerts.clear() }

    fun notificationText(): String = when {
        count > 1 -> "$count timers finished"
        count == 1 -> alerts.values.first().label.ifBlank { "Timer" }
        else -> "Timer"
    }

    fun publicNotificationText(): String = when {
        count > 1 -> "$count timers finished"
        else -> TimerNotifications.GENERIC_TIMER_TEXT
    }
}

/**
 * Sole audible/vibration owner for finished countdown timers. Every expiry is
 * atomically claimed in [TimerStore] before it reaches this service, so UI and
 * AlarmManager delivery races cannot create two players. Multiple timers share
 * one foreground notification, one looping player, and one vibration waveform.
 */
class TimerAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val alerts = TimerAlertBatch()
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { autoSilenceAndStop() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FIRED -> {
                val id = intent.getIntExtra(EXTRA_TIMER_ID, -1)
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                if (id <= 0) {
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                TimerNotifications.cancelTimer(this, id)
                alerts.add(id, label)
                startForegroundAlert()
                ensureSoundPlaying()
                scheduleAutoStop()
            }
            ACTION_DISMISS -> {
                val id = intent.getIntExtra(EXTRA_TIMER_ID, -1)
                if (id > 0) {
                    alerts.remove(id)
                    runCatching { TimerStore(this).remove(id) }
                    TimerNotifications.cancelFinished(this, id)
                }
                if (alerts.isEmpty) stopEverything() else refreshNotification()
            }
            ACTION_RESTART -> {
                val id = intent.getIntExtra(EXTRA_TIMER_ID, -1)
                val restarted = if (id > 0) {
                    runCatching { TimerStore(this).restartFinished(id) }
                        .onFailure { Log.w(TAG, "Could not restart finished timer $id", it) }
                        .getOrNull()
                } else {
                    null
                }
                if (id > 0) {
                    alerts.remove(id)
                    TimerNotifications.cancelFinished(this, id)
                }
                if (restarted != null) {
                    TimerAlarmScheduler.schedule(this, restarted.id, restarted.endElapsedRealtime)
                    TimerNotifications.postRunning(this, restarted)
                }
                if (alerts.isEmpty) stopEverything() else refreshNotification()
            }
            ACTION_DISMISS_ALL, null -> {
                dismissAllAndStop()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundAlert() {
        AlarmService.createNotificationChannels(this)
        runCatching { startForeground(NOTIFICATION_ID, buildNotification()) }
            .onFailure { Log.w(TAG, "startForeground for timer alert failed", it) }
    }

    private fun refreshNotification() {
        runCatching {
            getSystemService(android.app.NotificationManager::class.java)
                ?.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    internal fun buildNotification(
        hidePublicLabel: Boolean = TimerNotifications.shouldHidePublicLabels(this)
    ): android.app.Notification {
        val fullScreen = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            NOTIFICATION_ID + 1,
            Intent(this, TimerAlarmService::class.java).setAction(ACTION_DISMISS_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val restart = alerts.snapshot().singleOrNull()?.let { alert ->
            TimerNotifications.restartPendingIntent(this, alert.id, NOTIFICATION_ID + 2)
        }
        val privateBuilder = NotificationCompat.Builder(this, AlarmService.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(TimerNotifications.FINISHED_TITLE)
            .setContentText(alerts.notificationText())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(R.drawable.ic_alarm, "Stop", stop)
            .also { builder ->
                if (restart != null) {
                    builder.addAction(R.drawable.ic_alarm, getString(R.string.timer_restart), restart)
                }
            }
        val publicBuilder = NotificationCompat.Builder(this, AlarmService.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(TimerNotifications.FINISHED_TITLE)
            .setContentText(alerts.publicNotificationText())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreen)
            .addAction(R.drawable.ic_alarm, "Stop", stop)
            .also { builder ->
                if (restart != null) {
                    builder.addAction(R.drawable.ic_alarm, getString(R.string.timer_restart), restart)
                }
            }
        val publicVersion = publicBuilder.build()
        return TimerNotifications.applyPublicLabelPolicy(
            privateBuilder = privateBuilder,
            hidePublicLabel = hidePublicLabel,
            publicVersion = publicVersion
        ).build()
    }

    private fun ensureSoundPlaying() {
        if (mediaPlayer != null) return
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AlarmAudioRouting.alarmSonificationAttributes())
                setDataSource(this@TimerAlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure {
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
            Log.w(TAG, "Failed to play timer alert sound", it)
        }
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            runCatching {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500, 500, 500), 0))
            }
        }
    }

    private var autoStopScheduled = false
    private fun scheduleAutoStop() {
        if (autoStopScheduled) return
        autoStopScheduled = true
        // Don't ring forever if nobody dismisses it.
        handler.postDelayed(autoStop, AUTO_STOP_MS)
    }

    private fun dismissAllAndStop() {
        val dismissed = alerts.clear()
        runCatching {
            val store = TimerStore(this)
            dismissed.forEach { alert ->
                store.remove(alert.id)
                TimerNotifications.cancelFinished(this, alert.id)
            }
        }
        stopEverything()
    }

    private fun autoSilenceAndStop() {
        // Auto-silence ends sound/vibration but is not a user dismissal. Keep
        // FINISHED records so process recreation still shows what elapsed, and
        // replace the foreground alert with per-timer passive notifications.
        alerts.clear().forEach { alert ->
            TimerNotifications.postFinished(this, alert.id, alert.label)
        }
        stopEverything()
    }

    private fun stopEverything() {
        handler.removeCallbacks(autoStop)
        autoStopScheduled = false
        runCatching {
            mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
        }
        mediaPlayer = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        autoStopScheduled = false
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { vibrator?.cancel() }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TimerAlarmService"
        const val NOTIFICATION_ID = 7_500
        private const val AUTO_STOP_MS = 3L * 60 * 1000
        const val ACTION_FIRED = "com.sysadmindoc.alarmclock.action.TIMER_ALARM_FIRED"
        const val ACTION_DISMISS = "com.sysadmindoc.alarmclock.action.TIMER_ALARM_DISMISS"
        const val ACTION_RESTART = "com.sysadmindoc.alarmclock.action.TIMER_ALARM_RESTART"
        const val ACTION_DISMISS_ALL = "com.sysadmindoc.alarmclock.action.TIMER_ALARM_DISMISS_ALL"
        const val EXTRA_TIMER_ID = "timer_id"
        const val EXTRA_LABEL = "timer_label"

        fun fire(context: Context, timerId: Int, label: String) {
            val intent = Intent(context, TimerAlarmService::class.java)
                .setAction(ACTION_FIRED)
                .putExtra(EXTRA_TIMER_ID, timerId)
                .putExtra(EXTRA_LABEL, label)
            runCatching { context.startForegroundService(intent) }
                .onFailure { Log.w(TAG, "Could not start timer alert service", it) }
        }

        /** Called from the UI when the user dismisses a finished timer, so a
         *  service that is ringing for it (killed-process case) also stops. */
        fun dismiss(context: Context, timerId: Int) {
            val intent = Intent(context, TimerAlarmService::class.java)
                .setAction(ACTION_DISMISS)
                .putExtra(EXTRA_TIMER_ID, timerId)
            runCatching { context.startService(intent) }
        }
    }
}
