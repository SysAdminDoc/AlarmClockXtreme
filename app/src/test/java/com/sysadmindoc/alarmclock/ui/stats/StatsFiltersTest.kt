package com.sysadmindoc.alarmclock.ui.stats

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class StatsFiltersTest {

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun filtersByAlarmLabelChallengeActionAndDayText() {
        val events = listOf(
            event(label = "Gym", action = AlarmEvent.ACTION_DISMISSED, challengeType = "MATH_EASY", day = DayOfWeek.MONDAY),
            event(label = "Medication", action = AlarmEvent.ACTION_SNOOZED, challengeType = "STROOP", day = DayOfWeek.TUESDAY),
            event(label = "School", action = AlarmEvent.ACTION_MISSED, challengeType = "NONE", day = DayOfWeek.FRIDAY)
        )

        assertEquals(listOf(events[0]), filterAlarmEvents(events, StatsHistoryFilter(query = "gym"), resources))
        assertEquals(listOf(events[1]), filterAlarmEvents(events, StatsHistoryFilter(query = "stroop"), resources))
        assertEquals(listOf(events[2]), filterAlarmEvents(events, StatsHistoryFilter(query = "missed"), resources))
        assertEquals(listOf(events[0]), filterAlarmEvents(events, StatsHistoryFilter(query = "mon"), resources))
    }

    @Test
    fun combinesActionAndDayFilters() {
        val events = listOf(
            event(label = "Work", action = AlarmEvent.ACTION_SNOOZED, day = DayOfWeek.MONDAY),
            event(label = "Work", action = AlarmEvent.ACTION_DISMISSED, day = DayOfWeek.MONDAY),
            event(label = "Work", action = AlarmEvent.ACTION_SNOOZED, day = DayOfWeek.TUESDAY)
        )

        val filtered = filterAlarmEvents(
            events,
            StatsHistoryFilter(
                action = AlarmEvent.ACTION_SNOOZED,
                day = DayOfWeek.MONDAY
            )
        , resources)

        assertEquals(listOf(events[0]), filtered)
    }

    @Test
    fun inactiveFilterReturnsAllEvents() {
        val events = listOf(
            event(label = "A", day = DayOfWeek.MONDAY),
            event(label = "B", day = DayOfWeek.SUNDAY)
        )

        assertEquals(events, filterAlarmEvents(events, StatsHistoryFilter(), resources))
    }

    @Test
    fun invalidDayLabelDoesNotCrashSearch() {
        val events = listOf(
            AlarmEvent(
                alarmId = 1,
                alarmLabel = "Corrupt day",
                scheduledTime = 0,
                firedAt = 0,
                action = AlarmEvent.ACTION_DISMISSED,
                dayOfWeek = 99
            )
        )

        assertEquals("Unknown", dayLabel(99))
        assertTrue(filterAlarmEvents(events, StatsHistoryFilter(query = "unknown"), resources).isNotEmpty())
    }

    private fun event(
        label: String,
        action: String = AlarmEvent.ACTION_DISMISSED,
        challengeType: String = "NONE",
        day: DayOfWeek
    ): AlarmEvent {
        return AlarmEvent(
            alarmId = label.hashCode().toLong(),
            alarmLabel = label,
            scheduledTime = 0L,
            firedAt = 0L,
            action = action,
            challengeType = challengeType,
            dayOfWeek = day.value
        )
    }
}
