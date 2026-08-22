package com.sysadmindoc.alarmclock.domain

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drift guard for the exact-alarm capability on Android 12 / 12L.
 *
 * USE_EXACT_ALARM only exists from API 33. On API 31/32 the platform gates
 * AlarmManager.canScheduleExactAlarms() on SCHEDULE_EXACT_ALARM, and an app
 * that never declares it is not even listed under Settings > Alarms &
 * reminders, so the user cannot grant it. Every AlarmScheduler entry point
 * bails out when canScheduleExactAlarms() is false, so dropping this
 * declaration silently stops every alarm on 12/12L.
 */
class ExactAlarmPermissionManifestTest {
    @Test
    fun `manifest declares SCHEDULE_EXACT_ALARM capped at API 32`() {
        val manifest = manifestFile().readText().replace("\r\n", "\n")
        val declaration = Regex(
            "<uses-permission\\s+[^>]*android:name=\"android\\.permission\\.SCHEDULE_EXACT_ALARM\"[^>]*>",
            RegexOption.DOT_MATCHES_ALL
        ).find(manifest)?.value

        assertTrue(
            "SCHEDULE_EXACT_ALARM must be declared or no alarm can ever arm on API 31/32",
            declaration != null
        )
        assertTrue(
            "SCHEDULE_EXACT_ALARM must be capped with maxSdkVersion=\"32\" so USE_EXACT_ALARM " +
                "governs API 33+ (Play policy)",
            declaration!!.contains("android:maxSdkVersion=\"32\"")
        )
    }

    @Test
    fun `manifest still declares USE_EXACT_ALARM for API 33 and up`() {
        val manifest = manifestFile().readText()
        assertTrue(manifest.contains("android.permission.USE_EXACT_ALARM"))
    }

    private fun manifestFile(): File {
        val fromRoot = File("app/src/main/AndroidManifest.xml")
        return if (fromRoot.isFile) fromRoot else File("src/main/AndroidManifest.xml")
            .also { check(it.isFile) { "Missing manifest: ${it.absolutePath}" } }
    }
}
