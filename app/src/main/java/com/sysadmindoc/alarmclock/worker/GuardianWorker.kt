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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * v1.2.0: Guardian Angel worker.
 *
 * If the alarm was not dismissed within `guardianDelaySec`, sends an SMS and
 * attempts to call the emergency contact. Both actions degrade gracefully when
 * permission is missing:
 *  - SMS without SEND_SMS permission is silently dropped (the user enables the
 *    permission per the README's Guardian setup steps).
 *  - Without CALL_PHONE permission the worker falls back to ACTION_DIAL, which
 *    pre-fills the dialer rather than placing the call automatically.
 *
 * The phone number is sanitised to the characters legal for `tel:` URIs
 * (digits, +, -, *, #) before being interpolated, to defend against
 * malformed input crashing `Uri.parse` or being misinterpreted by other apps.
 */
@HiltWorker
class GuardianWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val rawPhone = inputData.getString("guardian_phone")?.trim().orEmpty()
        if (rawPhone.isBlank()) return Result.success()
        val phone = sanitisePhone(rawPhone) ?: return Result.success()
        val label = inputData.getString("alarm_label") ?: "Alarm"

        // SMS — silently no-op if permission missing or SmsManager unavailable.
        if (hasPermission(Manifest.permission.SEND_SMS)) {
            try {
                val smsManager = context.getSystemService(SmsManager::class.java)
                smsManager?.sendTextMessage(
                    phone, null,
                    "AlarmClockXtreme Guardian Alert: $label was not dismissed. Please check on the user.",
                    null, null
                )
            } catch (_: Exception) { /* defensive: never crash from optional notify */ }
        }

        // Call (or fall back to dialer if direct-call permission missing).
        try {
            val callAction = if (hasPermission(Manifest.permission.CALL_PHONE)) {
                Intent.ACTION_CALL
            } else {
                Intent.ACTION_DIAL
            }
            val callIntent = Intent(callAction, Uri.parse("tel:$phone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
        } catch (_: Exception) {
            // Some OEMs block background-started activities; we can't surface UX from
            // a worker, but we've already attempted the SMS path above.
        }

        return Result.success()
    }

    private fun hasPermission(name: String): Boolean =
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED

    /**
     * Restrict the phone number to characters legal in a `tel:` URI per RFC 3966
     * (digits, +, -, *, #, and visual separators). Returns null if nothing usable
     * remains, so we don't dial garbage.
     */
    private fun sanitisePhone(raw: String): String? {
        val cleaned = buildString {
            for (c in raw) {
                if (c.isDigit() || c == '+' || c == '-' || c == '*' || c == '#') append(c)
            }
        }
        // Require at least 3 digits to be a plausibly dialable number.
        return if (cleaned.count { it.isDigit() } >= 3) cleaned else null
    }
}
