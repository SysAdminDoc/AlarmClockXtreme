package com.sysadmindoc.alarmclock.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
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
    val startFormatted: String get() {
        if (allDay) return "All day"
        val instant = Instant.ofEpochMilli(startTime)
        val local = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return local.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    val endFormatted: String get() {
        if (allDay) return ""
        val instant = Instant.ofEpochMilli(endTime)
        val local = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return local.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    val timeRange: String get() = if (allDay) "All day" else "$startFormatted - $endFormatted"
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
                            title = it.getString(1) ?: "Untitled",
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
