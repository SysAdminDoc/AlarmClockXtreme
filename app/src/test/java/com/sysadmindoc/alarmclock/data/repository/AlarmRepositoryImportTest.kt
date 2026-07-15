package com.sysadmindoc.alarmclock.data.repository

import com.sysadmindoc.alarmclock.data.local.AlarmDao
import com.sysadmindoc.alarmclock.data.model.Alarm
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRepositoryImportTest {
    @Test
    fun `migration batch is sanitized disabled and sent through one transaction`() = runTest {
        val dao = mockk<AlarmDao>()
        val captured = slot<List<Alarm>>()
        coEvery { dao.insertAllWithStableOrder(capture(captured)) } returns listOf(11L, 12L)
        val repository = AlarmRepository(dao)

        val ids = repository.importDisabledAtomically(
            listOf(
                Alarm(id = 99, hour = 6, isEnabled = true, nextTriggerTime = 123L),
                Alarm(id = 100, hour = 7, isEnabled = true, nextTriggerTime = 456L)
            )
        )

        assertEquals(listOf(11L, 12L), ids)
        assertEquals(2, captured.captured.size)
        captured.captured.forEach {
            assertEquals(0L, it.id)
            assertFalse(it.isEnabled)
            assertEquals(0L, it.nextTriggerTime)
        }
        coVerify(exactly = 1) { dao.insertAllWithStableOrder(any()) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `transaction failure is surfaced without falling back to partial row inserts`() = runTest {
        val dao = mockk<AlarmDao>()
        coEvery { dao.insertAllWithStableOrder(any()) } throws IllegalStateException("write failed")
        val repository = AlarmRepository(dao)

        var failed = false
        try {
            repository.importDisabledAtomically(listOf(Alarm()))
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue(failed)
        coVerify(exactly = 1) { dao.insertAllWithStableOrder(any()) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }
}
