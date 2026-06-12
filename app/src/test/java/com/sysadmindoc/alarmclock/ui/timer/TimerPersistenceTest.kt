package com.sysadmindoc.alarmclock.ui.timer

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TimerPersistenceTest {
    private lateinit var context: Context
    private lateinit var store: TimerStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("timer_state", Context.MODE_PRIVATE).edit().clear().commit()
        store = TimerStore(context)
    }

    @Test
    fun runningTimerRestoresRemainingTimeFromElapsedEndTime() {
        val now = SystemClock.elapsedRealtime()
        store.upsert(
            PersistedTimerRecord(
                id = 4,
                label = "5m",
                totalSeconds = 300,
                remainingMillis = 300_000,
                state = TimerState.RUNNING,
                endElapsedRealtime = now + 120_000
            )
        )

        val timer = store.loadTimers(nowElapsed = now + 30_000).single()

        assertEquals(4, timer.id)
        assertEquals(TimerState.RUNNING, timer.state)
        assertEquals(90_000, timer.remainingMillis)
    }

    @Test
    fun expiredRunningTimerRestoresAsFinished() {
        val now = SystemClock.elapsedRealtime()
        store.upsert(
            PersistedTimerRecord(
                id = 5,
                label = "done",
                totalSeconds = 1,
                remainingMillis = 1_000,
                state = TimerState.RUNNING,
                endElapsedRealtime = now - 1
            )
        )

        val timer = store.loadTimers(nowElapsed = now).single()

        assertEquals(TimerState.FINISHED, timer.state)
        assertEquals(0, timer.remainingMillis)
    }

    @Test
    fun expiryReceiverMarksPersistedTimerFinished() {
        val now = SystemClock.elapsedRealtime()
        store.upsert(
            PersistedTimerRecord(
                id = 8,
                label = "tea",
                totalSeconds = 180,
                remainingMillis = 180_000,
                state = TimerState.RUNNING,
                endElapsedRealtime = now + 180_000
            )
        )

        TimerExpiryReceiver().onReceive(
            context,
            Intent(context, TimerExpiryReceiver::class.java).apply {
                action = TimerAlarmScheduler.ACTION_TIMER_EXPIRED
                putExtra(TimerAlarmScheduler.EXTRA_TIMER_ID, 8)
            }
        )

        val timer = store.loadTimers().single()
        assertEquals(TimerState.FINISHED, timer.state)
        assertEquals(0, timer.remainingMillis)
    }

    @Test
    fun rebootCleanupRemovesOnlyRunningTimers() {
        store.replace(
            listOf(
                PersistedTimerRecord(1, "running", 60, 60_000, TimerState.RUNNING, 100_000),
                PersistedTimerRecord(2, "paused", 60, 30_000, TimerState.PAUSED)
            )
        )

        val removed = store.removeRunningTimersForReboot()

        assertEquals(listOf(1), removed.map { it.id })
        assertEquals(listOf(2), store.loadTimers().map { it.id })
        assertTrue(store.loadTimers().all { it.state != TimerState.RUNNING })
    }
}
