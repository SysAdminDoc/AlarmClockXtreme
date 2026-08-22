package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.remote.CurrentUnits
import com.sysadmindoc.alarmclock.data.remote.CurrentWeather
import com.sysadmindoc.alarmclock.data.remote.DailyWeather
import com.sysadmindoc.alarmclock.data.remote.WeatherResponse
import com.sysadmindoc.alarmclock.data.repository.CalendarEvent
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
    fun hapticControllerBuildsBoundedEscalatingEnvelopeAtSelectedStrength() {
        val gentleAlarm = Alarm(
            vibrationEnabled = true,
            vibrationPattern = "escalating",
            vibrationIntensity = 1
        )
        val strongAlarm = gentleAlarm.copy(vibrationIntensity = 2)

        val gentle = requireNotNull(AlarmHapticController.escalatingEnvelope(gentleAlarm))
        val strong = requireNotNull(AlarmHapticController.escalatingEnvelope(strongAlarm))

        assertEquals(8, gentle.points.size)
        assertTrue(gentle.points.all { it.durationMillis in 20L..1_000L })
        assertEquals(0f, gentle.points.last().intensity)
        assertEquals(0.55f, gentle.points.maxOf { it.intensity })
        assertEquals(1f, strong.points.maxOf { it.intensity })
        assertEquals(140, AlarmHapticController.waveform(gentleAlarm).amplitudes.max())
        assertEquals(255, AlarmHapticController.waveform(strongAlarm).amplitudes.max())
        assertNull(AlarmHapticController.escalatingEnvelope(gentleAlarm.copy(vibrationPattern = "sos")))
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
        // The template and the spoken clock come from the caller now, so the
        // day and month can follow the locale rather than the enum constant.
        val text = AlarmPostDismissController.morningAnnouncementText(
            template = "It is %1\$s. Today is %2\$s, %3\$s %4\$d.",
            spokenTime = "6:05 AM",
            now = LocalTime.of(6, 5),
            today = LocalDate.of(2026, 7, 2)
        )
        val payload = AlarmPostDismissController.morningBriefingPayload(
            alarm = Alarm(morningRoutine = "Stretch, water"),
            now = LocalTime.of(13, 7),
            today = LocalDate.of(2026, 7, 2),
            is24Hour = false
        )

        assertEquals("It is 6:05 AM. Today is Thursday, July 2.", text)
        assertEquals("1:07 PM", payload.time)
        assertEquals("Thursday, July 2", payload.date)
        assertEquals("", payload.weather)
        assertEquals("", payload.nextEvent)
        assertEquals("Stretch, water", payload.routine)
    }

    @Test
    fun postDismissSummaryIsOptInAndFormatsCachedDayData() {
        assertFalse(AlarmPostDismissController.shouldShowMorningBriefing(AppSettings()))
        assertTrue(
            AlarmPostDismissController.shouldShowMorningBriefing(
                AppSettings(postDismissSummaryEnabled = true)
            )
        )
        val weather = WeatherResponse(
            current = CurrentWeather(
                temperature = 63.6,
                humidity = 50,
                weatherCode = 2,
                windSpeed = 4.0,
                feelsLike = 63.0,
                uvIndex = 1.0
            ),
            hourly = null,
            daily = DailyWeather(
                time = listOf("2026-07-02"),
                maxTemp = listOf(72.2),
                minTemp = listOf(55.7),
                weatherCode = listOf(2),
                precipChance = listOf(30),
                sunrise = null,
                sunset = null,
                uvIndexMax = null
            ),
            currentUnits = CurrentUnits(temperature = "°F")
        )
        val event = CalendarEvent(
            id = 1L,
            title = "Team sync",
            startTime = 1_783_001_400_000L,
            endTime = 1_783_005_000_000L,
            allDay = false,
            location = "",
            calendarColor = 0
        )

        assertEquals(
            "Partly cloudy · 64°F · high 72, low 56 · 30% precipitation",
            // The description is the caller's to resolve: WeatherCodes hands
            // back a resource id and this object has no Context.
            AlarmPostDismissController.cachedWeatherSummary(
                weather = weather,
                describeCode = { code -> if (code == 2) "Partly cloudy" else "" }
            )
        )
        assertTrue(
            AlarmPostDismissController.nextCalendarEventSummary(
                events = listOf(event),
                untitledLabel = "Calendar event",
                allDayTemplate = "%1\$s \u00b7 All day",
                atTemplate = "%1\$s \u00b7 %2\$s",
                nowMillis = event.startTime - 1L,
                is24Hour = false
            ).startsWith("Team sync · ")
        )
        assertEquals(
            "",
            AlarmPostDismissController.nextCalendarEventSummary(
                events = listOf(event),
                untitledLabel = "Calendar event",
                allDayTemplate = "%1\$s \u00b7 All day",
                atTemplate = "%1\$s \u00b7 %2\$s",
                nowMillis = event.endTime + 1L,
                is24Hour = false
            )
        )
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
