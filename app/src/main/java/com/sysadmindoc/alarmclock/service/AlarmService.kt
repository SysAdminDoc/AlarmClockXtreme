package com.sysadmindoc.alarmclock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.receiver.DismissReceiver
import com.sysadmindoc.alarmclock.receiver.SnoozeReceiver
import com.sysadmindoc.alarmclock.ui.alarmfiring.AlarmFiringActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground service that handles alarm firing:
 * - Plays alarm sound with gradual volume increase
 * - Triggers vibration
 * - Shows full-screen notification + launches dismiss/snooze Activity
 * - Handles snooze and dismiss actions
 */
@AndroidEntryPoint
class AlarmService : Service() {

    @Inject lateinit var repository: AlarmRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var eventRepository: AlarmEventRepository
    @Inject lateinit var preferencesManager: com.sysadmindoc.alarmclock.data.preferences.PreferencesManager

    companion object {
        const val ACTION_START_ALARM = "com.sysadmindoc.alarmclock.START_ALARM"
        const val ACTION_SNOOZE = "com.sysadmindoc.alarmclock.SNOOZE"
        const val ACTION_DISMISS = "com.sysadmindoc.alarmclock.DISMISS"

        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_UPCOMING = "upcoming_alarm_channel"
        const val CHANNEL_MISSED = "missed_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val MISSED_NOTIFICATION_ID = 1003
        const val DEFAULT_AUTO_SILENCE_MINUTES = 10L

        fun createNotificationChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(alarmChannel)

            val upcomingChannel = NotificationChannel(
                CHANNEL_UPCOMING,
                "Upcoming Alarms",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows upcoming alarm information"
            }
            nm.createNotificationChannel(upcomingChannel)

            val missedChannel = NotificationChannel(
                CHANNEL_MISSED,
                "Missed Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for alarms that were auto-silenced"
            }
            nm.createNotificationChannel(missedChannel)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeJob: Job? = null
    private var currentAlarmId: Long = -1
    private var alarmFiredAt: Long = 0
    private var autoSilenceJob: Job? = null
    private var currentSnoozeCount: Int = 0
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmClockXtreme::AlarmWakeLock"
        ).apply {
            acquire(5 * 60 * 1000L) // 5 minute max
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                if (alarmId != -1L) {
                    if (alarmId != currentAlarmId) {
                        currentSnoozeCount = 0  // Reset for new alarm
                    }
                    currentAlarmId = alarmId
                    alarmFiredAt = System.currentTimeMillis()
                    serviceScope.launch { startAlarm(alarmId) }
                }
            }
            ACTION_SNOOZE -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarmId)
                serviceScope.launch { snoozeAlarm(alarmId) }
            }
            ACTION_DISMISS -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarmId)
                serviceScope.launch { dismissAlarm(alarmId) }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun startAlarm(alarmId: Long) {
        val alarm = repository.getById(alarmId) ?: run {
            stopSelf()
            return
        }

        val notification = buildAlarmNotification(alarm)
        startForeground(NOTIFICATION_ID, notification)

        val firingIntent = Intent(this, AlarmFiringActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        startActivity(firingIntent)

        startAudio(alarm)

        if (alarm.vibrationEnabled) {
            startVibration(alarm.vibrationIntensity)
        }

        // Auto-silence after timeout - records as missed
        val settings = preferencesManager.getCurrentSettings()
        val autoSilenceMinutes = settings.autoSilenceMinutes.toLong()
        if (autoSilenceMinutes > 0) {
            autoSilenceJob = serviceScope.launch {
                kotlinx.coroutines.delay(autoSilenceMinutes * 60 * 1000L)
                val missedAlarm = repository.getById(alarmId)
                if (missedAlarm != null) {
                    recordEvent(missedAlarm, com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent.ACTION_MISSED)
                    showMissedNotification(missedAlarm, autoSilenceMinutes)
                }
                alarmScheduler.handleAlarmFired(alarmId)
                stopAlarmPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun buildAlarmNotification(alarm: Alarm): Notification {
        val fullScreenIntent = Intent(this, AlarmFiringActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, alarm.id.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
        }
        val snoozePi = PendingIntent.getBroadcast(
            this, alarm.id.toInt() + 10000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, DismissReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
        }
        val dismissPi = PendingIntent.getBroadcast(
            this, alarm.id.toInt() + 20000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = String.format("%d:%02d %s",
            if (alarm.hour % 12 == 0) 12 else alarm.hour % 12,
            alarm.minute,
            if (alarm.hour < 12) "AM" else "PM"
        )

        return NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Alarm")
            .setContentText(if (alarm.label.isNotBlank()) alarm.label else timeText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPi, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_alarm, "Snooze", snoozePi)
            .addAction(R.drawable.ic_alarm, "Dismiss", dismissPi)
            .build()
    }

    private fun startAudio(alarm: Alarm) {
        // Silent mode - skip audio entirely
        if (alarm.ringtoneUri == "silent") return

        val uri = if (alarm.ringtoneUri.isNotBlank()) {
            Uri.parse(alarm.ringtoneUri)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()

                if (alarm.overrideSystemVolume) {
                    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    val targetVol = (maxVol * alarm.volume / 100f).toInt().coerceIn(1, maxVol)
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                }

                setVolume(0f, 0f)
                start()
            }

            val fadeInMs = alarm.gradualVolumeSeconds * 1000L
            if (fadeInMs > 0) {
                volumeJob = serviceScope.launch {
                    val steps = 50
                    val stepDelay = fadeInMs / steps
                    for (i in 1..steps) {
                        delay(stepDelay)
                        val volume = i.toFloat() / steps
                        mediaPlayer?.setVolume(volume, volume)
                    }
                }
            } else {
                mediaPlayer?.setVolume(1f, 1f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration(intensity: Int) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = when (intensity) {
            1 -> longArrayOf(0, 200, 1000, 200, 1000)
            else -> longArrayOf(0, 500, 500, 500, 500)
        }
        val amplitudes = when (intensity) {
            1 -> intArrayOf(0, 80, 0, 80, 0)
            else -> intArrayOf(0, 255, 0, 255, 0)
        }

        if (vibrator?.hasAmplitudeControl() == true) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
        } else {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private suspend fun snoozeAlarm(alarmId: Long) {
        autoSilenceJob?.cancel()
        stopAlarmPlayback()
        val alarm = repository.getById(alarmId)
        if (alarm != null) {
            currentSnoozeCount++
            if (currentSnoozeCount > alarm.maxSnoozeCount) {
                // Max snoozes reached - treat as dismiss
                recordEvent(alarm, AlarmEvent.ACTION_DISMISSED)
                alarmScheduler.handleAlarmFired(alarmId)
            } else {
                alarmScheduler.scheduleSnooze(alarm)
                recordEvent(alarm, AlarmEvent.ACTION_SNOOZED)
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun dismissAlarm(alarmId: Long) {
        autoSilenceJob?.cancel()
        stopAlarmPlayback()
        val alarm = repository.getById(alarmId)
        if (alarm != null) {
            recordEvent(alarm, AlarmEvent.ACTION_DISMISSED)
        }
        alarmScheduler.handleAlarmFired(alarmId)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun recordEvent(alarm: Alarm, action: String) {
        val now = System.currentTimeMillis()
        val dayOfWeek = java.time.Instant.ofEpochMilli(now)
            .atZone(java.time.ZoneId.systemDefault())
            .dayOfWeek.value
        eventRepository.record(
            AlarmEvent(
                alarmId = alarm.id,
                alarmLabel = alarm.label,
                scheduledTime = alarm.nextTriggerTime,
                firedAt = alarmFiredAt,
                action = action,
                actionAt = now,
                challengeType = alarm.challengeType,
                dayOfWeek = dayOfWeek
            )
        )
    }

    private fun showMissedNotification(alarm: Alarm, autoSilenceMinutes: Long = DEFAULT_AUTO_SILENCE_MINUTES) {
        val nm = getSystemService(NotificationManager::class.java)
        val hour12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
        val amPm = if (alarm.hour < 12) "AM" else "PM"
        val timeStr = "$hour12:${String.format("%02d", alarm.minute)} $amPm"

        val notification = NotificationCompat.Builder(this, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Missed Alarm")
            .setContentText("${alarm.label.ifBlank { "Alarm" }} at $timeStr was auto-silenced after $autoSilenceMinutes minutes")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        nm.notify(MISSED_NOTIFICATION_ID, notification)
    }

    private fun stopAlarmPlayback() {
        volumeJob?.cancel()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        autoSilenceJob?.cancel()
        stopAlarmPlayback()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
