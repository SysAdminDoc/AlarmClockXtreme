package com.sysadmindoc.alarmclock.data.support

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLogExportFormatterTest {
    @Test
    fun formatScrubsCrashLogContentsAndLabelsEachFile() {
        val export = CrashLogExportFormatter.format(
            listOf(
                CrashLogExportEntry(
                    fileName = "crash_2026-08-12.txt",
                    text = "Request failed at https://private.example.test/api with token=secret"
                ),
                CrashLogExportEntry(
                    fileName = "crash_2026-08-11.txt",
                    text = "Second crash"
                )
            )
        )

        assertTrue(export.contains("===== crash_2026-08-12.txt ====="))
        assertTrue(export.contains("===== crash_2026-08-11.txt ====="))
        assertTrue(export.contains("[URL_REDACTED]"))
        assertTrue(export.contains("[SECRET_REDACTED]"))
        assertFalse(export.contains("private.example.test"))
        assertFalse(export.contains("token=secret"))
    }

    @Test
    fun formatExplainsWhenNoCrashLogsExist() {
        assertEquals(
            "No local crash logs were present.\n",
            CrashLogExportFormatter.format(emptyList())
        )
    }
}
