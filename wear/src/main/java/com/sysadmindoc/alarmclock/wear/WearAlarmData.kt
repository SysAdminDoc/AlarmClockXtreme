package com.sysadmindoc.alarmclock.wear

import android.content.Context
import androidx.core.content.edit
import com.google.android.gms.wearable.DataMap

object WearAlarmData {
    const val PATH_NEXT_ALARM = "/alarmclockxtreme/next_alarm"
    const val PATH_ACTION_SKIP = "/alarmclockxtreme/action/skip"
    const val PATH_ACTION_SNOOZE = "/alarmclockxtreme/action/snooze"
    const val PATH_ACTION_DISMISS = "/alarmclockxtreme/action/dismiss"

    const val KEY_HAS_ALARM = "has_alarm"
    const val KEY_ALARM_ID = "alarm_id"
    const val KEY_LABEL = "label"
    const val KEY_TIME_LABEL = "time_label"
    const val KEY_TRIGGER_TIME = "trigger_time"
    const val KEY_IS_FIRING = "is_firing"
    const val KEY_UPDATED_AT = "updated_at"
}

data class WearAlarmSnapshot(
    val hasAlarm: Boolean = false,
    val alarmId: Long = -1L,
    val label: String = "",
    val timeLabel: String = "",
    val triggerTime: Long = 0L,
    val isFiring: Boolean = false,
    val updatedAt: Long = 0L,
)

object WearAlarmStore {
    private const val PREFS = "wear_alarm_snapshot"

    fun load(context: Context): WearAlarmSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WearAlarmSnapshot(
            hasAlarm = prefs.getBoolean(WearAlarmData.KEY_HAS_ALARM, false),
            alarmId = prefs.getLong(WearAlarmData.KEY_ALARM_ID, -1L),
            label = prefs.getString(WearAlarmData.KEY_LABEL, "").orEmpty(),
            timeLabel = prefs.getString(WearAlarmData.KEY_TIME_LABEL, "").orEmpty(),
            triggerTime = prefs.getLong(WearAlarmData.KEY_TRIGGER_TIME, 0L),
            isFiring = prefs.getBoolean(WearAlarmData.KEY_IS_FIRING, false),
            updatedAt = prefs.getLong(WearAlarmData.KEY_UPDATED_AT, 0L),
        )
    }

    fun save(context: Context, snapshot: WearAlarmSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putBoolean(WearAlarmData.KEY_HAS_ALARM, snapshot.hasAlarm)
                putLong(WearAlarmData.KEY_ALARM_ID, snapshot.alarmId)
                putString(WearAlarmData.KEY_LABEL, snapshot.label)
                putString(WearAlarmData.KEY_TIME_LABEL, snapshot.timeLabel)
                putLong(WearAlarmData.KEY_TRIGGER_TIME, snapshot.triggerTime)
                putBoolean(WearAlarmData.KEY_IS_FIRING, snapshot.isFiring)
                putLong(WearAlarmData.KEY_UPDATED_AT, snapshot.updatedAt)
            }
    }

    fun fromDataMap(dataMap: DataMap): WearAlarmSnapshot {
        return WearAlarmSnapshot(
            hasAlarm = dataMap.getBoolean(WearAlarmData.KEY_HAS_ALARM, false),
            alarmId = dataMap.getLong(WearAlarmData.KEY_ALARM_ID, -1L),
            label = dataMap.getString(WearAlarmData.KEY_LABEL, "").orEmpty(),
            timeLabel = dataMap.getString(WearAlarmData.KEY_TIME_LABEL, "").orEmpty(),
            triggerTime = dataMap.getLong(WearAlarmData.KEY_TRIGGER_TIME, 0L),
            isFiring = dataMap.getBoolean(WearAlarmData.KEY_IS_FIRING, false),
            updatedAt = dataMap.getLong(WearAlarmData.KEY_UPDATED_AT, 0L),
        )
    }
}
