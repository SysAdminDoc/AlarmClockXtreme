package com.sysadmindoc.alarmclock.ui.alarmlist

import android.content.Context
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmEventRepository
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.domain.NextAlarmCalculator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BulkDeleteUndoTest {

    private val dispatcher = StandardTestDispatcher()

    /** Stands in for the Room table: save() keeps a nonzero id, like the REPLACE insert does. */
    private val store = linkedMapOf<Long, Alarm>()
    private val saves = mutableListOf<Alarm>()

    private val repository = mockk<AlarmRepository>(relaxed = true).also { repo ->
        coEvery { repo.getById(any()) } answers { store[firstArg()] }
        coEvery { repo.getAll() } answers { store.values.toList() }
        // Suspends after removing, the way Room's DAO hops threads, so two deletes can interleave.
        coEvery { repo.delete(any()) } coAnswers { store.remove(firstArg<Alarm>().id); delay(10); Unit }
        coEvery { repo.save(any()) } answers {
            val alarm = firstArg<Alarm>()
            saves += alarm
            store[alarm.id] = alarm
            alarm.id
        }
    }
    private val scheduler = mockk<AlarmScheduler>(relaxed = true)

    private val future = System.currentTimeMillis() + 3_600_000L
    private val upcoming = Alarm(id = 11, hour = 6, minute = 30, isEnabled = true, nextTriggerTime = future)
    private val overdue = Alarm(id = 12, hour = 7, minute = 0, isEnabled = true, nextTriggerTime = 0L)
    private val off = Alarm(id = 13, hour = 8, minute = 15, isEnabled = false, nextTriggerTime = 0L)
    private val untouched = Alarm(id = 14, hour = 9, minute = 0, isEnabled = true, nextTriggerTime = future)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        listOf(upcoming, overdue, off, untouched).forEach { store[it.id] = it }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AlarmListViewModel(
        context = mockk<Context>(relaxed = true),
        repository = repository,
        scheduler = scheduler,
        calculator = mockk<NextAlarmCalculator>(relaxed = true),
        preferencesManager = mockk<PreferencesManager>(relaxed = true),
        eventRepository = mockk<AlarmEventRepository>(relaxed = true)
    )

    @Test
    fun `one undo restores every alarm a bulk delete removed, under the same ids`() = runTest(dispatcher) {
        val vm = viewModel()
        val deleted = listOf(upcoming, overdue, off)
        vm.selectMany(deleted.map { it.id }.toSet())

        vm.deleteSelected()
        advanceUntilIdle()

        assertEquals(listOf(untouched.id), store.keys.toList())
        deleted.forEach { verify { scheduler.cancel(it.id) } }

        vm.undoDelete()
        advanceUntilIdle()

        assertEquals(deleted.toSet(), saves.toSet())
        assertEquals(deleted.associateBy { it.id } + (untouched.id to untouched), store.toMap())
        coVerify(exactly = 1) { scheduler.scheduleAt(upcoming, future, any()) }
        coVerify(exactly = 1) { scheduler.schedule(overdue, any(), any()) }
        coVerify(exactly = 0) { scheduler.schedule(off, any(), any()) }
        coVerify(exactly = 0) { scheduler.scheduleAt(off, any(), any()) }
    }

    @Test
    fun `a second undo restores nothing`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.selectMany(setOf(upcoming.id, overdue.id))
        vm.deleteSelected()
        advanceUntilIdle()

        vm.undoDelete()
        advanceUntilIdle()
        val savesAfterFirstUndo = saves.toList()

        vm.undoDelete()
        advanceUntilIdle()

        assertEquals(savesAfterFirstUndo, saves)
        assertEquals(2, saves.size)
        coVerify(exactly = 1) { scheduler.scheduleAt(upcoming, future, any()) }
        coVerify(exactly = 1) { scheduler.schedule(overdue, any(), any()) }
    }

    @Test
    fun `a second tap on Delete while the first is running keeps the whole set restorable`() = runTest(dispatcher) {
        val vm = viewModel()
        val deleted = listOf(upcoming, overdue, off)
        vm.selectMany(deleted.map { it.id }.toSet())

        vm.deleteSelected()
        advanceTimeBy(15)
        vm.deleteSelected()
        advanceUntilIdle()
        vm.undoDelete()
        advanceUntilIdle()

        assertEquals(deleted.toSet(), saves.toSet())
        assertEquals(4, store.size)
    }

    @Test
    fun `a bulk delete keeps an earlier swipe delete restorable`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.deleteAlarm(untouched)
        advanceUntilIdle()
        vm.selectMany(setOf(upcoming.id, off.id))
        vm.deleteSelected()
        advanceUntilIdle()

        vm.undoDelete()
        advanceUntilIdle()

        assertEquals(setOf(untouched, upcoming, off), saves.toSet())
        assertEquals(4, store.size)
    }

    @Test
    fun `dismissing the snackbar makes the bulk delete final`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.selectMany(setOf(upcoming.id, off.id))
        vm.deleteSelected()
        advanceUntilIdle()

        vm.confirmDelete()
        vm.undoDelete()
        advanceUntilIdle()

        assertTrue(saves.isEmpty())
        assertEquals(setOf(overdue.id, untouched.id), store.keys)
    }
}
