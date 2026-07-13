package com.sysadmindoc.alarmclock.ui.timer

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.service.AlarmAudioRouting
import com.sysadmindoc.alarmclock.service.AlarmService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-wide signal: is a live [TimerViewModel] present that will play the
 * finish sound itself? The timer countdown runs on `viewModelScope`, so a
 * foreground OR backgrounded (but not killed) app already alerts via the
 * ViewModel. Only when the process was killed does no ViewModel exist — in that
 * case a fresh process is spawned just for [TimerExpiryReceiver], the flag reads
 * its default `false`, and this service takes over the alerting. This prevents
 * double sound while guaranteeing a killed-app timer is never silent.
 */
object TimerAlertState {
    private val uiAlive = AtomicBoolean(false)
    fun setUiAlive(alive: Boolean) = uiAlive.set(alive)
    fun uiWillHandleSound(): Boolean = uiAlive.get()
}

/**
 * Foreground service that audibly alerts for a finished countdown timer when no
 * live ViewModel can (i.e. the app process was killed). Plays the default alarm
 * tone on a loop with vibration, shows a high-importance full-screen-intent
 * notification with a Stop action, auto-silences after a few minutes, and
 * coalesces multiple simultaneously-finished timers into one alert.
 */
class TimerAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val firedTimerIds = linkedSetOf<Int>()
    private var lastLabel: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FIRED -> {
                val id = intent.getIntExtra(EXTRA_TIMER_ID, -1)
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                if (id > 0) firedTimerIds.add(id)
                if (label.isNotBlank()) lastLabel = label
                startForegroundAlert()
                ensureSoundPlaying()
                scheduleAutoStop()
            }
            ACTION_DISMISS -> {
                val id = intent.getIntExtra(EXTRA_TIMER_ID, -1)
                if (id > 0) {
                    firedTimerIds.remove(id)
                    runCatching { TimerStore(this).remove(id) }
                }
                if (firedTimerIds.isEmpty()) stopEverything() else refreshNotification()
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

    private fun buildNotification(): android.app.Notification {
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
        val count = firedTimerIds.size
        val text = when {
            count > 1 -> "$count timers finished"
            lastLabel.isNotBlank() -> lastLabel
            else -> "Timer"
        }
        return NotificationCompat.Builder(this, AlarmService.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Timer finished")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(R.drawable.ic_alarm, "Stop", stop)
            .build()
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
        android.os.Handler(mainLooper).postDelayed({ dismissAllAndStop() }, AUTO_STOP_MS)
    }

    private fun dismissAllAndStop() {
        val ids = firedTimerIds.toList()
        firedTimerIds.clear()
        runCatching { val store = TimerStore(this); ids.forEach { store.remove(it) } }
        stopEverything()
    }

    private fun stopEverything() {
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
