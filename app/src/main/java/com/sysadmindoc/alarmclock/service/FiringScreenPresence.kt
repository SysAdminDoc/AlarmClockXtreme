package com.sysadmindoc.alarmclock.service

import java.util.concurrent.atomic.AtomicReference

/**
 * Which alarm's ringing screen is in the foreground. The ringing screen reports
 * itself from onResume and onStop, and the service reads it after launching the
 * screen with the app open or putting it back over Spotify: Android blocks a
 * background activity start without throwing, so this is the only way to know
 * the launch landed.
 */
internal object FiringScreenPresence {
    private val shownFireId = AtomicReference<String?>(null)
    private val seenFireId = AtomicReference<String?>(null)

    fun shown(fireId: String) {
        if (fireId.isNotBlank()) {
            shownFireId.set(fireId)
            seenFireId.set(fireId)
        }
    }

    /** A screen for an older fire stopping late must not hide a newer one. */
    fun hidden(fireId: String) {
        val current = shownFireId.get() ?: return
        if (current == fireId) shownFireId.compareAndSet(current, null)
    }

    fun isShowing(fireId: String): Boolean =
        fireId.isNotBlank() && shownFireId.get() == fireId

    /** How long the service waits after a relaunch before it looks. */
    const val RELAUNCH_CHECK_DELAY_MS = 2_000L

    enum class RelaunchOutcome(val reasonCode: String?) {
        RETURNED("SPOTIFY_FALLBACK_SCREEN_RETURNED"),
        BLOCKED("SPOTIFY_FALLBACK_SCREEN_BLOCKED"),

        /** The alarm was snoozed, dismissed or replaced in the meantime. */
        MOOT(null)
    }

    fun relaunchOutcome(stillRinging: Boolean, fireId: String): RelaunchOutcome = when {
        !stillRinging -> RelaunchOutcome.MOOT
        isShowing(fireId) -> RelaunchOutcome.RETURNED
        else -> RelaunchOutcome.BLOCKED
    }

    /**
     * A new start for this fire. A wake-up check's re-ring and a redelivered
     * start reuse the fire id, so its screen has to come up again to count.
     */
    fun ringStarted(fireId: String) {
        val seen = seenFireId.get() ?: return
        if (seen == fireId) seenFireId.compareAndSet(seen, null)
    }

    /** Whether this fire's ringing screen came up since it started, even if it's gone again. */
    fun hasShown(fireId: String): Boolean =
        fireId.isNotBlank() && seenFireId.get() == fireId

    /**
     * After the first launch, a screen that came up and was left counts as
     * shown, and so does one still up from before a redelivered start.
     */
    fun firstLaunchOutcome(stillRinging: Boolean, fireId: String): RelaunchOutcome = when {
        !stillRinging -> RelaunchOutcome.MOOT
        isShowing(fireId) || hasShown(fireId) -> RelaunchOutcome.RETURNED
        else -> RelaunchOutcome.BLOCKED
    }
}
