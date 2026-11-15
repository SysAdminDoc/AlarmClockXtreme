package com.sysadmindoc.alarmclock.service

import android.annotation.SuppressLint
import android.content.Context

/**
 * Per-occurrence state that has to survive the service being killed and
 * restarted between a fire and the user's next tap.
 *
 * AlarmService owns the snooze count writes; the firing screen reads the snooze
 * count so it can show how many snoozes are left and stop offering one that
 * will be refused. AlarmScheduler keeps the last occurrence that ended, so a
 * ring of that occurrence again is known as one even in a fresh service. A
 * missed-alarm replay rings at the time of the miss, so the replay marks that
 * time as ended before it starts.
 */
object AlarmRuntimeState {
    private const val PREFS_NAME = "alarm_runtime_state"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun snoozeCountKey(alarmId: Long) = "snooze_count_$alarmId"

    private fun endedOccurrenceKey(alarmId: Long) = "ended_occurrence_$alarmId"

    /** The time [alarmId]'s last ended ring was set for, or 0. */
    fun lastEndedOccurrence(context: Context, alarmId: Long): Long {
        if (alarmId <= 0L) return 0L
        return prefs(context).getLong(endedOccurrenceKey(alarmId), 0L)
    }

    // Kept past clear(): it names one occurrence, so it can only ever match a
    // later ring of that same occurrence.
    @SuppressLint("ApplySharedPref")
    fun setLastEndedOccurrence(context: Context, alarmId: Long, scheduledAt: Long) {
        if (alarmId <= 0L || scheduledAt <= 0L) return
        prefs(context).edit()
            .putLong(endedOccurrenceKey(alarmId), scheduledAt)
            .commit()
    }

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
