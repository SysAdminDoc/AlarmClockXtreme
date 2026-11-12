package com.sysadmindoc.alarmclock.service

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.receiver.AlarmTestRunReceiver
import com.sysadmindoc.alarmclock.worker.WakeConfirmWorker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The parts of "Test now" that live outside the service: the flag on every
 * intent, the refusal to ring over a real alarm, and the separate snooze
 * counter, snooze timer and wake check that keep a test from touching the
 * real alarm's state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class AlarmTestRunTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AlarmService.activeAlarm.set(null)
    }

    @After
    fun tearDown() {
        AlarmService.activeAlarm.set(null)
        AlarmTestRun.clearSnoozeCount(context)
        AlarmRuntimeState.clear(context, ALARM_ID)
    }

    @Test
    fun `only a test run carries the flag`() {
        val real = listOf(
            AlarmFireDismissContract.startServiceIntent(context, ALARM_ID, SCHEDULED_AT),
            AlarmFireDismissContract.snoozeServiceIntent(context, ALARM_ID, SCHEDULED_AT),
            AlarmFireDismissContract.dismissServiceIntent(context, ALARM_ID, SCHEDULED_AT),
            AlarmFireDismissContract.firingActivityIntent(context, ALARM_ID, SCHEDULED_AT)
        )
        val test = listOf(
            AlarmFireDismissContract.startServiceIntent(context, ALARM_ID, SCHEDULED_AT, testRun = true),
            AlarmFireDismissContract.snoozeServiceIntent(context, ALARM_ID, SCHEDULED_AT, testRun = true),
            AlarmFireDismissContract.dismissServiceIntent(context, ALARM_ID, SCHEDULED_AT, testRun = true),
            AlarmFireDismissContract.firingActivityIntent(context, ALARM_ID, SCHEDULED_AT, testRun = true)
        )

        real.forEach { intent ->
            assertFalse(intent.action.toString(), intent.hasExtra(AlarmTestRun.EXTRA_TEST_RUN))
            assertFalse(AlarmTestRun.isTestRun(intent))
        }
        test.forEach { intent ->
            assertTrue(intent.action.toString(), AlarmTestRun.isTestRun(intent))
        }
        assertFalse(AlarmTestRun.isTestRun(null))
    }

    @Test
    fun `a test starts the service flagged as a test`() {
        val result = AlarmTestRun.start(context, ALARM_ID, now = SCHEDULED_AT)

        assertEquals(AlarmTestRun.StartResult.STARTED, result)
        val started = shadowOf(context).nextStartedService
        assertEquals(AlarmService.ACTION_START_ALARM, started.action)
        assertEquals(ALARM_ID, started.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L))
        assertTrue(AlarmTestRun.isTestRun(started))
    }

    @Test
    fun `a test never rings over a real alarm`() {
        AlarmService.activeAlarm.set(
            AlarmService.Companion.ActiveAlarmSnapshot(alarmId = 3L, scheduledAt = 1L, fireId = "real")
        )

        val result = AlarmTestRun.start(context, ALARM_ID, now = SCHEDULED_AT)

        assertEquals(AlarmTestRun.StartResult.ALARM_RINGING, result)
        assertNull(shadowOf(context).nextStartedService)
    }

    @Test
    fun `a new test replaces a test that is already ringing`() {
        AlarmService.activeAlarm.set(
            AlarmService.Companion.ActiveAlarmSnapshot(alarmId = 3L, scheduledAt = 1L, fireId = "test", isTestRun = true)
        )

        assertEquals(AlarmTestRun.StartResult.STARTED, AlarmTestRun.start(context, ALARM_ID, now = SCHEDULED_AT))
    }

    @Test
    fun `test snoozes are counted apart from the real alarm`() {
        AlarmRuntimeState.setSnoozeCount(context, ALARM_ID, 1)
        AlarmTestRun.setSnoozeCount(context, ALARM_ID, 3)

        assertEquals(1, AlarmRuntimeState.snoozeCount(context, ALARM_ID))
        assertEquals(3, AlarmTestRun.snoozeCount(context, ALARM_ID))

        AlarmTestRun.end(context)

        assertEquals(0, AlarmTestRun.snoozeCount(context, ALARM_ID))
        assertEquals("ending a test touched the real snooze count", 1, AlarmRuntimeState.snoozeCount(context, ALARM_ID))
    }

    @Test
    fun `a test snooze rings through its own timer and ending the test drops it`() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + 5 * 60_000L

        assertTrue(AlarmTestRun.scheduleSnooze(context, ALARM_ID, SCHEDULED_AT, "fire", at))

        val scheduled = shadowOf(alarmManager).scheduledAlarms.single()
        assertEquals(at, scheduled.triggerAtMs)
        val target = shadowOf(scheduled.operation).savedIntent
        assertEquals(AlarmTestRunReceiver::class.java.name, target.component?.className)
        assertEquals(AlarmTestRun.ACTION_RING, target.action)
        assertTrue(AlarmTestRun.isTestRun(target))

        AlarmTestRun.end(context)

        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `a test wake check never shares the real one's name or answer`() {
        val alarm = Alarm(id = ALARM_ID, hour = 6, minute = 0, wakeConfirmEnabled = true, wakeConfirmDelayMinutes = 3)

        val real = AlarmPostDismissController.wakeConfirmationPlan(alarm, "fire", SCHEDULED_AT, refireCount = 0)
        val test = AlarmPostDismissController.wakeConfirmationPlan(
            alarm, "fire", SCHEDULED_AT, refireCount = 0, testRun = true
        )

        assertEquals("wake_confirm_$ALARM_ID", real.uniqueWorkName)
        assertFalse(real.inputData.getBoolean(WakeConfirmWorker.KEY_TEST_RUN, true))
        assertNotEquals(real.uniqueWorkName, test.uniqueWorkName)
        assertEquals(AlarmTestRun.WAKE_CONFIRM_TAG, test.tag)
        assertTrue(test.inputData.getBoolean(WakeConfirmWorker.KEY_TEST_RUN, false))
        assertEquals(real.delayMinutes, test.delayMinutes)
        assertNotEquals(
            AlarmTestRun.wakeConfirmKey(ALARM_ID, testRun = false),
            AlarmTestRun.wakeConfirmKey(ALARM_ID, testRun = true)
        )
        assertEquals("confirmed_$ALARM_ID", AlarmTestRun.wakeConfirmKey(ALARM_ID, testRun = false))
    }

    private companion object {
        const val ALARM_ID = 42L
        const val SCHEDULED_AT = 1_700_000_000_000L
    }
}
