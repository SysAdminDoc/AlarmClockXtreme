package com.sysadmindoc.alarmclock.data.health

import androidx.activity.result.contract.ActivityResultContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FdroidHealthConnectSleepRepository @Inject constructor() : HealthConnectSleepRepository {
    override val requiredPermissions: Set<String> = emptySet()

    override fun createPermissionRequestContract(): ActivityResultContract<Set<String>, Set<String>>? = null

    override suspend fun readRecentSleepSummary(
        daysBack: Long,
        includeRecords: Boolean
    ): HealthConnectSleepSummary =
        HealthConnectSleepSummary(
            availability = HealthConnectAvailability.NOT_INCLUDED,
            permissionGranted = false,
            errorMessage = "Health Connect is not included in the F-Droid flavor."
        )
}
