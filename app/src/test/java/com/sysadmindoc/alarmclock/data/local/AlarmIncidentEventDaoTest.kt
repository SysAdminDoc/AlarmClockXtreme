package com.sysadmindoc.alarmclock.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
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
class AlarmIncidentEventDaoTest {

    private lateinit var database: AlarmDatabase
    private lateinit var dao: AlarmIncidentEventDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AlarmDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.alarmIncidentEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun reasonCountMatchesOnlyTheListedReasonsForThatOccurrence() = runTest {
        suspend fun insert(reason: String, alarmId: Long = 7L, scheduledAt: Long = 1_000L) {
            dao.insert(
                AlarmIncidentEvent(
                    fireId = "alarm-$alarmId-test",
                    alarmId = alarmId,
                    scheduledAt = scheduledAt,
                    eventAt = 1_500L,
                    elapsedMs = 500L,
                    type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                    status = AlarmIncidentEvent.STATUS_RECEIVED,
                    reasonCode = reason,
                    source = "test",
                    sdkInt = 30,
                    standbyBucket = "ACTIVE_10",
                    exactAlarmAllowed = "TRUE",
                    notificationPermissionGranted = "TRUE",
                    fullScreenIntentAllowed = "TRUE",
                    batteryOptimizationsIgnored = "TRUE",
                    algorithmVersion = AlarmIncidentEvent.VALUE_NONE
                )
            )
        }
        insert("A")
        insert("B")
        insert("B")
        insert("C")
        insert("A", alarmId = 8L)
        insert("A", scheduledAt = 2_000L)

        assertEquals(3, dao.countByOccurrenceAndReasons(7L, 1_000L, listOf("A", "B")))
        assertEquals(1, dao.countByOccurrenceAndReasons(7L, 1_000L, listOf("C")))
        assertEquals(0, dao.countByOccurrenceAndReasons(7L, 1_000L, listOf("D")))
    }
}
