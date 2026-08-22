package com.sysadmindoc.alarmclock.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import com.sysadmindoc.alarmclock.util.AlarmTimeFormatter
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
        // A database failure used to render as "No alarms set", which reads as
        // "you have none" rather than "the widget could not check".
        val load = withContext(Dispatchers.IO) {
            runCatching { loadNextAlarm(context) }
        }
        // A cancelled widget update is not a load failure; let it propagate
        // rather than render "Couldn't load alarms".
        load.exceptionOrNull()?.let { if (it is kotlinx.coroutines.CancellationException) throw it }

        provideContent {
            NextAlarmWidgetContent(
                data = load.getOrNull(),
                failedToLoad = load.isFailure
            )
        }
    }

    private suspend fun loadNextAlarm(context: Context): WidgetAlarmData? {
        return run {
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
                val timeStr = localTime.format(AlarmTimeFormatter.formatter(is24Hour))
                val dayStr = localTime.format(DateTimeFormatter.ofPattern("EEE"))

                WidgetAlarmData(
                    alarmId = alarm.id,
                    triggerTimeMs = alarm.nextTriggerTime,
                    timeFormatted = timeStr,
                    dayFormatted = dayStr,
                    remaining = remaining,
                    label = AlarmPublicText.optionalAlarmLabel(alarm.label, hideLabel),
                    fixedTimezoneId = alarm.fixedTimezoneId.takeIf { alarm.usesFixedTimezone }.orEmpty()
                )
            } else null
        }
    }
}

data class WidgetAlarmData(
    val alarmId: Long,
    val triggerTimeMs: Long,
    val timeFormatted: String,
    val dayFormatted: String,
    val remaining: String,
    val label: String,
    val fixedTimezoneId: String = ""
) {
    val isWithin24Hours: Boolean
        get() = triggerTimeMs - System.currentTimeMillis() in 1..24 * 60 * 60 * 1000L
}

private val AdjustMinutesKey = ActionParameters.Key<Int>("adjust_minutes")

class AdjustAlarmAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val minutes = parameters[AdjustMinutesKey] ?: return
        val deltaMs = minutes * 60_000L
        try {
            val ep = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AlarmClockApp.AppEntryPoint::class.java
            )
            val alarm = ep.alarmRepository().getNextAlarm() ?: return
            if (alarm.nextTriggerTime <= System.currentTimeMillis()) return
            val newTrigger = alarm.nextTriggerTime + deltaMs
            if (newTrigger <= System.currentTimeMillis()) return
            ep.alarmScheduler().scheduleAt(alarm, newTrigger)
        } catch (_: Exception) {
            // Non-fatal — widget tap should never crash
        }
        NextAlarmWidget().update(context, glanceId)
    }
}

// Glance has its own color system, so the app's theme tokens are mirrored here
// by value. They had drifted to an older palette, and WidgetTextMuted measured
// about 2.3:1 on the background, well under the 4.5:1 body-text bar.
private val WidgetBg = Color(0xFF070B11)        // SurfaceDark
private val WidgetCardBg = Color(0xFF15202E)    // SurfaceCard
private val WidgetAccent = Color(0xFF6FB7FF)    // BluePrimary
private val WidgetTextPrimary = Color(0xFFF1F5FB)   // TextPrimary
private val WidgetTextSecondary = Color(0xFFA9BED8) // TextSecondary
private val WidgetTextMuted = Color(0xFF7E93AE)     // TextMuted
private val WidgetWarning = Color(0xFFF5C96B)       // SnoozeYellow

@Composable
private fun NextAlarmWidgetContent(data: WidgetAlarmData?, failedToLoad: Boolean = false) {
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
                if (data.fixedTimezoneId.isNotBlank()) {
                    Text(
                        text = data.fixedTimezoneId.replace('_', ' '),
                        style = TextStyle(
                            color = ColorProvider(WidgetAccent),
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
                        fontSize = 12.sp
                    )
                )

                if (data.isWithin24Hours) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Row(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = " -10m ",
                            style = TextStyle(
                                color = ColorProvider(WidgetAccent),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier
                                .background(WidgetCardBg)
                                .cornerRadius(6.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .clickable(actionRunCallback<AdjustAlarmAction>(
                                    actionParametersOf(AdjustMinutesKey to -10)
                                ))
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = " +10m ",
                            style = TextStyle(
                                color = ColorProvider(WidgetAccent),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier
                                .background(WidgetCardBg)
                                .cornerRadius(6.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .clickable(actionRunCallback<AdjustAlarmAction>(
                                    actionParametersOf(AdjustMinutesKey to 10)
                                ))
                        )
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    text = if (failedToLoad) "Couldn't load alarms" else "No alarms set",
                    style = TextStyle(
                        color = ColorProvider(
                            if (failedToLoad) WidgetWarning else WidgetTextMuted
                        ),
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}
