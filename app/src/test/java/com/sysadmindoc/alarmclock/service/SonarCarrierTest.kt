package com.sysadmindoc.alarmclock.service

import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The point of these is the selectivity case: a loud room does not look like a
 * carrier. That is the property the broadband stillness detector does not have,
 * and the reason this filter exists.
 */
class SonarCarrierTest {

    private val sampleRate = 44100
    private val n = SonarCarrier.ANALYSIS_SAMPLES

    private fun tone(hz: Double, amplitude: Double, count: Int = n) =
        ShortArray(count) { i ->
            (amplitude * 32767.0 * sin(2.0 * Math.PI * hz * i / sampleRate)).toInt().toShort()
        }

    private fun broadbandRms(buffer: ShortArray): Float {
        var sum = 0.0
        buffer.forEach { s -> (s / 32768.0).let { sum += it * it } }
        return sqrt(sum / buffer.size).toFloat()
    }

    /** A hum plus hiss, near full scale: what a fan and a road sound like. */
    private fun loudRoom(seed: Int = 20260822): ShortArray {
        val random = Random(seed)
        return ShortArray(n) { i ->
            val hum = 0.5 * sin(2.0 * Math.PI * 1000.0 * i / sampleRate)
            val hiss = random.nextDouble(-0.4, 0.4)
            ((hum + hiss).coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
        }
    }

    private fun plusCarrier(room: ShortArray, amplitude: Double) = ShortArray(n) { i ->
        val carrier = amplitude * sin(2.0 * Math.PI * 18750.0 * i / sampleRate)
        ((room[i] / 32768.0 + carrier).coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
    }

    @Test
    fun `the carrier lands exactly on a bin`() {
        // 2058 * 18750 / 44100 = 875 with no remainder. If either constant moves
        // and this stops being whole, every reading drops for a reason that
        // would look like a hardware fault.
        assertEquals(875, SonarCarrier.CARRIER_BIN)
        assertEquals(0, SonarCarrier.ANALYSIS_SAMPLES * 18750 % 44100)
    }

    @Test
    fun `the reading is the carrier's amplitude`() {
        assertEquals(1.00f, SonarCarrier.magnitude(tone(18750.0, 1.0)), 0.01f)
        assertEquals(0.10f, SonarCarrier.magnitude(tone(18750.0, 0.10)), 0.005f)
        assertEquals(0.01f, SonarCarrier.magnitude(tone(18750.0, 0.01)), 0.001f)
        assertEquals(0f, SonarCarrier.magnitude(ShortArray(n)), 1e-6f)
    }

    @Test
    fun `a short read measures nothing rather than measuring the wrong thing`() {
        // A partial window has different bin spacing, so the carrier would fall
        // between bins and read low. Zero is the honest answer.
        assertEquals(0f, SonarCarrier.magnitude(tone(18750.0, 1.0, count = 1000)), 1e-6f)
        assertEquals(0f, SonarCarrier.magnitude(tone(18750.0, 1.0), length = 1000), 1e-6f)
    }

    @Test
    fun `a loud room barely reaches the carrier bin`() {
        val room = loudRoom()
        val broadband = broadbandRms(room)
        val carrierBin = SonarCarrier.magnitude(room)

        // Around 0.42 broadband against 0.01 in the bin: the filter rejects the
        // room by a factor of roughly 40. Feeding that broadband figure into a
        // stillness threshold, which is what the service does today, is why the
        // emitted tone barely matters to the measurement.
        assertTrue("expected a loud room, got $broadband", broadband > 0.3f)
        assertTrue(
            "carrier bin $carrierBin should be far below the broadband $broadband",
            carrierBin < broadband / 20f
        )
    }

    @Test
    fun `a carrier the room cannot drown out reads as itself`() {
        val room = loudRoom()
        val floor = SonarCarrier.magnitude(room)

        // The room's own energy in the bin adds incoherently, so it moves the
        // reading by about that much and no more. Asserting against the floor
        // rather than a fixed tolerance says why the error is the size it is.
        listOf(0.10f, 0.05f).forEach { amplitude ->
            val reading = SonarCarrier.magnitude(plusCarrier(room, amplitude.toDouble()))
            assertTrue(
                "a $amplitude carrier read $reading, further from itself than the " +
                    "room's bin energy ($floor) explains",
                kotlin.math.abs(reading - amplitude) <= floor * 1.5f
            )
        }

        // At the 1% the emitter actually uses, the carrier is the same size as
        // the room's own energy in that bin, so the two combine to something
        // between them. That is the number that has to be calibrated on a real
        // device before this replaces the broadband detector, and it is why it
        // has not yet.
        val atOnePercent = SonarCarrier.magnitude(plusCarrier(room, 0.01))
        assertTrue(
            "1% carrier ($atOnePercent) should be within a factor of two of the " +
                "room's own bin energy ($floor), which is the calibration problem",
            atOnePercent < floor * 3f
        )
    }

    @Test
    fun `in a quiet room the carrier is unmistakable`() {
        val quiet = ShortArray(n) { (Random(7).nextDouble(-0.01, 0.01) * 32767.0).toInt().toShort() }
        val withCarrier = plusCarrier(quiet, 0.01)

        assertTrue("a quiet room should read near zero", SonarCarrier.magnitude(quiet) < 0.002f)
        assertEquals(0.01f, SonarCarrier.magnitude(withCarrier), 0.002f)
    }
}
