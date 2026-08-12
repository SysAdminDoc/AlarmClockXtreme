package com.sysadmindoc.alarmclock.ui.onboarding

import android.Manifest
import android.app.Instrumentation
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingTestAlarmDeviceSmokeTest {

    @Test
    fun alarmManagerWakesTheLockedScreenIntoTheTestAlarmActivity() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ApplicationProvider.getApplicationContext<Context>()
        grantNotificationPermission(context)
        wakeScreenIfNeeded(instrumentation)

        val scheduleResult = OnboardingTestAlarm.schedule(context)
        assertTrue("Test alarm could not be scheduled: $scheduleResult", scheduleResult.isSuccess)

        try {
            executeShell(instrumentation, "input keyevent KEYCODE_SLEEP")
            delay(750L)
            val powerManager = context.getSystemService(PowerManager::class.java)
            assertNotNull("PowerManager is unavailable", powerManager)
            assertFalse("The smoke must enter the screen-off state before firing", powerManager!!.isInteractive)

            val proof = waitForProof(context)
            assertTrue("AlarmManager did not record a fired test alarm: $proof", proof.firedAt > 0L)
            assertTrue("The test alarm did not request a full-screen intent: $proof", proof.fullScreenIntentRequested)
            assertTrue("The test alarm activity did not launch: $proof", proof.activityLaunchSucceeded)
            assertTrue("The test alarm fired before it was scheduled: $proof", proof.firedAt >= proof.scheduledAt)
            assertTrue("The alarm did not wake the screen", powerManager.isInteractive)

            val activity = waitForResumedTestActivity()
            assertNotNull("The test alarm activity was not resumed", activity)
            val activityDump = executeShellOutput(instrumentation, "dumpsys activity activities")
            val lockScreenLines = activityDump.lineSequence()
                .filter { line ->
                    line.contains("OnboardingTestAlarmActivity") ||
                        line.contains("showWhenLocked", ignoreCase = true) ||
                        line.contains("mCurrentFocus")
                }
                .joinToString("\n")
            assertTrue(
                "The test alarm activity was not configured for the lock screen. Activity state:\n$lockScreenLines",
                activityDump.contains("showWhenLocked=true", ignoreCase = true) ||
                    activityDump.contains("mShowWhenLocked=true", ignoreCase = true)
            )
        } finally {
            OnboardingTestAlarm.cancel(context)
            finishResumedTestActivities()
        }
    }

    private suspend fun waitForProof(context: Context): com.sysadmindoc.alarmclock.data.readiness.TestAlarmProof {
        repeat(60) {
            val proof = OnboardingTestAlarm.lastProof(context)
            if (proof.firedAt > 0L && proof.activityLaunchSucceeded) return proof
            delay(500L)
        }
        return OnboardingTestAlarm.lastProof(context)
    }

    private suspend fun waitForResumedTestActivity(): OnboardingTestAlarmActivity? {
        repeat(20) {
            resumedTestActivity()?.let { return it }
            delay(250L)
        }
        return resumedTestActivity()
    }

    private fun resumedTestActivity(): OnboardingTestAlarmActivity? {
        var activity: OnboardingTestAlarmActivity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<OnboardingTestAlarmActivity>()
                .firstOrNull()
        }
        return activity
    }

    private fun finishResumedTestActivities() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activities = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<OnboardingTestAlarmActivity>()
            activities.forEach { activity ->
                activity.finish()
            }
        }
    }

    private fun grantNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun wakeScreenIfNeeded(instrumentation: Instrumentation) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(PowerManager::class.java)
        if (powerManager?.isInteractive == false) {
            executeShell(instrumentation, "input keyevent KEYCODE_WAKEUP")
        }
    }

    private fun executeShell(
        instrumentation: Instrumentation,
        command: String
    ) {
        executeShellOutput(instrumentation, command)
    }

    private fun executeShellOutput(
        instrumentation: Instrumentation,
        command: String
    ): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
            it.readBytes().toString(Charsets.UTF_8)
        }
    }
}
