package com.sysadmindoc.alarmclock.service

import android.content.Context
import android.content.Intent

object AlarmBroadcastContract {
    private const val PREFIX = "com.sysadmindoc.alarmclock.action"
    const val ACTION_ALARM_FIRED = "$PREFIX.ALARM_FIRED"
    const val ACTION_ALARM_SNOOZED = "$PREFIX.ALARM_SNOOZED"
    const val ACTION_ALARM_DISMISSED = "$PREFIX.ALARM_DISMISSED"
    const val ACTION_ALARM_MISSED = "$PREFIX.ALARM_MISSED"
    const val ACTION_ALARM_SKIPPED = "$PREFIX.ALARM_SKIPPED"

    const val EXTRA_ALARM_ID = "alarmId"
    const val EXTRA_LABEL = "label"
    const val EXTRA_DISPLAY_TIME = "displayTime"
    const val EXTRA_OCCURRED_AT = "occurredAt"
    const val EXTRA_FIRE_ID = "fireId"

    fun send(
        context: Context,
        action: String,
        alarmId: Long,
        label: String = "",
        displayTime: String = "",
        fireId: String = ""
    ) {
        val intent = Intent(action).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_DISPLAY_TIME, displayTime)
            putExtra(EXTRA_OCCURRED_AT, System.currentTimeMillis())
            if (fireId.isNotBlank()) putExtra(EXTRA_FIRE_ID, fireId)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
}
