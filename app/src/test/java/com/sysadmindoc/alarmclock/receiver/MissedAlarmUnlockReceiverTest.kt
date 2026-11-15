package com.sysadmindoc.alarmclock.receiver

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmRuntimeState
import com.sysadmindoc.alarmclock.service.AlarmService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Runs the real receiver against the app's own database and settings, the
 * way an unlock reaches it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MissedAlarmUnlockReceiverTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `a replay marks the time of the miss as an ended ring`() {
        val entryPoint = EntryPointAccessors.fromApplication(app, AlarmClockApp.AppEntryPoint::class.java)
        val alarmId = runBlocking { entryPoint.alarmRepository().save(Alarm(isEnabled = true)) }
        val missedAt = System.currentTimeMillis() - 60_000L
        app.getSharedPreferences("missed_alarm_state", Context.MODE_PRIVATE).edit()
            .putLong("last_missed_at", missedAt)
            .putLong("last_missed_id", alarmId)
            .commit()

        app.sendBroadcast(Intent(Intent.ACTION_USER_PRESENT))
        val replay = awaitReplayStart()

        assertEquals(alarmId, replay.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L))
        assertEquals(missedAt, replay.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0L))
        assertEquals(missedAt, AlarmRuntimeState.lastEndedOccurrence(app, alarmId))
    }

    /** The receiver works on a background dispatcher, so this waits for its start. */
    private fun awaitReplayStart(): Intent {
        val deadline = System.currentTimeMillis() + 10_000L
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            while (true) {
                val started = shadowOf(app).nextStartedService ?: break
                if (started.action == AlarmService.ACTION_START_ALARM) return started
            }
            Thread.sleep(20)
        }
        throw AssertionError("the replay never started")
    }
}
