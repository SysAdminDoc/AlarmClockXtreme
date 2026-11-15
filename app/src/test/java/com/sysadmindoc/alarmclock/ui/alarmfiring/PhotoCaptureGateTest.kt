package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.app.Activity
import android.app.KeyguardManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Photo Match opens one camera at a time, and Take photo works again once the
 * capture is over, including when the camera was left open somewhere else.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PhotoCaptureGateTest {

    private val gate = PhotoCaptureGate()

    private class Screen(state: Lifecycle.State) : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).also { it.currentState = state }
        override val lifecycle: Lifecycle get() = registry
    }

    private val activity = mockk<Activity>(relaxed = true)
    private var locked = true
    private var needsPin = true
    private val keyguard = mockk<KeyguardManager>(relaxed = true).also {
        every { it.isKeyguardLocked } answers { locked }
        every { it.isDeviceLocked } answers { needsPin }
    }
    private val unlocks = mutableListOf<KeyguardManager.KeyguardDismissCallback>()

    /**
     * The PIN goes in, and every unlock asked for answers. The lock screen is
     * gone by then, but isDeviceLocked can still read true for a moment.
     */
    private fun enterPin() {
        locked = false
        unlocks.forEach { it.onDismissSucceeded() }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { keyguard.requestDismissKeyguard(activity, capture(unlocks)) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `two quick taps open one camera`() {
        assertTrue(gate.start())
        gate.cameraOpening()
        assertFalse(gate.start())
    }

    @Test
    fun `a tap right after allowing the camera opens no second camera`() {
        assertTrue(gate.start())
        // The permission dialog stopped the ringing screen, then the grant
        // arrives before the resume that follows it.
        gate.onStop()
        gate.cameraOpening()
        gate.onResume()
        assertFalse(gate.start())
    }

    @Test
    fun `a grant that reaches a recreated screen still blocks a second tap`() {
        // Rotated during the permission dialog: this screen never saw the tap.
        gate.cameraOpening()
        gate.onResume()
        assertFalse(gate.start())
    }

    @Test
    fun `coming back with the camera left open elsewhere lets Take photo work again`() {
        assertTrue(gate.start())
        gate.cameraOpening()
        gate.onStop()
        gate.onResume()
        assertTrue(gate.start())
    }

    @Test
    fun `a photo coming back lets the next tap open the camera`() {
        assertTrue(gate.start())
        gate.cameraOpening()
        gate.onStop()
        gate.finished()
        gate.onResume()
        assertTrue(gate.start())
    }

    @Test
    fun `a refused permission or a missing camera app lets the next tap try again`() {
        assertTrue(gate.start())
        gate.finished()
        assertTrue(gate.start())
        gate.cameraOpening()
        gate.finished()
        assertTrue(gate.start())
    }

    @Test
    fun `a resume with nothing in flight changes nothing`() {
        gate.onStop()
        gate.onResume()
        assertTrue(gate.start())
        gate.onResume()
        assertFalse(gate.start())
    }

    @Test
    fun `unlocks that answer while the ringing screen is hidden wait for it to come back`() {
        val screen = Screen(Lifecycle.State.RESUMED)
        var cameras = 0
        // Two quick taps, then the PIN pad hides the ringing screen.
        repeat(2) {
            unlockThenWhenShown(activity, screen, keyguard) {
                if (gate.start()) {
                    gate.cameraOpening()
                    cameras++
                }
            }
        }
        screen.registry.currentState = Lifecycle.State.CREATED
        enterPin()
        screen.registry.currentState = Lifecycle.State.STARTED
        assertEquals(0, cameras)

        // The ringing screen's own onResume runs before the lifecycle tells
        // the waiting steps, so it can't free the camera they open.
        gate.onResume()
        screen.registry.currentState = Lifecycle.State.RESUMED
        assertEquals(1, cameras)
        assertFalse(gate.start())
    }

    @Test
    fun `a step waits for the ringing screen to resume, not just start`() {
        val screen = Screen(Lifecycle.State.STARTED)
        var ran = 0
        unlockThenWhenShown(activity, screen, keyguard) { ran++ }
        enterPin()
        assertEquals(0, ran)
        screen.registry.currentState = Lifecycle.State.RESUMED
        assertEquals(1, ran)
    }

    @Test
    fun `a step runs at once when the unlock answers over the ringing screen`() {
        val screen = Screen(Lifecycle.State.RESUMED)
        var ran = 0
        unlockThenWhenShown(activity, screen, keyguard) { ran++ }
        assertEquals(0, ran)
        enterPin()
        assertEquals(1, ran)

        // Hiding and showing the screen later doesn't run it again.
        screen.registry.currentState = Lifecycle.State.CREATED
        screen.registry.currentState = Lifecycle.State.RESUMED
        assertEquals(1, ran)
    }

    @Test
    fun `a step waiting for the ringing screen runs once`() {
        val screen = Screen(Lifecycle.State.CREATED)
        var ran = 0
        unlockThenWhenShown(activity, screen, keyguard) { ran++ }
        enterPin()
        screen.registry.currentState = Lifecycle.State.RESUMED
        screen.registry.currentState = Lifecycle.State.CREATED
        screen.registry.currentState = Lifecycle.State.RESUMED
        assertEquals(1, ran)
    }

    @Test
    fun `a phone locked again before the ringing screen comes back drops the step`() {
        // The camera would open behind the lock screen.
        val screen = Screen(Lifecycle.State.CREATED)
        var ran = 0
        unlockThenWhenShown(activity, screen, keyguard) { ran++ }
        enterPin()
        locked = true
        screen.registry.currentState = Lifecycle.State.RESUMED
        assertEquals(0, ran)
    }

    @Test
    fun `a ringing screen that comes back only to close runs nothing`() {
        // A newer alarm brings the screen forward and closes it from
        // onNewIntent: it starts while not yet finishing, then never resumes.
        val screen = Screen(Lifecycle.State.CREATED)
        var ran = 0
        unlockThenWhenShown(activity, screen, keyguard) { ran++ }
        enterPin()
        screen.registry.currentState = Lifecycle.State.STARTED
        every { activity.isFinishing } returns true
        screen.registry.currentState = Lifecycle.State.DESTROYED
        assertEquals(0, ran)
    }

    @Test
    fun `a ringing screen closed while hidden runs nothing`() {
        val screen = Screen(Lifecycle.State.CREATED)
        var ran = 0
        unlockThenWhenShown(activity, screen, keyguard) { ran++ }
        enterPin()
        screen.registry.currentState = Lifecycle.State.DESTROYED
        assertEquals(0, ran)
    }

    @Test
    fun `an unlocked phone runs the step at once`() {
        locked = false
        needsPin = false
        val screen = Screen(Lifecycle.State.RESUMED)
        var ran = 0
        unlockThenWhenShown(activity, screen, keyguard) { ran++ }
        assertEquals(1, ran)
        assertEquals(0, unlocks.size)
    }
}
