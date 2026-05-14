package com.sysadmindoc.alarmclock.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayWearNextAlarmBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val preferencesManager: PreferencesManager,
) : WearNextAlarmBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val firingAlarmId = AtomicLong(-1L)

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            repository.observeNextAlarm()
                .combine(
                    preferencesManager.settings
                        .map { it.is24HourFormat }
                        .distinctUntilChanged()
                ) { alarm, is24HourFormat ->
                    alarm to is24HourFormat
                }
                .collect { (alarm, is24HourFormat) ->
                    publish(alarm, is24HourFormat)
                }
        }
    }

    override fun publishAlarmFiring(alarm: Alarm) {
        firingAlarmId.set(alarm.id)
        scope.launch {
            publish(alarm, preferencesManager.getCachedSettings().is24HourFormat)
        }
    }

    override fun publishAlarmIdle(alarmId: Long) {
        firingAlarmId.compareAndSet(alarmId, -1L)
        scope.launch {
            publish(repository.getNextAlarm(), preferencesManager.getCachedSettings().is24HourFormat)
        }
    }

    private fun publish(alarm: Alarm?, is24HourFormat: Boolean) {
        if (!isWearableApiAvailable()) return

        val now = System.currentTimeMillis()
        val activeAlarm = alarm?.takeIf { it.isEnabled && it.nextTriggerTime > now }
        val request = PutDataMapRequest.create(WearAlarmData.PATH_NEXT_ALARM)
        request.dataMap.apply {
            putLong(WearAlarmData.KEY_UPDATED_AT, now)
            putBoolean(WearAlarmData.KEY_HAS_ALARM, activeAlarm != null)
            if (activeAlarm == null) {
                putLong(WearAlarmData.KEY_ALARM_ID, -1L)
                putString(WearAlarmData.KEY_LABEL, "")
                putString(WearAlarmData.KEY_TIME_LABEL, "")
                putLong(WearAlarmData.KEY_TRIGGER_TIME, 0L)
                putBoolean(WearAlarmData.KEY_IS_FIRING, false)
            } else {
                putLong(WearAlarmData.KEY_ALARM_ID, activeAlarm.id)
                putString(
                    WearAlarmData.KEY_LABEL,
                    activeAlarm.label.ifBlank { "Alarm" }
                )
                putString(
                    WearAlarmData.KEY_TIME_LABEL,
                    formatTriggerTime(activeAlarm.nextTriggerTime, is24HourFormat)
                )
                putLong(WearAlarmData.KEY_TRIGGER_TIME, activeAlarm.nextTriggerTime)
                putBoolean(
                    WearAlarmData.KEY_IS_FIRING,
                    firingAlarmId.get() == activeAlarm.id
                )
            }
        }

        Wearable.getDataClient(context)
            .putDataItem(request.asPutDataRequest().setUrgent())
            .addOnFailureListener { e ->
                Log.w(TAG, "Wear next-alarm sync failed", e)
            }
    }

    private fun isWearableApiAvailable(): Boolean {
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    private fun formatTriggerTime(triggerTime: Long, is24HourFormat: Boolean): String {
        val pattern = if (is24HourFormat) "EEE HH:mm" else "EEE h:mm a"
        return Instant.ofEpochMilli(triggerTime)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(pattern))
    }

    companion object {
        private const val TAG = "WearAlarmBridge"
    }
}
