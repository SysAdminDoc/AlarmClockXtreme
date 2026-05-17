package com.sysadmindoc.alarmclock.data.support

import android.Manifest
import android.app.AlarmManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.util.CrashLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class SupportExportFile(
    val uri: Uri,
    val fileName: String,
    val mimeType: String = "application/zip"
)

@Singleton
class SupportExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val alarmEventRepository: AlarmEventRepository
) {
    suspend fun createSupportExport(): SupportExportFile {
        val generatedAt = Instant.now()
        val exportDir = File(context.cacheDir, EXPORT_DIR_NAME).apply { mkdirs() }
        exportDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(FILE_PREFIX) && it.extension == "zip" }
            ?.forEach { runCatching { it.delete() } }

        val fileName = "$FILE_PREFIX-${FILE_TIMESTAMP.format(generatedAt)}.zip"
        val zipFile = File(exportDir, fileName)
        val alarms = alarmRepository.getAll().map(SupportAlarmDiagnostic::from)
        val enabledCount = alarms.count { it.enabled }
        val nextTrigger = alarms
            .filter { it.enabled && it.nextTriggerTime > 0L }
            .minOfOrNull { it.nextTriggerTime }
        val stats = alarmEventRepository.getStats()
        val crashLogs = CrashLogger.getLogFiles(context).take(MAX_CRASH_LOGS)

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zip.writeTextEntry(
                "diagnostics.txt",
                SupportDiagnosticsFormatter.diagnosticsText(
                    generatedAt = generatedAt,
                    appVersion = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    flavor = BuildConfig.FLAVOR,
                    buildType = BuildConfig.BUILD_TYPE,
                    packageName = context.packageName,
                    deviceManufacturer = Build.MANUFACTURER,
                    deviceModel = Build.MODEL,
                    androidRelease = Build.VERSION.RELEASE,
                    sdkInt = Build.VERSION.SDK_INT,
                    notificationPermissionGranted = hasNotificationPermission(),
                    exactAlarmsAllowed = canScheduleExactAlarms(),
                    ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(),
                    appStandbyBucket = appStandbyBucketLabel(),
                    totalAlarms = alarms.size,
                    enabledAlarms = enabledCount,
                    nextTriggerTime = nextTrigger,
                    crashLogCount = crashLogs.size,
                    stats = stats
                )
            )
            zip.writeTextEntry("alarms_redacted.csv", SupportDiagnosticsFormatter.alarmCsv(alarms))
            if (crashLogs.isEmpty()) {
                zip.writeTextEntry("crash_logs/README.txt", "No local crash logs were present.\n")
            } else {
                crashLogs.forEach { file ->
                    zip.writeTextEntry(
                        name = "crash_logs/${file.name}",
                        text = runCatching { file.readText() }
                            .getOrElse { "Unable to read crash log: ${it.message}\n" }
                    )
                }
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
        return SupportExportFile(uri = uri, fileName = fileName)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    private fun appStandbyBucketLabel(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return "unavailable"
        val bucket = runCatching {
            (context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)
                ?.appStandbyBucket
        }.getOrNull() ?: return "unknown"
        val label = when (bucket) {
            UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "ACTIVE"
            UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "WORKING_SET"
            UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "FREQUENT"
            UsageStatsManager.STANDBY_BUCKET_RARE -> "RARE"
            UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "RESTRICTED"
            else -> "UNKNOWN"
        }
        return "$label ($bucket)"
    }

    private fun ZipOutputStream.writeTextEntry(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private companion object {
        const val EXPORT_DIR_NAME = "support_exports"
        const val FILE_PREFIX = "alarmclockxtreme-support"
        const val MAX_CRASH_LOGS = 10
        val FILE_TIMESTAMP: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss", Locale.US)
            .withZone(java.time.ZoneOffset.UTC)
    }
}
