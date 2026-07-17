package com.sysadmindoc.alarmclock.domain

import kotlin.math.abs
import kotlin.math.roundToInt

enum class JetLagDirection(
    val storageKey: String,
    val label: String
) {
    AUTO("auto", "Auto"),
    ADVANCE("advance", "Earlier"),
    DELAY("delay", "Later");

    companion object {
        fun fromKey(key: String): JetLagDirection =
            entries.firstOrNull { it.storageKey == key.trim().lowercase() } ?: AUTO
    }
}

data class JetLagDayPlan(
    val dayNumber: Int,
    val wakeMinutes: Int,
    val bedtimeMinutes: Int,
    val brightLightStartMinutes: Int,
    val brightLightEndMinutes: Int,
    val dimLightStartMinutes: Int,
    val dimLightEndMinutes: Int
)

data class JetLagPlan(
    val currentWakeMinutes: Int,
    val targetWakeMinutes: Int,
    val sleepGoalMinutes: Int,
    val requestedDirection: JetLagDirection,
    val resolvedDirection: JetLagDirection,
    val adjustmentDays: Int,
    val totalShiftMinutes: Int,
    val dailyShiftMinutes: Int,
    val days: List<JetLagDayPlan>
) {
    val alreadyAligned: Boolean
        get() = totalShiftMinutes == 0
}

object JetLagPlanner {
    private const val MINUTES_PER_DAY = 24 * 60

    fun plan(
        currentWakeMinutes: Int,
        targetWakeMinutes: Int,
        sleepGoalMinutes: Int,
        adjustmentDays: Int,
        direction: JetLagDirection
    ): JetLagPlan {
        val currentWake = normalizeMinuteOfDay(currentWakeMinutes)
        val targetWake = normalizeMinuteOfDay(targetWakeMinutes)
        val safeSleepGoal = sleepGoalMinutes.coerceIn(60, 16 * 60)
        val safeDays = adjustmentDays.coerceIn(1, 14)
        val resolvedDirection = resolveDirection(currentWake, targetWake, direction)
        val totalShift = totalShiftMinutes(currentWake, targetWake, resolvedDirection)
        val dailyShift = (totalShift.toDouble() / safeDays).roundToInt()
        val dayPlans = (1..safeDays).map { day ->
            val wake = if (day == safeDays) {
                targetWake
            } else {
                normalizeMinuteOfDay(
                    roundToNearestFive(currentWake + (totalShift.toDouble() * day / safeDays).roundToInt())
                )
            }
            dayPlan(
                dayNumber = day,
                wakeMinutes = wake,
                sleepGoalMinutes = safeSleepGoal,
                direction = resolvedDirection
            )
        }
        return JetLagPlan(
            currentWakeMinutes = currentWake,
            targetWakeMinutes = targetWake,
            sleepGoalMinutes = safeSleepGoal,
            requestedDirection = direction,
            resolvedDirection = resolvedDirection,
            adjustmentDays = safeDays,
            totalShiftMinutes = totalShift,
            dailyShiftMinutes = dailyShift,
            days = dayPlans
        )
    }

    fun normalizeMinuteOfDay(minutes: Int): Int =
        ((minutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY

    private fun resolveDirection(
        currentWakeMinutes: Int,
        targetWakeMinutes: Int,
        direction: JetLagDirection
    ): JetLagDirection {
        if (currentWakeMinutes == targetWakeMinutes) return JetLagDirection.AUTO
        if (direction != JetLagDirection.AUTO) return direction
        val delay = delayMinutes(currentWakeMinutes, targetWakeMinutes)
        val advance = advanceMinutes(currentWakeMinutes, targetWakeMinutes)
        return if (delay <= advance) JetLagDirection.DELAY else JetLagDirection.ADVANCE
    }

    private fun totalShiftMinutes(
        currentWakeMinutes: Int,
        targetWakeMinutes: Int,
        direction: JetLagDirection
    ): Int {
        if (currentWakeMinutes == targetWakeMinutes) return 0
        return when (direction) {
            JetLagDirection.ADVANCE -> -advanceMinutes(currentWakeMinutes, targetWakeMinutes)
            JetLagDirection.DELAY -> delayMinutes(currentWakeMinutes, targetWakeMinutes)
            JetLagDirection.AUTO -> 0
        }
    }

    private fun delayMinutes(currentWakeMinutes: Int, targetWakeMinutes: Int): Int =
        normalizeMinuteOfDay(targetWakeMinutes - currentWakeMinutes)

    private fun advanceMinutes(currentWakeMinutes: Int, targetWakeMinutes: Int): Int {
        val delay = delayMinutes(currentWakeMinutes, targetWakeMinutes)
        return if (delay == 0) 0 else MINUTES_PER_DAY - delay
    }

    private fun dayPlan(
        dayNumber: Int,
        wakeMinutes: Int,
        sleepGoalMinutes: Int,
        direction: JetLagDirection
    ): JetLagDayPlan {
        val bedtime = normalizeMinuteOfDay(wakeMinutes - sleepGoalMinutes)
        return when (direction) {
            JetLagDirection.ADVANCE -> JetLagDayPlan(
                dayNumber = dayNumber,
                wakeMinutes = wakeMinutes,
                bedtimeMinutes = bedtime,
                brightLightStartMinutes = normalizeMinuteOfDay(wakeMinutes + 30),
                brightLightEndMinutes = normalizeMinuteOfDay(wakeMinutes + 150),
                dimLightStartMinutes = normalizeMinuteOfDay(bedtime - 180),
                dimLightEndMinutes = bedtime
            )
            JetLagDirection.DELAY -> JetLagDayPlan(
                dayNumber = dayNumber,
                wakeMinutes = wakeMinutes,
                bedtimeMinutes = bedtime,
                brightLightStartMinutes = normalizeMinuteOfDay(bedtime - 240),
                brightLightEndMinutes = normalizeMinuteOfDay(bedtime - 120),
                dimLightStartMinutes = wakeMinutes,
                dimLightEndMinutes = normalizeMinuteOfDay(wakeMinutes + 120)
            )
            JetLagDirection.AUTO -> JetLagDayPlan(
                dayNumber = dayNumber,
                wakeMinutes = wakeMinutes,
                bedtimeMinutes = bedtime,
                brightLightStartMinutes = normalizeMinuteOfDay(wakeMinutes + 30),
                brightLightEndMinutes = normalizeMinuteOfDay(wakeMinutes + 90),
                dimLightStartMinutes = normalizeMinuteOfDay(bedtime - 90),
                dimLightEndMinutes = bedtime
            )
        }
    }

    private fun roundToNearestFive(minutes: Int): Int =
        (minutes / 5.0).roundToInt() * 5
}

fun formatJetLagShift(minutes: Int): String {
    val absolute = abs(minutes)
    val hours = absolute / 60
    val mins = absolute % 60
    val duration = when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
    return when {
        minutes < 0 -> "$duration earlier"
        minutes > 0 -> "$duration later"
        else -> "No shift"
    }
}
