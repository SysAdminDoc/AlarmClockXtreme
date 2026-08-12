package com.sysadmindoc.alarmclock.service

import android.content.Context
import android.content.Intent
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.ui.alarmfiring.AlarmFiringActivity
import java.time.Instant
import java.time.ZoneId

object AlarmFireDismissContract {

    fun fireId(alarmId: Long, scheduledAt: Long): String =
        AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)

    fun startServiceIntent(
        context: Context,
        alarmId: Long,
        scheduledAt: Long,
        fireId: String = fireId(alarmId, scheduledAt)
    ): Intent = Intent(context, AlarmService::class.java).apply {
        action = AlarmService.ACTION_START_ALARM
        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
        putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, fireId)
    }

    fun snoozeServiceIntent(
        context: Context,
        alarmId: Long,
        scheduledAt: Long,
        fireId: String = fireId(alarmId, scheduledAt),
        customMinutes: Int? = null,
        snoozeAtMillis: Long? = null
    ): Intent = Intent(context, AlarmService::class.java).apply {
        action = AlarmService.ACTION_SNOOZE
        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
        putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, fireId)
        if (customMinutes != null) {
            putExtra(AlarmService.EXTRA_CUSTOM_SNOOZE_MINUTES, customMinutes)
        }
        if (snoozeAtMillis != null) {
            putExtra(AlarmService.EXTRA_SNOOZE_UNTIL_MILLIS, snoozeAtMillis)
        }
    }

    fun dismissServiceIntent(
        context: Context,
        alarmId: Long,
        scheduledAt: Long,
        fireId: String = fireId(alarmId, scheduledAt),
        challengeRetryCount: Int = 0,
        challengeSolveTimeMs: Long = 0L
    ): Intent = Intent(context, AlarmService::class.java).apply {
        action = AlarmService.ACTION_DISMISS
        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
        putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, fireId)
        putExtra(AlarmService.EXTRA_CHALLENGE_RETRY_COUNT, challengeRetryCount.coerceAtLeast(0))
        putExtra(AlarmService.EXTRA_CHALLENGE_SOLVE_TIME_MS, challengeSolveTimeMs.coerceAtLeast(0L))
    }

    fun firingActivityIntent(
        context: Context,
        alarmId: Long,
        scheduledAt: Long,
        fireId: String = fireId(alarmId, scheduledAt)
    ): Intent = Intent(context, AlarmFiringActivity::class.java).apply {
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        )
        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
        putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, fireId)
    }

    fun alarmEvent(
        alarm: Alarm,
        scheduledAt: Long,
        firedAt: Long,
        action: String,
        actionAt: Long,
        challengeRetryCount: Int = 0,
        challengeSolveTimeMs: Long = 0L,
        snoozeCount: Int = 0
    ): AlarmEvent {
        val dayOfWeek = Instant.ofEpochMilli(actionAt.coerceAtLeast(0L))
            .atZone(ZoneId.systemDefault())
            .dayOfWeek
            .value
        return AlarmEvent(
            alarmId = alarm.id,
            alarmLabel = alarm.label,
            scheduledTime = scheduledAt,
            firedAt = firedAt,
            action = action,
            actionAt = actionAt,
            challengeType = alarm.challengeType,
            challengeSolveTimeMs = challengeSolveTimeMs.coerceAtLeast(0L),
            challengeRetryCount = challengeRetryCount.coerceAtLeast(0),
            snoozeCount = snoozeCount.coerceAtLeast(0),
            dayOfWeek = dayOfWeek
        )
    }
}
