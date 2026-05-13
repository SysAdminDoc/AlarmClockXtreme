package com.sysadmindoc.alarmclock.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Re-arms enabled alarms after exact-alarm access is granted. Runs outside the
 * broadcast receiver so large alarm sets are not constrained by the receiver
 * ANR window.
 */
@HiltWorker
class ExactAlarmPermissionRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val alarmScheduler: AlarmScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            withTimeout(RESCHEDULE_TIMEOUT_MS) {
                alarmScheduler.rescheduleAll(forceRecalculate = true)
            }
            Log.i(TAG, "Rescheduled enabled alarms after exact-alarm permission grant")
            Result.success()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timed out rescheduling alarms after exact-alarm permission grant", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule alarms after exact-alarm permission grant", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "exact_alarm_permission_reschedule"
        private const val TAG = "ExactAlarmPermWorker"
        private const val MAX_ATTEMPTS = 3
        private const val RESCHEDULE_TIMEOUT_MS = 20_000L
    }
}
