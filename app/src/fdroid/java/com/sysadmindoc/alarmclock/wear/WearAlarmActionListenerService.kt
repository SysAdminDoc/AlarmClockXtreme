package com.sysadmindoc.alarmclock.wear

import android.app.Service
import android.content.Intent
import android.os.IBinder

class WearAlarmActionListenerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
