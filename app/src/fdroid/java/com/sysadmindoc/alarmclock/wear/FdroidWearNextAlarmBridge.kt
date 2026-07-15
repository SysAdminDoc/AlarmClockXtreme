package com.sysadmindoc.alarmclock.wear

import com.sysadmindoc.alarmclock.data.model.Alarm
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FdroidWearNextAlarmBridge @Inject constructor() : WearNextAlarmBridge {
    override fun start() { /* no-op */ }
    override fun stop() { /* no-op */ }
    override fun publishAlarmFiring(alarm: Alarm) { /* no-op */ }
    override fun publishAlarmIdle(alarmId: Long) { /* no-op */ }
}
