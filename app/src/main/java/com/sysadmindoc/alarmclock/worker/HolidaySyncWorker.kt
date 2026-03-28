package com.sysadmindoc.alarmclock.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.data.repository.HolidayRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * F13: Periodic worker that refreshes the public holiday cache.
 * Scheduled weekly from AlarmClockApp on startup.
 */
@HiltWorker
class HolidaySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val holidayRepository: HolidayRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            holidayRepository.refresh()
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
