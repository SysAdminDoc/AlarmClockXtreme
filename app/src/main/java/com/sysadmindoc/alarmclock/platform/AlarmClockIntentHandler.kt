package com.sysadmindoc.alarmclock.platform

import android.content.Context
import android.content.Intent
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import com.sysadmindoc.alarmclock.service.AlarmFireDismissContract
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.ui.timer.TimerAlarmScheduler
import com.sysadmindoc.alarmclock.ui.timer.TimerNotifications
import com.sysadmindoc.alarmclock.ui.timer.TimerStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AlarmClockHandleResult {
    data class Handled(
        val route: String? = null,
        /**
         * An alarm was created with no screen shown, because the caller
         * asked for EXTRA_SKIP_UI. The caller only needs SET_ALARM, a
         * normal permission, so the proxy activity says so instead of
         * leaving a new enabled alarm to be discovered later.
         */
        val createdSilently: Boolean = false,
        /** The row the caller created, so the notice can point at it. */
        val createdAlarmId: Long? = null
    ) : AlarmClockHandleResult
    data object Invalid : AlarmClockHandleResult
    data object Duplicate : AlarmClockHandleResult
    data object Unsupported : AlarmClockHandleResult
}

@Singleton
class AlarmClockIntentHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val calculator: NextAlarmCalculator,
    private val timerStore: TimerStore,
    private val deliveryGuard: AlarmClockIntentDeliveryGuard
) {
    suspend fun handle(intent: Intent?): AlarmClockHandleResult {
        val command = when (val parsed = AlarmClockIntentParser.parse(intent)) {
            is AlarmClockParseResult.Valid -> parsed.command
            AlarmClockParseResult.Invalid -> return AlarmClockHandleResult.Invalid
            AlarmClockParseResult.Unsupported -> return AlarmClockHandleResult.Unsupported
        }
        if (!deliveryGuard.claim(command.fingerprint)) return AlarmClockHandleResult.Duplicate

        return when (command) {
            is AlarmClockCommand.OpenAlarmEditor ->
                AlarmClockHandleResult.Handled(ROUTE_NEW_ALARM)
            is AlarmClockCommand.SetAlarm -> setAlarm(command)
            is AlarmClockCommand.DismissAlarm -> dismissAlarm(command.search)
            is AlarmClockCommand.SnoozeAlarm -> snoozeAlarm(command.durationMinutes)
            is AlarmClockCommand.OpenTimer -> AlarmClockHandleResult.Handled(ROUTE_TIMER)
            is AlarmClockCommand.SetTimer -> setTimer(command)
        }
    }

    private suspend fun setAlarm(command: AlarmClockCommand.SetAlarm): AlarmClockHandleResult {
        val draft = Alarm(
            hour = command.hour,
            minute = command.minute,
            label = command.label,
            repeatDays = command.repeatDays,
            ringtoneUri = command.ringtoneUri,
            vibrationEnabled = command.vibrate,
            isEnabled = true
        ).sanitized()
        val existing = repository.getEnabled().firstOrNull { alarm ->
            alarm.hour == draft.hour &&
                alarm.minute == draft.minute &&
                alarm.label == draft.label &&
                alarm.repeatDays == draft.repeatDays &&
                alarm.ringtoneUri == draft.ringtoneUri &&
                alarm.vibrationEnabled == draft.vibrationEnabled
        }
        val saved = if (existing != null) {
            existing
        } else {
            val id = repository.save(draft)
            val withId = draft.copy(id = id)
            val trigger = calculator.calculate(withId)
            repository.updateNextTrigger(id, trigger)
            val scheduled = withId.copy(nextTriggerTime = trigger)
            scheduler.schedule(scheduled)
            scheduled
        }
        return AlarmClockHandleResult.Handled(
            route = if (command.skipUi) null else "acx://navigate/alarm_edit/${saved.id}",
            // Only a new alarm is worth announcing; matching an existing
            // one changes nothing the user can see.
            createdSilently = command.skipUi && existing == null,
            createdAlarmId = saved.id
        )
    }

    private fun setTimer(command: AlarmClockCommand.SetTimer): AlarmClockHandleResult {
        val label = command.label.ifBlank { timerLabel(command.lengthSeconds) }
        val result = timerStore.startOrReuse(command.lengthSeconds.toLong(), label)
        if (result.created) {
            TimerAlarmScheduler.schedule(context, result.record.id, result.record.endElapsedRealtime)
            TimerNotifications.postRunning(context, result.record)
        }
        return AlarmClockHandleResult.Handled(if (command.skipUi) null else ROUTE_TIMER)
    }

    private suspend fun snoozeAlarm(durationMinutes: Int?): AlarmClockHandleResult {
        val active = AlarmService.activeAlarm.get() ?: return AlarmClockHandleResult.Handled()
        context.startService(
            AlarmFireDismissContract.snoozeServiceIntent(
                context = context,
                alarmId = active.alarmId,
                scheduledAt = active.scheduledAt,
                fireId = active.fireId,
                customMinutes = durationMinutes
            )
        )
        return AlarmClockHandleResult.Handled()
    }

    private suspend fun dismissAlarm(search: AlarmSearch): AlarmClockHandleResult {
        val active = AlarmService.activeAlarm.get()
        val resolution = resolveDismissTargets(search, active?.alarmId)

        // Whatever is ringing gets silenced whether or not the rest of the
        // request needs confirming. The person is standing over a ringing
        // phone: there is nothing hidden about stopping it, and making them
        // confirm through the alarm list first would be worse than useless.
        if (resolution.ringingTarget != null && active != null) {
            context.startService(
                AlarmFireDismissContract.dismissServiceIntent(
                    context = context,
                    alarmId = active.alarmId,
                    scheduledAt = active.scheduledAt,
                    fireId = active.fireId
                )
            )
        }
        if (resolution.needsSelectionUi) {
            return AlarmClockHandleResult.Handled(ROUTE_ALARMS)
        }
        resolution.alarms
            .filterNot { it.id == resolution.ringingTarget }
            .forEach { alarm -> dismissScheduledAlarm(alarm) }
        return AlarmClockHandleResult.Handled()
    }

    private suspend fun resolveDismissTargets(
        search: AlarmSearch,
        activeAlarmId: Long?
    ): DismissResolution {
        val enabled = repository.getEnabled()
        val activeAlarm = activeAlarmId?.let { id -> repository.getById(id) }
        val candidates = (enabled + listOfNotNull(activeAlarm)).distinctBy(Alarm::id)
        val matches = when (search) {
            is AlarmSearch.ById -> listOfNotNull(repository.getById(search.id))
            AlarmSearch.Active -> activeAlarm?.let(::listOf) ?: enabled
            AlarmSearch.Next -> activeAlarm?.let(::listOf)
                ?: listOfNotNull(repository.getNextAlarm())
            AlarmSearch.All -> candidates
            is AlarmSearch.Label -> candidates.filter { alarm ->
                alarm.label.contains(search.query, ignoreCase = true)
            }
            is AlarmSearch.Time -> candidates.filter { alarm ->
                (search.hour == null || alarm.hour == search.hour) &&
                    (search.minute == null || alarm.minute == search.minute)
            }
        }
        // A dismiss that names an id, or asks for everything, arrives from any
        // app holding SET_ALARM, which is a normal permission. Turning off an
        // alarm that is not currently ringing is not something a caller should
        // be able to do unseen, so those two go through the alarm list and let
        // the person decide. A single unambiguous match by time or label stays
        // immediate, as it always was.
        //
        // The ringing alarm is reported separately and is always dismissed:
        // gating that behind a confirmation would leave the phone ringing
        // while a screen asks permission to stop it.
        val ringingTarget = activeAlarmId?.takeIf { id -> matches.any { it.id == id } }
        val onlyTargetsRingingAlarm = ringingTarget != null && matches.size == 1
        val needsConfirmation = !onlyTargetsRingingAlarm &&
            (search is AlarmSearch.ById || search is AlarmSearch.All)
        val needsSelection = needsConfirmation ||
            (search !is AlarmSearch.All && matches.size > 1)
        return DismissResolution(
            alarms = if (needsSelection) emptyList() else matches,
            needsSelectionUi = needsSelection,
            ringingTarget = ringingTarget
        )
    }

    private suspend fun dismissScheduledAlarm(alarm: Alarm) {
        scheduler.cancel(alarm.id)
        if (!alarm.isRecurringSchedule) {
            repository.setEnabled(alarm.id, enabled = false, nextTrigger = 0L)
            return
        }
        val nextFromMillis = alarm.nextTriggerTime
            .coerceAtLeast(System.currentTimeMillis()) + 60_000L
        val nextFrom = Instant.ofEpochMilli(nextFromMillis).atZone(ZoneId.systemDefault())
        val nextTrigger = calculator.calculate(alarm, nextFrom)
        repository.updateNextTrigger(alarm.id, nextTrigger)
        scheduler.scheduleAt(alarm.copy(nextTriggerTime = nextTrigger), nextTrigger)
    }

    private fun timerLabel(totalSeconds: Int): String = buildString {
        val hours = totalSeconds / 3_600
        val minutes = totalSeconds % 3_600 / 60
        val seconds = totalSeconds % 60
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        if (seconds > 0) append("${seconds}s")
    }.trim()

    private data class DismissResolution(
        val alarms: List<Alarm>,
        val needsSelectionUi: Boolean,
        /** The id of the alarm ringing right now, when the request names it. */
        val ringingTarget: Long? = null
    )

    companion object {
        const val ROUTE_ALARMS = "acx://navigate/alarm_list"
        const val ROUTE_NEW_ALARM = "acx://navigate/alarm_edit/0"
        const val ROUTE_TIMER = "acx://navigate/timer"
    }
}
