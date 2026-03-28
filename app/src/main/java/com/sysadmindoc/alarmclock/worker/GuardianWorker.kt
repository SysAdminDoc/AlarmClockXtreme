package com.sysadmindoc.alarmclock.worker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * v1.2.0: Guardian Angel worker.
 * If the alarm was not dismissed within [guardianDelaySec], sends an SMS
 * and attempts to call the emergency contact.
 */
@HiltWorker
class GuardianWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val phone = inputData.getString("guardian_phone") ?: return Result.success()
        val label = inputData.getString("alarm_label") ?: "Alarm"

        // Send SMS
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager?.sendTextMessage(
                phone, null,
                "AlarmClockXtreme Guardian Alert: $label was not dismissed. Please check on the user.",
                null, null
            )
        } catch (_: Exception) {}

        // Initiate phone call
        try {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
        } catch (_: Exception) {}

        return Result.success()
    }
}
