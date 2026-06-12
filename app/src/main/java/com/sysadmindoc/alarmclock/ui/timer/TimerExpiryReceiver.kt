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
        TimerNotifications.postFinished(appContext, finished.id, finished.label)
    }
}
