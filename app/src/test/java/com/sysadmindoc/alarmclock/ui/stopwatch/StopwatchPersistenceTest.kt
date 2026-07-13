package com.sysadmindoc.alarmclock.ui.stopwatch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The stopwatch used to keep its state only in memory, so a running watch and
 * its laps vanished on process death (common overnight). These verify the
 * state survives a simulated process restart (a fresh ViewModel over the same
 * app context).
 */
@RunWith(RobolectricTestRunner::class)
class StopwatchPersistenceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun clearPrefs() {
        context.getSharedPreferences("stopwatch_state", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `paused stopwatch with laps survives a process restart`() {
        val first = StopwatchViewModel(context)
        first.start()
        first.lap()
        first.lap()
        first.pause()
        val lapCount = first.uiState.value.laps.size

        // Simulate process death + recreation: a brand-new ViewModel instance.
        val restored = StopwatchViewModel(context)
        assertEquals(StopwatchState.PAUSED, restored.uiState.value.state)
        assertEquals(lapCount, restored.uiState.value.laps.size)
    }

    @Test
    fun `reset clears persisted state so a restart starts idle`() {
        val first = StopwatchViewModel(context)
        first.start()
        first.pause()
        first.reset()

        val restored = StopwatchViewModel(context)
        assertEquals(StopwatchState.IDLE, restored.uiState.value.state)
        assertEquals(0, restored.uiState.value.laps.size)
    }
}
