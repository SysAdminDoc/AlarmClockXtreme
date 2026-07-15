package com.sysadmindoc.alarmclock.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

data class CommuteHistorySample(val minutes: Int, val observedAtMillis: Long)

data class LearnedCommuteEstimate(
    val minutes: Int,
    val sampleCount: Int,
    val newestSampleAtMillis: Long
)

data class CommuteHistorySummary(val routeCount: Int, val sampleCount: Int)

object CommuteHistoryPolicy {
    const val MIN_SAMPLES = 3
    const val MAX_SAMPLES_PER_ROUTE = 8
    const val MAX_ROUTE_COUNT = 50
    const val RETENTION_MILLIS = 45L * 24 * 60 * 60 * 1_000

    fun routeKey(originLatitude: Double, originLongitude: Double, destination: String): String {
        val origin = String.format(Locale.US, "%.3f,%.3f", originLatitude, originLongitude)
        val normalizedDestination = Normalizer.normalize(destination, Normalizer.Form.NFKC)
            .lowercase(Locale.US)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$origin|$normalizedDestination".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun addSample(
        existing: List<CommuteHistorySample>,
        minutes: Int,
        observedAtMillis: Long
    ): List<CommuteHistorySample> {
        if (minutes !in 1..240 || observedAtMillis <= 0L) return retained(existing, observedAtMillis)
        val retained = retained(existing, observedAtMillis)
        if (retained.size >= MIN_SAMPLES) {
            val median = retained.map { it.minutes }.sorted()[retained.size / 2]
            if (minutes < median / 3.0 || minutes > median * 3.0) return retained
        }
        return (retained + CommuteHistorySample(minutes, observedAtMillis))
            .sortedByDescending { it.observedAtMillis }
            .take(MAX_SAMPLES_PER_ROUTE)
    }

    fun estimate(samples: List<CommuteHistorySample>, nowMillis: Long): LearnedCommuteEstimate? {
        val retained = retained(samples, nowMillis)
        if (retained.size < MIN_SAMPLES) return null
        val sortedMinutes = retained.map { it.minutes }.sorted()
        val percentileIndex = (ceil(sortedMinutes.size * 0.75).toInt() - 1)
            .coerceIn(0, sortedMinutes.lastIndex)
        return LearnedCommuteEstimate(
            minutes = sortedMinutes[percentileIndex],
            sampleCount = retained.size,
            newestSampleAtMillis = retained.maxOf { it.observedAtMillis }
        )
    }

    fun retained(samples: List<CommuteHistorySample>, nowMillis: Long): List<CommuteHistorySample> {
        if (nowMillis <= 0L) return emptyList()
        val cutoff = nowMillis - RETENTION_MILLIS
        return samples
            .filter { it.minutes in 1..240 && it.observedAtMillis in cutoff..nowMillis }
            .sortedByDescending { it.observedAtMillis }
            .take(MAX_SAMPLES_PER_ROUTE)
    }

    fun encode(samples: List<CommuteHistorySample>): String = samples.joinToString(";") {
        "${it.observedAtMillis}:${it.minutes}"
    }

    fun decode(value: String?): List<CommuteHistorySample> = value.orEmpty()
        .split(';')
        .mapNotNull { token ->
            val parts = token.split(':', limit = 2)
            val timestamp = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            CommuteHistorySample(minutes, timestamp)
        }
}

@Singleton
class CommuteHistoryStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    fun record(
        originLatitude: Double,
        originLongitude: Double,
        destination: String,
        minutes: Int,
        observedAtMillis: Long = System.currentTimeMillis()
    ) = synchronized(lock) {
        if (destination.isBlank()) return@synchronized
        val key = routePreferenceKey(originLatitude, originLongitude, destination)
        val updated = CommuteHistoryPolicy.addSample(
            CommuteHistoryPolicy.decode(preferences.getString(key, null)),
            minutes,
            observedAtMillis
        )
        if (updated.isNotEmpty()) preferences.edit().putString(key, CommuteHistoryPolicy.encode(updated)).apply()
        pruneRouteCount()
    }

    fun estimate(
        originLatitude: Double,
        originLongitude: Double,
        destination: String,
        nowMillis: Long = System.currentTimeMillis()
    ): LearnedCommuteEstimate? = synchronized(lock) {
        if (destination.isBlank()) return@synchronized null
        val key = routePreferenceKey(originLatitude, originLongitude, destination)
        val decoded = CommuteHistoryPolicy.decode(preferences.getString(key, null))
        val retained = CommuteHistoryPolicy.retained(decoded, nowMillis)
        if (retained.size != decoded.size) {
            val edit = preferences.edit()
            if (retained.isEmpty()) edit.remove(key) else edit.putString(key, CommuteHistoryPolicy.encode(retained))
            edit.apply()
        }
        CommuteHistoryPolicy.estimate(retained, nowMillis)
    }

    fun clear() = synchronized(lock) { preferences.edit().clear().apply() }

    fun summary(nowMillis: Long = System.currentTimeMillis()): CommuteHistorySummary = synchronized(lock) {
        val retainedRoutes = preferences.all.values.mapNotNull { value ->
            CommuteHistoryPolicy.retained(CommuteHistoryPolicy.decode(value as? String), nowMillis)
                .takeIf { it.isNotEmpty() }
        }
        CommuteHistorySummary(
            routeCount = retainedRoutes.size,
            sampleCount = retainedRoutes.sumOf { it.size }
        )
    }

    private fun routePreferenceKey(latitude: Double, longitude: Double, destination: String) =
        ROUTE_PREFIX + CommuteHistoryPolicy.routeKey(latitude, longitude, destination)

    private fun pruneRouteCount() {
        val overflow = preferences.all.entries
            .filter { it.key.startsWith(ROUTE_PREFIX) }
            .map { entry ->
                entry.key to (CommuteHistoryPolicy.decode(entry.value as? String)
                    .maxOfOrNull { it.observedAtMillis } ?: Long.MIN_VALUE)
            }
            .sortedByDescending { it.second }
            .drop(CommuteHistoryPolicy.MAX_ROUTE_COUNT)
        if (overflow.isNotEmpty()) {
            preferences.edit().also { edit -> overflow.forEach { edit.remove(it.first) } }.apply()
        }
    }

    private companion object {
        const val FILE_NAME = "commute_history"
        const val ROUTE_PREFIX = "route_"
    }
}
