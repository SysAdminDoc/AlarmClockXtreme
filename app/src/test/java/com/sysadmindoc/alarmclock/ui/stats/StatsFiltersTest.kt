package com.sysadmindoc.alarmclock.ui.stats

import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class StatsFiltersTest {

    @Test
    fun filtersByAlarmLabelChallengeActionAndDayText() {
        val events = listOf(
            event(label = "Gym", action = AlarmEvent.ACTION_DISMISSED, challengeType = "MATH_EASY", day = DayOfWeek.MONDAY),
            event(label = "Medication", action = AlarmEvent.ACTION_SNOOZED, challengeType = "STROOP", day = DayOfWeek.TUESDAY),
            event(label = "School", action = AlarmEvent.ACTION_MISSED, challengeType = "NONE", day = DayOfWeek.FRIDAY)
        )

        assertEquals(listOf(events[0]), filterAlarmEvents(events, StatsHistoryFilter(query = "gym")))
        assertEquals(listOf(events[1]), filterAlarmEvents(events, StatsHistoryFilter(query = "stroop")))
        assertEquals(listOf(events[2]), filterAlarmEvents(events, StatsHistoryFilter(query = "missed")))
        assertEquals(listOf(events[0]), filterAlarmEvents(events, StatsHistoryFilter(query = "mon")))
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
        )

        assertEquals(listOf(events[0]), filtered)
    }

    @Test
    fun inactiveFilterReturnsAllEvents() {
        val events = listOf(
            event(label = "A", day = DayOfWeek.MONDAY),
            event(label = "B", day = DayOfWeek.SUNDAY)
        )

        assertEquals(events, filterAlarmEvents(events, StatsHistoryFilter()))
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
        assertTrue(filterAlarmEvents(events, StatsHistoryFilter(query = "unknown")).isNotEmpty())
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
