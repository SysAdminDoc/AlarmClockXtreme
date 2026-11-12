package com.sysadmindoc.alarmclock.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import com.sysadmindoc.alarmclock.util.AlarmPublicText
import com.sysadmindoc.alarmclock.util.AlarmTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the persistent notification showing the next scheduled alarm.
 * Uses IMPORTANCE_LOW so it's silent and sits in the shade without sound/vibration.
 */
@Singleton
class NextAlarmNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlarmRepository,
    private val calculator: NextAlarmCalculator,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        const val CHANNEL_PERSISTENT = "persistent_alarm_channel"
        const val NOTIFICATION_ID_PERSISTENT = 2004
        private const val ANDROID_16_API = 36
        private const val SHOW_WAKEUP_REQUEST_CODE = 2004
        private const val HIDDEN_RECHECK_MS = 60_000L
    }

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var observeJob: kotlinx.coroutines.Job? = null
    @Volatile private var scheduledShowAt = 0L

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_PERSISTENT,
            context.getString(R.string.notif_next_alarm_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notif_next_alarm_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Start observing the next alarm and update notification reactively.
     * Called once from Application.onCreate().
     */
    fun startObserving() {
        observeJob?.cancel()
        observeJob = scope.launch {
            repository.observeNextAlarm()
                .combine(
                    preferencesManager.settings
                        .map { it.toNotificationSettings() }
                        .distinctUntilChanged()
                ) { alarm, settings ->
                    alarm to settings
                }
                .collectLatest { (alarm, settings) ->
                    if (alarm != null && alarm.nextTriggerTime > System.currentTimeMillis()) {
                        refreshNotificationUntilInvalid(alarm, settings)
                    } else {
                        cancelShowWakeup()
                        dismiss()
                    }
                }
        }
    }

    /**
     * Called by [NextAlarmNotificationReceiver] when a lead-time window opens.
     * Coroutine delays stop counting while the phone is in deep sleep, so the
     * observer can be asleep past the moment the notification was due. Show
     * it now if it is due, then restart the observer from the wall clock.
     */
    suspend fun refreshNow() {
        val settings = preferencesManager.getCurrentSettings().toNotificationSettings()
        val alarm = repository.getNextAlarm()
        val now = System.currentTimeMillis()
        if (alarm != null && alarm.isEnabled && alarm.nextTriggerTime > now &&
            NextAlarmNotificationTiming.millisUntilShown(
                settings.leadMinutes, now, alarm.nextTriggerTime
            ) == 0L
        ) {
            showNotification(alarm, settings)
        }
        startObserving()
    }

    fun stopObserving() {
        val job = observeJob
        observeJob = null
        if (job != null) {
            runBlocking { job.cancelAndJoin() }
        }
    }

    private suspend fun refreshNotificationUntilInvalid(
        initialAlarm: Alarm,
        settings: NextAlarmNotificationSettings
    ) {
        var alarm = initialAlarm
        while (true) {
            val now = System.currentTimeMillis()
            if (!alarm.isEnabled || alarm.nextTriggerTime <= now) {
                val nextAlarm = repository.getNextAlarm()
                if (nextAlarm != null && nextAlarm.nextTriggerTime > now) {
                    alarm = nextAlarm
                    continue
                } else {
                    cancelShowWakeup()
                    dismiss()
                    return
                }
            }

            val untilShown = NextAlarmNotificationTiming.millisUntilShown(
                settings.leadMinutes, now, alarm.nextTriggerTime
            )
            if (untilShown > 0L) {
                dismiss()
                val showAt = now + untilShown
                if (showAt != scheduledShowAt) scheduleShowWakeup(showAt)
                delay(untilShown.coerceAtMost(HIDDEN_RECHECK_MS))
            } else {
                showNotification(alarm, settings)
                delay(NextAlarmNotificationTiming.millisUntilNextRefresh(now, alarm.nextTriggerTime))
            }
            alarm = repository.getById(alarm.id) ?: repository.getNextAlarm() ?: run {
                cancelShowWakeup()
                dismiss()
                return
            }
        }
    }

    /**
     * Wakes the app when a hidden notification is due, because the observer's
     * delay alone can run late in Doze. Exact when the app may schedule exact
     * alarms, otherwise the closest inexact equivalent.
     */
    private fun scheduleShowWakeup(showAtMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = showWakeupIntent()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, showAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, showAtMillis, pendingIntent)
        }
        scheduledShowAt = showAtMillis
    }

    private fun cancelShowWakeup() {
        if (scheduledShowAt == 0L) return
        context.getSystemService(AlarmManager::class.java).cancel(showWakeupIntent())
        scheduledShowAt = 0L
    }

    private fun showWakeupIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        SHOW_WAKEUP_REQUEST_CODE,
        Intent(context, NextAlarmNotificationReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun showNotification(alarm: Alarm, settings: NextAlarmNotificationSettings) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // v1.2.0: Skip next occurrence action — routes through SkipNextReceiver so it
        // does NOT trigger the post-fire flow (TTS, morning briefing, wake confirmation).
        val skipIntent = Intent(context, com.sysadmindoc.alarmclock.receiver.SkipNextReceiver::class.java).apply {
            putExtra(com.sysadmindoc.alarmclock.domain.AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
        }
        val skipPi = PendingIntent.getBroadcast(
            context, alarm.id.toInt() + 30000, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showsSkip = NextAlarmNotificationTiming.showsSkipAction(
            earlyDismissMinutes = alarm.earlyDismissMinutes,
            nowMillis = System.currentTimeMillis(),
            triggerTimeMillis = alarm.nextTriggerTime
        )

        // Format time
        val triggerInstant = Instant.ofEpochMilli(alarm.nextTriggerTime)
        val localDateTime = triggerInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val timePattern = "EEE " + AlarmTimeFormatter.pattern(settings.is24HourFormat)
        val timeStr = localDateTime.format(DateTimeFormatter.ofPattern(timePattern))
        val remaining = calculator.formatRemaining(context, alarm.nextTriggerTime)

        val title = AlarmPublicText.requiredAlarmLabel(
            label = alarm.label,
            hideLabel = settings.hideAlarmLabelsOnPublicSurfaces
        )

        val now = System.currentTimeMillis()
        val notification = if (
            Build.VERSION.SDK_INT >= ANDROID_16_API &&
            NextAlarmNotificationTiming.shouldUseLiveUpdate(now, alarm.nextTriggerTime)
        ) {
            buildLiveUpdateNotification(
                alarm = alarm,
                title = title,
                timeStr = timeStr,
                remaining = remaining,
                pendingIntent = pendingIntent,
                skipPi = skipPi.takeIf { showsSkip },
                now = now
            )
        } else {
            buildCompatNotification(
                title = title,
                timeStr = timeStr,
                remaining = remaining,
                pendingIntent = pendingIntent,
                skipPi = skipPi.takeIf { showsSkip }
            )
        }

        notificationManager.notify(NOTIFICATION_ID_PERSISTENT, notification)
    }

    private fun buildCompatNotification(
        title: String,
        timeStr: String,
        remaining: String,
        pendingIntent: PendingIntent,
        skipPi: PendingIntent?
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.notif_next_alarm_title, timeStr))
            .setContentText(context.getString(R.string.notif_next_alarm_body, title, remaining))
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .apply {
                if (skipPi != null) {
                    addAction(
                        R.drawable.ic_alarm,
                        context.getString(R.string.notif_skip_action),
                        skipPi
                    )
                }
            }
            .build()
    }

    @RequiresApi(ANDROID_16_API)
    private fun buildLiveUpdateNotification(
        alarm: Alarm,
        title: String,
        timeStr: String,
        remaining: String,
        pendingIntent: PendingIntent,
        skipPi: PendingIntent?,
        now: Long
    ): Notification {
        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(true)
            .setProgress(NextAlarmNotificationTiming.liveUpdateProgress(now, alarm.nextTriggerTime))
            .setProgressSegments(
                listOf(
                    Notification.ProgressStyle.Segment(
                        NextAlarmNotificationTiming.LIVE_UPDATE_PROGRESS_MAX
                    )
                )
            )
            .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_alarm))

        val notification = Notification.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.notif_next_alarm_title, timeStr))
            .setContentText(context.getString(R.string.notif_next_alarm_body, title, remaining))
            .setSubText(context.getString(R.string.notif_next_alarm_subtext))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(alarm.nextTriggerTime)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setStyle(progressStyle)
            .apply {
                if (skipPi != null) {
                    addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource(context, R.drawable.ic_alarm),
                            context.getString(R.string.notif_skip_action),
                            skipPi
                        ).build()
                    )
                }
            }

        PromotedOngoingNotification.request(notification)
        return notification.build()
    }

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID_PERSISTENT)
    }
}

private data class NextAlarmNotificationSettings(
    val is24HourFormat: Boolean,
    val hideAlarmLabelsOnPublicSurfaces: Boolean,
    val leadMinutes: Int
)

private fun AppSettings.toNotificationSettings() = NextAlarmNotificationSettings(
    is24HourFormat = is24HourFormat,
    hideAlarmLabelsOnPublicSurfaces = hideAlarmLabelsOnPublicSurfaces,
    leadMinutes = nextAlarmNotificationLeadMinutes
)
