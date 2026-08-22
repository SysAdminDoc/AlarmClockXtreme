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
import java.time.format.TextStyle
import java.util.Locale
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

    /**
     * The sentence the alarm speaks after dismissal.
     *
     * The day and month came from `DayOfWeek.name` and `Month.name`, which are
     * the English enum constants, so this was English even on a phone that had
     * never seen an English string anywhere else. They come from the locale's
     * own full-form names now, the clock time from the shared formatter, and
     * the sentence from a resource so a translation can reorder it.
     *
     * @param spokenTime a clock reading a text-to-speech engine will say
     * naturally, which is why the caller passes the 12-hour form.
     */
    fun morningAnnouncementText(
        template: String,
        spokenTime: String,
        now: LocalTime = LocalTime.now(),
        today: LocalDate = LocalDate.now()
    ): String {
        val locale = Locale.getDefault()
        val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val monthName = today.month.getDisplayName(TextStyle.FULL, locale)
        return String.format(locale, template, spokenTime, dayName, monthName, today.dayOfMonth)
    }

    fun morningBriefingPayload(
        alarm: Alarm,
        weather: String = "",
        nextEvent: String = "",
        now: LocalTime = LocalTime.now(),
        today: LocalDate = LocalDate.now(),
        is24Hour: Boolean
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

    /**
     * @param describeCode turns a WMO weather code into a phrase. Passed in
     * because WeatherCodes hands back a resource id and this object has no
     * Context to resolve one with.
     */
    fun cachedWeatherSummary(
        weather: WeatherResponse?,
        describeCode: (Int) -> String
    ): String {
        val response = weather ?: return ""
        val current = response.current
        val daily = response.daily
        val parts = buildList {
            current?.weatherCode?.let { add(describeCode(it)) }
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

    /**
     * @param untitledLabel what an event with no title is called
     * @param allDayTemplate "%1$s · All day"
     * @param atTemplate "%1$s · %2$s"
     *
     * Three strings in rather than a Context, because this object is a pure
     * function under unit test and the caller is a service that has one.
     */
    fun nextCalendarEventSummary(
        events: List<CalendarEvent>,
        untitledLabel: String,
        allDayTemplate: String,
        atTemplate: String,
        nowMillis: Long = System.currentTimeMillis(),
        is24Hour: Boolean
    ): String {
        val next = events
            .asSequence()
            .filter { it.endTime >= nowMillis }
            .minByOrNull { it.startTime }
            ?: return ""
        val title = next.title.trim().take(80).ifBlank { untitledLabel }
        return if (next.allDay) {
            String.format(allDayTemplate, title)
        } else {
            String.format(atTemplate, title, next.startFormatted(is24Hour))
        }
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
