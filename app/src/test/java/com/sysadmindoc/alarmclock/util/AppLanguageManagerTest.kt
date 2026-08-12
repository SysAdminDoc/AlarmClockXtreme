package com.sysadmindoc.alarmclock.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageManagerTest {
    @Test
    fun emptyLanguageTagsUseSystemDefault() {
        assertEquals(
            AppLanguageOption.SYSTEM_DEFAULT,
            AppLanguageManager.optionForLanguageTags("")
        )
    }

    @Test
    fun englishLanguageTagsSelectEnglish() {
        assertEquals(
            AppLanguageOption.ENGLISH,
            AppLanguageManager.optionForLanguageTags("en-US,en")
        )
    }

    @Test
    fun unsupportedLanguageFallsBackToSystemDefault() {
        assertEquals(
            AppLanguageOption.SYSTEM_DEFAULT,
            AppLanguageManager.optionForLanguageTags("fr")
        )
    }
}
