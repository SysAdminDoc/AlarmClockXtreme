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
 * F13: Caches public holiday data locally (simple newline-delimited ISO dates in filesDir).
 * TTL: 7 days. Country determined from user preferences.
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

    /** Returns true if [date] is a public holiday for the configured country. */
    suspend fun isHoliday(date: LocalDate): Boolean {
        val settings = preferencesManager.getCurrentSettings()
        if (!settings.holidayAutoSkipEnabled || settings.holidayCountryCode.isBlank()) return false

        mutex.withLock { ensureCacheValid(settings.holidayCountryCode, date.year) }
        val isoDate = date.toString()  // "YYYY-MM-DD"
        return try {
            cacheFile.exists() && cacheFile.readLines().any { it.trim() == isoDate }
        } catch (_: Exception) { false }
    }

    /** Refresh holiday data for the current year. Called by HolidaySyncWorker. */
    suspend fun refresh() {
        val settings = preferencesManager.getCurrentSettings()
        val countryCode = settings.holidayCountryCode
        if (countryCode.isBlank()) return

        val year = LocalDate.now().year
        fetchAndCache(countryCode, year)
        // Also pre-fetch next year in December
        if (LocalDate.now().monthValue == 12) {
            fetchAndCache(countryCode, year + 1)
        }
    }

    private suspend fun ensureCacheValid(countryCode: String, year: Int) {
        val meta = if (metaFile.exists()) metaFile.readText() else ""
        val parts = meta.split("|")
        val cachedCountry = parts.getOrNull(0) ?: ""
        val cachedTs = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val expired = System.currentTimeMillis() - cachedTs > cacheTtlMs
        if (cachedCountry != countryCode || expired) {
            fetchAndCache(countryCode, year)
        }
    }

    private suspend fun fetchAndCache(countryCode: String, year: Int) {
        try {
            val holidays = holidayApi.getPublicHolidays(year, countryCode)
            val dates = holidays.map { it.date }
            val content = dates.joinToString("\n")
            // Write content first, then update meta atomically
            cacheFile.writeText(content)
            metaFile.writeText("$countryCode|${System.currentTimeMillis()}")
        } catch (_: Exception) {
            // Keep stale cache on network failure — do not update meta timestamp
        }
    }
}
