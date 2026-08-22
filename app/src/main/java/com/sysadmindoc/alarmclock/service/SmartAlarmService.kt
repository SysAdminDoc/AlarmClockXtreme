package com.sysadmindoc.alarmclock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.actigraphy.ActigraphyEpoch
import com.sysadmindoc.alarmclock.data.actigraphy.ActigraphySleepClassifier
import com.sysadmindoc.alarmclock.data.actigraphy.SmartWakeDecision
import com.sysadmindoc.alarmclock.data.actigraphy.SmartWakeDecisionEngine
import com.sysadmindoc.alarmclock.data.actigraphy.SmartWakeDecisionResult
import com.sysadmindoc.alarmclock.data.repository.ActigraphyRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt
import javax.inject.Inject

/**
 * F7: Smart alarm window — monitors accelerometer overnight.
 * Started by AlarmScheduler [smartAlarmWindowMinutes] before the scheduled alarm time.
 * If scored phone-motion buckets indicate a conservative light-motion window, fires the alarm early.
 * If the scheduled time arrives without detection, the regular AlarmReceiver fires normally.
 */
@AndroidEntryPoint
class SmartAlarmService : Service(), SensorEventListener {

    @Inject lateinit var actigraphyRepository: ActigraphyRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

    companion object {
        const val ACTION_START_SMART = "com.sysadmindoc.alarmclock.SMART_ALARM_START"
        const val EXTRA_ALARM_ID = "smart_alarm_id"
        const val EXTRA_TARGET_TIME = "smart_target_time"
        const val CHANNEL_SMART = "smart_alarm_channel"
        const val NOTIF_ID_SMART = 2003
        private const val MAX_SMART_WAKELOCK_MS = 65 * 60 * 1000L
        private const val WAKELOCK_TEARDOWN_BUFFER_MS = 5 * 60 * 1000L
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var alarmId: Long = -1L
    private var targetTimeMs: Long = 0L
    private var windowMotionMax = 0f
    private var windowStartMs = 0L
    private var sessionStartMs = 0L
    private val sessionClosed = AtomicBoolean(false)
    private var latestDecision: SmartWakeDecisionResult? = null
    private val WINDOW_MS = 30_000L  // 30-second motion sampling windows
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val halfMinuteMotion = mutableListOf<Float>()
    private val actigraphyEpochs = mutableListOf<ActigraphyEpoch>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // v1.5.4: Safe casts — stripped-down AOSP / managed-profile devices
        // have been seen to return null for SENSOR_SERVICE / POWER_SERVICE.
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmClockXtreme::SmartAlarmWakeLock"
        )?.apply {
            // Reference counting means two overlapping smart windows acquire
            // twice while onDestroy releases once, leaving the CPU held.
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START_SMART) return START_NOT_STICKY

        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        targetTimeMs = intent.getLongExtra(EXTRA_TARGET_TIME, 0L)

        if (alarmId == -1L || targetTimeMs == 0L) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_SMART)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(getString(R.string.notif_smart_alarm_title))
            .setContentText(getString(R.string.notif_smart_alarm_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIF_ID_SMART, notification)

        val now = System.currentTimeMillis()
        // v1.13.11 (roadmap A4): the UI/product cap is 60 minutes, so the
        // wake lock now follows the remaining smart window plus a small
        // teardown buffer instead of a stale 90-minute hard cap.
        val wakeLockTimeoutMs = (targetTimeMs - now + WAKELOCK_TEARDOWN_BUFFER_MS)
            .coerceIn(WAKELOCK_TEARDOWN_BUFFER_MS, MAX_SMART_WAKELOCK_MS)
        // v1.11.4 (roadmap N4) Play wake-lock policy audit:
        //   SmartAlarmService is the one PARTIAL_WAKE_LOCK acquisition in the
        //   app that is NOT exempt under the March-2026 Play policy — it
        //   wraps a `dataSync` foreground service (not media playback) and
        //   the accelerometer monitoring isn't an exempted activity. The
        //   65-minute command-local cap keeps one default smart-wake run
        //   under the 2 h / 24 h non-exempt budget with margin.
        wakeLock?.acquire(wakeLockTimeoutMs)
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        windowStartMs = now
        sessionStartMs = now
        sessionClosed.set(false)
        latestDecision = null
        halfMinuteMotion.clear()
        actigraphyEpochs.clear()

        return START_NOT_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()

        // If we've passed the scheduled alarm time, stop — regular alarm will fire
        if (now >= targetTimeMs) {
            val scored = ActigraphySleepClassifier.classify(actigraphyEpochs)
            finishMonitoring(
                firedEarly = false,
                launchAlarm = false,
                decisionResult = SmartWakeDecisionEngine.reachedTarget(scored)
            )
            return
        }

        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val mag = sqrt(x * x + y * y + z * z)
        val delta = abs(mag - SensorManager.GRAVITY_EARTH)

        if (delta > windowMotionMax) windowMotionMax = delta

        // Check window boundary
        if (now - windowStartMs >= WINDOW_MS) {
            val appendedMinute = appendActigraphyWindow(windowStartMs, windowMotionMax)
            if (appendedMinute) {
                val decision = SmartWakeDecisionEngine.decide(
                    scoredEpochs = ActigraphySleepClassifier.classify(actigraphyEpochs),
                    sessionStartMs = sessionStartMs,
                    nowMs = now,
                    targetTimeMs = targetTimeMs
                )
                latestDecision = decision
                if (decision.decision == SmartWakeDecision.FIRE_EARLY) {
                    fireAlarmEarly(decision)
                    return
                }
            }
            windowMotionMax = 0f
            windowStartMs = now
        }
    }

    private fun fireAlarmEarly(decisionResult: SmartWakeDecisionResult) {
        finishMonitoring(firedEarly = true, launchAlarm = true, decisionResult = decisionResult)
    }

    private fun appendActigraphyWindow(startMillis: Long, motionMax: Float): Boolean {
        halfMinuteMotion.add(motionMax)
        if (halfMinuteMotion.size < 2) return false

        val minuteMotion = halfMinuteMotion.maxOrNull() ?: 0f
        val minuteStart = startMillis - WINDOW_MS
        actigraphyEpochs.add(
            ActigraphyEpoch(
                startMillis = minuteStart,
                activityCount = ActigraphySleepClassifier.phoneMotionToActivityCount(minuteMotion)
            )
        )
        halfMinuteMotion.clear()
        return true
    }

    private fun finishMonitoring(
        firedEarly: Boolean,
        launchAlarm: Boolean,
        decisionResult: SmartWakeDecisionResult? = null
    ) {
        if (!sessionClosed.compareAndSet(false, true)) return
        sensorManager?.unregisterListener(this)
        val endedAt = System.currentTimeMillis()
        val epochs = actigraphyEpochs.toList()
        if (epochs.isEmpty()) {
            if (launchAlarm) startAlarmService()
            stopSelf()
            return
        }
        val scored = ActigraphySleepClassifier.classify(epochs)
        val summary = ActigraphySleepClassifier.summarizeScored(scored)
        val finalDecision = decisionResult
            ?: latestDecision
            ?: SmartWakeDecisionEngine.reachedTarget(scored)
        serviceScope.launch {
            runCatching {
                actigraphyRepository.record(
                    alarmId = alarmId,
                    startedAt = sessionStartMs,
                    endedAt = endedAt,
                    targetTime = targetTimeMs,
                    firedEarly = firedEarly,
                    summary = summary,
                    decisionReason = finalDecision.reasonCode,
                    observedMinutesBeforeDecision = finalDecision.observedMinutes,
                    smartWakeMode = finalDecision.mode
                )
            }
            withContext(Dispatchers.Main) {
                if (launchAlarm) startAlarmService()
                stopSelf()
            }
        }
    }

    private fun startAlarmService() {
        // Fire early via the foreground service directly. The original exact alarm
        // scheduled at the unmodified trigger time is still armed (this path does
        // not consume its PendingIntent), so cancel it now to prevent a spurious
        // second fire at the originally-scheduled minute.
        runCatching { alarmScheduler.cancelMainScheduledAlarm(alarmId) }
            .onFailure { android.util.Log.e("SmartAlarmService", "Failed to cancel stale alarm $alarmId", it) }

        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            // Thread the original occurrence through so the post-dismiss
            // reschedule floors "next" past it. Without this a recurring
            // alarm dismissed after an early fire recomputes today's
            // still-future original minute and rings a second time.
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, targetTimeMs)
        }
        try {
            startForegroundService(intent)
        } catch (e: Exception) {
            android.util.Log.e("SmartAlarmService", "startForegroundService failed for alarm $alarmId", e)
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        val scored = ActigraphySleepClassifier.classify(actigraphyEpochs)
        finishMonitoring(
            firedEarly = false,
            launchAlarm = false,
            decisionResult = SmartWakeDecisionEngine.serviceTimeout(scored)
        )
        super.onTimeout(startId, fgsType)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        wakeLock?.let { if (it.isHeld) it.release() }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL_SMART, "Smart Alarm", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Smart sleep phase detection"
        }
        nm.createNotificationChannel(ch)
    }
}
