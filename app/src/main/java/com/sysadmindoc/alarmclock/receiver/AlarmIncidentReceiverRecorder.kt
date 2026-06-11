package com.sysadmindoc.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal data class ReceiverAlarmIncident(
    val alarmId: Long,
    val fireId: String,
    val scheduledAt: Long,
    val type: String,
    val status: String,
    val reasonCode: String,
    val source: String
)

internal fun BroadcastReceiver.recordAlarmIncidentAsync(
    context: Context,
    alarmId: Long,
    fireId: String,
    scheduledAt: Long,
    type: String,
    status: String,
    reasonCode: String,
    source: String
) {
    recordAlarmIncidentsAsync(
        context = context,
        events = listOf(
            ReceiverAlarmIncident(
                alarmId = alarmId,
                fireId = fireId,
                scheduledAt = scheduledAt,
                type = type,
                status = status,
                reasonCode = reasonCode,
                source = source
            )
        )
    )
}

internal fun BroadcastReceiver.recordAlarmIncidentsAsync(
    context: Context,
    events: List<ReceiverAlarmIncident>
) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AlarmClockApp.AppEntryPoint::class.java
            )
            events.forEach { event ->
                entryPoint.alarmIncidentRepository().record(
                    alarmId = event.alarmId,
                    fireId = event.fireId.ifBlank {
                        AlarmIncidentEvent.fireIdFor(event.alarmId, event.scheduledAt)
                    },
                    scheduledAt = event.scheduledAt,
                    type = event.type,
                    status = event.status,
                    reasonCode = event.reasonCode,
                    source = event.source
                )
            }
        } finally {
            pending.finish()
        }
    }
}
