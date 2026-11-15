package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyFallbackTimingTest {

    // When Spotify never plays, the watchdog brings the ringing screen back
    // over Spotify with a plain startActivity. Android only lets that through
    // for about 10 s after one of the app's windows was last visible
    // (BAL_ALLOW_GRACE_PERIOD), and a blocked start doesn't throw, so a longer
    // timeout would leave Dismiss hidden behind Spotify without a trace.
    @Test
    fun `the Spotify fallback fires inside the background start grace period`() {
        val backgroundStartGraceMs = 10_000L
        assertTrue(
            "PLAYBACK_START_TIMEOUT_MS must stay under Android's ~10 s grace period",
            AlarmService.PLAYBACK_START_TIMEOUT_MS < backgroundStartGraceMs
        )
    }
}
