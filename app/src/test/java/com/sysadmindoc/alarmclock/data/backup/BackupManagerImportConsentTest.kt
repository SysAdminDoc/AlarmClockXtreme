package com.sysadmindoc.alarmclock.data.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A backup file is untrusted input. It can arrive from a forum post or a chat
 * message, and it carries a webhook endpoint, a Hue key and a phone number that
 * gets texted and called after a missed alarm. None of that may be installed
 * without the user saying so.
 */
@RunWith(RobolectricTestRunner::class)
class BackupManagerImportConsentTest {
    private lateinit var context: Context
    private lateinit var repository: AlarmRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var scheduler: AlarmScheduler
    private lateinit var backupManager: BackupManager

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
        coEvery { repository.getAll() } returns emptyList()
        coEvery { repository.save(any()) } returns 1L
    }

    private fun hostileSettings() = AppSettings(
        webhookEnabled = true,
        webhookUrl = "https://collector.example.net/hook",
        webhookSigningSecret = "s3cret",
        hueBridgeIp = "10.0.0.9",
        hueApiKey = "hue-key",
        googleRoutesApiKey = "routes-key",
        is24HourFormat = true
    )

    /**
     * Builds a backup file the way an attacker would: a real export, produced
     * from settings that point at their infrastructure.
     */
    private suspend fun writeBackup(name: String = "consent-backup.json"): Uri {
        coEvery { repository.getAll() } returns listOf(
            Alarm(
                id = 5L,
                hour = 6,
                minute = 30,
                guardianEnabled = true,
                guardianPhone = "+15550002222"
            )
        )
        coEvery { preferencesManager.getCurrentSettings() } returns hostileSettings()
        val json = backupManager.export()
        coEvery { repository.getAll() } returns emptyList()
        val file = File(context.cacheDir, name).apply { writeText(json) }
        return Uri.fromFile(file)
    }

    private suspend fun importedSettings(options: BackupImportOptions): AppSettings? {
        val transform = slot<(AppSettings) -> AppSettings>()
        coEvery { preferencesManager.update(capture(transform)) } returns Unit
        val result = backupManager.importFromUri(writeBackup(), options)
        assertTrue(result.exceptionOrNull()?.message ?: "import failed", result.isSuccess)
        return if (transform.isCaptured) transform.captured(AppSettings()) else null
    }

    @Test
    fun `by default an imported webhook and guardian contact are not installed`() = runTest {
        val applied = importedSettings(BackupImportOptions())

        assertTrue("Settings should still be applied", applied != null)
        assertFalse(applied!!.webhookEnabled)
        assertEquals("", applied.webhookUrl)
        assertEquals("", applied.webhookSigningSecret)
        assertEquals("", applied.hueApiKey)
        assertEquals("", applied.hueBridgeIp)
        assertEquals("", applied.googleRoutesApiKey)
        // Harmless preferences still restore.
        assertTrue(applied.is24HourFormat)
    }

    @Test
    fun `opting in keeps the integrations the file carried`() = runTest {
        val applied = importedSettings(
            BackupImportOptions(keepIntegrationsAndContacts = true)
        )

        assertTrue(applied!!.webhookEnabled)
        assertEquals("https://collector.example.net/hook", applied.webhookUrl)
    }

    @Test
    fun `declining settings leaves the current ones untouched`() = runTest {
        val applied = importedSettings(BackupImportOptions(importSettings = false))

        assertTrue("No settings write should happen at all", applied == null)
    }

    @Test
    fun `per-alarm guardian escalation is stripped unless integrations are kept`() = runTest {
        val saved = slot<Alarm>()
        coEvery { repository.save(capture(saved)) } returns 1L

        backupManager.importFromUri(writeBackup(), BackupImportOptions())

        assertFalse(saved.captured.guardianEnabled)
        assertEquals("", saved.captured.guardianPhone)
    }

    @Test
    fun `the preview names the endpoint and the phone number`() = runTest {
        val preview = backupManager.inspectImportFromUri(writeBackup()).getOrThrow()

        assertTrue(
            "Preview should name the webhook host: ${preview.riskyImportValues}",
            preview.riskyImportValues.any { it.contains("collector.example.net") }
        )
        assertTrue(
            "Preview should name the per-alarm guardian number: ${preview.riskyImportValues}",
            preview.riskyImportValues.any { it.contains("+15550002222") }
        )
    }

    @Test
    fun `an oversized file is rejected instead of being read into memory`() = runTest {
        val file = File(context.cacheDir, "huge-backup.json").apply {
            writeText("{\"padding\":\"" + "x".repeat(BackupManager.MAX_IMPORT_CHARS + 16) + "\"}")
        }

        val result = backupManager.importFromUri(Uri.fromFile(file), BackupImportOptions())

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message.orEmpty().contains("too large")
        )
    }

    @Test
    fun `replace inserts before it deletes and keeps alarms when nothing imports`() = runTest {
        val existing = Alarm(id = 44L, hour = 5, minute = 0, label = "Keep me")
        coEvery { repository.getAll() } returns listOf(existing)
        val order = mutableListOf<String>()
        coEvery { repository.save(any()) } coAnswers { order += "save"; 101L }
        coEvery { repository.deleteById(any()) } coAnswers { order += "delete" }

        val uri = writeBackup(name = "replace-order.json")
        coEvery { repository.getAll() } returns listOf(existing)
        val result = backupManager.importFromUri(
            uri,
            BackupImportOptions(mode = BackupImportMode.Replace)
        )

        assertTrue(result.isSuccess)
        assertEquals(
            "The old rows must not be removed until the new ones are on disk",
            listOf("save", "delete"),
            order
        )
    }

    @Test
    fun `replace keeps the existing alarms when the file has none`() = runTest {
        val existing = Alarm(id = 44L, hour = 5, minute = 0, label = "Keep me")
        coEvery { repository.getAll() } returns emptyList()
        coEvery { preferencesManager.getCurrentSettings() } returns AppSettings()
        val emptyJson = backupManager.export()
        val file = File(context.cacheDir, "empty-backup.json").apply { writeText(emptyJson) }

        coEvery { repository.getAll() } returns listOf(existing)
        val result = backupManager.importFromUri(
            Uri.fromFile(file),
            BackupImportOptions(mode = BackupImportMode.Replace)
        )

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
        coVerify(exactly = 0) { repository.deleteById(any()) }
    }
}
