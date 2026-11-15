package com.sysadmindoc.alarmclock.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RedeliveredStartTest {

    private val released = mutableListOf<Int>()
    private val start = RedeliveredStart { released += it }

    @Test
    fun `a newer alarm releases the one it took over from`() {
        start.hold(1)
        start.hold(4)
        assertEquals(listOf(1), released)
    }

    @Test
    fun `snooze or dismiss releases the held start once`() {
        start.hold(2)
        start.releaseHeld()
        start.releaseHeld()
        assertEquals(listOf(2), released)
    }

    @Test
    fun `nothing held means nothing released`() {
        start.releaseHeld()
        assertEquals(emptyList<Int>(), released)
    }
}
