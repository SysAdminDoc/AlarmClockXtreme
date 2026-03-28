package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * F4: Walk-steps challenge — uses TYPE_STEP_COUNTER to count steps since registration.
 *
 * TYPE_STEP_COUNTER reports cumulative steps since device reboot.
 * We capture a baseline on [start] and report the delta to [onStepDelta].
 */
class StepCounterListener(
    context: Context,
    private val onStepDelta: (steps: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private var baseline: Float = -1f
    private var detectorCount = 0

    /** @return true if a step sensor is available on this device */
    fun isAvailable() = stepCounter != null || stepDetector != null

    fun start() {
        if (stepCounter != null) {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
        } else if (stepDetector != null) {
            // Fallback: TYPE_STEP_DETECTOR fires once per step
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        baseline = -1f
        detectorCount = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val total = event.values[0]
                if (baseline < 0f) baseline = total
                onStepDelta((total - baseline).toInt().coerceAtLeast(0))
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                detectorCount++
                onStepDelta(detectorCount)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
