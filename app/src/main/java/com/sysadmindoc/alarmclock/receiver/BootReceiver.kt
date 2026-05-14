package com.sysadmindoc.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sysadmindoc.alarmclock.worker.BootRescheduleWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reschedules all enabled alarms after device boot, app update, or a clock
 * change (TIME_SET / TIMEZONE_CHANGED / DATE_CHANGED). AlarmManager intents
 * are lost on reboot, so this is essential.
 *
 * v1.5.1 additions:
 * - Clears the `missed_alarm_state` prefs on BOOT_COMPLETED so a stale
 *   "last missed at" timestamp from before the reboot doesn't trigger
 *   [MissedAlarmUnlockReceiver] on the user's first unlock.
 * - Wraps [AlarmScheduler.rescheduleAll] in a `withTimeout` so a
 *   corrupt DB or storage lock can't pin the PendingResult forever.
 *
 * v1.5.4: Ceiling tightened from 30s → 8s. `goAsync()` extends the
 * BroadcastReceiver ANR window but only up to ~10 seconds on most
 * Android versions — a 30-second timeout could trip ANR before the
 * timeout fires. 8 seconds leaves headroom while still comfortably
 * covering realistic schedules.
 *
 * v1.10.0: Receiver work is now limited to quick state cleanup and enqueueing
 * [BootRescheduleWorker]. The actual alarm batch runs in WorkManager, outside
 * the receiver ANR window, so users with large alarm libraries are not racing
 * an 8-second broadcast timeout at boot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_DATE_CHANGED) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // v1.5.1: A reboot invalidates the "user will unlock any second now"
                // semantics of repeat-missed-alarm. Clear the state so the receiver
                // doesn't fire a stale miss after the user pressed the power button
                // deliberately.
                if (action == Intent.ACTION_BOOT_COMPLETED ||
                    action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    try {
                        appContext.getSharedPreferences("missed_alarm_state", Context.MODE_PRIVATE)
                            .edit().clear().apply()
                    } catch (_: Exception) {
                        // Prefs backup failure isn't fatal — continue to reschedule.
                    }
                }

                val forceRecalculate = action == Intent.ACTION_TIME_CHANGED ||
                    action == Intent.ACTION_TIMEZONE_CHANGED ||
                    action == Intent.ACTION_DATE_CHANGED
                BootRescheduleWorker.enqueue(
                    context = appContext,
                    sourceAction = action,
                    forceRecalculate = forceRecalculate
                )
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to enqueue alarm reschedule", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
