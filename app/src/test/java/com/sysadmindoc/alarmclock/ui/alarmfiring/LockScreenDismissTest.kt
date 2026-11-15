package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.app.Activity
import android.app.KeyguardManager
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.PIN_LOCK_RECHECK_MS
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.watchPinLock
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A ringing alarm on a locked PIN, pattern or password phone must not ask to
 * dismiss the lock screen by itself. That request put the PIN pad over the
 * ringing screen, hiding Snooze and Dismiss. Steps that need the phone
 * unlocked (another app's camera, an NFC tag) ask when the user starts them.
 */
class LockScreenDismissTest {

    private val activity = mockk<Activity>(relaxed = true)

    private fun keyguard(showing: Boolean, needsPin: Boolean) = mockk<KeyguardManager>(relaxed = true).also {
        every { it.isKeyguardLocked } returns showing
        every { it.isDeviceLocked } returns needsPin
    }

    @Test
    fun `a locked PIN or pattern phone keeps its lock under the ringing screen`() {
        val keyguard = keyguard(showing = true, needsPin = true)
        dismissLockScreenIfUnlocked(activity, keyguard)
        verify(exactly = 0) { keyguard.requestDismissKeyguard(any(), any()) }
    }

    @Test
    fun `a lock screen that needs no PIN is cleared`() {
        val keyguard = keyguard(showing = true, needsPin = false)
        dismissLockScreenIfUnlocked(activity, keyguard)
        verify(exactly = 1) { keyguard.requestDismissKeyguard(activity, null) }
    }

    @Test
    fun `nothing is dismissed when no lock screen is up`() {
        val keyguard = keyguard(showing = false, needsPin = false)
        dismissLockScreenIfUnlocked(activity, keyguard)
        verify(exactly = 0) { keyguard.requestDismissKeyguard(any(), any()) }
    }

    @Test
    fun `no keyguard service is not a crash`() {
        dismissLockScreenIfUnlocked(activity, null)
    }

    @Test
    fun `a step that needs the phone unlocked waits for the PIN`() {
        val keyguard = keyguard(showing = true, needsPin = true)
        val callback = slot<KeyguardManager.KeyguardDismissCallback>()
        every { keyguard.requestDismissKeyguard(activity, capture(callback)) } returns Unit
        var ran = 0

        unlockThen(activity, keyguard) { ran++ }
        assertEquals(0, ran)

        callback.captured.onDismissSucceeded()
        assertEquals(1, ran)
    }

    @Test
    fun `a lock screen that needs no PIN is still cleared before the step`() {
        // A swipe lock back after a power press: the camera or NFC can't work under it.
        val keyguard = keyguard(showing = true, needsPin = false)
        val callback = slot<KeyguardManager.KeyguardDismissCallback>()
        every { keyguard.requestDismissKeyguard(activity, capture(callback)) } returns Unit
        var ran = 0

        unlockThen(activity, keyguard) { ran++ }
        assertEquals(0, ran)

        callback.captured.onDismissSucceeded()
        assertEquals(1, ran)
    }

    @Test
    fun `a cancelled unlock doesn't run the step`() {
        val keyguard = keyguard(showing = true, needsPin = true)
        val callback = slot<KeyguardManager.KeyguardDismissCallback>()
        every { keyguard.requestDismissKeyguard(activity, capture(callback)) } returns Unit
        var ran = 0

        unlockThen(activity, keyguard) { ran++ }
        callback.captured.onDismissCancelled()

        assertEquals(0, ran)
    }

    @Test
    fun `a cancelled unlock doesn't run the step even when the lock already went`() {
        var showing = true
        val keyguard = mockk<KeyguardManager>(relaxed = true).also {
            every { it.isKeyguardLocked } answers { showing }
            every { it.isDeviceLocked } returns false
        }
        val callback = slot<KeyguardManager.KeyguardDismissCallback>()
        every { keyguard.requestDismissKeyguard(activity, capture(callback)) } returns Unit
        var ran = 0

        unlockThen(activity, keyguard) { ran++ }
        showing = false
        callback.captured.onDismissCancelled()

        assertEquals(0, ran)
    }

    @Test
    fun `an unlock that answers after the ringing screen closed doesn't run the step`() {
        val keyguard = keyguard(showing = true, needsPin = true)
        val callback = slot<KeyguardManager.KeyguardDismissCallback>()
        every { keyguard.requestDismissKeyguard(activity, capture(callback)) } returns Unit
        var ran = 0

        unlockThen(activity, keyguard) { ran++ }
        every { activity.isFinishing } returns true
        callback.captured.onDismissSucceeded()
        every { keyguard.isKeyguardLocked } returns false
        callback.captured.onDismissError()

        assertEquals(0, ran)
    }

    @Test
    fun `an unlock that answers after a rotation replaced the ringing screen doesn't run the step`() {
        // A recreated screen is destroyed without finishing, and its camera
        // launcher is already gone.
        val keyguard = keyguard(showing = true, needsPin = true)
        val callback = slot<KeyguardManager.KeyguardDismissCallback>()
        every { keyguard.requestDismissKeyguard(activity, capture(callback)) } returns Unit
        var ran = 0

        unlockThen(activity, keyguard) { ran++ }
        every { activity.isFinishing } returns false
        every { activity.isDestroyed } returns true
        callback.captured.onDismissSucceeded()

        assertEquals(0, ran)
    }

    @Test
    fun `a step whose unlock errors after the lock already went still runs`() {
        // Tapped while the swipe lock cleared on resume was still on its way out.
        var showing = true
        val keyguard = mockk<KeyguardManager>(relaxed = true).also {
            every { it.isKeyguardLocked } answers { showing }
            every { it.isDeviceLocked } returns false
        }
        val callback = slot<KeyguardManager.KeyguardDismissCallback>()
        every { keyguard.requestDismissKeyguard(activity, capture(callback)) } returns Unit
        var ran = 0

        unlockThen(activity, keyguard) { ran++ }
        showing = false
        callback.captured.onDismissError()

        assertEquals(1, ran)
    }

    @Test
    fun `a step whose unlock errors with the lock still up doesn't run`() {
        val keyguard = keyguard(showing = true, needsPin = true)
        val callback = slot<KeyguardManager.KeyguardDismissCallback>()
        every { keyguard.requestDismissKeyguard(activity, capture(callback)) } returns Unit
        var ran = 0

        unlockThen(activity, keyguard) { ran++ }
        callback.captured.onDismissError()

        assertEquals(0, ran)
    }

    @Test
    fun `an unlocked phone runs the step straight away`() {
        val keyguard = keyguard(showing = false, needsPin = false)
        var ran = 0

        unlockThen(activity, keyguard) { ran++ }
        unlockThen(activity, null) { ran++ }

        assertEquals(2, ran)
        verify(exactly = 0) { keyguard.requestDismissKeyguard(any(), any()) }
    }

    @Test
    fun `the PIN lock check reads at once and again every second`() = runTest {
        var needsPin = false
        val keyguard = mockk<KeyguardManager>(relaxed = true).also {
            every { it.isKeyguardLocked } answers { needsPin }
            every { it.isDeviceLocked } answers { needsPin }
        }
        val seen = mutableListOf<Boolean>()

        val watch = launch { watchPinLock(activity, keyguard) { seen += it } }
        runCurrent()
        assertEquals(listOf(false), seen)

        // The lock timer runs out while the ringing screen keeps the display on.
        needsPin = true
        advanceTimeBy(PIN_LOCK_RECHECK_MS)
        runCurrent()
        assertEquals(listOf(false, true), seen)

        // A fingerprint unlock, which gives the screen no lifecycle event either.
        needsPin = false
        advanceTimeBy(PIN_LOCK_RECHECK_MS)
        runCurrent()
        assertEquals(listOf(false, true, false), seen)
        watch.cancel()
        // A PIN lock is never dismissed from here, that would bring up the PIN pad.
        verify(exactly = 0) { keyguard.requestDismissKeyguard(any(), any()) }
    }

    @Test
    fun `the lock check clears a lock that needs no PIN when it comes back mid-ring`() = runTest {
        // The lock timer runs out on a phone Smart Lock keeps unlocked.
        var showing = false
        val keyguard = mockk<KeyguardManager>(relaxed = true).also {
            every { it.isKeyguardLocked } answers { showing }
            every { it.isDeviceLocked } returns false
        }
        val seen = mutableListOf<Boolean>()

        val watch = launch { watchPinLock(activity, keyguard) { seen += it } }
        runCurrent()
        verify(exactly = 0) { keyguard.requestDismissKeyguard(any(), any()) }

        showing = true
        advanceTimeBy(PIN_LOCK_RECHECK_MS)
        runCurrent()
        // One read isn't enough, see the next test.
        verify(exactly = 0) { keyguard.requestDismissKeyguard(any(), any()) }

        advanceTimeBy(PIN_LOCK_RECHECK_MS)
        runCurrent()
        watch.cancel()

        verify(exactly = 1) { keyguard.requestDismissKeyguard(activity, null) }
        // No unlock button for it: nothing to type.
        assertEquals(listOf(false, false, false), seen)
    }

    @Test
    fun `a lock seen once before SystemUI says it needs a PIN is never cleared`() = runTest {
        // The lock timer runs out on a PIN phone. For a moment the lock screen
        // is up and isDeviceLocked hasn't caught up yet.
        var showing = false
        var needsPin = false
        val keyguard = mockk<KeyguardManager>(relaxed = true).also {
            every { it.isKeyguardLocked } answers { showing }
            every { it.isDeviceLocked } answers { needsPin }
        }
        val seen = mutableListOf<Boolean>()

        val watch = launch { watchPinLock(activity, keyguard) { seen += it } }
        runCurrent()
        showing = true
        advanceTimeBy(PIN_LOCK_RECHECK_MS)
        runCurrent()
        needsPin = true
        advanceTimeBy(PIN_LOCK_RECHECK_MS)
        runCurrent()
        advanceTimeBy(PIN_LOCK_RECHECK_MS)
        runCurrent()
        watch.cancel()

        // Clearing then would have put the PIN pad over the ringing screen.
        verify(exactly = 0) { keyguard.requestDismissKeyguard(any(), any()) }
        assertEquals(listOf(false, false, true, true), seen)
    }

    @Test
    fun `a lagging read after an unlock and a second lock is never cleared`() = runTest {
        // Lock timer, fingerprint, lock timer again: each lock is read once
        // before SystemUI says it needs a PIN. Two such reads, with a PIN
        // read between them, aren't two reads in a row.
        val needsPinReads = ArrayDeque(listOf(false, true, false, true))
        val keyguard = mockk<KeyguardManager>(relaxed = true).also {
            every { it.isKeyguardLocked } returns true
            every { it.isDeviceLocked } answers { needsPinReads.removeFirst() }
        }

        val watch = launch { watchPinLock(activity, keyguard) { } }
        runCurrent()
        repeat(3) {
            advanceTimeBy(PIN_LOCK_RECHECK_MS)
            runCurrent()
        }
        watch.cancel()

        assertEquals(0, needsPinReads.size)
        verify(exactly = 0) { keyguard.requestDismissKeyguard(any(), any()) }
    }
}
