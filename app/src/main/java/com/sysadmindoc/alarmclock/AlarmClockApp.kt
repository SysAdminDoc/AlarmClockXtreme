package com.sysadmindoc.alarmclock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.service.NextAlarmNotifier
import com.sysadmindoc.alarmclock.worker.HolidaySyncWorker
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
    }

    override fun onCreate() {
        super.onCreate()

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

        // Start persistent next-alarm notification observer
        val entryPoint = EntryPointAccessors.fromApplication(this, AppEntryPoint::class.java)
        entryPoint.nextAlarmNotifier().startObserving()

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
