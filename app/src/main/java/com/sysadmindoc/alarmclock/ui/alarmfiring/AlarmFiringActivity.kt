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

        setContent {
            AlarmClockXtremeTheme {
                AlarmFiringScreen(
                    onDismiss = { dismiss() },
                    onSnooze = { snooze() },
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

    private fun snooze() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
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

    override fun onDestroy() {
        stopShakeDetection()
        super.onDestroy()
    }
}
