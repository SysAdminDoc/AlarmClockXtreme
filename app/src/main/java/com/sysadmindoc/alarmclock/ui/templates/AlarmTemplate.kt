package com.sysadmindoc.alarmclock.ui.templates

import java.time.DayOfWeek

/**
 * Predefined alarm configurations for quick setup.
 * Users can tap a template to pre-fill the alarm edit screen.
 */
data class AlarmTemplate(
    val name: String,
    val description: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<DayOfWeek>,
    val gradualVolumeSeconds: Int = 60,
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Int = 2,
    val snoozeDurationMinutes: Int = 10,
    val challengeType: String = "NONE"
)

val defaultTemplates = listOf(
    AlarmTemplate(
        name = "Early Bird",
        description = "5:30 AM weekdays, gentle wake",
        hour = 5, minute = 30,
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        gradualVolumeSeconds = 120,
        vibrationIntensity = 1
    ),
    AlarmTemplate(
        name = "Work Alarm",
        description = "7:00 AM weekdays, math challenge",
        hour = 7, minute = 0,
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        challengeType = "MATH_EASY"
    ),
    AlarmTemplate(
        name = "Weekend Sleep-In",
        description = "9:00 AM weekends, gentle",
        hour = 9, minute = 0,
        repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        gradualVolumeSeconds = 180,
        vibrationIntensity = 1,
        snoozeDurationMinutes = 15
    ),
    AlarmTemplate(
        name = "Power Nap",
        description = "20 min timer, shake to dismiss",
        hour = 0, minute = 20, // Interpreted as +20 mins from now
        repeatDays = emptySet(),
        gradualVolumeSeconds = 0,
        vibrationIntensity = 2,
        snoozeDurationMinutes = 5,
        challengeType = "SHAKE"
    ),
    AlarmTemplate(
        name = "Heavy Sleeper",
        description = "6:00 AM daily, hard math, max volume",
        hour = 6, minute = 0,
        repeatDays = DayOfWeek.entries.toSet(),
        gradualVolumeSeconds = 0,
        vibrationIntensity = 2,
        snoozeDurationMinutes = 5,
        challengeType = "MATH_HARD"
    ),
    AlarmTemplate(
        name = "Medication Reminder",
        description = "8:00 AM daily, gentle nudge",
        hour = 8, minute = 0,
        repeatDays = DayOfWeek.entries.toSet(),
        gradualVolumeSeconds = 90,
        vibrationIntensity = 1,
        snoozeDurationMinutes = 10
    )
)
