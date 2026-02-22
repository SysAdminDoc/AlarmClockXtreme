package com.sysadmindoc.alarmclock.ui.bedtime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedtimeScreen(
    viewModel: BedtimeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text("Bedtime", style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Sleep goal & reminder", color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sleep arc visualization
        SleepArc(
            bedtimeHour = state.bedtimeHour,
            bedtimeMinute = state.bedtimeMinute,
            sleepHours = state.sleepGoalHours,
            sleepMinutes = state.sleepGoalMinutes,
            modifier = Modifier.size(240.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sleep duration label
        Text(state.sleepDurationFormatted, fontSize = 32.sp, fontWeight = FontWeight.Light, color = TextPrimary)
        Text("sleep goal", color = TextMuted, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        // Enable toggle
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isEnabled) AccentBlue.copy(alpha = 0.1f) else SurfaceMedium
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Bedtime, null, tint = if (state.isEnabled) AccentBlue else TextMuted)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bedtime Reminder", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        if (state.isEnabled) "Reminder ${state.reminderMinutesBefore}m before bedtime"
                        else "Get notified when it's time to sleep",
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = viewModel::toggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentBlue,
                        checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bedtime & wake info
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bedtime card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                onClick = { showTimePicker = true }
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NightsStay, null, tint = BlueLight, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Bedtime", color = TextMuted, fontSize = 12.sp)
                    Text(state.bedtimeFormatted, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Wake time card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.WbSunny, null, tint = SnoozeYellow, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Wake up", color = TextMuted, fontSize = 12.sp)
                    Text(
                        state.wakeTimeFormatted.ifBlank { "--:--" },
                        color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Suggested bedtime
        if (state.suggestedBedtime.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DismissGreen.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, null, tint = DismissGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Suggested bedtime", color = DismissGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Go to bed by ${state.suggestedBedtime} to get ${state.sleepDurationFormatted} of sleep",
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Sleep goal adjuster
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMedium)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sleep Goal", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val total = state.sleepGoalHours * 60 + state.sleepGoalMinutes - 30
                        if (total >= 300) viewModel.updateSleepGoal(total / 60, total % 60) // Min 5h
                    }) {
                        Icon(Icons.Default.Remove, null, tint = TextSecondary)
                    }

                    Text(
                        state.sleepDurationFormatted,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    IconButton(onClick = {
                        val total = state.sleepGoalHours * 60 + state.sleepGoalMinutes + 30
                        if (total <= 720) viewModel.updateSleepGoal(total / 60, total % 60) // Max 12h
                    }) {
                        Icon(Icons.Default.Add, null, tint = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Bedtime picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.bedtimeHour,
            initialMinute = state.bedtimeMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateBedtime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = TextSecondary) }
            },
            text = {
                TimePicker(state = timePickerState,
                    colors = TimePickerDefaults.colors(containerColor = SurfaceMedium))
            },
            containerColor = SurfaceMedium
        )
    }
}

/**
 * Circular sleep arc showing bedtime-to-wake span on a 12-hour clock face.
 */
@Composable
private fun SleepArc(
    bedtimeHour: Int,
    bedtimeMinute: Int,
    sleepHours: Int,
    sleepMinutes: Int,
    modifier: Modifier = Modifier
) {
    val sleepTotalMinutes = sleepHours * 60f + sleepMinutes
    val bedtimeTotalMinutes = bedtimeHour * 60f + bedtimeMinute

    // Convert to 12-hour clock angles (0 = top, clockwise)
    val startAngle = ((bedtimeTotalMinutes % 720f) / 720f) * 360f - 90f
    val sweepAngle = (sleepTotalMinutes / 720f) * 360f

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 20f

        // Background track
        drawCircle(
            color = SurfaceCard,
            radius = radius,
            center = center,
            style = Stroke(width = 12f)
        )

        // Sleep arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(BlueLight, AccentBlue, BlueLight),
                center = center
            ),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 14f, cap = StrokeCap.Round)
        )

        // 12 hour markers
        for (i in 0 until 12) {
            val angle = (i / 12f) * 2 * PI - PI / 2
            val inner = radius - 16f
            val outer = radius - 6f
            drawLine(
                color = TextMuted,
                start = Offset(
                    center.x + (inner * cos(angle)).toFloat(),
                    center.y + (inner * sin(angle)).toFloat()
                ),
                end = Offset(
                    center.x + (outer * cos(angle)).toFloat(),
                    center.y + (outer * sin(angle)).toFloat()
                ),
                strokeWidth = if (i % 3 == 0) 3f else 1.5f
            )
        }

        // Bedtime dot
        val bedAngleRad = (startAngle + 90) * PI / 180
        drawCircle(
            color = BlueLight,
            radius = 8f,
            center = Offset(
                center.x + (radius * cos(bedAngleRad - PI / 2)).toFloat(),
                center.y + (radius * sin(bedAngleRad - PI / 2)).toFloat()
            )
        )

        // Wake dot
        val wakeAngleRad = (startAngle + sweepAngle + 90) * PI / 180
        drawCircle(
            color = SnoozeYellow,
            radius = 8f,
            center = Offset(
                center.x + (radius * cos(wakeAngleRad - PI / 2)).toFloat(),
                center.y + (radius * sin(wakeAngleRad - PI / 2)).toFloat()
            )
        )
    }
}
