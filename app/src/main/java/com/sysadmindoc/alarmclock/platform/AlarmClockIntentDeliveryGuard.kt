package com.sysadmindoc.alarmclock.platform

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmClockIntentDeliveryGuard @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @SuppressLint("ApplySharedPref")
    fun claim(fingerprint: String, nowElapsed: Long = SystemClock.elapsedRealtime()): Boolean =
        synchronized(CLAIM_LOCK) {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(fingerprint.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte) }
            val priorDigest = prefs.getString(KEY_DIGEST, null)
            val priorElapsed = prefs.getLong(KEY_ELAPSED, -1L)
            val isDuplicate = digest == priorDigest &&
                priorElapsed >= 0L &&
                nowElapsed >= priorElapsed &&
                nowElapsed - priorElapsed <= DUPLICATE_WINDOW_MS
            if (isDuplicate) return@synchronized false
            prefs.edit()
                .putString(KEY_DIGEST, digest)
                .putLong(KEY_ELAPSED, nowElapsed)
                .commit()
            true
        }

    companion object {
        private const val PREFS_NAME = "alarm_clock_intent_delivery"
        private const val KEY_DIGEST = "last_digest"
        private const val KEY_ELAPSED = "last_elapsed"
        private const val DUPLICATE_WINDOW_MS = 5_000L
        private val CLAIM_LOCK = Any()
    }
}
