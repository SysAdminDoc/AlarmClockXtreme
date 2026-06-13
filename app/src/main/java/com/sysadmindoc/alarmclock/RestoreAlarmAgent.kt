package com.sysadmindoc.alarmclock

import android.app.backup.BackupAgentHelper
import com.sysadmindoc.alarmclock.worker.BootRescheduleWorker

class RestoreAlarmAgent : BackupAgentHelper() {

    override fun onRestoreFinished() {
        super.onRestoreFinished()
        BootRescheduleWorker.enqueue(
            context = applicationContext,
            sourceAction = "RESTORE_FINISHED",
            forceRecalculate = true
        )
    }
}
