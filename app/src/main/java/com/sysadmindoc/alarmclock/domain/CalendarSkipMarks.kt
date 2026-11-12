package com.sysadmindoc.alarmclock.domain

import android.content.Context

/**
 * Remembers which alarms the calendar tag moved, and the trigger it moved
 * them from.
 *
 * [AlarmScheduler] clears an alarm's mark at the start of every schedule pass
 * and writes it only when the calendar step skipped a day, so a mark always
 * describes the alarm's current trigger. Two readers rely on that:
 *
 *  - the calendar refresh recomputes a marked alarm from the original trigger,
 *    so deleting the tagged event brings the alarm back, while an unmarked
 *    alarm is only ever moved later (a skip-next or a dismissed early fire is
 *    never undone by a calendar sync);
 *  - the alarm list shows "Skipped" next to the tag.
 */
object CalendarSkipMarks {
    private const val PREFS_NAME = "calendar_skip_marks"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(alarmId: Long) = "origin_$alarmId"

    /** The trigger the calendar step skipped for [alarmId], or null. */
    fun origin(context: Context, alarmId: Long): Long? {
        if (alarmId <= 0L) return null
        val value = prefs(context).getLong(key(alarmId), 0L)
        return value.takeIf { it > 0L }
    }

    fun set(context: Context, alarmId: Long, originTrigger: Long) {
        if (alarmId <= 0L || originTrigger <= 0L) return
        prefs(context).edit().putLong(key(alarmId), originTrigger).apply()
    }

    fun clear(context: Context, alarmId: Long) {
        if (alarmId <= 0L) return
        val store = prefs(context)
        if (store.contains(key(alarmId))) store.edit().remove(key(alarmId)).apply()
    }
}
