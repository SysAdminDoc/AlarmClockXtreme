package com.sysadmindoc.alarmclock.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.sysadmindoc.alarmclock.domain.EnvironmentalNoiseBaseline
import com.sysadmindoc.alarmclock.domain.EnvironmentalNoiseBaselinePolicy
import com.sysadmindoc.alarmclock.domain.EnvironmentalNoiseLevel
import kotlin.math.sqrt

data class BedtimeNoiseBaselineSnapshot(
    val baseline: EnvironmentalNoiseBaseline? = null,
    val measuredAtMillis: Long = 0L
)

object BedtimeNoiseBaselineSampler {
    private const val PREFS_NAME = "bedtime_noise_baseline"
    private const val KEY_LEVEL = "level"
    private const val KEY_DBFS = "dbfs"
    private const val KEY_MEASURED_AT = "measured_at"
    private const val SAMPLE_RATE = 16_000
    private const val SAMPLE_DURATION_MS = 700L
    private const val MAX_READ_BUFFER_SAMPLES = 2_048

    fun sampleAndPersist(context: Context): EnvironmentalNoiseBaseline? {
        val baseline = sample(context) ?: return null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LEVEL, baseline.level.name)
            .putFloat(KEY_DBFS, baseline.dbfs)
            .putLong(KEY_MEASURED_AT, System.currentTimeMillis())
            .apply()
        return baseline
    }

    fun readSnapshot(context: Context): BedtimeNoiseBaselineSnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val measuredAt = prefs.getLong(KEY_MEASURED_AT, 0L)
        val level = prefs.getString(KEY_LEVEL, null)
            ?.let { runCatching { EnvironmentalNoiseLevel.valueOf(it) }.getOrNull() }
        val dbfs = prefs.getFloat(KEY_DBFS, Float.NaN)
        if (measuredAt <= 0L || level == null || !dbfs.isFinite()) {
            return BedtimeNoiseBaselineSnapshot()
        }
        return BedtimeNoiseBaselineSnapshot(
            baseline = EnvironmentalNoiseBaseline(dbfs = dbfs, level = level),
            measuredAtMillis = measuredAt
        )
    }

    @SuppressLint("MissingPermission")
    private fun sample(context: Context): EnvironmentalNoiseBaseline? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val minBufferBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferBytes <= 0) return null

        var recorder: AudioRecord? = null
        return runCatching {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferBytes
            )
            val activeRecorder = recorder ?: return@runCatching null
            activeRecorder.startRecording()
            if (activeRecorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return@runCatching null
            }

            val bufferSamples = (minBufferBytes / 2)
                .coerceIn(256, MAX_READ_BUFFER_SAMPLES)
            val buffer = ShortArray(bufferSamples)
            val deadline = System.currentTimeMillis() + SAMPLE_DURATION_MS
            var totalSamples = 0
            var squareSum = 0.0

            while (System.currentTimeMillis() < deadline) {
                val read = activeRecorder.read(buffer, 0, buffer.size)
                if (read <= 0) continue
                for (i in 0 until read) {
                    val normalized = buffer[i].toDouble() / Short.MAX_VALUE.toDouble()
                    squareSum += normalized * normalized
                }
                totalSamples += read
            }

            if (totalSamples == 0) {
                null
            } else {
                EnvironmentalNoiseBaselinePolicy.fromNormalizedRms(
                    sqrt(squareSum / totalSamples).toFloat()
                )
            }
        }.getOrNull().also {
            recorder?.let { activeRecorder ->
                runCatching {
                    if (activeRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        activeRecorder.stop()
                    }
                }
                runCatching { activeRecorder.release() }
            }
        }
    }
}
