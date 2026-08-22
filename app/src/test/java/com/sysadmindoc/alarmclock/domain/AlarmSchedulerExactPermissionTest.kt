package com.sysadmindoc.alarmclock.domain

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Operation
import androidx.work.WorkManager
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.HolidayRepository
import com.sysadmindoc.alarmclock.data.repository.WeatherRepository
import com.sysadmindoc.alarmclock.widget.WidgetUpdater
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.time.DayOfWeek
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/**
 * What the scheduler does when the user has taken exact-alarm access away.
 *
 * Runs on API 31 and 33 because the two are governed by different manifest
 * permissions (`SCHEDULE_EXACT_ALARM` up to 32, `USE_EXACT_ALARM` from 33) and
 * the code has to behave the same either way, and on API 30 separately because
 * the permission does not exist there and the version guard is the only thing
 * stopping a call that would throw.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 33], application = Application::class)
class AlarmSchedulerExactPermissionTest {
    private lateinit var context: Context
    private lateinit var repository: AlarmRepository
    private lateinit var calculator: NextAlarmCalculator
    private lateinit var incidentRepository: AlarmIncidentRepository
    private lateinit var scheduler: AlarmScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)
        calculator = mockk()
        incidentRepository = mockk(relaxed = true)
        val preferencesManager: PreferencesManager = mockk()
        val holidayRepository: HolidayRepository = mockk()
        val weatherRepository: WeatherRepository = mockk(relaxed = true)
        val workManager: WorkManager = mockk(relaxed = true)

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        every { workManager.cancelUniqueWork(any()) } returns mockk<Operation>(relaxed = true)

        mockkObject(WidgetUpdater)
        every { WidgetUpdater.requestUpdate(any()) } just Runs

        coEvery { preferencesManager.getCurrentSettings() } returns AppSettings()
        every { preferencesManager.getCachedSettings() } returns AppSettings()
        coEvery { holidayRepository.isHoliday(any()) } returns false
        every { weatherRepository.getCachedWeather() } returns null

        scheduler = AlarmScheduler(
            context = context,
            repository = repository,
            calculator = calculator,
            preferencesManager = preferencesManager,
            holidayRepository = holidayRepository,
            alarmIncidentRepository = incidentRepository,
            weatherRepository = weatherRepository
        )
    }

    @After
    fun tearDown() {
        // The shadow's flag is static, so leaving it false would deny the
        // permission to every test that runs after this class.
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        unmockkAll()
    }

    @Test
    fun `without the permission nothing is registered and the stored trigger is cleared`() =
        runTest {
            ShadowAlarmManager.setCanScheduleExactAlarms(false)
            every { calculator.calculate(any<Alarm>(), any()) } returns
                System.currentTimeMillis() + 15 * 60_000L

            scheduler.schedule(enabledAlarm(id = 71L), requestWidgetUpdate = false)

            assertTrue(scheduledAlarms().isEmpty())
            // Zero, not the calculated time: the next-alarm UI has to say there
            // is no alarm rather than promise one that will not fire.
            coVerify { repository.updateNextTrigger(71L, 0) }
        }

    @Test
    fun `with the permission the alarm is registered as before`() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val triggerTime = System.currentTimeMillis() + 15 * 60_000L
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime

        scheduler.schedule(enabledAlarm(id = 72L), requestWidgetUpdate = false)

        assertNotNull(scheduledAlarms().single().alarmClockInfo)
        coVerify { repository.updateNextTrigger(72L, triggerTime) }
    }

    @Test
    fun `a snooze survives access being revoked mid-ring`() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        val armed = scheduler.scheduleSnooze(enabledAlarm(id = 73L), customMinutes = 9)

        // The service has already told the user they snoozed. Dropping it
        // silently is the one outcome that cannot be allowed, so the snooze
        // degrades to an inexact wakeup instead of disappearing.
        assertTrue("the snooze reported failure", armed)
        val scheduled = scheduledAlarms().single()
        assertNull("an inexact wakeup must not claim to be an alarm clock", scheduled.alarmClockInfo)
        assertTrue(scheduled.isAllowWhileIdle)
        coVerify { repository.updateNextTrigger(73L, more(0L)) }
        verify {
            incidentRepository.recordAsync(
                alarmId = 73L,
                fireId = any(),
                scheduledAt = any(),
                eventAt = any(),
                type = AlarmIncidentEvent.TYPE_SCHEDULE,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "SNOOZE_INEXACT_NO_EXACT_PERMISSION",
                source = "AlarmScheduler"
            )
        }
    }

    @Test
    fun `a snooze at a chosen time survives it too`() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val target = System.currentTimeMillis() + 40 * 60_000L

        val armed = scheduler.scheduleSnoozeAt(enabledAlarm(id = 74L), target)

        assertTrue("the snooze reported failure", armed)
        assertNull(scheduledAlarms().single().alarmClockInfo)
        verify {
            incidentRepository.recordAsync(
                alarmId = 74L,
                fireId = any(),
                scheduledAt = target,
                eventAt = any(),
                type = AlarmIncidentEvent.TYPE_SCHEDULE,
                status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                reasonCode = "SNOOZE_AT_INEXACT_NO_EXACT_PERMISSION",
                source = "AlarmScheduler"
            )
        }
    }

    /**
     * Before API 31 there is no permission to lose. If the version guard ever
     * goes away the call throws NoSuchMethodError on a real API 30 device, and
     * every alarm on it stops being scheduled.
     */
    @Test
    @Config(sdk = [30])
    fun `before API 31 the alarm is scheduled whatever the shadow says`() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val triggerTime = System.currentTimeMillis() + 15 * 60_000L
        every { calculator.calculate(any<Alarm>(), any()) } returns triggerTime

        scheduler.schedule(enabledAlarm(id = 75L), requestWidgetUpdate = false)

        assertEquals(1, scheduledAlarms().size)
        coVerify { repository.updateNextTrigger(75L, triggerTime) }
    }

    private fun scheduledAlarms() =
        shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms

    private fun enabledAlarm(id: Long): Alarm = Alarm(
        id = id,
        hour = 7,
        minute = 30,
        label = "Morning",
        isEnabled = true,
        repeatDays = setOf(DayOfWeek.MONDAY)
    )
}
