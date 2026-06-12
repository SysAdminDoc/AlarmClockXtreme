package com.sysadmindoc.alarmclock.ui.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class PersistedTimerRecord(
    val id: Int,
    val label: String,
    val totalSeconds: Long,
    val remainingMillis: Long,
    val state: TimerState,
    val endElapsedRealtime: Long = 0L
) {
    fun normalized(nowElapsed: Long = SystemClock.elapsedRealtime()): PersistedTimerRecord {
        if (state != TimerState.RUNNING) return this
        val remaining = (endElapsedRealtime - nowElapsed).coerceAtLeast(0L)
        return if (remaining <= 0L) {
            copy(remainingMillis = 0L, state = TimerState.FINISHED, endElapsedRealtime = 0L)
        } else {
            copy(remainingMillis = remaining)
        }
    }

    fun toTimerInstance(nowElapsed: Long = SystemClock.elapsedRealtime()): TimerInstance {
        val record = normalized(nowElapsed)
        return TimerInstance(
            id = record.id,
            label = record.label,
            totalSeconds = record.totalSeconds,
            remainingMillis = record.remainingMillis,
            state = record.state
        )
    }
}

class TimerStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadRecords(nowElapsed: Long = SystemClock.elapsedRealtime()): List<PersistedTimerRecord> {
        val raw = prefs.getString(KEY_TIMERS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    add(
                        PersistedTimerRecord(
                            id = obj.optInt("id"),
                            label = obj.optString("label"),
                            totalSeconds = obj.optLong("totalSeconds"),
                            remainingMillis = obj.optLong("remainingMillis"),
                            state = runCatching {
                                TimerState.valueOf(obj.optString("state"))
                            }.getOrDefault(TimerState.IDLE),
                            endElapsedRealtime = obj.optLong("endElapsedRealtime")
                        ).normalized(nowElapsed)
                    )
                }
            }
                .filter { it.id > 0 && it.totalSeconds > 0L && it.state != TimerState.IDLE }
                .sortedBy { it.id }
        }.getOrElse { error ->
            Log.w(TAG, "Failed to read persisted timers; clearing corrupt state", error)
            prefs.edit().remove(KEY_TIMERS).apply()
            emptyList()
        }
    }

    fun loadTimers(nowElapsed: Long = SystemClock.elapsedRealtime()): List<TimerInstance> =
        loadRecords(nowElapsed).map { it.toTimerInstance(nowElapsed) }

    fun replace(records: List<PersistedTimerRecord>) {
        val array = JSONArray()
        records
            .filter { it.id > 0 && it.totalSeconds > 0L && it.state != TimerState.IDLE }
            .sortedBy { it.id }
            .forEach { record ->
                array.put(
                    JSONObject()
                        .put("id", record.id)
                        .put("label", record.label)
                        .put("totalSeconds", record.totalSeconds)
                        .put("remainingMillis", record.remainingMillis)
                        .put("state", record.state.name)
                        .put("endElapsedRealtime", record.endElapsedRealtime)
                )
            }
        prefs.edit().putString(KEY_TIMERS, array.toString()).apply()
    }

    fun upsert(record: PersistedTimerRecord) {
        val records = loadRecords()
            .filterNot { it.id == record.id } + record
        replace(records)
    }

    fun remove(id: Int) {
        replace(loadRecords().filterNot { it.id == id })
    }

    fun markFinished(id: Int): PersistedTimerRecord? {
        val records = loadRecords()
        val timer = records.firstOrNull { it.id == id } ?: return null
        val finished = timer.copy(
            remainingMillis = 0L,
            state = TimerState.FINISHED,
            endElapsedRealtime = 0L
        )
        replace(records.map { if (it.id == id) finished else it })
        return finished
    }

    fun nextId(): Int = (loadRecords().maxOfOrNull { it.id } ?: 0) + 1

    fun removeRunningTimersForReboot(): List<PersistedTimerRecord> {
        val records = loadRecords()
        val running = records.filter { it.state == TimerState.RUNNING }
        if (running.isNotEmpty()) {
            replace(records.filterNot { it.state == TimerState.RUNNING })
        }
        return running
    }

    companion object {
        private const val TAG = "TimerStore"
        private const val PREFS_NAME = "timer_state"
        private const val KEY_TIMERS = "timers_json"
    }
}

object TimerAlarmScheduler {
    const val ACTION_TIMER_EXPIRED = "com.sysadmindoc.alarmclock.action.TIMER_EXPIRED"
    const val EXTRA_TIMER_ID = "timer_id"
    private const val REQUEST_BASE = 40_000

    fun schedule(context: Context, timerId: Int, endElapsedRealtime: Long) {
        if (timerId <= 0 || endElapsedRealtime <= SystemClock.elapsedRealtime()) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntent(context, timerId)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                endElapsedRealtime,
                pendingIntent
            )
        } catch (security: SecurityException) {
            Log.w("TimerAlarmScheduler", "Exact timer expiry denied; using inexact fallback", security)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                endElapsedRealtime,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, timerId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(context, timerId))
    }

    private fun pendingIntent(context: Context, timerId: Int): PendingIntent {
        val intent = Intent(context, TimerExpiryReceiver::class.java).apply {
            action = ACTION_TIMER_EXPIRED
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + timerId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
