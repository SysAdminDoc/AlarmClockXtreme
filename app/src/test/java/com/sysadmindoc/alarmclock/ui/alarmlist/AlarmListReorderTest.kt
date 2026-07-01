package com.sysadmindoc.alarmclock.ui.alarmlist

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmListReorderTest {

    @Test
    fun manualSortUsesPersistedOrderBeforeClockTime() {
        val alarms = listOf(
            Alarm(id = 1L, hour = 9, minute = 0, sortOrder = 3_000),
            Alarm(id = 2L, hour = 7, minute = 30, sortOrder = 1_000),
            Alarm(id = 3L, hour = 6, minute = 45, sortOrder = 2_000),
            Alarm(id = 4L, hour = 5, minute = 15, sortOrder = 2_000)
        )

        val sorted = sortAlarmsForList(alarms, AlarmSortOrder.MANUAL)

        assertEquals(listOf(2L, 4L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun reorderAlarmIdsMovesDraggedIdBeforeTarget() {
        val visibleIds = listOf(10L, 20L, 30L, 40L)

        assertEquals(
            listOf(10L, 30L, 20L, 40L),
            reorderAlarmIds(visibleIds, movedId = 30L, targetId = 20L)
        )
        assertEquals(
            listOf(10L, 30L, 40L, 20L),
            reorderAlarmIds(visibleIds, movedId = 20L, targetId = 40L)
        )
    }

    @Test
    fun reorderAlarmIdsIgnoresMissingIdsAndNoOps() {
        val visibleIds = listOf(10L, 20L, 30L)

        assertEquals(visibleIds, reorderAlarmIds(visibleIds, movedId = 99L, targetId = 20L))
        assertEquals(visibleIds, reorderAlarmIds(visibleIds, movedId = 20L, targetId = 99L))
        assertEquals(visibleIds, reorderAlarmIds(visibleIds, movedId = 20L, targetId = 20L))
    }
}
