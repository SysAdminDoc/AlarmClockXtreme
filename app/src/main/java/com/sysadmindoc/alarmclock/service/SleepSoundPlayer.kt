package com.sysadmindoc.alarmclock.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import android.util.Log
import com.sysadmindoc.alarmclock.domain.NoiseSynth
import com.sysadmindoc.alarmclock.domain.SleepNoisePreset
import kotlinx.coroutines.*
import kotlin.math.max

/**
 * F10: Sleep sound player with configurable auto-fade.
 * Renders continuous procedural noise and fades out over [fadeMinutes].
 */
class SleepSoundPlayer {

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private var fadeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun play(preset: SleepNoisePreset, fadeOutMinutes: Int, fadeDurationSeconds: Int = 60) {
        stop()
        try {
            val minBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).takeIf { it > 0 } ?: (SAMPLE_RATE / 2)
            val bufferSamples = max(minBytes / BYTES_PER_SAMPLE, SAMPLE_RATE / 4)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSamples * BYTES_PER_SAMPLE)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.setVolume(1f)
            track.play()
            playbackJob = scope.launch(Dispatchers.IO) {
                renderLoop(track, preset, bufferSamples)
            }
            if (fadeOutMinutes > 0) {
                scheduleFade(fadeOutMinutes, fadeDurationSeconds.coerceIn(5, 600))
            }
        } catch (error: Exception) {
            Log.w(TAG, "Failed to start sleep sound", error)
            stop()
        }
    }

    fun stop() {
        fadeJob?.cancel()
        fadeJob = null
        playbackJob?.cancel()
        playbackJob = null
        audioTrack?.let { track ->
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                }
            }
            runCatching { track.release() }
        }
        audioTrack = null
    }

    fun isPlaying() = audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    private suspend fun renderLoop(track: AudioTrack, preset: SleepNoisePreset, bufferSamples: Int) {
        val synth = NoiseSynth(
            preset = preset,
            seed = SystemClock.elapsedRealtimeNanos()
        )
        val buffer = ShortArray(bufferSamples)
        while (currentCoroutineContext().isActive) {
            synth.fill(buffer)
            val written = runCatching {
                track.write(buffer, 0, buffer.size)
            }.getOrElse { error ->
                Log.w(TAG, "Sleep sound render stopped", error)
                return
            }
            if (written < 0) return
        }
    }

    private fun scheduleFade(totalMinutes: Int, fadeDurationSeconds: Int) {
        fadeJob = scope.launch {
            val totalMs = totalMinutes * 60 * 1000L
            val fadeMs = fadeDurationSeconds * 1000L
            val holdMs = totalMs - fadeMs
            if (holdMs > 0) delay(holdMs)

            val steps = 60
            val stepDelayMs = fadeMs / steps
            for (i in steps downTo 0) {
                val vol = i.toFloat() / steps
                audioTrack?.setVolume(vol)
                delay(stepDelayMs)
            }
            stop()
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }

    companion object {
        private const val TAG = "SleepSoundPlayer"
        private const val SAMPLE_RATE = NoiseSynth.DEFAULT_SAMPLE_RATE
        private const val BYTES_PER_SAMPLE = 2
    }
}
