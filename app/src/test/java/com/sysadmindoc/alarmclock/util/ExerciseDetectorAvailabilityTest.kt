package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor

/**
 * P0 guard: squat/push-up dismiss challenges are accelerometer-backed. On a
 * device with no accelerometer the alarm must NOT become undismissable — the
 * detector reports [isAvailable] == false so the firing screen can offer a
 * "Continue without sensor" fallback (challenge-bypass is off by default, so
 * without this the only escape would be force-stopping the app).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35]) // Robolectric 4.14.1 supports up to SDK 35; app targets 36.
class ExerciseDetectorAvailabilityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun sensorManager() =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    @Test
    fun `squat detector reports unavailable without accelerometer`() {
        assertFalse(SquatDetector(context) {}.isAvailable())
    }

    @Test
    fun `push-up detector reports unavailable without accelerometer`() {
        assertFalse(PushUpDetector(context) {}.isAvailable())
    }

    @Test
    fun `squat detector reports available when an accelerometer exists`() {
        shadowOf(sensorManager()).addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        assertTrue(SquatDetector(context) {}.isAvailable())
    }

    @Test
    fun `push-up detector reports available when an accelerometer exists`() {
        shadowOf(sensorManager()).addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        assertTrue(PushUpDetector(context) {}.isAvailable())
    }
}
