package com.sysadmindoc.alarmclock.data.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PlayHealthConnectSleepRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : HealthConnectSleepRepository {
    override val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    override fun createPermissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    override suspend fun readRecentSleepSummary(
        daysBack: Long,
        includeRecords: Boolean
    ): HealthConnectSleepSummary =
        withContext(Dispatchers.IO) {
            val availability = when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED
                else -> HealthConnectAvailability.UNAVAILABLE
            }

            if (availability != HealthConnectAvailability.AVAILABLE) {
                return@withContext HealthConnectSleepSummary(
                    availability = availability,
                    refreshedAtMillis = System.currentTimeMillis()
                )
            }

            runCatching {
                val client = HealthConnectClient.getOrCreate(context)
                val granted = client.permissionController
                    .getGrantedPermissions()
                    .containsAll(requiredPermissions)

                if (!granted) {
                    return@runCatching HealthConnectSleepSummary(
                        availability = HealthConnectAvailability.AVAILABLE,
                        permissionGranted = false,
                        refreshedAtMillis = System.currentTimeMillis()
                    )
                }
                if (!includeRecords) {
                    return@runCatching HealthConnectSleepSummary(
                        availability = HealthConnectAvailability.AVAILABLE,
                        permissionGranted = true,
                        refreshedAtMillis = System.currentTimeMillis()
                    )
                }

                val end = Instant.now()
                val start = end.minus(Duration.ofDays(daysBack.coerceAtLeast(1L)))
                val response = client.readRecords(
                    ReadRecordsRequest<SleepSessionRecord>(
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )
                summarize(response.records)
            }.getOrElse { error ->
                HealthConnectSleepSummary(
                    availability = HealthConnectAvailability.AVAILABLE,
                    permissionGranted = false,
                    errorMessage = error.message ?: error::class.java.simpleName,
                    refreshedAtMillis = System.currentTimeMillis()
                )
            }
        }

    private fun summarize(records: List<SleepSessionRecord>): HealthConnectSleepSummary {
        val sorted = records.sortedByDescending { it.endTime }
        val last = sorted.firstOrNull()
        val stageTotals = last?.stages.orEmpty().fold(StageTotals()) { totals, stage ->
            val minutes = Duration.between(stage.startTime, stage.endTime)
                .toMinutes()
                .coerceAtLeast(0)
            when (stage.stage) {
                SleepSessionRecord.STAGE_TYPE_AWAKE,
                SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> totals.copy(
                    awake = totals.awake + minutes
                )
                SleepSessionRecord.STAGE_TYPE_SLEEPING -> totals.copy(
                    asleep = totals.asleep + minutes
                )
                SleepSessionRecord.STAGE_TYPE_LIGHT -> totals.copy(
                    light = totals.light + minutes
                )
                SleepSessionRecord.STAGE_TYPE_DEEP -> totals.copy(
                    deep = totals.deep + minutes
                )
                SleepSessionRecord.STAGE_TYPE_REM -> totals.copy(
                    rem = totals.rem + minutes
                )
                else -> totals.copy(unknown = totals.unknown + minutes)
            }
        }

        val duration = last?.let {
            Duration.between(it.startTime, it.endTime).toMinutes().coerceAtLeast(0)
        }

        return HealthConnectSleepSummary(
            availability = HealthConnectAvailability.AVAILABLE,
            permissionGranted = true,
            sessionsRead = sorted.size,
            lastSessionStartMillis = last?.startTime?.toEpochMilli(),
            lastSessionEndMillis = last?.endTime?.toEpochMilli(),
            lastSessionDurationMinutes = duration,
            asleepStageMinutes = stageTotals.asleep,
            lightStageMinutes = stageTotals.light,
            deepStageMinutes = stageTotals.deep,
            remStageMinutes = stageTotals.rem,
            awakeStageMinutes = stageTotals.awake,
            unknownStageMinutes = stageTotals.unknown,
            refreshedAtMillis = System.currentTimeMillis()
        )
    }

    private data class StageTotals(
        val asleep: Long = 0,
        val light: Long = 0,
        val deep: Long = 0,
        val rem: Long = 0,
        val awake: Long = 0,
        val unknown: Long = 0
    )
}
