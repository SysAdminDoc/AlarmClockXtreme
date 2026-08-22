package com.sysadmindoc.alarmclock.ui.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class PersistedTimerRecord(
    val id: Int,
    val label: String,
    val totalSeconds: Long,
    val remainingMillis: Long,
    val state: TimerState,
    val endElapsedRealtime: Long = 0L,
    /**
     * `Settings.Global.BOOT_COUNT` when the record was written, or -1 when the
     * counter was unreadable.
     *
     * endElapsedRealtime is measured against `SystemClock.elapsedRealtime()`,
     * which resets to zero on every boot. Without knowing which boot a record
     * belongs to, a pre-reboot deadline either looks days away or looks long
     * overdue and rings a timer the user never started.
     */
    val bootCount: Long = -1L
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

data class TimerRestoreSnapshot(
    val records: List<PersistedTimerRecord>,
    val newlyFinished: List<PersistedTimerRecord>
)

data class TimerStartResult(
    val record: PersistedTimerRecord,
    val created: Boolean
)

@Singleton
class TimerStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val contentResolver = context.applicationContext.contentResolver

    fun loadRecords(nowElapsed: Long = SystemClock.elapsedRealtime()): List<PersistedTimerRecord> {
        return readStoredRecords().map { it.normalized(nowElapsed) }
    }

    /**
     * Atomically restores persisted timers and claims any overdue running timers.
     * The caller must alert for [TimerRestoreSnapshot.newlyFinished]. Keeping the
     * transition and returned claim under one lock prevents a receiver and a
     * freshly-created ViewModel from both starting the timer alert service.
     */
    fun restoreSnapshot(nowElapsed: Long = SystemClock.elapsedRealtime()): TimerRestoreSnapshot =
        synchronized(WRITE_LOCK) {
            val stored = readStoredRecords()
            val newlyFinished = stored
                .filter { record ->
                    record.state == TimerState.RUNNING &&
                        record.endElapsedRealtime <= nowElapsed
                }
                .map { it.asFinished() }
            val newlyFinishedIds = newlyFinished.mapTo(mutableSetOf()) { it.id }
            val persisted = if (newlyFinishedIds.isEmpty()) {
                stored
            } else {
                stored.map { record ->
                    if (record.id in newlyFinishedIds) record.asFinished() else record
                }.also(::replace)
            }
            TimerRestoreSnapshot(
                records = persisted.map { it.normalized(nowElapsed) },
                newlyFinished = newlyFinished
            )
        }

    /** Monotonic per-boot counter (API 24+). Returns -1 when unavailable. */
    private fun currentBootCount(): Long = runCatching {
        Settings.Global.getLong(contentResolver, Settings.Global.BOOT_COUNT)
    }.getOrDefault(-1L)

    /**
     * @param dropStaleBoot drop RUNNING records written before the current
     *   boot. The reboot cleanup path passes false because it wants to count
     *   and report them.
     */
    private fun readStoredRecords(dropStaleBoot: Boolean = true): List<PersistedTimerRecord> {
        val raw = prefs.getString(KEY_TIMERS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        val bootCount = if (dropStaleBoot) currentBootCount() else -1L
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
                            endElapsedRealtime = obj.optLong("endElapsedRealtime"),
                            bootCount = obj.optLong("bootCount", -1L)
                        )
                    )
                }
            }
                .filter { it.id > 0 && it.totalSeconds > 0L && it.state != TimerState.IDLE }
                // A RUNNING record from a previous boot carries a deadline on a
                // clock that no longer exists. Drop it rather than ring a
                // phantom timer or show a countdown of days.
                .filter { record ->
                    record.state != TimerState.RUNNING ||
                        bootCount < 0L ||
                        record.bootCount < 0L ||
                        record.bootCount == bootCount
                }
                .sortedBy { it.id }
        }.getOrElse { error ->
            Log.w(TAG, "Failed to read persisted timers; clearing corrupt state", error)
            prefs.edit().remove(KEY_TIMERS).apply()
            emptyList()
        }
    }

    fun loadTimers(nowElapsed: Long = SystemClock.elapsedRealtime()): List<TimerInstance> =
        loadRecords(nowElapsed).map { it.toTimerInstance(nowElapsed) }

    fun replace(records: List<PersistedTimerRecord>) = synchronized(WRITE_LOCK) {
        val bootCountNow = currentBootCount()
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
                        .put(
                            "bootCount",
                            if (record.state == TimerState.RUNNING) {
                                bootCountNow
                            } else {
                                record.bootCount
                            }
                        )
                )
            }
        prefs.edit().putString(KEY_TIMERS, array.toString()).apply()
    }

    // Read-modify-write mutators are serialized on a process-wide lock: the
    // countdown coroutine (UI) and TimerExpiryReceiver (broadcast thread)
    // construct separate TimerStore instances over the same SharedPreferences,
    // so without this an interleaved load/replace would drop or resurrect a
    // timer (e.g. a tick write clobbering a concurrent "finished" write).
    fun upsert(record: PersistedTimerRecord) = synchronized(WRITE_LOCK) {
        val records = readStoredRecords()
            .filterNot { it.id == record.id } + record
        replace(records)
    }

    fun remove(id: Int) = synchronized(WRITE_LOCK) {
        replace(readStoredRecords().filterNot { it.id == id })
    }

    fun markFinished(id: Int): PersistedTimerRecord? = synchronized(WRITE_LOCK) {
        val records = readStoredRecords()
        val timer = records.firstOrNull {
            it.id == id && it.state == TimerState.RUNNING
        } ?: return@synchronized null
        val finished = timer.asFinished()
        replace(records.map { if (it.id == id) finished else it })
        finished
    }

    /**
     * Atomically consumes one finished timer and creates a fresh running timer
     * with the same duration and label. Re-delivery is intentionally a no-op:
     * the source record no longer exists after the first successful restart.
     */
    fun restartFinished(
        id: Int,
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): PersistedTimerRecord? = synchronized(WRITE_LOCK) {
        val records = readStoredRecords()
        val finished = records.firstOrNull {
            it.id == id && it.state == TimerState.FINISHED
        } ?: return@synchronized null
        val newId = (records.maxOfOrNull { it.id } ?: 0) + 1
        val restarted = PersistedTimerRecord(
            id = newId,
            label = finished.label,
            totalSeconds = finished.totalSeconds,
            remainingMillis = finished.totalSeconds * 1_000L,
            state = TimerState.RUNNING,
            endElapsedRealtime = nowElapsed + finished.totalSeconds * 1_000L
        )
        replace(records.filterNot { it.id == id } + restarted)
        restarted
    }

    fun nextId(): Int = synchronized(WRITE_LOCK) {
        (readStoredRecords().maxOfOrNull { it.id } ?: 0) + 1
    }

    /**
     * Starts a timer as one atomic read-modify-write. An identical timer whose
     * deadline is within the delivery-coalescing window is returned rather than
     * duplicated, which makes repeated AlarmClock intents idempotent even if
     * Android recreates the proxy activity between deliveries.
     */
    fun startOrReuse(
        totalSeconds: Long,
        label: String,
        nowElapsed: Long = SystemClock.elapsedRealtime()
    ): TimerStartResult = synchronized(WRITE_LOCK) {
        require(totalSeconds in 1L..86_400L)
        val records = readStoredRecords()
        val endElapsed = nowElapsed + totalSeconds * 1_000L
        val existing = records.firstOrNull { record ->
            record.state == TimerState.RUNNING &&
                record.totalSeconds == totalSeconds &&
                record.label == label &&
                kotlin.math.abs(record.endElapsedRealtime - endElapsed) <= DUPLICATE_WINDOW_MS
        }
        if (existing != null) {
            return@synchronized TimerStartResult(existing.normalized(nowElapsed), created = false)
        }
        val id = (records.maxOfOrNull { it.id } ?: 0) + 1
        val record = PersistedTimerRecord(
            id = id,
            label = label,
            totalSeconds = totalSeconds,
            remainingMillis = totalSeconds * 1_000L,
            state = TimerState.RUNNING,
            endElapsedRealtime = endElapsed
        )
        replace(records + record)
        TimerStartResult(record, created = true)
    }

    fun removeRunningTimersForReboot(): List<PersistedTimerRecord> = synchronized(WRITE_LOCK) {
        val records = readStoredRecords(dropStaleBoot = false)
        val running = records.filter { it.state == TimerState.RUNNING }
        if (running.isNotEmpty()) {
            replace(records.filterNot { it.state == TimerState.RUNNING })
        }
        running
    }

    private fun PersistedTimerRecord.asFinished(): PersistedTimerRecord = copy(
        remainingMillis = 0L,
        state = TimerState.FINISHED,
        endElapsedRealtime = 0L
    )

    companion object {
        private const val TAG = "TimerStore"
        private const val PREFS_NAME = "timer_state"
        private const val KEY_TIMERS = "timers_json"
        private const val DUPLICATE_WINDOW_MS = 5_000L

        // Process-wide: guards the read-modify-write of the shared prefs across
        // all TimerStore instances.
        private val WRITE_LOCK = Any()
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
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    endElapsedRealtime,
                    pendingIntent
                )
            } catch (fallback: Exception) {
                Log.e("TimerAlarmScheduler", "Inexact timer fallback also failed", fallback)
            }
        } catch (e: Exception) {
            Log.e("TimerAlarmScheduler", "Timer expiry registration failed", e)
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
