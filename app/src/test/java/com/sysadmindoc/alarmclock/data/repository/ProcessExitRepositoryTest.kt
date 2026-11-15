package com.sysadmindoc.alarmclock.data.repository

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivityManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class ProcessExitRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val incidents = mockk<AlarmIncidentRepository>()
    private val alarms = mockk<AlarmRepository>()

    private val now = System.currentTimeMillis()
    private val exitAt = now - 2 * 3_600_000L
    private val alarmAt = now - 3_600_000L

    private val scheduledBeforeExit = AlarmIncidentEvent(
        fireId = AlarmIncidentEvent.fireIdFor(5L, alarmAt),
        alarmId = 5L,
        scheduledAt = alarmAt,
        eventAt = exitAt - 60_000L,
        elapsedMs = exitAt - 60_000L - alarmAt,
        type = AlarmIncidentEvent.TYPE_SCHEDULE,
        status = AlarmIncidentEvent.STATUS_SUCCEEDED,
        reasonCode = AlarmIncidentEvent.REASON_NONE,
        source = "AlarmScheduler",
        sdkInt = 30,
        standbyBucket = "ACTIVE_10",
        exactAlarmAllowed = "TRUE",
        notificationPermissionGranted = "TRUE",
        fullScreenIntentAllowed = AlarmIncidentEvent.VALUE_NOT_APPLICABLE,
        batteryOptimizationsIgnored = "TRUE",
        algorithmVersion = AlarmIncidentEvent.VALUE_NONE
    )

    @Test
    fun `a force stop reads back with its time and the alarm it cut off`() = runTest {
        shadowOf(context.getSystemService(ActivityManager::class.java)).addApplicationExitInfo(
            ShadowActivityManager.ApplicationExitInfoBuilder.newBuilder()
                .setProcessName(context.packageName)
                .setReason(ApplicationExitInfo.REASON_USER_REQUESTED)
                .setTimestamp(exitAt)
                .setDescription("stop ${context.packageName} due to from pid 1234")
                .build()
        )
        coEvery { incidents.getRecent(any()) } returns listOf(scheduledBeforeExit)
        coEvery { alarms.getEnabled() } returns listOf(Alarm(id = 5L, hour = 7, minute = 0))

        val exits = ProcessExitRepository(context, incidents, alarms).recentExits()

        assertEquals(1, exits.size)
        val exit = exits.single()
        assertEquals(ApplicationExitInfo.REASON_USER_REQUESTED, exit.reasonCode)
        assertEquals(exitAt, exit.timestamp)
        assertEquals(ProcessExitCause.FORCE_STOPPED, exit.cause)
        assertEquals(ExitAlarmOutcome.NO_RING_RECORDED, exit.alarmOutcome)
        assertEquals(alarmAt, exit.armedAlarmAt)
    }

    @Test
    fun `an alarm that is gone with no cancel on record is not reported as cut off`() = runTest {
        shadowOf(context.getSystemService(ActivityManager::class.java)).addApplicationExitInfo(
            ShadowActivityManager.ApplicationExitInfoBuilder.newBuilder()
                .setProcessName(context.packageName)
                .setReason(ApplicationExitInfo.REASON_USER_REQUESTED)
                .setTimestamp(exitAt)
                .build()
        )
        coEvery { incidents.getRecent(any()) } returns listOf(scheduledBeforeExit)
        coEvery { alarms.getEnabled() } returns emptyList()

        val exit = ProcessExitRepository(context, incidents, alarms).recentExits().single()

        assertEquals(ExitAlarmOutcome.NO_ALARM_SET, exit.alarmOutcome)
        assertEquals(0L, exit.armedAlarmAt)
    }

    @Test
    fun `no recorded exits skips the incident read`() = runTest {
        val exits = ProcessExitRepository(context, incidents, alarms).recentExits()
        assertTrue(exits.isEmpty())
        coVerify(exactly = 0) { incidents.getRecent(any()) }
    }

    @Test
    @Config(sdk = [29], application = Application::class)
    fun `Android 10 keeps no exit record to read`() = runTest {
        assertTrue(ProcessExitRepository(context, incidents, alarms).recentExits().isEmpty())
    }
}
