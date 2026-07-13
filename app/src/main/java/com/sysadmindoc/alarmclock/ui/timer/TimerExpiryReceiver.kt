package com.sysadmindoc.alarmclock.ui.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TimerExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimerAlarmScheduler.ACTION_TIMER_EXPIRED) return
        val timerId = intent.getIntExtra(TimerAlarmScheduler.EXTRA_TIMER_ID, -1)
        if (timerId <= 0) return

        val appContext = context.applicationContext
        val finished = TimerStore(appContext).markFinished(timerId)
        if (finished == null) {
            Log.w("TimerExpiryReceiver", "Timer $timerId expired but no persisted record exists")
            return
        }
        if (TimerAlertState.uiWillHandleSound()) {
            // A live ViewModel is present (foreground or backgrounded app) and
            // plays the finish sound + posts the notification itself.
            TimerNotifications.postFinished(appContext, finished.id, finished.label)
        } else {
            // Process was killed: nothing else will alert, so start the foreground
            // service to actually ring instead of posting a silent notification.
            TimerAlarmService.fire(appContext, finished.id, finished.label)
        }
    }
}
