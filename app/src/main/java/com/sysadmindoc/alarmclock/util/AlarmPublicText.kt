package com.sysadmindoc.alarmclock.util

object AlarmPublicText {
    const val GENERIC_ALARM_LABEL = "Alarm"
    const val GENERIC_NEXT_ALARM_LABEL = "Next alarm"

    fun requiredAlarmLabel(label: String, hideLabel: Boolean): String {
        return if (hideLabel) GENERIC_ALARM_LABEL else label.ifBlank { GENERIC_ALARM_LABEL }
    }

    fun optionalAlarmLabel(label: String, hideLabel: Boolean): String {
        return if (hideLabel) GENERIC_ALARM_LABEL else label
    }

    fun firingNotificationText(label: String, fallbackTime: String, hideLabel: Boolean): String {
        return if (hideLabel) GENERIC_ALARM_LABEL else label.ifBlank { fallbackTime }
    }

    fun wakeConfirmTitle(label: String, hideLabel: Boolean): String {
        return if (hideLabel || label.isBlank()) {
            "Are you awake?"
        } else {
            "Awake check: $label"
        }
    }

    fun quickSettingsSubtitle(label: String, hideLabel: Boolean): String {
        return if (hideLabel || label.isBlank()) GENERIC_NEXT_ALARM_LABEL else label
    }
}
