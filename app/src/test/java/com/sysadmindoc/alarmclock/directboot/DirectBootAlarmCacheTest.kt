package com.sysadmindoc.alarmclock.directboot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Direct Boot fallback cache invariants. This cache is the only thing that can
 * re-arm an alarm before first unlock after a reboot, so its "keep the earliest
 * upcoming alarm" and one-shot fired-marker logic must hold.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DirectBootAlarmCacheTest {

    private lateinit var context: Context
    private val now = 1_700_000_000_000L

    private fun alarm(id: Long, repeating: Boolean = false) = Alarm(
        id = id,
        hour = 7,
        minute = 0,
        repeatDays = if (repeating) setOf(java.time.DayOfWeek.MONDAY) else emptySet()
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DirectBootAlarmCache.clear(context)
    }

    @Test
    fun saveIfEarlierWritesAndReadsBackOnEmptyCache() {
        DirectBootAlarmCache.saveIfEarlier(context, alarm(1L), now + 600_000L, now)

        val snapshot = DirectBootAlarmCache.read(context)
        assertEquals(1L, snapshot?.alarmId)
        assertEquals(now + 600_000L, snapshot?.triggerTime)
        // Labels are never persisted to device-protected storage for privacy.
        assertEquals("", snapshot?.label)
    }

    @Test
    fun pastTriggerTimeAndInvalidIdAreIgnored() {
        DirectBootAlarmCache.saveIfEarlier(context, alarm(1L), now - 1_000L, now)
        assertNull(DirectBootAlarmCache.read(context))

        DirectBootAlarmCache.saveIfEarlier(context, alarm(0L), now + 600_000L, now)
        assertNull(DirectBootAlarmCache.read(context))
    }

    @Test
    fun keepsEarliestAcrossDifferentAlarms() {
        DirectBootAlarmCache.saveIfEarlier(context, alarm(1L), now + 600_000L, now)
        // A later, different alarm must not displace the earlier cached one.
        DirectBootAlarmCache.saveIfEarlier(context, alarm(2L), now + 1_200_000L, now)
        assertEquals(1L, DirectBootAlarmCache.read(context)?.alarmId)

        // An earlier, different alarm replaces it.
        DirectBootAlarmCache.saveIfEarlier(context, alarm(3L), now + 300_000L, now)
        val snapshot = DirectBootAlarmCache.read(context)
        assertEquals(3L, snapshot?.alarmId)
        assertEquals(now + 300_000L, snapshot?.triggerTime)
    }

    @Test
    fun sameAlarmIdRefreshesEvenIfLater() {
        DirectBootAlarmCache.saveIfEarlier(context, alarm(1L), now + 600_000L, now)
        // The same alarm rescheduled to a later time should refresh the cache.
        DirectBootAlarmCache.saveIfEarlier(context, alarm(1L), now + 1_800_000L, now)
        assertEquals(now + 1_800_000L, DirectBootAlarmCache.read(context)?.triggerTime)
    }

    @Test
    fun clearRemovesCachedAlarm() {
        DirectBootAlarmCache.saveIfEarlier(context, alarm(1L), now + 600_000L, now)
        DirectBootAlarmCache.clear(context)
        assertNull(DirectBootAlarmCache.read(context))
    }

    @Test
    fun firedOneShotMarkerIsConsumedOnceForOneShotAlarms() {
        DirectBootAlarmCache.recordFired(context, alarmId = 9L, triggerTime = now - 1_000L, firedAt = now)

        // First consume succeeds for a matching one-shot alarm fired in the past.
        assertTrue(DirectBootAlarmCache.consumeFiredOneShotMarker(context, alarm(9L), now))
        // Marker is cleared, so a second consume fails.
        assertFalse(DirectBootAlarmCache.consumeFiredOneShotMarker(context, alarm(9L), now))
    }

    @Test
    fun firedMarkerIsIgnoredForRepeatingAlarmsAndMismatches() {
        DirectBootAlarmCache.recordFired(context, alarmId = 9L, triggerTime = now - 1_000L, firedAt = now)

        // Repeating alarms never consume the one-shot marker.
        assertFalse(DirectBootAlarmCache.consumeFiredOneShotMarker(context, alarm(9L, repeating = true), now))
        // A different alarm id does not match.
        assertFalse(DirectBootAlarmCache.consumeFiredOneShotMarker(context, alarm(8L), now))
    }

    @Test
    fun futureFiredTriggerIsNotConsumed() {
        // A fired marker whose trigger is in the future is stale/invalid and must not consume.
        DirectBootAlarmCache.recordFired(context, alarmId = 9L, triggerTime = now + 60_000L, firedAt = now)
        assertFalse(DirectBootAlarmCache.consumeFiredOneShotMarker(context, alarm(9L), now))
    }
}
