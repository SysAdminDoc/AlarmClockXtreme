package com.sysadmindoc.alarmclock.service

import androidx.work.Data
import androidx.work.workDataOf
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.remote.WeatherCodes
import com.sysadmindoc.alarmclock.data.remote.WeatherResponse
import com.sysadmindoc.alarmclock.data.repository.CalendarEvent
import com.sysadmindoc.alarmclock.util.AlarmTimeFormatter
import com.sysadmindoc.alarmclock.worker.WakeConfirmWorker
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

internal data class MorningBriefingPayload(
    val time: String,
    val date: String,
    val weather: String = "",
    val nextEvent: String = "",
    val routine: String
)

internal data class WakeConfirmationPlan(
    val uniqueWorkName: String,
    val tag: String,
    val delayMinutes: Long,
    val inputData: Data
)

internal object AlarmPostDismissController {
    fun shouldShowMorningBriefing(settings: AppSettings): Boolean =
        settings.postDismissSummaryEnabled

    fun shouldSpeakMorningAnnouncement(alarm: Alarm): Boolean = alarm.ttsEnabled

    fun morningAnnouncementText(
        now: LocalTime = LocalTime.now(),
        today: LocalDate = LocalDate.now()
    ): String {
        val h = if (now.hour % 12 == 0) 12 else now.hour % 12
        val minStr = when {
            now.minute == 0 -> "o'clock"
            now.minute < 10 -> "oh ${now.minute}"
            else -> "${now.minute}"
        }
        val amPm = if (now.hour < 12) "A.M." else "P.M."
        val dayName = today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val monthName = today.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return "It is $h $minStr $amPm. Today is $dayName, $monthName ${today.dayOfMonth}."
    }

    fun morningBriefingPayload(
        alarm: Alarm,
        weather: String = "",
        nextEvent: String = "",
        now: LocalTime = LocalTime.now(),
        today: LocalDate = LocalDate.now(),
        is24Hour: Boolean = false
    ): MorningBriefingPayload {
        // Takes the preference for the same reason nextCalendarEventSummary
        // does: the header and the next-event line under it are one screen, and
        // this one used to hand-roll 12-hour time with literal AM/PM, so a
        // 24-hour phone read "7:05 AM" above "Standup, 07:30".
        val time = AlarmTimeFormatter.format(now.hour, now.minute, is24Hour)
        val date = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        return MorningBriefingPayload(
            time = time,
            date = date,
            weather = weather,
            nextEvent = nextEvent,
            routine = alarm.morningRoutine
        )
    }

    fun cachedWeatherSummary(weather: WeatherResponse?): String {
        val response = weather ?: return ""
        val current = response.current
        val daily = response.daily
        val parts = buildList {
            current?.weatherCode?.let { add(WeatherCodes.describe(it)) }
            current?.temperature?.let { temperature ->
                add("${temperature.roundToInt()}${response.currentUnits?.temperature.orEmpty()}")
            }
            val high = daily?.maxTemp?.firstOrNull()?.roundToInt()
            val low = daily?.minTemp?.firstOrNull()?.roundToInt()
            if (high != null && low != null) {
                add("high $high, low $low")
            }
            daily?.precipChance?.firstOrNull()?.takeIf { it > 0 }?.let {
                add("$it% precipitation")
            }
        }
        return parts.joinToString(" · ")
    }

    fun nextCalendarEventSummary(
        events: List<CalendarEvent>,
        nowMillis: Long = System.currentTimeMillis(),
        is24Hour: Boolean = false
    ): String {
        val next = events
            .asSequence()
            .filter { it.endTime >= nowMillis }
            .minByOrNull { it.startTime }
            ?: return ""
        val title = next.title.trim().take(80).ifBlank { "Calendar event" }
        return if (next.allDay) "$title · All day" else "$title · ${next.startFormatted(is24Hour)}"
    }

    fun shouldScheduleWakeConfirmation(alarm: Alarm): Boolean = alarm.wakeConfirmEnabled

    fun wakeConfirmationPlan(
        alarm: Alarm,
        fireId: String,
        scheduledAt: Long,
        refireCount: Int
    ): WakeConfirmationPlan {
        val delayMinutes = alarm.wakeConfirmDelayMinutes.coerceAtLeast(1).toLong()
        val data = workDataOf(
            WakeConfirmWorker.KEY_ALARM_ID to alarm.id,
            WakeConfirmWorker.KEY_ALARM_FIRE_ID to fireId,
            WakeConfirmWorker.KEY_SCHEDULED_AT to scheduledAt,
            WakeConfirmWorker.KEY_REFIRE_COUNT to refireCount
        )
        val name = "wake_confirm_${alarm.id}"
        return WakeConfirmationPlan(
            uniqueWorkName = name,
            tag = name,
            delayMinutes = delayMinutes,
            inputData = data
        )
    }
}
