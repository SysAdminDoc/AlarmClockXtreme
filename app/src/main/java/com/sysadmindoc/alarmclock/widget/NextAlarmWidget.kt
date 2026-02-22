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
import androidx.room.Room
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.data.local.AlarmDatabase
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Home screen widget showing next alarm time and countdown.
 * Uses Jetpack Glance for Compose-based widget rendering.
 *
 * Note: Glance widgets cannot use Hilt injection, so we access
 * Room directly here.
 */
class NextAlarmWidget : GlanceAppWidget() {

    companion object {
        @Volatile
        private var dbInstance: AlarmDatabase? = null

        private fun getDatabase(context: Context): AlarmDatabase {
            return dbInstance ?: synchronized(this) {
                dbInstance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_clock.db"
                )
                    .addMigrations(AlarmDatabase.MIGRATION_1_2, AlarmDatabase.MIGRATION_2_3)
                    .allowMainThreadQueries()
                    .build()
                    .also { dbInstance = it }
            }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val alarmData = withContext(Dispatchers.IO) {
            loadNextAlarm(context)
        }

        provideContent {
            NextAlarmWidgetContent(alarmData)
        }
    }

    private fun loadNextAlarm(context: Context): WidgetAlarmData? {
        return try {
            val db = getDatabase(context)
            val alarm = db.alarmDao().getNextAlarmSync()

            if (alarm != null && alarm.nextTriggerTime > System.currentTimeMillis()) {
                val calc = NextAlarmCalculator()
                val remaining = calc.formatRemaining(alarm.nextTriggerTime)
                val triggerInstant = Instant.ofEpochMilli(alarm.nextTriggerTime)
                val localTime = triggerInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
                val timeStr = localTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                val dayStr = localTime.format(DateTimeFormatter.ofPattern("EEE"))

                WidgetAlarmData(
                    timeFormatted = timeStr,
                    dayFormatted = dayStr,
                    remaining = remaining,
                    label = alarm.label
                )
            } else null
        } catch (e: Exception) {
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
