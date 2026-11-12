package com.sysadmindoc.alarmclock.ui.alarmedit

import android.content.Context
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every editor page hands LazyColumn its item keys, and a repeated key is not
 * a cosmetic problem: LazyColumn throws the moment the page is composed. That
 * is issue #59, where the dismiss-action card reused the Spotify section and
 * tapping "Integrations & safety" crashed the app on both flavors.
 *
 * The page builders only register items here, nothing is composed, so the
 * view model and context are never touched.
 */
class AlarmEditorPageKeysTest {

    private class RecordingScope : LazyListScope {
        val keys = mutableListOf<Any>()

        override fun item(
            key: Any?,
            contentType: Any?,
            content: @Composable LazyItemScope.() -> Unit
        ) {
            key?.let(keys::add)
        }

        override fun items(
            count: Int,
            key: ((index: Int) -> Any)?,
            contentType: (index: Int) -> Any?,
            itemContent: @Composable LazyItemScope.(index: Int) -> Unit
        ) {
            if (key != null) repeat(count) { keys += key(it) }
        }
    }

    private fun keysFor(page: AlarmEditorPage): List<Any> {
        val scope = RecordingScope()
        val state = AlarmEditUiState()
        val viewModel = mockk<AlarmEditViewModel>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        // Mirrors the page dispatch in AlarmEditScreen.
        with(scope) {
            when (page) {
                AlarmEditorPage.OVERVIEW -> alarmEditOverviewSections(page, state, viewModel, {}, {})
                AlarmEditorPage.SOUND -> alarmEditSoundSections(page, state, viewModel, {})
                AlarmEditorPage.DISMISS ->
                    alarmEditDismissSections(page, state, viewModel, context, {}, "", {}, "", {})
                AlarmEditorPage.SCHEDULE -> alarmEditScheduleSections(page, state, viewModel)
                AlarmEditorPage.WAKE -> alarmEditWakeSections(page, state, viewModel, {}, "")
                AlarmEditorPage.INTEGRATIONS -> alarmEditIntegrationSections(page, state, viewModel, context)
                AlarmEditorPage.ADVANCED -> alarmEditAdvancedSection(page, state, viewModel)
            }
        }
        return scope.keys
    }

    @Test
    fun everyEditorPageRegistersUniqueLazyKeys() {
        AlarmEditorPage.entries.forEach { page ->
            val keys = keysFor(page)
            val repeated = keys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            assertEquals("$page repeats LazyColumn keys", emptySet<Any>(), repeated)
        }
    }

    @Test
    fun integrationsPageShowsTheDismissActionUnderItsOwnSection() {
        val keys = keysFor(AlarmEditorPage.INTEGRATIONS)
        assertTrue(keys.contains("alarm-editor-spotify"))
        assertTrue(keys.contains("alarm-editor-dismiss_action"))
    }
}
