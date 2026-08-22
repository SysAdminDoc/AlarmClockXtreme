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
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.CalendarRepository
import com.sysadmindoc.alarmclock.data.repository.WeatherRepository
import com.sysadmindoc.alarmclock.domain.OnCallDndOverride
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.domain.SnoozeCapOutcome
import com.sysadmindoc.alarmclock.domain.SnoozeCapPolicy
import com.sysadmindoc.alarmclock.receiver.DismissReceiver
import com.sysadmindoc.alarmclock.receiver.SnoozeCountdownReceiver
import com.sysadmindoc.alarmclock.receiver.SnoozeReceiver
import com.sysadmindoc.alarmclock.ui.alarmfiring.MorningBriefingActivity
import com.sysadmindoc.alarmclock.util.AlarmPublicText
import com.sysadmindoc.alarmclock.wear.WearNextAlarmBridge
import com.sysadmindoc.alarmclock.worker.WakeConfirmWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
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
    @Inject lateinit var dismissActionExecutor: DismissActionExecutor
    @Inject lateinit var weatherRepository: WeatherRepository
    @Inject lateinit var calendarRepository: CalendarRepository

    companion object {
        const val ACTION_START_ALARM = "com.sysadmindoc.alarmclock.START_ALARM"
        const val ACTION_SNOOZE = "com.sysadmindoc.alarmclock.SNOOZE"
        const val ACTION_DISMISS = "com.sysadmindoc.alarmclock.DISMISS"
        const val ACTION_SET_CHALLENGE_DUCKING =
            "com.sysadmindoc.alarmclock.SET_CHALLENGE_DUCKING"
        const val EXTRA_CUSTOM_SNOOZE_MINUTES = "custom_snooze_minutes"
        const val EXTRA_SNOOZE_UNTIL_MILLIS = "snooze_until_millis"
        const val EXTRA_CHALLENGE_RETRY_COUNT = "challenge_retry_count"
        const val EXTRA_CHALLENGE_SOLVE_TIME_MS = "challenge_solve_time_ms"
        const val EXTRA_WAKE_CONFIRM_REFIRE_COUNT = "wake_confirm_refire_count"
        const val EXTRA_CHALLENGE_DUCKING_ACTIVE = "challenge_ducking_active"
        const val EXTRA_CHALLENGE_DUCK_PERCENT = "challenge_duck_percent"
        private const val MIN_CUSTOM_SNOOZE_MINUTES = 1
        private const val MAX_CUSTOM_SNOOZE_MINUTES = 120

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
        // Grace period for a Media3 player to actually start before we assume it
        // stalled and fall back to the default tone.
        private const val PLAYBACK_START_TIMEOUT_MS = 8_000L
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
                context.getString(R.string.alarm_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_alarm_desc)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(alarmChannel)

            val upcomingChannel = NotificationChannel(
                CHANNEL_UPCOMING,
                context.getString(R.string.upcoming_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_upcoming_desc)
            }
            nm.createNotificationChannel(upcomingChannel)

            val missedChannel = NotificationChannel(
                CHANNEL_MISSED,
                context.getString(R.string.missed_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_missed_desc)
            }

            // v1.12.1 (roadmap N8): timer-finished channel. IMPORTANCE_HIGH
            // so it heads-up and bypasses standard "minimised" treatment,
            // mirroring the missed-alarm class. Vibration is disabled at the
            // channel — the timer's own MediaPlayer + vibrator handle the
            // foreground experience; this notification is the
            // user-isn't-looking-at-the-app surface.
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                context.getString(R.string.notif_channel_timer_finished),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_timer_finished_desc)
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(timerChannel)
            nm.createNotificationChannel(missedChannel)
        }
    }

    private var alarmPlayback: AlarmPlaybackPlayer? = null
    private var mediaSession: MediaSession? = null
    private var vibrator: Vibrator? = null
    private var volumeJob: Job? = null
    private var hapticOnlyJob: Job? = null
    // Fires the guaranteed default tone if the Media3 player never actually
    // starts (stuck buffering with no onPlayerError) so the alarm can't ring silently.
    private var playbackWatchdogJob: Job? = null
    @Volatile
    private var forceBuiltInSpeakerForFallback: Boolean = false
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
    // otherwise leak orphaned playback instances.
    private val audioStarting = AtomicBoolean(false)

    // v1.11.2 (roadmap N2): Telephony-aware muting. When a call is OFFHOOK or
    // RINGING during alarm playback the player is muted (vibration and
    // the firing screen are intentionally kept so the user still has wake
    // cues that don't interrupt the call audio). On IDLE the player is
    // restored to full volume. We register on first startAudio() and
    // unregister in stopAlarmPlayback()/onDestroy() so the listener only
    // exists while the alarm is actually ringing — registering globally
    // would mean every passive call state change touched the player ref.
    @Volatile
    private var callMutedAudio: Boolean = false
    @Volatile
    private var challengeAudioDuckingActive: Boolean = false
    @Volatile
    private var challengeAudioDuckPercent: Int = 35
    @Volatile
    private var playbackRampGain: Float = 1f
    private var telephonyCallback: TelephonyCallback? = null
    @Suppress("DEPRECATION")
    private var legacyPhoneStateListener: PhoneStateListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        OnCallDndOverride.restoreStale(this)
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
                    // A different alarm still mid-ring is being preempted (two
                    // alarms in the same minute, or one firing inside another's
                    // auto-silence window). Finalize it first: record the missed
                    // outcome and re-arm its next occurrence — otherwise a
                    // repeating alarm is left with a stale past nextTriggerTime
                    // and no armed PendingIntent, silently losing every future
                    // occurrence until the next full reschedule pass.
                    val preemptedAlarmId = currentAlarmId
                    if (preemptedAlarmId != -1L && preemptedAlarmId != alarmId) {
                        val preemptedScheduledAt = currentScheduledAt
                        val preemptedFiredAt = alarmFiredAt
                        val preemptedSnoozeCount = currentSnoozeCount
                        val preemptedFireId = currentFireId
                        serviceScope.launch {
                            finalizePreemptedAlarm(
                                alarmId = preemptedAlarmId,
                                scheduledAt = preemptedScheduledAt,
                                firedAt = preemptedFiredAt,
                                snoozeCount = preemptedSnoozeCount,
                                fireId = preemptedFireId
                            )
                        }
                    }
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
                val snoozeAtMillis = intent.getLongExtra(EXTRA_SNOOZE_UNTIL_MILLIS, -1L)
                    .takeIf { it > System.currentTimeMillis() }
                // v1.5.1: If the service was killed+restarted between fire and
                // snooze, currentSnoozeCount is 0 (fresh instance). Re-read the
                // persisted count so the progressive-snooze ladder doesn't reset.
                if (currentAlarmId == -1L && alarmId > 0L) {
                    currentAlarmId = alarmId
                    currentScheduledAt = scheduledAt
                    currentFireId = fireId
                    currentSnoozeCount = readPersistedSnoozeCount(alarmId)
                }
                serviceScope.launch { snoozeAlarm(alarmId, customMinutes, snoozeAtMillis) }
            }
            ACTION_SET_CHALLENGE_DUCKING -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
                if (alarmId == currentAlarmId && alarmId > 0L) {
                    challengeAudioDuckingActive = intent.getBooleanExtra(
                        EXTRA_CHALLENGE_DUCKING_ACTIVE,
                        false
                    )
                    challengeAudioDuckPercent = intent.getIntExtra(
                        EXTRA_CHALLENGE_DUCK_PERCENT,
                        35
                    ).coerceIn(10, 80)
                    applyPlaybackGain()
                }
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
        SnoozeCountdownReceiver.cancel(this)
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

        val settings = preferencesManager.getCurrentSettings()
        OnCallDndOverride.begin(this, settings.onCallModeEnabled)

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

        val firingIntent = AlarmFireDismissContract.firingActivityIntent(
            context = this,
            alarmId = alarmId,
            scheduledAt = currentScheduledAt,
            fireId = currentFireId
        )
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

        AlarmHapticController.vibrationDelayMillis(alarm)?.let { delayMillis ->
            // v1.12.0 (roadmap N7): optional pre-vibration delay. Defers haptic
            // onset for `vibrationDelaySeconds` so it can pair with a long
            // gradualVolumeSeconds fade-in ("audio first, vibration when the
            // fade is well underway"). The delay job is cancellable via
            // volumeJob's parent scope — snooze / dismiss / auto-silence all
            // cancel serviceScope, so a queued vibration never fires after the
            // alarm is gone.
            if (delayMillis > 0) {
                serviceScope.launch {
                    kotlinx.coroutines.delay(delayMillis)
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
        AlarmBroadcastContract.send(
            this, AlarmBroadcastContract.ACTION_ALARM_FIRED,
            alarmId = alarm.id, label = alarm.label,
            displayTime = formatAlarmTime(alarm), fireId = currentFireId
        )

        // Auto-silence after timeout - records as missed
        val autoSilenceMinutes = settings.autoSilenceMinutes.toLong()
        if (autoSilenceMinutes > 0) {
            autoSilenceJob = serviceScope.launch {
                kotlinx.coroutines.delay(autoSilenceMinutes * 60 * 1000L)
                val missedAlarm = repository.getById(alarmId)
                if (missedAlarm != null) {
                    // Once the auto-silence delay elapses the outcome IS "missed".
                    // A dismiss arriving at this exact instant can cancel
                    // serviceScope (onDestroy -> serviceScope.cancel()) and drop
                    // these writes half-finished. NonCancellable guarantees the
                    // missed event, incident, webhook, broadcast, notification,
                    // and repeat-missed state all land atomically before teardown.
                    withContext(NonCancellable) {
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
                        AlarmBroadcastContract.send(
                            this@AlarmService, AlarmBroadcastContract.ACTION_ALARM_MISSED,
                            alarmId = missedAlarm.id, label = missedAlarm.label,
                            displayTime = formatAlarmTime(missedAlarm), fireId = currentFireId
                        )
                        showMissedNotification(missedAlarm, autoSilenceMinutes)
                        // v1.4.0/v1.15.20: Repeat missed alarms — record the alarm
                        // id / timestamp so MissedAlarmUnlockReceiver can re-fire
                        // when the user unlocks or unplugs soon after.
                        if (settings.repeatMissedAlarms) {
                            getSharedPreferences("missed_alarm_state", MODE_PRIVATE)
                                .edit()
                                .putLong("last_missed_at", System.currentTimeMillis())
                                .putLong("last_missed_id", missedAlarm.id)
                                .commit()
                        }
                    }
                }
                clearAlarmRuntimeState(alarmId)
                currentSnoozeCount = 0
                currentAlarmId = -1
                activeAlarm.set(null)
                alarmScheduler.handleAlarmFired(alarmId, currentScheduledAt)
                stopAlarmPlayback()
                if (isForeground.compareAndSet(true, false)) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                stopSelf()
            }
        }

        // v1.2.0: Backup sound escalation
        if (alarm.backupSoundEnabled && !AlarmHapticController.usesMutedAlarmAudio(alarm)) {
            backupSoundJob = serviceScope.launch {
                delay(alarm.backupSoundDelaySec * 1000L)
                // Escalate: set volume to max and switch to system alarm tone.
                // v1.11.2: the call observer still has priority — if we're
                // muted because of a call, escalate the *system* stream so the
                // post-call audio is loud, but don't unmute the player
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
                playbackRampGain = 1f
                applyPlaybackGain()
            }
        }

        // v1.2.0: Flashlight strobe
        val flashingAllowed = com.sysadmindoc.alarmclock.util.MotionPolicy.allowsMotion(
            this,
            settings.reduceMotionAndFlashing
        )
        AlarmFlashlightController.strobePlan(alarm, flashingAllowed)?.let { plan ->
            startFlashlightStrobe(plan)
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
        val fullScreenIntent = AlarmFireDismissContract.firingActivityIntent(
            context = this,
            alarmId = alarm.id,
            scheduledAt = currentScheduledAt,
            fireId = currentFireId
        )
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
            .apply {
                // Dropped once a challenge alarm has spent its snooze cap, so
                // the notification cannot offer a way out the screen refuses.
                if (SnoozeCapPolicy.canSnooze(alarm, currentSnoozeCount)) {
                    addAction(
                        R.drawable.ic_alarm,
                        getString(R.string.notif_snooze_action, alarm.snoozeDurationMinutes),
                        snoozePi
                    )
                }
                // An alarm that gates dismissal behind a challenge or a location
                // must not hand out a one-tap Dismiss from the shade: that
                // skipped the gate entirely. Tapping the notification opens the
                // firing screen, which is where the gate (and its accessibility
                // bypass timer) lives, so the alarm can always still be stopped.
                if (!SnoozeCapPolicy.hasDismissGate(alarm)) {
                    addAction(
                        R.drawable.ic_alarm,
                        getString(R.string.notif_dismiss_action),
                        dismissPi
                    )
                }
            }
            .build()
    }

    /**
     * Re-posts the ongoing alarm notification so its actions match the current
     * snooze state. The alarm keeps ringing throughout.
     */
    private fun refreshFiringNotification(alarm: Alarm) {
        runCatching {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildAlarmNotification(alarm))
        }
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
        if (alarm.ringtoneUri == "silent" || AlarmHapticController.usesMutedAlarmAudio(alarm)) {
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
        // playback instances. The guard is released when the method returns.
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

        // F14: Spotify ringtone — open Spotify URI and skip in-app playback.
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
                    // Delegation is a promise, not proof. A background-activity
                    // start can be refused without throwing, and Spotify can
                    // open without playing (logged out, offline, free tier).
                    // Either way the alarm would be completely silent, so watch
                    // for real playback and fall back if none arrives.
                    armSpotifyPlaybackWatchdog(alarm, mediaStreamsBefore())
                    return
                }
            } catch (_: Exception) {
                // Spotify not installed or URI invalid — fall through to default audio
            }
        }

        when (AlarmPlaybackBackend.fromBuildFlag(BuildConfig.USE_MEDIA3_ALARM_PLAYER)) {
            AlarmPlaybackBackend.MEDIA3 -> startMedia3AudioInternal(alarm)
            AlarmPlaybackBackend.MEDIA_PLAYER -> startMediaPlayerAudioInternal(alarm)
        }
    }

    private fun startMedia3AudioInternal(alarm: Alarm) {
        val radioUrl = alarm.internetRadioUrl.trim()
        if (radioUrl.isNotBlank() && (radioUrl.startsWith("http://", true) || radioUrl.startsWith("https://", true))) {
            if (startMedia3Radio(alarm, radioUrl)) {
                return
            }
        }

        val uri = if (alarm.ringtoneUri.isNotBlank()) {
            runCatching { Uri.parse(alarm.ringtoneUri) }.getOrNull()
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } ?: run {
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_SKIPPED,
                reasonCode = "NO_DEFAULT_TONE",
                source = "AlarmService"
            )
            return
        }

        startMedia3Tone(alarm, uri)
    }

    private fun startMedia3Radio(alarm: Alarm, radioUrl: String): Boolean {
        return try {
            var playbackRef: AlarmPlaybackPlayer? = null
            val playbackStarted = AtomicBoolean(false)
            val playback = createMedia3Playback(
                mediaItem = MediaItem.fromUri(radioUrl),
                audioAttributes = AlarmAudioRouting.media3AlarmMusicAttributes(),
                repeatMode = Player.REPEAT_MODE_OFF,
                initialVolume = alarmPlaybackGain(
                    callMuted = callMutedAudio,
                    challengeDuckingActive = challengeAudioDuckingActive,
                    challengeDuckPercent = challengeAudioDuckPercent,
                    rampGain = 1f
                ),
                onReady = {
                    playbackStarted.set(true)
                    cancelPlaybackWatchdog()
                    updateAlarmMediaSessionState(PlaybackState.STATE_PLAYING)
                    recordIncidentAsync(
                        type = AlarmIncidentEvent.TYPE_AUDIO,
                        status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                        reasonCode = "MEDIA3_INTERNET_RADIO_STARTED",
                        source = "AlarmService"
                    )
                    if (alarm.overrideSystemVolume) {
                        setConfiguredAlarmStreamVolume(alarm)
                    }
                    playbackRampGain = 1f
                    applyPlaybackGain()
                },
                onEnded = {},
                onError = { error ->
                    cancelPlaybackWatchdog()
                    playbackRef?.let { releasePlaybackIfCurrent(it) }
                    recordIncidentAsync(
                        type = AlarmIncidentEvent.TYPE_AUDIO,
                        status = AlarmIncidentEvent.STATUS_FAILED,
                        reasonCode = "MEDIA3_INTERNET_RADIO_ERROR_${error.javaClass.simpleName}",
                        source = "AlarmService"
                    )
                    serviceScope.launch { escalateMedia3PlaybackFailure(alarm) }
                }
            )
            playbackRef = playback
            alarmPlayback = playback
            armMedia3PlaybackWatchdog(alarm, playback, playbackStarted)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_REQUESTED,
                reasonCode = "MEDIA3_INTERNET_RADIO_PREPARING",
                source = "AlarmService"
            )
            true
        } catch (e: Exception) {
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "MEDIA3_INTERNET_RADIO_SETUP_FAILED_${e.javaClass.simpleName}",
                source = "AlarmService"
            )
            try { alarmPlayback?.stopAndRelease() } catch (_: Exception) {}
            alarmPlayback = null
            false
        }
    }

    private fun startMedia3Tone(alarm: Alarm, uri: Uri) {
        try {
            val fadeInMs = alarm.gradualVolumeSeconds * 1000L
            val initialVolume = when {
                fadeInMs > 0 -> 0f
                else -> alarmPlaybackGain(
                    callMuted = callMutedAudio,
                    challengeDuckingActive = challengeAudioDuckingActive,
                    challengeDuckPercent = challengeAudioDuckPercent,
                    rampGain = 1f
                )
            }
            playbackRampGain = if (fadeInMs > 0) 0f else 1f
            var playbackRef: AlarmPlaybackPlayer? = null
            val playbackStarted = AtomicBoolean(false)
            val playback = createMedia3Playback(
                mediaItem = MediaItem.fromUri(uri),
                audioAttributes = AlarmAudioRouting.media3AlarmSonificationAttributes(),
                repeatMode = if (alarm.dismissAtRingtoneEnd) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE,
                initialVolume = initialVolume,
                onReady = {
                    playbackStarted.set(true)
                    cancelPlaybackWatchdog()
                    seekToRandomPoolOffset(alarm, playbackRef)
                    if (alarm.overrideSystemVolume) {
                        setConfiguredAlarmStreamVolume(alarm)
                    }
                    applyPlaybackGain()
                    updateAlarmMediaSessionState(PlaybackState.STATE_PLAYING)
                    recordIncidentAsync(
                        type = AlarmIncidentEvent.TYPE_AUDIO,
                        status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                        reasonCode = "MEDIA3_PLAYER_STARTED",
                        source = "AlarmService"
                    )
                },
                onEnded = {
                    if (alarm.dismissAtRingtoneEnd) {
                        val id = currentAlarmId
                        if (id != -1L) {
                            serviceScope.launch { dismissAlarm(id) }
                        }
                    }
                },
                onError = { error ->
                    cancelPlaybackWatchdog()
                    playbackRef?.let { releasePlaybackIfCurrent(it) }
                    recordIncidentAsync(
                        type = AlarmIncidentEvent.TYPE_AUDIO,
                        status = AlarmIncidentEvent.STATUS_FAILED,
                        reasonCode = "MEDIA3_PLAYER_FAILED_${error.javaClass.simpleName}",
                        source = "AlarmService"
                    )
                    escalateMedia3PlaybackFailure(alarm)
                }
            )
            playbackRef = playback
            alarmPlayback = playback
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_REQUESTED,
                reasonCode = "MEDIA3_PLAYER_PREPARING",
                source = "AlarmService"
            )

            // If the player never reaches READY (for example, a stream stuck
            // buffering with no onPlayerError), escalate before the delayed
            // backup-sound timer gets a chance to fire.
            armMedia3PlaybackWatchdog(alarm, playback, playbackStarted)

            if (fadeInMs > 0) {
                volumeJob = serviceScope.launch {
                    val steps = 50
                    val stepDelay = fadeInMs / steps
                    for (i in 1..steps) {
                        delay(stepDelay)
                        val volume = i.toFloat() / steps
                        playbackRampGain = volume
                        applyPlaybackGain()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start Media3 alarm sound; falling back to default tone", e)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "MEDIA3_PLAYER_SETUP_FAILED_${e.javaClass.simpleName}",
                source = "AlarmService"
            )
            try { alarmPlayback?.stopAndRelease() } catch (_: Exception) {}
            alarmPlayback = null
            escalateMedia3PlaybackFailure(alarm)
        }
    }

    private fun armMedia3PlaybackWatchdog(
        alarm: Alarm,
        playback: AlarmPlaybackPlayer,
        playbackStarted: AtomicBoolean
    ) {
        cancelPlaybackWatchdog()
        playbackWatchdogJob = serviceScope.launch {
            delay(PLAYBACK_START_TIMEOUT_MS)
            if (!playbackStarted.get() && currentAlarmId == alarm.id && alarmPlayback === playback) {
                recordIncidentAsync(
                    type = AlarmIncidentEvent.TYPE_AUDIO,
                    status = AlarmIncidentEvent.STATUS_FAILED,
                    reasonCode = "MEDIA3_PLAYER_STALL_TIMEOUT",
                    source = "AlarmService"
                )
                releasePlaybackIfCurrent(playback)
                escalateMedia3PlaybackFailure(alarm)
            }
        }
    }

    /**
     * Falls back to the in-app tone when nothing is actually playing a few
     * seconds after Spotify was handed the alarm.
     */
    /**
     * How many media streams are playing right now.
     *
     * `isMusicActive` alone is not proof Spotify started: a podcast or
     * white-noise app the user fell asleep to keeps it true, and the alarm
     * would stay silent while the watchdog congratulated itself. Counting
     * active media configurations lets a *new* stream be distinguished from
     * one that was already running.
     */
    private fun mediaStreamsBefore(): Int {
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return 0
        return runCatching {
            audioManager.activePlaybackConfigurations.count {
                it.audioAttributes.usage == AudioAttributes.USAGE_MEDIA
            }
        }.getOrDefault(if (audioManager.isMusicActive) 1 else 0)
    }

    private fun armSpotifyPlaybackWatchdog(alarm: Alarm, mediaStreamsAtLaunch: Int) {
        cancelPlaybackWatchdog()
        playbackWatchdogJob = serviceScope.launch {
            delay(PLAYBACK_START_TIMEOUT_MS)
            if (currentAlarmId != alarm.id || alarmPlayback != null) return@launch
            val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
            val started = audioManager != null && mediaStreamsBefore() > mediaStreamsAtLaunch
            if (started) {
                recordIncidentAsync(
                    type = AlarmIncidentEvent.TYPE_AUDIO,
                    status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                    reasonCode = "SPOTIFY_PLAYBACK_CONFIRMED",
                    source = "AlarmService"
                )
                return@launch
            }
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_AUDIO,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "SPOTIFY_FALLBACK",
                source = "AlarmService"
            )
            startMedia3DefaultFallback(alarm)
        }
    }

    private fun cancelPlaybackWatchdog() {
        playbackWatchdogJob?.cancel()
        playbackWatchdogJob = null
    }

    private fun escalateMedia3PlaybackFailure(alarm: Alarm) {
        cancelPlaybackWatchdog()
        forceBuiltInSpeakerForFallback = true
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            runCatching {
                backupSoundOriginalAlarmVolume = backupSoundOriginalAlarmVolume
                    ?: audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    0
                )
            }.onFailure { error ->
                recordIncidentAsync(
                    type = AlarmIncidentEvent.TYPE_AUDIO,
                    status = AlarmIncidentEvent.STATUS_FAILED,
                    reasonCode = "MEDIA3_FALLBACK_VOLUME_FAILED_${error.javaClass.simpleName}",
                    source = "AlarmService"
                )
            }
        }
        recordIncidentAsync(
            type = AlarmIncidentEvent.TYPE_AUDIO,
            status = AlarmIncidentEvent.STATUS_FAILED,
            reasonCode = "MEDIA3_FALLBACK_ROUTING_FORCED",
            source = "AlarmService"
        )
        startMedia3DefaultFallback(alarm)
    }

    private fun startMedia3DefaultFallback(alarm: Alarm) {
        cancelPlaybackWatchdog()
        recordIncidentAsync(
            type = AlarmIncidentEvent.TYPE_AUDIO,
            status = AlarmIncidentEvent.STATUS_REQUESTED,
            reasonCode = "MEDIA3_DEFAULT_FALLBACK_TO_LEGACY",
            source = "AlarmService"
        )
        startMediaPlayerAudioInternal(
            alarm.copy(
                internetRadioUrl = "",
                spotifyUri = ""
            )
        )
    }

    @AndroidXOptIn(UnstableApi::class)
    private fun createMedia3Playback(
        mediaItem: MediaItem,
        audioAttributes: androidx.media3.common.AudioAttributes,
        repeatMode: Int,
        initialVolume: Float,
        onReady: () -> Unit,
        onEnded: () -> Unit,
        onError: (PlaybackException) -> Unit
    ): AlarmPlaybackPlayer {
        return callOnPlaybackMainThread {
            var readyReported = false
            val player = ExoPlayer.Builder(this)
                .setLooper(Looper.getMainLooper())
                .build()
                .apply {
                    setAudioAttributes(audioAttributes, false)
                    setMediaItem(mediaItem)
                    this.repeatMode = repeatMode
                    volume = initialVolume.coerceIn(0f, 1f)
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    if (!readyReported) {
                                        readyReported = true
                                        onReady()
                                    }
                                }
                                Player.STATE_ENDED -> onEnded()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            onError(error)
                        }
                    })
                    prepare()
                    playWhenReady = true
                }
            forcedSpeakerDevice()?.let { runCatching { player.setPreferredAudioDevice(it) } }
            Media3AlarmPlaybackPlayer(player)
        }
    }

    /**
     * The built-in speaker to force alarm output to when the user enabled
     * "use phone speakers", or null to keep default routing. Never overrides
     * system-managed hearing-aid / BLE devices.
     */
    private fun forcedSpeakerDevice(): android.media.AudioDeviceInfo? {
        val usePhoneSpeakers = forceBuiltInSpeakerForFallback ||
            preferencesManager.getCachedSettings().usePhoneSpeakers
        if (!usePhoneSpeakers) return null
        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return null
        val outputTypes = audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .map { it.type }
        if (!AlarmAudioRouting.shouldForceBuiltInSpeaker(usePhoneSpeakers, outputTypes)) {
            return null
        }
        return AlarmAudioRouting.builtInSpeaker(audioManager)
    }

    private fun releasePlaybackIfCurrent(playback: AlarmPlaybackPlayer) {
        try { playback.stopAndRelease() } catch (_: Exception) {}
        if (alarmPlayback === playback) {
            alarmPlayback = null
        }
    }

    private fun setConfiguredAlarmStreamVolume(alarm: Alarm) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val targetVol = (maxVol * alarm.volume / 100f).toInt().coerceIn(1, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
    }

    private fun startMediaPlayerAudioInternal(alarm: Alarm) {
        // v1.2.0: Internet radio stream. Defensive: only accept http(s) URLs so a
        // malformed setting can't crash MediaPlayer with an unknown scheme.
        val radioUrl = alarm.internetRadioUrl.trim()
        if (radioUrl.isNotBlank() && (radioUrl.startsWith("http://", true) || radioUrl.startsWith("https://", true))) {
            try {
                val player = MediaPlayer()
                val playback = MediaPlayerAlarmPlaybackPlayer(player)
                alarmPlayback = playback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    forcedSpeakerDevice()?.let { runCatching { player.setPreferredDevice(it) } }
                }
                player.apply {
                    setAudioAttributes(AlarmAudioRouting.alarmMusicAttributes())
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
                        playbackRampGain = 1f
                        applyPlaybackGain()
                    }
                    // Without an OnErrorListener, a stream failure (DNS, 404, codec
                    // mismatch) results in a silent alarm — fall back to the device
                    // default ringtone via the standard path below.
                    setOnErrorListener { mp, _, _ ->
                        try { playback.stopAndRelease() } catch (_: Exception) {}
                        if (alarmPlayback === playback) alarmPlayback = null
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
                try { alarmPlayback?.stopAndRelease() } catch (_: Exception) {}
                alarmPlayback = null
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
            val player = MediaPlayer()
            val playback = MediaPlayerAlarmPlaybackPlayer(player)
            alarmPlayback = playback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                forcedSpeakerDevice()?.let { runCatching { player.setPreferredDevice(it) } }
            }
            player.apply {
                setAudioAttributes(AlarmAudioRouting.alarmSonificationAttributes())
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

                seekToRandomPoolOffset(alarm, playback)

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
                playbackRampGain = if (fadeInMs > 0) 0f else 1f
                val initialGain = alarmPlaybackGain(
                    callMuted = callMutedAudio,
                    challengeDuckingActive = challengeAudioDuckingActive,
                    challengeDuckPercent = challengeAudioDuckPercent,
                    rampGain = playbackRampGain
                )
                setVolume(initialGain, initialGain)
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
                        val volume = i.toFloat() / steps
                        playbackRampGain = volume
                        applyPlaybackGain()
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
            try { alarmPlayback?.stopAndRelease() } catch (_: Exception) {}
            alarmPlayback = null
            // Fallback to default alarm sound
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (fallbackUri != null) {
                    val player = MediaPlayer()
                    val playback = MediaPlayerAlarmPlaybackPlayer(player)
                    alarmPlayback = playback
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        forcedSpeakerDevice()?.let { runCatching { player.setPreferredDevice(it) } }
                    }
                    player.apply {
                        setAudioAttributes(AlarmAudioRouting.alarmSonificationAttributes())
                        setDataSource(applicationContext, fallbackUri)
                        isLooping = true
                        prepare()
                        // v1.11.2: honour an active call right out of the gate.
                        playbackRampGain = 1f
                        val gain = alarmPlaybackGain(
                            callMuted = callMutedAudio,
                            challengeDuckingActive = challengeAudioDuckingActive,
                            challengeDuckPercent = challengeAudioDuckPercent,
                            rampGain = playbackRampGain
                        )
                        setVolume(gain, gain)
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

    private fun seekToRandomPoolOffset(alarm: Alarm, playback: AlarmPlaybackPlayer?) {
        if (alarm.ringtonePool.isBlank() || playback == null) return
        // Dismiss-at-ringtone-end auto-dismisses when the track finishes;
        // combined with a random start offset the guaranteed ring window
        // shrinks to the ~15 s seek tail — far too short to wake anyone.
        // A pool alarm with that mode always plays from the top.
        if (alarm.dismissAtRingtoneEnd) return
        val offset = randomRingtoneStartOffsetMs(
            durationMs = playback.durationMs(),
            randomUnit = kotlin.random.Random.nextDouble()
        )
        if (offset > 0L) {
            runCatching { playback.seekTo(offset) }
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

        if (Build.VERSION.SDK_INT >= 36 && startEscalatingEnvelope(alarm)) {
            return
        }

        if (
            alarm.vibrationPattern != "escalating" &&
            AlarmHapticController.usesHapticOnlyProfile(alarm) &&
            startHapticOnlyCompositionLoop()
        ) {
            return
        }

        val waveform = AlarmHapticController.waveform(alarm)

        val vibrationAttributes = alarmVibrationAttributes()
        if (vibrator?.hasAmplitudeControl() == true) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(waveform.pattern, waveform.amplitudes, 0),
                vibrationAttributes
            )
        } else {
            vibrator?.vibrate(VibrationEffect.createWaveform(waveform.pattern, 0), vibrationAttributes)
        }
    }

    @androidx.annotation.RequiresApi(36)
    private fun startEscalatingEnvelope(alarm: Alarm): Boolean {
        val activeVibrator = vibrator ?: return false
        val plan = AlarmHapticController.escalatingEnvelope(alarm) ?: return false
        return runCatching {
            if (!activeVibrator.areEnvelopeEffectsSupported()) return@runCatching false
            val builder = VibrationEffect.BasicEnvelopeBuilder()
                .setInitialSharpness(plan.initialSharpness)
            plan.points.forEach { point ->
                builder.addControlPoint(point.intensity, point.sharpness, point.durationMillis)
            }
            val repeatingEffect = VibrationEffect.createRepeatingEffect(builder.build())
            activeVibrator.vibrate(
                repeatingEffect,
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
            )
            true
        }.getOrDefault(false)
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
                delay(AlarmHapticController.HAPTIC_ONLY_COMPOSITION_INTERVAL_MS)
            }
        }
        return true
    }

    private fun alarmVibrationAttributes(): AudioAttributes {
        return AlarmAudioRouting.alarmSonificationAttributes()
    }

    private suspend fun snoozeAlarm(
        alarmId: Long,
        customMinutes: Int? = null,
        snoozeAtMillis: Long? = null
    ) {
        // A challenge-protected alarm must not be talked out of its challenge
        // by tapping Snooze past the cap, so check before anything is torn
        // down: the alarm has to keep ringing exactly as it was.
        val capped = repository.getById(alarmId)?.sanitized()
        if (capped != null &&
            SnoozeCapPolicy.outcomeFor(capped, currentSnoozeCount) == SnoozeCapOutcome.REFUSE
        ) {
            recordIncident(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_SKIPPED,
                reasonCode = "SNOOZE_CAP_REACHED",
                source = "AlarmService",
                alarmId = capped.id
            )
            refreshFiringNotification(capped)
            return
        }
        SnoozeCountdownReceiver.cancel(this)
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
            if (SnoozeCapPolicy.outcomeFor(alarm, currentSnoozeCount) ==
                SnoozeCapOutcome.AUTO_DISMISS
            ) {
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
                // A user who snoozed to the cap is the strongest still-asleep
                // signal there is — the cap-exhaustion auto-dismiss must not
                // skip the wake-confirmation follow-up a real dismiss gets.
                if (AlarmPostDismissController.shouldScheduleWakeConfirmation(alarm)) {
                    scheduleWakeConfirmation(
                        alarm = alarm,
                        fireId = currentFireId,
                        scheduledAt = currentScheduledAt
                    )
                }
                clearAlarmRuntimeState(alarmId)
                currentSnoozeCount = 0
                currentAlarmId = -1
                activeAlarm.set(null)
                alarmScheduler.handleAlarmFired(alarmId, currentScheduledAt)
                webhookEvent = WebhookEvent.AlarmDismissed
            } else {
                currentSnoozeCount = nextSnoozeCount
                persistSnoozeCount(alarmId, currentSnoozeCount)
                val effectiveSnooze = if (alarm.progressiveSnooze && customMinutes == null && snoozeAtMillis == null) {
                    (alarm.snoozeDurationMinutes - currentSnoozeCount).coerceAtLeast(1)
                } else customMinutes
                val snoozeStartedAt = System.currentTimeMillis()
                val defaultSnoozeEndAt = snoozeStartedAt +
                    (effectiveSnooze ?: alarm.snoozeDurationMinutes)
                        .coerceAtLeast(1) * 60_000L
                val snoozeEndAt = (snoozeAtMillis ?: defaultSnoozeEndAt)
                    .coerceAtLeast(snoozeStartedAt + 1_000L)
                if (alarmScheduler.scheduleSnoozeAt(alarm, snoozeEndAt)) {
                    SnoozeCountdownReceiver.schedule(
                        context = this@AlarmService,
                        startAtMillis = snoozeStartedAt,
                        endAtMillis = snoozeEndAt
                    )
                }
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
            AlarmBroadcastContract.send(
                this, AlarmBroadcastContract.ACTION_ALARM_SNOOZED,
                alarmId = alarm.id, label = alarm.label,
                displayTime = formatAlarmTime(alarm), fireId = webhookFireId
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
        SnoozeCountdownReceiver.cancel(this)
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
            AlarmBroadcastContract.send(
                this, AlarmBroadcastContract.ACTION_ALARM_DISMISSED,
                alarmId = alarm.id, label = alarm.label,
                displayTime = formatAlarmTime(alarm), fireId = wakeConfirmFireId
            )

            // v1.15.1: Per-alarm dismiss action (AlarmKit pattern)
            dismissActionExecutor.executeAsync(alarm)

            // F11: TTS morning announcement
            if (AlarmPostDismissController.shouldSpeakMorningAnnouncement(alarm)) {
                speakMorningAnnouncement()
            }

            // F12: Morning briefing screen
            showMorningBriefing(alarm)

            // F5: Post-alarm wake confirmation
            if (AlarmPostDismissController.shouldScheduleWakeConfirmation(alarm)) {
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
        alarmScheduler.handleAlarmFired(alarmId, currentScheduledAt)
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
    private fun speakMorningAnnouncement() {
        val text = AlarmPostDismissController.morningAnnouncementText()

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
        val settings = preferencesManager.getCachedSettings()
        if (!AlarmPostDismissController.shouldShowMorningBriefing(settings)) return

        val hasLocation = settings.lastKnownLatitude != 0.0 || settings.lastKnownLongitude != 0.0
        val cachedWeather = if (settings.showWeatherOnDashboard) {
            weatherRepository.getCachedWeather(
                latitude = settings.lastKnownLatitude.takeIf { hasLocation },
                longitude = settings.lastKnownLongitude.takeIf { hasLocation }
            )
        } else {
            null
        }
        val events = if (settings.showCalendarOnDashboard) {
            calendarRepository.getTodayEvents().getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val payload = AlarmPostDismissController.morningBriefingPayload(
            alarm = alarm,
            weather = AlarmPostDismissController.cachedWeatherSummary(cachedWeather),
            nextEvent = AlarmPostDismissController.nextCalendarEventSummary(events)
        )

        val intent = Intent(this, MorningBriefingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MorningBriefingActivity.EXTRA_TIME, payload.time)
            putExtra(MorningBriefingActivity.EXTRA_DATE, payload.date)
            putExtra(MorningBriefingActivity.EXTRA_WEATHER, payload.weather)  // Weather cached separately
            putExtra(MorningBriefingActivity.EXTRA_NEXT_EVENT, payload.nextEvent)
            putExtra(MorningBriefingActivity.EXTRA_ROUTINE, payload.routine)
        }
        runCatching { startActivity(intent) }
            .onFailure { Log.w(TAG, "Post-dismiss summary could not be shown", it) }
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
        val plan = AlarmPostDismissController.wakeConfirmationPlan(
            alarm = alarm,
            fireId = fireId,
            scheduledAt = scheduledAt,
            refireCount = refireCount
        )
        val request = OneTimeWorkRequestBuilder<WakeConfirmWorker>()
            .setInitialDelay(plan.delayMinutes, TimeUnit.MINUTES)
            .setInputData(plan.inputData)
            .addTag(plan.tag)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                plan.uniqueWorkName,
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

    /**
     * Completes the lifecycle of an alarm that was still ringing when a
     * different alarm's ACTION_START_ALARM preempted it: records the missed
     * outcome with the *preempted* fire's identifiers (the service fields
     * already hold the new alarm's) and re-arms the next occurrence.
     */
    private suspend fun finalizePreemptedAlarm(
        alarmId: Long,
        scheduledAt: Long,
        firedAt: Long,
        snoozeCount: Int,
        fireId: String
    ) {
        try {
            val alarm = repository.getById(alarmId)
            if (alarm != null) {
                eventRepository.record(
                    AlarmFireDismissContract.alarmEvent(
                        alarm = alarm,
                        scheduledAt = scheduledAt,
                        firedAt = firedAt,
                        action = AlarmEvent.ACTION_MISSED,
                        actionAt = System.currentTimeMillis(),
                        challengeRetryCount = 0,
                        challengeSolveTimeMs = 0L,
                        snoozeCount = snoozeCount
                    )
                )
            }
            alarmIncidentRepository.recordAsync(
                alarmId = alarmId,
                fireId = fireId.ifBlank { AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt) },
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "PREEMPTED_BY_OVERLAPPING_ALARM",
                source = "AlarmService"
            )
            clearAlarmRuntimeState(alarmId)
            alarmScheduler.handleAlarmFired(alarmId, scheduledAt)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize preempted alarm $alarmId", e)
        }
    }

    private suspend fun recordEvent(
        alarm: Alarm,
        action: String,
        challengeRetryCount: Int = 0,
        challengeSolveTimeMs: Long = 0L
    ) {
        eventRepository.record(
            AlarmFireDismissContract.alarmEvent(
                alarm = alarm,
                scheduledAt = currentScheduledAt,
                firedAt = alarmFiredAt,
                action = action,
                actionAt = System.currentTimeMillis(),
                challengeRetryCount = challengeRetryCount,
                challengeSolveTimeMs = challengeSolveTimeMs,
                snoozeCount = currentSnoozeCount
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

    private fun readPersistedSnoozeCount(alarmId: Long): Int =
        AlarmRuntimeState.snoozeCount(this, alarmId)

    private fun persistSnoozeCount(alarmId: Long, count: Int) =
        AlarmRuntimeState.setSnoozeCount(this, alarmId, count)

    private fun clearAlarmRuntimeState(alarmId: Long) =
        AlarmRuntimeState.clear(this, alarmId)

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

    private fun startFlashlightStrobe(plan: AlarmFlashlightStrobePlan) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            flashlightJob = serviceScope.launch {
                try {
                    while (isActive) {
                        try {
                            cameraManager.setTorchMode(cameraId, true)
                            delay(plan.onMillis)
                            cameraManager.setTorchMode(cameraId, false)
                            delay(plan.offMillis)
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
        OnCallDndOverride.end(this)
        volumeJob?.cancel()
        volumeJob = null
        playbackWatchdogJob?.cancel()
        playbackWatchdogJob = null
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
            alarmPlayback?.stopAndRelease()
        } catch (_: Exception) { /* already released */ }
        alarmPlayback = null
        vibrator?.cancel()
        vibrator = null
        unregisterCallObserver()
        callMutedAudio = false
        forceBuiltInSpeakerForFallback = false
        challengeAudioDuckingActive = false
        challengeAudioDuckPercent = 35
        playbackRampGain = 1f
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
     * Volume side-effect for a call-state change. Uses player-local volume
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
            applyPlaybackGain()
        } else if (!onCall && callMutedAudio) {
            callMutedAudio = false
            applyPlaybackGain()
        }
    }

    private fun applyPlaybackGain() {
        val gain = alarmPlaybackGain(
            callMuted = callMutedAudio,
            challengeDuckingActive = challengeAudioDuckingActive,
            challengeDuckPercent = challengeAudioDuckPercent,
            rampGain = playbackRampGain
        )
        try { alarmPlayback?.setVolume(gain, gain) } catch (_: Exception) {}
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
