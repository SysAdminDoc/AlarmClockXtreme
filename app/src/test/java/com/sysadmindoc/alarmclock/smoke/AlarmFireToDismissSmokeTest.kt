package com.sysadmindoc.alarmclock.smoke

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.local.AlarmDatabase
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmFireDismissContract
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.ui.alarmfiring.AlarmFiringActivity
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AlarmFireToDismissSmokeTest {

    private lateinit var context: Context
    private lateinit var database: AlarmDatabase
    private lateinit var eventRepository: AlarmEventRepository
    private lateinit var incidentRepository: AlarmIncidentRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventRepository = AlarmEventRepository(database.alarmEventDao())
        incidentRepository = AlarmIncidentRepository(database.alarmIncidentEventDao(), context)
        AlarmService.activeAlarm.set(null)
    }

    @After
    fun tearDown() {
        AlarmService.activeAlarm.set(null)
        database.close()
    }

    @Test
    fun localFireToDismissContractRecordsUiLaunchDismissAndCleanup() = runTest {
        val scheduledAt = System.currentTimeMillis() - 4_000L
        val firedAt = scheduledAt + 1_500L
        val dismissedAt = scheduledAt + 4_000L
        val alarm = Alarm(
            id = 42L,
            hour = 6,
            minute = 30,
            label = "Smoke alarm",
            challengeType = "NONE",
            vibrationEnabled = false,
            gradualVolumeSeconds = 0,
            wakeConfirmEnabled = false,
            ttsEnabled = false
        )
        val fireId = AlarmFireDismissContract.fireId(alarm.id, scheduledAt)

        val startIntent = AlarmFireDismissContract.startServiceIntent(context, alarm.id, scheduledAt, fireId)
        assertEquals(AlarmService.ACTION_START_ALARM, startIntent.action)
        assertEquals(AlarmService::class.java.name, startIntent.component?.className)
        assertAlarmExtras(startIntent, alarm.id, scheduledAt, fireId)

        AlarmService.activeAlarm.set(AlarmService.Companion.ActiveAlarmSnapshot(alarm.id, scheduledAt, fireId))
        incidentRepository.record(
            alarmId = alarm.id,
            fireId = fireId,
            scheduledAt = scheduledAt,
            eventAt = firedAt,
            type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
            status = AlarmIncidentEvent.STATUS_RECEIVED,
            reasonCode = "START_COMMAND_RECEIVED",
            source = "AlarmService"
        )

        val firingIntent = AlarmFireDismissContract.firingActivityIntent(context, alarm.id, scheduledAt, fireId)
        assertEquals(AlarmFiringActivity::class.java.name, firingIntent.component?.className)
        assertAlarmExtras(firingIntent, alarm.id, scheduledAt, fireId)
        assertTrue(firingIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(firingIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(firingIntent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS != 0)
        incidentRepository.record(
            alarmId = alarm.id,
            fireId = fireId,
            scheduledAt = scheduledAt,
            eventAt = firedAt + 100L,
            type = AlarmIncidentEvent.TYPE_ACTIVITY_LAUNCH,
            status = AlarmIncidentEvent.STATUS_SUCCEEDED,
            reasonCode = "FIRING_ACTIVITY_LAUNCHED",
            source = "AlarmService"
        )

        val dismissIntent = AlarmFireDismissContract.dismissServiceIntent(
            context = context,
            alarmId = alarm.id,
            scheduledAt = scheduledAt,
            fireId = fireId,
            challengeRetryCount = 2,
            challengeSolveTimeMs = 1_200L
        )
        assertEquals(AlarmService.ACTION_DISMISS, dismissIntent.action)
        assertEquals(AlarmService::class.java.name, dismissIntent.component?.className)
        assertAlarmExtras(dismissIntent, alarm.id, scheduledAt, fireId)
        assertEquals(2, dismissIntent.getIntExtra(AlarmService.EXTRA_CHALLENGE_RETRY_COUNT, -1))
        assertEquals(1_200L, dismissIntent.getLongExtra(AlarmService.EXTRA_CHALLENGE_SOLVE_TIME_MS, -1L))
        incidentRepository.record(
            alarmId = alarm.id,
            fireId = fireId,
            scheduledAt = scheduledAt,
            eventAt = dismissedAt - 100L,
            type = AlarmIncidentEvent.TYPE_USER_ACTION,
            status = AlarmIncidentEvent.STATUS_REQUESTED,
            reasonCode = "UI_DISMISS_REQUESTED",
            source = "AlarmFiringActivity"
        )

        eventRepository.record(
            AlarmFireDismissContract.alarmEvent(
                alarm = alarm,
                scheduledAt = scheduledAt,
                firedAt = firedAt,
                action = AlarmEvent.ACTION_DISMISSED,
                actionAt = dismissedAt,
                challengeRetryCount = 2,
                challengeSolveTimeMs = 1_200L,
                snoozeCount = 1
            )
        )
        incidentRepository.record(
            alarmId = alarm.id,
            fireId = fireId,
            scheduledAt = scheduledAt,
            eventAt = dismissedAt,
            type = AlarmIncidentEvent.TYPE_USER_ACTION,
            status = AlarmIncidentEvent.STATUS_SUCCEEDED,
            reasonCode = "DISMISSED",
            source = "AlarmService"
        )
        AlarmService.activeAlarm.set(null)

        val events = eventRepository.getSince(scheduledAt - 1L)
        assertEquals(1, events.size)
        val event = events.single()
        assertEquals(AlarmEvent.ACTION_DISMISSED, event.action)
        assertEquals(alarm.id, event.alarmId)
        assertEquals("Smoke alarm", event.alarmLabel)
        assertEquals(scheduledAt, event.scheduledTime)
        assertEquals(firedAt, event.firedAt)
        assertEquals(dismissedAt, event.actionAt)
        assertEquals(2, event.challengeRetryCount)
        assertEquals(1_200L, event.challengeSolveTimeMs)
        assertEquals(1, event.snoozeCount)
        assertTrue(event.responseTimeMs > 0L)

        val incidents = incidentRepository.getRecent(10)
        val reasonCodes = incidents.map { it.reasonCode }.toSet()
        assertTrue(reasonCodes.contains("START_COMMAND_RECEIVED"))
        assertTrue(reasonCodes.contains("FIRING_ACTIVITY_LAUNCHED"))
        assertTrue(reasonCodes.contains("UI_DISMISS_REQUESTED"))
        assertTrue(reasonCodes.contains("DISMISSED"))
        assertNotNull(incidents.firstOrNull { it.type == AlarmIncidentEvent.TYPE_ACTIVITY_LAUNCH })
        assertNull(AlarmService.activeAlarm.get())
    }

    private fun assertAlarmExtras(intent: Intent, alarmId: Long, scheduledAt: Long, fireId: String) {
        assertEquals(alarmId, intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L))
        assertEquals(scheduledAt, intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, -1L))
        assertEquals(fireId, intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID))
    }
}
