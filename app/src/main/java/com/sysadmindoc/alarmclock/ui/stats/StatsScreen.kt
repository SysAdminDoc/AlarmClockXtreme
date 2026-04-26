package com.sysadmindoc.alarmclock.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.ui.components.AlarmClockHeroHeader
import com.sysadmindoc.alarmclock.ui.components.AppEmptyState
import com.sysadmindoc.alarmclock.ui.components.AppLoadingCard
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
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
                        label = "${stats.currentStreak} day streak",
                        icon = Icons.Default.LocalFireDepartment,
                        color = DismissGreen
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
                            value = "${stats.currentStreak}d",
                            color = DismissGreen,
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
                    shape = RoundedCornerShape(16.dp)
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
            shape = RoundedCornerShape(22.dp)
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
            shape = RoundedCornerShape(18.dp),
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
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(text) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = TextPrimary,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    )
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
private fun BreakdownRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(999.dp))
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
            shape = RoundedCornerShape(16.dp),
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
        if (event.responseTimeMs > 0) {
            AppStatusChip(
                label = "${event.responseTimeMs / 1000}s",
                color = TextMuted
            )
        }
    }
}
