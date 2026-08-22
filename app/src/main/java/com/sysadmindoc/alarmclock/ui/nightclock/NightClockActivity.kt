package com.sysadmindoc.alarmclock.ui.nightclock

import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import com.sysadmindoc.alarmclock.ui.theme.BlueLight
import com.sysadmindoc.alarmclock.ui.theme.NightClockBottom
import com.sysadmindoc.alarmclock.ui.theme.NightClockHint
import com.sysadmindoc.alarmclock.ui.theme.NightClockPanel
import com.sysadmindoc.alarmclock.ui.theme.NightClockTop
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.LocalMotionEnabled
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

/**
 * v1.2.0: Night clock / bedside mode.
 * Full-screen always-on display showing time in large text.
 * Long-press to exit. Keeps screen on at minimum brightness.
 */
@AndroidEntryPoint
class NightClockActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.also { it.screenBrightness = 0.01f }

        setContent {
            val settings by preferencesManager.settings.collectAsStateWithLifecycle(AppSettings())
            AlarmClockXtremeTheme(reduceMotionAndFlashing = settings.reduceMotionAndFlashing) {
                NightClockScreen(onExit = { finish() })
            }
        }
    }
}

@Composable
fun NightClockScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val timePattern = if (is24Hour) "HH:mm" else "h:mm"
    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    val currentDate by produceState(initialValue = LocalDate.now()) {
        while (true) {
            value = LocalDate.now()
            delay(60_000)
        }
    }
    val motionEnabled = LocalMotionEnabled.current
    val glowAlpha = if (motionEnabled) {
        rememberInfiniteTransition(label = stringResource(R.string.night_clock_nightambient)).animateFloat(
            initialValue = 0.18f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        ).value
    } else {
        0.24f
    }
    val amPm = if (is24Hour) "" else currentTime.format(DateTimeFormatter.ofPattern("a"))

    // Burn-in drift is a hardware safeguard (imperceptibly slow pixel shifting), not
    // decorative motion, so it deliberately ignores the reduce-motion gate. Only the
    // glow pulse above stays gated behind motionEnabled.
    val driftTransition = rememberInfiniteTransition(label = "burnInDrift")
    val driftX = driftTransition.animateFloat(
        initialValue = -24f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftX"
    ).value
    val driftY = driftTransition.animateFloat(
        initialValue = 18f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftY"
    ).value

    // Resolved outside the semantics lambda, which is not a composable scope.
    val screenLabel = stringResource(R.string.settings_night_clock)
    val exitLabel = stringResource(R.string.night_clock_exit_night_clock)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NightClockTop,
                        NightClockBottom
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onExit() })
            }
            // A raw long-press gesture is invisible to a screen reader, so the
            // only way out of night clock had no accessible equivalent.
            .semantics {
                contentDescription = screenLabel
                onLongClick(label = exitLabel) {
                    onExit()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SnoozeYellow.copy(alpha = glowAlpha),
                            BlueLight.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(driftX.dp.roundToPx(), driftY.dp.roundToPx()) }
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SnoozeYellow.copy(alpha = 0.09f)
            ) {
                Text(
                    text = stringResource(R.string.night_clock_bedside_mode),
                    color = SnoozeYellow.copy(alpha = 0.74f),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTime.format(DateTimeFormatter.ofPattern(timePattern)),
                        color = TextPrimary.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Light
                    )
                    if (amPm.isNotBlank()) {
                        Text(
                            text = amPm,
                            color = SnoozeYellow.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 10.dp, bottom = 10.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NightClockPanel
                ) {
                    Text(
                        text = currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        color = TextMuted.copy(alpha = 0.74f),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.36f)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SnoozeYellow.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NightClockPanel
                ) {
                    Text(
                        text = stringResource(R.string.night_clock_long_press_anywhere_exit),
                        color = NightClockHint,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
