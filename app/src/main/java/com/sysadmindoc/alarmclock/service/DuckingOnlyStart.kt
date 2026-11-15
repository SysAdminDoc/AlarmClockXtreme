package com.sysadmindoc.alarmclock.service

/**
 * Stops an AlarmService instance that exists only because of a challenge
 * ducking command. The firing screen un-ducks from onDestroy, which lands
 * after dismiss or snooze already stopped the service, so that one command
 * created a fresh instance (and its onCreate wake lock) with nothing to ring.
 * It used to idle there, holding the phone awake, until Android reaped it.
 *
 * [stop] is `stopSelfResult`, so a newer start that arrived in the meantime
 * keeps the service alive.
 */
internal class DuckingOnlyStart(private val stop: (Int) -> Unit) {
    private var handledAlarmCommand = false

    /** Call first for every start command this instance receives. */
    fun onCommand(isDuckingCommand: Boolean) {
        if (!isDuckingCommand) handledAlarmCommand = true
    }

    /** Call for a ducking command that matched no ringing alarm. */
    fun stopIfOrphaned(startId: Int) {
        if (!handledAlarmCommand) stop(startId)
    }
}
