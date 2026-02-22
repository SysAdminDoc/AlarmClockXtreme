package com.sysadmindoc.alarmclock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sysadmindoc.alarmclock.MainActivity
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    private val calculator: NextAlarmCalculator
) {
    companion object {
        const val CHANNEL_PERSISTENT = "persistent_alarm_channel"
        const val NOTIFICATION_ID_PERSISTENT = 2001
    }

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var observeJob: kotlinx.coroutines.Job? = null

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_PERSISTENT,
            "Next Alarm",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when the next alarm will fire"
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
            repository.observeNextAlarm().collectLatest { alarm ->
                if (alarm != null && alarm.nextTriggerTime > System.currentTimeMillis()) {
                    showNotification(alarm)
                } else {
                    dismiss()
                }
            }
        }
    }

    private fun showNotification(alarm: Alarm) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format time
        val triggerInstant = Instant.ofEpochMilli(alarm.nextTriggerTime)
        val localDateTime = triggerInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val timeStr = localDateTime.format(DateTimeFormatter.ofPattern("EEE h:mm a"))
        val remaining = calculator.formatRemaining(alarm.nextTriggerTime)

        val title = if (alarm.label.isNotBlank()) alarm.label else "Alarm"

        val notification = NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Next: $timeStr")
            .setContentText("$title - $remaining remaining")
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_PERSISTENT, notification)
    }

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID_PERSISTENT)
    }
}
