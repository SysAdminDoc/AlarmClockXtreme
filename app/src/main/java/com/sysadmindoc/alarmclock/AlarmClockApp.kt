package com.sysadmindoc.alarmclock

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserManager
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.service.NextAlarmNotifier
import com.sysadmindoc.alarmclock.service.YouTubeDownloadInitializer
import com.sysadmindoc.alarmclock.wear.WearNextAlarmBridge
import com.sysadmindoc.alarmclock.worker.CalendarAutoAlarmWorker
import com.sysadmindoc.alarmclock.worker.HolidaySyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AlarmClockApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    /**
     * Flavor-bound: real on play (unpacks yt-dlp binaries), no-op on f-droid.
     * Run off the main thread so process startup isn't blocked.
     */
    @Inject lateinit var youTubeDownloadInitializer: YouTubeDownloadInitializer
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var wearNextAlarmBridge: WearNextAlarmBridge

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var unlockedStartupComplete = false
    private var unlockReceiver: BroadcastReceiver? = null

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppEntryPoint {
        fun nextAlarmNotifier(): NextAlarmNotifier
        fun alarmRepository(): com.sysadmindoc.alarmclock.data.repository.AlarmRepository
        fun alarmScheduler(): com.sysadmindoc.alarmclock.domain.AlarmScheduler
        fun nextAlarmCalculator(): com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
        fun alarmEventRepository(): com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
        fun alarmIncidentRepository(): com.sysadmindoc.alarmclock.data.repository.AlarmIncidentRepository
        fun webhookService(): com.sysadmindoc.alarmclock.service.WebhookService
    }

    override fun onCreate() {
        super.onCreate()

        if (!isUserUnlocked()) {
            registerPostUnlockInitializer()
            return
        }

        initializeUnlockedApp()
    }

    private fun initializeUnlockedApp() {
        if (unlockedStartupComplete) return
        unlockedStartupComplete = true
        unlockReceiver?.let { receiver ->
            runCatching { unregisterReceiver(receiver) }
            unlockReceiver = null
        }

        // Install crash logger for debugging
        com.sysadmindoc.alarmclock.util.CrashLogger.install(this)
        AlarmService.createNotificationChannels(this)

        // F13: Schedule weekly holiday sync
        val holidaySync = PeriodicWorkRequestBuilder<HolidaySyncWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "holiday_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            holidaySync
        )

        // v1.10.6: Keep the first-meeting auto-alarm responsive without
        // running periodic calendar reads when the feature is disabled.
        appScope.launch {
            val settings = preferencesManager.getCurrentSettings()
            if (settings.calendarAutoAlarmEnabled) {
                CalendarAutoAlarmWorker.schedulePeriodic(this@AlarmClockApp)
            } else {
                CalendarAutoAlarmWorker.cancelPeriodic(this@AlarmClockApp)
            }
            CalendarAutoAlarmWorker.enqueueRefresh(this@AlarmClockApp, "app_start")
        }

        // Start persistent next-alarm notification observer
        val entryPoint = EntryPointAccessors.fromApplication(this, AppEntryPoint::class.java)
        entryPoint.nextAlarmNotifier().startObserving()
        wearNextAlarmBridge.start()

        // v1.7.0: Unpack yt-dlp binaries off the main thread so the YouTube
        // download path is ready by the time the user opens the ringtone
        // picker. No-op on the f-droid flavor.
        appScope.launch {
            try {
                youTubeDownloadInitializer.initialize()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Init failure must NOT crash the app — the downloader checks
                // isAvailable() before letting the UI start a download.
            }
        }

        // Seed default alarm on first launch
        val prefs = getSharedPreferences("app_prefs", 0)
        if (!prefs.getBoolean("default_alarm_seeded", false)) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    seedDefaultAlarm(entryPoint)
                    prefs.edit().putBoolean("default_alarm_seeded", true).apply()
                } catch (_: Exception) { /* Will retry next launch */ }
            }
        }
    }

    private fun isUserUnlocked(): Boolean {
        val userManager = getSystemService(UserManager::class.java)
        return userManager?.isUserUnlocked != false
    }

    private fun registerPostUnlockInitializer() {
        if (unlockReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_USER_UNLOCKED) {
                    initializeUnlockedApp()
                }
            }
        }
        unlockReceiver = receiver
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_USER_UNLOCKED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private suspend fun seedDefaultAlarm(ep: AppEntryPoint) {
        val repo = ep.alarmRepository()
        val scheduler = ep.alarmScheduler()
        val calculator = ep.nextAlarmCalculator()

        val weekdays = setOf(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        )

        val alarm = com.sysadmindoc.alarmclock.data.model.Alarm(
            hour = 6,
            minute = 0,
            label = "Wake Up",
            isEnabled = true,
            repeatDays = weekdays,
            vibrationEnabled = true,
            snoozeDurationMinutes = 10,
            challengeType = "NONE"
        )

        val id = repo.save(alarm)
        val triggerTime = calculator.calculate(alarm)
        repo.updateNextTrigger(id, triggerTime)
        scheduler.schedule(alarm.copy(id = id, nextTriggerTime = triggerTime))
    }
}
