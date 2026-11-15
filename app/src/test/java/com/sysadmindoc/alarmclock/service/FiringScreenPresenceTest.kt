package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.service.FiringScreenPresence.RelaunchOutcome
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class FiringScreenPresenceTest {

    private fun newFireId() = "fire-${UUID.randomUUID()}"

    @Test
    fun `a ringing screen that resumed after the relaunch counts as returned`() {
        val fireId = newFireId()
        FiringScreenPresence.shown(fireId)

        val outcome = FiringScreenPresence.relaunchOutcome(stillRinging = true, fireId = fireId)

        assertEquals(RelaunchOutcome.RETURNED, outcome)
        assertEquals("SPOTIFY_FALLBACK_SCREEN_RETURNED", outcome.reasonCode)
        FiringScreenPresence.hidden(fireId)
    }

    @Test
    fun `a screen that Spotify covered and never came back is blocked`() {
        val fireId = newFireId()
        FiringScreenPresence.shown(fireId)
        FiringScreenPresence.hidden(fireId)

        val outcome = FiringScreenPresence.relaunchOutcome(stillRinging = true, fireId = fireId)

        assertEquals(RelaunchOutcome.BLOCKED, outcome)
        assertEquals("SPOTIFY_FALLBACK_SCREEN_BLOCKED", outcome.reasonCode)
    }

    @Test
    fun `a screen that never showed for this fire is blocked`() {
        val outcome = FiringScreenPresence.relaunchOutcome(stillRinging = true, fireId = newFireId())

        assertEquals(RelaunchOutcome.BLOCKED, outcome)
    }

    @Test
    fun `an older screen stopping late does not hide the newer one`() {
        val older = newFireId()
        val newer = newFireId()
        FiringScreenPresence.shown(older)
        FiringScreenPresence.shown(newer)
        FiringScreenPresence.hidden(older)

        assertEquals(
            RelaunchOutcome.RETURNED,
            FiringScreenPresence.relaunchOutcome(stillRinging = true, fireId = newer)
        )
        FiringScreenPresence.hidden(newer)
    }

    @Test
    fun `nothing is recorded once the alarm stopped ringing`() {
        val fireId = newFireId()

        val outcome = FiringScreenPresence.relaunchOutcome(stillRinging = false, fireId = fireId)

        assertEquals(RelaunchOutcome.MOOT, outcome)
        assertNull(outcome.reasonCode)
    }

    @Test
    fun `a blank fire id never counts as showing`() {
        FiringScreenPresence.shown("")

        assertFalse(FiringScreenPresence.isShowing(""))
    }
}
