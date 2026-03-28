package com.sysadmindoc.alarmclock.ui.nightclock

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sysadmindoc.alarmclock.ui.theme.AccentBlue
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * v1.2.0: Night clock / bedside mode.
 * Full-screen always-on display showing time in large text.
 * Tap to exit. Keeps screen on at minimum brightness.
 */
class NightClockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.also { it.screenBrightness = 0.01f }

        setContent {
            NightClockScreen(onExit = { finish() })
        }
    }
}

@Composable
fun NightClockScreen(onExit: () -> Unit) {
    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            timeText = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d"))
            delay(1000)
        }
    }

    // Dim red text on pure black — minimal light emission
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onExit() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                fontSize = 96.sp,
                fontWeight = FontWeight.Thin,
                color = AccentBlue.copy(alpha = 0.7f)
            )
            Text(
                text = dateText,
                fontSize = 20.sp,
                color = TextMuted.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                "Tap to exit",
                fontSize = 12.sp,
                color = TextMuted.copy(alpha = 0.3f)
            )
        }
    }
}
