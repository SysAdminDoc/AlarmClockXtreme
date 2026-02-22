package com.sysadmindoc.alarmclock.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alarm_settings")

data class AppSettings(
    val is24HourFormat: Boolean = false,
    val defaultSnoozeDuration: Int = 10,
    val defaultGradualVolume: Int = 60,
    val usePhoneSpeakers: Boolean = false,
    val showOnLockScreen: Boolean = true,
    val upcomingAlarmMinutes: Int = 60,
    val showNoAlarmsWarning: Boolean = true,
    // Vacation mode
    val vacationModeEnabled: Boolean = false,
    val vacationStartMillis: Long = 0,
    val vacationEndMillis: Long = 0,
    // Dashboard
    val showWeatherOnDashboard: Boolean = true,
    val showCalendarOnDashboard: Boolean = true,
    val lastKnownLatitude: Double = 0.0,
    val lastKnownLongitude: Double = 0.0,
    // Auto-silence
    val autoSilenceMinutes: Int = 10, // 0 = never, 5/10/15/30
    // Temperature unit
    val temperatureUnit: String = "fahrenheit", // "fahrenheit" or "celsius"
    // Manual location for weather
    val locationName: String = "", // e.g. "Dallas, Texas, United States"
    val useManualLocation: Boolean = false,
    // Bedtime
    val bedtimeEnabled: Boolean = false,
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val sleepGoalHours: Int = 8,
    val sleepGoalMinutes: Int = 0,
    val bedtimeReminderMinutes: Int = 30,
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour")
        val DEFAULT_SNOOZE = intPreferencesKey("default_snooze")
        val DEFAULT_GRADUAL_VOLUME = intPreferencesKey("default_gradual_volume")
        val USE_PHONE_SPEAKERS = booleanPreferencesKey("use_phone_speakers")
        val SHOW_ON_LOCK_SCREEN = booleanPreferencesKey("show_on_lock_screen")
        val UPCOMING_ALARM_MINUTES = intPreferencesKey("upcoming_alarm_minutes")
        val SHOW_NO_ALARMS_WARNING = booleanPreferencesKey("show_no_alarms_warning")
        val VACATION_ENABLED = booleanPreferencesKey("vacation_enabled")
        val VACATION_START = longPreferencesKey("vacation_start")
        val VACATION_END = longPreferencesKey("vacation_end")
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val SHOW_CALENDAR = booleanPreferencesKey("show_calendar")
        val LAST_LATITUDE = doublePreferencesKey("last_latitude")
        val LAST_LONGITUDE = doublePreferencesKey("last_longitude")
        val AUTO_SILENCE = intPreferencesKey("auto_silence_minutes")
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        val LOCATION_NAME = stringPreferencesKey("location_name")
        val USE_MANUAL_LOCATION = booleanPreferencesKey("use_manual_location")
        val BEDTIME_ENABLED = booleanPreferencesKey("bedtime_enabled")
        val BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val BEDTIME_MINUTE = intPreferencesKey("bedtime_minute")
        val SLEEP_GOAL_HOURS = intPreferencesKey("sleep_goal_hours")
        val SLEEP_GOAL_MINUTES = intPreferencesKey("sleep_goal_minutes")
        val BEDTIME_REMINDER_MINUTES = intPreferencesKey("bedtime_reminder_minutes")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            AppSettings(
                is24HourFormat = prefs[Keys.IS_24_HOUR] ?: false,
                defaultSnoozeDuration = prefs[Keys.DEFAULT_SNOOZE] ?: 10,
                defaultGradualVolume = prefs[Keys.DEFAULT_GRADUAL_VOLUME] ?: 60,
                usePhoneSpeakers = prefs[Keys.USE_PHONE_SPEAKERS] ?: false,
                showOnLockScreen = prefs[Keys.SHOW_ON_LOCK_SCREEN] ?: true,
                upcomingAlarmMinutes = prefs[Keys.UPCOMING_ALARM_MINUTES] ?: 60,
                showNoAlarmsWarning = prefs[Keys.SHOW_NO_ALARMS_WARNING] ?: true,
                vacationModeEnabled = prefs[Keys.VACATION_ENABLED] ?: false,
                vacationStartMillis = prefs[Keys.VACATION_START] ?: 0,
                vacationEndMillis = prefs[Keys.VACATION_END] ?: 0,
                showWeatherOnDashboard = prefs[Keys.SHOW_WEATHER] ?: true,
                showCalendarOnDashboard = prefs[Keys.SHOW_CALENDAR] ?: true,
                lastKnownLatitude = prefs[Keys.LAST_LATITUDE] ?: 0.0,
                lastKnownLongitude = prefs[Keys.LAST_LONGITUDE] ?: 0.0,
                autoSilenceMinutes = prefs[Keys.AUTO_SILENCE] ?: 10,
                temperatureUnit = prefs[Keys.TEMPERATURE_UNIT] ?: "fahrenheit",
                locationName = prefs[Keys.LOCATION_NAME] ?: "",
                useManualLocation = prefs[Keys.USE_MANUAL_LOCATION] ?: false,
                bedtimeEnabled = prefs[Keys.BEDTIME_ENABLED] ?: false,
                bedtimeHour = prefs[Keys.BEDTIME_HOUR] ?: 23,
                bedtimeMinute = prefs[Keys.BEDTIME_MINUTE] ?: 0,
                sleepGoalHours = prefs[Keys.SLEEP_GOAL_HOURS] ?: 8,
                sleepGoalMinutes = prefs[Keys.SLEEP_GOAL_MINUTES] ?: 0,
                bedtimeReminderMinutes = prefs[Keys.BEDTIME_REMINDER_MINUTES] ?: 30,
            )
        }

    suspend fun getCurrentSettings(): AppSettings {
        return settings.first()
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val current = AppSettings() // defaults
        context.dataStore.edit { prefs ->
            val old = AppSettings(
                is24HourFormat = prefs[Keys.IS_24_HOUR] ?: current.is24HourFormat,
                defaultSnoozeDuration = prefs[Keys.DEFAULT_SNOOZE] ?: current.defaultSnoozeDuration,
                defaultGradualVolume = prefs[Keys.DEFAULT_GRADUAL_VOLUME] ?: current.defaultGradualVolume,
                usePhoneSpeakers = prefs[Keys.USE_PHONE_SPEAKERS] ?: current.usePhoneSpeakers,
                showOnLockScreen = prefs[Keys.SHOW_ON_LOCK_SCREEN] ?: current.showOnLockScreen,
                upcomingAlarmMinutes = prefs[Keys.UPCOMING_ALARM_MINUTES] ?: current.upcomingAlarmMinutes,
                showNoAlarmsWarning = prefs[Keys.SHOW_NO_ALARMS_WARNING] ?: current.showNoAlarmsWarning,
                vacationModeEnabled = prefs[Keys.VACATION_ENABLED] ?: current.vacationModeEnabled,
                vacationStartMillis = prefs[Keys.VACATION_START] ?: current.vacationStartMillis,
                vacationEndMillis = prefs[Keys.VACATION_END] ?: current.vacationEndMillis,
                showWeatherOnDashboard = prefs[Keys.SHOW_WEATHER] ?: current.showWeatherOnDashboard,
                showCalendarOnDashboard = prefs[Keys.SHOW_CALENDAR] ?: current.showCalendarOnDashboard,
                lastKnownLatitude = prefs[Keys.LAST_LATITUDE] ?: current.lastKnownLatitude,
                lastKnownLongitude = prefs[Keys.LAST_LONGITUDE] ?: current.lastKnownLongitude,
                autoSilenceMinutes = prefs[Keys.AUTO_SILENCE] ?: current.autoSilenceMinutes,
                temperatureUnit = prefs[Keys.TEMPERATURE_UNIT] ?: current.temperatureUnit,
                locationName = prefs[Keys.LOCATION_NAME] ?: current.locationName,
                useManualLocation = prefs[Keys.USE_MANUAL_LOCATION] ?: current.useManualLocation,
                bedtimeEnabled = prefs[Keys.BEDTIME_ENABLED] ?: current.bedtimeEnabled,
                bedtimeHour = prefs[Keys.BEDTIME_HOUR] ?: current.bedtimeHour,
                bedtimeMinute = prefs[Keys.BEDTIME_MINUTE] ?: current.bedtimeMinute,
                sleepGoalHours = prefs[Keys.SLEEP_GOAL_HOURS] ?: current.sleepGoalHours,
                sleepGoalMinutes = prefs[Keys.SLEEP_GOAL_MINUTES] ?: current.sleepGoalMinutes,
                bedtimeReminderMinutes = prefs[Keys.BEDTIME_REMINDER_MINUTES] ?: current.bedtimeReminderMinutes,
            )
            val new = transform(old)
            prefs[Keys.IS_24_HOUR] = new.is24HourFormat
            prefs[Keys.DEFAULT_SNOOZE] = new.defaultSnoozeDuration
            prefs[Keys.DEFAULT_GRADUAL_VOLUME] = new.defaultGradualVolume
            prefs[Keys.USE_PHONE_SPEAKERS] = new.usePhoneSpeakers
            prefs[Keys.SHOW_ON_LOCK_SCREEN] = new.showOnLockScreen
            prefs[Keys.UPCOMING_ALARM_MINUTES] = new.upcomingAlarmMinutes
            prefs[Keys.SHOW_NO_ALARMS_WARNING] = new.showNoAlarmsWarning
            prefs[Keys.VACATION_ENABLED] = new.vacationModeEnabled
            prefs[Keys.VACATION_START] = new.vacationStartMillis
            prefs[Keys.VACATION_END] = new.vacationEndMillis
            prefs[Keys.SHOW_WEATHER] = new.showWeatherOnDashboard
            prefs[Keys.SHOW_CALENDAR] = new.showCalendarOnDashboard
            prefs[Keys.LAST_LATITUDE] = new.lastKnownLatitude
            prefs[Keys.LAST_LONGITUDE] = new.lastKnownLongitude
            prefs[Keys.AUTO_SILENCE] = new.autoSilenceMinutes
            prefs[Keys.TEMPERATURE_UNIT] = new.temperatureUnit
            prefs[Keys.LOCATION_NAME] = new.locationName
            prefs[Keys.USE_MANUAL_LOCATION] = new.useManualLocation
            prefs[Keys.BEDTIME_ENABLED] = new.bedtimeEnabled
            prefs[Keys.BEDTIME_HOUR] = new.bedtimeHour
            prefs[Keys.BEDTIME_MINUTE] = new.bedtimeMinute
            prefs[Keys.SLEEP_GOAL_HOURS] = new.sleepGoalHours
            prefs[Keys.SLEEP_GOAL_MINUTES] = new.sleepGoalMinutes
            prefs[Keys.BEDTIME_REMINDER_MINUTES] = new.bedtimeReminderMinutes
        }
    }
}
