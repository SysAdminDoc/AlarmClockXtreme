package com.sysadmindoc.alarmclock.service

/**
 * The START_ALARM start id that Android holds for redelivery. A real alarm's
 * start returns START_REDELIVER_INTENT, so if the process dies mid-ring (low
 * memory kill, an OEM task killer) the system restarts the service with that
 * intent and the alarm rings again instead of going silent for good.
 *
 * Snooze, dismiss and a newer alarm taking over release it, so a ring the user
 * already finished never comes back. [release] is `stopSelfResult`, which drops
 * the held start without stopping the service while a newer start is open.
 */
internal class RedeliveredStart(private val release: (Int) -> Unit) {
    private var heldStartId = 0

    fun hold(startId: Int) {
        releaseHeld()
        heldStartId = startId
    }

    fun releaseHeld() {
        val startId = heldStartId
        if (startId <= 0) return
        heldStartId = 0
        release(startId)
    }
}
