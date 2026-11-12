package com.sysadmindoc.alarmclock.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.receiver.AlarmTestRunReceiver
import java.util.Date

/**
 * "Test now": rings a saved alarm right away with its real configuration
 * (sound, volume ramp, vibration, challenges, snooze, wake confirmation)
 * without touching anything the real alarm owns.
 *
 * The flag travels as [EXTRA_TEST_RUN] on every intent of the run (start,
 * snooze, dismiss, the firing screen, the wake-confirmation work) and sits in
 * [AlarmService.activeAlarm] while it rings. The service routes every lasting
 * side effect through [AlarmOutcomeEffects], which drops them for a test.
 * A test snooze has its own PendingIntent and counter, so a real snooze on
 * the same alarm is never disturbed, and only one test exists at a time.
 */
object AlarmTestRun {
    const val EXTRA_TEST_RUN = "com.sysadmindoc.alarmclock.extra.ALARM_TEST_RUN"

    const val ACTION_RING = "com.sysadmindoc.alarmclock.TEST_ALARM_RING"
    const val ACTION_SNOOZE = "com.sysadmindoc.alarmclock.TEST_ALARM_SNOOZE"
    const val ACTION_DISMISS = "com.sysadmindoc.alarmclock.TEST_ALARM_DISMISS"
    const val ACTION_END = "com.sysadmindoc.alarmclock.TEST_ALARM_END"

    const val SNOOZED_NOTIFICATION_ID = 1908
    const val WAKE_CONFIRM_NOTIFICATION_ID = 1909
    const val WAKE_CONFIRM_TAG = "wake_confirm_test"

    private const val RING_REQUEST_CODE = 1908
    private const val SNOOZE_REQUEST_CODE = 1910
    private const val DISMISS_REQUEST_CODE = 1911
    private const val END_REQUEST_CODE = 1912
    private const val PREFS_NAME = "alarm_test_run"
    private const val TAG = "AlarmTestRun"

    enum class StartResult { STARTED, ALARM_RINGING, FAILED }

    private val _ended = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires when a test is ended from anywhere, so an open test screen closes too. */
    val ended: SharedFlow<Unit> = _ended.asSharedFlow()

    fun isTestRun(intent: Intent?): Boolean =
        intent?.getBooleanExtra(EXTRA_TEST_RUN, false) == true

    fun wakeConfirmWorkName(alarmId: Long): String = "wake_confirm_test_$alarmId"

    /** The wake-confirm screen's answer key; a test never answers the real prompt. */
    fun wakeConfirmKey(alarmId: Long, testRun: Boolean): String =
        if (testRun) "confirmed_test_$alarmId" else "confirmed_$alarmId"

    /**
     * Rings [alarmId] now as a test. Refused while a real alarm is ringing: a
     * test must never talk over, or end, an alarm someone has to wake up to.
     */
    fun start(context: Context, alarmId: Long, now: Long = System.currentTimeMillis()): StartResult {
        val active = AlarmService.activeAlarm.get()
        if (active != null && !active.isTestRun) return StartResult.ALARM_RINGING
        val appContext = context.applicationContext
        cancelPending(appContext)
        clearSnoozeCount(appContext)
        val intent = AlarmFireDismissContract.startServiceIntent(
            context = appContext,
            alarmId = alarmId,
            scheduledAt = now,
            testRun = true
        )
        return try {
            appContext.startForegroundService(intent)
            StartResult.STARTED
        } catch (e: Exception) {
            Log.w(TAG, "Test alarm could not start", e)
            StartResult.FAILED
        }
    }

    /**
     * Ends the test wherever it is: ringing, snoozed, or waiting on a wake
     * confirmation. A real alarm is never touched.
     */
    fun end(context: Context) {
        val appContext = context.applicationContext
        cancelPending(appContext)
        clearSnoozeCount(appContext)
        _ended.tryEmit(Unit)
        if (AlarmService.activeAlarm.get()?.isTestRun == true) {
            try {
                appContext.startService(
                    Intent(appContext, AlarmService::class.java)
                        .setAction(AlarmService.ACTION_END_TEST)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not reach the ringing test", e)
            }
        }
    }

    /** Called when a test snooze runs out: ring the same test again. */
    internal fun ringAgain(context: Context, alarmId: Long, scheduledAt: Long, fireId: String) {
        val appContext = context.applicationContext
        appContext.getSystemService(NotificationManager::class.java)?.cancel(SNOOZED_NOTIFICATION_ID)
        val active = AlarmService.activeAlarm.get()
        if (active != null && !active.isTestRun) {
            // A real alarm owns the screen now. The test just ends.
            clearSnoozeCount(appContext)
            return
        }
        try {
            appContext.startForegroundService(
                AlarmFireDismissContract.startServiceIntent(
                    context = appContext,
                    alarmId = alarmId,
                    scheduledAt = scheduledAt,
                    fireId = fireId,
                    testRun = true
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Test snooze could not ring again", e)
            clearSnoozeCount(appContext)
        }
    }

    fun snoozeCount(context: Context, alarmId: Long): Int =
        prefs(context).getInt(snoozeKey(alarmId), 0)

    fun setSnoozeCount(context: Context, alarmId: Long, count: Int) {
        prefs(context).edit().putInt(snoozeKey(alarmId), count.coerceAtLeast(0)).commit()
    }

    fun clearSnoozeCount(context: Context) {
        prefs(context).edit().clear().commit()
    }

    /**
     * Arms the test's snooze re-ring on its own PendingIntent, never the
     * scheduler's, and posts a notification that can end the test early.
     */
    fun scheduleSnooze(
        context: Context,
        alarmId: Long,
        scheduledAt: Long,
        fireId: String,
        atMillis: Long
    ): Boolean {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return false
        val pendingIntent = ringPendingIntent(appContext, alarmId, scheduledAt, fireId)
        val scheduled = runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            }
        }.recoverCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }.isSuccess
        if (scheduled) postSnoozedNotification(appContext, atMillis)
        return scheduled
    }

    /** Drops everything a test left waiting: the snooze re-ring, the wake check and their notifications. */
    fun cancelPending(context: Context) {
        val appContext = context.applicationContext
        val pendingIntent = ringPendingIntent(appContext, 0L, 0L, "")
        appContext.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent)
        pendingIntent.cancel()
        runCatching { WorkManager.getInstance(appContext).cancelAllWorkByTag(WAKE_CONFIRM_TAG) }
        appContext.getSystemService(NotificationManager::class.java)?.let { nm ->
            nm.cancel(SNOOZED_NOTIFICATION_ID)
            nm.cancel(WAKE_CONFIRM_NOTIFICATION_ID)
        }
    }

    /** Notification actions for a ringing test go here instead of the real receivers. */
    fun actionPendingIntent(
        context: Context,
        action: String,
        alarmId: Long,
        scheduledAt: Long,
        fireId: String
    ): PendingIntent {
        val requestCode = when (action) {
            ACTION_SNOOZE -> SNOOZE_REQUEST_CODE
            ACTION_DISMISS -> DISMISS_REQUEST_CODE
            else -> END_REQUEST_CODE
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            receiverIntent(context, action, alarmId, scheduledAt, fireId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun ringPendingIntent(
        context: Context,
        alarmId: Long,
        scheduledAt: Long,
        fireId: String
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        RING_REQUEST_CODE,
        receiverIntent(context, ACTION_RING, alarmId, scheduledAt, fireId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun receiverIntent(
        context: Context,
        action: String,
        alarmId: Long,
        scheduledAt: Long,
        fireId: String
    ): Intent = Intent(context, AlarmTestRunReceiver::class.java).apply {
        this.action = action
        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
        putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, fireId)
        putExtra(EXTRA_TEST_RUN, true)
    }

    private fun postSnoozedNotification(context: Context, atMillis: Long) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        AlarmService.createNotificationChannels(context)
        val time = DateFormat.getTimeFormat(context).format(Date(atMillis))
        val notification = NotificationCompat.Builder(context, AlarmService.CHANNEL_UPCOMING)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.alarm_test_snoozed_title))
            .setContentText(context.getString(R.string.alarm_test_snoozed_text, time))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_alarm,
                context.getString(R.string.alarm_test_end),
                actionPendingIntent(context, ACTION_END, 0L, 0L, "")
            )
            .build()
        runCatching { nm.notify(SNOOZED_NOTIFICATION_ID, notification) }
    }

    private fun snoozeKey(alarmId: Long) = "snooze_count_$alarmId"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
