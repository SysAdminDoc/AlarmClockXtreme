package com.sysadmindoc.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

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
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_DATE_CHANGED) return

        val appContext = context.applicationContext

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
                // Prefs backup failure isn't fatal — swallow and continue.
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val forceRecalculate = action == Intent.ACTION_TIME_CHANGED ||
                    action == Intent.ACTION_TIMEZONE_CHANGED ||
                    action == Intent.ACTION_DATE_CHANGED
                // v1.5.4: Enforce a reasonable ceiling. rescheduleAll() is
                // typically sub-second, but a corrupt DB page has been seen
                // to hang it. 8 s is safely under the ~10 s goAsync ANR
                // ceiling and long enough for any realistic schedule.
                withTimeout(8_000L) {
                    alarmScheduler.rescheduleAll(forceRecalculate = forceRecalculate)
                }
            } catch (e: TimeoutCancellationException) {
                android.util.Log.e("BootReceiver", "Timed out rescheduling alarms", e)
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Failed to reschedule alarms", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
