package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects shake gestures using the accelerometer.
 * Counts individual shakes (acceleration > threshold) with debounce.
 */
class ShakeDetector(
    context: Context,
    private val onShake: (shakeCount: Int) -> Unit
) : SensorEventListener {

    // v1.5.4: Safe cast — stripped-down AOSP builds may return null.
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeCount = 0
    private var lastShakeTime = 0L
    private val shakeThreshold = 14f  // m/s^2 above gravity
    private val shakeCooldownMs = 250L

    fun start() {
        val sm = sensorManager ?: return
        accelerometer?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    fun reset() {
        shakeCount = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = sqrt(x * x + y * y + z * z)

        val now = System.currentTimeMillis()
        if (acceleration > shakeThreshold && (now - lastShakeTime) > shakeCooldownMs) {
            shakeCount++
            lastShakeTime = now
            onShake(shakeCount)
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
