package com.sysadmindoc.alarmclock.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.*

/**
 * F10: Sleep sound player with configurable auto-fade.
 * Plays a looping sound from res/raw/ and fades out over [fadeMinutes].
 */
class SleepSoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var fadeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun play(rawResId: Int, fadeOutMinutes: Int, fadeDurationSeconds: Int = 60) {
        stop()
        try {
            mediaPlayer = MediaPlayer.create(context, rawResId)?.apply {
                isLooping = true
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(1f, 1f)
                start()
            }

            if (fadeOutMinutes > 0) {
                scheduleFade(fadeOutMinutes, fadeDurationSeconds.coerceIn(5, 600))
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        fadeJob?.cancel()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    fun isPlaying() = mediaPlayer?.isPlaying == true

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
                mediaPlayer?.setVolume(vol, vol)
                delay(stepDelayMs)
            }
            stop()
        }
    }

    fun release() {
        scope.cancel()
        stop()
    }
}
