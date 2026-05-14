package com.sysadmindoc.alarmclock.wear

import com.sysadmindoc.alarmclock.data.model.Alarm

/**
 * Flavor-bound Wear OS bridge.
 *
 * The Play build publishes next-alarm state to Wear OS through the Data Layer.
 * The F-Droid build binds a no-op implementation so proprietary Play Services
 * never enters the F-Droid dependency graph.
 */
interface WearNextAlarmBridge {
    fun start()
    fun publishAlarmFiring(alarm: Alarm)
    fun publishAlarmIdle(alarmId: Long)
}
