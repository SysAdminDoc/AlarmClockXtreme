package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.worker.WakeConfirmWorker
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmServiceControllerTest {
    @Test
    fun hapticControllerPlansDelayAndMutedProfile() {
        val muted = Alarm(
            id = 7,
            vibrationEnabled = true,
            vibrationDelaySeconds = 45,
            overrideSystemVolume = true,
            volume = 0
        )

        assertEquals(45_000L, AlarmHapticController.vibrationDelayMillis(muted))
        assertTrue(AlarmHapticController.usesMutedAlarmAudio(muted))
        assertTrue(AlarmHapticController.usesHapticOnlyProfile(muted))
        assertNull(AlarmHapticController.vibrationDelayMillis(muted.copy(vibrationEnabled = false)))
    }

    @Test
    fun hapticControllerKeepsLegacyWaveforms() {
        val gentle = Alarm(vibrationEnabled = true, vibrationPattern = "gentle")
        val sos = Alarm(vibrationEnabled = true, vibrationPattern = "sos")
        val softDefault = Alarm(vibrationEnabled = true, vibrationIntensity = 1)

        assertArrayEquals(longArrayOf(0, 200, 1200, 200, 1200), AlarmHapticController.waveform(gentle).pattern)
        assertArrayEquals(intArrayOf(0, 60, 0, 60, 0), AlarmHapticController.waveform(gentle).amplitudes)
        assertArrayEquals(
            longArrayOf(0, 150, 100, 150, 100, 150, 300, 400, 100, 400, 100, 400, 300, 150, 100, 150, 100, 150, 600),
            AlarmHapticController.waveform(sos).pattern
        )
        assertArrayEquals(longArrayOf(0, 200, 1000, 200, 1000), AlarmHapticController.waveform(softDefault).pattern)
    }

    @Test
    fun flashlightControllerOnlyPlansEnabledStrobe() {
        assertNull(AlarmFlashlightController.strobePlan(Alarm(flashlightStrobe = false)))

        val plan = AlarmFlashlightController.strobePlan(Alarm(flashlightStrobe = true))
        assertEquals(200L, plan?.onMillis)
        assertEquals(300L, plan?.offMillis)
        assertNull(
            AlarmFlashlightController.strobePlan(
                Alarm(flashlightStrobe = true),
                flashingAllowed = false
            )
        )
    }

    @Test
    fun postDismissControllerBuildsAnnouncementAndBriefingPayload() {
        val text = AlarmPostDismissController.morningAnnouncementText(
            now = LocalTime.of(6, 5),
            today = LocalDate.of(2026, 7, 2)
        )
        val payload = AlarmPostDismissController.morningBriefingPayload(
            alarm = Alarm(morningRoutine = "Stretch, water"),
            now = LocalTime.of(13, 7),
            today = LocalDate.of(2026, 7, 2)
        )

        assertEquals("It is 6 oh 5 A.M.. Today is Thursday, July 2.", text)
        assertEquals("1:07 PM", payload.time)
        assertEquals("Thursday, July 2", payload.date)
        assertEquals("", payload.weather)
        assertEquals("", payload.nextEvent)
        assertEquals("Stretch, water", payload.routine)
    }

    @Test
    fun postDismissControllerBuildsWakeConfirmationPlan() {
        val alarm = Alarm(id = 42, wakeConfirmEnabled = true, wakeConfirmDelayMinutes = 0)
        val plan = AlarmPostDismissController.wakeConfirmationPlan(
            alarm = alarm,
            fireId = "fire-42",
            scheduledAt = 123_456L,
            refireCount = 2
        )

        assertTrue(AlarmPostDismissController.shouldScheduleWakeConfirmation(alarm))
        assertFalse(AlarmPostDismissController.shouldSpeakMorningAnnouncement(alarm))
        assertEquals("wake_confirm_42", plan.uniqueWorkName)
        assertEquals("wake_confirm_42", plan.tag)
        assertEquals(1L, plan.delayMinutes)
        assertEquals(42L, plan.inputData.getLong(WakeConfirmWorker.KEY_ALARM_ID, -1L))
        assertEquals("fire-42", plan.inputData.getString(WakeConfirmWorker.KEY_ALARM_FIRE_ID))
        assertEquals(123_456L, plan.inputData.getLong(WakeConfirmWorker.KEY_SCHEDULED_AT, -1L))
        assertEquals(2, plan.inputData.getInt(WakeConfirmWorker.KEY_REFIRE_COUNT, -1))
    }
}
