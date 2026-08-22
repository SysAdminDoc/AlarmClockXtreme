package com.sysadmindoc.alarmclock.platform

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.ui.timer.TimerStore
import com.sysadmindoc.alarmclock.ui.timer.PersistedTimerRecord
import com.sysadmindoc.alarmclock.ui.timer.TimerStartResult
import com.sysadmindoc.alarmclock.ui.timer.TimerState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AlarmClockIntentHandlerTest {
    private lateinit var context: Context
    private lateinit var repository: AlarmRepository
    private lateinit var scheduler: AlarmScheduler
    private lateinit var calculator: NextAlarmCalculator
    private lateinit var timerStore: TimerStore
    private lateinit var guard: AlarmClockIntentDeliveryGuard
    private lateinit var handler: AlarmClockIntentHandler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as Application).clearStartedServices()
        repository = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        calculator = mockk(relaxed = true)
        timerStore = mockk(relaxed = true)
        guard = mockk(relaxed = true)
        every { guard.claim(any(), any()) } returns true
        handler = AlarmClockIntentHandler(
            context,
            repository,
            scheduler,
            calculator,
            timerStore,
            guard
        )
    }

    @After
    fun tearDown() {
        AlarmService.activeAlarm.set(null)
    }

    @Test
    fun `dismiss-all still silences the alarm that is ringing, then asks about the rest`() = runTest {
        // The confirmation gate only exempted a request resolving to exactly
        // one alarm, so "dismiss all my alarms" with a second alarm in the list
        // opened the alarm list and left the phone ringing.
        val ringing = Alarm(id = 7L, hour = 6, minute = 0, isEnabled = true)
        val later = Alarm(id = 8L, hour = 7, minute = 0, isEnabled = true)
        AlarmService.activeAlarm.set(
            AlarmService.Companion.ActiveAlarmSnapshot(7L, 1_000L, "fire-1")
        )
        coEvery { repository.getEnabled() } returns listOf(ringing, later)
        coEvery { repository.getById(7L) } returns ringing

        val result = handler.handle(
            Intent(AlarmClock.ACTION_DISMISS_ALARM)
                .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_ALL)
        )

        // The ringing one is stopped without asking...
        val started = shadowOf(context as Application).nextStartedService
        assertNotNull("the ringing alarm must be dismissed immediately", started)
        // ...and the rest of the batch goes through the list.
        assertEquals("acx://navigate/alarm_list", (result as AlarmClockHandleResult.Handled).route)
        coVerify(exactly = 0) { repository.setEnabled(8L, any(), any()) }
    }

    @Test
    fun `dismissing a named alarm that is not ringing asks the user first`() = runTest {
        // SET_ALARM is a normal permission, so any installed app can send this.
        // Turning off an alarm that is not currently ringing is not something a
        // caller gets to do unseen.
        val sleeping = Alarm(id = 7L, hour = 6, minute = 0, isEnabled = true)
        coEvery { repository.getEnabled() } returns listOf(sleeping)
        coEvery { repository.getById(7L) } returns sleeping

        val result = handler.handle(
            Intent(AlarmClock.ACTION_DISMISS_ALARM)
                .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "")
                .setData(android.net.Uri.parse("acx://alarm/7"))
        )

        assertEquals("acx://navigate/alarm_list", (result as AlarmClockHandleResult.Handled).route)
        coVerify(exactly = 0) { repository.setEnabled(any(), any(), any()) }
    }

    @Test
    fun `dismissing the alarm that is ringing right now still happens immediately`() = runTest {
        // This is the case the platform contract exists for.
        val ringing = Alarm(id = 7L, hour = 6, minute = 0, isEnabled = true)
        AlarmService.activeAlarm.set(
            AlarmService.Companion.ActiveAlarmSnapshot(7L, 1_000L, "fire-1")
        )
        coEvery { repository.getEnabled() } returns listOf(ringing)
        coEvery { repository.getById(7L) } returns ringing

        val result = handler.handle(
            Intent(AlarmClock.ACTION_DISMISS_ALARM)
                .setData(android.net.Uri.parse("acx://alarm/7"))
        )

        assertNull((result as AlarmClockHandleResult.Handled).route)
    }

    @Test
    fun `a skip-UI alarm reports that it was created without showing anything`() = runTest {
        coEvery { repository.getEnabled() } returns emptyList()
        coEvery { repository.save(any()) } returns 11L
        every { calculator.calculate(any(), any()) } returns 123_456L

        val result = handler.handle(
            Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, 7)
                .putExtra(AlarmClock.EXTRA_MINUTES, 30)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        )

        assertTrue((result as AlarmClockHandleResult.Handled).createdSilently)
    }

    @Test
    fun `matching an existing alarm is not announced, because nothing was added`() = runTest {
        val existing = Alarm(id = 3L, hour = 7, minute = 30, isEnabled = true)
        coEvery { repository.getEnabled() } returns listOf(existing)

        val result = handler.handle(
            Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, 7)
                .putExtra(AlarmClock.EXTRA_MINUTES, 30)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        )

        assertFalse((result as AlarmClockHandleResult.Handled).createdSilently)
    }

    @Test
    fun duplicateSetAlarmDeliveryCreatesAndSchedulesOnlyOnce() = runTest {
        coEvery { repository.getEnabled() } returns emptyList()
        coEvery { repository.save(any()) } returns 42L
        every { calculator.calculate(any(), any()) } returns 123_456L
        every { guard.claim(any(), any()) } returnsMany listOf(true, false)
        val first = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, 7)
            .putExtra(AlarmClock.EXTRA_MINUTES, 30)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        val duplicate = Intent(first)

        val firstResult = handler.handle(first)
        val duplicateResult = handler.handle(duplicate)

        assertTrue(firstResult is AlarmClockHandleResult.Handled)
        assertNull((firstResult as AlarmClockHandleResult.Handled).route)
        assertEquals(AlarmClockHandleResult.Duplicate, duplicateResult)
        coVerify(exactly = 1) { repository.save(match { it.hour == 7 && it.minute == 30 }) }
        coVerify(exactly = 1) { scheduler.schedule(match { it.id == 42L }) }
    }

    @Test
    fun malformedRequestSchedulesNothingAndDoesNotConsumeDeliveryClaim() = runTest {
        val result = handler.handle(
            Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, 99)
        )

        assertEquals(AlarmClockHandleResult.Invalid, result)
        verify(exactly = 0) { guard.claim(any(), any()) }
        coVerify(exactly = 0) { repository.save(any()) }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun setTimerPersistsRequestAndHonorsVisibleUiDefault() = runTest {
        val record = PersistedTimerRecord(
            id = 5,
            label = "Tea",
            totalSeconds = 90,
            remainingMillis = 90_000,
            state = TimerState.RUNNING,
            endElapsedRealtime = 100_000
        )
        every { timerStore.startOrReuse(90L, "Tea", any()) } returns
            TimerStartResult(record, created = false)

        val result = handler.handle(
            Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, 90)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Tea")
        )

        assertEquals(
            AlarmClockHandleResult.Handled(AlarmClockIntentHandler.ROUTE_TIMER),
            result
        )
        verify(exactly = 1) { timerStore.startOrReuse(90L, "Tea", any()) }
    }

    @Test
    fun snoozeTargetsCurrentlyRingingAlarmWithRequestedDuration() = runTest {
        AlarmService.activeAlarm.set(AlarmService.Companion.ActiveAlarmSnapshot(77L, 1_000L, "fire-77"))

        val result = handler.handle(
            Intent(AlarmClock.ACTION_SNOOZE_ALARM)
                .putExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, 15)
        )
        val started = shadowOf(context as Application).nextStartedService

        assertTrue(result is AlarmClockHandleResult.Handled)
        assertEquals(AlarmService.ACTION_SNOOZE, started.action)
        assertEquals(77L, started.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L))
        assertEquals(15, started.getIntExtra(AlarmService.EXTRA_CUSTOM_SNOOZE_MINUTES, -1))
    }

    @Test
    fun dismissTargetsCurrentlyRingingAlarmBeforeOtherEnabledAlarms() = runTest {
        val active = Alarm(id = 77L, hour = 7, minute = 0, nextTriggerTime = 1_000L)
        AlarmService.activeAlarm.set(AlarmService.Companion.ActiveAlarmSnapshot(77L, 1_000L, "fire-77"))
        coEvery { repository.getEnabled() } returns listOf(active, Alarm(id = 88L))
        coEvery { repository.getById(77L) } returns active

        val result = handler.handle(Intent(AlarmClock.ACTION_DISMISS_ALARM))
        val started = shadowOf(context as Application).nextStartedService

        assertTrue(result is AlarmClockHandleResult.Handled)
        assertNull((result as AlarmClockHandleResult.Handled).route)
        assertEquals(AlarmService.ACTION_DISMISS, started.action)
        assertEquals(77L, started.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L))
        verify(exactly = 0) { scheduler.cancel(88L) }
    }

    @Test
    fun manifestProtectsOnlyTheAlarmClockContractActivity() {
        val packageManager = context.packageManager
        val proxyInfo = packageManager.getActivityInfo(
            ComponentName(context, AlarmClockIntentActivity::class.java),
            0
        )
        val launcherInfo = packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)

        assertEquals("com.android.alarm.permission.SET_ALARM", proxyInfo.permission)
        assertNull(launcherInfo.permission)
    }
}
