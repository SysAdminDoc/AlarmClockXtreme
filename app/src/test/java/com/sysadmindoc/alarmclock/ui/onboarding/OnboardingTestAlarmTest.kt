package com.sysadmindoc.alarmclock.ui.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class OnboardingTestAlarmTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun firedAndCompletedProofRoundTrips() {
        val scheduledAt = 1_700_000_000_000L
        val firedAt = scheduledAt + 2_500L
        val beforeComplete = System.currentTimeMillis()

        OnboardingTestAlarm.recordFired(
            context = context,
            scheduledAt = scheduledAt,
            firedAt = firedAt,
            notificationPermissionGranted = true,
            fullScreenIntentRequested = true,
            activityLaunchSucceeded = true
        )
        OnboardingTestAlarm.markCompleted(context)

        val proof = OnboardingTestAlarm.lastProof(context)
        assertTrue(OnboardingTestAlarm.isCompleted(context))
        assertTrue(proof.hasDetailedCompletion)
        assertEquals(scheduledAt, proof.scheduledAt)
        assertEquals(firedAt, proof.firedAt)
        assertTrue(proof.completedAt >= beforeComplete)
        assertEquals(2_500L, proof.latencyMs)
        assertTrue(proof.notificationPermissionGranted)
        assertTrue(proof.fullScreenIntentRequested)
        assertTrue(proof.activityLaunchSucceeded)
        assertFalse(proof.legacyCompleted)
    }

    @Test
    fun scheduleClearsOldCompletionProofBeforeTheNewTestRings() {
        OnboardingTestAlarm.recordFired(
            context = context,
            scheduledAt = 1_000L,
            firedAt = 2_000L,
            notificationPermissionGranted = true,
            fullScreenIntentRequested = true,
            activityLaunchSucceeded = true
        )
        OnboardingTestAlarm.markCompleted(context)

        val beforeSchedule = System.currentTimeMillis()
        assertTrue(OnboardingTestAlarm.schedule(context).isSuccess)

        val proof = OnboardingTestAlarm.lastProof(context)
        assertFalse(proof.isCompleted)
        assertTrue(proof.scheduledAt >= beforeSchedule)
        assertEquals(0L, proof.firedAt)
        assertEquals(0L, proof.completedAt)
        assertFalse(proof.notificationPermissionGranted)
        assertFalse(proof.fullScreenIntentRequested)
        assertFalse(proof.activityLaunchSucceeded)
    }

    @Test
    fun legacyCompletionStillCountsButNeedsFreshProof() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("completed", true)
            .commit()

        val proof = OnboardingTestAlarm.lastProof(context)
        assertTrue(proof.isCompleted)
        assertFalse(proof.hasDetailedCompletion)
        assertTrue(proof.legacyCompleted)
    }

    companion object {
        private const val PREFS = "onboarding_test_alarm"
    }
}
