package com.sysadmindoc.alarmclock.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonDataException
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.remote.ReleaseApi
import com.sysadmindoc.alarmclock.domain.UpdateCheckPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.util.concurrent.TimeUnit
import retrofit2.HttpException

/**
 * Opt-in daily check of GitHub Releases. It reads only the latest release's
 * tag, posts one notification per new tag, and never downloads anything.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesManager: PreferencesManager,
    private val releaseApi: ReleaseApi
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = preferencesManager.getCurrentSettings()
        if (!settings.updateCheckEnabled) return Result.success()
        val tag = try {
            releaseApi.latest().tagName
        } catch (_: IOException) {
            // Offline or GitHub unreachable: say nothing, try again tomorrow.
            return Result.success()
        } catch (_: HttpException) {
            // Rate limited (60 an hour per IP) or no release published.
            return Result.success()
        } catch (_: JsonDataException) {
            // A response without a tag. Same as no answer.
            return Result.success()
        }
        if (!UpdateCheckPolicy.isNewer(tag, BuildConfig.VERSION_NAME)) return Result.success()
        if (tag == settings.updateCheckLastNotifiedTag) return Result.success()
        // Notifications off: remember nothing, so the notice shows once they're back on.
        if (UpdateNotice.post(applicationContext, tag)) {
            preferencesManager.update { it.copy(updateCheckLastNotifiedTag = tag) }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "update_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
