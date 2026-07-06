package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import com.sysadmindoc.alarmclock.data.local.entity.PreSleepTagEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class PreSleepTagDefinition(
    val key: String,
    val label: String,
    val helper: String
)

data class PreSleepTagCorrelation(
    val key: String,
    val label: String,
    val loggedNights: Int,
    val nightsWithSessions: Int,
    val averageRestlessMinutes: Int?,
    val baselineRestlessMinutes: Int?,
    val deltaRestlessMinutes: Int?
)

object PreSleepTags {
    const val CAFFEINE = "caffeine"
    const val EXERCISE = "exercise"
    const val ALCOHOL = "alcohol"
    const val STRESS = "stress"

    val all = listOf(
        PreSleepTagDefinition(CAFFEINE, "Caffeine", "Coffee, tea, energy drinks"),
        PreSleepTagDefinition(EXERCISE, "Exercise", "Late workout or training"),
        PreSleepTagDefinition(ALCOHOL, "Alcohol", "Any evening alcohol"),
        PreSleepTagDefinition(STRESS, "Stress", "High-stress evening")
    )

    fun labelFor(key: String): String = all.firstOrNull { it.key == key }?.label ?: key
}

object PreSleepTagAnalytics {
    fun tagDateFor(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalDate {
        val local = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        return if (local.hour < MORNING_CUTOFF_HOUR) {
            local.toLocalDate().minusDays(1)
        } else {
            local.toLocalDate()
        }
    }

    fun buildCorrelations(
        tags: List<PreSleepTagEntry>,
        sessions: List<ActigraphySession>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<PreSleepTagCorrelation> {
        val sessionsByTagDate = sessions.groupBy { session ->
            Instant.ofEpochMilli(session.startedAt).atZone(zoneId).toLocalDate().toString()
        }
        val restlessByDate = sessionsByTagDate.mapValues { (_, dateSessions) ->
            dateSessions.map { it.awakeMinutes + it.lightMinutes }.average().roundToInt()
        }
        val baseline = restlessByDate.values.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        val datesByTag = tags.groupBy { it.tagKey }
            .mapValues { (_, rows) -> rows.map { it.localDate }.toSet() }

        return PreSleepTags.all.map { definition ->
            val tagDates = datesByTag[definition.key].orEmpty()
            val restless = tagDates.mapNotNull { restlessByDate[it] }
            val averageRestless = restless.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
            PreSleepTagCorrelation(
                key = definition.key,
                label = definition.label,
                loggedNights = tagDates.size,
                nightsWithSessions = restless.size,
                averageRestlessMinutes = averageRestless,
                baselineRestlessMinutes = baseline,
                deltaRestlessMinutes = if (averageRestless != null && baseline != null) {
                    averageRestless - baseline
                } else {
                    null
                }
            )
        }
    }

    private const val MORNING_CUTOFF_HOUR = 6
}
