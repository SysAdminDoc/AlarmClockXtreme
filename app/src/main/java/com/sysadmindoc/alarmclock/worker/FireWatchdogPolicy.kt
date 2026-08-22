package com.sysadmindoc.alarmclock.worker

/**
 * Pure decision function for the proactive fire watchdog.
 *
 * A watchdog check is enqueued [WATCHDOG_DELAY_MS] after every scheduled alarm
 * fire. When it runs it asks: did AlarmManager actually deliver this fire? The
 * only writer of a `BROADCAST` incident is `AlarmReceiver`, and only on real
 * delivery, so a zero broadcast count means the alarm was silently suppressed
 * (the Pixel "missed alarm — unknown reason" / OEM-Doze failure class). In that
 * case — and only then — the alarm is re-fired.
 *
 * Extracted from [FireWatchdogWorker] so the guards can be unit-tested without
 * WorkManager / Hilt / a real foreground service. The worker holds no other
 * policy; it just dispatches the side effect this returns.
 */
object FireWatchdogPolicy {

    /** How long after the scheduled fire the watchdog check runs. */
    const val WATCHDOG_DELAY_MS: Long = 2 * 60 * 1000L

    /**
     * A re-fire is only sensible inside a bounded window. Below [MIN_AGE_MS] the
     * original fire may still be arriving; past [MAX_AGE_MS] the user has moved
     * on (mirrors [com.sysadmindoc.alarmclock.receiver.MissedAlarmReplayPolicy]'s
     * 10-minute ceiling, with headroom for the 2-minute check delay).
     */
    const val MIN_AGE_MS: Long = 30 * 1000L
    const val MAX_AGE_MS: Long = 15 * 60 * 1000L

    /**
     * @param repeatMissedEnabled the existing "repeat missed alarms" setting;
     *   the watchdog is another missed-alarm recovery mechanism, so it shares
     *   that opt-in rather than adding a second toggle.
     * @param alarmExists whether the alarm row still exists.
     * @param isEnabled whether the alarm is still enabled. A one-shot is
     *   auto-disabled only *after* it fires, so a still-enabled one-shot that
     *   has no broadcast genuinely never rang.
     * @param deliveryCount BROADCAST plus FOREGROUND_SERVICE incidents for this
     *   occurrence (0 = the alarm never reached the user).
     * @param scheduledAtMs the occurrence's scheduled fire time.
     * @param nowMs injected wall clock.
     */
    fun decide(
        repeatMissedEnabled: Boolean,
        alarmExists: Boolean,
        isEnabled: Boolean,
        deliveryCount: Int,
        scheduledAtMs: Long,
        nowMs: Long
    ): Decision {
        if (!repeatMissedEnabled) return Decision.SKIP_DISABLED_SETTING
        if (!alarmExists) return Decision.SKIP_NO_ALARM
        if (!isEnabled) return Decision.SKIP_DISABLED
        if (deliveryCount > 0) return Decision.SKIP_ALREADY_FIRED
        if (scheduledAtMs <= 0L) return Decision.SKIP_STALE
        val age = nowMs - scheduledAtMs
        if (age < MIN_AGE_MS || age > MAX_AGE_MS) return Decision.SKIP_STALE
        return Decision.REFIRE
    }

    enum class Decision(val shouldRefire: Boolean, val reasonCode: String) {
        REFIRE(true, "FIRE_WATCHDOG_REFIRE"),
        SKIP_DISABLED_SETTING(false, "FIRE_WATCHDOG_SETTING_OFF"),
        SKIP_NO_ALARM(false, "FIRE_WATCHDOG_ALARM_ROW_MISSING"),
        SKIP_DISABLED(false, "FIRE_WATCHDOG_ALARM_DISABLED"),
        SKIP_ALREADY_FIRED(false, "FIRE_WATCHDOG_ALREADY_FIRED"),
        SKIP_STALE(false, "FIRE_WATCHDOG_OUTSIDE_WINDOW")
    }
}
