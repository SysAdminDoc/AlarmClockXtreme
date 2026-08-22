package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.sysadmindoc.alarmclock.data.local.CommuteHistoryStore
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.repository.CommuteRouteRepository
import com.sysadmindoc.alarmclock.data.repository.WeatherRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        preferencesManager = mockk()
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
        verify(exactly = 0) { scheduler.cancel(9L) }
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

    private fun auto(id: Long) = Alarm(
        id = id,
        hour = 6,
        minute = 45,
        label = "Before: standup",
        isEnabled = true,
        group = "Calendar",
        profileName = "calendar_auto"
    )

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
                        commuteHistoryStore = mockk<CommuteHistoryStore>(relaxed = true)
                    )
                }
            )
            .build()
}
