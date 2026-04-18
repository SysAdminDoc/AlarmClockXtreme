package com.sysadmindoc.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * v1.4.0: Safety net for auto-silenced alarms.
 *
 * If the most recent alarm auto-silenced within the last 10 minutes and the
 * user has the repeat-missed setting enabled, re-fire that alarm once when
 * the user unlocks the device. Clears the persisted state on every fire so
 * a single missed alarm can only re-trigger once.
 */
class MissedAlarmUnlockReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        val pending = goAsync()
        scope.launch {
            try {
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
                val ageMs = System.currentTimeMillis() - at
                // v1.5.1: half-open window so a second unlock at exactly the
                // boundary can't stack a replay on top of the next alarm.
                val withinWindow = at > 0 && ageMs in 0 until (10 * 60 * 1000L)

                if (!settings.repeatMissedAlarms || !withinWindow || id <= 0L) {
                    store.edit().clear().apply()
                    return@launch
                }

                // v1.5.1: Don't replay the miss if an alarm is already firing
                // right now. Otherwise the foreground service could be started
                // twice (once for the live alarm, once for the replay) and
                // audio would conflict.
                if (com.sysadmindoc.alarmclock.service.AlarmService.activeAlarmId != -1L) {
                    store.edit().clear().apply()
                    return@launch
                }

                val alarm = ep.alarmRepository().getById(id) ?: run {
                    store.edit().clear().apply()
                    return@launch
                }

                // Clear the record first so the re-fired alarm can't retrigger itself
                // if the user dismisses it quickly and unlocks again.
                store.edit().clear().apply()

                val fireIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START_ALARM
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                }
                context.startForegroundService(fireIntent)
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
