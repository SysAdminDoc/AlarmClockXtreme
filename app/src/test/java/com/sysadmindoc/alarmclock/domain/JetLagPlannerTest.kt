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
    fun autoChoosesDelayWhenTargetIsClosestByDelay() {
        // 07:00 -> 09:00: delay is +120, advance is -1320, so AUTO resolves DELAY.
        val plan = JetLagPlanner.plan(
            currentWakeMinutes = 7 * 60,
            targetWakeMinutes = 9 * 60,
            sleepGoalMinutes = 8 * 60,
            adjustmentDays = 4,
            direction = JetLagDirection.AUTO
        )

        assertEquals(JetLagDirection.DELAY, plan.resolvedDirection)
        assertEquals(120, plan.totalShiftMinutes)
        assertEquals(30, plan.dailyShiftMinutes)
        assertEquals(9 * 60, plan.days.last().wakeMinutes)
    }

    @Test
    fun autoBreaksTwelveHourTieTowardDelay() {
        // Exactly 12h apart: delay == advance == 720. `delay <= advance` picks DELAY.
        val plan = JetLagPlanner.plan(
            currentWakeMinutes = 0,
            targetWakeMinutes = 12 * 60,
            sleepGoalMinutes = 8 * 60,
            adjustmentDays = 6,
            direction = JetLagDirection.AUTO
        )

        assertEquals(JetLagDirection.DELAY, plan.resolvedDirection)
        assertEquals(720, plan.totalShiftMinutes)
        assertEquals(12 * 60, plan.days.last().wakeMinutes)
    }

    @Test
    fun requestedDirectionOverridesShorterAutoArc() {
        // Shorter arc for 07:00 -> 09:00 is DELAY (+120), but the user forces
        // ADVANCE, so the planner takes the long -1320 route instead.
        val plan = JetLagPlanner.plan(
            currentWakeMinutes = 7 * 60,
            targetWakeMinutes = 9 * 60,
            sleepGoalMinutes = 8 * 60,
            adjustmentDays = 3,
            direction = JetLagDirection.ADVANCE
        )

        assertEquals(JetLagDirection.ADVANCE, plan.resolvedDirection)
        assertEquals(-1_320, plan.totalShiftMinutes)
        assertEquals(9 * 60, plan.days.last().wakeMinutes)
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
}
