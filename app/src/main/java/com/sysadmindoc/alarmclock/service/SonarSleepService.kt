package com.sysadmindoc.alarmclock.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sqrt

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
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null

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
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        stopSonarHardware()
        super.onDestroy()
    }

    // ── Sonar engine ──────────────────────────────────────────────────────────

    private fun startSonar() {
        scope.launch {
            try {
                startToneEmitter()
                startReflectionAnalyzer()
            } catch (_: Exception) {
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

        // Continuously write tone buffer in a loop
        scope.launch {
            while (isActive) {
                audioTrack?.write(samples, 0, samples.size)
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
            recentRms.addLast(rms)
            if (recentRms.size > DEEP_SLEEP_WINDOWS) recentRms.removeFirst()

            val variance = variance(recentRms)

            if (variance < DEEP_SLEEP_THRESHOLD) {
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
        stopSonarHardware()
        stopSelf()
    }

    private fun stopSonarHardware() {
        audioTrack?.let { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop(); it.release() }
        audioTrack = null
        audioRecord?.let { if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop(); it.release() }
        audioRecord = null
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
