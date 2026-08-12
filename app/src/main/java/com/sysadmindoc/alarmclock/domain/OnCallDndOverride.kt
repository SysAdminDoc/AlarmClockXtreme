package com.sysadmindoc.alarmclock.domain

import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Temporarily moves total-silence DND to alarms-only for an explicitly enabled
 * on-call alarm. The previous filter is persisted so a killed service can
 * restore the user's DND choice when the next service instance starts.
 */
object OnCallDndOverride {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_ACTIVE = "on_call_dnd_override_active"
    private const val KEY_PREVIOUS_FILTER = "on_call_dnd_previous_filter"

    // Android's public NotificationManager constants, kept here as primitive
    // values so the decision helpers remain easy to unit-test on the JVM.
    const val FILTER_TOTAL_SILENCE = 3
    const val FILTER_ALARMS_ONLY = 4

    internal fun shouldOverride(
        enabled: Boolean,
        policyAccessGranted: Boolean,
        currentFilter: Int
    ): Boolean = enabled && policyAccessGranted && currentFilter == FILTER_TOTAL_SILENCE

    internal fun shouldRestore(previousFilter: Int, currentFilter: Int): Boolean =
        previousFilter == FILTER_TOTAL_SILENCE && currentFilter == FILTER_ALARMS_ONLY

    fun isPolicyAccessGranted(context: Context): Boolean = runCatching {
        context.getSystemService(NotificationManager::class.java)
            ?.isNotificationPolicyAccessGranted == true
    }.getOrDefault(false)

    fun begin(context: Context, enabled: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val policyAccessGranted = runCatching {
            manager.isNotificationPolicyAccessGranted
        }.getOrDefault(false)
        val currentFilter = runCatching { manager.currentInterruptionFilter }.getOrNull()
            ?: return false
        if (!shouldOverride(enabled, policyAccessGranted, currentFilter)) return false

        return runCatching {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ACTIVE, true)
                .putInt(KEY_PREVIOUS_FILTER, currentFilter)
                .apply()
            true
        }.getOrDefault(false)
    }

    /** Restores an owned override, but never overwrites a newer user decision. */
    fun end(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ACTIVE, false)) return

        val previousFilter = prefs.getInt(KEY_PREVIOUS_FILTER, FILTER_TOTAL_SILENCE)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val currentFilter = manager?.currentInterruptionFilter
                if (manager != null && currentFilter != null &&
                    shouldRestore(previousFilter, currentFilter) &&
                    manager.isNotificationPolicyAccessGranted
                ) {
                    manager.setInterruptionFilter(previousFilter)
                }
            }
        }.onFailure {
            // A revoked policy grant or OEM restriction must never block alarm teardown.
        }
        prefs.edit().remove(KEY_ACTIVE).remove(KEY_PREVIOUS_FILTER).apply()
    }

    fun restoreStale(context: Context) = end(context)
}
