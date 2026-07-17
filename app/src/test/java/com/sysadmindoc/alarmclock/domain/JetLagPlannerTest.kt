package com.sysadmindoc.alarmclock.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JetLagPlannerTest {
    @Test
    fun autoChoosesEarlierShiftWhenTargetIsClosestByAdvance() {
        val plan = JetLagPlanner.plan(
            currentWakeMinutes = 7 * 60,
            targetWakeMinutes = 5 * 60,
            sleepGoalMinutes = 8 * 60,
            adjustmentDays = 4,
            direction = JetLagDirection.AUTO
        )

        assertEquals(JetLagDirection.ADVANCE, plan.resolvedDirection)
        assertEquals(-120, plan.totalShiftMinutes)
        assertEquals(-30, plan.dailyShiftMinutes)
        assertEquals(listOf(390, 360, 330, 300), plan.days.map { it.wakeMinutes })
        assertEquals(21 * 60, plan.days.last().bedtimeMinutes)
        assertEquals(330, plan.days.last().brightLightStartMinutes)
        assertEquals(450, plan.days.last().brightLightEndMinutes)
    }

    @Test
    fun delayShiftWrapsCleanlyAcrossMidnight() {
        val plan = JetLagPlanner.plan(
            currentWakeMinutes = 23 * 60,
            targetWakeMinutes = 2 * 60,
            sleepGoalMinutes = 7 * 60,
            adjustmentDays = 3,
            direction = JetLagDirection.DELAY
        )

        assertEquals(180, plan.totalShiftMinutes)
        assertEquals(60, plan.dailyShiftMinutes)
        assertEquals(listOf(0, 60, 120), plan.days.map { it.wakeMinutes })
        assertEquals(19 * 60, plan.days.last().bedtimeMinutes)
        assertEquals(15 * 60, plan.days.last().brightLightStartMinutes)
        assertEquals(17 * 60, plan.days.last().brightLightEndMinutes)
        assertEquals(120, plan.days.last().dimLightStartMinutes)
        assertEquals(240, plan.days.last().dimLightEndMinutes)
    }

    @Test
    fun clampsInputsToUsableBounds() {
        val plan = JetLagPlanner.plan(
            currentWakeMinutes = -30,
            targetWakeMinutes = 3_000,
            sleepGoalMinutes = 20 * 60,
            adjustmentDays = 30,
            direction = JetLagDirection.ADVANCE
        )

        assertEquals(1_410, plan.currentWakeMinutes)
        assertEquals(120, plan.targetWakeMinutes)
        assertEquals(16 * 60, plan.sleepGoalMinutes)
        assertEquals(14, plan.adjustmentDays)
        assertEquals(14, plan.days.size)
    }

    @Test
    fun alignedWakeTimesReturnStableNoShiftPlan() {
        val plan = JetLagPlanner.plan(
            currentWakeMinutes = 7 * 60,
            targetWakeMinutes = 7 * 60,
            sleepGoalMinutes = 8 * 60,
            adjustmentDays = 5,
            direction = JetLagDirection.AUTO
        )

        assertTrue(plan.alreadyAligned)
        assertEquals(0, plan.totalShiftMinutes)
        assertEquals(0, plan.dailyShiftMinutes)
        assertEquals(List(5) { 7 * 60 }, plan.days.map { it.wakeMinutes })
    }

    @Test
    fun shiftLabelsStayCompact() {
        assertEquals("2h 15m earlier", formatJetLagShift(-135))
        assertEquals("45m later", formatJetLagShift(45))
        assertEquals("No shift", formatJetLagShift(0))
    }
}
