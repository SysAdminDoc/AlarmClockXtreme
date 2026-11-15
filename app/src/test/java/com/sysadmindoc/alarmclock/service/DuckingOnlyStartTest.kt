package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertEquals
import org.junit.Test

class DuckingOnlyStartTest {

    private val stopped = mutableListOf<Int>()
    private val start = DuckingOnlyStart { stopped += it }

    @Test
    fun `an un-duck that arrives after dismiss stops the instance it created`() {
        start.onCommand(isDuckingCommand = true)
        start.stopIfOrphaned(1)
        assertEquals(listOf(1), stopped)
    }

    @Test
    fun `a stale duck during a ring leaves the service running`() {
        start.onCommand(isDuckingCommand = false)
        start.onCommand(isDuckingCommand = true)
        start.stopIfOrphaned(2)
        assertEquals(emptyList<Int>(), stopped)
    }

    @Test
    fun `an alarm start after the orphan duck keeps the instance`() {
        start.onCommand(isDuckingCommand = true)
        start.onCommand(isDuckingCommand = false)
        start.onCommand(isDuckingCommand = true)
        start.stopIfOrphaned(3)
        assertEquals(emptyList<Int>(), stopped)
    }
}
