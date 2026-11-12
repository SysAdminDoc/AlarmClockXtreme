package com.sysadmindoc.alarmclock.worker

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.data.repository.CalendarRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Re-checks alarms that skip days with a calendar tag whenever the calendar
 * provider reports a change, so adding or deleting a tagged event moves the
 * alarm without waiting for its next ring.
 *
 * A content-URI trigger fires once, so each run arms the next one while any
 * tagged alarm is left. WorkManager keeps the pending request across reboots.
 * [AlarmScheduler] arms it whenever it schedules a tagged alarm, and the boot
 * reschedule runs [AlarmScheduler.rescheduleCalendarTaggedAlarms] once to
 * catch edits made while the phone was off.
 */
@HiltWorker
class CalendarSkipWatchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val alarmScheduler: AlarmScheduler,
    private val calendarRepository: CalendarRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val tagged = alarmScheduler.rescheduleCalendarTaggedAlarms()
            if (tagged > 0 && calendarRepository.hasReadPermission()) {
                // APPEND_OR_REPLACE queues the next watch behind this run;
                // KEEP would drop it because this request is still running.
                enqueueWatch(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
            }
            Log.i(TAG, "Rechecked $tagged calendar-tagged alarms")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recheck calendar-tagged alarms", e)
            // Keep watching: one failed read should not end calendar skips.
            enqueueWatch(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
            Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "calendar_skip_watch"
        private const val TAG = "CalendarSkipWatch"

        /** Arms the calendar watch unless one is already waiting. */
        fun watch(context: Context) {
            enqueueWatch(context.applicationContext, ExistingWorkPolicy.KEEP)
        }

        private fun enqueueWatch(context: Context, policy: ExistingWorkPolicy) {
            val constraints = Constraints.Builder()
                .addContentUriTrigger(CalendarContract.CONTENT_URI, true)
                // Calendar sync writes in bursts; wait for it to settle.
                .setTriggerContentUpdateDelay(10, TimeUnit.SECONDS)
                .setTriggerContentMaxDelay(2, TimeUnit.MINUTES)
                .build()
            val request = OneTimeWorkRequestBuilder<CalendarSkipWatchWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
        }
    }
}
