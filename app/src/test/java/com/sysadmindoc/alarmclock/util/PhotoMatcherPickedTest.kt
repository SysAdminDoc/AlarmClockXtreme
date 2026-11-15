package com.sysadmindoc.alarmclock.util

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A gallery photo picked as the Photo Match reference (Discussion #52). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class PhotoMatcherPickedTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `a huge photo is read at a fraction of its size`() {
        val uri = jpeg("huge.jpg", halves(3000, 2000))

        val picked = requireNotNull(PhotoMatcher.decodePicked(context, uri, targetSide = 256))

        val longSide = maxOf(picked.width, picked.height)
        assertTrue("long side was $longSide", longSide in 256 until 512)
        assertEquals(3000f / 2000f, picked.width.toFloat() / picked.height, 0.02f)
    }

    @Test
    fun `a photo saved sideways with an EXIF rotation comes out upright`() {
        // Stored as red on the left and blue on the right, tagged to be shown
        // turned a quarter clockwise, which puts red on top.
        val uri = jpeg("sideways.jpg", halves(200, 100), ExifInterface.ORIENTATION_ROTATE_90)

        val picked = requireNotNull(PhotoMatcher.decodePicked(context, uri))

        assertEquals(100, picked.width)
        assertEquals(200, picked.height)
        assertTrue(isRed(picked.getPixel(50, 30)))
        assertTrue(isBlue(picked.getPixel(50, 170)))
    }

    @Test
    fun `a picked photo is copied in, so deleting the original doesn't break the alarm`() {
        val scene = halves(400, 300)
        val original = File(context.cacheDir, "gallery.jpg")
        val uri = jpeg(original.name, scene)

        val picked = requireNotNull(PhotoMatcher.decodePicked(context, uri))
        val saved = PhotoMatcher.saveReference(context, 7L, picked)
        assertTrue(original.delete())

        assertTrue(File(saved).exists())
        assertEquals(context.filesDir, File(saved).parentFile)
        assertTrue(PhotoMatcher.compare(context, scene, saved) > 0.9f)
    }

    @Test
    fun `a replace that fails keeps the alarm's old reference`() {
        val saved = PhotoMatcher.saveReference(context, 8L, halves(400, 300))
        val before = File(saved).readBytes()
        val broken = halves(400, 300).apply { recycle() }

        val replace = runCatching { PhotoMatcher.saveReference(context, 8L, broken) }

        assertTrue(replace.isFailure)
        assertTrue(File(saved).readBytes().contentEquals(before))
        assertTrue(leftoverTemps(8L).isEmpty())
    }

    @Test
    fun `picking a new photo replaces the old reference`() {
        val first = PhotoMatcher.saveReference(context, 9L, halves(400, 300))
        val plain = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }

        val second = PhotoMatcher.saveReference(context, 9L, plain)

        assertEquals(first, second)
        assertTrue(PhotoMatcher.compare(context, plain, second) > 0.9f)
        assertTrue(leftoverTemps(9L).isEmpty())
    }

    @Test
    fun `a move that fails leaves no temp file behind`() {
        // A non-empty folder where the photo should go: the file can't be moved over it.
        val blocked = File(context.filesDir, "photo_match_10.jpg").apply { mkdirs() }
        File(blocked, "keep").writeText("x")

        val save = runCatching { PhotoMatcher.saveReference(context, 10L, halves(400, 300)) }

        assertTrue(save.isFailure)
        assertTrue(File(blocked, "keep").exists())
        assertTrue(leftoverTemps(10L).isEmpty())
    }

    private fun leftoverTemps(alarmId: Long): List<String> =
        context.filesDir.list().orEmpty().filter { it.startsWith("photo_match_$alarmId") && it.endsWith(".tmp") }

    @Test
    fun `a photo that can't be opened gives nothing`() {
        assertNull(PhotoMatcher.decodePicked(context, Uri.parse("content://nowhere/1")))
        assertNull(PhotoMatcher.decodePicked(context, Uri.fromFile(File(context.cacheDir, "missing.jpg"))))
    }

    @Test
    fun `a file that isn't an image gives nothing`() {
        val notAnImage = File(context.cacheDir, "notes.jpg").apply { writeText("not a picture") }

        assertNull(PhotoMatcher.decodePicked(context, Uri.fromFile(notAnImage)))
    }

    private fun halves(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint()
            paint.color = Color.RED
            canvas.drawRect(0f, 0f, width / 2f, height.toFloat(), paint)
            paint.color = Color.BLUE
            canvas.drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), paint)
        }

    private fun jpeg(name: String, bitmap: Bitmap, orientation: Int? = null): Uri {
        val file = File(context.cacheDir, name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (orientation != null) {
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
        }
        return Uri.fromFile(file)
    }

    private fun isRed(pixel: Int) = Color.red(pixel) > 200 && Color.blue(pixel) < 60

    private fun isBlue(pixel: Int) = Color.blue(pixel) > 200 && Color.red(pixel) < 60
}
