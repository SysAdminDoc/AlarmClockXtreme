package com.sysadmindoc.alarmclock.worker

import com.sysadmindoc.alarmclock.R
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

        // Ids, not sentences. This used to match substrings of the English
        // ("background activity", "Notification permission"), which is the
        // assertion that stops meaning anything once the copy is translated.
        assertEquals(4, issues.size)
        assertEquals(
            listOf(
                R.string.health_background_restricted,
                R.string.health_battery_optimization_manufacturer,
                R.string.health_notifications_denied,
                R.string.health_exact_alarms_revoked
            ),
            issues.map { it.messageRes }
        )
        // The manufacturer is carried as an argument rather than spliced into
        // the sentence, so the translation decides where it goes.
        assertEquals("Samsung", issues[1].manufacturer)
    }
}
