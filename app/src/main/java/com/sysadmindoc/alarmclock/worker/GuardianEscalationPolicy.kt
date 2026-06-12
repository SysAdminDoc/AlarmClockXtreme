package com.sysadmindoc.alarmclock.worker

internal object GuardianEscalationPolicy {
    const val FDROID_FLAVOR = "fdroid"

    fun canSendDirectSms(flavor: String, hasSendSmsPermission: Boolean): Boolean =
        flavor == FDROID_FLAVOR && hasSendSmsPermission

    fun buildMessage(label: String): String =
        "AlarmClockXtreme Guardian Alert: $label was not dismissed. Please check on the user."

    /**
     * Keep only characters that are safe in tel:/smsto: targets. Returns null
     * when fewer than three digits remain, so the worker never opens garbage.
     */
    fun sanitisePhone(raw: String): String? {
        val cleaned = buildString {
            for (c in raw) {
                if (c.isDigit() || c == '+' || c == '-' || c == '*' || c == '#') append(c)
            }
        }
        return if (cleaned.count { it.isDigit() } >= 3) cleaned else null
    }
}
