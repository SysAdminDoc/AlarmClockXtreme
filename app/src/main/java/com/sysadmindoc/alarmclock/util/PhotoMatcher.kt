package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * F16: Simple photo similarity comparison using pixel histogram matching.
 *
 * Both bitmaps are scaled to 64x64 and compared per-channel histogram.
 * Returns a score from 0.0 (completely different) to 1.0 (identical).
 * No external library required.
 */
object PhotoMatcher {

    private const val THUMB_SIZE = 64
    private const val HISTOGRAM_BINS = 16

    /**
     * A picked photo's long side is decoded between this and twice this, or
     * at its own size when it's smaller.
     */
    private const val PICKED_TARGET_SIDE = 1024

    /**
     * Compare [captured] bitmap against the reference photo stored at [referenceUri].
     * @return similarity score 0.0–1.0
     */
    fun compare(context: Context, captured: Bitmap, referenceUri: String): Float {
        if (referenceUri.isBlank()) return 1f // No reference registered — always pass

        val refBitmap = loadReference(context, referenceUri) ?: return 1f

        val capturedThumb = Bitmap.createScaledBitmap(captured, THUMB_SIZE, THUMB_SIZE, true)
        val refThumb = Bitmap.createScaledBitmap(refBitmap, THUMB_SIZE, THUMB_SIZE, true)

        val score = try {
            val capturedHist = buildHistogram(capturedThumb)
            val refHist = buildHistogram(refThumb)
            histogramIntersection(capturedHist, refHist)
        } finally {
            // Free the intermediate thumbs and the loaded reference promptly so
            // a sequence of failed photo-match attempts doesn't accumulate large
            // bitmaps in the GC roots.
            if (capturedThumb !== captured) capturedThumb.recycle()
            if (refThumb !== refBitmap) refThumb.recycle()
            refBitmap.recycle()
        }
        return score
    }

    /**
     * Save a reference bitmap to filesDir for later retrieval. It's written
     * to its own temp file, synced, and moved over the old one in one step,
     * so a failed or interrupted replace keeps the alarm's previous reference
     * intact and two saves at once can't write into the same file.
     * @return absolute file path (URI) of the saved file
     */
    fun saveReference(context: Context, alarmId: Long, bitmap: Bitmap): String {
        val file = File(context.filesDir, "photo_match_$alarmId.jpg")
        val temp = File.createTempFile("photo_match_$alarmId", ".tmp", context.filesDir)
        try {
            val written = FileOutputStream(temp).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out).also { if (it) out.fd.sync() }
            }
            check(written) { "JPEG compress failed" }
            Files.move(
                temp.toPath(), file.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
            )
        } finally {
            temp.delete()
        }
        return file.absolutePath
    }

    /**
     * Reads a picked image for use as a reference: decoded at a fraction of
     * its size, so a huge photo can't run the app out of memory, and turned
     * upright from its EXIF orientation. Null when it can't be read.
     */
    fun decodePicked(context: Context, uri: Uri, targetSide: Int = PICKED_TARGET_SIDE): Bitmap? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (longSide <= 0) {
            null
        } else {
            var sample = 1
            while (longSide / (sample * 2) >= targetSide) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
            val orientation = runCatching {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
            decoded?.let { upright(it, orientation) }
        }
    } catch (_: Exception) {
        null
    }

    private fun upright(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply { postRotate(270f); postScale(-1f, 1f) }
            else -> return bitmap
        }
        val turned = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (turned !== bitmap) bitmap.recycle()
        return turned
    }

    private fun loadReference(context: Context, referenceUri: String): Bitmap? {
        return try {
            // referenceUri is an absolute file path saved by saveReference()
            val file = File(referenceUri)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                BitmapFactory.decodeStream(context.contentResolver.openInputStream(Uri.parse(referenceUri)))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildHistogram(bitmap: Bitmap): FloatArray {
        // 3 channels × HISTOGRAM_BINS bins each
        val hist = FloatArray(3 * HISTOGRAM_BINS)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val total = pixels.size.toFloat()

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF) * HISTOGRAM_BINS / 256
            val g = (pixel shr 8 and 0xFF) * HISTOGRAM_BINS / 256
            val b = (pixel and 0xFF) * HISTOGRAM_BINS / 256
            hist[r]++
            hist[HISTOGRAM_BINS + g]++
            hist[2 * HISTOGRAM_BINS + b]++
        }

        // Normalize
        for (i in hist.indices) hist[i] /= total
        return hist
    }

    /** Histogram intersection similarity: sum of min(a,b) per bin, normalized to [0,1] */
    private fun histogramIntersection(a: FloatArray, b: FloatArray): Float {
        var intersection = 0f
        for (i in a.indices) {
            intersection += minOf(a[i], b[i])
        }
        // Each channel sums to 1.0 when normalized, so 3 channels → max intersection = 3.0
        return (intersection / 3f).coerceIn(0f, 1f)
    }
}
