package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.ZoneId

/**
 * v1.2.0: Calendar auto-alarm worker.
 * Checks tomorrow's first calendar event and creates/updates an auto-alarm.
 */
@HiltWorker
class CalendarAutoAlarmWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesManager: PreferencesManager,
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = preferencesManager.getCurrentSettings()
        if (!settings.calendarAutoAlarmEnabled) return Result.success()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) return Result.success()

        val minutesBefore = settings.calendarAutoAlarmMinutesBefore
        val tomorrowStart = java.time.LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tomorrowEnd = tomorrowStart + 24 * 60 * 60 * 1000L

        try {
            val uri = CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(CalendarContract.Events.DTSTART, CalendarContract.Events.TITLE)
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?"
            val selectionArgs = arrayOf(tomorrowStart.toString(), tomorrowEnd.toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT 1"

            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dtStart = cursor.getLong(0)
                    val title = cursor.getString(1) ?: "Calendar Event"

                    val alarmTime = Instant.ofEpochMilli(dtStart - minutesBefore * 60 * 1000L)
                        .atZone(ZoneId.systemDefault())

                    val alarm = Alarm(
                        hour = alarmTime.hour,
                        minute = alarmTime.minute,
                        label = "Before: $title",
                        isEnabled = true,
                        group = "Calendar"
                    )
                    val id = repository.save(alarm)
                    scheduler.schedule(alarm.copy(id = id))
                }
            }
        } catch (_: Exception) {}

        return Result.success()
    }
}
