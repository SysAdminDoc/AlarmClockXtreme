package com.sysadmindoc.alarmclock.data.readiness

import android.content.Context

data class TestAlarmProof(
    val scheduledAt: Long = 0L,
    val firedAt: Long = 0L,
    val completedAt: Long = 0L,
    val notificationPermissionGranted: Boolean = false,
    val fullScreenIntentRequested: Boolean = false,
    val activityLaunchSucceeded: Boolean = false,
    val legacyCompleted: Boolean = false
) {
    val hasDetailedCompletion: Boolean
        get() = completedAt > 0L

    val isCompleted: Boolean
        get() = hasDetailedCompletion || legacyCompleted

    val latencyMs: Long?
        get() = if (scheduledAt > 0L && firedAt > 0L) {
            (firedAt - scheduledAt).coerceAtLeast(0L)
        } else {
            null
        }
}

object TestAlarmProofStore {
    private const val PREFS = "onboarding_test_alarm"
    private const val KEY_COMPLETED = "completed"
    private const val KEY_SCHEDULED_AT = "scheduled_at"
    private const val KEY_FIRED_AT = "fired_at"
    private const val KEY_COMPLETED_AT = "completed_at"
    private const val KEY_NOTIFICATION_PERMISSION_GRANTED = "notification_permission_granted"
    private const val KEY_FULL_SCREEN_INTENT_REQUESTED = "full_screen_intent_requested"
    private const val KEY_ACTIVITY_LAUNCH_SUCCEEDED = "activity_launch_succeeded"

    fun isCompleted(context: Context): Boolean = lastProof(context).isCompleted

    fun lastProof(context: Context): TestAlarmProof {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val completedAt = prefs.getLong(KEY_COMPLETED_AT, 0L)
        return TestAlarmProof(
            scheduledAt = prefs.getLong(KEY_SCHEDULED_AT, 0L),
            firedAt = prefs.getLong(KEY_FIRED_AT, 0L),
            completedAt = completedAt,
            notificationPermissionGranted = prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_GRANTED, false),
            fullScreenIntentRequested = prefs.getBoolean(KEY_FULL_SCREEN_INTENT_REQUESTED, false),
            activityLaunchSucceeded = prefs.getBoolean(KEY_ACTIVITY_LAUNCH_SUCCEEDED, false),
            legacyCompleted = completedAt == 0L && prefs.getBoolean(KEY_COMPLETED, false)
        )
    }

    fun recordScheduled(context: Context, scheduledAt: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, false)
            .putLong(KEY_SCHEDULED_AT, scheduledAt)
            .putLong(KEY_FIRED_AT, 0L)
            .putLong(KEY_COMPLETED_AT, 0L)
            .putBoolean(KEY_NOTIFICATION_PERMISSION_GRANTED, false)
            .putBoolean(KEY_FULL_SCREEN_INTENT_REQUESTED, false)
            .putBoolean(KEY_ACTIVITY_LAUNCH_SUCCEEDED, false)
            .apply()
    }

    fun recordFired(
        context: Context,
        scheduledAt: Long,
        firedAt: Long,
        notificationPermissionGranted: Boolean,
        fullScreenIntentRequested: Boolean,
        activityLaunchSucceeded: Boolean
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_SCHEDULED_AT, scheduledAt)
            .putLong(KEY_FIRED_AT, firedAt)
            .putBoolean(KEY_NOTIFICATION_PERMISSION_GRANTED, notificationPermissionGranted)
            .putBoolean(KEY_FULL_SCREEN_INTENT_REQUESTED, fullScreenIntentRequested)
            .putBoolean(KEY_ACTIVITY_LAUNCH_SUCCEEDED, activityLaunchSucceeded)
            .apply()
    }

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, true)
            .putLong(KEY_COMPLETED_AT, System.currentTimeMillis())
            .apply()
    }
}
