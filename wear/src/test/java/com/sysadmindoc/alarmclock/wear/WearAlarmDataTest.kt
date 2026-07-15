package com.sysadmindoc.alarmclock.wear

import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.DataMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WearAlarmDataTest {

    private val base = 1_700_000_000_000L

    private fun snapshot(
        hasAlarm: Boolean = true,
        alarmId: Long = 7L,
        label: String = "",
        timeLabel: String = "",
        triggerTime: Long = 0L,
        isFiring: Boolean = false,
        updatedAt: Long = System.currentTimeMillis(),
        timezonePolicy: String = "LOCAL",
        fixedTimezoneId: String = ""
    ) = WearAlarmSnapshot(
        hasAlarm, alarmId, label, timeLabel, triggerTime, isFiring, updatedAt,
        timezonePolicy, fixedTimezoneId
    )

    // --- WearAlarmText.formatRemaining ---

    @Test
    fun formatRemainingCoversAllBuckets() {
        assertEquals("due now", WearAlarmText.formatRemaining(base, base))
        assertEquals("due now", WearAlarmText.formatRemaining(base - 1_000L, base))
        assertEquals("<1m", WearAlarmText.formatRemaining(base + 30_000L, base))
        assertEquals("5m", WearAlarmText.formatRemaining(base + 5 * 60_000L, base))
        assertEquals("2h 10m", WearAlarmText.formatRemaining(base + (2 * 60 + 10) * 60_000L, base))
        assertEquals("1d 3h", WearAlarmText.formatRemaining(base + (27L * 3_600_000L), base))
    }

    // --- Tile text ---

    @Test
    fun mainTimeLabelReflectsState() {
        assertEquals("Open phone app", WearAlarmText.mainTimeLabel(snapshot(hasAlarm = false)))
        assertEquals("7:30 AM", WearAlarmText.mainTimeLabel(snapshot(timeLabel = "7:30 AM")))
        assertEquals("Scheduled", WearAlarmText.mainTimeLabel(snapshot(timeLabel = "")))
    }

    @Test
    fun secondaryLabelHonoursStatusFiringAndCountdown() {
        assertEquals("Queued for phone", WearAlarmText.secondaryLabel(snapshot(), actionStatus = "Queued for phone"))
        assertEquals("Waiting for phone sync", WearAlarmText.secondaryLabel(snapshot(hasAlarm = false)))
        assertEquals("Alarm is ringing", WearAlarmText.secondaryLabel(snapshot(isFiring = true)))
        assertEquals(
            "Gym - 5m",
            WearAlarmText.secondaryLabel(snapshot(label = "Gym", triggerTime = base + 5 * 60_000L), now = base)
        )
        assertEquals(
            "10m",
            WearAlarmText.secondaryLabel(snapshot(label = "", triggerTime = base + 10 * 60_000L), now = base)
        )
    }

    @Test
    fun secondaryLabelBlocksStalePhoneSnapshots() {
        val stale = snapshot(
            label = "Gym",
            triggerTime = base + 5 * 60_000L,
            updatedAt = base - WearAlarmText.STALE_AFTER_MS - 1L
        )

        assertEquals(true, WearAlarmText.isStale(stale, now = base))
        assertEquals("Phone sync stale", WearAlarmText.secondaryLabel(stale, now = base))
        assertEquals("Phone sync stale", WearAlarmText.complicationLongText(stale, now = base))
        assertEquals("Sync", WearAlarmText.complicationShortText(stale))
    }

    // --- Complication text ---

    @Test
    fun complicationShortTextAndTitle() {
        assertEquals("Ringing", WearAlarmText.complicationShortText(snapshot(isFiring = true)))
        assertEquals("7:30 AM", WearAlarmText.complicationShortText(snapshot(timeLabel = "7:30 AM")))
        assertEquals("Alarm", WearAlarmText.complicationShortText(snapshot(timeLabel = "")))
        assertEquals("No alarm", WearAlarmText.complicationShortText(snapshot(hasAlarm = false)))

        assertEquals("ACX", WearAlarmText.complicationShortTitle(snapshot(isFiring = true)))
        assertEquals("Next", WearAlarmText.complicationShortTitle(snapshot(label = "")))
        // Title is clamped to the short limit.
        assertEquals(
            "WorkdayAlarm".take(WearAlarmText.SHORT_TITLE_LIMIT),
            WearAlarmText.complicationShortTitle(snapshot(label = "WorkdayAlarm wake up now"))
        )
    }

    @Test
    fun complicationLongTextJoinsParts() {
        assertEquals("Alarm is ringing", WearAlarmText.complicationLongText(snapshot(isFiring = true)))
        assertEquals(
            "7:30 AM - Gym - 5m",
            WearAlarmText.complicationLongText(
                snapshot(label = "Gym", timeLabel = "7:30 AM", triggerTime = base + 5 * 60_000L),
                now = base
            )
        )
        assertEquals("No phone alarm synced", WearAlarmText.complicationLongText(snapshot(hasAlarm = false)))
    }

    @Test
    fun fixedTimezoneIsDisclosedOnWearTextSurfaces() {
        val fixed = snapshot(
            label = "Medication",
            timeLabel = "4:00 AM",
            triggerTime = base + 5 * 60_000L,
            timezonePolicy = "FIXED",
            fixedTimezoneId = "America/New_York"
        )

        assertEquals(
            "Medication - 5m - America/New York",
            WearAlarmText.secondaryLabel(fixed, now = base)
        )
        assertEquals(
            "4:00 AM - Medication - America/New York - 5m",
            WearAlarmText.complicationLongText(fixed, now = base)
        )
    }

    @Test
    fun contentDescriptionReflectsState() {
        assertEquals("AlarmClockXtreme alarm is ringing", WearAlarmText.contentDescription(snapshot(isFiring = true)))
        assertEquals(
            "Next AlarmClockXtreme alarm 7:30 AM",
            WearAlarmText.contentDescription(snapshot(timeLabel = "7:30 AM"))
        )
        assertEquals(
            "No AlarmClockXtreme alarm synced from phone",
            WearAlarmText.contentDescription(snapshot(hasAlarm = false))
        )
    }

    // --- Action routing ---

    @Test
    fun actionPathForClickRoutesKnownActionsOnly() {
        assertEquals(WearAlarmData.PATH_ACTION_SKIP, WearAlarmData.actionPathForClick(WearAlarmData.CLICK_SKIP))
        assertEquals(WearAlarmData.PATH_ACTION_SNOOZE, WearAlarmData.actionPathForClick(WearAlarmData.CLICK_SNOOZE))
        assertEquals(WearAlarmData.PATH_ACTION_DISMISS, WearAlarmData.actionPathForClick(WearAlarmData.CLICK_DISMISS))
        assertNull(WearAlarmData.actionPathForClick("refresh"))
        assertNull(WearAlarmData.actionPathForClick(""))
    }

    // --- Snapshot persistence ---

    @Test
    fun storeRoundTripsThroughSharedPreferences() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val snap = WearAlarmSnapshot(
            hasAlarm = true,
            alarmId = 42L,
            label = "Gym",
            timeLabel = "6:00 AM",
            triggerTime = base,
            isFiring = true,
            updatedAt = base + 1_000L
        )

        WearAlarmStore.save(context, snap)
        assertEquals(snap, WearAlarmStore.load(context))
    }

    @Test
    fun fromDataMapDeserializesSnapshot() {
        val dataMap = DataMap().apply {
            putBoolean(WearAlarmData.KEY_HAS_ALARM, true)
            putLong(WearAlarmData.KEY_ALARM_ID, 99L)
            putString(WearAlarmData.KEY_LABEL, "Work")
            putString(WearAlarmData.KEY_TIME_LABEL, "8:15 AM")
            putLong(WearAlarmData.KEY_TRIGGER_TIME, base)
            putBoolean(WearAlarmData.KEY_IS_FIRING, false)
            putLong(WearAlarmData.KEY_UPDATED_AT, base + 5L)
            putString(WearAlarmData.KEY_TIMEZONE_POLICY, "FIXED")
            putString(WearAlarmData.KEY_FIXED_TIMEZONE_ID, "Europe/London")
        }

        val snap = WearAlarmStore.fromDataMap(dataMap)
        assertEquals(
            WearAlarmSnapshot(
                true, 99L, "Work", "8:15 AM", base, false, base + 5L,
                "FIXED", "Europe/London"
            ),
            snap
        )
    }

    @Test
    fun fromDataMapUsesDefaultsForEmptyMap() {
        assertEquals(WearAlarmSnapshot(), WearAlarmStore.fromDataMap(DataMap()))
    }
}
