package com.sysadmindoc.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmFireDismissContract
import com.sysadmindoc.alarmclock.service.AlarmTestRun

/**
 * Notification actions and the snooze re-ring of a test run. Kept apart from
 * [SnoozeReceiver] and [DismissReceiver] so a test never reaches the incident
 * log those write to.
 */
class AlarmTestRunReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0L)
        val fireId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: AlarmFireDismissContract.fireId(alarmId, scheduledAt)
        when (intent.action) {
            AlarmTestRun.ACTION_END -> AlarmTestRun.end(context)
            AlarmTestRun.ACTION_RING -> if (alarmId > 0L) {
                AlarmTestRun.ringAgain(context, alarmId, scheduledAt, fireId)
            }
            AlarmTestRun.ACTION_SNOOZE -> if (alarmId > 0L) {
                startService(
                    context,
                    AlarmFireDismissContract.snoozeServiceIntent(
                        context = context,
                        alarmId = alarmId,
                        scheduledAt = scheduledAt,
                        fireId = fireId,
                        testRun = true
                    )
                )
            }
            AlarmTestRun.ACTION_DISMISS -> if (alarmId > 0L) {
                startService(
                    context,
                    AlarmFireDismissContract.dismissServiceIntent(
                        context = context,
                        alarmId = alarmId,
                        scheduledAt = scheduledAt,
                        fireId = fireId,
                        testRun = true
                    )
                )
            }
        }
    }

    private fun startService(context: Context, serviceIntent: Intent) {
        try {
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e("AlarmTestRunReceiver", "startForegroundService failed for a test alarm", e)
        }
    }
}
