package com.sysadmindoc.alarmclock.service

import androidx.work.Data
import androidx.work.workDataOf
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.worker.WakeConfirmWorker
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
        now: LocalTime = LocalTime.now(),
        today: LocalDate = LocalDate.now()
    ): MorningBriefingPayload {
        val time = "${if (now.hour % 12 == 0) 12 else now.hour % 12}:${String.format("%02d", now.minute)} ${if (now.hour < 12) "AM" else "PM"}"
        val date = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        return MorningBriefingPayload(
            time = time,
            date = date,
            routine = alarm.morningRoutine
        )
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
