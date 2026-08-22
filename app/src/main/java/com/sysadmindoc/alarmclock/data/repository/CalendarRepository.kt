package com.sysadmindoc.alarmclock.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.util.AlarmTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean,
    val location: String,
    val calendarColor: Int
) {
    /**
     * These take the preference rather than reading it, because a calendar row
     * is a plain data class with no settings and no Context. They used to
     * hardcode "h:mm a", so a 24-hour phone still saw "6:30 AM" on the
     * dashboard's calendar strip. The all-day case is the caller's to name.
     */
    fun startFormatted(is24Hour: Boolean): String {
        if (allDay) return ""
        return AlarmTimeFormatter.format(startTime, is24Hour)
    }

    fun endFormatted(is24Hour: Boolean): String {
        if (allDay) return ""
        return AlarmTimeFormatter.format(endTime, is24Hour)
    }
}

@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Get today's calendar events from all synced calendars.
     * Uses CalendarContract - works with Google Calendar, CalDAV, local calendars.
     * Requires READ_CALENDAR permission.
     */
    fun getTodayEvents(): Result<List<CalendarEvent>> {
        return try {
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val startMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val events = queryEvents(startMillis, endMillis)
            Result.success(events)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get events for a specific date range.
     */
    fun getEvents(startMillis: Long, endMillis: Long): Result<List<CalendarEvent>> {
        return try {
            Result.success(queryEvents(startMillis, endMillis))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.CALENDAR_COLOR
        )

        val selection = "${CalendarContract.Instances.BEGIN} >= ? AND ${CalendarContract.Instances.BEGIN} < ?"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startMillis.toString())
            .appendPath(endMillis.toString())
            .build()

        val events = mutableListOf<CalendarEvent>()
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.let {
                while (it.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            id = it.getLong(0),
                            title = it.getString(1) ?: context.getString(R.string.calendar_untitled_event),
                            startTime = it.getLong(2),
                            endTime = it.getLong(3),
                            allDay = it.getInt(4) == 1,
                            location = it.getString(5) ?: "",
                            calendarColor = it.getInt(6)
                        )
                    )
                }
            }
        } finally {
            cursor?.close()
        }

        return events
    }
}
