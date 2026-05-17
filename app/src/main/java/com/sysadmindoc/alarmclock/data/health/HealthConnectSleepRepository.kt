package com.sysadmindoc.alarmclock.data.health

import androidx.activity.result.contract.ActivityResultContract

enum class HealthConnectAvailability {
    AVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
    UNAVAILABLE,
    NOT_INCLUDED
}

data class HealthConnectSleepSummary(
    val availability: HealthConnectAvailability = HealthConnectAvailability.NOT_INCLUDED,
    val permissionGranted: Boolean = false,
    val sessionsRead: Int = 0,
    val lastSessionStartMillis: Long? = null,
    val lastSessionEndMillis: Long? = null,
    val lastSessionDurationMinutes: Long? = null,
    val asleepStageMinutes: Long = 0,
    val lightStageMinutes: Long = 0,
    val deepStageMinutes: Long = 0,
    val remStageMinutes: Long = 0,
    val awakeStageMinutes: Long = 0,
    val unknownStageMinutes: Long = 0,
    val errorMessage: String? = null,
    val refreshedAtMillis: Long = 0
) {
    val isAvailable: Boolean
        get() = availability == HealthConnectAvailability.AVAILABLE

    val hasRecentSession: Boolean
        get() = lastSessionDurationMinutes != null
}

interface HealthConnectSleepRepository {
    val requiredPermissions: Set<String>

    fun createPermissionRequestContract(): ActivityResultContract<Set<String>, Set<String>>?

    suspend fun readRecentSleepSummary(
        daysBack: Long = 14,
        includeRecords: Boolean = true
    ): HealthConnectSleepSummary
}
