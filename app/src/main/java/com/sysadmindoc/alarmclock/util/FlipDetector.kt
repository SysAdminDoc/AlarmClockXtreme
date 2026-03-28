package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * F6: Flip-to-snooze / face-up-to-dismiss detection.
 *
 * Face-down: z-axis strongly negative (phone face on table) AND proximity sensor near.
 * Face-up:   z-axis strongly positive AND proximity sensor far.
 *
 * On face-down → calls [onFaceDown]. Once face-down has been detected, flipping
 * face-up calls [onFaceUp].
 */
class FlipDetector(
    context: Context,
    private val onFaceDown: () -> Unit,
    private val onFaceUp: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isFaceDown = false
    private var proximityNear = false
    private var lastZAccel = 0f
    private var lastEventMs = 0L
    private val debounceMs = 600L

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val now = System.currentTimeMillis()
        if (now - lastEventMs < debounceMs) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastZAccel = event.values[2]
            }
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = proximity?.maximumRange ?: 5f
                proximityNear = event.values[0] < maxRange * 0.5f
            }
            else -> return
        }

        val isFaceDownNow = lastZAccel < -7f // z strongly negative → face down
        val isFaceUpNow = lastZAccel > 7f    // z strongly positive → face up

        if (isFaceDownNow && !isFaceDown) {
            isFaceDown = true
            lastEventMs = now
            onFaceDown()
        } else if (isFaceUpNow && isFaceDown) {
            isFaceDown = false
            lastEventMs = now
            onFaceUp()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
