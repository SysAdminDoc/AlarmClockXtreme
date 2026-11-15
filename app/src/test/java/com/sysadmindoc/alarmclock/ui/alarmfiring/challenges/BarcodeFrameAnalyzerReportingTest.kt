package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import androidx.camera.core.ImageProxy
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import zxingcpp.BarcodeReader

/**
 * What the analyzer passes on from the decoder, with the decoder faked. The
 * real decoder on a real frame is covered by the instrumented
 * BarcodeFrameAnalyzerTest.
 */
class BarcodeFrameAnalyzerReportingTest {
    private val reader = mockk<BarcodeReader>()
    private val codes = mutableListOf<String>()
    private val errors = mutableListOf<Throwable>()
    private val analyzer = BarcodeFrameAnalyzer(reader, { codes += it }, { errors += it })

    private fun frame(): ImageProxy = mockk(relaxed = true)

    private fun result(value: String?) = mockk<BarcodeReader.Result> { every { text } returns value }

    private fun decoderFails() {
        every { reader.read(any<ImageProxy>()) } throws IllegalStateException("bad frame")
    }

    @Test
    fun everyCodeInViewIsReportedOnce() {
        every { reader.read(any<ImageProxy>()) } returnsMany listOf(
            listOf(result("4006381333931"), result("ACX-51 kitchen fridge")),
            listOf(result("ACX-51 kitchen fridge")),
            listOf(result("4006381333931"), result(" "), result(null))
        )

        repeat(3) { analyzer.analyze(frame()) }

        assertEquals(listOf("4006381333931", "ACX-51 kitchen fridge"), codes)
    }

    @Test
    fun aCodeThatLeavesAndComesBackIsNotReportedAgain() {
        every { reader.read(any<ImageProxy>()) } returnsMany listOf(
            listOf(result("A")),
            listOf(result("B")),
            listOf(result("A"))
        )

        repeat(3) { analyzer.analyze(frame()) }

        assertEquals(listOf("A", "B"), codes)
    }

    @Test
    fun badFramesBetweenGoodOnesAreNotAnError() {
        decoderFails()
        repeat(BarcodeFrameAnalyzer.FAILED_FRAMES_LIMIT - 1) { analyzer.analyze(frame()) }
        every { reader.read(any<ImageProxy>()) } returns emptyList()
        analyzer.analyze(frame())
        decoderFails()
        repeat(BarcodeFrameAnalyzer.FAILED_FRAMES_LIMIT - 1) { analyzer.analyze(frame()) }

        assertEquals(emptyList<Throwable>(), errors)
    }

    @Test
    fun aDecoderThatKeepsFailingIsReportedOnce() {
        decoderFails()

        repeat(BarcodeFrameAnalyzer.FAILED_FRAMES_LIMIT * 3) { analyzer.analyze(frame()) }

        assertEquals(1, errors.size)
    }

    @Test
    fun aFrameIsClosedEvenWhenDecodingFails() {
        decoderFails()
        val failing = frame()

        analyzer.analyze(failing)

        verify { failing.close() }
    }
}
