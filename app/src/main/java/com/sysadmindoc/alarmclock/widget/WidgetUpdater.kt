package com.sysadmindoc.alarmclock.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility to trigger widget refresh when alarm state changes.
 * Call from AlarmScheduler, AlarmService, etc.
 */
object WidgetUpdater {

    fun requestUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NextAlarmWidget().updateAll(context)
            } catch (_: Exception) {
                // Widget may not be placed - ignore
            }
        }
    }
}
