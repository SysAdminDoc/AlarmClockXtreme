package com.sysadmindoc.alarmclock.worker

enum class GuardianSmsPath {
    INACTIVE,
    DIRECT_SMS,
    NEEDS_SEND_SMS_PERMISSION,
    SMS_COMPOSER
}

data class GuardianReadiness(
    val enabledAlarmCount: Int,
    val smsPath: GuardianSmsPath,
    val hasSendSmsPermission: Boolean,
    val hasCallPhonePermission: Boolean
) {
    val hasEnabledAlarms: Boolean
        get() = enabledAlarmCount > 0

    val needsSmsPermission: Boolean
        get() = smsPath == GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION

    val needsCallPermission: Boolean
        get() = hasEnabledAlarms && !hasCallPhonePermission

    val needsUserAction: Boolean
        get() = needsSmsPermission || needsCallPermission
}

internal object GuardianEscalationPolicy {
    const val FDROID_FLAVOR = "fdroid"

    fun canSendDirectSms(flavor: String, hasSendSmsPermission: Boolean): Boolean =
        flavor == FDROID_FLAVOR && hasSendSmsPermission

    fun readiness(
        flavor: String,
        enabledAlarmCount: Int,
        hasSendSmsPermission: Boolean,
        hasCallPhonePermission: Boolean
    ): GuardianReadiness {
        val count = enabledAlarmCount.coerceAtLeast(0)
        val smsPath = when {
            count == 0 -> GuardianSmsPath.INACTIVE
            canSendDirectSms(flavor, hasSendSmsPermission) -> GuardianSmsPath.DIRECT_SMS
            flavor == FDROID_FLAVOR -> GuardianSmsPath.NEEDS_SEND_SMS_PERMISSION
            else -> GuardianSmsPath.SMS_COMPOSER
        }
        return GuardianReadiness(
            enabledAlarmCount = count,
            smsPath = smsPath,
            hasSendSmsPermission = hasSendSmsPermission,
            hasCallPhonePermission = hasCallPhonePermission
        )
    }

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
