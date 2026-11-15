package com.sysadmindoc.alarmclock.ui.alarmfiring

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
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

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
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
        val screen = Screen(Lifecycle.State.CREATED)
        var cameras = 0
        // Two quick taps, both answered once the PIN is in.
        repeat(2) {
            runWhenShown(screen) {
                if (gate.start()) {
                    gate.cameraOpening()
                    cameras++
                }
            }
        }
        assertEquals(0, cameras)

        screen.registry.currentState = Lifecycle.State.RESUMED
        // The resume right after can't free the camera that just started opening.
        gate.onResume()
        assertEquals(1, cameras)
        assertFalse(gate.start())
    }

    @Test
    fun `a step runs at once while the ringing screen shows`() {
        val screen = Screen(Lifecycle.State.RESUMED)
        var ran = 0
        runWhenShown(screen) { ran++ }
        assertEquals(1, ran)
    }

    @Test
    fun `a ringing screen closed while hidden runs nothing`() {
        val screen = Screen(Lifecycle.State.CREATED)
        var ran = 0
        runWhenShown(screen) { ran++ }
        screen.registry.currentState = Lifecycle.State.DESTROYED
        assertEquals(0, ran)
    }
}
