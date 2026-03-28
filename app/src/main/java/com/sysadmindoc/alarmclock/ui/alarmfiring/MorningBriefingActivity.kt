package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sysadmindoc.alarmclock.ui.theme.*

/**
 * F12: Morning briefing screen shown after alarm dismiss.
 * Receives weather/calendar summary as intent extras and displays a "Good morning" card.
 * Tap anywhere to close.
 */
class MorningBriefingActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TIME = "briefing_time"
        const val EXTRA_DATE = "briefing_date"
        const val EXTRA_WEATHER = "briefing_weather"
        const val EXTRA_NEXT_EVENT = "briefing_next_event"
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

        setContent {
            com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme {
                MorningBriefingScreen(
                    time = time,
                    date = date,
                    weather = weather,
                    nextEvent = nextEvent,
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
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(HeaderTop, SurfaceDark))
            )
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.WbSunny,
                contentDescription = null,
                tint = SnoozeYellow,
                modifier = Modifier.size(64.dp)
            )

            Text(
                "Good Morning",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary
            )

            if (time.isNotBlank()) {
                Text(time, fontSize = 56.sp, fontWeight = FontWeight.Thin, color = TextPrimary)
            }

            if (date.isNotBlank()) {
                Text(date, fontSize = 16.sp, color = TextSecondary)
            }

            if (weather.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cloud, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(weather, color = TextPrimary, fontSize = 15.sp)
                    }
                }
            }

            if (nextEvent.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, null, tint = DismissGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(nextEvent, color = TextPrimary, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap anywhere to continue", color = TextMuted, fontSize = 13.sp)
        }
    }
}
