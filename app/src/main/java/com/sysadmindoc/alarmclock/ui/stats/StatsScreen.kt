package com.sysadmindoc.alarmclock.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.ui.theme.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = state.stats

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text("Statistics", style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Your alarm habits", color = TextSecondary)
                }
            }
        }

        // Summary cards row
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard("Streak", "${stats.currentStreak}d", DismissGreen, Icons.Default.LocalFireDepartment, Modifier.weight(1f))
                StatMiniCard("This Week", "${stats.alarmsThisWeek}", AccentBlue, Icons.Default.DateRange, Modifier.weight(1f))
                StatMiniCard("Snooze", "${stats.snoozeRate}%", SnoozeYellow, Icons.Default.Snooze, Modifier.weight(1f))
            }
        }

        // Response time card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Average Wake-Up Time", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        val mins = stats.averageDismissTimeSec / 60
                        val secs = stats.averageDismissTimeSec % 60
                        Text(
                            if (mins > 0) "${mins}m ${secs}s" else "${secs}s",
                            fontSize = 36.sp, fontWeight = FontWeight.Light, color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("to dismiss", color = TextMuted, fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
        }

        // Breakdown card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Breakdown", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    BreakdownRow("Dismissed", stats.totalDismissed, DismissGreen)
                    BreakdownRow("Snoozed", stats.totalSnoozed, SnoozeYellow)
                    BreakdownRow("Skipped", stats.totalSkipped, AccentBlue)
                    BreakdownRow("Missed", stats.totalMissed, AccentRed)
                }
            }
        }

        // Day of week chart
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alarms by Day", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    DayOfWeekChart(stats.dayOfWeekCounts, modifier = Modifier.fillMaxWidth().height(100.dp))
                }
            }
        }

        // Recent history
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Recent History",
                color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        if (state.recentEvents.isEmpty()) {
            item {
                Text(
                    "No alarm events recorded yet",
                    color = TextMuted, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        } else {
            items(state.recentEvents) { event ->
                EventRow(event)
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    label: String, value: String, color: Color, icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
private fun BreakdownRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(count.toString(), color = TextSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayOfWeekChart(counts: Map<DayOfWeek, Int>, modifier: Modifier = Modifier) {
    val maxCount = counts.values.maxOrNull() ?: 1

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        DayOfWeek.entries.forEach { day ->
            val count = counts[day] ?: 0
            val heightRatio = if (maxCount > 0) count.toFloat() / maxCount else 0f

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bar
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight(heightRatio.coerceAtLeast(0.05f))
                        .background(AccentBlue, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    day.name.take(1),
                    fontSize = 10.sp, color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun EventRow(event: AlarmEvent) {
    val timeStr = remember(event.firedAt) {
        Instant.ofEpochMilli(event.firedAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
    }

    val (actionIcon, actionColor) = when (event.action) {
        AlarmEvent.ACTION_DISMISSED -> Icons.Default.CheckCircle to DismissGreen
        AlarmEvent.ACTION_SNOOZED -> Icons.Default.Snooze to SnoozeYellow
        AlarmEvent.ACTION_SKIPPED -> Icons.Default.SkipNext to AccentBlue
        AlarmEvent.ACTION_MISSED -> Icons.Default.ErrorOutline to AccentRed
        else -> Icons.Default.Alarm to TextMuted
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(actionIcon, null, tint = actionColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.alarmLabel.ifBlank { "Alarm" },
                color = TextPrimary, fontSize = 14.sp
            )
            Text(timeStr, color = TextMuted, fontSize = 11.sp)
        }
        if (event.responseTimeMs > 0) {
            val sec = event.responseTimeMs / 1000
            Text("${sec}s", color = TextSecondary, fontSize = 12.sp)
        }
    }
}
