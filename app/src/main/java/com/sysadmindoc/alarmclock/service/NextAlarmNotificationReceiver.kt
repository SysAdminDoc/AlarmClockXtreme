package com.sysadmindoc.alarmclock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sysadmindoc.alarmclock.AlarmClockApp
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Fires when a next-alarm notification held back by the lead-time setting is
 * due, so it appears on time even if the app was asleep or not running.
 */
class NextAlarmNotificationReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                withTimeout(8_000L) {
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        AlarmClockApp.AppEntryPoint::class.java
                    ).nextAlarmNotifier().refreshNow()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
