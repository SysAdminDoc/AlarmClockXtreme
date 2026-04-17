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
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.receiver.DismissReceiver
import com.sysadmindoc.alarmclock.receiver.SnoozeReceiver
import com.sysadmindoc.alarmclock.ui.alarmfiring.AlarmFiringActivity
import com.sysadmindoc.alarmclock.ui.alarmfiring.MorningBriefingActivity
import com.sysadmindoc.alarmclock.worker.WakeConfirmWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
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
    @Inject lateinit var webhookService: WebhookService

    companion object {
        const val ACTION_START_ALARM = "com.sysadmindoc.alarmclock.START_ALARM"
        const val ACTION_SNOOZE = "com.sysadmindoc.alarmclock.SNOOZE"
        const val ACTION_DISMISS = "com.sysadmindoc.alarmclock.DISMISS"
        const val EXTRA_CUSTOM_SNOOZE_MINUTES = "custom_snooze_minutes"

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
    private var backupSoundJob: Job? = null
    private var flashlightJob: Job? = null
    private var currentSnoozeCount: Int = 0
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmClockXtreme::AlarmWakeLock"
        ).apply {
            acquire(30 * 60 * 1000L) // 30 minutes — covers max auto-silence; released in onDestroy()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                if (alarmId != -1L) {
                    // Cancel any prior auto-silence/fade jobs before starting new alarm
                    autoSilenceJob?.cancel()
                    volumeJob?.cancel()
                    stopAlarmPlayback()
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
                val customMinutes = intent.getIntExtra(EXTRA_CUSTOM_SNOOZE_MINUTES, -1)
                serviceScope.launch { snoozeAlarm(alarmId, if (customMinutes > 0) customMinutes else null) }
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
        isForeground = true

        val firingIntent = Intent(this, AlarmFiringActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        startActivity(firingIntent)

        startAudio(alarm)

        if (alarm.vibrationEnabled) {
            startVibration(alarm)
        }

        // F8: Webhook on alarm fire
        serviceScope.launch {
            webhookService.fire("fired", alarm.id, alarm.label, formatAlarmTime(alarm))
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
                    // v1.4.0: Repeat missed alarms — record the alarm id / timestamp
                    // so MissedAlarmUnlockReceiver can re-fire when the user unlocks
                    // soon after. Guard on the user-level preference.
                    if (settings.repeatMissedAlarms) {
                        getSharedPreferences("missed_alarm_state", MODE_PRIVATE)
                            .edit()
                            .putLong("last_missed_at", System.currentTimeMillis())
                            .putLong("last_missed_id", missedAlarm.id)
                            .apply()
                    }
                }
                alarmScheduler.handleAlarmFired(alarmId)
                stopAlarmPlayback()
                if (isForeground) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    isForeground = false
                }
                stopSelf()
            }
        }

        // v1.2.0: Backup sound escalation
        if (alarm.backupSoundEnabled) {
            backupSoundJob = serviceScope.launch {
                delay(alarm.backupSoundDelaySec * 1000L)
                // Escalate: set volume to max and switch to system alarm tone
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                mediaPlayer?.setVolume(1f, 1f)
            }
        }

        // v1.2.0: Flashlight strobe
        if (alarm.flashlightStrobe) {
            startFlashlightStrobe()
        }

        // v1.2.0: Guardian Angel — schedule emergency contact call if not dismissed.
        // Floor at 30 s so a misconfigured 0-second delay can't fire the guardian
        // before the user has any reasonable chance to interact with the alarm.
        if (alarm.guardianEnabled && alarm.guardianPhone.isNotBlank()) {
            val guardianDelay = alarm.guardianDelaySec.coerceAtLeast(30).toLong()
            val guardianData = workDataOf(
                "alarm_id" to alarm.id,
                "guardian_phone" to alarm.guardianPhone,
                "alarm_label" to alarm.label
            )
            val guardianRequest = OneTimeWorkRequestBuilder<com.sysadmindoc.alarmclock.worker.GuardianWorker>()
                .setInitialDelay(guardianDelay, TimeUnit.SECONDS)
                .setInputData(guardianData)
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "guardian_${alarm.id}",
                ExistingWorkPolicy.REPLACE,
                guardianRequest
            )
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
            .addAction(R.drawable.ic_alarm, "Snooze ${alarm.snoozeDurationMinutes}m", snoozePi)
            .addAction(R.drawable.ic_alarm, "Dismiss", dismissPi)
            .build()
    }

    private fun startAudio(alarm: Alarm) {
        // Silent mode - skip audio entirely
        if (alarm.ringtoneUri == "silent") return

        // v1.4.0: Random pick from a ringtone pool (comma-separated URIs).
        // Picking at this layer means the pool wins over a static ringtoneUri
        // so the user doesn't habituate to a single sound. We copy the alarm
        // to preserve the pool for future fires and let the rest of the method
        // operate on a single resolved URI.
        val pooledAlarm = alarm.ringtonePool.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }
            ?.let { pool -> alarm.copy(ringtoneUri = pool.random()) }
            ?: alarm

        startAudioInternal(pooledAlarm)
    }

    private fun startAudioInternal(alarm: Alarm) {

        // F14: Spotify ringtone — open Spotify URI and skip MediaPlayer.
        // Only accept canonical Spotify schemes ("spotify:..." or
        // "https://open.spotify.com/...") so a typo'd setting can't accidentally
        // open the browser or another deep-linked app at alarm time.
        val spotifyUri = alarm.spotifyUri.trim()
        if (spotifyUri.isNotBlank() && (
                spotifyUri.startsWith("spotify:", ignoreCase = true) ||
                spotifyUri.startsWith("https://open.spotify.com/", ignoreCase = true)
            )
        ) {
            try {
                val parsed = Uri.parse(spotifyUri)
                val spotifyIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    parsed
                ).apply {
                    setPackage("com.spotify.music")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("android.intent.extra.START_PLAYBACK", true)
                }
                // Verify Spotify is actually installed before launching; if not,
                // fall through to the default ringtone path so the alarm still
                // makes noise instead of silently no-oping.
                if (spotifyIntent.resolveActivity(packageManager) != null) {
                    startActivity(spotifyIntent)
                    return  // Spotify handles playback; no MediaPlayer needed
                }
            } catch (_: Exception) {
                // Spotify not installed or URI invalid — fall through to default audio
            }
        }

        // v1.2.0: Internet radio stream. Defensive: only accept http(s) URLs so a
        // malformed setting can't crash MediaPlayer with an unknown scheme.
        val radioUrl = alarm.internetRadioUrl.trim()
        if (radioUrl.isNotBlank() && (radioUrl.startsWith("http://", true) || radioUrl.startsWith("https://", true))) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                    )
                    setDataSource(radioUrl)
                    isLooping = false  // Streams don't loop
                    setOnPreparedListener { mp ->
                        mp.start()
                        if (alarm.overrideSystemVolume) {
                            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                            val targetVol = (maxVol * alarm.volume / 100f).toInt().coerceIn(1, maxVol)
                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                        }
                    }
                    // Without an OnErrorListener, a stream failure (DNS, 404, codec
                    // mismatch) results in a silent alarm — fall back to the device
                    // default ringtone via the standard path below.
                    setOnErrorListener { mp, _, _ ->
                        try { mp.release() } catch (_: Exception) {}
                        if (mediaPlayer === mp) mediaPlayer = null
                        // Re-enter startAudio without the radio URL so the default
                        // ringtone path runs. Done on the service scope so the
                        // OnErrorListener returns immediately.
                        serviceScope.launch {
                            startAudio(alarm.copy(internetRadioUrl = ""))
                        }
                        true
                    }
                    prepareAsync()
                }
                return  // Radio handles playback
            } catch (_: Exception) {
                // Fall through to default audio
                try { mediaPlayer?.release() } catch (_: Exception) {}
                mediaPlayer = null
            }
        }

        val uri = if (alarm.ringtoneUri.isNotBlank()) {
            runCatching { Uri.parse(alarm.ringtoneUri) }.getOrNull()
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } ?: run {
            // Stripped-down AOSP / managed-profile devices may not report any
            // default ringtone. Don't crash — leave the alarm silent (the
            // notification + vibration + flashlight still fire) and bail out.
            return
        }

        try {
            val fadeInMs = alarm.gradualVolumeSeconds * 1000L
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                )
                setDataSource(applicationContext, uri)
                // v1.4.0: "Dismiss at ringtone end" — honour a song/ringtone's
                // natural length by disabling the loop and auto-dismissing
                // when playback completes.
                isLooping = !alarm.dismissAtRingtoneEnd
                if (alarm.dismissAtRingtoneEnd) {
                    setOnCompletionListener {
                        val id = currentAlarmId
                        if (id != -1L) {
                            serviceScope.launch { dismissAlarm(id) }
                        }
                    }
                }
                prepare()

                if (alarm.overrideSystemVolume) {
                    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    val targetVol = (maxVol * alarm.volume / 100f).toInt().coerceIn(1, maxVol)
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                }

                // For a fade-in we start at 0 so the first sample isn't the loud
                // attack of the ringtone; otherwise start at full so the user
                // hears the alarm immediately at the configured level.
                if (fadeInMs > 0) setVolume(0f, 0f) else setVolume(1f, 1f)
                start()
            }

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
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to default alarm sound
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (fallbackUri != null) {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        )
                        setDataSource(applicationContext, fallbackUri)
                        isLooping = true
                        prepare()
                        setVolume(1f, 1f)
                        start()
                    }
                }
            } catch (_: Exception) {
                // Last resort - alarm fires silently but notification is still shown
            }
        }
    }

    private fun startVibration(alarm: Alarm) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        // Devices without a vibrator (some tablets, Wear shells, emulators) — skip silently.
        if (vibrator == null || vibrator?.hasVibrator() != true) return

        val (pattern, amplitudes) = when (alarm.vibrationPattern) {
            "gentle" -> longArrayOf(0, 200, 1200, 200, 1200) to intArrayOf(0, 60, 0, 60, 0)
            "heartbeat" -> longArrayOf(0, 150, 100, 150, 800) to intArrayOf(0, 200, 0, 255, 0)
            "escalating" -> longArrayOf(0, 200, 600, 300, 500, 400, 400, 500, 300) to
                intArrayOf(0, 60, 0, 120, 0, 180, 0, 255, 0)
            "sos" -> longArrayOf(0, 150, 100, 150, 100, 150, 300, 400, 100, 400, 100, 400, 300, 150, 100, 150, 100, 150, 600) to
                intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0)
            else -> { // "default"
                when (alarm.vibrationIntensity) {
                    1 -> longArrayOf(0, 200, 1000, 200, 1000) to intArrayOf(0, 80, 0, 80, 0)
                    else -> longArrayOf(0, 500, 500, 500, 500) to intArrayOf(0, 255, 0, 255, 0)
                }
            }
        }

        if (vibrator?.hasAmplitudeControl() == true) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
        } else {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private suspend fun snoozeAlarm(alarmId: Long, customMinutes: Int? = null) {
        autoSilenceJob?.cancel()
        backupSoundJob?.cancel()
        flashlightJob?.cancel()
        volumeJob?.cancel()
        stopAlarmPlayback()
        val alarm = repository.getById(alarmId)
        if (alarm != null) {
            // Snoozing means the user interacted with the alarm, so cancel any pending
            // Guardian Angel call/SMS — they're plainly awake enough to hit snooze.
            // The next fire after snooze will re-arm Guardian if still configured.
            if (alarm.guardianEnabled) {
                WorkManager.getInstance(applicationContext)
                    .cancelUniqueWork("guardian_${alarm.id}")
            }
            currentSnoozeCount++
            if (alarm.maxSnoozeCount > 0 && currentSnoozeCount > alarm.maxSnoozeCount) {
                // Max snoozes reached - treat as dismiss
                recordEvent(alarm, AlarmEvent.ACTION_DISMISSED)
                alarmScheduler.handleAlarmFired(alarmId)
            } else {
                val effectiveSnooze = if (alarm.progressiveSnooze && customMinutes == null) {
                    (alarm.snoozeDurationMinutes - currentSnoozeCount).coerceAtLeast(1)
                } else customMinutes
                alarmScheduler.scheduleSnooze(alarm, effectiveSnooze)
                recordEvent(alarm, AlarmEvent.ACTION_SNOOZED)
            }
            // F8: Webhook on snooze
            serviceScope.launch {
                webhookService.fire("snoozed", alarm.id, alarm.label, formatAlarmTime(alarm))
            }
        }
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
        stopSelf()
    }

    private suspend fun dismissAlarm(alarmId: Long) {
        autoSilenceJob?.cancel()
        backupSoundJob?.cancel()
        flashlightJob?.cancel()
        volumeJob?.cancel()
        stopAlarmPlayback()
        val alarm = repository.getById(alarmId)
        if (alarm != null) {
            recordEvent(alarm, AlarmEvent.ACTION_DISMISSED)

            // F8: Webhook on dismiss
            serviceScope.launch {
                webhookService.fire("dismissed", alarm.id, alarm.label, formatAlarmTime(alarm))
            }

            // F11: TTS morning announcement
            if (alarm.ttsEnabled) {
                speakMorningAnnouncement(alarm)
            }

            // F12: Morning briefing screen
            showMorningBriefing(alarm)

            // F5: Post-alarm wake confirmation
            if (alarm.wakeConfirmEnabled) {
                scheduleWakeConfirmation(alarm)
            }

            // v1.2.0: Cancel guardian if active (alarm was dismissed in time)
            WorkManager.getInstance(applicationContext).cancelUniqueWork("guardian_${alarm.id}")
        }
        alarmScheduler.handleAlarmFired(alarmId)
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
        stopSelf()
    }

    // F11: TTS morning announcement
    //
    // Shutdown is driven by [UtteranceProgressListener] (onDone/onError/onStop)
    // rather than a serviceScope.delay-based timer. The previous approach
    // relied on a coroutine launched in serviceScope; when the service was
    // destroyed within 8 s of dismiss (which is the common case — dismiss
    // immediately stops the service) the cleanup coroutine was cancelled
    // before it could call `tts.shutdown()`, leaking the TTS engine.
    //
    // Hard 30 s safety net is scheduled via the AppContext-bound
    // ScheduledExecutorService below (independent of serviceScope) so a
    // pathological TTS backend never holds the engine forever.
    private fun speakMorningAnnouncement(alarm: Alarm) {
        val now = LocalTime.now()
        val h = if (now.hour % 12 == 0) 12 else now.hour % 12
        val minStr = when {
            now.minute == 0 -> "o'clock"
            now.minute < 10 -> "oh ${now.minute}"
            else -> "${now.minute}"
        }
        val amPm = if (now.hour < 12) "A.M." else "P.M."
        val today = LocalDate.now()
        val dayName = today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val monthName = today.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val text = "It is $h $minStr $amPm. Today is $dayName, $monthName ${today.dayOfMonth}."

        val ttsRef = java.util.concurrent.atomic.AtomicReference<TextToSpeech?>()
        val safetyCancel = java.util.concurrent.atomic.AtomicReference<java.util.concurrent.ScheduledFuture<*>?>()
        val safetyExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "AlarmTtsSafety").apply { isDaemon = true }
        }

        fun shutdownAll() {
            try { safetyCancel.getAndSet(null)?.cancel(false) } catch (_: Exception) {}
            try { ttsRef.getAndSet(null)?.shutdown() } catch (_: Exception) {}
            try { safetyExecutor.shutdownNow() } catch (_: Exception) {}
        }

        val listener = TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.SUCCESS) {
                shutdownAll()
                return@OnInitListener
            }
            val tts = ttsRef.get() ?: return@OnInitListener
            tts.setOnUtteranceProgressListener(
                object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { shutdownAll() }
                    @Deprecated("legacy", ReplaceWith(""))
                    override fun onError(utteranceId: String?) { shutdownAll() }
                    override fun onError(utteranceId: String?, errorCode: Int) { shutdownAll() }
                    override fun onStop(utteranceId: String?, interrupted: Boolean) { shutdownAll() }
                }
            )
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "morning_announcement")
            // Hard cap: even if the TTS engine never reports completion (some
            // OEM impls swallow callbacks) we won't leak past 30 s.
            safetyCancel.set(
                safetyExecutor.schedule({ shutdownAll() }, 30, java.util.concurrent.TimeUnit.SECONDS)
            )
        }
        ttsRef.set(TextToSpeech(applicationContext, listener))
    }

    // F12: Launch morning briefing Activity
    private fun showMorningBriefing(alarm: Alarm) {
        val now = LocalTime.now()
        val timeStr = "${if (now.hour % 12 == 0) 12 else now.hour % 12}:${String.format("%02d", now.minute)} ${if (now.hour < 12) "AM" else "PM"}"
        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

        val intent = Intent(this, MorningBriefingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MorningBriefingActivity.EXTRA_TIME, timeStr)
            putExtra(MorningBriefingActivity.EXTRA_DATE, dateStr)
            putExtra(MorningBriefingActivity.EXTRA_WEATHER, "")  // Weather cached separately
            putExtra(MorningBriefingActivity.EXTRA_NEXT_EVENT, "")
            putExtra(MorningBriefingActivity.EXTRA_ROUTINE, alarm.morningRoutine)
        }
        startActivity(intent)
    }

    // F5: Schedule wake confirmation via WorkManager. Clamp the configured
    // delay to a sane minimum so a value of 0 (or a negative one resulting
    // from a corrupt setting) doesn't fire the worker the same instant we
    // dismissed — which would race the worker's prompt against the morning
    // briefing animation.
    private fun scheduleWakeConfirmation(alarm: Alarm) {
        val delayMinutes = alarm.wakeConfirmDelayMinutes.coerceAtLeast(1).toLong()
        val data = workDataOf(WakeConfirmWorker.KEY_ALARM_ID to alarm.id)
        val request = OneTimeWorkRequestBuilder<WakeConfirmWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(data)
            .addTag("wake_confirm_${alarm.id}")
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "wake_confirm_${alarm.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    private fun formatAlarmTime(alarm: Alarm): String {
        val h = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
        val amPm = if (alarm.hour < 12) "AM" else "PM"
        return "$h:${String.format("%02d", alarm.minute)} $amPm"
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

    private fun startFlashlightStrobe() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            flashlightJob = serviceScope.launch {
                while (isActive) {
                    try {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(200)
                        cameraManager.setTorchMode(cameraId, false)
                        delay(300)
                    } catch (_: Exception) { break }
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopAlarmPlayback() {
        volumeJob?.cancel()
        volumeJob = null
        flashlightJob?.cancel()
        flashlightJob = null
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            cm.cameraIdList.firstOrNull()?.let { cm.setTorchMode(it, false) }
        } catch (_: Exception) {}
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) { /* already released */ }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        autoSilenceJob?.cancel()
        backupSoundJob?.cancel()
        flashlightJob?.cancel()
        stopAlarmPlayback()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
