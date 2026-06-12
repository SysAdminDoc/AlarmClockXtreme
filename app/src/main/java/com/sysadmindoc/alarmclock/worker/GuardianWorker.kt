package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.BuildConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * v1.2.0: Guardian Angel worker.
 *
 * If the alarm was not dismissed within `guardianDelaySec`, escalates to the
 * emergency contact. F-Droid builds can send automatic SMS after explicit user
 * opt-in. Play builds never use direct SMS; they open a prefilled SMS composer
 * and only fall back to call/dialer if no SMS app can handle the intent.
 *
 * Both actions degrade gracefully when permission or platform support is
 * missing:
 *  - Direct SMS is used only by the F-Droid flavor when SEND_SMS is granted.
 *  - Without CALL_PHONE permission the worker falls back to ACTION_DIAL, which
 *    pre-fills the dialer rather than placing the call automatically.
 *
 * The phone number is sanitised before it is used in tel:/smsto: targets.
 */
@HiltWorker
class GuardianWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val rawPhone = inputData.getString("guardian_phone")?.trim().orEmpty()
        if (rawPhone.isBlank()) return Result.success()
        val phone = GuardianEscalationPolicy.sanitisePhone(rawPhone) ?: return Result.success()
        val label = inputData.getString("alarm_label") ?: "Alarm"
        val message = GuardianEscalationPolicy.buildMessage(label)
        val canSendDirectSms = GuardianEscalationPolicy.canSendDirectSms(
            flavor = BuildConfig.FLAVOR,
            hasSendSmsPermission = hasPermission(Manifest.permission.SEND_SMS)
        )

        val directSmsSent = canSendDirectSms && sendDirectSms(phone, message)
        val composerOpened = if (directSmsSent) {
            false
        } else {
            openSmsComposer(phone, message)
        }

        if (directSmsSent || !composerOpened) {
            openEmergencyCall(phone)
        }

        return Result.success()
    }

    private fun sendDirectSms(phone: String, message: String): Boolean =
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager?.sendTextMessage(phone, null, message, null, null)
            smsManager != null
        } catch (_: Exception) {
            false
        }

    private fun openSmsComposer(phone: String, message: String): Boolean =
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", phone, null)).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            true
        } catch (_: Exception) {
            false
        }

    private fun openEmergencyCall(phone: String) {
        try {
            val callAction = if (hasPermission(Manifest.permission.CALL_PHONE)) {
                Intent.ACTION_CALL
            } else {
                Intent.ACTION_DIAL
            }
            val callIntent = Intent(callAction, Uri.fromParts("tel", phone, null)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
        } catch (_: Exception) {
            // Some OEMs block background-started activities; we can't surface UX from
            // a worker, but we've already attempted the SMS path above when possible.
        }
    }

    private fun hasPermission(name: String): Boolean =
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED
}
