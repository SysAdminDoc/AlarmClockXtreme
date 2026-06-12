package com.sysadmindoc.alarmclock.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.util.AlarmPublicText
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
                        .map {
                            WearPublishSettings(
                                is24HourFormat = it.is24HourFormat,
                                hideAlarmLabelsOnPublicSurfaces = it.hideAlarmLabelsOnPublicSurfaces
                            )
                        }
                        .distinctUntilChanged()
                ) { alarm, settings ->
                    alarm to settings
                }
                .collect { (alarm, settings) ->
                    publish(alarm, settings)
                }
        }
    }

    override fun publishAlarmFiring(alarm: Alarm) {
        firingAlarmId.set(alarm.id)
        scope.launch {
            publish(alarm, WearPublishSettings.from(preferencesManager.getCachedSettings()))
        }
    }

    override fun publishAlarmIdle(alarmId: Long) {
        firingAlarmId.compareAndSet(alarmId, -1L)
        scope.launch {
            val settings = WearPublishSettings.from(preferencesManager.getCachedSettings())
            publish(repository.getNextAlarm(), settings)
        }
    }

    private fun publish(alarm: Alarm?, settings: WearPublishSettings) {
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
                    AlarmPublicText.requiredAlarmLabel(
                        label = activeAlarm.label,
                        hideLabel = settings.hideAlarmLabelsOnPublicSurfaces
                    )
                )
                putString(
                    WearAlarmData.KEY_TIME_LABEL,
                    formatTriggerTime(activeAlarm.nextTriggerTime, settings.is24HourFormat)
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

private data class WearPublishSettings(
    val is24HourFormat: Boolean,
    val hideAlarmLabelsOnPublicSurfaces: Boolean
) {
    companion object {
        fun from(settings: AppSettings): WearPublishSettings {
            return WearPublishSettings(
                is24HourFormat = settings.is24HourFormat,
                hideAlarmLabelsOnPublicSurfaces = settings.hideAlarmLabelsOnPublicSurfaces
            )
        }
    }
}
