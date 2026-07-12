package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * v1.2.0: Detects squat motions using the accelerometer.
 * A squat is: downward acceleration (phone goes down) followed by upward acceleration.
 * Tracks gravity Z-axis deviation to detect vertical up-down-up pattern.
 */
class SquatDetector(
    context: Context,
    private val onSquat: (squatCount: Int) -> Unit
) : SensorEventListener {

    // v1.5.4: Safe cast.
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var squatCount = 0
    private var lastY = 0f
    private var isGoingDown = false
    private var wasDown = false
    private val downThreshold = 7.5f  // Below gravity = going down
    private val upThreshold = 11.5f   // Above gravity = coming back up
    private val cooldownMs = 800L
    private var lastSquatTime = 0L

    /** True when an accelerometer exists to count squats. */
    fun isAvailable() = accelerometer != null

    fun start() {
        val sm = sensorManager ?: return
        accelerometer?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val y = event.values[1] // Vertical axis when phone is in pocket/hand

        if (y < downThreshold && !isGoingDown) {
            isGoingDown = true
            wasDown = true
        }
        if (y > upThreshold && wasDown && isGoingDown) {
            val now = System.currentTimeMillis()
            if (now - lastSquatTime > cooldownMs) {
                squatCount++
                lastSquatTime = now
                onSquat(squatCount)
            }
            isGoingDown = false
            wasDown = false
        }

        lastY = y
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
