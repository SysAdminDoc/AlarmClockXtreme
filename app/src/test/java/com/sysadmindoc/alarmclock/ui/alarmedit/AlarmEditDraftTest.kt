package com.sysadmindoc.alarmclock.ui.alarmedit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmEditDraftTest {

    @Test
    fun `user-editable fields make a loaded draft dirty`() {
        val loaded = AlarmEditUiState(hour = 7, minute = 30, label = "Work")

        assertTrue(loaded.copy(label = "Gym").hasDraftChangesFrom(loaded))
        assertTrue(loaded.copy(repeatDays = setOf(java.time.DayOfWeek.MONDAY)).hasDraftChangesFrom(loaded))
        assertFalse(loaded.copy(label = "Gym").copy(label = "Work").hasDraftChangesFrom(loaded))
    }

    @Test
    fun `transient editor state does not make a draft dirty`() {
        val loaded = AlarmEditUiState(hour = 7, minute = 30, label = "Work")
        val transient = loaded.copy(
            isSaving = true,
            saveError = "Retry",
            notFound = true,
            forecastDates = listOf(ForecastEntry(123L)),
            hasUnsavedChanges = true
        )

        assertFalse(transient.hasDraftChangesFrom(loaded))
    }

    @Test
    fun `back navigation confirms only dirty drafts and stays during save`() {
        assertEquals(
            AlarmEditorExitDecision.NAVIGATE,
            alarmEditorExitDecision(hasUnsavedChanges = false, isSaving = false)
        )
        assertEquals(
            AlarmEditorExitDecision.CONFIRM_DISCARD,
            alarmEditorExitDecision(hasUnsavedChanges = true, isSaving = false)
        )
        assertEquals(
            AlarmEditorExitDecision.STAY,
            alarmEditorExitDecision(hasUnsavedChanges = true, isSaving = true)
        )
        assertEquals(
            AlarmEditorExitDecision.SHOW_OVERVIEW,
            alarmEditorExitDecision(
                hasUnsavedChanges = true,
                isSaving = false,
                page = AlarmEditorPage.SOUND
            )
        )
    }

    @Test
    fun `editor categories adapt to compact and wide layouts`() {
        assertEquals(1, alarmEditorCategoryColumns(360))
        assertEquals(1, alarmEditorCategoryColumns(719))
        assertEquals(2, alarmEditorCategoryColumns(720))
        assertEquals(2, alarmEditorCategoryColumns(840))
    }

    @Test
    fun `alarm numpad parses and validates 24 hour times`() {
        assertEquals(AlarmNumpadTime(7, 0), parseAlarmNumpadTime("0700", is24Hour = true, isPm = false))
        assertEquals(AlarmNumpadTime(23, 59), parseAlarmNumpadTime("2359", is24Hour = true, isPm = false))
        assertEquals(null, parseAlarmNumpadTime("2400", is24Hour = true, isPm = false))
        assertEquals(null, parseAlarmNumpadTime("1260", is24Hour = true, isPm = false))
        assertEquals(null, parseAlarmNumpadTime("700", is24Hour = true, isPm = false))
    }

    @Test
    fun `alarm numpad converts 12 hour periods without noon or midnight drift`() {
        assertEquals(AlarmNumpadTime(0, 0), parseAlarmNumpadTime("1200", is24Hour = false, isPm = false))
        assertEquals(AlarmNumpadTime(12, 0), parseAlarmNumpadTime("1200", is24Hour = false, isPm = true))
        assertEquals(AlarmNumpadTime(19, 30), parseAlarmNumpadTime("0730", is24Hour = false, isPm = true))
        assertEquals(null, parseAlarmNumpadTime("0000", is24Hour = false, isPm = false))
        assertEquals(null, parseAlarmNumpadTime("1300", is24Hour = false, isPm = true))
    }

    @Test
    fun `every settings section is assigned to a focused page`() {
        assertEquals(AlarmEditorPage.OVERVIEW, alarmEditorPageForSection("Label"))
        assertEquals(AlarmEditorPage.SOUND, alarmEditorPageForSection("Vibration"))
        assertEquals(AlarmEditorPage.DISMISS, alarmEditorPageForSection("Mission chaining"))
        assertEquals(AlarmEditorPage.SCHEDULE, alarmEditorPageForSection("Smart alarm"))
        assertEquals(AlarmEditorPage.WAKE, alarmEditorPageForSection("Morning routine"))
        assertEquals(AlarmEditorPage.INTEGRATIONS, alarmEditorPageForSection("Guardian Angel"))
        assertEquals(AlarmEditorPage.ADVANCED, alarmEditorPageForSection("Advanced"))
    }
}
