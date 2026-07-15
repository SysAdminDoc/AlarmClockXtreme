package com.sysadmindoc.alarmclock.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LazyScreenArchitectureTest {
    @Test
    fun `settings content is split into stable keyed lazy items`() {
        val source = sourceFile("ui/settings/SettingsScreen.kt").readText()
        val content = source.substringAfter("val settingsContent:")
            .substringBefore("\n        if (useTwoPane) {\n            Row(")

        assertTrue(content.contains("LazyColumn("))
        assertTrue(content.contains("state = settingsListState"))
        assertTrue(content.countOccurrences("settingsItem(\"") >= 20)
        assertFalse(content.contains(".verticalScroll("))
    }

    @Test
    fun `alarm editor registers stable keyed page sections in a lazy column`() {
        val source = sourceFile("ui/alarmedit/AlarmEditScreen.kt").readText()
        val content = source.substringAfter(") { padding ->")
            .substringBefore("// Time Picker Dialog")

        assertTrue(content.contains("LazyColumn("))
        assertTrue(content.contains("state = editorScrollState"))
        assertTrue(source.contains("if (section.page != activePage) return"))
        assertTrue(source.contains("item(key = \"alarm-editor-\${section.name.lowercase()}\")"))
        assertFalse(content.contains(".verticalScroll(editorScrollState)"))
    }

    private fun sourceFile(relativePath: String): File {
        val fromRoot = File("app/src/main/java/com/sysadmindoc/alarmclock/$relativePath")
        return if (fromRoot.isFile) {
            fromRoot
        } else {
            File("src/main/java/com/sysadmindoc/alarmclock/$relativePath")
        }.also { check(it.isFile) { "Missing source file: ${it.absolutePath}" } }
    }

    private fun String.countOccurrences(token: String): Int =
        windowed(token.length).count { it == token }
}
