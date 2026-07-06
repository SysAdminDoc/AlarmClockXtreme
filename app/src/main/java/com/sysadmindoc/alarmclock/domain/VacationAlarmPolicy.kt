package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure vacation-mode scheduling rules.
 *
 * Vacation mode is intentionally scoped to recurring schedules: one-shot
 * alarms are often deliberate travel/reminder alarms and should not be
 * swallowed by a broad "time away" setting.
 */
object VacationAlarmPolicy {

    data class Adjustment(
        val triggerTime: Long,
        val skippedByVacation: Boolean
    )

    fun adjustTrigger(
        alarm: Alarm,
        initialTriggerTime: Long,
        settings: AppSettings,
        zone: ZoneId = ZoneId.systemDefault(),
        calculateFrom: (Alarm, ZonedDateTime) -> Long
    ): Adjustment {
        if (!alarm.isRecurringSchedule || !isInsideVacationWindow(settings, initialTriggerTime)) {
            return Adjustment(initialTriggerTime, skippedByVacation = false)
        }

        val resumeFrom = Instant.ofEpochMilli(settings.vacationEndMillis + 1L).atZone(zone)
        val resumedTrigger = calculateFrom(alarm, resumeFrom)
        return Adjustment(resumedTrigger, skippedByVacation = true)
    }

    fun isActive(settings: AppSettings, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return hasEnabledWindow(settings) && nowMillis in settings.vacationStartMillis..settings.vacationEndMillis
    }

    fun isInsideVacationWindow(settings: AppSettings, triggerTime: Long): Boolean {
        return hasEnabledWindow(settings) &&
            triggerTime in settings.vacationStartMillis..settings.vacationEndMillis
    }

    fun hasConfiguredWindow(settings: AppSettings): Boolean {
        return settings.vacationStartMillis > 0 &&
            settings.vacationEndMillis > settings.vacationStartMillis
    }

    private fun hasEnabledWindow(settings: AppSettings): Boolean {
        return settings.vacationModeEnabled && hasConfiguredWindow(settings)
    }
}
