package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Detects push-up motions using the accelerometer.
 *
 * The phone should be placed face-down on the floor. A push-up is detected when
 * the Z-axis accelerometer value (perpendicular to the screen) shows the pattern:
 * phone rises (Z approaches -9.8 as gravity pulls screen-side), then lowers
 * (Z returns toward 0 or positive as the chest approaches the floor).
 *
 * In a face-down orientation the resting Z value is roughly +9.8 (gravity
 * pointing from screen toward back). During a push-up the phone tilts and the
 * Z value dips as the user's body rises and returns.
 *
 * This uses the same threshold/cooldown pattern as [SquatDetector].
 */
class PushUpDetector(
    context: Context,
    private val onPushUp: (pushUpCount: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var pushUpCount = 0

    // In face-down position, resting Z ~ +9.8 (gravity through back of phone).
    // During a push-up the phone rises and tilts, reducing the Z reading.
    private val downThreshold = 7.0f   // Z drops below this = body rising (push phase)
    private val upThreshold = 9.0f     // Z returns above this = body lowering (return phase)
    private var isRising = false
    private var wasRising = false
    private val cooldownMs = 1000L
    private var lastPushUpTime = 0L

    /** True when an accelerometer exists to count push-ups. */
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
        val z = event.values[2] // Z-axis: perpendicular to screen

        if (z < downThreshold && !isRising) {
            isRising = true
            wasRising = true
        }
        if (z > upThreshold && wasRising && isRising) {
            val now = System.currentTimeMillis()
            if (now - lastPushUpTime > cooldownMs) {
                pushUpCount++
                lastPushUpTime = now
                onPushUp(pushUpCount)
            }
            isRising = false
            wasRising = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
