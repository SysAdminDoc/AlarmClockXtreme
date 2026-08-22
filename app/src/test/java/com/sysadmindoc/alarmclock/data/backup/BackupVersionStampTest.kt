package com.sysadmindoc.alarmclock.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.full.primaryConstructor

/**
 * Keeps the stamped backup version honest.
 *
 * Exports were stamped `version = 17` while the format kept gaining fields and
 * changing what it applies on import, so a reader could not tell one v17 file
 * from another. Field counts are recorded here on purpose: adding, removing or
 * repurposing a field breaks this test, and the only way to fix it is to bump
 * the version alongside the count.
 */
class BackupVersionStampTest {

    private companion object {
        /** Bump with [BackupData.version] whenever these change. */
        // 71 since v19 added AlarmBackup.id.
        const val ALARM_BACKUP_FIELDS = 71
        const val SETTINGS_BACKUP_FIELDS = 77
    }

    @Test
    fun `exports are stamped with the newest version this build understands`() {
        assertEquals(
            "A build must never write a backup it would refuse to read",
            BackupManager.MAX_SUPPORTED_BACKUP_VERSION,
            BackupData(alarms = emptyList(), settings = null).version
        )
    }

    @Test
    fun `the alarm payload has not changed without a version bump`() {
        assertEquals(
            "AlarmBackup changed shape. Bump BackupData.version and " +
                "MAX_SUPPORTED_BACKUP_VERSION together, then update this count.",
            ALARM_BACKUP_FIELDS,
            AlarmBackup::class.primaryConstructor!!.parameters.size
        )
    }

    @Test
    fun `the settings payload has not changed without a version bump`() {
        assertEquals(
            "SettingsBackup changed shape. Bump BackupData.version and " +
                "MAX_SUPPORTED_BACKUP_VERSION together, then update this count.",
            SETTINGS_BACKUP_FIELDS,
            SettingsBackup::class.primaryConstructor!!.parameters.size
        )
    }
}
