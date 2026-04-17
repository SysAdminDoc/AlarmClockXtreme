package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * v1.4.0: "Cover the phone" snooze detector.
 *
 * Pairs with flip-to-snooze for users whose phones don't have a reliable
 * face-down accelerometer reading (e.g. in a phone stand) — covering the
 * proximity sensor for [holdMs] milliseconds triggers a snooze.
 *
 * A short hold is required to avoid accidental snoozes from hand-wave
 * gestures or brief pocket contact while the alarm starts.
 */
class ProximityCoverDetector(
    context: Context,
    private val onCovered: () -> Unit,
    private val holdMs: Long = 1500L
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var coveredSinceMs = 0L
    private var triggered = false

    fun start() {
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        coveredSinceMs = 0L
        triggered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return

        val maxRange = proximity?.maximumRange ?: 5f
        val isCovered = event.values[0] < maxRange * 0.5f
        val now = System.currentTimeMillis()

        if (isCovered) {
            if (coveredSinceMs == 0L) {
                coveredSinceMs = now
            } else if (!triggered && now - coveredSinceMs >= holdMs) {
                triggered = true
                onCovered()
            }
        } else {
            coveredSinceMs = 0L
            triggered = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
