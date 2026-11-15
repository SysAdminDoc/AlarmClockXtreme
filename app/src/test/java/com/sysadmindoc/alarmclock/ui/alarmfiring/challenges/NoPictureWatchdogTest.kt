package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import android.os.Handler
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * A scanner whose camera never shows a picture has to give up, or the alarm
 * can't be turned off. Only time with the screen in front counts.
 */
@RunWith(RobolectricTestRunner::class)
class NoPictureWatchdogTest {
    private var timeouts = 0
    private val watchdog = NoPictureWatchdog(Handler(Looper.getMainLooper())) { timeouts++ }

    private fun pass(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun `no picture for the whole timeout while in front gives up once`() {
        watchdog.onInFront(true)
        pass(NoPictureWatchdog.TIMEOUT_MS - 1)
        assertEquals(0, timeouts)

        pass(1)
        assertEquals(1, timeouts)

        pass(NoPictureWatchdog.TIMEOUT_MS * 3)
        assertEquals(1, timeouts)
    }

    @Test
    fun `a picture in time keeps it quiet`() {
        watchdog.onInFront(true)
        pass(NoPictureWatchdog.TIMEOUT_MS / 2)
        watchdog.onStreaming(true)
        pass(NoPictureWatchdog.TIMEOUT_MS * 3)
        assertEquals(0, timeouts)
    }

    @Test
    fun `a picture that stops starts the clock again`() {
        watchdog.onInFront(true)
        watchdog.onStreaming(true)
        pass(NoPictureWatchdog.TIMEOUT_MS * 2)
        watchdog.onStreaming(false)
        pass(NoPictureWatchdog.TIMEOUT_MS - 1)
        assertEquals(0, timeouts)

        pass(1)
        assertEquals(1, timeouts)
    }

    @Test
    fun `time in the background doesn't count`() {
        watchdog.onInFront(true)
        pass(NoPictureWatchdog.TIMEOUT_MS - 1_000)
        watchdog.onInFront(false)
        pass(NoPictureWatchdog.TIMEOUT_MS * 3)
        assertEquals(0, timeouts)

        // Back in front, the full timeout starts over.
        watchdog.onInFront(true)
        pass(NoPictureWatchdog.TIMEOUT_MS - 1)
        assertEquals(0, timeouts)
        pass(1)
        assertEquals(1, timeouts)
    }

    @Test
    fun `stopping it cancels a pending timeout`() {
        watchdog.onInFront(true)
        pass(NoPictureWatchdog.TIMEOUT_MS - 1)
        watchdog.stop()
        pass(NoPictureWatchdog.TIMEOUT_MS * 3)
        assertEquals(0, timeouts)
    }
}
