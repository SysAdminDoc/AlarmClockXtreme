package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmFireDismissContract
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.Challenge
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import com.sysadmindoc.alarmclock.util.FlipDetector
import com.sysadmindoc.alarmclock.util.PhotoMatcher
import com.sysadmindoc.alarmclock.util.ProximityCoverDetector
import com.sysadmindoc.alarmclock.util.ShakeDetector
import com.sysadmindoc.alarmclock.util.SquatDetector
import com.sysadmindoc.alarmclock.util.PushUpDetector
import com.sysadmindoc.alarmclock.util.StepCounterListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Full-screen Activity shown when an alarm fires.
 * Shows on lock screen, turns screen on, handles dismiss challenges.
 */
@AndroidEntryPoint
class AlarmFiringActivity : ComponentActivity() {

    @Inject lateinit var alarmIncidentRepository: AlarmIncidentRepository

    private val viewModel: AlarmFiringViewModel by viewModels()
    private var shakeDetector: ShakeDetector? = null
    private var squatDetector: SquatDetector? = null
    private var pushUpDetector: PushUpDetector? = null
    private var stepCounterListener: StepCounterListener? = null
    private var flipDetector: FlipDetector? = null
    private var coverDetector: ProximityCoverDetector? = null
    private var nfcAdapter: NfcAdapter? = null
    private var nfcDispatchEnabled = false
    private var alarmId: Long = -1
    private var scheduledAt: Long = 0L
    private var fireId: String = ""
    private var wifiPollingJob: kotlinx.coroutines.Job? = null
    private var walkPermissionRequestInFlight = false
    private var wifiPermissionRequestInFlight = false
    private var locationDismissPermissionRequestInFlight = false
    private var locationDismissListener: LocationListener? = null

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
        } else {
            viewModel.onPhotoCaptureUnavailable("No photo captured. Try again.")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            photoLauncher.launch(null)
        } else {
            viewModel.onPhotoCaptureUnavailable("Camera permission is required for photo match.")
        }
    }

    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        walkPermissionRequestInFlight = false
        if (granted) {
            startWalkSteps()
        } else {
            viewModel.onWalkChallengeUnavailable("Activity recognition permission is required to count steps on this device.")
        }
    }

    private val wifiLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        wifiPermissionRequestInFlight = false
        if (granted) {
            startWifiPolling()
        } else {
            viewModel.onWifiChallengeUnavailable("Location permission is required for Android to reveal the current Wi-Fi network name.")
        }
    }

    private val locationDismissPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationDismissPermissionRequestInFlight = false
        if (granted) {
            startLocationDismissMonitoring()
        } else {
            viewModel.onLocationDismissUnavailable("Location permission is required before dismiss can unlock.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        scheduledAt = intent?.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0L) ?: 0L
        fireId = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID).orEmpty()
        // Defensive: if launched without a valid alarm id (rare — only really
        // possible from a stale full-screen-intent or a third party), get out
        // immediately rather than rendering broken state. The user will see
        // the firing notification's actions if the AlarmService is still up.
        if (alarmId == -1L) {
            finish()
            return
        }
        if (fireId.isBlank()) {
            fireId = AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)
        }
        recordIncidentAsync(
            type = AlarmIncidentEvent.TYPE_ACTIVITY_LAUNCH,
            status = AlarmIncidentEvent.STATUS_RECEIVED,
            reasonCode = "FIRING_ACTIVITY_CREATED"
        )
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
                    challenge is Challenge.PushUpChallenge && !state.challengeSolved -> startPushUpDetection()
                    else -> stopPushUpDetection()
                }
                when {
                    challenge is Challenge.WifiChallenge && !state.challengeSolved -> startWifiPolling()
                    else -> stopWifiPolling()
                }
                when {
                    state.alarm?.locationDismissEnabled == true && !state.locationDismissReady -> startLocationDismissMonitoring()
                    else -> stopLocationDismissMonitoring()
                }
                when {
                    challenge is Challenge.NfcChallenge && !state.challengeSolved -> enableNfcForegroundDispatch()
                    else -> disableNfcForegroundDispatch()
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

        // v1.4.0: Cover-to-snooze — hold a hand over the proximity sensor to snooze.
        // Registered only when the user has opted in (shares the proximity sensor
        // with FlipDetector, so we don't double-register when both are active).
        lifecycleScope.launch {
            viewModel.coverToSnoozeEnabled.collectLatest { enabled ->
                if (enabled && viewModel.uiState.value.alarm != null) {
                    startCoverDetector()
                } else {
                    stopCoverDetector()
                }
            }
        }

        // v1.5.1: Kick off flash-wake + sunrise simulation exactly once, the
        // first time the alarm becomes non-null. The jobs themselves have
        // start-guards, but previously `collectLatest` re-evaluated on every
        // state emission and cancelled the coroutine body mid-check, which
        // thrashed the dispatcher without effect.
        lifecycleScope.launch {
            viewModel.uiState
                .map { it.alarm }
                .filterNotNull()
                .distinctUntilChanged { a, b -> a.id == b.id }
                .collect { alarm ->
                    if (alarm.flashWake && alarm.gradualVolumeSeconds > 0) {
                        startFlashWake(alarm.gradualVolumeSeconds)
                    }
                    if (alarm.sunriseSimulation && alarm.sunriseMinutes > 0) {
                        startSunriseSimulation(alarm.sunriseMinutes)
                    }
                }
        }

        // v1.5.1: If the alarm row disappeared between schedule and fire,
        // the view model signals finish so we don't render a blank screen.
        lifecycleScope.launch {
            viewModel.finishEvents.collect {
                recordIncidentAsync(
                    type = AlarmIncidentEvent.TYPE_ACTIVITY_LAUNCH,
                    status = AlarmIncidentEvent.STATUS_SKIPPED,
                    reasonCode = "FIRING_ACTIVITY_FINISHED_BY_VIEWMODEL"
                )
                finish()
            }
        }

        setContent {
            AlarmClockXtremeTheme {
                AlarmFiringScreen(
                    onDismiss = { dismiss() },
                    onSnooze = { snooze() },
                    onSnoozeCustom = { minutes -> snooze(minutes) },
                    onTakePhoto = { launchPhotoCapture() },
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

    private fun launchPhotoCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            photoLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
        if (nfcDispatchEnabled) return
        try {
            val intent = Intent(this, AlarmFiringActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pi = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_MUTABLE
            )
            nfcAdapter?.enableForegroundDispatch(this, pi, null, null)
            nfcDispatchEnabled = true
        } catch (_: Exception) {}
    }

    private fun disableNfcForegroundDispatch() {
        if (!nfcDispatchEnabled) return
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (_: Exception) {
        } finally {
            nfcDispatchEnabled = false
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

    private fun startWalkSteps() {
        if (stepCounterListener != null) return
        if (viewModel.uiState.value.walkFallbackAllowed) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            if (!walkPermissionRequestInFlight) {
                walkPermissionRequestInFlight = true
                activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            return
        }

        val listener = StepCounterListener(this) { steps ->
            viewModel.updateStepCount(steps)
        }
        if (!listener.isAvailable()) {
            viewModel.onWalkChallengeUnavailable("This device does not expose a step sensor.")
            return
        }
        stepCounterListener = listener.also { it.start() }
    }

    private fun stopWalkSteps() {
        stepCounterListener?.stop()
        stepCounterListener = null
    }

    private fun startSquatDetection() {
        if (squatDetector != null) return
        val detector = SquatDetector(this) { count ->
            viewModel.updateSquatCount(count)
        }
        if (!detector.isAvailable()) {
            viewModel.onExerciseChallengeUnavailable("This device does not expose a motion sensor to count squats.")
            return
        }
        squatDetector = detector.also { it.start() }
    }

    private fun stopSquatDetection() {
        squatDetector?.stop()
        squatDetector = null
    }

    private fun startPushUpDetection() {
        if (pushUpDetector != null) return
        val detector = PushUpDetector(this) { count ->
            viewModel.updatePushUpCount(count)
        }
        if (!detector.isAvailable()) {
            viewModel.onExerciseChallengeUnavailable("This device does not expose a motion sensor to count push-ups.")
            return
        }
        pushUpDetector = detector.also { it.start() }
    }

    private fun stopPushUpDetection() {
        pushUpDetector?.stop()
        pushUpDetector = null
    }

    private fun startWifiPolling() {
        if (wifiPollingJob != null) return
        if (viewModel.uiState.value.wifiFallbackAllowed) return
        // Android 12+ requires ACCESS_FINE_LOCATION for WifiManager.connectionInfo to
        // return a real SSID. Coarse-only always yields "<unknown ssid>" on API 31+.
        val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.ACCESS_FINE_LOCATION
        } else {
            Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (ContextCompat.checkSelfPermission(this, requiredPermission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            if (!wifiPermissionRequestInFlight) {
                wifiPermissionRequestInFlight = true
                wifiLocationPermissionLauncher.launch(requiredPermission)
            }
            return
        }
        val wifiManager = applicationContext.getSystemService(android.net.wifi.WifiManager::class.java)
        if (wifiManager == null) {
            viewModel.onWifiChallengeUnavailable("This device does not expose Wi-Fi connection details.")
            return
        }
        wifiPollingJob = lifecycleScope.launch {
            var unknownSsidCount = 0
            while (isActive) {
                @Suppress("DEPRECATION")
                val info = try {
                    wifiManager.connectionInfo
                } catch (_: SecurityException) {
                    viewModel.onWifiChallengeUnavailable("Android blocked access to the current Wi-Fi network name.")
                    return@launch
                }
                @Suppress("DEPRECATION")
                val rawSsid = info?.ssid?.removeSurrounding("\"") ?: ""
                if (rawSsid.isNotBlank() && rawSsid != "<unknown ssid>") {
                    unknownSsidCount = 0
                    viewModel.updateWifiSsid(rawSsid)
                } else {
                    // After 5 consecutive unknown-SSID results (~10 s), trigger the
                    // fallback so the challenge doesn't hang silently. This covers the
                    // case where the user granted coarse-only location before API 31
                    // and then upgraded, or the device simply isn't on Wi-Fi.
                    unknownSsidCount++
                    if (unknownSsidCount >= 5) {
                        viewModel.onWifiChallengeUnavailable("Unable to read the current Wi-Fi network. Make sure location is enabled and you are connected to Wi-Fi.")
                        return@launch
                    }
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun stopWifiPolling() {
        wifiPollingJob?.cancel()
        wifiPollingJob = null
    }

    private fun startLocationDismissMonitoring() {
        if (locationDismissListener != null) return
        val alarm = viewModel.uiState.value.alarm ?: return
        if (!alarm.locationDismissEnabled || viewModel.uiState.value.locationDismissReady) return
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            if (!locationDismissPermissionRequestInFlight) {
                locationDismissPermissionRequestInFlight = true
                locationDismissPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            return
        }

        val locationManager = getSystemService(LocationManager::class.java)
        if (locationManager == null) {
            viewModel.onLocationDismissUnavailable("This device does not expose a location service.")
            return
        }

        val listener = LocationListener { location: Location ->
            viewModel.onLocationDismissLocation(location.latitude, location.longitude)
        }
        locationDismissListener = listener

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var requestedProvider = false
        try {
            providers.forEach { provider ->
                if (!locationManager.isProviderEnabled(provider)) return@forEach
                requestedProvider = true
                @Suppress("MissingPermission")
                locationManager.requestLocationUpdates(provider, 5_000L, 10f, listener)
                @Suppress("MissingPermission")
                locationManager.getLastKnownLocation(provider)?.let { lastKnown ->
                    viewModel.onLocationDismissLocation(lastKnown.latitude, lastKnown.longitude)
                }
            }
        } catch (_: SecurityException) {
            stopLocationDismissMonitoring()
            viewModel.onLocationDismissUnavailable("Android blocked location access during alarm firing.")
            return
        } catch (_: IllegalArgumentException) {
            stopLocationDismissMonitoring()
            viewModel.onLocationDismissUnavailable("No usable location provider is available.")
            return
        }

        if (!requestedProvider) {
            stopLocationDismissMonitoring()
            viewModel.onLocationDismissUnavailable("Turn on device Location to unlock dismissal after leaving the saved place.")
        }
    }

    private fun stopLocationDismissMonitoring() {
        val listener = locationDismissListener ?: return
        runCatching {
            getSystemService(LocationManager::class.java)?.removeUpdates(listener)
        }
        locationDismissListener = null
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

    private fun startCoverDetector() {
        if (coverDetector != null) return
        coverDetector = ProximityCoverDetector(
            context = this,
            onCovered = { snooze() }
        ).also { it.start() }
    }

    private fun stopCoverDetector() {
        coverDetector?.stop()
        coverDetector = null
    }

    // v1.4.0: Per-alarm hardware-button action. Volume keys intercepted so the
    // user can snooze or dismiss without looking at the screen. NONE falls
    // through to the system's volume control.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val action = viewModel.uiState.value.alarm?.hardwareButtonAction ?: "NONE"
        if (action == "NONE" || (event?.repeatCount ?: 0) > 0) {
            return super.onKeyDown(keyCode, event)
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_CAMERA -> {
                when (action) {
                    "SNOOZE" -> snooze()
                    "DISMISS" -> {
                        val state = viewModel.uiState.value
                        if (state.canDismiss && state.alarm?.holdToDismissEnabled != true) {
                            dismiss()
                        } else {
                            snooze()
                        }
                    }
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun snooze(customMinutes: Int? = null) {
        val intent = AlarmFireDismissContract.snoozeServiceIntent(
            context = this,
            alarmId = alarmId,
            scheduledAt = scheduledAt,
            fireId = fireId,
            customMinutes = customMinutes
        )
        try {
            startForegroundService(intent)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_REQUESTED,
                reasonCode = if (customMinutes != null) "UI_CUSTOM_SNOOZE_REQUESTED" else "UI_SNOOZE_REQUESTED"
            )
        } catch (e: Exception) {
            android.util.Log.e("AlarmFiringActivity", "startForegroundService(snooze) failed", e)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "UI_SNOOZE_START_FAILED_${e.javaClass.simpleName}"
            )
        }
        finish()
    }

    private fun dismiss() {
        val state = viewModel.uiState.value
        val challengeSolveTimeMs = state.challengeStartedAtMillis
            .takeIf { it > 0L && state.requiresChallenge }
            ?.let { (System.currentTimeMillis() - it).coerceAtLeast(0L) }
            ?: 0L
        val intent = AlarmFireDismissContract.dismissServiceIntent(
            context = this,
            alarmId = alarmId,
            scheduledAt = scheduledAt,
            fireId = fireId,
            challengeRetryCount = state.totalWrongAttempts,
            challengeSolveTimeMs = challengeSolveTimeMs
        )
        try {
            startForegroundService(intent)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_REQUESTED,
                reasonCode = "UI_DISMISS_REQUESTED"
            )
        } catch (e: Exception) {
            android.util.Log.e("AlarmFiringActivity", "startForegroundService(dismiss) failed", e)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "UI_DISMISS_START_FAILED_${e.javaClass.simpleName}"
            )
        }
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
        stopPushUpDetection()
        stopWalkSteps()
        stopWifiPolling()
        stopLocationDismissMonitoring()
        stopFlipDetector()
        stopCoverDetector()
        super.onDestroy()
    }

    private fun recordIncidentAsync(
        type: String,
        status: String,
        reasonCode: String
    ) {
        if (alarmId <= 0L) return
        // Repository-owned scope: most records here are immediately followed
        // by finish(), which would cancel lifecycleScope mid-write.
        alarmIncidentRepository.recordAsync(
            alarmId = alarmId,
            fireId = fireId.ifBlank { AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt) },
            scheduledAt = scheduledAt,
            type = type,
            status = status,
            reasonCode = reasonCode,
            source = "AlarmFiringActivity"
        )
    }
}
