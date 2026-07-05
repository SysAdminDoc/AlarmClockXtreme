package com.sysadmindoc.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.service.AlarmFireDismissContract
import com.sysadmindoc.alarmclock.service.AlarmService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * v1.4.0: Safety net for auto-silenced alarms.
 *
 * If the most recent alarm auto-silenced within the last 10 minutes and the
 * user has the repeat-missed setting enabled, re-fire that alarm once when
 * the user unlocks the device or unplugs it from power. Clears the persisted
 * state on every fire so a single missed alarm can only re-trigger once.
 */
class MissedAlarmUnlockReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (!MissedAlarmReplayPolicy.isReplayTrigger(action)) return
        val incidentSource = MissedAlarmReplayPolicy.sourceForTrigger(action)

        val pending = goAsync()
        scope.launch {
            try {
                // v1.6.3: 25 s was a real ANR risk — `goAsync()` extends the
                // BroadcastReceiver window only to ~10 s on most Android
                // versions, so a 25 s timeout was guaranteed to ANR before it
                // ever fired. Match BootReceiver's v1.5.4 ceiling of 8 s, which
                // safely sits under the goAsync() limit while comfortably
                // covering any DataStore / Room lookup.
                withTimeout(8_000L) {
                    val ep = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        AlarmClockApp.AppEntryPoint::class.java
                    )
                    // Consult the current preference snapshot. If the user turned the
                    // feature off since the miss was recorded, drop the record and bail.
                    // (Also drops stale records older than the 10-minute window.)
                    val prefsMgr = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        PreferencesEntryPoint::class.java
                    ).preferencesManager()
                    val settings = prefsMgr.getCurrentSettings()

                    val store = context.applicationContext
                        .getSharedPreferences("missed_alarm_state", Context.MODE_PRIVATE)
                    val at = store.getLong("last_missed_at", 0L)
                    val id = store.getLong("last_missed_id", -1L)

                    // v1.5.2: Single source of truth for the replay decision lives
                    // in [MissedAlarmReplayPolicy] so it can be unit-tested without
                    // BroadcastReceiver / Hilt wiring.
                    val decision = MissedAlarmReplayPolicy.shouldReplay(
                        repeatMissedEnabled = settings.repeatMissedAlarms,
                        lastMissedAtMs = at,
                        lastMissedId = id,
                        alarmCurrentlyFiringId = AlarmService.activeAlarmId,
                        nowMs = System.currentTimeMillis()
                    )
                    if (decision.shouldClearState) {
                        store.edit().clear().commit()
                    }
                    if (!decision.shouldReplay) return@withTimeout

                    // Policy has already cleared the record above; if the DB
                    // row vanished between the record write and now, we just
                    // silently drop the replay.
                    val alarm = ep.alarmRepository().getById(id) ?: return@withTimeout

                    val fireId = AlarmFireDismissContract.fireId(alarm.id, at)
                    val fireIntent = AlarmFireDismissContract.startServiceIntent(context, alarm.id, at, fireId)
                    try {
                        context.startForegroundService(fireIntent)
                        ep.alarmIncidentRepository().record(
                            alarmId = alarm.id,
                            fireId = fireId,
                            scheduledAt = at,
                            type = AlarmIncidentEvent.TYPE_BROADCAST,
                            status = AlarmIncidentEvent.STATUS_REQUESTED,
                            reasonCode = "MISSED_REPLAY_REQUESTED",
                            source = incidentSource
                        )
                    } catch (e: Exception) {
                        Log.e("MissedAlarmUnlockReceiver",
                            "startForegroundService failed for replay alarm ${alarm.id}", e)
                        ep.alarmIncidentRepository().record(
                            alarmId = alarm.id,
                            fireId = fireId,
                            scheduledAt = at,
                            type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                            status = AlarmIncidentEvent.STATUS_FAILED,
                            reasonCode = "MISSED_REPLAY_START_FAILED_${e.javaClass.simpleName}",
                            source = incidentSource
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("MissedAlarmUnlockReceiver", "Timed out evaluating missed alarm replay", e)
            } catch (e: Exception) {
                Log.e("MissedAlarmUnlockReceiver", "Unexpected error in missed alarm replay", e)
            } finally {
                pending.finish()
            }
        }
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface PreferencesEntryPoint {
        fun preferencesManager(): com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
    }
}
