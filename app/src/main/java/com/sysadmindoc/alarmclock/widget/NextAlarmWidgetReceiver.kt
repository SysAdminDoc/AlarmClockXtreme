package com.sysadmindoc.alarmclock.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver for the NextAlarmWidget.
 * Declared in AndroidManifest.xml.
 */
class NextAlarmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextAlarmWidget()
}
