package com.sysadmindoc.alarmclock.service

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

internal interface AlarmPlaybackPlayer {
    fun setVolume(left: Float, right: Float)
    fun stopAndRelease()
}

private object PlaybackMainThread {
    private val handler = Handler(Looper.getMainLooper())

    fun run(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            handler.post(block)
        }
    }

    fun runBlocking(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
            return
        }

        val result = AtomicReference<Result<Unit>>()
        val latch = CountDownLatch(1)
        handler.post {
            result.set(runCatching(block))
            latch.countDown()
        }
        latch.await()
        result.get().getOrThrow()
    }

    fun <T> callBlocking(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        }

        val result = AtomicReference<Result<T>>()
        val latch = CountDownLatch(1)
        handler.post {
            result.set(runCatching(block))
            latch.countDown()
        }
        latch.await()
        return result.get().getOrThrow()
    }
}

internal fun <T> callOnPlaybackMainThread(block: () -> T): T {
    return PlaybackMainThread.callBlocking(block)
}

internal enum class AlarmPlaybackBackend {
    MEDIA_PLAYER,
    MEDIA3;

    companion object {
        fun fromBuildFlag(useMedia3: Boolean): AlarmPlaybackBackend {
            return if (useMedia3) MEDIA3 else MEDIA_PLAYER
        }
    }
}

internal class MediaPlayerAlarmPlaybackPlayer(
    private val player: MediaPlayer
) : AlarmPlaybackPlayer {
    override fun setVolume(left: Float, right: Float) {
        player.setVolume(left, right)
    }

    override fun stopAndRelease() {
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (_: Exception) {
            // Some platform players throw when queried after an async error.
        } finally {
            player.release()
        }
    }
}

internal class Media3AlarmPlaybackPlayer(
    private val player: ExoPlayer
) : AlarmPlaybackPlayer {
    override fun setVolume(left: Float, right: Float) {
        PlaybackMainThread.run {
            player.volume = minOf(left, right).coerceIn(0f, 1f)
        }
    }

    override fun stopAndRelease() {
        PlaybackMainThread.runBlocking {
            try {
                player.stop()
            } finally {
                player.release()
            }
        }
    }
}
