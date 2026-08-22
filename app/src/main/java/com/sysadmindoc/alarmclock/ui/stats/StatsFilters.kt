package com.sysadmindoc.alarmclock.ui.stats

import android.content.res.Resources
import com.sysadmindoc.alarmclock.R
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
    filter: StatsHistoryFilter,
    resources: Resources
): List<AlarmEvent> {
    val normalizedQuery = filter.query.trim().lowercase(Locale.US)

    return events.filter { event ->
        val matchesQuery = normalizedQuery.isBlank() ||
            event.alarmLabel.lowercase(Locale.US).contains(normalizedQuery) ||
            event.challengeType.lowercase(Locale.US).replace("_", " ").contains(normalizedQuery) ||
            dayLabel(event.dayOfWeek).lowercase(Locale.US).startsWith(normalizedQuery) ||
            actionLabel(resources, event.action).lowercase(Locale.US).startsWith(normalizedQuery)

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

private fun actionLabel(resources: Resources, action: String): String {
    val resource = when (action) {
        AlarmEvent.ACTION_DISMISSED -> R.string.stats_action_dismissed
        AlarmEvent.ACTION_SNOOZED -> R.string.stats_action_snoozed
        AlarmEvent.ACTION_SKIPPED -> R.string.stats_action_skipped
        AlarmEvent.ACTION_MISSED -> R.string.stats_action_missed
        else -> return action.lowercase(Locale.US).replace("_", " ")
    }
    return resources.getString(resource)
}
