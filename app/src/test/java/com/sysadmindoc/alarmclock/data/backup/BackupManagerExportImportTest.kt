package com.sysadmindoc.alarmclock.data.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.squareup.moshi.Moshi
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.time.DayOfWeek
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupManagerExportImportTest {
    private lateinit var context: Context
    private lateinit var repository: AlarmRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var scheduler: AlarmScheduler
    private lateinit var backupManager: BackupManager
    private val backupAdapter = Moshi.Builder().build().adapter(BackupData::class.java)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        backupManager = BackupManager(
            context = context,
            repository = repository,
            preferencesManager = preferencesManager,
            scheduler = scheduler
        )
    }

    @Test
    fun exportIncludesAlarmAndSettingsRoundTripFields() = runTest {
        coEvery { repository.getAll() } returns listOf(
            morningAlarm().copy(
                id = 12L,
                internetRadioUrl = "https://radio.example/stream",
                vibrationDelaySeconds = 45
            )
        )
        coEvery { preferencesManager.getCurrentSettings() } returns premiumSettings()

        val backup = backupAdapter.fromJson(backupManager.export())

        assertNotNull(backup)
        assertEquals(BackupManager.MAX_SUPPORTED_BACKUP_VERSION, backup!!.version)
        assertEquals(1, backup.alarms.size)

        val alarm = backup.alarms.single()
        assertEquals(6, alarm.hour)
        assertEquals(35, alarm.minute)
        assertEquals("Weekday lift", alarm.label)
        assertEquals(setOf("MONDAY", "WEDNESDAY", "FRIDAY"), alarm.repeatDays.toSet())
        assertEquals("https://radio.example/stream", alarm.internetRadioUrl)
        assertEquals(45, alarm.vibrationDelaySeconds)

        val settings = backup.settings!!
        assertTrue(settings.is24HourFormat)
        assertEquals(25, settings.defaultSnoozeDuration)
        assertEquals(15, settings.autoSilenceMinutes)
        assertEquals("Portland, Oregon", settings.locationName)
        assertEquals(false, settings.showTimerTab)
        assertEquals("https://feeds.example/private.xml", settings.newsFeedUrl)
        assertEquals(true, settings.hideAlarmLabelsOnPublicSurfaces)
    }

    @Test
    fun importFromUriRestoresSettingsAndSchedulesEnabledAlarms() = runTest {
        val backupJson = backupAdapter.toJson(
            BackupData(
                alarms = listOf(morningAlarm().toAlarmBackup()),
                settings = SettingsBackup(
                    is24HourFormat = true,
                    defaultSnoozeDuration = 25,
                    defaultGradualVolume = 90,
                    usePhoneSpeakers = true,
                    showOnLockScreen = false,
                    hideAlarmLabelsOnPublicSurfaces = true,
                    vacationModeEnabled = false,
                    vacationStartMillis = 0,
                    vacationEndMillis = 0,
                    showWeatherOnDashboard = false,
                    showCalendarOnDashboard = true,
                    autoSilenceMinutes = 15,
                    locationName = "Portland, Oregon",
                    useManualLocation = true,
                    showTimerTab = false,
                    newsFeedUrl = "https://feeds.example/private.xml"
                )
            )
        )
        val backupFile = File(context.cacheDir, "backup-manager-import-test.json")
            .apply { writeText(backupJson) }
        var restoredSettings: AppSettings? = null
        coEvery { repository.save(any()) } returns 99L
        coEvery { preferencesManager.update(any()) } coAnswers {
            val transform = firstArg<(AppSettings) -> AppSettings>()
            restoredSettings = transform(AppSettings())
        }

        val result = backupManager.importFromUri(Uri.fromFile(backupFile))

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals("Portland, Oregon", restoredSettings!!.locationName)
        assertEquals(false, restoredSettings!!.showTimerTab)
        assertEquals("https://feeds.example/private.xml", restoredSettings!!.newsFeedUrl)
        assertEquals(true, restoredSettings!!.hideAlarmLabelsOnPublicSurfaces)
        coVerify {
            repository.save(
                match {
                    it.label == "Weekday lift" &&
                        it.repeatDays == setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY) &&
                        it.nextTriggerTime == 0L
                }
            )
        }
        coVerify {
            scheduler.schedule(
                match { it.id == 99L && it.label == "Weekday lift" && it.isEnabled }
            )
        }
    }

    private fun morningAlarm(): Alarm = Alarm(
        hour = 6,
        minute = 35,
        label = "Weekday lift",
        isEnabled = true,
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        ringtoneUri = "content://media/external/audio/media/42",
        vibrationEnabled = true,
        vibrationIntensity = 1,
        volume = 85,
        gradualVolumeSeconds = 90,
        snoozeDurationMinutes = 25,
        maxSnoozeCount = 4,
        showOnLockScreen = false,
        wakeConfirmEnabled = true,
        wakeConfirmDelayMinutes = 12,
        smartAlarmEnabled = true,
        smartAlarmWindowMinutes = 45
    )

    private fun premiumSettings(): AppSettings = AppSettings(
        is24HourFormat = true,
        defaultSnoozeDuration = 25,
        defaultGradualVolume = 90,
        usePhoneSpeakers = true,
        showOnLockScreen = false,
        hideAlarmLabelsOnPublicSurfaces = true,
        showWeatherOnDashboard = false,
        showCalendarOnDashboard = true,
        autoSilenceMinutes = 15,
        locationName = "Portland, Oregon",
        useManualLocation = true,
        showTimerTab = false,
        newsFeedUrl = "https://feeds.example/private.xml"
    )
}
