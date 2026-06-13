package com.sysadmindoc.alarmclock.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import android.text.format.DateFormat
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import com.sysadmindoc.alarmclock.util.AlarmPublicText
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Home screen widget showing next alarm time and countdown.
 * Uses Jetpack Glance for Compose-based widget rendering.
 *
 * Reuses the app's singleton Room database via Hilt's EntryPoint API so the
 * widget and the rest of the app share one connection (avoids duplicate
 * SQLite connections that historically caused stale data and corruption risk).
 */
class NextAlarmWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val alarmData = withContext(Dispatchers.IO) {
            loadNextAlarm(context)
        }

        provideContent {
            NextAlarmWidgetContent(alarmData)
        }
    }

    private suspend fun loadNextAlarm(context: Context): WidgetAlarmData? {
        return try {
            val ep = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AlarmClockApp.AppEntryPoint::class.java
            )
            val alarm = ep.alarmRepository().getNextAlarm()

            if (alarm != null && alarm.nextTriggerTime > System.currentTimeMillis()) {
                val hideLabel = ep.preferencesManager()
                    .getCurrentSettings()
                    .hideAlarmLabelsOnPublicSurfaces
                val calc = ep.nextAlarmCalculator()
                val remaining = calc.formatRemaining(alarm.nextTriggerTime)
                val triggerInstant = Instant.ofEpochMilli(alarm.nextTriggerTime)
                val localTime = triggerInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
                val is24Hour = DateFormat.is24HourFormat(context)
                val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
                val timeStr = localTime.format(DateTimeFormatter.ofPattern(timePattern))
                val dayStr = localTime.format(DateTimeFormatter.ofPattern("EEE"))

                WidgetAlarmData(
                    timeFormatted = timeStr,
                    dayFormatted = dayStr,
                    remaining = remaining,
                    label = AlarmPublicText.optionalAlarmLabel(alarm.label, hideLabel)
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

data class WidgetAlarmData(
    val timeFormatted: String,
    val dayFormatted: String,
    val remaining: String,
    val label: String
)

// Widget color constants (Glance uses its own color system)
private val WidgetBg = Color(0xFF0D1B2A)
private val WidgetCardBg = Color(0xFF152238)
private val WidgetAccent = Color(0xFF5B9EF4)
private val WidgetTextPrimary = Color(0xFFE8ECF0)
private val WidgetTextSecondary = Color(0xFF8A9BB5)
private val WidgetTextMuted = Color(0xFF4A5568)

@Composable
private fun NextAlarmWidgetContent(data: WidgetAlarmData?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBg)
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(
                Intent(LocalContext.current, MainActivity::class.java)
            )),
        contentAlignment = Alignment.CenterStart
    ) {
        if (data != null) {
            Column {
                // Day label
                Text(
                    text = data.dayFormatted,
                    style = TextStyle(
                        color = ColorProvider(WidgetAccent),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Time
                Text(
                    text = data.timeFormatted,
                    style = TextStyle(
                        color = ColorProvider(WidgetTextPrimary),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal
                    )
                )

                // Label
                if (data.label.isNotBlank()) {
                    Text(
                        text = data.label,
                        style = TextStyle(
                            color = ColorProvider(WidgetTextSecondary),
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Remaining
                Text(
                    text = data.remaining,
                    style = TextStyle(
                        color = ColorProvider(WidgetTextMuted),
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    text = "No alarms set",
                    style = TextStyle(
                        color = ColorProvider(WidgetTextMuted),
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}
