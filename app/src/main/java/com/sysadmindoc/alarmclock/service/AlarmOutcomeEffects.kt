package com.sysadmindoc.alarmclock.service

import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.wear.WearNextAlarmBridge

/**
 * Everything a ring leaves behind outside the ringing itself: the history row
 * that stats and streaks are built from, the incident log, webhooks, the
 * post-dismiss action, the watch, and the alarm's schedule.
 *
 * A test run rings exactly like the real alarm but must leave every one of
 * these untouched, so [AlarmService] routes them through here with the run's
 * test flag instead of calling the dependencies directly. Keeping the gate in
 * one place is what lets a unit test prove nothing slips through.
 */
internal class AlarmOutcomeEffects(
    private val eventRepository: AlarmEventRepository,
    private val incidentRepository: AlarmIncidentRepository,
    private val webhookService: WebhookService,
    private val dismissActionExecutor: DismissActionExecutor,
    private val alarmScheduler: AlarmScheduler,
    private val wearBridge: WearNextAlarmBridge
) {

    suspend fun recordEvent(testRun: Boolean, event: () -> AlarmEvent) {
        if (testRun) return
        eventRepository.record(event())
    }

    fun recordIncidentAsync(
        testRun: Boolean,
        alarmId: Long,
        fireId: String,
        scheduledAt: Long,
        type: String,
        status: String,
        reasonCode: String,
        source: String
    ) {
        if (testRun || alarmId <= 0L) return
        incidentRepository.recordAsync(
            alarmId = alarmId,
            fireId = fireId,
            scheduledAt = scheduledAt,
            type = type,
            status = status,
            reasonCode = reasonCode,
            source = source
        )
    }

    suspend fun recordIncident(
        testRun: Boolean,
        alarmId: Long,
        fireId: String,
        scheduledAt: Long,
        type: String,
        status: String,
        reasonCode: String,
        source: String
    ) {
        if (testRun || alarmId <= 0L) return
        incidentRepository.record(
            alarmId = alarmId,
            fireId = fireId,
            scheduledAt = scheduledAt,
            type = type,
            status = status,
            reasonCode = reasonCode,
            source = source
        )
    }

    fun fireWebhook(
        testRun: Boolean,
        event: WebhookEvent,
        alarm: Alarm,
        timeFormatted: String,
        scheduledForMillis: Long?,
        fireId: String
    ) {
        if (testRun) return
        webhookService.fireAsync(
            event = event,
            alarmId = alarm.id,
            label = alarm.label,
            timeFormatted = timeFormatted,
            scheduledForMillis = scheduledForMillis,
            fireId = fireId
        )
    }

    fun runDismissAction(testRun: Boolean, alarm: Alarm) {
        if (testRun) return
        dismissActionExecutor.executeAsync(alarm)
    }

    fun publishFiring(testRun: Boolean, alarm: Alarm) {
        if (testRun) return
        wearBridge.publishAlarmFiring(alarm)
    }

    fun publishIdle(testRun: Boolean, alarmId: Long) {
        if (testRun) return
        wearBridge.publishAlarmIdle(alarmId)
    }

    /**
     * Ends the occurrence in the schedule: a one-shot is switched off and a
     * repeating alarm moves to its next time. A test never fired a scheduled
     * occurrence, so there is nothing to move.
     */
    suspend fun finishOccurrence(testRun: Boolean, alarmId: Long, scheduledAt: Long) {
        if (testRun) return
        alarmScheduler.handleAlarmFired(alarmId, scheduledAt)
    }

    /**
     * Arms the snooze re-ring. A real snooze goes through the scheduler, which
     * also moves the alarm's next trigger to the snooze time. A test snooze
     * uses [scheduleTestSnooze] instead, its own PendingIntent, so the saved
     * alarm's next time and any real snooze on it stay exactly as they were.
     */
    suspend fun scheduleSnooze(
        testRun: Boolean,
        alarm: Alarm,
        snoozeEndAt: Long,
        scheduleTestSnooze: (Alarm, Long) -> Boolean
    ): Boolean = if (testRun) {
        scheduleTestSnooze(alarm, snoozeEndAt)
    } else {
        alarmScheduler.scheduleSnoozeAt(alarm, snoozeEndAt)
    }
}
