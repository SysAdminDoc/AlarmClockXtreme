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
 *
 * v1.5.1: Guarded against quirky OEM proximity sensors that report
 * `maximumRange <= 0` (contact-only or bugged drivers). Threshold now
 * floors at a physically plausible 3 cm.
 */
class ProximityCoverDetector(
    context: Context,
    private val onCovered: () -> Unit,
    private val holdMs: Long = 1500L
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximity: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    // v1.5.1: Precompute a safe threshold at registration time. Some
    // devices report `maximumRange` as 0 or a microscopic value, which
    // would otherwise make every sample look "covered" or "uncovered"
    // depending on rounding. Clamp to a sane minimum of 3 cm.
    //
    // v1.5.2: Threshold selection moved to a pure helper for unit testing.
    private val threshold: Float = computeThreshold(proximity?.maximumRange ?: DEFAULT_MAX_RANGE_CM)
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

        val isCovered = event.values[0] < threshold
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

    companion object {
        /**
         * Typical smartphone proximity sensors max out around 5 cm. This
         * default is only used when the driver reports an implausible
         * max range.
         */
        const val DEFAULT_MAX_RANGE_CM = 5f

        /** Half of a plausible `maximumRange`, floored so that a driver
         *  reporting 0 / microscopic range doesn't produce a useless
         *  threshold. Extracted as a pure function so v1.5.2 unit tests
         *  can pin the behaviour. */
        fun computeThreshold(sensorMaxRange: Float): Float {
            val clamped = if (sensorMaxRange > 0.5f) sensorMaxRange else DEFAULT_MAX_RANGE_CM
            return clamped * 0.5f
        }
    }
}
