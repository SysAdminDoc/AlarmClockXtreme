package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.app.KeyguardManager
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.Challenge
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import com.sysadmindoc.alarmclock.util.FlipDetector
import com.sysadmindoc.alarmclock.util.PhotoMatcher
import com.sysadmindoc.alarmclock.util.ShakeDetector
import com.sysadmindoc.alarmclock.util.SquatDetector
import com.sysadmindoc.alarmclock.util.StepCounterListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen Activity shown when an alarm fires.
 * Shows on lock screen, turns screen on, handles dismiss challenges.
 */
@AndroidEntryPoint
class AlarmFiringActivity : ComponentActivity() {

    private val viewModel: AlarmFiringViewModel by viewModels()
    private var shakeDetector: ShakeDetector? = null
    private var squatDetector: SquatDetector? = null
    private var stepCounterListener: StepCounterListener? = null
    private var flipDetector: FlipDetector? = null
    private var nfcAdapter: NfcAdapter? = null
    private var alarmId: Long = -1
    private var wifiPollingJob: kotlinx.coroutines.Job? = null

    // F16: Camera launcher for photo-match challenge
    private val photoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val challenge = viewModel.uiState.value.challenge as? Challenge.PhotoMatchChallenge ?: return@registerForActivityResult
            lifecycleScope.launch(Dispatchers.Default) {
                val score = PhotoMatcher.compare(this@AlarmFiringActivity, bitmap, challenge.referencePhotoUri)
                withContext(Dispatchers.Main) {
                    viewModel.onPhotoTaken(score)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        // Defensive: if launched without a valid alarm id (rare — only really
        // possible from a stale full-screen-intent or a third party), get out
        // immediately rather than rendering broken state. The user will see
        // the firing notification's actions if the AlarmService is still up.
        if (alarmId == -1L) {
            finish()
            return
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Show on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
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

        // Observe challenge type and start/stop appropriate sensors
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val challenge = state.challenge
                when {
                    challenge is Challenge.ShakeChallenge && !state.challengeSolved -> startShakeDetection()
                    else -> stopShakeDetection()
                }
                when {
                    challenge is Challenge.WalkChallenge && !state.challengeSolved -> startWalkSteps()
                    else -> stopWalkSteps()
                }
                when {
                    challenge is Challenge.SquatChallenge && !state.challengeSolved -> startSquatDetection()
                    else -> stopSquatDetection()
                }
                when {
                    challenge is Challenge.WifiChallenge && !state.challengeSolved -> startWifiPolling()
                    else -> stopWifiPolling()
                }
            }
        }

        // F6: Flip-to-snooze — only register the sensor listener when:
        //   1) the alarm has loaded, AND
        //   2) the user has explicitly enabled the global flip-to-snooze setting.
        // (Previously the detector was registered unconditionally, which both wasted
        //  battery and could snooze alarms for users who never opted in.)
        lifecycleScope.launch {
            viewModel.flipToSnoozeEnabled.collectLatest { enabled ->
                if (enabled && viewModel.uiState.value.alarm != null) {
                    startFlipDetector()
                } else {
                    stopFlipDetector()
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
                // v1.2.0: Sunrise simulation — tint window background from dark red to warm yellow
                if (alarm.sunriseSimulation && alarm.sunriseMinutes > 0) {
                    startSunriseSimulation(alarm.sunriseMinutes)
                }
            }
        }

        setContent {
            AlarmClockXtremeTheme {
                AlarmFiringScreen(
                    onDismiss = { dismiss() },
                    onSnooze = { snooze() },
                    onSnoozeCustom = { minutes -> snooze(minutes) },
                    onTakePhoto = { photoLauncher.launch(null) },
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Enable NFC foreground dispatch for NFC_SCAN challenge
        val challenge = viewModel.uiState.value.challenge
        if (challenge is Challenge.NfcChallenge) {
            enableNfcForegroundDispatch()
        }
    }

    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle NFC tag tap
        if (intent.action in listOf(
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED
            )
        ) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            tag?.let {
                val tagId = it.id.joinToString("") { byte -> "%02x".format(byte) }
                viewModel.onNfcTagDetected(tagId)
            }
        }
    }

    private fun enableNfcForegroundDispatch() {
        try {
            val intent = Intent(this, AlarmFiringActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pi = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_MUTABLE
            )
            nfcAdapter?.enableForegroundDispatch(this, pi, null, null)
        } catch (_: Exception) {}
    }

    private fun disableNfcForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (_: Exception) {}
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

    private fun startWalkSteps() {
        if (stepCounterListener != null) return
        stepCounterListener = StepCounterListener(this) { steps ->
            viewModel.updateStepCount(steps)
        }.also { it.start() }
    }

    private fun stopWalkSteps() {
        stepCounterListener?.stop()
        stepCounterListener = null
    }

    private fun startSquatDetection() {
        if (squatDetector != null) return
        squatDetector = SquatDetector(this) { count ->
            viewModel.updateSquatCount(count)
        }.also { it.start() }
    }

    private fun stopSquatDetection() {
        squatDetector?.stop()
        squatDetector = null
    }

    private fun startWifiPolling() {
        if (wifiPollingJob != null) return
        wifiPollingJob = lifecycleScope.launch {
            val wifiManager = applicationContext.getSystemService(android.net.wifi.WifiManager::class.java)
            while (isActive) {
                @Suppress("DEPRECATION")
                val info = try { wifiManager?.connectionInfo } catch (_: SecurityException) { null }
                @Suppress("DEPRECATION")
                val rawSsid = info?.ssid?.removeSurrounding("\"") ?: ""
                if (rawSsid.isNotBlank() && rawSsid != "<unknown ssid>") {
                    viewModel.updateWifiSsid(rawSsid)
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun stopWifiPolling() {
        wifiPollingJob?.cancel()
        wifiPollingJob = null
    }

    private fun startFlipDetector() {
        if (flipDetector != null) return
        flipDetector = FlipDetector(
            context = this,
            onFaceDown = { snooze() },
            onFaceUp = { /* face-up alone does not dismiss; user must use button or swipe */ }
        ).also { it.start() }
    }

    private fun stopFlipDetector() {
        flipDetector?.stop()
        flipDetector = null
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

    private var sunriseJob: kotlinx.coroutines.Job? = null
    private var flashWakeJob: kotlinx.coroutines.Job? = null

    private fun startFlashWake(durationSeconds: Int) {
        if (flashWakeJob != null) return
        flashWakeJob = lifecycleScope.launch {
            val steps = 50
            val stepDelay = (durationSeconds * 1000L) / steps
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

    /**
     * v1.2.0: Sunrise simulation — gradually tint the window background
     * from dark red (0xFF330000) to warm yellow (0xFFFFCC00) over [minutes].
     */
    private fun startSunriseSimulation(minutes: Int) {
        if (sunriseJob != null) return
        sunriseJob = lifecycleScope.launch {
            val steps = 100
            val stepDelay = (minutes * 60 * 1000L) / steps
            for (i in 0..steps) {
                val fraction = i.toFloat() / steps
                // Interpolate RGB: dark red (51,0,0) -> warm yellow (255,204,0)
                val r = (51 + (204 * fraction)).toInt().coerceIn(0, 255)
                val g = (0 + (204 * fraction)).toInt().coerceIn(0, 255)
                val b = 0
                val color = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                window.decorView.setBackgroundColor(color)
                delay(stepDelay)
            }
        }
    }

    override fun onDestroy() {
        flashWakeJob?.cancel()
        sunriseJob?.cancel()
        stopShakeDetection()
        stopSquatDetection()
        stopWalkSteps()
        stopWifiPolling()
        stopFlipDetector()
        super.onDestroy()
    }
}
