package com.sysadmindoc.alarmclock.data.repository

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Why Android ended the app's process, grouped into what a user can act on. */
enum class ProcessExitCause {
    MEMORY_LIMIT,
    LOW_MEMORY,
    FORCE_STOPPED,
    TASK_MANAGER,
    CRASH,
    NOT_RESPONDING,
    APP_UPDATED,
    PERMISSION_CHANGED,
    RESOURCE_USE,
    FROZEN,
    SELF_EXIT,
    START_FAILED,
    SYSTEM_KILLED,
    UNKNOWN
}

/** What the incident log says about the alarm that was set when the process died. */
enum class ExitAlarmOutcome {
    /** The incident log doesn't reach back to the exit. */
    UNKNOWN,
    NO_ALARM_SET,
    NOT_DUE_YET,
    RANG,
    NO_RING_RECORDED,
    /** The alarm armed at the exit was moved, skipped or turned off before it came up. */
    CHANGED
}

/** One past death of the app's process, as Android recorded it. Read live, never stored. */
data class ProcessExit(
    val reasonCode: Int,
    val timestamp: Long,
    val description: String,
    val cause: ProcessExitCause,
    val alarmOutcome: ExitAlarmOutcome,
    /** When the next alarm armed across the exit was due, or 0 when none is on record. */
    val armedAlarmAt: Long = 0L
)

/**
 * Reads the OS's own record of why the app's process ended
 * ([ActivityManager.getHistoricalProcessExitReasons], Android 11+) and lines
 * each exit up against the alarm incident log. Android keeps that record
 * itself, so nothing here is written to disk.
 */
@Singleton
class ProcessExitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incidentRepository: AlarmIncidentRepository,
    private val alarmRepository: AlarmRepository
) {
    suspend fun recentExits(limit: Int = DEFAULT_LIMIT): List<ProcessExit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return emptyList()
        val exits = withContext(Dispatchers.IO) {
            runCatching {
                activityManager.getHistoricalProcessExitReasons(context.packageName, 0, limit)
            }.getOrNull().orEmpty()
        }
        if (exits.isEmpty()) return emptyList()
        val incidents = runCatching { incidentRepository.getRecent(limit = INCIDENT_WINDOW) }
            .getOrDefault(emptyList())
        val enabledAlarms = runCatching { alarmRepository.getEnabled().associate { it.id to it.createdAt } }
            .getOrDefault(emptyMap())
        val now = System.currentTimeMillis()
        return exits.map { info ->
            ProcessExitReports.build(
                reasonCode = info.reason,
                timestamp = info.timestamp,
                description = info.description.orEmpty(),
                incidents = incidents,
                enabledAlarms = enabledAlarms,
                now = now
            )
        }
    }

    private companion object {
        const val DEFAULT_LIMIT = 10
        /** The incident table's own cap, so every kept row can be matched. */
        const val INCIDENT_WINDOW = 100
    }
}

internal object ProcessExitReports {
    /** Android 17's memory limiter tags its kills only in the description. */
    private const val MEMORY_LIMITER_TAG = "MemoryLimiter"

    /** Only the ringing service's own rows prove a ring started. */
    private const val RING_SOURCE = "AlarmService"

    /** An alarm's state rows in time order. An edit writes a cancel and a new arm in the same millisecond, so on a tie the arm wins. */
    private val STATE_ORDER = compareBy<AlarmIncidentEvent>(
        { it.eventAt },
        { it.reasonCode != AlarmIncidentEvent.REASON_CANCELLED }
    )

    fun build(
        reasonCode: Int,
        timestamp: Long,
        description: String,
        incidents: List<AlarmIncidentEvent>,
        enabledAlarms: Map<Long, Long>,
        now: Long
    ): ProcessExit {
        val (outcome, armedAt) = alarmOutcome(timestamp, incidents, enabledAlarms, now)
        return ProcessExit(
            reasonCode = reasonCode,
            timestamp = timestamp,
            description = description,
            cause = cause(reasonCode, description),
            alarmOutcome = outcome,
            armedAlarmAt = armedAt
        )
    }

    fun cause(reasonCode: Int, description: String): ProcessExitCause {
        if (description.contains(MEMORY_LIMITER_TAG, ignoreCase = true)) return ProcessExitCause.MEMORY_LIMIT
        return when (reasonCode) {
            ApplicationExitInfo.REASON_LOW_MEMORY -> ProcessExitCause.LOW_MEMORY
            ApplicationExitInfo.REASON_USER_REQUESTED -> ProcessExitCause.FORCE_STOPPED
            ApplicationExitInfo.REASON_USER_STOPPED -> ProcessExitCause.TASK_MANAGER
            ApplicationExitInfo.REASON_CRASH,
            ApplicationExitInfo.REASON_CRASH_NATIVE -> ProcessExitCause.CRASH
            ApplicationExitInfo.REASON_ANR -> ProcessExitCause.NOT_RESPONDING
            ApplicationExitInfo.REASON_PACKAGE_UPDATED,
            ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> ProcessExitCause.APP_UPDATED
            ApplicationExitInfo.REASON_PERMISSION_CHANGE -> ProcessExitCause.PERMISSION_CHANGED
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> ProcessExitCause.RESOURCE_USE
            ApplicationExitInfo.REASON_FREEZER -> ProcessExitCause.FROZEN
            ApplicationExitInfo.REASON_EXIT_SELF -> ProcessExitCause.SELF_EXIT
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> ProcessExitCause.START_FAILED
            ApplicationExitInfo.REASON_SIGNALED,
            ApplicationExitInfo.REASON_DEPENDENCY_DIED,
            ApplicationExitInfo.REASON_OTHER -> ProcessExitCause.SYSTEM_KILLED
            else -> ProcessExitCause.UNKNOWN
        }
    }

    /**
     * Each alarm's last successful SCHEDULE row before the exit is its state
     * at that moment: armed for a time, or disarmed by a CANCELLED row. The
     * earliest armed time after the exit is the one the dead process should
     * have rung next. It rang if the ringing service logged more starts for
     * that alarm and time than starts that found the alarm gone. The service
     * logs one start row per start command and fails a start only when the
     * row is missing, so a redelivered start that fails doesn't undo a ring
     * that already happened. AlarmReceiver's own rows don't count, because
     * the service start they ask for can still fail.
     *
     * An alarm moved, skipped, paused or deleted after the exit but before it
     * came up wasn't missed, so its last state before the due time has to
     * still be that same arm. When every armed alarm was changed that way the
     * outcome is [ExitAlarmOutcome.CHANGED].
     *
     * Builds before 1.16.0 logged no CANCELLED row, so an armed row can
     * outlive its alarm. It only counts while something backs it: the alarm
     * is on now ([enabledAlarms], id to creation time), it rang, or a state
     * row logged after the exit shows it was still there.
     *
     * The incident table keeps 100 rows, so an alarm armed long ago can have
     * lost its arm row. "No alarm was set" needs every alarm that is on now,
     * and existed at the exit, to have its state at the exit on record.
     */
    fun alarmOutcome(
        exitAt: Long,
        incidents: List<AlarmIncidentEvent>,
        enabledAlarms: Map<Long, Long>,
        now: Long
    ): Pair<ExitAlarmOutcome, Long> {
        if (incidents.none { it.eventAt <= exitAt }) return ExitAlarmOutcome.UNKNOWN to 0L
        val states = incidents.filter {
            it.type == AlarmIncidentEvent.TYPE_SCHEDULE && it.status == AlarmIncidentEvent.STATUS_SUCCEEDED
        }
        fun sameFire(row: AlarmIncidentEvent, armed: AlarmIncidentEvent) =
            row.alarmId == armed.alarmId && row.scheduledAt == armed.scheduledAt
        fun serviceRow(row: AlarmIncidentEvent, status: String) =
            row.type == AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE && row.status == status && row.source == RING_SOURCE
        fun rang(armed: AlarmIncidentEvent) =
            incidents.count { serviceRow(it, AlarmIncidentEvent.STATUS_RECEIVED) && sameFire(it, armed) } >
                incidents.count { serviceRow(it, AlarmIncidentEvent.STATUS_FAILED) && sameFire(it, armed) }
        fun stillArmedWhenDue(armed: AlarmIncidentEvent): Boolean {
            val last = states.filter { it.alarmId == armed.alarmId && it.eventAt < armed.scheduledAt }
                .maxWithOrNull(STATE_ORDER) ?: return false
            return last.reasonCode != AlarmIncidentEvent.REASON_CANCELLED && last.scheduledAt == armed.scheduledAt
        }
        fun backed(armed: AlarmIncidentEvent) = armed.alarmId in enabledAlarms || rang(armed) ||
            states.any { it.alarmId == armed.alarmId && it.eventAt > exitAt }

        val armedAtExit = states
            .filter { it.eventAt <= exitAt }
            .groupBy { it.alarmId }
            .values
            .mapNotNull { rows -> rows.maxWithOrNull(STATE_ORDER) }
            .filter { it.reasonCode != AlarmIncidentEvent.REASON_CANCELLED && it.scheduledAt > exitAt }
            .filter { backed(it) }
            .sortedBy { it.scheduledAt }
        val armed = armedAtExit.firstOrNull { rang(it) || stillArmedWhenDue(it) }
        if (armed == null) {
            armedAtExit.firstOrNull()?.let { return ExitAlarmOutcome.CHANGED to it.scheduledAt }
            val stateLost = enabledAlarms.any { (id, createdAt) ->
                createdAt <= exitAt && states.none { it.alarmId == id && it.eventAt <= exitAt }
            }
            return (if (stateLost) ExitAlarmOutcome.UNKNOWN else ExitAlarmOutcome.NO_ALARM_SET) to 0L
        }
        val outcome = when {
            rang(armed) -> ExitAlarmOutcome.RANG
            armed.scheduledAt > now -> ExitAlarmOutcome.NOT_DUE_YET
            else -> ExitAlarmOutcome.NO_RING_RECORDED
        }
        return outcome to armed.scheduledAt
    }
}
