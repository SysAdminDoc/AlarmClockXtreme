package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.Challenge
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ChallengeType
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import com.sysadmindoc.alarmclock.util.ShakeDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Full-screen Activity shown when an alarm fires.
 * Shows on lock screen, turns screen on, handles dismiss challenges.
 */
@AndroidEntryPoint
class AlarmFiringActivity : ComponentActivity() {

    private val viewModel: AlarmFiringViewModel by viewModels()
    private var shakeDetector: ShakeDetector? = null
    private var alarmId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1

        // Show on lock screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val km = getSystemService(KeyguardManager::class.java)
        km?.requestDismissKeyguard(this, null)

        // Prevent accidental dismiss via back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* Block back press during alarm */ }
        })

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        // Start shake detection if needed
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val challenge = state.challenge
                if (challenge is Challenge.ShakeChallenge && !state.challengeSolved) {
                    startShakeDetection()
                } else {
                    stopShakeDetection()
                }
            }
        }

        // Flash wake - gradually increase screen brightness
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val alarm = state.alarm ?: return@collectLatest
                if (alarm.flashWake && alarm.gradualVolumeSeconds > 0) {
                    startFlashWake(alarm.gradualVolumeSeconds)
                }
            }
        }

        setContent {
            AlarmClockXtremeTheme {
                AlarmFiringScreen(
                    onDismiss = { dismiss() },
                    onSnooze = { snooze() },
                    onSnoozeCustom = { minutes -> snooze(minutes) },
                    viewModel = viewModel
                )
            }
        }
    }

    private fun startShakeDetection() {
        if (shakeDetector != null) return
        shakeDetector = ShakeDetector(this) { count ->
            viewModel.updateShakeCount(count)
        }.also { it.start() }
    }

    private fun stopShakeDetection() {
        shakeDetector?.stop()
        shakeDetector = null
    }

    private fun snooze(customMinutes: Int? = null) {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            if (customMinutes != null) {
                putExtra(AlarmService.EXTRA_CUSTOM_SNOOZE_MINUTES, customMinutes)
            }
        }
        startForegroundService(intent)
        finish()
    }

    private fun dismiss() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        startForegroundService(intent)
        finish()
    }

    private var flashWakeJob: kotlinx.coroutines.Job? = null

    private fun startFlashWake(durationSeconds: Int) {
        if (flashWakeJob != null) return
        flashWakeJob = lifecycleScope.launch {
            val steps = 50
            val stepDelay = (durationSeconds * 1000L) / steps
            // Start with minimum brightness
            window.attributes = window.attributes.also {
                it.screenBrightness = 0.01f
            }
            for (i in 1..steps) {
                delay(stepDelay)
                val brightness = i.toFloat() / steps
                window.attributes = window.attributes.also {
                    it.screenBrightness = brightness
                }
            }
        }
    }

    override fun onDestroy() {
        flashWakeJob?.cancel()
        stopShakeDetection()
        super.onDestroy()
    }
}
