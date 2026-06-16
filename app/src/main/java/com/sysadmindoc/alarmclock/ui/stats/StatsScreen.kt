package com.sysadmindoc.alarmclock.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.health.HealthConnectAvailability
import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepSummary
import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.repository.AlarmStats
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppInlineNotice
import com.sysadmindoc.alarmclock.ui.components.AppLoadingCard
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.components.AppInputShape
import com.sysadmindoc.alarmclock.ui.components.appOutlinedTextFieldColors
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = state.stats
    // Pull the 24-hour preference straight from the ViewModel so EventRow
    // timestamps match the rest of the app. The previous parameterised
    // signature defaulted to false because the nav graph never passed it.
    val is24Hour = state.is24Hour
    var showClearDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
    val historyFilter = StatsHistoryFilter(
        query = searchQuery,
        action = selectedAction,
        day = selectedDay
    )
    val filteredEvents = remember(state.recentEvents, historyFilter) {
        filterAlarmEvents(state.recentEvents, historyFilter)
    }

    val summaryLine = when {
        state.isLoading -> "Collecting history and response patterns."
        state.recentEvents.isEmpty() -> "Your alarm habits will start to appear once you build some history."
        else -> "Track consistency, snooze behavior, and which mornings are easiest to handle."
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            AlarmClockHeroHeader(
                title = "Statistics",
                subtitle = summaryLine,
                overline = "Alarm history",
                badge = {
                    AppStatusChip(
                        label = wakeStreakBadgeLabel(stats),
                        icon = Icons.Default.LocalFireDepartment,
                        color = if (stats.currentStreak > 0) SnoozeYellow else TextMuted
                    )
                    AppStatusChip(
                        label = "${stats.alarmsThisWeek} this week",
                        icon = Icons.Default.CalendarMonth
                    )
                    if (state.recentEvents.isNotEmpty()) {
                        AppStatusChip(
                            label = "${state.recentEvents.size} recent events",
                            icon = Icons.Default.BarChart,
                            color = SnoozeYellow
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close statistics", tint = TextMuted)
                    }
                }
            )
        }

        if (state.isLoading) {
            items(3) {
                AppLoadingCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    height = 150.dp
                )
            }
        } else {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        StatMiniCard(
                            label = "Streak",
                            value = compactDays(stats.currentStreak),
                            color = if (stats.currentStreak > 0) SnoozeYellow else TextMuted,
                            icon = Icons.Default.LocalFireDepartment,
                            modifier = Modifier.width(138.dp)
                        )
                    }
                    item {
                        StatMiniCard(
                            label = "This week",
                            value = "${stats.alarmsThisWeek}",
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.CalendarMonth,
                            modifier = Modifier.width(138.dp)
                        )
                    }
                    item {
                        StatMiniCard(
                            label = "Snoozed",
                            value = "${stats.snoozeRate}%",
                            color = SnoozeYellow,
                            icon = Icons.Default.Snooze,
                            modifier = Modifier.width(138.dp)
                        )
                    }
                }
            }

            item {
                WakeStreakBadge(
                    stats = stats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (state.healthConnectEnabled) {
                item {
                    HealthConnectStatsCard(
                        summary = state.healthConnectSleepSummary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            item {
                SleepWakeAnalyticsCard(
                    analytics = state.sleepWakeAnalytics,
                    healthConnectEnabled = state.healthConnectEnabled,
                    sleepPermissionGranted = state.healthConnectSleepSummary.permissionGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ActigraphyBucketsCard(
                    sessions = state.actigraphySessions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    AppSectionTitle(
                        title = "Average wake-up time",
                        description = "How long it usually takes to dismiss an alarm after it starts."
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        val mins = stats.averageDismissTimeSec / 60
                        val secs = stats.averageDismissTimeSec % 60
                        Text(
                            text = if (mins > 0) "${mins}m ${secs}s" else "${secs}s",
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "average response",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        AppSectionTitle(
                            title = "Outcome mix",
                            description = "How alarms usually resolve."
                        )
                        BreakdownRow("Dismissed", stats.totalDismissed, DismissGreen)
                        BreakdownRow("Snoozed", stats.totalSnoozed, SnoozeYellow)
                        BreakdownRow("Skipped", stats.totalSkipped, MaterialTheme.colorScheme.primary)
                        BreakdownRow("Missed", stats.totalMissed, AccentRed)
                    }

                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        AppSectionTitle(
                            title = "Busiest day",
                            description = "Where alarms cluster most often."
                        )
                        val busiest = stats.dayOfWeekCounts.maxByOrNull { it.value }
                        Text(
                            text = busiest?.key?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "No data",
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = busiest?.let { "${it.value} alarms recorded" } ?: "Alarm history will fill this in.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        val calmest = stats.dayOfWeekAvgResponseSec
                            .filterValues { it > 0 }
                            .minByOrNull { it.value }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = calmest?.let {
                                "Fastest responses: ${it.key.name.lowercase().replaceFirstChar { c -> c.uppercase() }} • ${it.value}s"
                            } ?: "Need more dismiss history for day-by-day response trends.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    AppSectionTitle(
                        title = "Alarms by day",
                        description = "A quick visual read on which days carry the most alarm load."
                    )
                    DayOfWeekChart(
                        counts = stats.dayOfWeekCounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppSectionTitle(
                        title = "Recent history",
                        description = "The last few alarm outcomes, useful for spotting patterns."
                    )
                    if (state.recentEvents.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear history")
                        }
                    }
                }
            }

            if (state.recentEvents.isNotEmpty()) {
                item {
                    StatsFilterCard(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        selectedAction = selectedAction,
                        onActionChange = { selectedAction = it },
                        selectedDay = selectedDay,
                        onDayChange = { selectedDay = it },
                        resultCount = filteredEvents.size,
                        totalCount = state.recentEvents.size,
                        onClearFilters = {
                            searchQuery = ""
                            selectedAction = null
                            selectedDay = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            if (state.recentEvents.isEmpty()) {
                item {
                    AppSurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        AppEmptyState(
                            icon = Icons.Default.BarChart,
                            title = "No alarm history yet",
                            description = "Dismissed, snoozed, skipped, and missed alarms will appear here once the app has something to learn from."
                        )
                    }
                }
            } else if (filteredEvents.isEmpty()) {
                item {
                    AppSurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        AppEmptyState(
                            icon = Icons.Default.Search,
                            title = "No history matches",
                            description = "Clear filters or try another alarm label, challenge, action, or day."
                        )
                    }
                }
            } else {
                item {
                    AppSurfaceCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        filteredEvents.forEachIndexed { index, event ->
                            EventRow(event = event, is24Hour = is24Hour)
                            if (index != filteredEvents.lastIndex) {
                                HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = AccentRed
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear history")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            title = {
                Text(
                    "Clear alarm history?",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "This removes recorded alarm outcomes and resets the statistics shown on this screen. It does not delete your actual alarms.",
                    color = TextSecondary
                )
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun StatsFilterCard(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedAction: String?,
    onActionChange: (String?) -> Unit,
    selectedDay: DayOfWeek?,
    onDayChange: (DayOfWeek?) -> Unit,
    resultCount: Int,
    totalCount: Int,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFiltered = query.isNotBlank() || selectedAction != null || selectedDay != null

    AppSurfaceCard(modifier = modifier) {
        AppSectionTitle(
            title = "Find patterns",
            description = if (isFiltered) {
                "$resultCount of $totalCount recent events match"
            } else {
                "Filter by alarm label, challenge, action, or weekday."
            },
            action = {
                if (isFiltered) {
                    TextButton(onClick = onClearFilters) {
                        Text("Clear", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search label, challenge, action, or day") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted)
                    }
                }
            },
            colors = appOutlinedTextFieldColors(),
            shape = AppInputShape,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        FilterChipRow(
            label = "Outcome",
            chips = listOf(
                null to "All",
                AlarmEvent.ACTION_DISMISSED to "Dismissed",
                AlarmEvent.ACTION_SNOOZED to "Snoozed",
                AlarmEvent.ACTION_SKIPPED to "Skipped",
                AlarmEvent.ACTION_MISSED to "Missed"
            ),
            selected = selectedAction,
            onSelect = onActionChange
        )

        FilterChipRow(
            label = "Day",
            chips = listOf(null to "All days") + DayOfWeek.entries.map { it to it.name.take(3) },
            selected = selectedDay,
            onSelect = onDayChange
        )
    }
}

@Composable
private fun <T> FilterChipRow(
    label: String,
    chips: List<Pair<T?, String>>,
    selected: T?,
    onSelect: (T?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips.forEach { (value, text) ->
                AppFilterChip(
                    label = text,
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    selectionSemantics = true,
                )
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(modifier = modifier, highlighted = true) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun WakeStreakBadge(stats: AlarmStats, modifier: Modifier = Modifier) {
    val current = stats.currentStreak
    val best = maxOf(stats.bestStreak, current)
    val goal = stats.nextStreakGoal.coerceAtLeast(3)
    val progress = (current.toFloat() / goal).coerceIn(0f, 1f)

    AppSurfaceCard(modifier = modifier, highlighted = current > 0) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SnoozeYellow.copy(alpha = if (current > 0) 0.18f else 0.08f)
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Wake streak",
                        tint = if (current > 0) SnoozeYellow else TextMuted,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (current > 0) "${dayCountLabel(current)} wake streak" else "Wake streak",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = wakeStreakStatus(stats),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppStatusChip(
                        label = "Best ${dayCountLabel(best)}",
                        icon = Icons.Default.CheckCircle,
                        color = DismissGreen
                    )
                    AppStatusChip(
                        label = "Next $goal",
                        icon = Icons.Default.LocalFireDepartment,
                        color = SnoozeYellow
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next streak badge",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "$current / $goal days",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(SurfaceCard.copy(alpha = 0.58f), RoundedCornerShape(8.dp))
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .background(SnoozeYellow, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthConnectStatsCard(
    summary: HealthConnectSleepSummary,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(modifier = modifier, highlighted = summary.permissionGranted && summary.hasRecentSession) {
        AppSectionTitle(
            title = "Sleep context",
            description = when {
                summary.availability == HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED ->
                    "Update Health Connect before sleep sessions can be included."
                summary.availability == HealthConnectAvailability.UNAVAILABLE ->
                    "Health Connect is not available on this device."
                !summary.permissionGranted ->
                    "Grant READ_SLEEP in Settings to compare wake behavior with recent sleep."
                summary.hasRecentSession ->
                    "Recent Health Connect sessions stay local and are shown beside alarm history."
                else ->
                    "READ_SLEEP is granted, but no recent sleep sessions were found."
            }
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = if (summary.permissionGranted) "READ_SLEEP granted" else "Permission needed",
                icon = if (summary.permissionGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                color = if (summary.permissionGranted) DismissGreen else SnoozeYellow
            )
            AppStatusChip(
                label = "${summary.sessionsRead} sessions",
                icon = Icons.Default.BarChart,
                color = if (summary.sessionsRead > 0) MaterialTheme.colorScheme.primary else TextMuted
            )
            if (summary.hasRecentSession) {
                AppStatusChip(
                    label = "Last ${formatSleepMinutes(summary.lastSessionDurationMinutes)}",
                    icon = Icons.Default.CalendarMonth,
                    color = MaterialTheme.colorScheme.primary
                )
                AppStatusChip(
                    label = "Deep ${formatSleepMinutes(summary.deepStageMinutes)}",
                    icon = Icons.Default.CheckCircle,
                    color = DismissGreen
                )
                AppStatusChip(
                    label = "REM ${formatSleepMinutes(summary.remStageMinutes)}",
                    icon = Icons.Default.CheckCircle,
                    color = SnoozeYellow
                )
            }
        }
        summary.errorMessage?.let { error ->
            AppInlineNotice(
                title = "Health Connect needs attention",
                message = error,
                icon = Icons.Default.ErrorOutline,
                color = SnoozeYellow
            )
        }
    }
}

@Composable
private fun SleepWakeAnalyticsCard(
    analytics: SleepWakeAnalytics,
    healthConnectEnabled: Boolean,
    sleepPermissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
    val hasSleepData = analytics.points.any { it.sleepMinutes != null }
    val hasWakeData = analytics.points.any { it.hasWakeData }

    AppSurfaceCard(modifier = modifier, highlighted = analytics.hasSleepWakeCorrelation) {
        AppSectionTitle(
            title = "Sleep and wake patterns",
            description = sleepWakeAnalyticsDescription(
                analytics = analytics,
                healthConnectEnabled = healthConnectEnabled,
                sleepPermissionGranted = sleepPermissionGranted,
                hasSleepData = hasSleepData,
                hasWakeData = hasWakeData
            )
        )

        if (!analytics.hasAnyData) {
            AppEmptyState(
                icon = Icons.Default.BarChart,
                title = "No sleep or wake trend yet",
                description = "Alarm history and optional Health Connect sleep sessions will build this view locally."
            )
            return@AppSurfaceCard
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = "Sleep ${formatSleepMinutes(analytics.averageSleepMinutes)} avg",
                icon = Icons.Default.CalendarMonth,
                color = if (hasSleepData) MaterialTheme.colorScheme.primary else TextMuted
            )
            analytics.latestSleepScore?.let { score ->
                AppStatusChip(
                    label = "Sleep score $score",
                    icon = Icons.Default.CheckCircle,
                    color = sleepScoreColor(score)
                )
            }
            if (hasSleepData) {
                AppStatusChip(
                    label = "Sleep debt ${formatSleepMinutes(analytics.sleepDebtMinutes)}",
                    icon = Icons.Default.Snooze,
                    color = if (analytics.sleepDebtMinutes > 0L) SnoozeYellow else DismissGreen
                )
            }
            AppStatusChip(
                label = "Response ${analytics.averageResponseSec?.let(::formatSeconds) ?: "0s"} avg",
                icon = Icons.Default.CheckCircle,
                color = if (analytics.averageResponseSec != null) DismissGreen else TextMuted
            )
            AppStatusChip(
                label = "${analytics.totalSnoozes} snoozes",
                icon = Icons.Default.Snooze,
                color = if (analytics.totalSnoozes > 0) SnoozeYellow else TextMuted
            )
            AppStatusChip(
                label = "${analytics.totalChallengeRetries} retries",
                icon = Icons.Default.ErrorOutline,
                color = if (analytics.totalChallengeRetries > 0) AccentRed else TextMuted
            )
        }

        SleepWakeTrendChart(
            points = analytics.points,
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
        )

        SleepWakeFrictionChart(
            points = analytics.points,
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        )

        analytics.responseDeltaAfterShortSleepSec?.let { delta ->
            val label = if (delta >= 0) {
                "${formatSignedSeconds(delta)} slower after short sleep"
            } else {
                "${formatSeconds(-delta)} faster after short sleep"
            }
            Text(
                text = label,
                color = if (delta > 0) SnoozeYellow else DismissGreen,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SleepWakeTrendChart(
    points: List<SleepWakeDayPoint>,
    modifier: Modifier = Modifier
) {
    val maxSleep = points.mapNotNull { it.sleepMinutes }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val maxResponse = points.mapNotNull { it.averageResponseSec }.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegend("Sleep", MaterialTheme.colorScheme.primary)
            ChartLegend("Dismiss", DismissGreen)
        }
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            points.forEach { point ->
                val sleepRatio = point.sleepMinutes?.let { (it.toFloat() / maxSleep).coerceIn(0.08f, 1f) }
                val responseRatio = point.averageResponseSec?.let { (it.toFloat() / maxResponse).coerceIn(0.08f, 1f) }
                Column(
                    modifier = Modifier.width(34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        modifier = Modifier
                            .height(96.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        ChartBar(
                            ratio = sleepRatio,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        ChartBar(
                            ratio = responseRatio,
                            color = DismissGreen,
                            modifier = Modifier.width(11.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = point.date.format(CHART_DAY_FORMATTER),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepWakeFrictionChart(
    points: List<SleepWakeDayPoint>,
    modifier: Modifier = Modifier
) {
    val maxFriction = points
        .maxOfOrNull { maxOf(it.snoozeCount, it.challengeRetryCount) }
        ?.coerceAtLeast(1)
        ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegend("Snooze", SnoozeYellow)
            ChartLegend("Retry", AccentRed)
        }
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            points.forEach { point ->
                val snoozeRatio = point.snoozeCount.takeIf { it > 0 }
                    ?.let { (it.toFloat() / maxFriction).coerceIn(0.08f, 1f) }
                val retryRatio = point.challengeRetryCount.takeIf { it > 0 }
                    ?.let { (it.toFloat() / maxFriction).coerceIn(0.08f, 1f) }
                Column(
                    modifier = Modifier.width(34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        modifier = Modifier
                            .height(62.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        ChartBar(
                            ratio = snoozeRatio,
                            color = SnoozeYellow,
                            modifier = Modifier.width(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        ChartBar(
                            ratio = retryRatio,
                            color = AccentRed,
                            modifier = Modifier.width(11.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${point.snoozeCount}/${point.challengeRetryCount}",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartBar(
    ratio: Float?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(SurfaceCard.copy(alpha = 0.54f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        ratio?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(it)
                    .background(color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            )
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color, RoundedCornerShape(6.dp))
        )
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ActigraphyBucketsCard(
    sessions: List<ActigraphySession>,
    modifier: Modifier = Modifier
) {
    val latest = sessions.firstOrNull()
    AppSurfaceCard(modifier = modifier, highlighted = latest?.firedEarly == true) {
        AppSectionTitle(
            title = "Phone-motion buckets",
            description = if (latest == null) {
                "Smart alarm windows will save compact awake-motion, light-motion, and still-motion buckets here."
            } else {
                "Experimental phone-motion buckets from smart alarm monitoring. They are not medical sleep stages."
            }
        )

        if (latest == null) {
            AppEmptyState(
                icon = Icons.Default.BarChart,
                title = "No phone-motion sessions yet",
                description = "Enable a smart alarm window on an alarm to collect compact local sleep-motion summaries."
            )
            return@AppSurfaceCard
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = "${latest.totalMinutes} min",
                icon = Icons.Default.CalendarMonth,
                color = MaterialTheme.colorScheme.primary
            )
            AppStatusChip(
                label = if (latest.firedEarly) "Fired early" else "Reached target",
                icon = Icons.Default.CheckCircle,
                color = if (latest.firedEarly) DismissGreen else TextMuted
            )
            AppStatusChip(
                label = "Motion index ${"%.2f".format(latest.averageSleepIndex)}",
                icon = Icons.Default.BarChart,
                color = TextMuted
            )
            AppStatusChip(
                label = "Decision ${smartWakeDecisionLabel(latest.decisionReason)}",
                icon = Icons.Default.Search,
                color = if (latest.firedEarly) DismissGreen else TextMuted
            )
        }

        StageDistributionBar(session = latest)

        sessions.take(3).forEachIndexed { index, session ->
            ActigraphySessionRow(session = session)
            if (index != sessions.take(3).lastIndex) {
                HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))
            }
        }
    }
}

@Composable
private fun StageDistributionBar(session: ActigraphySession) {
    val total = session.totalMinutes.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(SurfaceCard.copy(alpha = 0.52f), RoundedCornerShape(10.dp))
        ) {
            StageSegment(session.awakeMinutes, total, AccentRed)
            StageSegment(session.lightMinutes, total, SnoozeYellow)
            StageSegment(session.deepMinutes, total, DismissGreen)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ChartLegend("Awake motion ${session.awakeMinutes}m", AccentRed)
            ChartLegend("Light motion ${session.lightMinutes}m", SnoozeYellow)
            ChartLegend("Still motion ${session.deepMinutes}m", DismissGreen)
        }
    }
}

@Composable
private fun RowScope.StageSegment(minutes: Int, total: Int, color: Color) {
    if (minutes <= 0) return
    Box(
        modifier = Modifier
            .weight(minutes.toFloat() / total)
            .fillMaxHeight()
            .background(color)
    )
}

@Composable
private fun ActigraphySessionRow(session: ActigraphySession) {
    val ended = remember(session.endedAt) {
        Instant.ofEpochMilli(session.endedAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.BarChart,
            // Outcome is otherwise conveyed by tint alone on this row.
            contentDescription = if (session.firedEarly) "Fired early" else "Reached target",
            tint = if (session.firedEarly) DismissGreen else TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ended,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "${session.awakeMinutes}m awake motion · ${session.lightMinutes}m light motion · ${session.deepMinutes}m still motion",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = smartWakeDecisionDetail(session),
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun smartWakeDecisionDetail(session: ActigraphySession): String {
    val observed = session.observedMinutesBeforeDecision.coerceAtLeast(0)
    return "Decision: ${smartWakeDecisionLabel(session.decisionReason)} after ${observed}m observed (${session.smartWakeMode.lowercase()})"
}

private fun smartWakeDecisionLabel(reason: String): String = when (reason) {
    "FIRE_LIGHT_MOTION" -> "light motion"
    "WAIT_INSUFFICIENT_DATA" -> "not enough data"
    "WAIT_TOO_ACTIVE" -> "too active"
    "WAIT_DEEP_OR_STILL" -> "still motion"
    "WAIT_LIGHT_NOT_STABLE" -> "unstable light motion"
    "WAIT_FINAL_MINUTE" -> "final minute"
    "WAIT_SERVICE_TIMEOUT" -> "service timeout"
    "REACHED_TARGET" -> "target time"
    "UNKNOWN" -> "unknown"
    else -> reason.lowercase().replace('_', ' ')
}

@Composable
private fun BreakdownRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(count.toString(), color = TextSecondary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DayOfWeekChart(counts: Map<DayOfWeek, Int>, modifier: Modifier = Modifier) {
    val maxCount = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        DayOfWeek.entries.forEach { day ->
            val count = counts[day] ?: 0
            val heightRatio = if (count == 0) 0.08f else count.toFloat() / maxCount

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = count.toString(),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight(heightRatio)
                        .background(
                            color = if (count > 0) MaterialTheme.colorScheme.primary else SurfaceCard,
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = day.name.take(3),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EventRow(event: AlarmEvent, is24Hour: Boolean) {
    val timeStr = remember(event.firedAt, is24Hour) {
        val pattern = if (is24Hour) "MMM d, HH:mm" else "MMM d, h:mm a"
        Instant.ofEpochMilli(event.firedAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(pattern))
    }

    val (actionIcon, actionColor, actionLabel) = when (event.action) {
        AlarmEvent.ACTION_DISMISSED -> Triple(Icons.Default.CheckCircle, DismissGreen, "Dismissed")
        AlarmEvent.ACTION_SNOOZED -> Triple(Icons.Default.Snooze, SnoozeYellow, "Snoozed")
        AlarmEvent.ACTION_SKIPPED -> Triple(Icons.Default.SkipNext, MaterialTheme.colorScheme.primary, "Skipped")
        AlarmEvent.ACTION_MISSED -> Triple(Icons.Default.ErrorOutline, AccentRed, "Missed")
        else -> Triple(Icons.Default.BarChart, TextMuted, "Alarm event")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = actionColor.copy(alpha = 0.14f)
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(actionIcon, contentDescription = actionLabel, tint = actionColor, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.alarmLabel.ifBlank { "Alarm" },
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = actionLabel,
                    color = actionColor
                )
                AppStatusChip(
                    label = timeStr,
                    icon = Icons.Default.CalendarMonth,
                    color = TextMuted
                )
            }
        }
        if (event.responseTimeMs > 0 || event.challengeRetryCount > 0) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (event.responseTimeMs > 0) {
                    AppStatusChip(
                        label = "${event.responseTimeMs / 1000}s",
                        color = TextMuted
                    )
                }
                if (event.challengeRetryCount > 0) {
                    AppStatusChip(
                        label = "${event.challengeRetryCount} retries",
                        color = AccentRed
                    )
                }
            }
        }
    }
}

private fun wakeStreakBadgeLabel(stats: AlarmStats): String {
    return if (stats.currentStreak > 0) {
        "${dayCountLabel(stats.currentStreak)} streak"
    } else {
        "Start streak"
    }
}

private fun wakeStreakStatus(stats: AlarmStats): String {
    return when {
        stats.currentStreak == 0 -> "Dismiss one alarm to start building a visible morning streak."
        stats.streakIncludesToday -> "Today's wake is already counted. Keep the chain going tomorrow."
        else -> "Still alive from yesterday. Dismiss today's alarm to protect it."
    }
}

private fun dayCountLabel(days: Int): String {
    return if (days == 1) "1 day" else "$days days"
}

private fun compactDays(days: Int): String = "${days}d"

private fun sleepWakeAnalyticsDescription(
    analytics: SleepWakeAnalytics,
    healthConnectEnabled: Boolean,
    sleepPermissionGranted: Boolean,
    hasSleepData: Boolean,
    hasWakeData: Boolean
): String {
    return when {
        analytics.hasSleepWakeCorrelation ->
            "Compares recent sleep duration with dismiss speed, snoozes, and challenge retries on this device."
        !healthConnectEnabled ->
            "Enable Health Connect to layer sleep duration over local wake behavior."
        !sleepPermissionGranted ->
            "Grant READ_SLEEP to add sleep duration; alarm response, snooze, and retry trends stay local."
        hasSleepData && !hasWakeData ->
            "Sleep sessions are ready. Alarm outcomes will add wake-behavior bars once they occur."
        !hasSleepData && hasWakeData ->
            "Wake-behavior bars are ready. Sleep bars appear after Health Connect returns recent sessions."
        else ->
            "Recent sleep sessions and alarm outcomes will build this chart locally."
    }
}

private fun formatSeconds(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

private fun formatSignedSeconds(seconds: Int): String = "+${formatSeconds(seconds)}"

private fun sleepScoreColor(score: Int): Color = when {
    score >= 85 -> DismissGreen
    score >= 70 -> SnoozeYellow
    else -> AccentRed
}

private fun formatSleepMinutes(minutes: Long?): String {
    val value = minutes ?: return "0m"
    val hours = value / 60
    val mins = value % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private val CHART_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("E")
