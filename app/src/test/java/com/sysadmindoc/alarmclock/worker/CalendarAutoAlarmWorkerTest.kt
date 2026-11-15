package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.sysadmindoc.alarmclock.data.local.CommuteHistoryStore
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.CommuteRouteRepository
import com.sysadmindoc.alarmclock.data.repository.WeatherRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmRuntimeState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * The worker owns exactly one alarm row, recognised by its profile name.
 *
 * The original implementation inserted a fresh row every run and left seven
 * behind after a week, and the periodic pass racing a one-shot refresh could
 * still leave two. Both paths converge on `findExistingAutoAlarm`, so this
 * pins the row count rather than the mechanism.
 */
@RunWith(RobolectricTestRunner::class)
class CalendarAutoAlarmWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var repository: AlarmRepository
    private lateinit var scheduler: AlarmScheduler
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var eventRepository: AlarmEventRepository

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 17)
    private val tomorrow = today.plusDays(1)

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        preferencesManager = mockk()
        // Relaxed: no history rows, so no day counts as done.
        eventRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        CalendarAutoAlarmWorker.clock = { System.currentTimeMillis() }
        FakeCalendar.events = emptyList()
        AlarmRuntimeState.clear(context, 1L)
    }

    /** The row has history rows (rung, missed or skipped) for rings set for [times]. */
    private fun historyAt(vararg times: Long) {
        coEvery { eventRepository.hasEventBetween(1L, any(), any()) } answers {
            times.any { it >= secondArg<Long>() && it < thirdArg<Long>() }
        }
    }

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    /** Today's 9:00 standup and the alarm the evening pass set an hour before it. */
    private fun calendarAt(now: Long, vararg events: Long): Alarm {
        CalendarAutoAlarmWorker.clock = { now }
        FakeCalendar.events = events.toList()
        Robolectric.setupContentProvider(FakeCalendar::class.java, CalendarContract.AUTHORITY)
        settings(enabled = true)
        shadowOf(context as Application).grantPermissions(Manifest.permission.READ_CALENDAR)
        val row = auto(id = 1L).copy(
            hour = 8,
            minute = 0,
            label = "Before: Standup",
            specificDate = today.toString(),
            nextTriggerTime = at(today, 8)
        ).sanitized()
        coEvery { repository.getAll() } returns listOf(row)
        every { scheduler.isArmed(1L) } returns true
        return row
    }

    @Test
    fun `a pass after midnight keeps the alarm for today's first event`() = runBlocking {
        // Nothing on tomorrow, which used to switch today's alarm off at 00:15.
        calendarAt(now = at(today, 0, 30), at(today, 9))

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 0) { repository.save(any()) }
        coVerify(exactly = 0) { repository.setEnabled(any(), any(), any()) }
        verify(exactly = 0) { scheduler.cancel(any(), any()) }
    }

    @Test
    fun `once today's alarm has rung the next pass sets up tomorrow and keeps the wake-up check`() = runBlocking {
        val rung = calendarAt(now = at(today, 10), at(today, 9), at(tomorrow, 9, 30))
        // Dismissing a one-shot switches it off and leaves a wake-up check running.
        coEvery { repository.getAll() } returns listOf(rung.copy(isEnabled = false, nextTriggerTime = 0L))
        every { scheduler.isArmed(1L) } returns false
        val saved = slot<Alarm>()
        coEvery { repository.save(capture(saved)) } returns 1L

        assertEquals(Result.success(), worker().doWork())

        assertEquals(tomorrow.toString(), saved.captured.specificDate)
        assertEquals(8 to 30, saved.captured.hour to saved.captured.minute)
        verify(exactly = 1) { scheduler.cancel(1L, keepFollowUps = true) }
        verify(exactly = 0) { scheduler.cancel(any(), keepFollowUps = false) }
    }

    @Test
    fun `a ring or a snooze in progress is left where it is`() = runBlocking {
        val armed = calendarAt(now = at(today, 8, 1), at(today, 9), at(tomorrow, 9, 30))
        // Ringing: the 8:00 arm came up and the row hasn't been dismissed.
        assertEquals(Result.success(), worker().doWork())

        // Snoozed until 8:14.
        CalendarAutoAlarmWorker.clock = { at(today, 8, 5) }
        coEvery { repository.getAll() } returns listOf(armed.copy(nextTriggerTime = at(today, 8, 14)))
        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { scheduler.cancel(any(), any()) }
    }

    @Test
    fun `today's event deleted before its alarm moves the alarm to tomorrow`() = runBlocking {
        calendarAt(now = at(today, 0, 30), at(tomorrow, 9, 30))
        val saved = slot<Alarm>()
        coEvery { repository.save(capture(saved)) } returns 1L

        assertEquals(Result.success(), worker().doWork())

        assertEquals(tomorrow.toString(), saved.captured.specificDate)
        verify(exactly = 1) { scheduler.cancel(1L, keepFollowUps = true) }
        verify(exactly = 0) { scheduler.cancel(any(), keepFollowUps = false) }
    }

    @Test
    fun `a day emptied before its alarm parks the row and keeps the wake-up check`() = runBlocking {
        // Today's 9:00 is deleted at 00:30 and tomorrow has nothing.
        calendarAt(now = at(today, 0, 30))

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 1) { repository.setEnabled(1L, enabled = false, nextTrigger = 0) }
        verify(exactly = 1) { scheduler.cancel(1L, keepFollowUps = true) }
        verify(exactly = 0) { scheduler.cancel(any(), keepFollowUps = false) }
    }

    @Test
    fun `an unchanged alarm the scheduler no longer holds is armed again`() = runBlocking {
        // Same event, same time, but the arm is gone (a failed re-arm, a force stop).
        calendarAt(now = at(today, 0, 30), at(today, 9))
        every { scheduler.isArmed(1L) } returns false
        coEvery { repository.save(any()) } returns 1L

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 1) { repository.save(any()) }
        coVerify(exactly = 1) { scheduler.schedule(match { it.id == 1L && it.specificDate == today.toString() }) }
    }

    @Test
    fun `a meeting moved ahead of its alarm rings soon instead of not at all`() = runBlocking {
        // At 7:00 the 9:00 standup moves to 7:30, so an hour before is 6:30, already gone.
        calendarAt(now = at(today, 7), at(today, 7, 30), at(tomorrow, 9, 30))
        val saved = slot<Alarm>()
        coEvery { repository.save(capture(saved)) } returns 1L

        assertEquals(Result.success(), worker().doWork())

        assertEquals(today.toString(), saved.captured.specificDate)
        assertEquals(7 to 2, saved.captured.hour to saved.captured.minute)

        // A pass a minute later keeps 7:02 rather than pushing it to 7:03.
        CalendarAutoAlarmWorker.clock = { at(today, 7, 1) }
        coEvery { repository.getAll() } returns
            listOf(saved.captured.copy(nextTriggerTime = at(today, 7, 2)).sanitized())
        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `a dismissed alarm doesn't ring again when a later meeting becomes the first`() = runBlocking {
        // The 9:00 standup rang at 8:00 and was dismissed, then got deleted,
        // which leaves the 14:00 review as today's first event.
        val rung = calendarAt(now = at(today, 10), at(today, 14), at(tomorrow, 9, 30))
        coEvery { repository.getAll() } returns listOf(rung.copy(isEnabled = false, nextTriggerTime = 0L))
        historyAt(at(today, 8))
        val saved = slot<Alarm>()
        coEvery { repository.save(capture(saved)) } returns 1L

        assertEquals(Result.success(), worker().doWork())

        assertEquals(tomorrow.toString(), saved.captured.specificDate)
        assertEquals(8 to 30, saved.captured.hour to saved.captured.minute)
    }

    @Test
    fun `a meeting that turns up overnight brings the alarm back to today`() = runBlocking {
        // At 00:30 today was empty, so the alarm went to tomorrow. An invite
        // for 10:00 today lands at 2:00.
        val moved = calendarAt(now = at(today, 2), at(today, 10), at(tomorrow, 9, 30))
        coEvery { repository.getAll() } returns listOf(
            moved.copy(minute = 30, specificDate = tomorrow.toString(), nextTriggerTime = at(tomorrow, 8, 30))
        )
        val saved = slot<Alarm>()
        coEvery { repository.save(capture(saved)) } returns 1L

        assertEquals(Result.success(), worker().doWork())

        assertEquals(today.toString(), saved.captured.specificDate)
        assertEquals(9 to 0, saved.captured.hour to saved.captured.minute)
    }

    @Test
    fun `a skip from the notification holds`() = runBlocking {
        // At 20:00 tomorrow's 8:30 alarm is skipped, which switches the row off.
        val skipped = calendarAt(now = at(today, 20), at(today, 9), at(tomorrow, 9, 30))
        coEvery { repository.getAll() } returns listOf(
            skipped.copy(minute = 30, specificDate = tomorrow.toString(), isEnabled = false, nextTriggerTime = 0L)
        )
        historyAt(at(tomorrow, 8, 30))

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 0) { repository.save(any()) }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun `a skip holds when the alarm comes the evening before a meeting just past midnight`() = runBlocking {
        // Tomorrow's first meeting is at 00:30, so its alarm is 23:30 tonight.
        // It's skipped at 22:00, and a pass runs at 22:15.
        val skipped = calendarAt(now = at(today, 22, 15), at(today, 9), at(tomorrow, 0, 30))
        coEvery { repository.getAll() } returns listOf(
            skipped.copy(hour = 23, minute = 30, isEnabled = false, nextTriggerTime = 0L)
        )
        historyAt(at(today, 23, 30))

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 0) { repository.save(any()) }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun `an alarm switched back on by hand after a skip stays on`() = runBlocking {
        val back = calendarAt(now = at(today, 20), at(today, 9), at(tomorrow, 9, 30))
        coEvery { repository.getAll() } returns listOf(
            back.copy(minute = 30, specificDate = tomorrow.toString(), nextTriggerTime = at(tomorrow, 8, 30))
        )
        historyAt(at(tomorrow, 8, 30))

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 0) { repository.setEnabled(any(), any(), any()) }
        verify(exactly = 0) { scheduler.cancel(any(), any()) }
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `an alarm switched back on by hand after a skip isn't moved to the next day's meeting`() = runBlocking {
        calendarAt(now = at(today, 7), at(today, 9), at(tomorrow, 9))
        historyAt(at(today, 8))

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 0) { repository.setEnabled(any(), any(), any()) }
        verify(exactly = 0) { scheduler.cancel(any(), any()) }
        coVerify(exactly = 0) { repository.save(any()) }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun `an alarm switched back on after its weather-moved ring was skipped stays on`() = runBlocking {
        // Bad weather moved the 8:00 alarm to 7:20, and the skip was saved for 7:20.
        val row = calendarAt(now = at(today, 7), at(today, 9), at(tomorrow, 9))
        coEvery { repository.getAll() } returns listOf(row.copy(weatherEarlyMinutes = 40))
        historyAt(at(today, 7, 20))

        assertEquals(Result.success(), worker().doWork())

        verify(exactly = 0) { scheduler.cancel(any(), any()) }
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `an earlier ring less than an hour before the row's time isn't taken for its own`() = runBlocking {
        // Today's 23:30 meeting rang at 23:00. Tomorrow's first meeting was at
        // 00:15, so the row was armed for 23:45 tonight. Then that meeting
        // moved to 9:00. The 23:00 ring was the day before's, so the row follows.
        val tonight = calendarAt(now = at(today, 23, 20), at(tomorrow, 9))
        coEvery { preferencesManager.getCurrentSettings() } returns
            AppSettings(calendarAutoAlarmEnabled = true, calendarAutoAlarmMinutesBefore = 30)
        coEvery { repository.getAll() } returns listOf(
            tonight.copy(hour = 23, minute = 45, nextTriggerTime = at(today, 23, 45))
        )
        historyAt(at(today, 23))
        coEvery { repository.save(any()) } returns 1L

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 1) {
            scheduler.schedule(
                match { it.id == 1L && it.specificDate == tomorrow.toString() && it.hour == 8 && it.minute == 30 }
            )
        }
    }

    @Test
    fun `an alarm set for tonight follows its meeting to tomorrow morning after an earlier ring today`() = runBlocking {
        // The alarm rang at 8:30 for today's standup. Tomorrow's first meeting
        // was at 00:30, and a 45 minute commute put its alarm at 23:15 tonight.
        // Then the meeting moved to 9:00. This morning's ring wasn't the 23:15
        // alarm's own, so the alarm moves with the meeting.
        val tonight = calendarAt(now = at(today, 21), at(today, 9), at(tomorrow, 9))
        coEvery { preferencesManager.getCurrentSettings() } returns
            AppSettings(calendarAutoAlarmEnabled = true, calendarAutoAlarmMinutesBefore = 30)
        coEvery { repository.getAll() } returns listOf(
            tonight.copy(hour = 23, minute = 15, nextTriggerTime = at(today, 23, 15))
        )
        historyAt(at(today, 8, 30))
        coEvery { repository.save(any()) } returns 1L

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 1) {
            scheduler.schedule(
                match { it.id == 1L && it.specificDate == tomorrow.toString() && it.hour == 8 && it.minute == 30 }
            )
        }
    }

    @Test
    fun `re-arming a row the scheduler switched off goes through a cancel that keeps the wake-up check`() = runBlocking {
        // The cancel is what clears a snooze count left behind when the
        // scheduler switched the row off without a ring ending.
        val off = calendarAt(now = at(today, 0, 30), at(today, 9))
        coEvery { repository.getAll() } returns listOf(off.copy(isEnabled = false, nextTriggerTime = 0L))
        AlarmRuntimeState.setSnoozeCount(context, 1L, 2)
        coEvery { repository.save(any()) } returns 1L

        assertEquals(Result.success(), worker().doWork())

        verify(exactly = 1) { scheduler.cancel(1L, keepFollowUps = true) }
        verify(exactly = 0) { scheduler.cancel(any(), keepFollowUps = false) }
        coVerify(exactly = 1) { scheduler.schedule(match { it.id == 1L && it.specificDate == today.toString() }) }
    }

    @Test
    fun `a ring holds the row for three hours and an early ring holds it too`() {
        val now = at(today, 12)
        val row = auto(id = 1L).copy(hour = 8, minute = 0, specificDate = today.toString())

        // Never dismissed: still the morning's ring until three hours have gone by.
        assertEquals(true, CalendarAutoAlarmWorker.occurrenceUnderway(row.copy(nextTriggerTime = now - 179 * 60_000L), now, zone))
        assertEquals(false, CalendarAutoAlarmWorker.occurrenceUnderway(row.copy(nextTriggerTime = now - 181 * 60_000L), now, zone))

        // A smart-wake ring at 7:40 for the 8:00 row leaves the trigger on 8:00.
        val early = row.copy(nextTriggerTime = at(today, 8))
        val quarterTo = at(today, 7, 45)
        assertEquals(false, CalendarAutoAlarmWorker.occurrenceUnderway(early, quarterTo, zone))
        assertEquals(true, CalendarAutoAlarmWorker.occurrenceUnderway(early, quarterTo, zone, inService = true))
        // Snoozed to 7:50: the count is what shows it.
        val snoozed = row.copy(nextTriggerTime = at(today, 7, 50))
        assertEquals(true, CalendarAutoAlarmWorker.occurrenceUnderway(snoozed, quarterTo, zone, snoozeTaken = true))
        assertEquals(false, CalendarAutoAlarmWorker.occurrenceUnderway(snoozed, quarterTo, zone))
        // A count left over from some other ring doesn't hold a row armed for tomorrow.
        val nextDay = row.copy(nextTriggerTime = at(tomorrow, 8))
        assertEquals(false, CalendarAutoAlarmWorker.occurrenceUnderway(nextDay, quarterTo, zone, snoozeTaken = true))
    }

    @Test
    fun `a second auto-alarm row is deleted and the oldest kept`() = runBlocking {
        settings(enabled = false)
        coEvery { repository.getAll() } returns listOf(auto(id = 1L), auto(id = 2L), auto(id = 3L))

        assertEquals(Result.success(), worker().doWork())

        // Oldest wins because it is the one the user may have edited.
        coVerify(exactly = 1) { repository.delete(match { it.id == 2L }) }
        coVerify(exactly = 1) { repository.delete(match { it.id == 3L }) }
        coVerify(exactly = 0) { repository.delete(match { it.id == 1L }) }
        verify { scheduler.cancel(2L) }
        verify { scheduler.cancel(3L) }
    }

    @Test
    fun `turning the feature off disables the survivor instead of deleting it`() = runBlocking {
        settings(enabled = false)
        coEvery { repository.getAll() } returns listOf(auto(id = 1L), auto(id = 2L))

        worker().doWork()

        // Disabled, not deleted: the row carries whatever sound and time the
        // user set on it, and turning the feature back on should get those back.
        coVerify { repository.setEnabled(1L, enabled = false, nextTrigger = 0) }
        verify { scheduler.cancel(1L) }
        coVerify(exactly = 0) { repository.delete(match { it.id == 1L }) }
    }

    @Test
    fun `alarms the worker does not own are left alone`() = runBlocking {
        settings(enabled = false)
        val mine = auto(id = 1L)
        val theirs = auto(id = 9L).copy(profileName = "", label = "Work")
        coEvery { repository.getAll() } returns listOf(mine, theirs)

        worker().doWork()

        coVerify(exactly = 0) { repository.delete(theirs) }
        verify(exactly = 0) { scheduler.cancel(9L, any()) }
        coVerify(exactly = 0) { repository.setEnabled(9L, any(), any()) }
    }

    @Test
    fun `an event-free tomorrow parks the row rather than adding another`() = runBlocking {
        // Robolectric has no calendar provider, so the query comes back empty,
        // which is exactly the shape of a day with nothing on it.
        settings(enabled = true)
        shadowOf(context as Application).grantPermissions(Manifest.permission.READ_CALENDAR)
        coEvery { repository.getAll() } returns listOf(auto(id = 1L), auto(id = 2L))

        assertEquals(Result.success(), worker().doWork())

        coVerify(exactly = 1) { repository.delete(match { it.id == 2L }) }
        coVerify { repository.setEnabled(1L, enabled = false, nextTrigger = 0) }
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `without calendar permission nothing is touched`() = runBlocking {
        settings(enabled = true)
        shadowOf(context as Application).denyPermissions(Manifest.permission.READ_CALENDAR)
        coEvery { repository.getAll() } returns listOf(auto(id = 1L), auto(id = 2L))

        assertEquals(Result.success(), worker().doWork())

        // Reading no events is not the same as there being none, so the stray
        // cleanup does not run either.
        coVerify(exactly = 0) { repository.delete(any()) }
        coVerify(exactly = 0) { repository.setEnabled(any(), any(), any()) }
    }

    private fun settings(enabled: Boolean) {
        coEvery { preferencesManager.getCurrentSettings() } returns
            AppSettings(calendarAutoAlarmEnabled = enabled)
    }

    @Test
    fun `a pass that finds the same event leaves the armed alarm alone`() {
        val now = 1_000_000L
        val stored = auto(id = 1L).copy(specificDate = "2026-11-16", nextTriggerTime = now + 3_600_000L)
        // What the pass builds from tomorrow's event: same row, trigger not yet computed.
        val rebuilt = stored.copy(nextTriggerTime = 0L)

        assertEquals(true, CalendarAutoAlarmWorker.alreadyArmed(stored, rebuilt, now, registered = true))
        // A moved event, a disarmed row, a trigger already past and a first run all re-arm.
        assertEquals(false, CalendarAutoAlarmWorker.alreadyArmed(stored, rebuilt.copy(minute = 30), now, registered = true))
        assertEquals(false, CalendarAutoAlarmWorker.alreadyArmed(stored.copy(nextTriggerTime = 0L), rebuilt, now, registered = true))
        assertEquals(false, CalendarAutoAlarmWorker.alreadyArmed(stored, rebuilt, now + 3_600_000L, registered = true))
        assertEquals(false, CalendarAutoAlarmWorker.alreadyArmed(null, rebuilt, now, registered = false))
        // A failed arm still wrote the trigger to the row, so only the scheduler can say it took.
        assertEquals(false, CalendarAutoAlarmWorker.alreadyArmed(stored, rebuilt, now, registered = false))
    }

    @Test
    fun `an event title past the label limit still counts as the same alarm`() {
        val now = 1_000_000L
        val title = "Before: " + "quarterly planning ".repeat(10)
        // The repository saves rows sanitized, which trims the label to 120 characters.
        val stored = auto(id = 1L).copy(label = title, nextTriggerTime = now + 3_600_000L).sanitized()
        // Rebuilt from the stored row the way the worker does it, so createdAt matches.
        val rebuilt = stored.copy(label = title, nextTriggerTime = 0L)

        assertEquals(true, CalendarAutoAlarmWorker.alreadyArmed(stored, rebuilt, now, registered = true))
    }

    private fun auto(id: Long) = Alarm(
        id = id,
        hour = 6,
        minute = 45,
        label = "Before: standup",
        isEnabled = true,
        group = "Calendar",
        profileName = "calendar_auto"
    )

    /** Answers CalendarContract.Instances queries from [events], each a timed "Standup" with no location. */
    class FakeCalendar : ContentProvider() {
        companion object {
            var events: List<Long> = emptyList()
        }

        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val (begin, end) = uri.pathSegments.takeLast(2).map { it.toLong() }
            val columns = projection ?: emptyArray()
            return MatrixCursor(columns).apply {
                events.filter { it in begin until end }.sorted().forEach { start ->
                    addRow(columns.map { column ->
                        when (column) {
                            CalendarContract.Instances.BEGIN -> start
                            CalendarContract.Instances.TITLE -> "Standup"
                            CalendarContract.Instances.ALL_DAY -> 0
                            else -> ""
                        }
                    })
                }
            }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ) = 0
    }

    private fun worker(): CalendarAutoAlarmWorker =
        TestListenableWorkerBuilder<CalendarAutoAlarmWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters
                    ) = CalendarAutoAlarmWorker(
                        context = appContext,
                        workerParams = workerParameters,
                        preferencesManager = preferencesManager,
                        repository = repository,
                        scheduler = scheduler,
                        weatherRepository = mockk<WeatherRepository>(relaxed = true),
                        commuteRouteRepository = mockk<CommuteRouteRepository>(relaxed = true),
                        commuteHistoryStore = mockk<CommuteHistoryStore>(relaxed = true),
                        eventRepository = eventRepository
                    )
                }
            )
            .build()
}
