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
import io.mockk.verify
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
                vibrationDelaySeconds = 45,
                firingBackgroundImageEnabled = true,
                firingBackgroundImageUri = "content://media/backgrounds/workday.jpg",
                firingBackgroundBlurEnabled = false,
                sortOrder = 7_000
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
        assertEquals(true, alarm.firingBackgroundImageEnabled)
        assertEquals("content://media/backgrounds/workday.jpg", alarm.firingBackgroundImageUri)
        assertEquals(false, alarm.firingBackgroundBlurEnabled)
        assertEquals(7_000, alarm.sortOrder)

        val settings = backup.settings!!
        assertTrue(settings.is24HourFormat)
        assertEquals(25, settings.defaultSnoozeDuration)
        assertEquals(15, settings.autoSilenceMinutes)
        assertEquals("Portland, Oregon", settings.locationName)
        assertEquals(false, settings.showTimerTab)
        assertEquals("https://feeds.example/private.xml", settings.newsFeedUrl)
        assertEquals(true, settings.hideAlarmLabelsOnPublicSurfaces)
        assertEquals(true, settings.calendarCommuteAwareEnabled)
        assertEquals(35, settings.calendarCommuteBaselineMinutes)
        assertEquals(20, settings.calendarCommuteWeatherExtraMinutes)
        assertEquals("routes-test-key", settings.googleRoutesApiKey)
        assertEquals("webhook-signing-secret", settings.webhookSigningSecret)
        assertEquals("0,1,2,3,4", settings.chronotypeAnswers)
        assertEquals(5 * 60 + 45, settings.jetLagTargetWakeMinutes)
        assertEquals(5, settings.jetLagAdjustmentDays)
        assertEquals("advance", settings.jetLagDirection)
    }

    @Test
    fun importFromUriRestoresSettingsAndSchedulesEnabledAlarms() = runTest {
        val backupJson = backupAdapter.toJson(
            BackupData(
                alarms = listOf(
                    morningAlarm().copy(
                        firingBackgroundImageEnabled = true,
                        firingBackgroundImageUri = "content://media/backgrounds/workday.jpg",
                        firingBackgroundBlurEnabled = false,
                        sortOrder = 7_000
                    ).toAlarmBackup()
                ),
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
                    newsFeedUrl = "https://feeds.example/private.xml",
                    chronotypeAnswers = "0,1,2,3,4",
                    webhookSigningSecret = "webhook-signing-secret",
                    calendarCommuteAwareEnabled = true,
                    calendarCommuteBaselineMinutes = 35,
                    calendarCommuteWeatherExtraMinutes = 20,
                    googleRoutesApiKey = "routes-test-key",
                    jetLagTargetWakeMinutes = 5 * 60 + 45,
                    jetLagAdjustmentDays = 5,
                    jetLagDirection = "advance"
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

        // This case is about the field mapping, so it opts into the integration
        // values. Importing them is consent-gated now; the default-off path is
        // covered by BackupManagerImportConsentTest.
        val result = backupManager.importFromUri(
            Uri.fromFile(backupFile),
            BackupImportOptions(keepIntegrationsAndContacts = true)
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals("Portland, Oregon", restoredSettings!!.locationName)
        assertEquals(false, restoredSettings!!.showTimerTab)
        assertEquals("https://feeds.example/private.xml", restoredSettings!!.newsFeedUrl)
        assertEquals(true, restoredSettings!!.hideAlarmLabelsOnPublicSurfaces)
        assertEquals(true, restoredSettings!!.calendarCommuteAwareEnabled)
        assertEquals(35, restoredSettings!!.calendarCommuteBaselineMinutes)
        assertEquals(20, restoredSettings!!.calendarCommuteWeatherExtraMinutes)
        assertEquals("routes-test-key", restoredSettings!!.googleRoutesApiKey)
        assertEquals("webhook-signing-secret", restoredSettings!!.webhookSigningSecret)
        assertEquals("0,1,2,3,4", restoredSettings!!.chronotypeAnswers)
        assertEquals(5 * 60 + 45, restoredSettings!!.jetLagTargetWakeMinutes)
        assertEquals(5, restoredSettings!!.jetLagAdjustmentDays)
        assertEquals("advance", restoredSettings!!.jetLagDirection)
        coVerify {
            repository.save(
                match {
                    it.label == "Weekday lift" &&
                        it.repeatDays == setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY) &&
                        it.firingBackgroundImageEnabled &&
                        it.firingBackgroundImageUri == "content://media/backgrounds/workday.jpg" &&
                        !it.firingBackgroundBlurEnabled &&
                        it.sortOrder == 7_000 &&
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

    @Test
    fun inspectImportFromUriPreviewsBackupWithoutWriting() = runTest {
        val backupJson = backupAdapter.toJson(
            BackupData(
                appVersion = "1.15.24",
                exportedAt = 1_700_000_000_000L,
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
                    webhookSigningSecret = "webhook-signing-secret"
                )
            )
        )
        val backupFile = File(context.cacheDir, "backup-manager-preview-test.json")
            .apply { writeText(backupJson) }

        val result = backupManager.inspectImportFromUri(Uri.fromFile(backupFile))

        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertEquals(BackupManager.MAX_SUPPORTED_BACKUP_VERSION, preview.version)
        assertEquals("1.15.24", preview.appVersion)
        assertEquals(1, preview.alarmCount)
        assertEquals(1, preview.enabledAlarmCount)
        assertEquals(0, preview.invalidAlarmCount)
        assertTrue(preview.settingsIncluded)
        assertTrue(preview.canImport)
        assertTrue(preview.privateDataCategories.contains("Webhook signing secret"))
        coVerify(exactly = 0) { repository.save(any()) }
        coVerify(exactly = 0) { preferencesManager.update(any()) }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
        verify(exactly = 0) { scheduler.cancel(any()) }
    }

    @Test
    fun importFromUriCanReplaceExistingAlarmsAndDisableImportedAlarms() = runTest {
        val existing = morningAlarm().copy(id = 44L, label = "Old alarm")
        val backupJson = backupAdapter.toJson(
            BackupData(
                alarms = listOf(morningAlarm().copy(id = 7L).toAlarmBackup()),
                settings = null
            )
        )
        val backupFile = File(context.cacheDir, "backup-manager-replace-test.json")
            .apply { writeText(backupJson) }
        coEvery { repository.getAll() } returns listOf(existing)
        coEvery { repository.save(any()) } returns 101L

        val result = backupManager.importFromUri(
            Uri.fromFile(backupFile),
            BackupImportOptions(
                mode = BackupImportMode.Replace,
                importEnabledAsDisabled = true
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        verify { scheduler.cancel(44L) }
        // Replace now removes the previous rows by id, after the new ones are
        // saved, so a crash mid-restore can never leave zero alarms.
        coVerify { repository.deleteById(44L) }
        coVerify {
            repository.save(
                match {
                    it.label == "Weekday lift" &&
                        // Replace writes over the same row, so alarm_events and
                        // the snooze counts keyed by this id stay attached.
                        it.id == 7L &&
                        !it.isEnabled &&
                        it.nextTriggerTime == 0L
                }
            )
        }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun appendKeepsAllocatingNewRowsSoAnImportCannotOverwriteALiveAlarm() = runTest {
        // Append merges someone else's alarms into a list that already has its
        // own. Reusing the file's ids there would silently replace whatever
        // happened to share a number.
        val backupJson = backupAdapter.toJson(
            BackupData(
                alarms = listOf(morningAlarm().copy(id = 7L).toAlarmBackup()),
                settings = null
            )
        )
        val backupFile = File(context.cacheDir, "backup-manager-append-id-test.json")
            .apply { writeText(backupJson) }
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.save(any()) } returns 202L

        val result = backupManager.importFromUri(
            Uri.fromFile(backupFile),
            BackupImportOptions(mode = BackupImportMode.Append)
        )

        assertTrue(result.isSuccess)
        coVerify { repository.save(match { it.label == "Weekday lift" && it.id == 0L }) }
    }

    @Test
    fun aBackupWrittenBeforeIdsWereStoredStillImports() = runTest {
        // Hand-written, because serialising through the current adapter emits
        // "id":0 and would exercise the present-but-zero case rather than the
        // absent one. A real pre-v19 file has no id key at all, which is the
        // only reason AlarmBackup.id carries a default.
        val legacy = """
            {"version":18,"appVersion":"1.15.0","exportedAt":1700000000000,
             "alarms":[{"hour":6,"minute":35,"label":"Weekday lift","isEnabled":true,
             "repeatDays":["MONDAY"],"ringtoneUri":"","vibrationEnabled":true,
             "vibrationIntensity":1,"volume":85,"overrideSystemVolume":false,
             "gradualVolumeSeconds":90,"snoozeDurationMinutes":25,"maxSnoozeCount":4,
             "showOnLockScreen":false,"challengeType":"NONE"}],"settings":null}
        """.trimIndent()
        val backupFile = File(context.cacheDir, "backup-manager-legacy-id-test.json")
            .apply { writeText(legacy) }
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.save(any()) } returns 303L

        val result = backupManager.importFromUri(
            Uri.fromFile(backupFile),
            BackupImportOptions(mode = BackupImportMode.Replace)
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        coVerify { repository.save(match { it.id == 0L }) }
    }

    @Test
    fun replaceDisarmsAnAlarmItOverwritesEvenWhenTheRowSurvives() = runTest {
        // Keeping the id means the row is no longer in the deletion set, which
        // is what used to disarm it. A restore that brings an alarm back as
        // disabled must still cancel what the old one had armed, or the phone
        // rings at the old time for an alarm the list shows as off.
        val existing = morningAlarm().copy(id = 7L, isEnabled = true)
        val backupJson = backupAdapter.toJson(
            BackupData(
                alarms = listOf(morningAlarm().copy(id = 7L).toAlarmBackup()),
                settings = null
            )
        )
        val backupFile = File(context.cacheDir, "backup-manager-disarm-test.json")
            .apply { writeText(backupJson) }
        coEvery { repository.getAll() } returns listOf(existing)
        coEvery { repository.save(any()) } returns 7L

        val result = backupManager.importFromUri(
            Uri.fromFile(backupFile),
            BackupImportOptions(mode = BackupImportMode.Replace, importEnabledAsDisabled = true)
        )

        assertTrue(result.isSuccess)
        verify { scheduler.cancel(7L) }
        // Disabled, so it must not be rearmed either.
        coVerify(exactly = 0) { scheduler.schedule(any()) }
        // The row itself stays: this is an overwrite, not a removal.
        coVerify(exactly = 0) { repository.deleteById(7L) }
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
        hideAlarmLabelsOnPublicSurfaces = true,
        showWeatherOnDashboard = false,
        showCalendarOnDashboard = true,
        autoSilenceMinutes = 15,
        locationName = "Portland, Oregon",
        useManualLocation = true,
        showTimerTab = false,
        newsFeedUrl = "https://feeds.example/private.xml",
        webhookSigningSecret = "webhook-signing-secret",
        calendarCommuteAwareEnabled = true,
        calendarCommuteBaselineMinutes = 35,
        calendarCommuteWeatherExtraMinutes = 20,
        googleRoutesApiKey = "routes-test-key",
        chronotypeAnswers = "0,1,2,3,4",
        jetLagTargetWakeMinutes = 5 * 60 + 45,
        jetLagAdjustmentDays = 5,
        jetLagDirection = "advance"
    )
}
