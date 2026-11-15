package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.remote.ReleaseApi
import com.sysadmindoc.alarmclock.data.remote.ReleaseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

// A plain Application: AlarmClockApp's startup posts the next-alarm notice on
// its own schedule, which would land in the shade these tests read.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class UpdateCheckWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val releaseApi = mockk<ReleaseApi>()
    private val preferences = mockk<PreferencesManager>()
    private var settings = AppSettings(updateCheckEnabled = true)

    @Before
    fun setUp() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        coEvery { preferences.getCurrentSettings() } answers { settings }
        coEvery { preferences.update(any()) } answers {
            settings = firstArg<(AppSettings) -> AppSettings>().invoke(settings)
        }
    }

    private fun run(): Result = runBlocking {
        TestListenableWorkerBuilder<UpdateCheckWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters
                    ) = UpdateCheckWorker(appContext, workerParameters, preferences, releaseApi)
                }
            )
            .build()
            .doWork()
    }

    private fun posted() =
        shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications

    @Test
    fun `switched off, it never calls GitHub`() {
        settings = AppSettings(updateCheckEnabled = false)
        assertEquals(Result.success(), run())
        coVerify(exactly = 0) { releaseApi.latest() }
        assertTrue(posted().isEmpty())
    }

    @Test
    fun `offline or rate limited, it says nothing`() {
        coEvery { releaseApi.latest() } throws IOException("no network")
        assertEquals(Result.success(), run())
        coEvery { releaseApi.latest() } throws HttpException(Response.error<ReleaseDto>(403, "".toResponseBody(null)))
        assertEquals(Result.success(), run())
        assertTrue(posted().isEmpty())
        assertEquals("", settings.updateCheckLastNotifiedTag)
    }

    @Test
    fun `the release this build already is posts nothing`() {
        coEvery { releaseApi.latest() } returns ReleaseDto(tagName = "v" + BuildConfig.VERSION_NAME)
        assertEquals(Result.success(), run())
        assertTrue(posted().isEmpty())
    }

    @Test
    fun `a newer release posts one notice that opens its page`() {
        coEvery { releaseApi.latest() } returns ReleaseDto(tagName = "v99.0.0")
        assertEquals(Result.success(), run())

        val notice = posted().single()
        val opened = shadowOf(notice.contentIntent).savedIntent
        assertEquals(
            "https://github.com/SysAdminDoc/AlarmClockXtreme/releases/tag/v99.0.0",
            opened.dataString
        )
        assertEquals("v99.0.0", settings.updateCheckLastNotifiedTag)

        // The next day's check finds the same release and stays quiet.
        context.getSystemService(NotificationManager::class.java).cancelAll()
        assertEquals(Result.success(), run())
        assertTrue(posted().isEmpty())
    }

    @Test
    fun `with notifications blocked it waits to tell them until they can see it`() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        coEvery { releaseApi.latest() } returns ReleaseDto(tagName = "v99.0.0")
        assertEquals(Result.success(), run())
        assertTrue(posted().isEmpty())
        assertEquals("", settings.updateCheckLastNotifiedTag)

        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(Result.success(), run())
        assertEquals(1, posted().size)
        assertEquals("v99.0.0", settings.updateCheckLastNotifiedTag)
    }

    @Test
    fun `with the app's notifications switched off it waits as well`() {
        val manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(false)
        coEvery { releaseApi.latest() } returns ReleaseDto(tagName = "v99.0.0")
        assertEquals(Result.success(), run())
        assertEquals("", settings.updateCheckLastNotifiedTag)

        shadowOf(manager).setNotificationsEnabled(true)
        assertEquals(Result.success(), run())
        assertEquals(1, posted().size)
        assertEquals("v99.0.0", settings.updateCheckLastNotifiedTag)
    }

    @Test
    fun `with only the updates channel blocked it waits as well`() {
        // The user turned the channel off in Android's settings. Creating it
        // again never raises the importance back.
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("app_updates", "App updates", NotificationManager.IMPORTANCE_NONE)
        )
        coEvery { releaseApi.latest() } returns ReleaseDto(tagName = "v99.0.0")
        assertEquals(Result.success(), run())
        assertEquals("", settings.updateCheckLastNotifiedTag)
    }
}
