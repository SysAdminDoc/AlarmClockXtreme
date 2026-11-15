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
    NO_RING_RECORDED
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
        val enabledAlarmIds = runCatching { alarmRepository.getEnabled().mapTo(HashSet()) { it.id } }
            .getOrDefault(emptySet())
        val now = System.currentTimeMillis()
        return exits.map { info ->
            ProcessExitReports.build(
                reasonCode = info.reason,
                timestamp = info.timestamp,
                description = info.description.orEmpty(),
                incidents = incidents,
                enabledAlarmIds = enabledAlarmIds,
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

    private val DELIVERY_TYPES = setOf(
        AlarmIncidentEvent.TYPE_BROADCAST,
        AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE
    )

    fun build(
        reasonCode: Int,
        timestamp: Long,
        description: String,
        incidents: List<AlarmIncidentEvent>,
        enabledAlarmIds: Set<Long>,
        now: Long
    ): ProcessExit {
        val (outcome, armedAt) = alarmOutcome(timestamp, incidents, enabledAlarmIds, now)
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
     * at that moment: armed for a time, or disarmed by a CANCELLED row. An
     * edit writes a cancel and a new arm in the same millisecond, so on a tie
     * the arm wins. The earliest armed time after the exit is the one the dead
     * process should have rung next; it rang if a delivery row that didn't
     * fail exists for the same alarm and time.
     *
     * Builds before 1.16.0 logged no CANCELLED row, and a few disable paths
     * (a skipped or calendar-retired one-shot) still don't, so an armed row
     * can outlive its alarm. It only counts while something backs it: the
     * alarm is on now ([enabledAlarmIds]), a cancel logged after the exit
     * shows it stayed armed until then, or it rang.
     */
    fun alarmOutcome(
        exitAt: Long,
        incidents: List<AlarmIncidentEvent>,
        enabledAlarmIds: Set<Long>,
        now: Long
    ): Pair<ExitAlarmOutcome, Long> {
        if (incidents.none { it.eventAt <= exitAt }) return ExitAlarmOutcome.UNKNOWN to 0L
        fun rang(armed: AlarmIncidentEvent) = incidents.any {
            it.type in DELIVERY_TYPES &&
                it.status != AlarmIncidentEvent.STATUS_FAILED &&
                it.alarmId == armed.alarmId &&
                it.scheduledAt == armed.scheduledAt
        }
        fun cancelledAfterExit(alarmId: Long) = incidents.any {
            it.alarmId == alarmId &&
                it.type == AlarmIncidentEvent.TYPE_SCHEDULE &&
                it.reasonCode == AlarmIncidentEvent.REASON_CANCELLED &&
                it.eventAt > exitAt
        }
        val armed = incidents
            .filter {
                it.type == AlarmIncidentEvent.TYPE_SCHEDULE &&
                    it.status == AlarmIncidentEvent.STATUS_SUCCEEDED &&
                    it.eventAt <= exitAt
            }
            .groupBy { it.alarmId }
            .values
            .mapNotNull { rows ->
                rows.maxWithOrNull(
                    compareBy<AlarmIncidentEvent>({ it.eventAt }, { it.reasonCode != AlarmIncidentEvent.REASON_CANCELLED })
                )
            }
            .filter { it.reasonCode != AlarmIncidentEvent.REASON_CANCELLED && it.scheduledAt > exitAt }
            .filter { it.alarmId in enabledAlarmIds || cancelledAfterExit(it.alarmId) || rang(it) }
            .minByOrNull { it.scheduledAt }
            ?: return ExitAlarmOutcome.NO_ALARM_SET to 0L
        val outcome = when {
            rang(armed) -> ExitAlarmOutcome.RANG
            armed.scheduledAt > now -> ExitAlarmOutcome.NOT_DUE_YET
            else -> ExitAlarmOutcome.NO_RING_RECORDED
        }
        return outcome to armed.scheduledAt
    }
}
