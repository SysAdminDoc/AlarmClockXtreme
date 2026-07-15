package com.sysadmindoc.alarmclock.ui.bedtime

import org.junit.Assert.assertEquals
import org.junit.Test

class SonarStartConfirmationTest {

    @Test
    fun `monitoring requires an active service snapshot`() {
        assertEquals(
            SonarStartConfirmation.MONITORING,
            sonarStartConfirmation(snapshotActive = true, attemptsRemaining = 0)
        )
        assertEquals(
            SonarStartConfirmation.WAITING,
            sonarStartConfirmation(snapshotActive = false, attemptsRemaining = 3)
        )
        assertEquals(
            SonarStartConfirmation.FAILED,
            sonarStartConfirmation(snapshotActive = false, attemptsRemaining = 0)
        )
    }
}
