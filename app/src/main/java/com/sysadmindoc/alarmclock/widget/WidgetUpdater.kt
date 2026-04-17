package com.sysadmindoc.alarmclock.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Utility to trigger widget refresh when alarm state changes.
 * Call from AlarmScheduler, AlarmService, etc.
 *
 * Uses a single process-scoped [SupervisorJob] instead of allocating a fresh
 * unrooted CoroutineScope per call (which leaked a Job each time the user
 * toggled an alarm).
 */
object WidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                NextAlarmWidget().updateAll(appContext)
            } catch (_: Exception) {
                // Widget may not be placed — ignore.
            }
        }
    }
}
