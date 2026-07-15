package com.sysadmindoc.alarmclock.data.backup

import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.full.primaryConstructor

/**
 * Field-drift guard for the backup settings round-trip.
 *
 * History: v1.1.0 shipped a backup format that silently dropped 15 settings,
 * and the v1-v8 formats accumulated 12 more AppSettings fields that never
 * made it into SettingsBackup (fixed in backup v9). This test makes the
 * omission a build-time failure instead of silent restore-time data loss:
 * every AppSettings constructor parameter must exist in SettingsBackup
 * unless it is deliberately listed as backup-exempt below.
 */
class BackupManagerSettingsDriftTest {

    /**
     * Fields that intentionally do NOT round-trip through backups.
     * Keep this list justified — an entry here means "resetting to the
     * default on restore is the correct behavior", not "we forgot".
     */
    private val intentionallyNotBackedUp = setOf(
        "ytEngineBundledVersion",
        "ytEngineActiveVersion",
        "ytEngineLastUpdateMs",
        "ytEngineLastUpdateStatus",
        "ytEngineLastUpdateSource",
        "ytEngineLastFailureReason",
        "bedtimeStayUpLateUntilMillis",
        "webhookLastDeliveryStatus",
        "webhookLastDeliveryAtMillis",
        "webhookDeliveryLog",
        // Device-local audio comfort settings should not silently change alarm
        // loudness when a backup moves to hardware with different speakers.
        "challengeAudioDuckingEnabled",
        "challengeAudioDuckPercent",
        // Accessibility behavior follows each device's display/animation
        // needs and Android system setting instead of migrating blindly.
        "reduceMotionAndFlashing",
    )

    @Test
    fun `every AppSettings field round-trips through SettingsBackup`() {
        val settingsFields = AppSettings::class.primaryConstructor!!
            .parameters.mapNotNull { it.name }.toSet()
        val backupFields = SettingsBackup::class.primaryConstructor!!
            .parameters.mapNotNull { it.name }.toSet()

        val missing = settingsFields - backupFields - intentionallyNotBackedUp
        assertTrue(
            "AppSettings fields missing from SettingsBackup (add them to the " +
                "backup format and bump the version, or list them as " +
                "intentionally exempt): $missing",
            missing.isEmpty()
        )
    }

    @Test
    fun `backup-exempt list only names real AppSettings fields`() {
        val settingsFields = AppSettings::class.primaryConstructor!!
            .parameters.mapNotNull { it.name }.toSet()
        val stale = intentionallyNotBackedUp - settingsFields
        assertTrue(
            "Backup-exempt entries no longer exist on AppSettings: $stale",
            stale.isEmpty()
        )
    }
}
