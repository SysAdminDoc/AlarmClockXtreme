package com.sysadmindoc.alarmclock.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilityDoctorTest {

    @Test
    fun firstFingerprintObservationDoesNotReopenChecklist() {
        assertFalse(ReliabilityDoctor.shouldReopenChecklist(null, "build-a"))
        assertFalse(ReliabilityDoctor.shouldReopenChecklist("", "build-a"))
    }

    @Test
    fun changedFingerprintReopensChecklist() {
        assertTrue(ReliabilityDoctor.shouldReopenChecklist("build-a", "build-b"))
    }

    @Test
    fun unchangedOrBlankFingerprintDoesNotReopenChecklist() {
        assertFalse(ReliabilityDoctor.shouldReopenChecklist("build-a", "build-a"))
        assertFalse(ReliabilityDoctor.shouldReopenChecklist("build-a", ""))
        assertFalse(ReliabilityDoctor.shouldReopenChecklist("build-a", "   "))
    }
}
