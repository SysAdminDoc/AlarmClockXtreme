package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.ExifData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real zxing-cpp decoder (native code, so not under Robolectric) on a
 * QR code fed to the analyzer the way CameraX hands it a frame: the Y plane of
 * a YUV_420_888 image.
 */
@RunWith(AndroidJUnit4::class)
class BarcodeFrameAnalyzerTest {

    private fun fixture(name: String): Bitmap {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        return assets.open(name).use { BitmapFactory.decodeStream(it) }
    }

    @Test
    fun aQrCodeFrameIsDecodedOnceAndEveryFrameIsClosed() {
        val codes = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
        val analyzer = BarcodeFrameAnalyzer(BarcodeFrameAnalyzer.newReader(), { codes += it }, { errors += it })
        val qr = fixture("qr_kitchen_fridge.png")

        val first = LumaFrame(qr)
        val second = LumaFrame(qr)
        analyzer.analyze(first)
        analyzer.analyze(second)

        assertEquals(listOf("ACX-51 kitchen fridge"), codes)
        assertEquals(emptyList<Throwable>(), errors)
        assertTrue(first.closed && second.closed)
    }

    @Test
    fun aFrameWithNoCodeReportsNothing() {
        val codes = mutableListOf<String>()
        val analyzer = BarcodeFrameAnalyzer(BarcodeFrameAnalyzer.newReader(), { codes += it }, { throw it })
        val blank = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF808080.toInt()) }
        val frame = LumaFrame(blank)

        analyzer.analyze(frame)

        assertEquals(emptyList<String>(), codes)
        assertTrue(frame.closed)
    }

    /** A YUV_420_888 frame holding [bitmap]'s luminance, padded rows included, as a camera delivers it. */
    private class LumaFrame(bitmap: Bitmap) : ImageProxy {
        private val w = bitmap.width
        private val h = bitmap.height
        private val stride = w + 16
        private val luma: ByteBuffer = ByteBuffer.allocateDirect(stride * h).apply {
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val p = pixels[y * w + x]
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    put(y * stride + x, ((r * 299 + g * 587 + b * 114) / 1000).toByte())
                }
            }
        }
        private var crop = Rect(0, 0, w, h)
        var closed = false

        override fun close() { closed = true }
        override fun getCropRect(): Rect = crop
        override fun setCropRect(rect: Rect?) { crop = rect ?: Rect(0, 0, w, h) }
        override fun getFormat(): Int = ImageFormat.YUV_420_888
        override fun getHeight(): Int = h
        override fun getWidth(): Int = w
        override fun getPlanes(): Array<ImageProxy.PlaneProxy> = arrayOf(
            object : ImageProxy.PlaneProxy {
                override fun getRowStride(): Int = stride
                override fun getPixelStride(): Int = 1
                override fun getBuffer(): ByteBuffer = luma
            }
        )
        override fun getImageInfo(): ImageInfo = object : ImageInfo {
            override fun getTagBundle(): TagBundle = TagBundle.emptyBundle()
            override fun getTimestamp(): Long = 0L
            override fun getRotationDegrees(): Int = 0
            override fun populateExifData(exifBuilder: ExifData.Builder) = Unit
        }
        override fun getImage(): Image? = null
    }
}
