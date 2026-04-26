package com.sysadmindoc.alarmclock.ui.stats

import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import java.time.DayOfWeek
import java.util.Locale

data class StatsHistoryFilter(
    val query: String = "",
    val action: String? = null,
    val day: DayOfWeek? = null
) {
    val isActive: Boolean
        get() = query.isNotBlank() || action != null || day != null
}

fun filterAlarmEvents(
    events: List<AlarmEvent>,
    filter: StatsHistoryFilter
): List<AlarmEvent> {
    val normalizedQuery = filter.query.trim().lowercase(Locale.US)

    return events.filter { event ->
        val matchesQuery = normalizedQuery.isBlank() ||
            event.alarmLabel.lowercase(Locale.US).contains(normalizedQuery) ||
            event.challengeType.lowercase(Locale.US).replace("_", " ").contains(normalizedQuery) ||
            dayLabel(event.dayOfWeek).lowercase(Locale.US).startsWith(normalizedQuery) ||
            actionLabel(event.action).lowercase(Locale.US).startsWith(normalizedQuery)

        val matchesAction = filter.action == null || event.action == filter.action
        val matchesDay = filter.day == null || event.dayOfWeek == filter.day.value

        matchesQuery && matchesAction && matchesDay
    }
}

fun dayLabel(dayOfWeek: Int): String {
    return runCatching {
        DayOfWeek.of(dayOfWeek).name.lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) }
    }.getOrDefault("Unknown")
}

private fun actionLabel(action: String): String {
    return when (action) {
        AlarmEvent.ACTION_DISMISSED -> "Dismissed"
        AlarmEvent.ACTION_SNOOZED -> "Snoozed"
        AlarmEvent.ACTION_SKIPPED -> "Skipped"
        AlarmEvent.ACTION_MISSED -> "Missed"
        else -> action.lowercase(Locale.US).replace("_", " ")
    }
}
