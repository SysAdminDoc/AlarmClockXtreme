package com.sysadmindoc.alarmclock.domain

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Operation
import androidx.work.WorkManager
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.CalendarEvent
import com.sysadmindoc.alarmclock.data.repository.CalendarRepository
import com.sysadmindoc.alarmclock.data.repository.HolidayRepository
import com.sysadmindoc.alarmclock.directboot.DirectBootAlarmCache
import com.sysadmindoc.alarmclock.receiver.AlarmReceiver
import com.sysadmindoc.alarmclock.widget.WidgetUpdater
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.time.DayOfWeek
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class AlarmSchedulerTest {
    private lateinit var context: Context
    private lateinit var repository: AlarmRepository
    private lateinit var calculator: NextAlarmCalculator
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var holidayRepository: HolidayRepository
    private lateinit var incidentRepository: AlarmIncidentRepository
    private lateinit var weatherRepository: com.sysadmindoc.alarmclock.data.repository.WeatherRepository
    private lateinit var workManager: WorkManager
    private lateinit var calendarRepository: CalendarRepository
    private lateinit var scheduler: AlarmScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)
        calculator = mockk()
        preferencesManager = mockk()
        holidayRepository = mockk()
        incidentRepository = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        calendarRepository = mockk()
        every { calendarRepository.hasReadPermission() } returns false

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        every { workManager.cancelUniqueWork(any()) } returns mockk<Operation>(relaxed = true)

        mockkObject(WidgetUpdater)
        every { WidgetUpdater.requestUpdate(any()) } just Runs

        coEvery { preferencesManager.getCurrentSettings() } returns AppSettings()
        every { preferencesManager.getCachedSettings() } returns AppSettings()
        coEvery { holidayRepository.isHoliday(any()) } returns false

        weatherRepository = mockk(relaxed = true)
        every { weatherRepository.getCachedWeather() } returns null

        scheduler = AlarmScheduler(
            context = context,
            repository = repository,
            calculator = calculator,
            preferencesManager = preferencesManager,
            holidayRepository = holidayRepository,
            alarmIncidentRepository = incidentRepository,
            weatherRepository = weatherRepository,
            calendarRepository = calendarRepository
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun scheduleFutureAlarmWritesNextTriggerAndRegistersAlarmClock() = runTest {
        val triggerTime = System.currentTimeMillis() + 15 * 60_000L
        val alarm = enabledAlarm(id = 42L)
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime

        scheduler.schedule(alarm, requestWidgetUpdate = false)

        coVerify { repository.updateNextTrigger(42L, triggerTime) }
        assertEquals(
            1,
            shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.size
        )
    }

    @Test
    fun scheduleFutureAlarmCanHideStatusIconWithExactIdleRegistration() = runTest {
        val triggerTime = System.currentTimeMillis() + 15 * 60_000L
        val alarm = enabledAlarm(id = 44L)
        every { preferencesManager.getCachedSettings() } returns
            AppSettings(showAlarmClockIcon = false)
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime

        scheduler.schedule(alarm, requestWidgetUpdate = false)

        val scheduledAlarm = shadowOf(context.getSystemService(AlarmManager::class.java))
            .scheduledAlarms
            .single()
        assertEquals(null, scheduledAlarm.alarmClockInfo)
        assertTrue(scheduledAlarm.isAllowWhileIdle)
        verify {
            incidentRepository.recordAsync(
                alarmId = 44L,
                fireId = any(),
                scheduledAt = triggerTime,
                eventAt = any(),
                type = AlarmIncidentEvent.TYPE_SCHEDULE,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "SET_EXACT_ALLOW_WHILE_IDLE",
                source = "AlarmScheduler"
            )
        }
    }

    @Test
    fun scheduleSnoozeAtPersistsSelectedTriggerAndRegistersAlarmClock() = runTest {
        val target = System.currentTimeMillis() + 15 * 60_000L
        val alarm = enabledAlarm(id = 43L)

        scheduler.scheduleSnoozeAt(alarm, target)

        coVerify { repository.updateNextTrigger(43L, target) }
        assertEquals(
            1,
            shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.size
        )
    }

    @Test
    fun cancelRemovesFollowUpWorkersAndRequestsWidgetRefresh() {
        scheduler.cancel(42L)

        verify { workManager.cancelUniqueWork("hue_sunrise_42") }
        verify { workManager.cancelUniqueWork("guardian_42") }
        verify { workManager.cancelUniqueWork("wake_confirm_42") }
        verify { WidgetUpdater.requestUpdate(context) }
    }

    @Test
    fun cancelLeavesADisarmedRowInTheIncidentLog() = runTest {
        every { calculator.calculate(any<Alarm>(), any()) } returns
            System.currentTimeMillis() + 15 * 60_000L
        scheduler.schedule(enabledAlarm(id = 42L), requestWidgetUpdate = false)
        assertTrue(scheduler.isArmed(42L))

        scheduler.cancel(42L)
        // Deleting it again, or turning off an alarm already off, adds nothing.
        scheduler.cancel(42L)

        assertFalse(scheduler.isArmed(42L))
        verify(exactly = 1) {
            incidentRepository.recordAsync(
                alarmId = 42L,
                fireId = any(),
                scheduledAt = 0L,
                eventAt = any(),
                type = AlarmIncidentEvent.TYPE_SCHEDULE,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = AlarmIncidentEvent.REASON_CANCELLED,
                source = "AlarmScheduler"
            )
        }
    }

    @Test
    fun cancellingAnAlarmThatWasNeverArmedLogsNothing() {
        scheduler.cancel(48L)

        verify(exactly = 0) {
            incidentRepository.recordAsync(
                alarmId = 48L,
                fireId = any(),
                scheduledAt = any(),
                eventAt = any(),
                type = any(),
                status = any(),
                reasonCode = AlarmIncidentEvent.REASON_CANCELLED,
                source = any()
            )
        }
    }

    @Test
    fun pausingAnArmedAlarmLogsTheDisarmOnce() = runTest {
        val alarm = enabledAlarm(id = 46L)
        every { calculator.calculate(any<Alarm>(), any()) } returns
            System.currentTimeMillis() + 15 * 60_000L
        scheduler.schedule(alarm, requestWidgetUpdate = false)

        coEvery { preferencesManager.getCurrentSettings() } returns
            AppSettings(pauseUntilMillis = System.currentTimeMillis() + 86_400_000L)
        scheduler.schedule(alarm, requestWidgetUpdate = false)
        // Every later reschedule pass during the pause finds it already off.
        scheduler.schedule(alarm, requestWidgetUpdate = false)

        assertEquals(
            0,
            shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.size
        )
        verify(exactly = 1) {
            incidentRepository.recordAsync(
                alarmId = 46L,
                fireId = any(),
                scheduledAt = 0L,
                eventAt = any(),
                type = AlarmIncidentEvent.TYPE_SCHEDULE,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = AlarmIncidentEvent.REASON_CANCELLED,
                source = "AlarmScheduler"
            )
        }
    }

    @Test
    fun aPauseOverAnAlarmThatWasNeverArmedLogsNothing() = runTest {
        coEvery { preferencesManager.getCurrentSettings() } returns
            AppSettings(pauseUntilMillis = System.currentTimeMillis() + 86_400_000L)

        scheduler.schedule(enabledAlarm(id = 47L), requestWidgetUpdate = false)

        verify(exactly = 0) {
            incidentRepository.recordAsync(
                alarmId = 47L,
                fireId = any(),
                scheduledAt = any(),
                eventAt = any(),
                type = any(),
                status = any(),
                reasonCode = AlarmIncidentEvent.REASON_CANCELLED,
                source = any()
            )
        }
    }

    @Test
    fun handleAlarmFiredTearsDownStaleExactAlarmForOneShot() = runTest {
        // A smart-wake early fire starts AlarmService directly without consuming
        // the original exact-alarm PendingIntent, so handleAlarmFired() must cancel
        // it for a one-shot — otherwise the alarm fires a second time at the
        // originally-scheduled minute.
        val triggerTime = System.currentTimeMillis() + 20 * 60_000L
        val oneShot = enabledAlarm(id = 55L).copy(repeatDays = emptySet())
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime
        coEvery { repository.getById(55L) } returns oneShot

        scheduler.schedule(oneShot, requestWidgetUpdate = false)
        val alarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))
        assertEquals(1, alarmManager.scheduledAlarms.size)

        scheduler.handleAlarmFired(55L)

        assertEquals(0, alarmManager.scheduledAlarms.size)
        coVerify { repository.setEnabled(55L, false, 0L) }
    }

    @Test
    fun handleAlarmFiredRecurringAfterEarlyFireSkipsTheOriginalMinute() = runTest {
        // Smart-wake fires a recurring alarm early; the user dismisses before
        // the original minute. The reschedule must floor "next" past the
        // original occurrence or the alarm rings a second time at it.
        val originalTrigger = System.currentTimeMillis() + 10 * 60_000L
        val nextDay = originalTrigger + 24 * 3_600_000L
        val recurring = enabledAlarm(id = 66L)
        coEvery { repository.getById(66L) } returns recurring
        val fromSlot = slot<java.time.ZonedDateTime>()
        every { calculator.calculate(any<Alarm>(), capture(fromSlot)) } returns nextDay

        scheduler.handleAlarmFired(66L, firedScheduledAt = originalTrigger)

        assertTrue(fromSlot.captured.toInstant().toEpochMilli() >= originalTrigger + 60_000L)
        coVerify { repository.updateNextTrigger(66L, nextDay) }
    }

    @Test
    fun oneShotHolidaySuppressionStoresZeroTrigger() = runTest {
        // Storing the suppressed future trigger let reboot/app-update
        // reschedules re-arm the alarm without a holiday check, so it fired
        // on the holiday the user asked to skip.
        val triggerTime = System.currentTimeMillis() + 20 * 60_000L
        val oneShot = enabledAlarm(id = 88L).copy(repeatDays = emptySet(), skipOnHolidays = true)
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime
        coEvery { preferencesManager.getCurrentSettings() } returns
            AppSettings(holidayAutoSkipEnabled = true)
        coEvery { holidayRepository.isHoliday(any()) } returns true

        scheduler.schedule(oneShot, requestWidgetUpdate = false)

        coVerify { repository.updateNextTrigger(88L, 0L) }
        assertEquals(
            0,
            shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.size
        )
    }

    @Test
    fun forceRecalculatePreservesLiveSnoozeTrigger() = runTest {
        // Automatic forced reschedules (dashboard location/solar refresh,
        // TIME_SET) must not silently un-snooze: a stored trigger earlier
        // than the natural next occurrence is a live snooze.
        val now = System.currentTimeMillis()
        val snoozeAt = now + 5 * 60_000L
        val naturalNext = now + 20 * 3_600_000L
        val alarm = enabledAlarm(id = 9L).copy(nextTriggerTime = snoozeAt)
        coEvery { repository.getEnabled() } returns listOf(alarm)
        every { calculator.calculate(any<Alarm>(), any()) } returns naturalNext

        scheduler.rescheduleAllInBatches(forceRecalculate = true, batchSize = 5)

        coVerify { repository.updateNextTrigger(9L, snoozeAt) }
    }

    @Test
    fun rescheduleAllKeepsExistingFutureTriggerAndReturnsProcessedCount() = runTest {
        val triggerTime = System.currentTimeMillis() + 30 * 60_000L
        val alarm = enabledAlarm(id = 7L).copy(nextTriggerTime = triggerTime)
        coEvery { repository.getEnabled() } returns listOf(alarm)

        val processed = scheduler.rescheduleAllInBatches(forceRecalculate = false, batchSize = 1)

        assertEquals(1, processed)
        coVerify { repository.updateNextTrigger(7L, triggerTime) }
        verify { WidgetUpdater.requestUpdate(context) }
    }

    @Test
    fun weatherEarlyShiftsTriggerTimeWhenSnowForecast() = runTest {
        val triggerTime = System.currentTimeMillis() + 8 * 3_600_000L
        val triggerDate = java.time.Instant.ofEpochMilli(triggerTime)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        val alarm = enabledAlarm(id = 99L).copy(weatherEarlyMinutes = 15)
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime

        val weatherResponse = com.sysadmindoc.alarmclock.data.remote.WeatherResponse(
            current = null,
            hourly = null,
            daily = com.sysadmindoc.alarmclock.data.remote.DailyWeather(
                time = listOf(triggerDate),
                maxTemp = listOf(28.0),
                minTemp = listOf(20.0),
                weatherCode = listOf(71),
                precipChance = null,
                sunrise = null,
                sunset = null,
                uvIndexMax = null
            ),
            currentUnits = null
        )
        every { weatherRepository.getCachedWeather() } returns weatherResponse

        scheduler.schedule(alarm, requestWidgetUpdate = false)

        coVerify { repository.updateNextTrigger(99L, triggerTime - 15 * 60_000L) }
    }

    @Test
    fun weatherEarlyNoShiftWhenClearForecast() = runTest {
        val triggerTime = System.currentTimeMillis() + 8 * 3_600_000L
        val triggerDate = java.time.Instant.ofEpochMilli(triggerTime)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        val alarm = enabledAlarm(id = 100L).copy(weatherEarlyMinutes = 15)
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime

        val weatherResponse = com.sysadmindoc.alarmclock.data.remote.WeatherResponse(
            current = null,
            hourly = null,
            daily = com.sysadmindoc.alarmclock.data.remote.DailyWeather(
                time = listOf(triggerDate),
                maxTemp = listOf(72.0),
                minTemp = listOf(55.0),
                weatherCode = listOf(0),
                precipChance = null,
                sunrise = null,
                sunset = null,
                uvIndexMax = null
            ),
            currentUnits = null
        )
        every { weatherRepository.getCachedWeather() } returns weatherResponse

        scheduler.schedule(alarm, requestWidgetUpdate = false)

        coVerify { repository.updateNextTrigger(100L, triggerTime) }
    }

    @Test
    fun isSnowOrIceCodeMatchesExpectedWmoCodes() {
        val snowCodes = listOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86)
        val clearCodes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)
        snowCodes.forEach { assert(AlarmScheduler.isSnowOrIceCode(it)) { "Expected $it to be snow/ice" } }
        clearCodes.forEach { assert(!AlarmScheduler.isSnowOrIceCode(it)) { "Expected $it to NOT be snow/ice" } }
    }

    @Test
    fun oneShotOnACalendarTaggedDayStoresZeroTrigger() = runTest {
        val triggerTime = System.currentTimeMillis() + 20 * 60_000L
        val oneShot = enabledAlarm(id = 88L).copy(
            repeatDays = emptySet(),
            skipCalendarTag = "#holiday"
        )
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime
        every { calendarRepository.hasReadPermission() } returns true
        every { calendarRepository.getEventsOverlapping(any(), any()) } returns
            Result.success(listOf(timedEvent("Office closed #Holiday", triggerTime)))

        scheduler.schedule(oneShot, requestWidgetUpdate = false)

        coVerify { repository.updateNextTrigger(88L, 0L) }
        assertEquals(
            0,
            shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.size
        )
    }

    @Test
    fun repeatingAlarmMovesPastACalendarTaggedDay() = runTest {
        val now = System.currentTimeMillis()
        val taggedDay = now + 2 * 3_600_000L
        val nextWeek = taggedDay + 7 * 86_400_000L
        val alarm = enabledAlarm(id = 61L).copy(skipCalendarTag = "#holiday")
        every { calculator.calculate(any<Alarm>(), any()) } answers {
            val from = secondArg<ZonedDateTime>().toInstant().toEpochMilli()
            if (from <= now + 60_000L) taggedDay else nextWeek
        }
        every { calendarRepository.hasReadPermission() } returns true
        every { calendarRepository.getEventsOverlapping(any(), any()) } returns
            Result.success(listOf(timedEvent("#holiday", taggedDay)))

        scheduler.schedule(alarm, requestWidgetUpdate = false)

        coVerify { repository.updateNextTrigger(61L, nextWeek) }
    }

    @Test
    fun calendarTagIsIgnoredWithoutCalendarPermission() = runTest {
        val triggerTime = System.currentTimeMillis() + 20 * 60_000L
        val oneShot = enabledAlarm(id = 89L).copy(
            repeatDays = emptySet(),
            skipCalendarTag = "#holiday"
        )
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime
        every { calendarRepository.hasReadPermission() } returns false

        scheduler.schedule(oneShot, requestWidgetUpdate = false)

        coVerify { repository.updateNextTrigger(89L, triggerTime) }
        verify(exactly = 0) { calendarRepository.getEventsOverlapping(any(), any()) }
    }

    @Test
    fun calendarReadFailureKeepsTheAlarm() = runTest {
        val triggerTime = System.currentTimeMillis() + 20 * 60_000L
        val oneShot = enabledAlarm(id = 90L).copy(
            repeatDays = emptySet(),
            skipCalendarTag = "#holiday"
        )
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime
        every { calendarRepository.hasReadPermission() } returns true
        every { calendarRepository.getEventsOverlapping(any(), any()) } returns
            Result.failure(SecurityException("revoked"))

        scheduler.schedule(oneShot, requestWidgetUpdate = false)

        coVerify { repository.updateNextTrigger(90L, triggerTime) }
    }

    @Test
    fun calendarRefreshReschedulesTaggedAlarmsAndLeavesASnoozeAlone() = runTest {
        val now = System.currentTimeMillis()
        val natural = now + 20 * 3_600_000L
        val snoozeAt = now + 5 * 60_000L
        val snoozed = enabledAlarm(id = 1L).copy(skipCalendarTag = "gym", nextTriggerTime = snoozeAt)
        val tagged = enabledAlarm(id = 2L).copy(skipCalendarTag = "gym")
        val untagged = enabledAlarm(id = 3L)
        coEvery { repository.getEnabled() } returns listOf(snoozed, tagged, untagged)
        every { calculator.calculate(any<Alarm>(), any()) } returns natural
        every { calendarRepository.hasReadPermission() } returns true
        every { calendarRepository.getEventsOverlapping(any(), any()) } returns
            Result.success(emptyList())

        val processed = scheduler.rescheduleCalendarTaggedAlarms()

        assertEquals(2, processed)
        coVerify { repository.updateNextTrigger(2L, natural) }
        coVerify(exactly = 0) { repository.updateNextTrigger(1L, any()) }
        coVerify(exactly = 0) { repository.updateNextTrigger(3L, any()) }
    }

    @Test
    fun calendarRefreshBringsBackADayWhoseTagWasRemoved() = runTest {
        val now = System.currentTimeMillis()
        val taggedDay = now + 2 * 3_600_000L
        val nextWeek = taggedDay + 7 * 86_400_000L
        val alarm = enabledAlarm(id = 62L).copy(skipCalendarTag = "#holiday")
        every { calculator.calculate(any<Alarm>(), any()) } answers {
            val from = secondArg<ZonedDateTime>().toInstant().toEpochMilli()
            if (from < taggedDay) taggedDay else nextWeek
        }
        every { calendarRepository.hasReadPermission() } returns true
        every { calendarRepository.getEventsOverlapping(any(), any()) } returns
            Result.success(listOf(timedEvent("#holiday", taggedDay)))
        scheduler.schedule(alarm, requestWidgetUpdate = false)
        coVerify { repository.updateNextTrigger(62L, nextWeek) }

        every { calendarRepository.getEventsOverlapping(any(), any()) } returns
            Result.success(emptyList())
        coEvery { repository.getEnabled() } returns listOf(alarm.copy(nextTriggerTime = nextWeek))
        scheduler.rescheduleCalendarTaggedAlarms()

        coVerify { repository.updateNextTrigger(62L, taggedDay) }
    }

    @Test
    fun calendarRefreshNeverUndoesASkipNext() = runTest {
        // The stored trigger sits past the natural one because the user
        // skipped it, not because of the calendar, so a calendar sync must
        // leave it there.
        val now = System.currentTimeMillis()
        val skippedDay = now + 2 * 3_600_000L
        val nextWeek = skippedDay + 7 * 86_400_000L
        val alarm = enabledAlarm(id = 63L).copy(
            skipCalendarTag = "#holiday",
            nextTriggerTime = nextWeek
        )
        every { calculator.calculate(any<Alarm>(), any()) } answers {
            val from = secondArg<ZonedDateTime>().toInstant().toEpochMilli()
            if (from < skippedDay) skippedDay else nextWeek
        }
        every { calendarRepository.hasReadPermission() } returns true
        every { calendarRepository.getEventsOverlapping(any(), any()) } returns
            Result.success(emptyList())
        coEvery { repository.getEnabled() } returns listOf(alarm)

        scheduler.rescheduleCalendarTaggedAlarms()

        coVerify { repository.updateNextTrigger(63L, nextWeek) }
        coVerify(exactly = 0) { repository.updateNextTrigger(63L, skippedDay) }
    }

    @Test
    fun theCachedAlarmMovingLaterPutsTheNextEnabledAlarmInTheDirectBootCache() = runTest {
        // A weekly alarm rings and rolls to next week while a daily one rings
        // tomorrow. The pre-unlock fallback has to cover the daily one.
        val now = System.currentTimeMillis()
        val tomorrow = now + 86_400_000L
        val nextWeek = now + 7 * 86_400_000L
        DirectBootAlarmCache.clear(context)
        DirectBootAlarmCache.saveIfEarlier(context, enabledAlarm(id = 70L), now + 3_600_000L)
        coEvery { repository.getEnabled() } returns listOf(
            enabledAlarm(id = 70L).copy(nextTriggerTime = nextWeek),
            enabledAlarm(id = 71L).copy(nextTriggerTime = tomorrow)
        )
        every { calculator.calculate(any<Alarm>(), any()) } returns nextWeek

        scheduler.schedule(enabledAlarm(id = 70L), requestWidgetUpdate = false)

        val cached = DirectBootAlarmCache.read(context)
        assertEquals(71L, cached?.alarmId)
        assertEquals(tomorrow, cached?.triggerTime)
    }

    @Test
    fun cancellingTheCachedAlarmRefillsTheDirectBootCache() = runTest {
        val now = System.currentTimeMillis()
        val later = now + 2 * 3_600_000L
        DirectBootAlarmCache.clear(context)
        DirectBootAlarmCache.saveIfEarlier(context, enabledAlarm(id = 72L), now + 3_600_000L)
        coEvery { repository.getEnabled() } returns listOf(
            // The database can still show the cancelled alarm for a moment.
            enabledAlarm(id = 72L).copy(nextTriggerTime = now + 3_600_000L),
            enabledAlarm(id = 73L).copy(nextTriggerTime = later)
        )

        scheduler.cancel(72L)
        scheduler.pendingDirectBootRefill?.join()

        assertEquals(73L, DirectBootAlarmCache.read(context)?.alarmId)
    }

    @Test
    fun aFailedArmLeavesNothingThatReadsAsArmed() = runTest {
        every { calculator.calculate(any<Alarm>(), any()) } returns
            System.currentTimeMillis() + 15 * 60_000L

        for ((id, denied) in listOf(50L to false, 52L to true)) {
            val failing = schedulerOver(failingAlarmManager(denied))

            failing.schedule(enabledAlarm(id), requestWidgetUpdate = false)

            assertFalse(failing.isArmed(id))
            verify {
                incidentRepository.recordAsync(
                    alarmId = id,
                    fireId = any(),
                    scheduledAt = any(),
                    eventAt = any(),
                    type = AlarmIncidentEvent.TYPE_SCHEDULE,
                    status = AlarmIncidentEvent.STATUS_FAILED,
                    reasonCode = if (denied) {
                        "SET_AND_ALLOW_WHILE_IDLE_FAILED_IllegalStateException"
                    } else {
                        "SET_ALARM_CLOCK_FAILED_IllegalStateException"
                    },
                    source = "AlarmScheduler"
                )
            }
        }
    }

    @Test
    fun failedReArmsKeepTheLiveArmAsItWas() = runTest {
        val first = System.currentTimeMillis() + 15 * 60_000L
        val day = 24 * 3_600_000L

        for ((id, denied, viaScheduleAt) in listOf(
            Triple(51L, false, false),
            Triple(53L, true, false),
            Triple(55L, false, true),
            Triple(57L, true, true)
        )) {
            // The row holds whatever the scheduler last stored, as Room would.
            var stored = 0L
            coEvery { repository.updateNextTrigger(id, any()) } answers { stored = secondArg() }
            coEvery { repository.getById(id) } answers { enabledAlarm(id).copy(nextTriggerTime = stored) }
            val alarmManager = failingAlarmManager(denied, armsThatWork = 1)
            val flaky = schedulerOver(alarmManager)

            for (trigger in listOf(first, first + day, first + 2 * day)) {
                if (viaScheduleAt) {
                    flaky.scheduleAt(enabledAlarm(id), trigger, requestWidgetUpdate = false)
                } else {
                    every { calculator.calculate(any<Alarm>(), any()) } returns trigger
                    flaky.schedule(enabledAlarm(id), requestWidgetUpdate = false)
                }
            }

            // Each failed call got back the PendingIntent AlarmManager still
            // holds from the first and wrote its own time into it. Cancelling
            // it would silence the first arm, and leaving a later time in it
            // would make the first arm ring as that occurrence.
            verify(exactly = 3) { alarmManager.setAlarmClock(any(), any()) }
            assertTrue(flaky.isArmed(id))
            val armed = shadowOf(armedPendingIntent(id)).savedIntent
            assertEquals(first, armed.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, -1L))
            assertEquals(
                AlarmIncidentEvent.fireIdFor(id, first),
                armed.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID)
            )
            assertEquals(first, stored)
        }
    }

    @Test
    fun aFailedReArmKeepsAPastArmOnlyUntilItRings() = runTest {
        val due = System.currentTimeMillis() - 60_000L
        val next = System.currentTimeMillis() + 24 * 3_600_000L
        every { calculator.calculate(any<Alarm>(), any()) } returns next

        for ((id, denied, fired) in listOf(
            Triple(54L, false, true),
            Triple(56L, true, true),
            Triple(58L, false, false),
            Triple(60L, true, false)
        )) {
            var stored = 0L
            coEvery { repository.updateNextTrigger(id, any()) } answers { stored = secondArg() }
            coEvery { repository.getById(id) } answers { enabledAlarm(id).copy(nextTriggerTime = stored) }
            coEvery { incidentRepository.firedCount(id, due) } returns if (fired) 1 else 0
            val flaky = schedulerOver(failingAlarmManager(denied, armsThatWork = 1))
            // An arm whose time has passed. Its PendingIntent stays registered
            // either way: after it rang, or while an inexact delivery runs late.
            flaky.scheduleAt(enabledAlarm(id), due, requestWidgetUpdate = false)
            assertTrue(flaky.isArmed(id))

            flaky.schedule(enabledAlarm(id), requestWidgetUpdate = false)

            if (fired) {
                assertFalse(flaky.isArmed(id))
                assertEquals(next, stored)
            } else {
                assertTrue(flaky.isArmed(id))
                val armed = shadowOf(armedPendingIntent(id)).savedIntent
                assertEquals(due, armed.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, -1L))
                assertEquals(due, stored)
            }
        }
    }

    @Test
    fun aFailedReArmAfterAnEditorSaveKeepsTheArmAsUnknown() = runTest {
        val first = System.currentTimeMillis() + 15 * 60_000L
        for ((id, denied) in listOf(62L to false, 64L to true)) {
            var stored = 0L
            coEvery { repository.updateNextTrigger(id, any()) } answers { stored = secondArg() }
            coEvery { repository.getById(id) } answers { enabledAlarm(id).copy(nextTriggerTime = stored) }
            val flaky = schedulerOver(failingAlarmManager(denied, armsThatWork = 1))
            flaky.scheduleAt(enabledAlarm(id), first, requestWidgetUpdate = false)
            stored = 0L // the editor saved the alarm, which writes no time

            flaky.scheduleAt(enabledAlarm(id), first + 60 * 60_000L, requestWidgetUpdate = false)

            assertTrue(flaky.isArmed(id))
            val armed = shadowOf(armedPendingIntent(id)).savedIntent
            assertEquals(0L, armed.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, -1L))
            assertEquals(0L, stored)
        }
    }

    /**
     * Lets the first [armsThatWork] arms through, then setAlarmClock throws.
     * A policy clamp lands in the generic catch. A denial goes to the inexact
     * fallback, which throws as well.
     */
    private fun failingAlarmManager(denied: Boolean, armsThatWork: Int = 0): AlarmManager {
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        var calls = 0
        every { alarmManager.setAlarmClock(any(), any()) } answers {
            if (++calls > armsThatWork) {
                throw if (denied) SecurityException("exact alarms denied") else IllegalStateException("policy clamp")
            }
        }
        every { alarmManager.setAndAllowWhileIdle(any(), any(), any<PendingIntent>()) } throws
            IllegalStateException("fallback refused")
        return alarmManager
    }

    private fun armedPendingIntent(alarmId: Long): PendingIntent = requireNotNull(
        PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            Intent(context, AlarmReceiver::class.java).setAction("com.sysadmindoc.alarmclock.ALARM_FIRE"),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    )

    private fun schedulerOver(alarmManager: AlarmManager) = AlarmScheduler(
        context = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.ALARM_SERVICE) alarmManager else super.getSystemService(name)
        },
        repository = repository,
        calculator = calculator,
        preferencesManager = preferencesManager,
        holidayRepository = holidayRepository,
        alarmIncidentRepository = incidentRepository,
        weatherRepository = weatherRepository,
        calendarRepository = calendarRepository
    )

    private fun timedEvent(title: String, around: Long) = CalendarEvent(
        id = 1L,
        title = title,
        startTime = around - 30 * 60_000L,
        endTime = around + 30 * 60_000L,
        allDay = false,
        location = "",
        calendarColor = 0
    )

    private fun enabledAlarm(id: Long): Alarm = Alarm(
        id = id,
        hour = 7,
        minute = 30,
        label = "Morning",
        isEnabled = true,
        repeatDays = setOf(DayOfWeek.MONDAY)
    )
}
