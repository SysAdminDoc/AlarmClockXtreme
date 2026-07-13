package com.sysadmindoc.alarmclock.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.actigraphy.ActigraphySessionSummary
import com.sysadmindoc.alarmclock.data.repository.ActigraphyRepository
import com.sysadmindoc.alarmclock.data.repository.SnoreEventRepository
import com.sysadmindoc.alarmclock.data.sonar.SonarSleepSessionSummarizer
import com.sysadmindoc.alarmclock.domain.SnoreEventCandidate
import com.sysadmindoc.alarmclock.domain.SnoreEventDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sqrt
import javax.inject.Inject

/**
 * F17: Experimental sonar-based sleep tracking.
 * Emits an inaudible 18–20 kHz tone via AudioTrack, then measures the Doppler-shifted
 * reflection with AudioRecord to detect breathing / movement patterns.
 *
 * HOW IT WORKS:
 * - AudioTrack plays a continuous 18,750 Hz sine wave at ~1% volume (above audible range
 *   for most adults; verify with target device; dogs and cats will hear it)
 * - AudioRecord captures at 44100 Hz; we compute RMS of short 50 ms windows
 * - High RMS variance → movement detected → likely light/REM sleep
 * - Low RMS variance over several consecutive windows → deep sleep detected
 * - Calls [onMovementCallback] / [onDeepSleepCallback] for external consumers
 *
 * NOTE: This is genuinely experimental. Results vary widely by room acoustics and device
 * microphone placement. Use alongside SmartAlarmService (accelerometer) for best results.
 *
 * Requires: RECORD_AUDIO, MODIFY_AUDIO_SETTINGS permissions.
 */
@AndroidEntryPoint
class SonarSleepService : Service() {

    companion object {
        const val ACTION_START = "com.sysadmindoc.alarmclock.SONAR_START"
        const val ACTION_STOP = "com.sysadmindoc.alarmclock.SONAR_STOP"
        const val CHANNEL_SONAR = "sonar_sleep_channel"
        const val NOTIF_ID = 2002

        private const val SAMPLE_RATE = 44100
        private const val TONE_HZ = 18750          // ~18.75 kHz — inaudible for most adults
        private const val WINDOW_MS = 50L
        private const val DEEP_SLEEP_THRESHOLD = 0.004f  // RMS variance below this = deep sleep
        private const val DEEP_SLEEP_WINDOWS = 6          // 6 × 50ms = 300ms of stillness
        private const val PREFS_NAME = "sonar_sleep_state"
        private const val KEY_ACTIVE = "active"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_LAST_ENDED_AT = "last_ended_at"
        private const val KEY_LAST_TOTAL_MINUTES = "last_total_minutes"
        private const val KEY_LAST_AWAKE_MINUTES = "last_awake_minutes"
        private const val KEY_LAST_LIGHT_MINUTES = "last_light_minutes"
        private const val KEY_LAST_DEEP_MINUTES = "last_deep_minutes"
        private const val KEY_LAST_SNORE_EVENTS = "last_snore_events"
        private const val KEY_LAST_SNORE_PEAK_DB = "last_snore_peak_db"

        fun readSnapshot(context: Context): SonarSleepSnapshot {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val storedActive = prefs.getBoolean(KEY_ACTIVE, false)
            val active = storedActive && isServiceRunning(context)
            if (storedActive && !active) {
                prefs.edit()
                    .putBoolean(KEY_ACTIVE, false)
                    .putLong(KEY_STARTED_AT, 0L)
                    .apply()
            }
            return SonarSleepSnapshot(
                active = active,
                startedAt = if (active) prefs.getLong(KEY_STARTED_AT, 0L) else 0L,
                lastEndedAt = prefs.getLong(KEY_LAST_ENDED_AT, 0L),
                lastTotalMinutes = prefs.getInt(KEY_LAST_TOTAL_MINUTES, 0),
                lastAwakeMinutes = prefs.getInt(KEY_LAST_AWAKE_MINUTES, 0),
                lastLightMinutes = prefs.getInt(KEY_LAST_LIGHT_MINUTES, 0),
                lastDeepMinutes = prefs.getInt(KEY_LAST_DEEP_MINUTES, 0),
                lastSnoreEventCount = prefs.getInt(KEY_LAST_SNORE_EVENTS, 0),
                lastSnorePeakDb = prefs.getFloat(KEY_LAST_SNORE_PEAK_DB, 0f)
            )
        }

        private fun isServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(ActivityManager::class.java) ?: return false
            @Suppress("DEPRECATION")
            return manager.getRunningServices(Int.MAX_VALUE).any { service ->
                service.service.className == SonarSleepService::class.java.name
            }
        }
    }

    @Inject lateinit var actigraphyRepository: ActigraphyRepository
    @Inject lateinit var snoreEventRepository: SnoreEventRepository

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    private val sessionLock = Any()
    private var sessionStartedAt = 0L
    private var stillWindows = 0
    private var movementWindows = 0
    private var sessionRecorded = false
    private var snoreDetector = SnoreEventDetector()
    private val snoreEvents = mutableListOf<SnoreEventCandidate>()

    // Callbacks: set by binding consumer or broadcast receivers
    var onMovementCallback: (() -> Unit)? = null
    var onDeepSleepCallback: (() -> Unit)? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSonar()
            ACTION_STOP -> stopSonarAndSelf()
        }
        return START_NOT_STICKY  // Experimental sleep session; don't auto-restart on death
    }

    override fun onDestroy() {
        stopSonarHardware()
        // If the OS killed us mid-session (task swipe, low memory, Doze eviction)
        // rather than an explicit ACTION_STOP, persist what we captured so the
        // night's sleep stages and snore timeline aren't silently discarded.
        // recordSession is idempotent (guards on sessionRecorded), so this is a
        // no-op when ACTION_STOP already recorded. Bounded so service teardown
        // can't hang; Room writes here are small and local.
        if (sessionStartedAt > 0L && !sessionRecorded) {
            runCatching {
                runBlocking {
                    withTimeoutOrNull(2_000L) { recordSession("SONAR_SERVICE_DESTROYED") }
                }
            }
        }
        markInactive()
        scope.cancel()
        super.onDestroy()
    }

    // ── Sonar engine ──────────────────────────────────────────────────────────

    private fun startSonar() {
        synchronized(sessionLock) {
            if (sessionStartedAt > 0L) return
            sessionStartedAt = System.currentTimeMillis()
            stillWindows = 0
            movementWindows = 0
            sessionRecorded = false
            snoreDetector = SnoreEventDetector()
            snoreEvents.clear()
        }
        markActive(sessionStartedAt)
        scope.launch {
            try {
                startToneEmitter()
                startReflectionAnalyzer()
            } catch (_: Exception) {
                recordSession("SONAR_START_FAILED")
                stopSonarHardware()
                stopSelf()
            }
        }
    }

    private fun startToneEmitter() {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val samples = ShortArray(bufferSize)
        // Pre-fill with a sine wave at TONE_HZ, amplitude ~1% of max (inaudible level)
        val amplitude = 32767 * 0.01f
        for (i in samples.indices) {
            samples[i] = (amplitude * Math.sin(2.0 * Math.PI * TONE_HZ * i / SAMPLE_RATE)).toInt().toShort()
        }

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .apply {
                play()
            }

        // Continuously write tone buffer in a loop. Catch any exception from
        // write() (e.g. AudioTrack released from stopSonarHardware) instead of
        // letting it bubble up and crash the service.
        scope.launch {
            while (isActive) {
                try {
                    val track = audioTrack ?: break
                    val written = track.write(samples, 0, samples.size)
                    if (written < 0) break
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun CoroutineScope.startReflectionAnalyzer() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).apply { startRecording() }

        val windowSamples = (SAMPLE_RATE * WINDOW_MS / 1000).toInt()
        val buffer = ShortArray(windowSamples)
        val recentRms = ArrayDeque<Float>(DEEP_SLEEP_WINDOWS + 1)
        var deepSleepCount = 0

        while (isActive) {
            val read = audioRecord?.read(buffer, 0, windowSamples) ?: break
            if (read <= 0) { delay(WINDOW_MS); continue }

            val rms = rms(buffer, read)
            recordSnoreWindow(rms)
            recentRms.addLast(rms)
            if (recentRms.size > DEEP_SLEEP_WINDOWS) recentRms.removeFirst()

            val variance = variance(recentRms)
            val still = variance < DEEP_SLEEP_THRESHOLD
            recordSonarWindow(still)

            if (still) {
                deepSleepCount++
                if (deepSleepCount >= DEEP_SLEEP_WINDOWS) {
                    onDeepSleepCallback?.invoke()
                    deepSleepCount = 0
                }
            } else {
                if (deepSleepCount > 0) onMovementCallback?.invoke()
                deepSleepCount = 0
            }

            delay(WINDOW_MS)
        }
    }

    private fun rms(buffer: ShortArray, len: Int): Float {
        var sum = 0.0
        for (i in 0 until len) sum += (buffer[i].toDouble() / 32768.0).let { it * it }
        return sqrt(sum / len).toFloat()
    }

    private fun variance(values: Collection<Float>): Float {
        if (values.size < DEEP_SLEEP_WINDOWS) return Float.MAX_VALUE // Not enough data yet
        val mean = values.average().toFloat()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    private fun stopSonarAndSelf() {
        scope.launch {
            stopSonarHardware()
            recordSession("SONAR_STOPPED")
            stopSelf()
        }
    }

    private fun stopSonarHardware() {
        // Note: previous code relied on `;` inside a single-expression let, which was
        // brittle. Using explicit blocks + try/catch makes both branches obviously
        // safe even if the underlying audio resources are already released.
        audioTrack?.let { track ->
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            } catch (_: Exception) {}
            try { track.release() } catch (_: Exception) {}
        }
        audioTrack = null
        audioRecord?.let { record ->
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
            } catch (_: Exception) {}
            try { record.release() } catch (_: Exception) {}
        }
        audioRecord = null
    }

    private fun recordSonarWindow(still: Boolean) {
        synchronized(sessionLock) {
            if (still) stillWindows++ else movementWindows++
        }
    }

    private fun recordSnoreWindow(rms: Float) {
        val windowStartedAt = System.currentTimeMillis()
        synchronized(sessionLock) {
            val completed = snoreDetector.acceptWindow(
                windowStartedAt = windowStartedAt,
                windowDurationMs = WINDOW_MS,
                rms = rms
            )
            if (completed != null) {
                snoreEvents.add(completed)
            }
        }
    }

    private suspend fun recordSession(decisionReason: String) {
        val endedAt = System.currentTimeMillis()
        val startedAt: Long
        val still: Int
        val movement: Int
        val snore: List<SnoreEventCandidate>
        synchronized(sessionLock) {
            if (sessionStartedAt == 0L || sessionRecorded) return
            sessionRecorded = true
            snoreDetector.flush()?.let(snoreEvents::add)
            startedAt = sessionStartedAt
            still = stillWindows
            movement = movementWindows
            snore = snoreEvents.toList()
        }

        val summary = SonarSleepSessionSummarizer.summarize(
            startedAt = startedAt,
            endedAt = endedAt,
            stillWindows = still,
            movementWindows = movement
        )

        runCatching {
            actigraphyRepository.record(
                alarmId = 0L,
                startedAt = startedAt,
                endedAt = endedAt,
                targetTime = endedAt,
                firedEarly = false,
                summary = summary,
                decisionReason = decisionReason,
                observedMinutesBeforeDecision = summary.totalMinutes,
                smartWakeMode = "SONAR"
            )
        }
        runCatching {
            snoreEventRepository.recordAll(
                sessionStartedAt = startedAt,
                events = snore
            )
        }
        persistLastSession(endedAt, summary, snore)
        synchronized(sessionLock) {
            sessionStartedAt = 0L
            stillWindows = 0
            movementWindows = 0
            snoreEvents.clear()
        }
    }

    private fun markActive(startedAt: Long) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_STARTED_AT, startedAt)
            .apply()
    }

    private fun markInactive() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACTIVE, false)
            .putLong(KEY_STARTED_AT, 0L)
            .apply()
    }

    private fun persistLastSession(
        endedAt: Long,
        summary: ActigraphySessionSummary,
        snoreEvents: List<SnoreEventCandidate>
    ) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACTIVE, false)
            .putLong(KEY_STARTED_AT, 0L)
            .putLong(KEY_LAST_ENDED_AT, endedAt)
            .putInt(KEY_LAST_TOTAL_MINUTES, summary.totalMinutes)
            .putInt(KEY_LAST_AWAKE_MINUTES, summary.awakeMinutes)
            .putInt(KEY_LAST_LIGHT_MINUTES, summary.lightMinutes)
            .putInt(KEY_LAST_DEEP_MINUTES, summary.deepMinutes)
            .putInt(KEY_LAST_SNORE_EVENTS, snoreEvents.size)
            .putFloat(KEY_LAST_SNORE_PEAK_DB, snoreEvents.maxOfOrNull { it.peakDb } ?: 0f)
            .apply()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_SONAR) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_SONAR, "Sonar Sleep Tracking", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Overnight sleep quality monitoring (experimental)" }
            )
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_SONAR)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Sleep tracking active")
            .setContentText("Sonar monitoring in progress")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
}

data class SonarSleepSnapshot(
    val active: Boolean,
    val startedAt: Long,
    val lastEndedAt: Long,
    val lastTotalMinutes: Int,
    val lastAwakeMinutes: Int,
    val lastLightMinutes: Int,
    val lastDeepMinutes: Int,
    val lastSnoreEventCount: Int,
    val lastSnorePeakDb: Float
)
