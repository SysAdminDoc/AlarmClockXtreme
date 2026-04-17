package com.sysadmindoc.alarmclock.util

import android.content.Context

/**
 * v1.5.0: Records the last app versionCode we showed a "What's new" dialog
 * for. Used by [MainActivity] to show the dialog once per upgrade, never on
 * first install.
 */
object WhatsNewTracker {

    private const val PREFS = "whats_new_tracker"
    private const val KEY_LAST_SHOWN = "last_shown_version_code"
    private const val KEY_INSTALLED_AT = "first_install_version_code"

    /**
     * Returns true iff the dialog should be shown for [currentVersionCode].
     * False on a fresh install (so we don't pile onboarding + whats-new).
     * False if we already showed it for this version.
     */
    fun shouldShow(context: Context, currentVersionCode: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val firstInstall = prefs.getInt(KEY_INSTALLED_AT, 0)
        val lastShown = prefs.getInt(KEY_LAST_SHOWN, 0)

        if (firstInstall == 0) {
            // First launch ever — record the install version but don't show.
            prefs.edit()
                .putInt(KEY_INSTALLED_AT, currentVersionCode)
                .putInt(KEY_LAST_SHOWN, currentVersionCode)
                .apply()
            return false
        }
        return currentVersionCode > lastShown
    }

    fun markShown(context: Context, currentVersionCode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_SHOWN, currentVersionCode)
            .apply()
    }
}
