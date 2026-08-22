package com.sysadmindoc.alarmclock.ui.alarmfiring

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.Challenge
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ChallengeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for GitHub issue #43.
 *
 * 1. Dismiss was live before the alarm row finished loading, so a challenge-protected
 *    alarm could be turned off without ever showing the challenge.
 * 2. `Settings → Custom typing phrases` never reached the challenge generator.
 */
class FiringDismissGateTest {

    private fun alarmWith(challengeType: String) = Alarm(
        id = 1L,
        hour = 7,
        minute = 0,
        challengeType = challengeType
    )

    @Test
    fun `dismiss is locked before the alarm row has loaded`() {
        // Exactly the initial state the firing screen renders while loadAlarm() runs.
        val loading = FiringUiState()

        assertFalse(
            "Dismiss must stay locked until we know whether a challenge is required",
            loading.canDismiss
        )
    }

    @Test
    fun `dismiss stays locked while a loaded challenge is unsolved`() {
        val state = FiringUiState(
            alarm = alarmWith("TYPING"),
            alarmLoaded = true,
            challenge = Challenge.TypingChallenge(phrase = "wake up"),
            challengeSolved = false
        )

        assertTrue(state.requiresChallenge)
        assertFalse(state.canDismiss)
    }

    @Test
    fun `dismiss unlocks once the challenge is solved`() {
        val state = FiringUiState(
            alarm = alarmWith("TYPING"),
            alarmLoaded = true,
            challenge = Challenge.TypingChallenge(phrase = "wake up"),
            challengeSolved = true
        )

        assertTrue(state.canDismiss)
    }

    @Test
    fun `dismiss unlocks immediately for an alarm with no challenge`() {
        val state = FiringUiState(
            alarm = alarmWith("NONE"),
            alarmLoaded = true,
            challenge = null,
            challengeSolved = true
        )

        assertTrue(state.canDismiss)
    }

    @Test
    fun `accessibility bypass still unlocks dismiss even before load completes`() {
        // The bypass is the guaranteed escape hatch — it must never be gated behind
        // load state, or a failed load could leave a ringing alarm undismissable.
        val state = FiringUiState(challengeBypassAvailable = true)

        assertTrue(state.canDismiss)
    }

    @Test
    fun `location dismissal still gates dismiss after the challenge is solved`() {
        val state = FiringUiState(
            alarm = alarmWith("NONE").copy(locationDismissEnabled = true),
            alarmLoaded = true,
            challenge = null,
            challengeSolved = true,
            locationDismissReady = false
        )

        assertFalse(state.canDismiss)
        assertTrue(state.copy(locationDismissReady = true).canDismiss)
    }

    @Test
    fun `custom typing phrases reach the challenge the firing screen builds`() {
        val custom = "get out of bed right now"
        val alarm = alarmWith("TYPING")

        // Sampled repeatedly because the phrase is drawn at random from the merged pool.
        val produced = (1..300)
            .mapNotNull { buildChallenge(ChallengeType.TYPING, alarm, custom) }
            .filterIsInstance<Challenge.TypingChallenge>()
            .map { it.phrase }
            .toSet()

        assertTrue(
            "Custom phrase never reached the typing challenge the firing screen builds",
            produced.contains(custom)
        )
    }

    @Test
    fun `custom phrases reach the voice challenge too`() {
        val custom = "rise and grind"
        val alarm = alarmWith("VOICE_PHRASE")

        val produced = (1..300)
            .mapNotNull { buildChallenge(ChallengeType.VOICE_PHRASE, alarm, custom) }
            .filterIsInstance<Challenge.VoicePhraseChallenge>()
            .map { it.phrase }
            .toSet()

        assertTrue(produced.contains(custom))
    }

    @Test
    fun `blank custom phrases leave the built-in pool intact`() {
        val challenge = buildChallenge(ChallengeType.TYPING, alarmWith("TYPING"), "")
            as Challenge.TypingChallenge

        assertTrue(challenge.phrase.isNotBlank())
    }

    @Test
    fun `alarm-backed challenges still read their per-alarm data`() {
        val alarm = alarmWith("BARCODE_SCAN").copy(barcodeValue = "12345")
        val challenge = buildChallenge(ChallengeType.BARCODE_SCAN, alarm, "ignored")
            as Challenge.BarcodeChallenge

        assertEquals("12345", challenge.registeredValue)
        assertNull(buildChallenge(ChallengeType.NONE, alarm, ""))
    }

    @Test
    fun `wifi challenge carries the SSID the editor saved`() {
        val alarm = alarmWith("WIFI_CONNECT").copy(wifiDismissSsid = "HomeNet")
        val challenge = buildChallenge(ChallengeType.WIFI_CONNECT, alarm, "ignored")
            as Challenge.WifiChallenge

        assertEquals("HomeNet", challenge.requiredSsid)
    }
}
