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
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.*
import android.speech.tts.TextToSpeech
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.receiver.DismissReceiver
import com.sysadmindoc.alarmclock.receiver.SnoozeReceiver
import com.sysadmindoc.alarmclock.ui.alarmfiring.AlarmFiringActivity
import com.sysadmindoc.alarmclock.ui.alarmfiring.MorningBriefingActivity
import com.sysadmindoc.alarmclock.util.AlarmPublicText
import com.sysadmindoc.alarmclock.wear.WearNextAlarmBridge
import com.sysadmindoc.alarmclock.worker.WakeConfirmWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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
    @Inject lateinit var alarmIncidentRepository: AlarmIncidentRepository
    @Inject lateinit var preferencesManager: com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
    @Inject lateinit var webhookService: WebhookService
    @Inject lateinit var wearNextAlarmBridge: WearNextAlarmBridge

    companion object {
        const val ACTION_START_ALARM = "com.sysadmindoc.alarmclock.START_ALARM"
        const val ACTION_SNOOZE = "com.sysadmindoc.alarmclock.SNOOZE"
        const val ACTION_DISMISS = "com.sysadmindoc.alarmclock.DISMISS"
        const val EXTRA_CUSTOM_SNOOZE_MINUTES = "custom_snooze_minutes"
        const val EXTRA_CHALLENGE_RETRY_COUNT = "challenge_retry_count"
        const val EXTRA_CHALLENGE_SOLVE_TIME_MS = "challenge_solve_time_ms"
        const val EXTRA_WAKE_CONFIRM_REFIRE_COUNT = "wake_confirm_refire_count"
        private const val MIN_CUSTOM_SNOOZE_MINUTES = 1
        private const val MAX_CUSTOM_SNOOZE_MINUTES = 120
        private const val HAPTIC_ONLY_COMPOSITION_INTERVAL_MS = 1_450L

        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_UPCOMING = "upcoming_alarm_channel"
        const val CHANNEL_MISSED = "missed_alarm_channel"
        // v1.12.1 (roadmap N8): dedicated channel for "your timer finished"
        // posts so the user can disable just timer pings without losing the
        // missed-alarm reliability path.
        const val CHANNEL_TIMER = "timer_finished_channel"
        const val NOTIFICATION_ID = 1001
        const val MISSED_NOTIFICATION_ID = 1003
        const val DEFAULT_AUTO_SILENCE_MINUTES = 10L
        private const val TAG = "AlarmService"

        data class ActiveAlarmSnapshot(
            val alarmId: Long,
            val scheduledAt: Long,
            val fireId: String
        )

        /**
         * v1.5.1: Live-alarm state surfaced to [MissedAlarmUnlockReceiver] and
         * [MainActivity] so they can detect a firing alarm. AtomicReference
         * ensures readers always see a consistent triplet — the previous three
         * separate @Volatile fields could tear across concurrent reads.
         */
        @JvmField
        internal val activeAlarm = AtomicReference<ActiveAlarmSnapshot?>(null)

        internal val activeAlarmId: Long get() = activeAlarm.get()?.alarmId ?: -1L

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

            // v1.12.1 (roadmap N8): timer-finished channel. IMPORTANCE_HIGH
            // so it heads-up and bypasses standard "minimised" treatment,
            // mirroring the missed-alarm class. Vibration is disabled at the
            // channel — the timer's own MediaPlayer + vibrator handle the
            // foreground experience; this notification is the
            // user-isn't-looking-at-the-app surface.
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                "Timer Finished",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when one of your countdown timers reaches zero."
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(timerChannel)
            nm.createNotificationChannel(missedChannel)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var vibrator: Vibrator? = null
    private var volumeJob: Job? = null
    private var hapticOnlyJob: Job? = null
    private var currentAlarmId: Long = -1
    private var currentFireId: String = ""
    private var currentScheduledAt: Long = 0L
    private var alarmFiredAt: Long = 0
    private var autoSilenceJob: Job? = null
    private var backupSoundJob: Job? = null
    @Volatile
    private var backupSoundOriginalAlarmVolume: Int? = null
    private var flashlightJob: Job? = null
    private var currentSnoozeCount: Int = 0
    private var currentWakeConfirmRefireCount: Int = 0
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    // v1.9.1: AtomicBoolean (was @Volatile Boolean). Multiple coroutines on
    // serviceScope's IO dispatcher can race the foreground stop path — e.g.
    // the auto-silence job firing at the same moment the user taps Dismiss.
    // The previous check-then-act on a volatile Boolean was non-atomic, so
    // both paths could pass the `if (isForeground)` guard and call
    // stopForeground() twice; some OEMs (Samsung One UI 6) treat the second
    // call as fatal. compareAndSet makes the transition single-fire.
    private val isForeground = AtomicBoolean(false)
    // v1.5.1: Guard against re-entering startAudio() from the internet-radio
    // error path — if both the radio and the default fallback fail, we could
    // otherwise leak orphaned MediaPlayer instances.
    private val audioStarting = AtomicBoolean(false)
    private val runtimeStatePrefs by lazy {
        getSharedPreferences("alarm_runtime_state", MODE_PRIVATE)
    }

    // v1.11.2 (roadmap N2): Telephony-aware muting. When a call is OFFHOOK or
    // RINGING during alarm playback the MediaPlayer is muted (vibration and
    // the firing screen are intentionally kept so the user still has wake
    // cues that don't interrupt the call audio). On IDLE the player is
    // restored to full volume. We register on first startAudio() and
    // unregister in stopAlarmPlayback()/onDestroy() so the listener only
    // exists while the alarm is actually ringing — registering globally
    // would mean every passive call state change touched the player ref.
    @Volatile
    private var callMutedAudio: Boolean = false
    private var telephonyCallback: TelephonyCallback? = null
    @Suppress("DEPRECATION")
    private var legacyPhoneStateListener: PhoneStateListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)

        // v1.5.4: Safe cast + defensive try around acquire(); rare OEM builds
        // throw SecurityException from newWakeLock() when the process is in a
        // restricted state.
        //
        // v1.11.4 (roadmap N4) Play wake-lock policy audit:
        //   This wake lock is held by AlarmService — a `mediaPlayback`
        //   foreground service playing AudioAttributes.USAGE_ALARM content.
        //   Both the FGS type and the alarm-audio activity are documented
        //   exempt categories under the Play Store March-2026 wake-lock
        //   technical-quality treatment, so the 30-minute ceiling does not
        //   count against the 2 h / 24 h non-exempt budget. The wake lock
        //   is released in onDestroy() as soon as the alarm is dismissed,
        //   snoozed, or auto-silenced — in practice the held time is the
        //   few seconds between alarm-fire and user-dismiss.
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        try {
            wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AlarmClockXtreme::AlarmWakeLock"
            )?.apply {
                acquire(30 * 60 * 1000L) // 30 minutes — covers max auto-silence; released in onDestroy()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wake lock acquisition failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                if (alarmId != -1L) {
                    val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0L)
                    val fireId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID)
                        ?: AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)
                    // Cancel any prior auto-silence/fade jobs before starting new alarm
                    autoSilenceJob?.cancel()
                    volumeJob?.cancel()
                    stopAlarmPlayback()
                    currentAlarmId = alarmId
                    currentScheduledAt = scheduledAt
                    currentFireId = fireId
                    activeAlarm.set(ActiveAlarmSnapshot(alarmId, scheduledAt, fireId))
                    currentSnoozeCount = readPersistedSnoozeCount(alarmId)
                    currentWakeConfirmRefireCount = intent.getIntExtra(
                        EXTRA_WAKE_CONFIRM_REFIRE_COUNT, 0
                    )
                    alarmFiredAt = System.currentTimeMillis()
                    recordIncidentAsync(
                        type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                        status = AlarmIncidentEvent.STATUS_RECEIVED,
                        reasonCode = "START_COMMAND_RECEIVED",
                        source = "AlarmService"
                    )
                    activateAlarmMediaSession("START_COMMAND")
                    // v1.5.4: Android 14+ requires startForeground() within ~5 s of
                    // startForegroundService() or the app crashes with
                    // ForegroundServiceDidNotStartInTimeException. Previously the
                    // call happened inside the IO-dispatched coroutine below, which
                    // on a busy device (cold start from Doze, heavy IO contention)
                    // could miss the window. Promote startForeground() out of the
                    // coroutine using a placeholder; startAlarm() later replaces
                    // the notification via nm.notify() with the labelled version.
                    startForegroundWithPlaceholder()
                    serviceScope.launch { startAlarm(alarmId) }
                }
            }
            ACTION_SNOOZE -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarmId)
                val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, currentScheduledAt)
                val fireId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID)
                    ?: currentFireId.ifBlank { AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt) }
                val customMinutes = intent.getIntExtra(EXTRA_CUSTOM_SNOOZE_MINUTES, -1)
                    .takeIf { it > 0 }
                    ?.coerceIn(MIN_CUSTOM_SNOOZE_MINUTES, MAX_CUSTOM_SNOOZE_MINUTES)
                // v1.5.1: If the service was killed+restarted between fire and
                // snooze, currentSnoozeCount is 0 (fresh instance). Re-read the
                // persisted count so the progressive-snooze ladder doesn't reset.
                if (currentAlarmId == -1L && alarmId > 0L) {
                    currentAlarmId = alarmId
                    currentScheduledAt = scheduledAt
                    currentFireId = fireId
                    currentSnoozeCount = readPersistedSnoozeCount(alarmId)
                }
                serviceScope.launch { snoozeAlarm(alarmId, customMinutes) }
            }
            ACTION_DISMISS -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarmId)
                val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, currentScheduledAt)
                val fireId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID)
                    ?: currentFireId.ifBlank { AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt) }
                val challengeRetryCount = intent
                    .getIntExtra(EXTRA_CHALLENGE_RETRY_COUNT, 0)
                    .coerceAtLeast(0)
                val challengeSolveTimeMs = intent
                    .getLongExtra(EXTRA_CHALLENGE_SOLVE_TIME_MS, 0L)
                    .coerceAtLeast(0L)
                // v1.5.1: Same service-restart protection as ACTION_SNOOZE.
                if (currentAlarmId == -1L && alarmId > 0L) {
                    currentAlarmId = alarmId
                    currentScheduledAt = scheduledAt
                    currentFireId = fireId
                    currentSnoozeCount = readPersistedSnoozeCount(alarmId)
                }
                serviceScope.launch {
                    dismissAlarm(
                        alarmId = alarmId,
                        challengeRetryCount = challengeRetryCount,
                        challengeSolveTimeMs = challengeSolveTimeMs
                    )
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun startAlarm(alarmId: Long) {
        // v1.5.1: Sanitise before any downstream logic touches the row. This
        // catches corrupt challengeType / vibrationPattern / specificDate /
        // ringtonePool entries so a bad backup restore or buggy older-version
        // write can't crash the firing path.
        val alarm = repository.getById(alarmId)?.sanitized() ?: run {
            recordIncident(
                type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "ALARM_ROW_MISSING",
                source = "AlarmService"
            )
            clearAlarmRuntimeState(alarmId)
            activeAlarm.set(null)
            releaseAlarmMediaSession()
            stopSelf()
            return
        }

        // v1.5.4: startForeground() was already called synchronously in
        // onStartCommand with a placeholder to satisfy Android 14+ timing.
        // Update the notification in-place with the fully-labelled version.
        val notification = buildAlarmNotification(alarm)
        wearNextAlarmBridge.publishAlarmFiring(alarm)
        try {
            if (isForeground.compareAndSet(false, true)) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, notification)
            }
            recordIncident(
                type = AlarmIncidentEvent.TYPE_NOTIFICATION,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "ALARM_NOTIFICATION_POSTED",
                source = "AlarmService"
            )
        } catch (e: Exception) {
            // Service may already be foregrounded or the system may reject
            // an update during teardown; neither is fatal — the alarm still
            // plays and the firing Activity is launched below.
            recordIncident(
                type = AlarmIncidentEvent.TYPE_NOTIFICATION,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "ALARM_NOTIFICATION_FAILED_${e.javaClass.simpleName}",
                source = "AlarmService"
            )
        }

        val firingIntent = Intent(this, AlarmFiringActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, currentScheduledAt)
            putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, currentFireId)
        }
        try {
            startActivity(firingIntent)
            recordIncident(
                type = AlarmIncidentEvent.TYPE_ACTIVITY_LAUNCH,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "FIRING_ACTIVITY_LAUNCHED",
                source = "AlarmService"
            )
        } catch (e: Exception) {
            recordIncident(
                type = AlarmIncidentEvent.TYPE_ACTIVITY_LAUNCH,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "FIRING_ACTIVITY_FAILED_${e.javaClass.simpleName}",
                source = "AlarmService"
            )
        }

        startAudio(alarm)

        if (alarm.vibrationEnabled) {
            // v1.12.0 (roadmap N7): optional pre-vibration delay. Defers haptic
            // onset for `vibrationDelaySeconds` so it can pair with a long
            // gradualVolumeSeconds fade-in ("audio first, vibration when the
            // fade is well underway"). The delay job is cancellable via
            // volumeJob's parent scope — snooze / dismiss / auto-silence all
            // cancel serviceScope, so a queued vibration never fires after the
            // alarm is gone.
            if (alarm.vibrationDelaySeconds > 0) {
                serviceScope.launch {
                    kotlinx.coroutines.delay(alarm.vibrationDelaySeconds * 1000L)
                    // Re-check the live alarm id — service-restart races could
                    // otherwise vibrate for an alarm the user already dismissed.
                    if (currentAlarmId == alarmId) startVibration(alarm)
                }
            } else {
                startVibration(alarm)
            }
        }

        // F8: Webhook on alarm fire (fire-and-forget on its own scope; see WebhookService)
        webhookService.fireAsync(
            event = WebhookEvent.AlarmFired,
            alarmId = alarm.id,
            label = alarm.label,
            timeFormatted = formatAlarmTime(alarm),
            scheduledForMillis = currentScheduledAt.takeIf { it > 0L },
            fireId = currentFireId
        )

        // Auto-silence after timeout - records as missed
        val settings = preferencesManager.getCurrentSettings()
        val autoSilenceMinutes = settings.autoSilenceMinutes.toLong()
        if (autoSilenceMinutes > 0) {
            autoSilenceJob = serviceScope.launch {
                kotlinx.coroutines.delay(autoSilenceMinutes * 60 * 1000L)
                val missedAlarm = repository.getById(alarmId)
                if (missedAlarm != null) {
                    recordEvent(missedAlarm, com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent.ACTION_MISSED)
                    recordIncident(
                        type = AlarmIncidentEvent.TYPE_AUTO_SILENCE,
                        status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                        reasonCode = "AUTO_SILENCED_AFTER_${autoSilenceMinutes}_MINUTES",
                        source = "AlarmService"
                    )
                    webhookService.fireAsync(
                        event = WebhookEvent.AlarmMissed,
                        alarmId = missedAlarm.id,
                        label = missedAlarm.label,
                        timeFormatted = formatAlarmTime(missedAlarm),
                        scheduledForMillis = currentScheduledAt.takeIf { it > 0L },
                        fireId = currentFireId
                    )
                    showMissedNotification(missedAlarm, autoSilenceMinutes)
                    // v1.4.0: Repeat missed alarms — record the alarm id / timestamp
                    // so MissedAlarmUnlockReceiver can re-fire when the user unlocks
                    // soon after. Guard on the user-level preference.
                    if (settings.repeatMissedAlarms) {
                        getSharedPreferences("missed_alarm_state", MODE_PRIVATE)
                            .edit()
                            .putLong("last_missed_at", System.currentTimeMillis())
                            .putLong("last_missed_id", missedAlarm.id)
                            .commit()
                    }
                }
                clearAlarmRuntimeState(alarmId)
                currentSnoozeCount = 0
                currentAlarmId = -1
                activeAlarm.set(null)
                alarmScheduler.handleAlarmFired(alarmId)
                stopAlarmPlayback()
                if (isForeground.compareAndSet(true, false)) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                stopSelf()
            }
        }

        // v1.2.0: Backup sound escalation
        if (alarm.backupSoundEnabled && !alarm.usesMutedAlarmAudio()) {
            backupSoundJob = serviceScope.launch {
                delay(alarm.backupSoundDelaySec * 1000L)
                // Escalate: set volume to max and switch to system alarm tone.
                // v1.11.2: the call observer still has priority — if we're
                // muted because of a call, escalate the *system* stream so the
                // post-call audio is loud, but don't unmute the MediaPlayer
                // mid-call.
                val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return@launch
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                backupSoundOriginalAlarmVolume = backupSoundOriginalAlarmVolume
                    ?: audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                recordIncident(
                    type = AlarmIncidentEvent.TYPE_AUDIO,
                    status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                    reasonCode = "BACKUP_SOUND_ESCALATED",
                    source = "AlarmService"
                )
                if (!callMutedAudio) mediaPlayer?.setVolume(1f, 1f)
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

    /**
     * v1.5.4: Synchronous foreground promotion with a minimal notification so
     * Android 14+'s 5-second startForeground() deadline is always met. The
     * real labelled notification replaces this via NotificationManager.notify()
     * once the alarm row has been fetched from Room.
     */
    private fun startForegroundWithPlaceholder() {
        // compareAndSet ensures only one caller wins the foreground transition;
        // a concurrent ACTION_START_ALARM (rare but possible if the AlarmReceiver
        // re-fires while we're still tearing down) won't double-start.
        if (!isForeground.compareAndSet(false, true)) return
        try {
            val placeholder = NotificationCompat.Builder(this, CHANNEL_ALARM)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(getString(R.string.notif_alarm_title))
                .setContentText(getString(R.string.notif_alarm_ringing))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
            startForeground(NOTIFICATION_ID, placeholder)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_FOREGROUND_PROMOTION,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "PLACEHOLDER_FOREGROUND_STARTED",
                source = "AlarmService"
            )
        } catch (e: Exception) {
            // Roll back the flag on failure so a subsequent retry can try again
            // (instead of pretending we already foregrounded).
            isForeground.set(false)
            // ForegroundServiceStartNotAllowedException can surface if the app
            // is background-restricted at fire time. The AlarmManager exact
            // alarm guarantee makes this very rare; log and continue.
            Log.w(TAG, "Failed to foreground service with placeholder", e)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_FOREGROUND_PROMOTION,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "PLACEHOLDER_FOREGROUND_FAILED_${e.javaClass.simpleName}",
                source = "AlarmService"
            )
        }
    }

    private fun buildAlarmNotification(alarm: Alarm): Notification {
        val fullScreenIntent = Intent(this, AlarmFiringActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, currentScheduledAt)
            putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, currentFireId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, alarm.id.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, currentScheduledAt)
            putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, currentFireId)
        }
        val snoozePi = PendingIntent.getBroadcast(
            this, alarm.id.toInt() + 10000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, DismissReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, currentScheduledAt)
            putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, currentFireId)
        }
        val dismissPi = PendingIntent.getBroadcast(
            this, alarm.id.toInt() + 20000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // v1.5.1: 24h preference honoured via shared formatter.
        val timeText = formatAlarmTime(alarm)
        val hideLabel = preferencesManager.getCachedSettings().hideAlarmLabelsOnPublicSurfaces

        return NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(getString(R.string.notif_alarm_title))
            .setContentText(
                AlarmPublicText.firingNotificationText(
                    label = alarm.label,
                    fallbackTime = timeText,
                    hideLabel = hideLabel
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_alarm, getString(R.string.notif_snooze_action, alarm.snoozeDurationMinutes), snoozePi)
            .addAction(R.drawable.ic_alarm, getString(R.string.notif_dismiss_action), dismissPi)
            .build()
    }

    private fun activateAlarmMediaSession(reasonCode: String) {
        val session = mediaSession ?: run {
            MediaSession(this, "AlarmClockXtremeAlarm").also { created ->
                mediaSession = created
            }
        }
        try {
            session.isActive = true
            updateAlarmMediaSessionState(PlaybackState.STATE_PLAYING)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "MEDIA_SESSION_ACTIVE_$reasonCode",
                source = "AlarmService"
            )
        } catch (e: Exception) {
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "MEDIA_SESSION_FAILED_${e.javaClass.simpleName}",
                source = "AlarmService"
            )
        }
    }

    private fun updateAlarmMediaSessionState(state: Int) {
        val session = mediaSession ?: return
        val speed = if (state == PlaybackState.STATE_PLAYING) 1f else 0f
        try {
            val playbackState = PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_STOP)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, speed)
                .build()
            session.setPlaybackState(playbackState)
        } catch (e: Exception) {
            Log.w(TAG, "MediaSession playback state update failed", e)
        }
    }

    private fun releaseAlarmMediaSession() {
        val session = mediaSession ?: return
        try {
            updateAlarmMediaSessionState(PlaybackState.STATE_STOPPED)
            session.isActive = false
            session.release()
        } catch (e: Exception) {
            Log.w(TAG, "MediaSession release failed", e)
        } finally {
            mediaSession = null
        }
    }

    private fun restoreBackupSoundVolume() {
        val originalVolume = backupSoundOriginalAlarmVolume ?: return
        backupSoundOriginalAlarmVolume = null
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                originalVolume.coerceIn(0, maxVol),
                0
            )
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "BACKUP_SOUND_VOLUME_RESTORED",
                source = "AlarmService"
            )
        } catch (e: Exception) {
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "BACKUP_SOUND_VOLUME_RESTORE_FAILED_${e.javaClass.simpleName}",
                source = "AlarmService"
            )
        }
    }

    private fun startAudio(alarm: Alarm) {
        // Silent mode - skip audio entirely
        if (alarm.ringtoneUri == "silent" || alarm.usesMutedAlarmAudio()) {
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_SKIPPED,
                reasonCode = "SILENT_OR_HAPTIC_ONLY",
                source = "AlarmService"
            )
            updateAlarmMediaSessionState(PlaybackState.STATE_PLAYING)
            return
        }

        // v1.11.2 (roadmap N2): Watch for in-progress / incoming calls so the
        // alarm audio can step out of the way without tearing down the rest
        // of the alarm. No-op on silent / haptic-only paths (we returned
        // above) and on tablets without telephony.
        registerCallObserver()

        // v1.5.1: Re-entry guard — the internet-radio error path re-calls
        // startAudio on serviceScope; without this, a transient failure
        // during the default-fallback path could recurse and leak
        // MediaPlayers. The guard is released when the method returns.
        if (!audioStarting.compareAndSet(false, true)) {
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_SKIPPED,
                reasonCode = "AUDIO_START_REENTRY_SKIPPED",
                source = "AlarmService"
            )
            return
        }
        try {
            val pooledAlarm = alarm.ringtonePool.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.let { pool -> alarm.copy(ringtoneUri = pool.random()) }
                ?: alarm

            startAudioInternal(pooledAlarm)
        } finally {
            audioStarting.set(false)
        }
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
                    updateAlarmMediaSessionState(PlaybackState.STATE_PLAYING)
                    recordIncidentAsync(
                        type = AlarmIncidentEvent.TYPE_AUDIO,
                        status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                        reasonCode = "SPOTIFY_DELEGATED",
                        source = "AlarmService"
                    )
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
                        updateAlarmMediaSessionState(PlaybackState.STATE_PLAYING)
                        recordIncidentAsync(
                            type = AlarmIncidentEvent.TYPE_AUDIO,
                            status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                            reasonCode = "INTERNET_RADIO_STARTED",
                            source = "AlarmService"
                        )
                        if (alarm.overrideSystemVolume) {
                            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                            val targetVol = (maxVol * alarm.volume / 100f).toInt().coerceIn(1, maxVol)
                            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                        }
                        // v1.11.2: if a call landed during prepareAsync, honour it.
                        if (callMutedAudio) try { mp.setVolume(0f, 0f) } catch (_: Exception) {}
                    }
                    // Without an OnErrorListener, a stream failure (DNS, 404, codec
                    // mismatch) results in a silent alarm — fall back to the device
                    // default ringtone via the standard path below.
                    setOnErrorListener { mp, _, _ ->
                        try { mp.release() } catch (_: Exception) {}
                        if (mediaPlayer === mp) mediaPlayer = null
                        recordIncidentAsync(
                            type = AlarmIncidentEvent.TYPE_AUDIO,
                            status = AlarmIncidentEvent.STATUS_FAILED,
                            reasonCode = "INTERNET_RADIO_ERROR",
                            source = "AlarmService"
                        )
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
                recordIncidentAsync(
                    type = AlarmIncidentEvent.TYPE_AUDIO,
                    status = AlarmIncidentEvent.STATUS_REQUESTED,
                    reasonCode = "INTERNET_RADIO_PREPARING",
                    source = "AlarmService"
                )
                return  // Radio handles playback
            } catch (e: Exception) {
                // Fall through to default audio
                recordIncidentAsync(
                    type = AlarmIncidentEvent.TYPE_AUDIO,
                    status = AlarmIncidentEvent.STATUS_FAILED,
                    reasonCode = "INTERNET_RADIO_SETUP_FAILED_${e.javaClass.simpleName}",
                    source = "AlarmService"
                )
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
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_SKIPPED,
                reasonCode = "NO_DEFAULT_TONE",
                source = "AlarmService"
            )
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
                // v1.11.2: callMutedAudio overrides both — a ringing call at
                // alarm fire-time keeps audio at 0 until the call ends.
                if (callMutedAudio) setVolume(0f, 0f)
                else if (fadeInMs > 0) setVolume(0f, 0f) else setVolume(1f, 1f)
                start()
                updateAlarmMediaSessionState(PlaybackState.STATE_PLAYING)
            }
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "MEDIA_PLAYER_STARTED",
                source = "AlarmService"
            )

            if (fadeInMs > 0) {
                volumeJob = serviceScope.launch {
                    val steps = 50
                    val stepDelay = fadeInMs / steps
                    for (i in 1..steps) {
                        delay(stepDelay)
                        // v1.11.2: the call observer takes priority; don't ramp
                        // volume during a call (it'll be restored on IDLE).
                        if (callMutedAudio) continue
                        val volume = i.toFloat() / steps
                        mediaPlayer?.setVolume(volume, volume)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start configured alarm sound; falling back to default tone", e)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "MEDIA_PLAYER_FAILED_${e.javaClass.simpleName}",
                source = "AlarmService"
            )
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
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
                        // v1.11.2: honour an active call right out of the gate.
                        if (callMutedAudio) setVolume(0f, 0f) else setVolume(1f, 1f)
                        start()
                        updateAlarmMediaSessionState(PlaybackState.STATE_PLAYING)
                    }
                    recordIncidentAsync(
                        type = AlarmIncidentEvent.TYPE_AUDIO,
                        status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                        reasonCode = "DEFAULT_FALLBACK_STARTED",
                        source = "AlarmService"
                    )
                }
            } catch (fallbackError: Exception) {
                // Last resort - alarm fires silently but notification is still shown
                recordIncidentAsync(
                    type = AlarmIncidentEvent.TYPE_AUDIO,
                    status = AlarmIncidentEvent.STATUS_FAILED,
                    reasonCode = "DEFAULT_FALLBACK_FAILED_${fallbackError.javaClass.simpleName}",
                    source = "AlarmService"
                )
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

        if (alarm.usesHapticOnlyProfile() && startHapticOnlyCompositionLoop()) {
            return
        }

        val (pattern, amplitudes) = when (alarm.vibrationPattern) {
            "gentle" -> longArrayOf(0, 200, 1200, 200, 1200) to intArrayOf(0, 60, 0, 60, 0)
            "heartbeat" -> longArrayOf(0, 150, 100, 150, 800) to intArrayOf(0, 200, 0, 255, 0)
            "escalating" -> longArrayOf(0, 200, 600, 300, 500, 400, 400, 500, 300) to
                intArrayOf(0, 60, 0, 120, 0, 180, 0, 255, 0)
            "sos" -> longArrayOf(0, 150, 100, 150, 100, 150, 300, 400, 100, 400, 100, 400, 300, 150, 100, 150, 100, 150, 600) to
                intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0)
            else -> { // "default"
                if (alarm.usesHapticOnlyProfile()) {
                    longArrayOf(0, 90, 140, 140, 720, 180, 1300) to
                        intArrayOf(0, 95, 0, 140, 0, 185, 0)
                } else {
                    when (alarm.vibrationIntensity) {
                        1 -> longArrayOf(0, 200, 1000, 200, 1000) to intArrayOf(0, 80, 0, 80, 0)
                        else -> longArrayOf(0, 500, 500, 500, 500) to intArrayOf(0, 255, 0, 255, 0)
                    }
                }
            }
        }

        val vibrationAttributes = alarmVibrationAttributes()
        if (vibrator?.hasAmplitudeControl() == true) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0), vibrationAttributes)
        } else {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0), vibrationAttributes)
        }
    }

    private fun startHapticOnlyCompositionLoop(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val activeVibrator = vibrator ?: return false
        if (!activeVibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )
        ) {
            return false
        }

        val vibrationAttributes = alarmVibrationAttributes()
        hapticOnlyJob?.cancel()
        hapticOnlyJob = serviceScope.launch {
            while (isActive) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.35f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.55f, 180)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.78f, 620)
                    .compose()
                activeVibrator.vibrate(effect, vibrationAttributes)
                delay(HAPTIC_ONLY_COMPOSITION_INTERVAL_MS)
            }
        }
        return true
    }

    private fun alarmVibrationAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private fun Alarm.usesMutedAlarmAudio(): Boolean {
        return overrideSystemVolume && volume <= 0
    }

    private fun Alarm.usesHapticOnlyProfile(): Boolean {
        return usesMutedAlarmAudio() && vibrationEnabled
    }

    private suspend fun snoozeAlarm(alarmId: Long, customMinutes: Int? = null) {
        autoSilenceJob?.cancel()
        backupSoundJob?.cancel()
        flashlightJob?.cancel()
        volumeJob?.cancel()
        stopAlarmPlayback()
        val alarm = repository.getById(alarmId)?.sanitized()
        if (alarm != null) {
            val webhookScheduledAt = currentScheduledAt
            val webhookFireId = currentFireId
            // Snoozing means the user interacted with the alarm, so cancel any pending
            // Guardian Angel call/SMS — they're plainly awake enough to hit snooze.
            // The next fire after snooze will re-arm Guardian if still configured.
            if (alarm.guardianEnabled) {
                WorkManager.getInstance(applicationContext)
                    .cancelUniqueWork("guardian_${alarm.id}")
            }
            val nextSnoozeCount = currentSnoozeCount + 1
            // v1.6.3: Track which event was actually persisted so the webhook
            // emits the matching event name. The previous code recorded
            // ACTION_DISMISSED when the snooze cap was hit but still fired the
            // "snoozed" webhook — Tasker integrations got the wrong event.
            val webhookEvent: WebhookEvent
            if (alarm.maxSnoozeCount > 0 && nextSnoozeCount > alarm.maxSnoozeCount) {
                // Max snoozes reached - treat as dismiss
                currentSnoozeCount = alarm.maxSnoozeCount
                recordEvent(alarm, AlarmEvent.ACTION_DISMISSED)
                recordIncident(
                    type = AlarmIncidentEvent.TYPE_USER_ACTION,
                    status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                    reasonCode = "MAX_SNOOZE_DISMISSED",
                    source = "AlarmService",
                    alarmId = alarm.id
                )
                clearAlarmRuntimeState(alarmId)
                currentSnoozeCount = 0
                currentAlarmId = -1
                activeAlarm.set(null)
                alarmScheduler.handleAlarmFired(alarmId)
                webhookEvent = WebhookEvent.AlarmDismissed
            } else {
                currentSnoozeCount = nextSnoozeCount
                persistSnoozeCount(alarmId, currentSnoozeCount)
                val effectiveSnooze = if (alarm.progressiveSnooze && customMinutes == null) {
                    (alarm.snoozeDurationMinutes - currentSnoozeCount).coerceAtLeast(1)
                } else customMinutes
                alarmScheduler.scheduleSnooze(alarm, effectiveSnooze)
                recordEvent(alarm, AlarmEvent.ACTION_SNOOZED)
                recordIncident(
                    type = AlarmIncidentEvent.TYPE_USER_ACTION,
                    status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                    reasonCode = "SNOOZED",
                    source = "AlarmService",
                    alarmId = alarm.id
                )
                webhookEvent = WebhookEvent.AlarmSnoozed
            }
            webhookService.fireAsync(
                event = webhookEvent,
                alarmId = alarm.id,
                label = alarm.label,
                timeFormatted = formatAlarmTime(alarm),
                scheduledForMillis = webhookScheduledAt.takeIf { it > 0L },
                fireId = webhookFireId
            )
            wearNextAlarmBridge.publishAlarmIdle(alarm.id)
        } else {
            recordIncident(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "SNOOZE_ALARM_ROW_MISSING",
                source = "AlarmService",
                alarmId = alarmId
            )
            clearAlarmRuntimeState(alarmId)
            currentSnoozeCount = 0
            currentAlarmId = -1
            activeAlarm.set(null)
        }
        if (isForeground.compareAndSet(true, false)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    private suspend fun dismissAlarm(
        alarmId: Long,
        challengeRetryCount: Int = 0,
        challengeSolveTimeMs: Long = 0L
    ) {
        autoSilenceJob?.cancel()
        backupSoundJob?.cancel()
        flashlightJob?.cancel()
        volumeJob?.cancel()
        stopAlarmPlayback()
        val alarm = repository.getById(alarmId)?.sanitized()
        if (alarm != null) {
            val wakeConfirmFireId = currentFireId.ifBlank {
                AlarmIncidentEvent.fireIdFor(alarm.id, currentScheduledAt)
            }
            val wakeConfirmScheduledAt = currentScheduledAt
            recordEvent(
                alarm = alarm,
                action = AlarmEvent.ACTION_DISMISSED,
                challengeRetryCount = challengeRetryCount,
                challengeSolveTimeMs = challengeSolveTimeMs
            )
            recordIncident(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "DISMISSED",
                source = "AlarmService",
                alarmId = alarm.id
            )
            clearAlarmRuntimeState(alarmId)
            currentSnoozeCount = 0
            currentAlarmId = -1
            activeAlarm.set(null)

            // F8: Webhook on dismiss (fire-and-forget on its own scope)
            webhookService.fireAsync(
                event = WebhookEvent.AlarmDismissed,
                alarmId = alarm.id,
                label = alarm.label,
                timeFormatted = formatAlarmTime(alarm),
                scheduledForMillis = wakeConfirmScheduledAt.takeIf { it > 0L },
                fireId = wakeConfirmFireId
            )

            // F11: TTS morning announcement
            if (alarm.ttsEnabled) {
                speakMorningAnnouncement(alarm)
            }

            // F12: Morning briefing screen
            showMorningBriefing(alarm)

            // F5: Post-alarm wake confirmation
            if (alarm.wakeConfirmEnabled) {
                scheduleWakeConfirmation(
                    alarm = alarm,
                    fireId = wakeConfirmFireId,
                    scheduledAt = wakeConfirmScheduledAt
                )
            }

            // v1.2.0: Cancel guardian if active (alarm was dismissed in time)
            WorkManager.getInstance(applicationContext).cancelUniqueWork("guardian_${alarm.id}")
        } else {
            recordIncident(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "DISMISS_ALARM_ROW_MISSING",
                source = "AlarmService",
                alarmId = alarmId
            )
            clearAlarmRuntimeState(alarmId)
            currentSnoozeCount = 0
            currentAlarmId = -1
            activeAlarm.set(null)
        }
        alarmScheduler.handleAlarmFired(alarmId)
        wearNextAlarmBridge.publishAlarmIdle(alarmId)
        if (isForeground.compareAndSet(true, false)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
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
        // v1.5.1: TextToSpeech constructor can throw on stripped-down AOSP or
        // managed-profile devices with no TTS engine installed. Don't crash
        // the service — the alarm is already dismissed, announcement is
        // best-effort.
        try {
            ttsRef.set(TextToSpeech(applicationContext, listener))
        } catch (_: Exception) {
            shutdownAll()
        }
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
    private fun scheduleWakeConfirmation(
        alarm: Alarm,
        fireId: String,
        scheduledAt: Long,
        refireCount: Int = currentWakeConfirmRefireCount
    ) {
        val delayMinutes = alarm.wakeConfirmDelayMinutes.coerceAtLeast(1).toLong()
        val data = workDataOf(
            WakeConfirmWorker.KEY_ALARM_ID to alarm.id,
            WakeConfirmWorker.KEY_ALARM_FIRE_ID to fireId,
            WakeConfirmWorker.KEY_SCHEDULED_AT to scheduledAt,
            WakeConfirmWorker.KEY_REFIRE_COUNT to refireCount
        )
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
        recordIncidentAsync(
            type = AlarmIncidentEvent.TYPE_WAKE_CONFIRM,
            status = AlarmIncidentEvent.STATUS_REQUESTED,
            reasonCode = "WAKE_CONFIRM_SCHEDULED",
            source = "AlarmService",
            alarmId = alarm.id,
            fireId = fireId,
            scheduledAt = scheduledAt
        )
    }

    private fun formatAlarmTime(alarm: Alarm): String {
        val time = alarm.time
        // v1.5.1: Respect the user's 24-hour preference (uses the app's cached
        // snapshot so this is safe from any thread).
        return if (preferencesManager.getCachedSettings().is24HourFormat) {
            String.format(Locale.US, "%02d:%02d", time.hour, time.minute)
        } else {
            val h = if (time.hour % 12 == 0) 12 else time.hour % 12
            val amPm = if (time.hour < 12) "AM" else "PM"
            String.format(Locale.US, "%d:%02d %s", h, time.minute, amPm)
        }
    }

    private suspend fun recordEvent(
        alarm: Alarm,
        action: String,
        challengeRetryCount: Int = 0,
        challengeSolveTimeMs: Long = 0L
    ) {
        val now = System.currentTimeMillis()
        val dayOfWeek = java.time.Instant.ofEpochMilli(now)
            .atZone(java.time.ZoneId.systemDefault())
            .dayOfWeek.value
        eventRepository.record(
            AlarmEvent(
                alarmId = alarm.id,
                alarmLabel = alarm.label,
                scheduledTime = currentScheduledAt,
                firedAt = alarmFiredAt,
                action = action,
                actionAt = now,
                challengeType = alarm.challengeType,
                challengeSolveTimeMs = challengeSolveTimeMs.coerceAtLeast(0L),
                challengeRetryCount = challengeRetryCount.coerceAtLeast(0),
                snoozeCount = currentSnoozeCount.coerceAtLeast(0),
                dayOfWeek = dayOfWeek
            )
        )
    }

    private fun recordIncidentAsync(
        type: String,
        status: String,
        reasonCode: String,
        source: String,
        alarmId: Long = currentAlarmId,
        fireId: String = currentFireId,
        scheduledAt: Long = currentScheduledAt
    ) {
        if (alarmId <= 0L) return
        // Repository-owned scope: records issued right before stopSelf()
        // must not race serviceScope.cancel() in onDestroy().
        alarmIncidentRepository.recordAsync(
            alarmId = alarmId,
            fireId = fireId.ifBlank { AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt) },
            scheduledAt = scheduledAt,
            type = type,
            status = status,
            reasonCode = reasonCode,
            source = source
        )
    }

    private suspend fun recordIncident(
        type: String,
        status: String,
        reasonCode: String,
        source: String,
        alarmId: Long = currentAlarmId,
        fireId: String = currentFireId,
        scheduledAt: Long = currentScheduledAt
    ) {
        if (alarmId <= 0L) return
        alarmIncidentRepository.record(
            alarmId = alarmId,
            fireId = fireId.ifBlank { AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt) },
            scheduledAt = scheduledAt,
            type = type,
            status = status,
            reasonCode = reasonCode,
            source = source
        )
    }

    private fun readPersistedSnoozeCount(alarmId: Long): Int {
        if (alarmId <= 0L) return 0
        return runtimeStatePrefs.getInt(snoozeCountKey(alarmId), 0).coerceAtLeast(0)
    }

    private fun persistSnoozeCount(alarmId: Long, count: Int) {
        if (alarmId <= 0L) return
        runtimeStatePrefs.edit()
            .putInt(snoozeCountKey(alarmId), count.coerceAtLeast(0))
            .commit()
    }

    private fun clearAlarmRuntimeState(alarmId: Long) {
        if (alarmId <= 0L) return
        runtimeStatePrefs.edit()
            .remove(snoozeCountKey(alarmId))
            .commit()
    }

    private fun snoozeCountKey(alarmId: Long): String = "snooze_count_$alarmId"

    private fun showMissedNotification(alarm: Alarm, autoSilenceMinutes: Long = DEFAULT_AUTO_SILENCE_MINUTES) {
        val nm = getSystemService(NotificationManager::class.java)
        // v1.5.1: 24-hour preference honoured.
        val timeStr = formatAlarmTime(alarm)
        val label = AlarmPublicText.requiredAlarmLabel(
            label = alarm.label,
            hideLabel = preferencesManager.getCachedSettings().hideAlarmLabelsOnPublicSurfaces
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(getString(R.string.notif_missed_title))
            .setContentText("$label at $timeStr was auto-silenced after $autoSilenceMinutes minutes")
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
                try {
                    while (isActive) {
                        try {
                            cameraManager.setTorchMode(cameraId, true)
                            delay(200)
                            cameraManager.setTorchMode(cameraId, false)
                            delay(300)
                        } catch (_: Exception) {
                            // Sensor access revoked mid-strobe (rare but possible
                            // when the user opens the Camera app during an alarm).
                            break
                        }
                    }
                } finally {
                    // v1.5.1: Always ensure the torch ends up OFF, even if the
                    // loop broke after a successful "on" call.
                    try { cameraManager.setTorchMode(cameraId, false) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopAlarmPlayback() {
        volumeJob?.cancel()
        volumeJob = null
        hapticOnlyJob?.cancel()
        hapticOnlyJob = null
        releaseAlarmMediaSession()
        restoreBackupSoundVolume()
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
        unregisterCallObserver()
        callMutedAudio = false
    }

    /**
     * v1.11.2 (roadmap N2): Register the platform-appropriate call-state
     * observer so a ringing or in-progress phone call mutes the alarm audio
     * without tearing down the rest of the alarm session.
     *
     * Permission note: `TelephonyCallback.CallStateListener` (API 31+) and
     * `PhoneStateListener.onCallStateChanged` (pre-31) both report the bare
     * state value with no permission needed. Only the optional incoming
     * number argument requires `READ_PHONE_STATE`, which we never read.
     *
     * Failure modes (no TelephonyManager on tablets without telephony,
     * `SecurityException` from stripped OEM builds, work-profile constraints)
     * are swallowed — the listener is best-effort and the alarm still rings.
     */
    private fun registerCallObserver() {
        val telephony = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (telephonyCallback != null) return
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        applyCallStateMute(state)
                    }
                }
                telephonyCallback = callback
                telephony.registerTelephonyCallback(mainExecutor, callback)
            } else {
                @Suppress("DEPRECATION")
                if (legacyPhoneStateListener != null) return
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Pre-API-31 fallback")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        applyCallStateMute(state)
                    }
                }
                @Suppress("DEPRECATION")
                legacyPhoneStateListener = listener
                @Suppress("DEPRECATION")
                telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Call-state observer registration failed; alarm will not auto-mute on calls", e)
            telephonyCallback = null
            legacyPhoneStateListener = null
        }
    }

    private fun unregisterCallObserver() {
        val telephony = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { cb ->
                try { telephony?.unregisterTelephonyCallback(cb) } catch (_: Exception) {}
            }
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION")
            legacyPhoneStateListener?.let { listener ->
                try {
                    @Suppress("DEPRECATION")
                    telephony?.listen(listener, PhoneStateListener.LISTEN_NONE)
                } catch (_: Exception) {}
            }
            @Suppress("DEPRECATION")
            legacyPhoneStateListener = null
        }
    }

    /**
     * Volume side-effect for a call-state change. Uses `MediaPlayer.setVolume`
     * (per-player attenuation, 0..1) rather than touching the system
     * STREAM_ALARM volume so we don't fight the gradual-volume coroutine or
     * surprise the user post-call. Vibration is intentionally left alone —
     * tactile wake cues don't interrupt the call audio.
     */
    private fun applyCallStateMute(state: Int) {
        val onCall = state == TelephonyManager.CALL_STATE_OFFHOOK ||
            state == TelephonyManager.CALL_STATE_RINGING
        if (onCall && !callMutedAudio) {
            callMutedAudio = true
            try { mediaPlayer?.setVolume(0f, 0f) } catch (_: Exception) {}
        } else if (!onCall && callMutedAudio) {
            callMutedAudio = false
            try { mediaPlayer?.setVolume(1f, 1f) } catch (_: Exception) {}
        }
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
