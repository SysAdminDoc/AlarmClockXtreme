package com.sysadmindoc.alarmclock.service

import java.util.concurrent.atomic.AtomicReference

/**
 * Which alarm's ringing screen is in the foreground. The ringing screen reports
 * itself from onResume and onStop, and the service reads it after putting the
 * screen back over Spotify: Android blocks a background activity start without
 * throwing, so this is the only way to know the launch landed.
 */
internal object FiringScreenPresence {
    private val shownFireId = AtomicReference<String?>(null)

    fun shown(fireId: String) {
        if (fireId.isNotBlank()) shownFireId.set(fireId)
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
}
