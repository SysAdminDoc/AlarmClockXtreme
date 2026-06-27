package com.sysadmindoc.alarmclock.directboot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import com.sysadmindoc.alarmclock.data.model.Alarm
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DirectBootAlarmCache {
    private const val TAG = "DirectBootAlarmCache"
    private const val PREFS_NAME = "direct_boot_next_alarm"
    private const val KEY_SCHEMA_VERSION = "schema_version"
    private const val KEY_ALARM_ID = "alarm_id"
    private const val KEY_TRIGGER_TIME = "trigger_time"
    private const val KEY_LABEL = "label"
    private const val KEY_TIME_LABEL = "time_label"
    private const val KEY_PLAY_DEFAULT_SOUND = "play_default_sound"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_FIRED_ALARM_ID = "fired_alarm_id"
    private const val KEY_FIRED_TRIGGER_TIME = "fired_trigger_time"
    private const val KEY_FIRED_AT = "fired_at"
    private const val DIRECT_BOOT_SHOW_REQUEST_CODE = 91_700

    fun saveIfEarlier(
        context: Context,
        alarm: Alarm,
        triggerTime: Long,
        now: Long = System.currentTimeMillis()
    ) {
        if (triggerTime <= now || alarm.id <= 0L) return

        val snapshot = DirectBootAlarmSnapshot.fromAlarm(
            alarm = alarm,
            triggerTime = triggerTime,
            timeLabel = formatTriggerTime(context, triggerTime),
            now = now
        )
        val current = read(context)
        if (current == null ||
            !current.isSchedulable(now) ||
            current.alarmId == snapshot.alarmId ||
            snapshot.triggerTime < current.triggerTime
        ) {
            write(context, snapshot)
        }
    }

    fun clear(context: Context) {
        prefs(context).edit()
            .remove(KEY_SCHEMA_VERSION)
            .remove(KEY_ALARM_ID)
            .remove(KEY_TRIGGER_TIME)
            .remove(KEY_LABEL)
            .remove(KEY_TIME_LABEL)
            .remove(KEY_PLAY_DEFAULT_SOUND)
            .remove(KEY_VIBRATION_ENABLED)
            .remove(KEY_UPDATED_AT)
            .apply()
    }

    fun removeIfMatches(context: Context, alarmId: Long) {
        val current = read(context)
        if (current?.alarmId == alarmId) {
            cancelScheduledFallback(context, alarmId)
            clear(context)
        }
    }

    fun scheduleCachedAlarm(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val snapshot = read(context)
        if (snapshot?.isSchedulable(now) != true) {
            clear(context)
            return false
        }

        return try {
            scheduleFallback(context, snapshot)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule direct-boot fallback alarm", e)
            false
        }
    }

    fun cancelScheduledFallback(context: Context, alarmId: Long) {
        val protectedContext = protectedContext(context)
        val pendingIntent = DirectBootAlarmReceiver.pendingIntent(
            context = protectedContext,
            snapshot = DirectBootAlarmSnapshot(
                alarmId = alarmId,
                triggerTime = 1L,
                label = "",
                timeLabel = "",
                playDefaultSound = true,
                vibrationEnabled = true,
                updatedAt = 0L
            ),
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarmManager = protectedContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun recordFired(context: Context, alarmId: Long, triggerTime: Long, firedAt: Long) {
        val edit = prefs(context).edit()
            .putLong(KEY_FIRED_ALARM_ID, alarmId)
            .putLong(KEY_FIRED_TRIGGER_TIME, triggerTime)
            .putLong(KEY_FIRED_AT, firedAt)
        read(context)?.let { current ->
            if (current.alarmId == alarmId) {
                edit
                    .remove(KEY_SCHEMA_VERSION)
                    .remove(KEY_ALARM_ID)
                    .remove(KEY_TRIGGER_TIME)
                    .remove(KEY_LABEL)
                    .remove(KEY_TIME_LABEL)
                    .remove(KEY_PLAY_DEFAULT_SOUND)
                    .remove(KEY_VIBRATION_ENABLED)
                    .remove(KEY_UPDATED_AT)
            }
        }
        edit.apply()
    }

    fun consumeFiredOneShotMarker(context: Context, alarm: Alarm, now: Long): Boolean {
        if (alarm.repeatDays.isNotEmpty()) return false

        val prefs = prefs(context)
        val firedAlarmId = prefs.getLong(KEY_FIRED_ALARM_ID, -1L)
        val firedTriggerTime = prefs.getLong(KEY_FIRED_TRIGGER_TIME, 0L)
        if (firedAlarmId != alarm.id || firedTriggerTime <= 0L || firedTriggerTime > now) {
            return false
        }

        val matchesPersistedTrigger = alarm.nextTriggerTime <= 0L ||
            alarm.nextTriggerTime == firedTriggerTime ||
            alarm.nextTriggerTime <= now
        if (!matchesPersistedTrigger) return false

        prefs.edit()
            .remove(KEY_FIRED_ALARM_ID)
            .remove(KEY_FIRED_TRIGGER_TIME)
            .remove(KEY_FIRED_AT)
            .apply()
        return true
    }

    fun read(context: Context): DirectBootAlarmSnapshot? {
        val prefs = prefs(context)
        val alarmId = prefs.getLong(KEY_ALARM_ID, -1L)
        val triggerTime = prefs.getLong(KEY_TRIGGER_TIME, 0L)
        if (alarmId <= 0L || triggerTime <= 0L) return null

        return DirectBootAlarmSnapshot(
            alarmId = alarmId,
            triggerTime = triggerTime,
            label = prefs.getString(KEY_LABEL, "").orEmpty(),
            timeLabel = prefs.getString(KEY_TIME_LABEL, "").orEmpty(),
            playDefaultSound = prefs.getBoolean(KEY_PLAY_DEFAULT_SOUND, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true),
            updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L),
            schemaVersion = prefs.getInt(
                KEY_SCHEMA_VERSION,
                DirectBootAlarmSnapshot.CURRENT_SCHEMA_VERSION
            )
        )
    }

    private fun write(context: Context, snapshot: DirectBootAlarmSnapshot) {
        prefs(context).edit()
            .putInt(KEY_SCHEMA_VERSION, snapshot.schemaVersion)
            .putLong(KEY_ALARM_ID, snapshot.alarmId)
            .putLong(KEY_TRIGGER_TIME, snapshot.triggerTime)
            .putString(KEY_LABEL, snapshot.label)
            .putString(KEY_TIME_LABEL, snapshot.timeLabel)
            .putBoolean(KEY_PLAY_DEFAULT_SOUND, snapshot.playDefaultSound)
            .putBoolean(KEY_VIBRATION_ENABLED, snapshot.vibrationEnabled)
            .putLong(KEY_UPDATED_AT, snapshot.updatedAt)
            .apply()
    }

    private fun scheduleFallback(context: Context, snapshot: DirectBootAlarmSnapshot) {
        val protectedContext = protectedContext(context)
        val alarmManager = protectedContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val operation = DirectBootAlarmReceiver.pendingIntent(protectedContext, snapshot)
            ?: return

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(snapshot.triggerTime, showIntent(protectedContext)),
                operation
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "setAlarmClock denied during Direct Boot; using exact fallback", e)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snapshot.triggerTime,
                operation
            )
        }
    }

    private fun showIntent(context: Context): PendingIntent {
        val snapshot = read(context)
        val launch = DirectBootAlarmActivity.intent(
            context = context,
            alarmId = snapshot?.alarmId ?: -1L,
            timeLabel = snapshot?.timeLabel.orEmpty()
        )
        return PendingIntent.getActivity(
            context,
            DIRECT_BOOT_SHOW_REQUEST_CODE,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun prefs(context: Context) = protectedContext(context).getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private fun protectedContext(context: Context): Context {
        return context.applicationContext.createDeviceProtectedStorageContext()
    }

    private fun formatTriggerTime(context: Context, triggerTime: Long): String {
        val zone = ZoneId.systemDefault()
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return Instant.ofEpochMilli(triggerTime)
            .atZone(zone)
            .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }
}
