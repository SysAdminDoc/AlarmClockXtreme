package com.sysadmindoc.alarmclock.ui.alarmfiring

/**
 * Keeps Photo Match to one camera at a time. A second camera would stack on
 * the first and its photo would never be scored. A capture is in flight from
 * the tap until its photo, a refusal or a failed launch comes back, or until
 * the user returns to the ringing screen with the camera left open somewhere
 * else (Home, then the notification).
 *
 * Every call comes while the ringing screen is showing: a tap whose unlock
 * answers while it's hidden waits for it ([unlockThenWhenShown]), and a permission
 * answer arrives as the screen starts.
 */
internal class PhotoCaptureGate {
    var inFlight = false
        private set

    // The camera covered the ringing screen. Only then can coming back mean
    // the camera was left open elsewhere.
    private var leftScreen = false

    /** A tap, once the phone is unlocked. False while a capture is already under way. */
    fun start(): Boolean {
        if (inFlight) return false
        inFlight = true
        return true
    }

    /**
     * The camera is about to open. Marks the capture in flight even without a
     * tap on this screen: a rotation during the permission dialog recreates
     * it, and the grant comes back to a screen that never saw the tap.
     */
    fun cameraOpening() {
        inFlight = true
        // A permission dialog that stopped the screen isn't the camera.
        leftScreen = false
    }

    /** The photo came back, the permission was refused or no camera app opened. */
    fun finished() {
        inFlight = false
        leftScreen = false
    }

    fun onStop() {
        if (inFlight) leftScreen = true
    }

    fun onResume() {
        // Not after a permission dialog, though: the grant opens the camera
        // just before this resume, and the camera may still be starting.
        if (leftScreen) finished()
    }
}
