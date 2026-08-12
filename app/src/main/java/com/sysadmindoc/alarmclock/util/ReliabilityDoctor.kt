package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.os.Build

/** Persists the small amount of state needed to re-check wake reliability after an OTA. */
object ReliabilityDoctor {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_BUILD_FINGERPRINT = "reliability_build_fingerprint"
    private const val KEY_CHECKLIST_DUE = "reliability_checklist_due"

    /**
     * Records the current OS build and marks the checklist due when it changed.
     * The first observation is intentionally not treated as an OTA: new installs
     * already enter the normal onboarding flow.
     */
    fun recordCurrentBuildFingerprint(
        context: Context,
        fingerprint: String = Build.FINGERPRINT
    ): Boolean {
        val current = fingerprint.trim()
        if (current.isBlank()) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_BUILD_FINGERPRINT, null)
        val changed = shouldReopenChecklist(previous, current)
        val editor = prefs.edit().putString(KEY_BUILD_FINGERPRINT, current)
        if (changed) editor.putBoolean(KEY_CHECKLIST_DUE, true)
        editor.apply()
        return changed
    }

    fun isChecklistDue(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CHECKLIST_DUE, false)

    fun markChecklistReviewed(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CHECKLIST_DUE, false)
            .apply()
    }

    internal fun shouldReopenChecklist(previous: String?, current: String): Boolean =
        !previous.isNullOrBlank() && current.isNotBlank() && previous != current
}
