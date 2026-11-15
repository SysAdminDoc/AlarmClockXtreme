package com.sysadmindoc.alarmclock

import android.annotation.SuppressLint
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.isActive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room 2.8 refreshes table invalidation on each database's own coroutine
 * scope after a write. Robolectric closes every SQLite connection after a
 * test, and a refresh that runs after that fails whichever runTest comes
 * next. onTerminate has to shut down the refresh scope of both the app's
 * database and WorkManager's.
 *
 * The scope is checked rather than isOpen: WorkManager's singleton outlives
 * each test's Application, and anything that touches its open helper after
 * the close reopens the connection without bringing the scope back.
 *
 * That singleton is shared by every test in the same Robolectric sandbox, so
 * an earlier AlarmClockApp test would already have closed it and the last
 * assertion would pass with no close at all. The only other SDK 29 test runs
 * on a plain Application that never starts WorkManager, so this class gets a
 * fresh singleton without adding a sandbox (one more android-all overflows
 * the default 512 MB test heap). Both scopes are asserted live before the
 * terminate, so a shared singleton fails loudly instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@SuppressLint("RestrictedApi")
class AlarmClockAppTerminateTest {

    @Test
    fun terminatingTheAppStopsRoomRefreshesOnBothDatabases() {
        val app = ApplicationProvider.getApplicationContext<AlarmClockApp>()
        val alarmDatabase = EntryPointAccessors
            .fromApplication(app, AlarmClockApp.AppEntryPoint::class.java)
            .alarmDatabase()
        assertTrue(WorkManager.isInitialized())
        val workDatabase = WorkManagerImpl.getInstance(app).workDatabase
        assertTrue(alarmDatabase.getCoroutineScope().isActive)
        assertTrue(workDatabase.getCoroutineScope().isActive)

        app.onTerminate()

        assertFalse(alarmDatabase.getCoroutineScope().isActive)
        assertFalse(workDatabase.getCoroutineScope().isActive)
    }
}
