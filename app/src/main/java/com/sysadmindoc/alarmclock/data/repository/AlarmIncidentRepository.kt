package com.sysadmindoc.alarmclock.data.repository

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.sysadmindoc.alarmclock.data.local.AlarmIncidentEventDao
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmIncidentRepository @Inject constructor(
    private val dao: AlarmIncidentEventDao,
    @ApplicationContext private val context: Context
) {
    // Application-lived scope so fire-and-forget records survive component
    // teardown. Callers that record right before finish()/stopSelf() (firing
    // activity, AlarmService, wake-confirm activity) would otherwise race
    // their own scope cancellation and silently drop the most diagnostic
    // events — the same bug class WebhookService.fireAsync() exists to fix.
    private val recordScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun observeRecent(limit: Int = DEFAULT_EXPORT_LIMIT): Flow<List<AlarmIncidentEvent>> {
        return dao.observeRecent(limit.coerceIn(1, MAX_ROWS))
    }

    suspend fun getRecent(limit: Int = DEFAULT_EXPORT_LIMIT): List<AlarmIncidentEvent> {
        return dao.getRecent(limit.coerceIn(1, MAX_ROWS))
    }

    suspend fun clearHistory() {
        dao.deleteAll()
    }

    /**
     * Number of `BROADCAST` incidents recorded for a specific alarm occurrence.
     * `AlarmReceiver` is the only writer of that type, and only on actual
     * AlarmManager delivery, so a zero here means the fire never reached the
     * receiver — the signal the fire watchdog acts on.
     */
    suspend fun broadcastDeliveryCount(alarmId: Long, scheduledAt: Long): Int {
        return dao.countByOccurrenceAndType(
            alarmId = alarmId,
            scheduledAt = scheduledAt,
            type = AlarmIncidentEvent.TYPE_BROADCAST
        )
    }

    fun recordAsync(
        alarmId: Long,
        fireId: String,
        scheduledAt: Long,
        eventAt: Long = System.currentTimeMillis(),
        type: String,
        status: String,
        reasonCode: String = AlarmIncidentEvent.REASON_NONE,
        source: String,
        algorithmVersion: String = AlarmIncidentEvent.VALUE_NONE
    ) {
        recordScope.launch {
            record(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                eventAt = eventAt,
                type = type,
                status = status,
                reasonCode = reasonCode,
                source = source,
                algorithmVersion = algorithmVersion
            )
        }
    }

    suspend fun record(
        alarmId: Long,
        fireId: String,
        scheduledAt: Long,
        eventAt: Long = System.currentTimeMillis(),
        type: String,
        status: String,
        reasonCode: String = AlarmIncidentEvent.REASON_NONE,
        source: String,
        algorithmVersion: String = AlarmIncidentEvent.VALUE_NONE
    ): Long? {
        return runCatching {
            val event = AlarmIncidentEvent(
                fireId = fireId.ifBlank { AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt) },
                alarmId = alarmId,
                scheduledAt = scheduledAt.coerceAtLeast(0L),
                eventAt = eventAt.coerceAtLeast(0L),
                elapsedMs = if (scheduledAt > 0L && eventAt > 0L) {
                    eventAt - scheduledAt
                } else {
                    0L
                },
                type = type,
                status = status,
                reasonCode = reasonCode,
                source = source,
                sdkInt = Build.VERSION.SDK_INT,
                standbyBucket = appStandbyBucketLabel(),
                exactAlarmAllowed = exactAlarmStatus(),
                notificationPermissionGranted = notificationPermissionStatus(),
                fullScreenIntentAllowed = fullScreenIntentStatus(),
                batteryOptimizationsIgnored = batteryOptimizationStatus(),
                algorithmVersion = algorithmVersion
            ).sanitized()
            val id = dao.insert(event)
            prune()
            id
        }.getOrNull()
    }

    private suspend fun prune() {
        val before = System.currentTimeMillis() - RETENTION_MS
        dao.deleteOlderThan(before)
        dao.trimToLatest(MAX_ROWS)
    }

    private fun notificationPermissionStatus(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return "TRUE"
        }
        return if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            "TRUE"
        } else {
            "FALSE"
        }
    }

    private fun exactAlarmStatus(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return "TRUE"
        return if (context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true) {
            "TRUE"
        } else {
            "FALSE"
        }
    }

    private fun fullScreenIntentStatus(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return AlarmIncidentEvent.VALUE_NOT_APPLICABLE
        }
        val allowed = runCatching {
            context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent()
        }.getOrNull()
        return when (allowed) {
            true -> "TRUE"
            false -> "FALSE"
            null -> AlarmIncidentEvent.VALUE_UNKNOWN
        }
    }

    private fun batteryOptimizationStatus(): String {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true) {
            "TRUE"
        } else {
            "FALSE"
        }
    }

    private fun appStandbyBucketLabel(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return AlarmIncidentEvent.VALUE_NOT_APPLICABLE
        }
        val bucket = runCatching {
            (context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)
                ?.appStandbyBucket
        }.getOrNull() ?: return AlarmIncidentEvent.VALUE_UNKNOWN
        val label = when (bucket) {
            UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "ACTIVE"
            UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "WORKING_SET"
            UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "FREQUENT"
            UsageStatsManager.STANDBY_BUCKET_RARE -> "RARE"
            UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "RESTRICTED"
            else -> "UNKNOWN"
        }
        return "${label}_${bucket}"
    }

    private companion object {
        const val DEFAULT_EXPORT_LIMIT = 25
        const val MAX_ROWS = 100
        const val RETENTION_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
