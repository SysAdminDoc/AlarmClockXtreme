package com.sysadmindoc.alarmclock.ui.timer

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
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
    fun finishClaimIsIdempotentAcrossUiAndReceiverDelivery() {
        val now = SystemClock.elapsedRealtime()
        store.upsert(
            PersistedTimerRecord(
                id = 9,
                label = "rice",
                totalSeconds = 60,
                remainingMillis = 0,
                state = TimerState.RUNNING,
                endElapsedRealtime = now
            )
        )

        assertNotNull(store.markFinished(9))
        assertNull(store.markFinished(9))
        assertEquals(TimerState.FINISHED, store.loadRecords().single().state)
    }

    @Test
    fun restartFinishedAtomicallyConsumesSourceAndIgnoresDuplicateDelivery() {
        store.replace(
            listOf(
                PersistedTimerRecord(4, "Tea", 90, 0, TimerState.FINISHED),
                PersistedTimerRecord(9, "Paused", 30, 10_000, TimerState.PAUSED)
            )
        )

        val restarted = store.restartFinished(4, nowElapsed = 100_000L)
        val duplicate = store.restartFinished(4, nowElapsed = 101_000L)
        val records = store.loadRecords(nowElapsed = 100_000L)

        assertNotNull(restarted)
        assertEquals(10, restarted?.id)
        assertEquals("Tea", restarted?.label)
        assertEquals(90L, restarted?.totalSeconds)
        assertEquals(90_000L, restarted?.remainingMillis)
        assertEquals(190_000L, restarted?.endElapsedRealtime)
        assertEquals(TimerState.RUNNING, restarted?.state)
        assertNull(duplicate)
        assertTrue(records.none { it.id == 4 })
        assertEquals(1, records.count { it.state == TimerState.RUNNING })
    }

    @Test
    fun restoreClaimsOverdueTimersExactlyOnce() {
        val now = SystemClock.elapsedRealtime()
        store.replace(
            listOf(
                PersistedTimerRecord(10, "overdue", 60, 1_000, TimerState.RUNNING, now - 1),
                PersistedTimerRecord(11, "later", 60, 60_000, TimerState.RUNNING, now + 60_000),
                PersistedTimerRecord(12, "paused", 60, 30_000, TimerState.PAUSED)
            )
        )

        val firstRestore = store.restoreSnapshot(now)
        val secondRestore = store.restoreSnapshot(now)

        assertEquals(listOf(10), firstRestore.newlyFinished.map { it.id })
        assertTrue(secondRestore.newlyFinished.isEmpty())
        assertEquals(TimerState.FINISHED, secondRestore.records.first { it.id == 10 }.state)
        assertEquals(TimerState.RUNNING, secondRestore.records.first { it.id == 11 }.state)
        assertEquals(TimerState.PAUSED, secondRestore.records.first { it.id == 12 }.state)
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

    @Test
    fun alarmClockStartIsAtomicAndReusesImmediateDuplicate() {
        val first = store.startOrReuse(totalSeconds = 90, label = "Tea", nowElapsed = 10_000L)
        val duplicate = store.startOrReuse(totalSeconds = 90, label = "Tea", nowElapsed = 12_000L)

        assertTrue(first.created)
        assertTrue(!duplicate.created)
        assertEquals(first.record.id, duplicate.record.id)
        assertEquals(1, store.loadRecords(nowElapsed = 12_000L).size)
    }

    @Test
    fun alarmClockStartAllowsSameTimerAfterCoalescingWindow() {
        store.startOrReuse(totalSeconds = 90, label = "Tea", nowElapsed = 10_000L)

        val later = store.startOrReuse(totalSeconds = 90, label = "Tea", nowElapsed = 16_000L)

        assertTrue(later.created)
        assertEquals(2, store.loadRecords(nowElapsed = 16_000L).size)
    }

    @Test
    fun runningTimersFromAPreviousBootAreDropped() {
        setBootCount(5L)
        store.replace(
            listOf(
                PersistedTimerRecord(1, "running", 600, 600_000, TimerState.RUNNING, 900_000),
                PersistedTimerRecord(2, "paused", 60, 30_000, TimerState.PAUSED)
            )
        )
        assertEquals(listOf(1, 2), store.loadTimers().map { it.id })

        // The device rebooted; elapsedRealtime restarted, so the stored
        // deadline means nothing any more.
        setBootCount(6L)

        assertEquals(listOf(2), store.loadTimers().map { it.id })
        assertTrue(store.restoreSnapshot().newlyFinished.isEmpty())
    }

    @Test
    fun runningTimersSurviveWithinTheSameBoot() {
        setBootCount(11L)
        store.replace(
            listOf(PersistedTimerRecord(3, "running", 600, 600_000, TimerState.RUNNING, 900_000))
        )

        assertEquals(listOf(3), store.loadTimers().map { it.id })
    }

    @Test
    fun timersWrittenBeforeTheBootStampExistedAreKept() {
        setBootCount(9L)
        context.getSharedPreferences("timer_state", Context.MODE_PRIVATE)
            .edit()
            .putString(
                "timers_json",
                """[{"id":7,"label":"legacy","totalSeconds":600,""" +
                    """"remainingMillis":600000,"state":"RUNNING","endElapsedRealtime":900000}]"""
            )
            .commit()

        assertEquals(listOf(7), TimerStore(context).loadTimers().map { it.id })
    }

    @Test
    fun rebootCleanupStillSeesTimersFromTheOldBoot() {
        setBootCount(2L)
        store.replace(
            listOf(PersistedTimerRecord(8, "running", 600, 600_000, TimerState.RUNNING, 900_000))
        )
        setBootCount(3L)

        assertEquals(listOf(8), store.removeRunningTimersForReboot().map { it.id })
    }

    @Test
    fun timersStartedThroughTheAlarmClockIntentCarryTheCurrentBoot() {
        setBootCount(14L)
        // startOrReuse builds its record with the default stamp, so the write
        // path is the only thing that can put the boot on it. If it stops
        // doing that the timer is unkillable by a reboot.
        val started = store.startOrReuse(totalSeconds = 600, label = "eggs")
        assertTrue(started.created)

        setBootCount(15L)

        assertEquals(emptyList<Int>(), store.loadTimers().map { it.id })
    }

    @Test
    fun aRestartedTimerBelongsToTheBootItWasRestartedIn() {
        setBootCount(21L)
        store.upsert(
            PersistedTimerRecord(6, "tea", 300, 0, TimerState.FINISHED, 0)
        )
        val restarted = store.restartFinished(6)
        assertNotNull(restarted)

        assertEquals(listOf(restarted!!.id), store.loadTimers().map { it.id })
        setBootCount(22L)
        assertEquals(emptyList<Int>(), store.loadTimers().map { it.id })
    }

    @Test
    fun aPausedTimerKeepsItsOldStampAndSurvivesAReboot() {
        setBootCount(31L)
        store.replace(
            listOf(PersistedTimerRecord(9, "paused", 600, 300_000, TimerState.PAUSED, 0))
        )
        setBootCount(32L)

        // Nothing about a paused timer depends on elapsedRealtime, so a reboot
        // is not a reason to drop it.
        assertEquals(listOf(9), store.loadTimers().map { it.id })
    }

    private fun setBootCount(value: Long) {
        android.provider.Settings.Global.putLong(
            context.contentResolver,
            android.provider.Settings.Global.BOOT_COUNT,
            value
        )
    }
}
