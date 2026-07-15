package com.sysadmindoc.alarmclock.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmHealthPolicyTest {

    @Test
    fun `healthy state has no warning`() {
        assertTrue(
            alarmHealthIssues(
                AlarmHealthSignals(
                    batteryOptimizationActive = false,
                    backgroundRestricted = false,
                    notificationsAllowed = true,
                    exactAlarmsAllowed = true,
                    manufacturer = "google"
                )
            ).isEmpty()
        )
    }

    @Test
    fun `regressed state reports every actionable issue`() {
        val issues = alarmHealthIssues(
            AlarmHealthSignals(
                batteryOptimizationActive = true,
                backgroundRestricted = true,
                notificationsAllowed = false,
                exactAlarmsAllowed = false,
                manufacturer = "samsung"
            )
        )

        assertEquals(4, issues.size)
        assertTrue(issues.any { it.contains("background activity", ignoreCase = true) })
        assertTrue(issues.any { it.contains("Samsung") })
        assertTrue(issues.any { it.contains("Notification permission") })
        assertTrue(issues.any { it.contains("Exact alarm permission") })
    }
}
