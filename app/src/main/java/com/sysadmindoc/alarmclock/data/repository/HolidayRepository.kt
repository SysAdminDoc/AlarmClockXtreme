package com.sysadmindoc.alarmclock.data.repository

import android.content.Context
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.remote.HolidayApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * F13: Caches public holiday data locally (newline-delimited ISO dates in filesDir).
 * TTL: 7 days. Country determined from user preferences.
 *
 * Concurrency:
 * - All file reads/writes happen inside [mutex] so concurrent calls do not race
 *   on the cache file.
 * - An in-memory snapshot of the parsed dates is reused so that repeating
 *   alarms which probe many candidate dates (see [com.sysadmindoc.alarmclock.domain.AlarmScheduler.schedule])
 *   don't hit the disk on every iteration.
 */
@Singleton
class HolidayRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val holidayApi: HolidayApi,
    private val preferencesManager: PreferencesManager
) {
    private val cacheFile get() = File(context.filesDir, "holiday_cache.txt")
    private val metaFile get() = File(context.filesDir, "holiday_cache_meta.txt")
    private val cacheTtlMs = 7L * 24 * 60 * 60 * 1000  // 7 days
    private val mutex = Mutex()

    @Volatile private var memoryCacheCountry: String? = null
    @Volatile private var memoryCacheLoadedAt: Long = 0L
    @Volatile private var memoryCacheDates: Set<String> = emptySet()
    // Which calendar years the in-memory snapshot actually covers. Without this
    // the fast path would answer "not a holiday" for a year that was never
    // fetched (the scheduler probes up to a year ahead), silently firing alarms
    // on holidays the user asked to skip.
    @Volatile private var memoryCacheYears: Set<Int> = emptySet()

    private data class CacheMeta(
        val countryCode: String = "",
        val fetchedAtMillis: Long = 0L,
        val coveredYears: Set<Int> = emptySet()
    )

    /** Returns true if [date] is a public holiday for the configured country. */
    suspend fun isHoliday(date: LocalDate): Boolean {
        val settings = preferencesManager.getCurrentSettings()
        if (!settings.holidayAutoSkipEnabled || settings.holidayCountryCode.isBlank()) return false

        val country = settings.holidayCountryCode
        // Fast path: in-memory cache for the right country, not stale, AND the
        // queried year is actually covered by that snapshot.
        val now = System.currentTimeMillis()
        if (memoryCacheCountry == country &&
            (now - memoryCacheLoadedAt) <= cacheTtlMs &&
            date.year in memoryCacheYears
        ) {
            return date.toString() in memoryCacheDates
        }

        return mutex.withLock {
            ensureCacheValidLocked(country, date.year)
            // Reload memory snapshot from disk while still holding the lock.
            memoryCacheDates = readCacheDatesLocked()
            memoryCacheYears = readMetaLocked().coveredYears
            memoryCacheCountry = country
            memoryCacheLoadedAt = System.currentTimeMillis()
            date.toString() in memoryCacheDates
        }
    }

    /** Refresh holiday data for the current year. Called by HolidaySyncWorker. */
    suspend fun refresh() {
        val settings = preferencesManager.getCurrentSettings()
        val countryCode = settings.holidayCountryCode
        if (countryCode.isBlank()) return

        val year = LocalDate.now().year
        mutex.withLock {
            fetchAndCacheLocked(countryCode, year)
            // Also pre-fetch next year in December
            if (LocalDate.now().monthValue == 12) {
                fetchAndCacheLocked(
                    countryCode = countryCode,
                    year = year + 1,
                    append = true,
                    coveredYears = setOf(year)
                )
            }
            memoryCacheDates = readCacheDatesLocked()
            memoryCacheYears = readMetaLocked().coveredYears
            memoryCacheCountry = countryCode
            memoryCacheLoadedAt = System.currentTimeMillis()
        }
    }

    private suspend fun ensureCacheValidLocked(countryCode: String, year: Int) {
        val meta = readMetaLocked()
        val expired = System.currentTimeMillis() - meta.fetchedAtMillis > cacheTtlMs
        if (meta.countryCode != countryCode || expired || !cacheFile.exists()) {
            fetchAndCacheLocked(countryCode, year)
        } else if (year !in meta.coveredYears) {
            fetchAndCacheLocked(
                countryCode = countryCode,
                year = year,
                append = true,
                coveredYears = meta.coveredYears
            )
        }
    }

    private suspend fun fetchAndCacheLocked(
        countryCode: String,
        year: Int,
        append: Boolean = false,
        coveredYears: Set<Int> = emptySet()
    ) {
        try {
            val holidays = holidayApi.getPublicHolidays(year, countryCode)
            val fetchedDates = holidays.map { it.date }.toSet()
            val allDates = if (append && cacheFile.exists()) {
                (readCacheDatesLocked() + fetchedDates).sorted()
            } else {
                fetchedDates.sorted()
            }
            cacheFile.writeText(allDates.joinToString("\n"))
            writeMetaLocked(
                CacheMeta(
                    countryCode = countryCode,
                    fetchedAtMillis = System.currentTimeMillis(),
                    coveredYears = coveredYears + year
                )
            )
        } catch (_: Exception) {
            // Keep stale cache on network failure — do not update meta timestamp
        }
    }

    private fun readMetaLocked(): CacheMeta {
        val raw = if (metaFile.exists()) runCatching { metaFile.readText() }.getOrDefault("") else ""
        val parts = raw.split("|")
        return CacheMeta(
            countryCode = parts.getOrNull(0).orEmpty(),
            fetchedAtMillis = parts.getOrNull(1)?.toLongOrNull() ?: 0L,
            coveredYears = parts.getOrNull(2)
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet()
                ?: emptySet()
        )
    }

    private fun writeMetaLocked(meta: CacheMeta) {
        metaFile.writeText(
            "${meta.countryCode}|${meta.fetchedAtMillis}|${meta.coveredYears.sorted().joinToString(",")}"
        )
    }

    private fun readCacheDatesLocked(): Set<String> {
        return try {
            if (!cacheFile.exists()) emptySet()
            else cacheFile.readLines()
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
}
