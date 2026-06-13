package com.sysadmindoc.alarmclock.domain

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Operation
import androidx.work.WorkManager
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.HolidayRepository
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AlarmSchedulerTest {
    private lateinit var context: Context
    private lateinit var repository: AlarmRepository
    private lateinit var calculator: NextAlarmCalculator
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var holidayRepository: HolidayRepository
    private lateinit var incidentRepository: AlarmIncidentRepository
    private lateinit var weatherRepository: com.sysadmindoc.alarmclock.data.repository.WeatherRepository
    private lateinit var workManager: WorkManager
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

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        every { workManager.cancelUniqueWork(any()) } returns mockk<Operation>(relaxed = true)

        mockkObject(WidgetUpdater)
        every { WidgetUpdater.requestUpdate(any()) } just Runs

        coEvery { preferencesManager.getCurrentSettings() } returns AppSettings()
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
            weatherRepository = weatherRepository
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
    fun cancelRemovesFollowUpWorkersAndRequestsWidgetRefresh() {
        scheduler.cancel(42L)

        verify { workManager.cancelUniqueWork("hue_sunrise_42") }
        verify { workManager.cancelUniqueWork("guardian_42") }
        verify { workManager.cancelUniqueWork("wake_confirm_42") }
        verify { WidgetUpdater.requestUpdate(context) }
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

    private fun enabledAlarm(id: Long): Alarm = Alarm(
        id = id,
        hour = 7,
        minute = 30,
        label = "Morning",
        isEnabled = true,
        repeatDays = setOf(DayOfWeek.MONDAY)
    )
}
