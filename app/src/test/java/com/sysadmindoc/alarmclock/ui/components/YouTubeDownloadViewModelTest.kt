package com.sysadmindoc.alarmclock.ui.components

import com.sysadmindoc.alarmclock.service.YouTubeAudioDownloader
import com.sysadmindoc.alarmclock.service.YouTubeEngineUpdateResult
import com.sysadmindoc.alarmclock.service.YouTubeEngineUpdateState
import com.sysadmindoc.alarmclock.service.YouTubeSearchHit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The download used to run on the dialog's own coroutine scope, so rotating the
 * phone cancelled it silently. These pin the two properties that fix depends
 * on: the job belongs to the ViewModel, and its result waits to be collected
 * rather than being emitted into a gap.
 */
class YouTubeDownloadViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeDownloader : YouTubeAudioDownloader {
        val pending = CompletableDeferred<Result<String>>()
        var downloadCalls = 0
        var lastUrl: String? = null
        var engineResult: Result<YouTubeEngineUpdateResult> =
            Result.failure(IllegalStateException("not configured"))

        override fun engineVersionName(): String = "2026.01.01"

        override suspend fun updateEngine(): Result<YouTubeEngineUpdateResult> = engineResult

        override suspend fun downloadAsAlarm(youtubeUrl: String, displayName: String): Result<String> {
            downloadCalls++
            lastUrl = youtubeUrl
            return pending.await()
        }

        override fun isAvailable(): Boolean = true

        override suspend fun searchAlarmSounds(
            query: String,
            maxDurationSeconds: Int
        ): Result<List<YouTubeSearchHit>> = Result.success(emptyList())

        override suspend fun getPreviewStreamUrl(youtubeUrl: String): Result<String> =
            Result.success("https://stream.example/audio")
    }

    @Test
    fun `a finished download waits in state until something collects it`() = runTest {
        val downloader = FakeDownloader()
        val viewModel = YouTubeDownloadViewModel(downloader)

        viewModel.download("https://youtube.com/watch?v=abc", "Rooster")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue("the job should be running", viewModel.downloading.value)

        // Nothing is observing at this moment, which is exactly what a rotation
        // looks like from here.
        downloader.pending.complete(Result.success("Rooster crow"))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.downloading.value)
        assertEquals(
            YouTubeDownloadViewModel.Outcome.Downloaded("Rooster crow"),
            viewModel.outcome.value
        )

        viewModel.consumeOutcome()
        assertNull("consuming twice would re-report the same download", viewModel.outcome.value)
    }

    @Test
    fun `a second tap while one download runs does not start a competing job`() = runTest {
        val downloader = FakeDownloader()
        val viewModel = YouTubeDownloadViewModel(downloader)

        viewModel.download("https://youtube.com/watch?v=first", "First")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.download("https://youtube.com/watch?v=second", "Second")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, downloader.downloadCalls)
        assertEquals("https://youtube.com/watch?v=first", downloader.lastUrl)
    }

    @Test
    fun `a failure carries the action it came from so the copy can match`() = runTest {
        val downloader = FakeDownloader()
        val viewModel = YouTubeDownloadViewModel(downloader)

        viewModel.download("https://youtube.com/watch?v=abc", "Rooster")
        dispatcher.scheduler.advanceUntilIdle()
        downloader.pending.complete(Result.failure(IllegalStateException("HTTP 403")))
        dispatcher.scheduler.advanceUntilIdle()

        val outcome = viewModel.outcome.value as YouTubeDownloadViewModel.Outcome.Failed
        assertEquals(YouTubeDialogAction.Download, outcome.action)
    }

    @Test
    fun `a successful engine update reports the new version, not an outcome`() = runTest {
        val downloader = FakeDownloader()
        downloader.engineResult = Result.success(
            YouTubeEngineUpdateResult(
                state = YouTubeEngineUpdateState.Updated,
                beforeVersionName = "2026.01.01",
                afterVersionName = "2026.07.04"
            )
        )
        val viewModel = YouTubeDownloadViewModel(downloader)

        viewModel.updateEngine()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026.07.04", viewModel.engineVersion.value)
        assertNull("an engine update is not a download result", viewModel.outcome.value)
        assertFalse(viewModel.updatingEngine.value)
    }
}
