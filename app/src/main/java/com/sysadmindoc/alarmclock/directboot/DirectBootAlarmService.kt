package com.sysadmindoc.alarmclock.directboot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.R

class DirectBootAlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarmId: Long = -1L
    private var currentTriggerTime: Long = 0L

    private val autoStop = Runnable {
        stopAlarm()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarm()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> startDirectBootAlarm(intent)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    private fun startDirectBootAlarm(intent: Intent) {
        val alarmId = intent.getLongExtra(DirectBootAlarmReceiver.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) {
            stopSelf()
            return
        }

        currentAlarmId = alarmId
        currentTriggerTime = intent.getLongExtra(DirectBootAlarmReceiver.EXTRA_TRIGGER_TIME, 0L)
        val label = intent.getStringExtra(DirectBootAlarmReceiver.EXTRA_LABEL).orEmpty()
        val timeLabel = intent.getStringExtra(DirectBootAlarmReceiver.EXTRA_TIME_LABEL).orEmpty()
        val playDefaultSound = intent.getBooleanExtra(
            DirectBootAlarmReceiver.EXTRA_PLAY_DEFAULT_SOUND,
            true
        )
        val vibrationEnabled = intent.getBooleanExtra(
            DirectBootAlarmReceiver.EXTRA_VIBRATION_ENABLED,
            true
        )

        startForeground(NOTIFICATION_ID, buildNotification(label, timeLabel))
        DirectBootAlarmCache.recordFired(
            context = this,
            alarmId = alarmId,
            triggerTime = currentTriggerTime,
            firedAt = System.currentTimeMillis()
        )

        if (playDefaultSound) startDefaultAlarmSound()
        if (vibrationEnabled) startVibration()
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, AUTO_STOP_MS)
    }

    private fun buildNotification(label: String, timeLabel: String): Notification {
        val stopIntent = Intent(this, DirectBootAlarmService::class.java).apply {
            action = ACTION_STOP
            putExtra(DirectBootAlarmReceiver.EXTRA_ALARM_ID, currentAlarmId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            currentAlarmId.toInt(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = when {
            label.isNotBlank() -> label
            timeLabel.isNotBlank() -> timeLabel
            else -> "Alarm"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Alarm")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_alarm, "Stop", stopPendingIntent)
            .build()
    }

    private fun startDefaultAlarmSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play default direct-boot alarm sound", e)
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
            mediaPlayer = null
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() != true) return

        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 500, 500, 500, 500),
            intArrayOf(0, 255, 0, 255, 0),
            0
        )
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        @Suppress("DEPRECATION")
        vibrator?.vibrate(effect, attributes)
    }

    private fun stopAlarm() {
        handler.removeCallbacks(autoStop)
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        vibrator = null
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (_: Exception) {
        }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        try {
            wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AlarmClockXtreme::DirectBootAlarmWakeLock"
            )?.apply {
                acquire(AUTO_STOP_MS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct-boot wake lock acquisition failed", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Direct Boot Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Fallback alarm before the device is unlocked after reboot"
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.sysadmindoc.alarmclock.directboot.START_ALARM"
        const val ACTION_STOP = "com.sysadmindoc.alarmclock.directboot.STOP_ALARM"

        private const val TAG = "DirectBootAlarmService"
        private const val CHANNEL_ID = "direct_boot_alarm_channel"
        private const val NOTIFICATION_ID = 1011
        private const val AUTO_STOP_MS = 10 * 60 * 1000L
    }
}
