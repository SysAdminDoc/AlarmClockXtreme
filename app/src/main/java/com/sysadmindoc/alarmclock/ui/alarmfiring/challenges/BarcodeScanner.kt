package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import zxingcpp.BarcodeReader

/** The barcode challenge's match rule: the same text once surrounding spaces are dropped. */
object BarcodeChallengeMatcher {
    fun matches(scanned: String, registered: String): Boolean = scanned.trim() == registered.trim()
}

private const val TAG = "BarcodeScanner"

/**
 * Reads barcodes and QR codes out of CameraX frames. Frames are decoded in
 * memory and closed straight away, never kept or written anywhere. Every code
 * in view is reported, so a second code beside the saved one can't hide it,
 * and each distinct code only once, so holding the camera on a code doesn't
 * repeat it every frame.
 */
class BarcodeFrameAnalyzer(
    private val reader: BarcodeReader,
    private val onCode: (String) -> Unit,
    private val onError: (Throwable) -> Unit
) : ImageAnalysis.Analyzer {
    private val seen = HashSet<String>()
    private var failedFrames = 0
    private var errorReported = false

    override fun analyze(image: ImageProxy) {
        val codes = image.use { frame ->
            try {
                reader.read(frame).mapNotNull { result -> result.text?.takeIf { it.isNotBlank() } }
                    .also { failedFrames = 0 }
            } catch (e: Exception) {
                // A bad frame now and then shouldn't end the scan, but a
                // decoder that fails frame after frame has to reach the screen.
                Log.w(TAG, "Barcode frame decode failed", e)
                failedFrames++
                if (failedFrames >= FAILED_FRAMES_LIMIT && !errorReported) {
                    errorReported = true
                    onError(e)
                }
                emptyList()
            }
        }
        codes.filter(seen::add).forEach(onCode)
    }

    companion object {
        /** Failed frames in a row before the decoder counts as broken. */
        const val FAILED_FRAMES_LIMIT = 10

        fun newReader(): BarcodeReader = BarcodeReader(
            BarcodeReader.Options(tryHarder = true, tryRotate = true, tryInvert = true)
        )
    }
}

/**
 * Calls [onTimeout] once the preview has gone [TIMEOUT_MS] without a picture
 * while the screen is in front. Some failures never reach the camera state,
 * such as a bind CameraX drops quietly or a camera another app holds, and a
 * preview with no picture leaves the code unreadable. Time in the background
 * doesn't count, since the camera is closed there anyway.
 */
internal class NoPictureWatchdog(private val handler: Handler, private val onTimeout: () -> Unit) {
    private val timeout = Runnable { onTimeout() }
    private var streaming = false
    private var inFront = false

    fun onStreaming(streaming: Boolean) {
        if (this.streaming == streaming) return
        this.streaming = streaming
        rearm()
    }

    fun onInFront(inFront: Boolean) {
        if (this.inFront == inFront) return
        this.inFront = inFront
        rearm()
    }

    fun stop() = handler.removeCallbacks(timeout)

    private fun rearm() {
        handler.removeCallbacks(timeout)
        if (inFront && !streaming) handler.postDelayed(timeout, TIMEOUT_MS)
    }

    companion object {
        const val TIMEOUT_MS = 15_000L
    }
}

/**
 * Live camera preview that reports each code it reads to [onCodeRead] on the
 * main thread. Uses the back camera, or the front one on a device without
 * it. [onCameraError] gets the first thing that stops scanning: no camera, a
 * camera that won't open or fails once open, a decoder that can't load or
 * keeps failing, or a preview that shows no picture for 15 seconds while the
 * screen is in front (a camera busy in another app ends up here too).
 */
@Composable
fun BarcodeCameraView(
    onCodeRead: (String) -> Unit,
    onCameraError: (Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val codeRead by rememberUpdatedState(onCodeRead)
    val cameraError by rememberUpdatedState(onCameraError)
    val previewView = remember(context) {
        PreviewView(context).apply {
            // TextureView: a SurfaceView ignores the rounded clip and can
            // draw over the dialog that hosts the register scan.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val controller = LifecycleCameraController(context)
        var disposed = false
        var failed = false
        var cameraState: LiveData<CameraState>? = null
        fun fail(error: Throwable) {
            mainExecutor.execute {
                if (!disposed && !failed) {
                    failed = true
                    cameraError(error)
                }
            }
        }
        val noPicture = NoPictureWatchdog(Handler(Looper.getMainLooper())) {
            fail(IllegalStateException("No camera picture"))
        }
        val streamObserver = Observer<PreviewView.StreamState> { stream ->
            noPicture.onStreaming(stream == PreviewView.StreamState.STREAMING)
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            noPicture.onInFront(event.targetState == Lifecycle.State.RESUMED)
        }
        previewView.previewStreamState.observe(lifecycleOwner, streamObserver)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        // Opening the camera happens after binding, so a camera that's
        // disabled by policy, removed or broken only shows up in its state.
        val stateObserver = Observer<CameraState> { state ->
            val error = state.error
            if (error != null && error.type == CameraState.ErrorType.CRITICAL) {
                fail(IllegalStateException("Camera error ${error.code}"))
            }
        }
        try {
            val analyzer = BarcodeFrameAnalyzer(
                reader = BarcodeFrameAnalyzer.newReader(),
                onCode = { code -> mainExecutor.execute { if (!disposed) codeRead(code) } },
                onError = ::fail
            )
            controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            controller.setImageAnalysisAnalyzer(analysisExecutor, analyzer)
            controller.initializationFuture.addListener({
                try {
                    controller.initializationFuture.get()
                    if (disposed) return@addListener
                    when {
                        controller.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> Unit
                        controller.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                            controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> {
                            fail(IllegalStateException("No camera found"))
                            return@addListener
                        }
                    }
                    // Bound only once a camera is picked. CameraX binds as soon
                    // as it's ready, with the back camera, and on a phone with
                    // only a front one that bind fails.
                    controller.bindToLifecycle(lifecycleOwner)
                    cameraState = controller.cameraInfo?.cameraState
                    cameraState?.observe(lifecycleOwner, stateObserver)
                        ?: Log.w(TAG, "No camera state to watch yet, so only a missing picture is reported")
                } catch (e: Exception) {
                    fail(e)
                }
            }, mainExecutor)
            previewView.controller = controller
        } catch (e: Exception) {
            fail(e)
        } catch (e: UnsatisfiedLinkError) {
            // The decoder's native library is missing for this device's ABI.
            fail(e)
        }
        onDispose {
            disposed = true
            noPicture.stop()
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            previewView.previewStreamState.removeObserver(streamObserver)
            cameraState?.removeObserver(stateObserver)
            previewView.controller = null
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
