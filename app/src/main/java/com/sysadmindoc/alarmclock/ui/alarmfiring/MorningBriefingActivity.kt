package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.HeaderTop
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

/**
 * F12: Morning briefing screen shown after alarm dismiss.
 * Receives weather/calendar summary as intent extras and displays a "Good morning" card.
 */
class MorningBriefingActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TIME = "briefing_time"
        const val EXTRA_DATE = "briefing_date"
        const val EXTRA_WEATHER = "briefing_weather"
        const val EXTRA_NEXT_EVENT = "briefing_next_event"
        const val EXTRA_ROUTINE = "briefing_routine"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val time = intent.getStringExtra(EXTRA_TIME) ?: ""
        val date = intent.getStringExtra(EXTRA_DATE) ?: ""
        val weather = intent.getStringExtra(EXTRA_WEATHER) ?: ""
        val nextEvent = intent.getStringExtra(EXTRA_NEXT_EVENT) ?: ""
        val routine = intent.getStringExtra(EXTRA_ROUTINE) ?: ""

        setContent {
            AlarmClockXtremeTheme {
                MorningBriefingScreen(
                    time = time,
                    date = date,
                    weather = weather,
                    nextEvent = nextEvent,
                    morningRoutine = routine,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun MorningBriefingScreen(
    time: String,
    date: String,
    weather: String,
    nextEvent: String,
    morningRoutine: String = "",
    onClose: () -> Unit
) {
    val routineItems = morningRoutine.split("\n").mapNotNull { it.trim().takeIf(String::isNotBlank) }
    val hasWeather = weather.isNotBlank()
    val hasEvent = nextEvent.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        HeaderTop.copy(alpha = 0.92f),
                        SurfaceDark
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SnoozeYellow.copy(alpha = 0.18f),
                            androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = SnoozeYellow,
                        modifier = Modifier.size(68.dp)
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.briefing_good_morning),
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.briefing_re_clear_alarm_here_s),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (time.isNotBlank()) {
                        Text(
                            text = time,
                            color = TextPrimary,
                            style = MaterialTheme.typography.displayLarge
                        )
                    }
                    if (date.isNotBlank()) {
                        Text(
                            text = date,
                            color = TextSecondary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppStatusChip(
                                label = if (hasWeather) "Weather ready" else "No weather",
                                icon = Icons.Default.Cloud,
                                color = if (hasWeather) AccentBlue else TextMuted
                            )
                            AppStatusChip(
                                label = if (hasEvent) "Next event" else "Open schedule",
                                icon = Icons.Default.Event,
                                color = if (hasEvent) DismissGreen else TextMuted
                            )
                            if (routineItems.isNotEmpty()) {
                                AppStatusChip(
                                    label = "${routineItems.size} routine items",
                                    icon = Icons.Default.CheckCircleOutline,
                                    color = SnoozeYellow
                                )
                            }
                        }
                    }
                }
            }

            AppSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true
            ) {
                AppSectionTitle(
                    title = stringResource(R.string.briefing_morning_briefing),
                    description = stringResource(R.string.briefing_quick_snapshot_start_moving_context)
                )
                if (weather.isNotBlank()) {
                    BriefingRow(
                        icon = Icons.Default.Cloud,
                        tint = AccentBlue,
                        text = weather
                    )
                }
                if (nextEvent.isNotBlank()) {
                    BriefingRow(
                        icon = Icons.Default.Event,
                        tint = DismissGreen,
                        text = nextEvent
                    )
                }
                if (weather.isBlank() && nextEvent.isBlank()) {
                    Text(
                        text = stringResource(R.string.briefing_nothing_urgent_queued_right_now),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (routineItems.isNotEmpty()) {
                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    AppSectionTitle(
                        title = stringResource(R.string.briefing_morning_routine),
                        description = stringResource(R.string.briefing_short_checklist_get_momentum_without)
                    )
                    routineItems.forEachIndexed { index, item ->
                        RoutineRow(
                            index = index + 1,
                            text = item
                        )
                    }
                }
            }

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DismissGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.briefing_close_briefing),
                    fontWeight = FontWeight.SemiBold
                )
            }

        }
    }
}

@Composable
private fun BriefingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun RoutineRow(
    index: Int,
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DismissGreen.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppStatusChip(
                label = index.toString(),
                color = DismissGreen
            )
            Text(
                text = text,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
