package com.sysadmindoc.alarmclock.receiver

import android.content.Context
import android.os.PowerManager
import android.util.Log

/**
 * Keeps the CPU awake long enough for an AlarmManager broadcast to hand off to
 * its foreground service during a cold process start. The timeout is
 * deliberately short: it bridges the receiver/service transition without
 * becoming another long-lived alarm wake lock.
 */
internal object AlarmDeliveryWakeLock {
    const val TIMEOUT_MS = 15_000L

    private const val TAG = "AlarmDeliveryWakeLock"
    private const val LOCK_TAG = "AlarmClockXtreme::AlarmDeliveryWakeLock"

    fun acquire(context: Context): PowerManager.WakeLock? {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return null
        val wakeLock = try {
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_TAG).apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Alarm delivery wake lock creation failed", e)
            return null
        }

        return try {
            wakeLock.acquire(TIMEOUT_MS)
            wakeLock
        } catch (e: Exception) {
            release(wakeLock)
            Log.w(TAG, "Alarm delivery wake lock acquisition failed", e)
            null
        }
    }

    fun release(wakeLock: PowerManager.WakeLock?) {
        try {
            if (wakeLock?.isHeld == true) wakeLock.release()
        } catch (e: Exception) {
            Log.w(TAG, "Alarm delivery wake lock release failed", e)
        }
    }
}
