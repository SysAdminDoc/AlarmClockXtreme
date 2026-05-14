package com.sysadmindoc.alarmclock.wear

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.receiver.DismissReceiver
import com.sysadmindoc.alarmclock.receiver.SkipNextReceiver
import com.sysadmindoc.alarmclock.receiver.SnoozeReceiver
import com.sysadmindoc.alarmclock.service.AlarmService

class WearAlarmActionListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val alarmId = runCatching {
            DataMap.fromByteArray(messageEvent.data)
                .getLong(WearAlarmData.KEY_ALARM_ID, -1L)
        }.getOrDefault(-1L)
        if (alarmId <= 0L) return

        when (messageEvent.path) {
            WearAlarmData.PATH_ACTION_SKIP -> {
                sendBroadcast(Intent(applicationContext, SkipNextReceiver::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                })
            }
            WearAlarmData.PATH_ACTION_SNOOZE -> forwardFiringOnly(
                alarmId = alarmId,
                receiver = SnoozeReceiver::class.java,
                actionName = "snooze"
            )
            WearAlarmData.PATH_ACTION_DISMISS -> forwardFiringOnly(
                alarmId = alarmId,
                receiver = DismissReceiver::class.java,
                actionName = "dismiss"
            )
        }
    }

    private fun forwardFiringOnly(
        alarmId: Long,
        receiver: Class<*>,
        actionName: String,
    ) {
        if (AlarmService.activeAlarmId != alarmId) {
            Log.i(TAG, "Ignored Wear $actionName for non-firing alarm $alarmId")
            return
        }
        sendBroadcast(Intent(applicationContext, receiver).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        })
    }

    companion object {
        private const val TAG = "WearAlarmAction"
    }
}
