package com.sysadmindoc.alarmclock.service

import android.annotation.SuppressLint
import android.content.Context

/**
 * Per-occurrence state that has to survive the service being killed and
 * restarted between a fire and the user's next tap.
 *
 * AlarmService owns the writes; the firing screen reads the snooze count so it
 * can show how many snoozes are left and stop offering one that will be
 * refused.
 */
object AlarmRuntimeState {
    private const val PREFS_NAME = "alarm_runtime_state"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun snoozeCountKey(alarmId: Long) = "snooze_count_$alarmId"

    fun snoozeCount(context: Context, alarmId: Long): Int {
        if (alarmId <= 0L) return 0
        return prefs(context).getInt(snoozeCountKey(alarmId), 0).coerceAtLeast(0)
    }

    // commit(), not apply(): this is the snooze count of an alarm that is
    // ringing right now. apply() queues the write, and a process death between
    // the queue and the flush hands the user a snooze cap that is already spent
    // or one that never runs out. The file holds one int.
    @SuppressLint("ApplySharedPref")
    fun setSnoozeCount(context: Context, alarmId: Long, count: Int) {
        if (alarmId <= 0L) return
        prefs(context).edit()
            .putInt(snoozeCountKey(alarmId), count.coerceAtLeast(0))
            .commit()
    }

    @SuppressLint("ApplySharedPref")
    fun clear(context: Context, alarmId: Long) {
        if (alarmId <= 0L) return
        prefs(context).edit()
            .remove(snoozeCountKey(alarmId))
            .commit()
    }
}
