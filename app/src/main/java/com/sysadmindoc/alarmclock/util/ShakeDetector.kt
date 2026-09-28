package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlin.math.sqrt

/**
 * Detects shake gestures using the accelerometer.
 * Counts individual shakes (acceleration > threshold) with debounce.
 */
class ShakeDetector(
    context: Context,
    private val onSensorFailure: () -> Unit = {},
    private val onShake: (shakeCount: Int) -> Unit
) : SensorEventListener {

    // v1.5.4: Safe cast — stripped-down AOSP builds may return null.
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeCount = 0
    private var lastShakeTime = 0L
    private val shakeThreshold = 14f  // m/s^2 above gravity
    private val shakeCooldownMs = 250L

    // v1.11.2 (ALA-85): Watchdog to detect hardware/driver freeze (no events for 5s)
    private val handler = Handler(Looper.getMainLooper())
    private val watchdog = Runnable { onSensorFailure() }
    private var watchdogActive = false

    fun start(): Boolean {
        val sm = sensorManager ?: return false
        val acc = accelerometer ?: return false
        val ok = sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME)
        if (ok) {
            startWatchdog()
        }
        return ok
    }

    fun stop() {
        stopWatchdog()
        sensorManager?.unregisterListener(this)
    }

    fun reset() {
        shakeCount = 0
    }

    private fun startWatchdog() {
        if (watchdogActive) return
        watchdogActive = true
        handler.postDelayed(watchdog, 5000L)
    }

    private fun stopWatchdog() {
        handler.removeCallbacks(watchdog)
        watchdogActive = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        // First event received; hardware is alive.
        stopWatchdog()

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
