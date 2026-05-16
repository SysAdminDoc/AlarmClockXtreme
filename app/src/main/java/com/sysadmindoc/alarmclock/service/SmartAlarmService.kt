package com.sysadmindoc.alarmclock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * F7: Smart alarm window — monitors accelerometer overnight.
 * Started by AlarmScheduler [smartAlarmWindowMinutes] before the scheduled alarm time.
 * If low motion is sustained (indicating light sleep), fires the alarm early.
 * If the scheduled time arrives without detection, the regular AlarmReceiver fires normally.
 */
@AndroidEntryPoint
class SmartAlarmService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START_SMART = "com.sysadmindoc.alarmclock.SMART_ALARM_START"
        const val EXTRA_ALARM_ID = "smart_alarm_id"
        const val EXTRA_TARGET_TIME = "smart_target_time"
        const val CHANNEL_SMART = "smart_alarm_channel"
        const val NOTIF_ID_SMART = 2003

        /** Threshold below which movement is considered "still / light sleep" */
        private const val MOTION_THRESHOLD = 0.8f  // m/s² delta from gravity
        /** How many consecutive low-motion windows (each ~30s) to confirm light sleep */
        private const val LOW_MOTION_WINDOWS_REQUIRED = 3
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var alarmId: Long = -1L
    private var targetTimeMs: Long = 0L
    private var lowMotionWindowCount = 0
    private var windowMotionMax = 0f
    private var windowStartMs = 0L
    private val WINDOW_MS = 30_000L  // 30-second motion sampling windows

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // v1.5.4: Safe casts — stripped-down AOSP / managed-profile devices
        // have been seen to return null for SENSOR_SERVICE / POWER_SERVICE.
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmClockXtreme::SmartAlarmWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START_SMART) return START_NOT_STICKY

        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        targetTimeMs = intent.getLongExtra(EXTRA_TARGET_TIME, 0L)

        if (alarmId == -1L || targetTimeMs == 0L) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_SMART)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Smart Alarm Active")
            .setContentText("Monitoring sleep quality…")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIF_ID_SMART, notification)

        // v1.11.4 (roadmap N4) Play wake-lock policy audit:
        //   SmartAlarmService is the one PARTIAL_WAKE_LOCK acquisition in the
        //   app that is NOT exempt under the March-2026 Play policy — it
        //   wraps a `dataSync` foreground service (not media playback) and
        //   the accelerometer monitoring isn't an exempted activity. The
        //   90-minute hard cap on a single window means a worst-case
        //   power-user with one smart-wake alarm holds 90 min / 24 h —
        //   under the 2 h / 24 h non-exempt budget. Users who layer
        //   multiple smart-wake alarms on the same calendar day (e.g., a
        //   morning alarm with smartAlarmWindowMinutes = 90 plus an
        //   afternoon nap with the same window) could theoretically exceed
        //   the cap; the service is single-instance and stopSelf()s after
        //   firing or canceling, so cumulative time is bounded by the sum
        //   of fires per day. If that pattern emerges in field data, lower
        //   the per-window cap or cumulate-time-track here and break
        //   monitoring early.
        wakeLock?.acquire(90 * 60 * 1000L)  // Max 90 min
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        windowStartMs = System.currentTimeMillis()

        return START_NOT_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()

        // If we've passed the scheduled alarm time, stop — regular alarm will fire
        if (now >= targetTimeMs) {
            stopSelf()
            return
        }

        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val mag = sqrt(x * x + y * y + z * z)
        val delta = abs(mag - SensorManager.GRAVITY_EARTH)

        if (delta > windowMotionMax) windowMotionMax = delta

        // Check window boundary
        if (now - windowStartMs >= WINDOW_MS) {
            if (windowMotionMax < MOTION_THRESHOLD) {
                lowMotionWindowCount++
                if (lowMotionWindowCount >= LOW_MOTION_WINDOWS_REQUIRED) {
                    // User is in light sleep — fire alarm early
                    fireAlarmEarly()
                    return
                }
            } else {
                lowMotionWindowCount = 0
            }
            windowMotionMax = 0f
            windowStartMs = now
        }
    }

    private fun fireAlarmEarly() {
        sensorManager?.unregisterListener(this)
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        try {
            startForegroundService(intent)
        } catch (e: Exception) {
            android.util.Log.e("SmartAlarmService", "startForegroundService failed for alarm $alarmId", e)
        }
        stopSelf()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL_SMART, "Smart Alarm", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Smart sleep phase detection"
        }
        nm.createNotificationChannel(ch)
    }
}
