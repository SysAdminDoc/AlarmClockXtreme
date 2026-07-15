package com.sysadmindoc.alarmclock.ui.bedtime

internal enum class SonarStartConfirmation {
    WAITING,
    MONITORING,
    FAILED
}

internal fun sonarStartConfirmation(
    snapshotActive: Boolean,
    attemptsRemaining: Int
): SonarStartConfirmation = when {
    snapshotActive -> SonarStartConfirmation.MONITORING
    attemptsRemaining > 0 -> SonarStartConfirmation.WAITING
    else -> SonarStartConfirmation.FAILED
}
