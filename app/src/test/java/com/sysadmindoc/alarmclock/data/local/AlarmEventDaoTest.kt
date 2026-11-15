package com.sysadmindoc.alarmclock.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class AlarmEventDaoTest {

    private lateinit var database: AlarmDatabase
    private lateinit var dao: AlarmEventDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AlarmDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.alarmEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun rowsAreCountedByTheTimeTheRingWasSetForAndFallBackToWhenItFired() = runTest {
        suspend fun insert(scheduledTime: Long, firedAt: Long, alarmId: Long = 7L) {
            dao.insert(
                AlarmEvent(
                    alarmId = alarmId,
                    scheduledTime = scheduledTime,
                    firedAt = firedAt,
                    action = AlarmEvent.ACTION_DISMISSED
                )
            )
        }
        // Set for 990, rang late at 1_010: counts at 990.
        insert(scheduledTime = 990L, firedAt = 1_010L)
        // No scheduled time on the row: placed by when it fired.
        insert(scheduledTime = 0L, firedAt = 1_500L)
        // Another alarm's row in the same window.
        insert(scheduledTime = 1_200L, firedAt = 1_200L, alarmId = 8L)

        assertEquals(1, dao.countForAlarmBetween(7L, 900L, 1_000L))
        assertEquals(0, dao.countForAlarmBetween(7L, 1_000L, 1_100L))
        assertEquals(1, dao.countForAlarmBetween(7L, 1_100L, 2_000L))
        assertEquals(0, dao.countForAlarmBetween(7L, 1_500L - 100L, 1_500L))
        assertEquals(2, dao.countForAlarmBetween(7L, 0L, 2_000L))
    }
}
