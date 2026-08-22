package com.sysadmindoc.alarmclock.ui.templates

import com.sysadmindoc.alarmclock.R
import androidx.annotation.StringRes
import java.time.DayOfWeek

/**
 * Predefined alarm configurations for quick setup.
 * Users can tap a template to pre-fill the alarm edit screen.
 */
data class AlarmTemplate(
    val key: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
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
        key = "early_bird",
        nameRes = R.string.template_early_bird_name,
        descriptionRes = R.string.template_early_bird_description,
        hour = 5, minute = 30,
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        gradualVolumeSeconds = 120,
        vibrationIntensity = 1
    ),
    AlarmTemplate(
        key = "work_alarm",
        nameRes = R.string.template_work_alarm_name,
        descriptionRes = R.string.template_work_alarm_description,
        hour = 7, minute = 0,
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        challengeType = "MATH_EASY"
    ),
    AlarmTemplate(
        key = "weekend_sleep_in",
        nameRes = R.string.template_weekend_sleep_in_name,
        descriptionRes = R.string.template_weekend_sleep_in_description,
        hour = 9, minute = 0,
        repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        gradualVolumeSeconds = 180,
        vibrationIntensity = 1,
        snoozeDurationMinutes = 15
    ),
    AlarmTemplate(
        key = "power_nap",
        nameRes = R.string.template_power_nap_name,
        descriptionRes = R.string.template_power_nap_description,
        hour = 0, minute = 20, // Interpreted as +20 mins from now
        repeatDays = emptySet(),
        gradualVolumeSeconds = 0,
        vibrationIntensity = 2,
        snoozeDurationMinutes = 5,
        challengeType = "SHAKE"
    ),
    AlarmTemplate(
        key = "heavy_sleeper",
        nameRes = R.string.template_heavy_sleeper_name,
        descriptionRes = R.string.template_heavy_sleeper_description,
        hour = 6, minute = 0,
        repeatDays = DayOfWeek.entries.toSet(),
        gradualVolumeSeconds = 0,
        vibrationIntensity = 2,
        snoozeDurationMinutes = 5,
        challengeType = "MATH_HARD"
    ),
    AlarmTemplate(
        key = "medication_reminder",
        nameRes = R.string.template_medication_reminder_name,
        descriptionRes = R.string.template_medication_reminder_description,
        hour = 8, minute = 0,
        repeatDays = DayOfWeek.entries.toSet(),
        gradualVolumeSeconds = 90,
        vibrationIntensity = 1,
        snoozeDurationMinutes = 10
    )
)
