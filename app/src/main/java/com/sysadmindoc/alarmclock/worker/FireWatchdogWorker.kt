package com.sysadmindoc.alarmclock.worker

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.service.AlarmFullScreenFallback
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Proactive fire watchdog. Enqueued [FireWatchdogPolicy.WATCHDOG_DELAY_MS] after
 * every scheduled alarm fire (see [AlarmScheduler.scheduleSupportingWork]). When
 * it runs it checks whether AlarmManager ever delivered the fire; if not — and
 * the alarm is still enabled, in-window, and the user opted into repeat-missed
 * recovery — it re-fires the alarm through the same `AlarmService` start path
 * `WakeConfirmWorker` uses.
 *
 * This catches the failure class where the alarm was silently suppressed (Pixel
 * "missed alarm — unknown reason", OEM Doze kills) — cases the reactive
 * on-unlock replay can't see, because no miss was ever recorded. It can never
 * double-fire a working alarm: a delivered fire always writes a `BROADCAST`
 * incident, which short-circuits the re-fire.
 */
@HiltWorker
class FireWatchdogWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val alarmRepository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
    private val alarmIncidentRepository: AlarmIncidentRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong(KEY_ALARM_ID, -1L)
        if (alarmId == -1L) return Result.success()
        val scheduledAt = inputData.getLong(KEY_SCHEDULED_AT, 0L)
        val fireId = AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)

        val alarm = alarmRepository.getById(alarmId)
        val repeatMissedEnabled = preferencesManager.getCurrentSettings().repeatMissedAlarms
        val deliveryCount = alarmIncidentRepository.occurrenceDeliveryCount(alarmId, scheduledAt)

        val decision = FireWatchdogPolicy.decide(
            repeatMissedEnabled = repeatMissedEnabled,
            alarmExists = alarm != null,
            isEnabled = alarm?.isEnabled == true,
            deliveryCount = deliveryCount,
            scheduledAtMs = scheduledAt,
            nowMs = System.currentTimeMillis()
        )

        if (!decision.shouldRefire) {
            recordIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_SCHEDULE,
                status = AlarmIncidentEvent.STATUS_SKIPPED,
                reasonCode = decision.reasonCode
            )
            return Result.success()
        }

        // Re-fire through the same path WakeConfirmWorker uses so behavior can't
        // diverge from a normal fire. This goes straight to the service (not via
        // AlarmReceiver), so it writes no BROADCAST incident and cannot trigger a
        // second watchdog.
        val startIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, fireId)
        }
        return try {
            context.startForegroundService(startIntent)
            recordIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                status = AlarmIncidentEvent.STATUS_REQUESTED,
                reasonCode = FireWatchdogPolicy.Decision.REFIRE.reasonCode
            )
            Result.success()
        } catch (_: Exception) {
            // Without the battery-optimisation exemption the platform refuses
            // the foreground-service start, and the whole point of the watchdog
            // is that a missed alarm does not stay missed. Surface it as a
            // full-screen intent instead of failing quietly.
            val fallbackPosted = alarm != null && AlarmFullScreenFallback.post(
                context = context,
                alarm = alarm,
                scheduledAt = scheduledAt,
                fireId = fireId,
                hideLabel = preferencesManager.getCurrentSettings()
                    .hideAlarmLabelsOnPublicSurfaces
            )
            recordIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                status = if (fallbackPosted) AlarmIncidentEvent.STATUS_SUCCEEDED
                else AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = if (fallbackPosted) "FIRE_WATCHDOG_FSI_FALLBACK"
                else "FIRE_WATCHDOG_REFIRE_START_FAILED"
            )
            Result.success()
        }
    }

    private suspend fun recordIncident(
        alarmId: Long,
        fireId: String,
        scheduledAt: Long,
        type: String,
        status: String,
        reasonCode: String
    ) {
        alarmIncidentRepository.record(
            alarmId = alarmId,
            fireId = fireId,
            scheduledAt = scheduledAt,
            type = type,
            status = status,
            reasonCode = reasonCode,
            source = SOURCE
        )
    }

    companion object {
        const val KEY_ALARM_ID = "alarm_id"
        const val KEY_SCHEDULED_AT = "scheduled_at"
        const val SOURCE = "FireWatchdogWorker"

        fun uniqueName(alarmId: Long): String = "fire_watchdog_$alarmId"
    }
}
