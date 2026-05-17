package com.sysadmindoc.alarmclock.data.preferences

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alarm_settings")

const val DEFAULT_NEWS_FEED_URL = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en"

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
    // v1.10.5: Own an app-created alarms-only DND rule for the sleep window.
    val bedtimeDndEnabled: Boolean = false,
    // F2: Flip-to-snooze (global toggle)
    val flipToSnoozeEnabled: Boolean = false,
    // F11: Webhook integrations
    val webhookEnabled: Boolean = false,
    val webhookUrl: String = "",
    // F13: Public holiday auto-skip
    val holidayAutoSkipEnabled: Boolean = false,
    val holidayCountryCode: String = "",  // ISO 3166-1 alpha-2 (e.g. "US", "GB")
    // F15: Philips Hue
    val hueBridgeIp: String = "",
    val hueApiKey: String = "",
    val hueLightIds: String = "",         // Comma-separated Hue light IDs
    // v1.2.0: Accent color (hex)
    val accentColor: String = "#5B9EF4",
    // v1.2.0: Adaptive challenge difficulty
    val adaptiveDifficultyEnabled: Boolean = false,
    // v1.2.0: Calendar auto-alarm
    val calendarAutoAlarmEnabled: Boolean = false,
    val calendarAutoAlarmMinutesBefore: Int = 60,
    // v1.2.0: Guardian defaults
    val guardianContactName: String = "",
    val guardianContactPhone: String = "",
    // v1.2.0: Custom typing phrases (newline-separated, appended to built-in)
    val customTypingPhrases: String = "",
    // v1.2.0: Night clock mode
    val nightClockEnabled: Boolean = false,
    // v1.2.0: Motivational quotes on alarm screen
    val showMotivationalQuotes: Boolean = true,
    // v1.4.0: Use Android 12+ Material You dynamic color palette (overrides accent)
    val dynamicColorEnabled: Boolean = false,
    // v1.10.9: Opt-in bolder shape rhythm and expressive accent semantics.
    val expressiveModeEnabled: Boolean = false,
    // v1.4.0: Proximity-sensor "cover phone to snooze" (global toggle, pairs with flip-to-snooze)
    val coverToSnoozeEnabled: Boolean = false,
    // v1.4.0: Pre-sleep bedtime checklist (newline-separated items; shown on Bedtime tab)
    val bedtimeChecklist: String = "",
    // v1.4.0: Sleep-sound auto-fade timer in minutes (0 = disabled)
    val sleepSoundTimerMinutes: Int = 0,
    // v1.4.0: Fade-out duration of the sleep-sound timer in seconds
    val sleepSoundFadeSeconds: Int = 60,
    // v1.4.0: Repeat missed alarms — re-fire briefly on next unlock if recent miss
    val repeatMissedAlarms: Boolean = true,
    // v1.4.0: Nap mode default duration (minutes) surfaced from the dashboard FAB
    val napDefaultMinutes: Int = 20,
    // v1.7.1: Bottom-nav visibility — Alarms and Settings always show; the
    // other three tabs can be hidden by users who don't use them.
    val showDashboardTab: Boolean = true,
    val showTimerTab: Boolean = true,
    val showWorldClockTab: Boolean = true,
    // v1.8.0: Show News tab (RSS feed reader, Google News default).
    val showNewsTab: Boolean = true,
    // v1.8.0: Show the Windy radar embed at the bottom of the Weather tab.
    val showRadarEmbed: Boolean = true,
    // v1.8.0: News feed source URL. Defaults to Google News top stories,
    // but power users can paste any RSS/Atom URL — Rome handles all three
    // major feed flavors.
    val newsFeedUrl: String = DEFAULT_NEWS_FEED_URL,
    // v1.11.6 (roadmap N6): "Pause all alarms" single-tap suspend. Distinct
    // from vacation mode — vacation requires a start+end date and only
    // touches repeating alarms; this hard-suspends every alarm (one-shots
    // included) until [pauseUntilMillis]. 0 = not paused. Once `pauseUntilMillis`
    // is in the past, the value is treated as expired and alarms resume on the
    // next reschedule pass.
    val pauseUntilMillis: Long = 0,
    // v1.13.1 (roadmap N12 + N13): user opt-in for Health Connect sleep
    // session reads. Defaults false because Health Connect requires its
    // own per-data-type permission grant (handled in a future release;
    // the first-pass UI surfaces the toggle + a status row but does not
    // yet pull data — see HealthConnectGateway). The Play Console health
    // permissions policy refresh required to actually ship the data path
    // is tracked alongside (roadmap N13).
    val healthConnectEnabled: Boolean = false,
)

private fun AppSettings.sanitized(): AppSettings {
    val normalizedVacationStart = vacationStartMillis.coerceAtLeast(0)
    val normalizedVacationEnd = vacationEndMillis.coerceAtLeast(0)
    val vacationWindowValid =
        normalizedVacationStart > 0 && normalizedVacationEnd > normalizedVacationStart
    val normalizedAccent = accentColor.trim().takeIf {
        it.startsWith("#") && runCatching { Color.parseColor(it) }.isSuccess
    } ?: "#5B9EF4"
    val normalizedTemperatureUnit =
        if (temperatureUnit.equals("celsius", ignoreCase = true)) "celsius" else "fahrenheit"
    val normalizedHolidayCountryCode = holidayCountryCode
        .trim()
        .uppercase(Locale.US)
        .filter(Char::isLetter)
        .take(2)
    val normalizedCustomTypingPhrases = customTypingPhrases
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
    val normalizedBedtimeChecklist = bedtimeChecklist
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
    val normalizedLocationName = locationName.trim().take(120)

    val normalizedPauseUntil = pauseUntilMillis.coerceAtLeast(0)
    return copy(
        defaultSnoozeDuration = defaultSnoozeDuration.coerceIn(1, 180),
        defaultGradualVolume = defaultGradualVolume.coerceIn(0, 300),
        upcomingAlarmMinutes = upcomingAlarmMinutes.coerceIn(0, 1_440),
        vacationModeEnabled = vacationModeEnabled && vacationWindowValid,
        vacationStartMillis = normalizedVacationStart,
        vacationEndMillis = normalizedVacationEnd,
        lastKnownLatitude = lastKnownLatitude.coerceIn(-90.0, 90.0),
        lastKnownLongitude = lastKnownLongitude.coerceIn(-180.0, 180.0),
        autoSilenceMinutes = autoSilenceMinutes.coerceIn(0, 240),
        temperatureUnit = normalizedTemperatureUnit,
        locationName = normalizedLocationName,
        useManualLocation = useManualLocation && normalizedLocationName.isNotBlank(),
        bedtimeHour = bedtimeHour.coerceIn(0, 23),
        bedtimeMinute = bedtimeMinute.coerceIn(0, 59),
        sleepGoalHours = sleepGoalHours.coerceIn(1, 16),
        sleepGoalMinutes = sleepGoalMinutes.coerceIn(0, 59),
        bedtimeReminderMinutes = bedtimeReminderMinutes.coerceIn(0, 180),
        webhookUrl = webhookUrl.trim(),
        holidayCountryCode = normalizedHolidayCountryCode,
        hueBridgeIp = hueBridgeIp.trim(),
        hueApiKey = hueApiKey.trim(),
        hueLightIds = hueLightIds.trim(),
        accentColor = normalizedAccent,
        calendarAutoAlarmMinutesBefore = calendarAutoAlarmMinutesBefore.coerceIn(0, 720),
        guardianContactName = guardianContactName.trim().take(80),
        guardianContactPhone = guardianContactPhone.trim().take(40),
        customTypingPhrases = normalizedCustomTypingPhrases,
        bedtimeChecklist = normalizedBedtimeChecklist,
        sleepSoundTimerMinutes = sleepSoundTimerMinutes.coerceIn(0, 240),
        sleepSoundFadeSeconds = sleepSoundFadeSeconds.coerceIn(5, 600),
        napDefaultMinutes = napDefaultMinutes.coerceIn(1, 180),
        pauseUntilMillis = normalizedPauseUntil
    )
}

/**
 * v1.11.6 (roadmap N6): true if the user has currently paused all alarms via
 * the "Pause for N days" Settings action. Stale pauses (the timestamp is in
 * the past) are treated as expired so resume happens lazily on the next
 * schedule pass without needing a scheduled wake-up to clear the flag.
 */
fun AppSettings.isPaused(now: Long = System.currentTimeMillis()): Boolean =
    pauseUntilMillis > now

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // v1.5.1: Lock-free latest-settings snapshot kept in sync by [settings]'s
    // collectors. Callers that can't suspend (scheduler math from a `combine`
    // block, sensor callbacks, receivers that need a quick peek) read this
    // instead of blocking DataStore. Initialised with defaults so the first
    // read before DataStore has emitted is still safe.
    @Volatile
    private var cachedSettings: AppSettings = AppSettings()

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
        val BEDTIME_DND_ENABLED = booleanPreferencesKey("bedtime_dnd_enabled")
        val FLIP_TO_SNOOZE = booleanPreferencesKey("flip_to_snooze")
        val WEBHOOK_ENABLED = booleanPreferencesKey("webhook_enabled")
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val HOLIDAY_AUTO_SKIP = booleanPreferencesKey("holiday_auto_skip")
        val HOLIDAY_COUNTRY_CODE = stringPreferencesKey("holiday_country_code")
        val HUE_BRIDGE_IP = stringPreferencesKey("hue_bridge_ip")
        val HUE_API_KEY = stringPreferencesKey("hue_api_key")
        val HUE_LIGHT_IDS = stringPreferencesKey("hue_light_ids")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ADAPTIVE_DIFFICULTY = booleanPreferencesKey("adaptive_difficulty")
        val CALENDAR_AUTO_ALARM = booleanPreferencesKey("calendar_auto_alarm")
        val CALENDAR_AUTO_ALARM_MINUTES = intPreferencesKey("calendar_auto_alarm_minutes")
        val GUARDIAN_CONTACT_NAME = stringPreferencesKey("guardian_contact_name")
        val GUARDIAN_CONTACT_PHONE = stringPreferencesKey("guardian_contact_phone")
        val CUSTOM_TYPING_PHRASES = stringPreferencesKey("custom_typing_phrases")
        val NIGHT_CLOCK = booleanPreferencesKey("night_clock")
        val SHOW_MOTIVATIONAL_QUOTES = booleanPreferencesKey("show_motivational_quotes")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val EXPRESSIVE_MODE = booleanPreferencesKey("expressive_mode")
        val COVER_TO_SNOOZE = booleanPreferencesKey("cover_to_snooze")
        val BEDTIME_CHECKLIST = stringPreferencesKey("bedtime_checklist")
        val SLEEP_SOUND_TIMER = intPreferencesKey("sleep_sound_timer_minutes")
        val SLEEP_SOUND_FADE = intPreferencesKey("sleep_sound_fade_seconds")
        val REPEAT_MISSED_ALARMS = booleanPreferencesKey("repeat_missed_alarms")
        val NAP_DEFAULT_MINUTES = intPreferencesKey("nap_default_minutes")
        val SHOW_DASHBOARD_TAB = booleanPreferencesKey("show_dashboard_tab")
        val SHOW_TIMER_TAB = booleanPreferencesKey("show_timer_tab")
        val SHOW_WORLD_CLOCK_TAB = booleanPreferencesKey("show_world_clock_tab")
        val SHOW_NEWS_TAB = booleanPreferencesKey("show_news_tab")
        val SHOW_RADAR_EMBED = booleanPreferencesKey("show_radar_embed")
        val NEWS_FEED_URL = stringPreferencesKey("news_feed_url")
        val PAUSE_UNTIL = longPreferencesKey("pause_until_millis")
        val HEALTH_CONNECT_ENABLED = booleanPreferencesKey("health_connect_enabled")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) {
                // v1.9.1: DataStore reports IOException on both transient I/O
                // failures and on a corrupted preferences file (which it
                // recovers from by emitting empty preferences). Either way the
                // user sees their settings reset to defaults, including
                // sensitive ones like webhookUrl/hueApiKey/newsFeedUrl. Log so
                // we have a breadcrumb instead of a silent factory-reset.
                Log.w(
                    "PreferencesManager",
                    "DataStore read failed; emitting defaults this collection",
                    e
                )
                emit(emptyPreferences())
            } else throw e
        }
        .map { it.toSettings().sanitized() }
        .onEach { cachedSettings = it }

    suspend fun getCurrentSettings(): AppSettings = settings.first()

    /**
     * v1.5.1: Non-suspend snapshot for callers that cannot afford a DataStore
     * round-trip (e.g., [NextAlarmCalculator.solarTimeFor] invoked from a
     * ViewModel `combine { }` running on Dispatchers.Main). Writes refresh this
     * snapshot inside the same edit block so immediate reschedules see the new
     * settings without waiting for the next DataStore flow emission.
     */
    fun getCachedSettings(): AppSettings = cachedSettings

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val old = prefs.toSettings().sanitized()
            val new = transform(old).sanitized()
            cachedSettings = new
            prefs.applySettings(new)
        }
    }

    /** Decode a Preferences snapshot into an AppSettings using the same defaults
     *  applied by the data class. Centralised so [settings] and [update] can't
     *  drift from each other (a previous source of bugs where new fields would
     *  reset to default during update because only [settings] knew about them). */
    private fun Preferences.toSettings(): AppSettings = AppSettings(
        is24HourFormat = this[Keys.IS_24_HOUR] ?: false,
        defaultSnoozeDuration = this[Keys.DEFAULT_SNOOZE] ?: 10,
        defaultGradualVolume = this[Keys.DEFAULT_GRADUAL_VOLUME] ?: 60,
        usePhoneSpeakers = this[Keys.USE_PHONE_SPEAKERS] ?: false,
        showOnLockScreen = this[Keys.SHOW_ON_LOCK_SCREEN] ?: true,
        upcomingAlarmMinutes = this[Keys.UPCOMING_ALARM_MINUTES] ?: 60,
        showNoAlarmsWarning = this[Keys.SHOW_NO_ALARMS_WARNING] ?: true,
        vacationModeEnabled = this[Keys.VACATION_ENABLED] ?: false,
        vacationStartMillis = this[Keys.VACATION_START] ?: 0,
        vacationEndMillis = this[Keys.VACATION_END] ?: 0,
        showWeatherOnDashboard = this[Keys.SHOW_WEATHER] ?: true,
        showCalendarOnDashboard = this[Keys.SHOW_CALENDAR] ?: true,
        lastKnownLatitude = this[Keys.LAST_LATITUDE] ?: 0.0,
        lastKnownLongitude = this[Keys.LAST_LONGITUDE] ?: 0.0,
        autoSilenceMinutes = this[Keys.AUTO_SILENCE] ?: 10,
        temperatureUnit = this[Keys.TEMPERATURE_UNIT] ?: "fahrenheit",
        locationName = this[Keys.LOCATION_NAME] ?: "",
        useManualLocation = this[Keys.USE_MANUAL_LOCATION] ?: false,
        bedtimeEnabled = this[Keys.BEDTIME_ENABLED] ?: false,
        bedtimeHour = this[Keys.BEDTIME_HOUR] ?: 23,
        bedtimeMinute = this[Keys.BEDTIME_MINUTE] ?: 0,
        sleepGoalHours = this[Keys.SLEEP_GOAL_HOURS] ?: 8,
        sleepGoalMinutes = this[Keys.SLEEP_GOAL_MINUTES] ?: 0,
        bedtimeReminderMinutes = this[Keys.BEDTIME_REMINDER_MINUTES] ?: 30,
        bedtimeDndEnabled = this[Keys.BEDTIME_DND_ENABLED] ?: false,
        flipToSnoozeEnabled = this[Keys.FLIP_TO_SNOOZE] ?: false,
        webhookEnabled = this[Keys.WEBHOOK_ENABLED] ?: false,
        webhookUrl = this[Keys.WEBHOOK_URL] ?: "",
        holidayAutoSkipEnabled = this[Keys.HOLIDAY_AUTO_SKIP] ?: false,
        holidayCountryCode = this[Keys.HOLIDAY_COUNTRY_CODE] ?: "",
        hueBridgeIp = this[Keys.HUE_BRIDGE_IP] ?: "",
        hueApiKey = this[Keys.HUE_API_KEY] ?: "",
        hueLightIds = this[Keys.HUE_LIGHT_IDS] ?: "",
        accentColor = this[Keys.ACCENT_COLOR] ?: "#5B9EF4",
        adaptiveDifficultyEnabled = this[Keys.ADAPTIVE_DIFFICULTY] ?: false,
        calendarAutoAlarmEnabled = this[Keys.CALENDAR_AUTO_ALARM] ?: false,
        calendarAutoAlarmMinutesBefore = this[Keys.CALENDAR_AUTO_ALARM_MINUTES] ?: 60,
        guardianContactName = this[Keys.GUARDIAN_CONTACT_NAME] ?: "",
        guardianContactPhone = this[Keys.GUARDIAN_CONTACT_PHONE] ?: "",
        customTypingPhrases = this[Keys.CUSTOM_TYPING_PHRASES] ?: "",
        nightClockEnabled = this[Keys.NIGHT_CLOCK] ?: false,
        showMotivationalQuotes = this[Keys.SHOW_MOTIVATIONAL_QUOTES] ?: true,
        dynamicColorEnabled = this[Keys.DYNAMIC_COLOR] ?: false,
        expressiveModeEnabled = this[Keys.EXPRESSIVE_MODE] ?: false,
        coverToSnoozeEnabled = this[Keys.COVER_TO_SNOOZE] ?: false,
        bedtimeChecklist = this[Keys.BEDTIME_CHECKLIST] ?: "",
        sleepSoundTimerMinutes = this[Keys.SLEEP_SOUND_TIMER] ?: 0,
        sleepSoundFadeSeconds = this[Keys.SLEEP_SOUND_FADE] ?: 60,
        repeatMissedAlarms = this[Keys.REPEAT_MISSED_ALARMS] ?: true,
        napDefaultMinutes = this[Keys.NAP_DEFAULT_MINUTES] ?: 20,
        showDashboardTab = this[Keys.SHOW_DASHBOARD_TAB] ?: true,
        showTimerTab = this[Keys.SHOW_TIMER_TAB] ?: true,
        showWorldClockTab = this[Keys.SHOW_WORLD_CLOCK_TAB] ?: true,
        showNewsTab = this[Keys.SHOW_NEWS_TAB] ?: true,
        showRadarEmbed = this[Keys.SHOW_RADAR_EMBED] ?: true,
        newsFeedUrl = this[Keys.NEWS_FEED_URL] ?: DEFAULT_NEWS_FEED_URL,
        pauseUntilMillis = this[Keys.PAUSE_UNTIL] ?: 0L,
        healthConnectEnabled = this[Keys.HEALTH_CONNECT_ENABLED] ?: false,
    )

    private fun MutablePreferences.applySettings(s: AppSettings) {
        this[Keys.IS_24_HOUR] = s.is24HourFormat
        this[Keys.DEFAULT_SNOOZE] = s.defaultSnoozeDuration
        this[Keys.DEFAULT_GRADUAL_VOLUME] = s.defaultGradualVolume
        this[Keys.USE_PHONE_SPEAKERS] = s.usePhoneSpeakers
        this[Keys.SHOW_ON_LOCK_SCREEN] = s.showOnLockScreen
        this[Keys.UPCOMING_ALARM_MINUTES] = s.upcomingAlarmMinutes
        this[Keys.SHOW_NO_ALARMS_WARNING] = s.showNoAlarmsWarning
        this[Keys.VACATION_ENABLED] = s.vacationModeEnabled
        this[Keys.VACATION_START] = s.vacationStartMillis
        this[Keys.VACATION_END] = s.vacationEndMillis
        this[Keys.SHOW_WEATHER] = s.showWeatherOnDashboard
        this[Keys.SHOW_CALENDAR] = s.showCalendarOnDashboard
        this[Keys.LAST_LATITUDE] = s.lastKnownLatitude
        this[Keys.LAST_LONGITUDE] = s.lastKnownLongitude
        this[Keys.AUTO_SILENCE] = s.autoSilenceMinutes
        this[Keys.TEMPERATURE_UNIT] = s.temperatureUnit
        this[Keys.LOCATION_NAME] = s.locationName
        this[Keys.USE_MANUAL_LOCATION] = s.useManualLocation
        this[Keys.BEDTIME_ENABLED] = s.bedtimeEnabled
        this[Keys.BEDTIME_HOUR] = s.bedtimeHour
        this[Keys.BEDTIME_MINUTE] = s.bedtimeMinute
        this[Keys.SLEEP_GOAL_HOURS] = s.sleepGoalHours
        this[Keys.SLEEP_GOAL_MINUTES] = s.sleepGoalMinutes
        this[Keys.BEDTIME_REMINDER_MINUTES] = s.bedtimeReminderMinutes
        this[Keys.BEDTIME_DND_ENABLED] = s.bedtimeDndEnabled
        this[Keys.FLIP_TO_SNOOZE] = s.flipToSnoozeEnabled
        this[Keys.WEBHOOK_ENABLED] = s.webhookEnabled
        this[Keys.WEBHOOK_URL] = s.webhookUrl
        this[Keys.HOLIDAY_AUTO_SKIP] = s.holidayAutoSkipEnabled
        this[Keys.HOLIDAY_COUNTRY_CODE] = s.holidayCountryCode
        this[Keys.HUE_BRIDGE_IP] = s.hueBridgeIp
        this[Keys.HUE_API_KEY] = s.hueApiKey
        this[Keys.HUE_LIGHT_IDS] = s.hueLightIds
        this[Keys.ACCENT_COLOR] = s.accentColor
        this[Keys.ADAPTIVE_DIFFICULTY] = s.adaptiveDifficultyEnabled
        this[Keys.CALENDAR_AUTO_ALARM] = s.calendarAutoAlarmEnabled
        this[Keys.CALENDAR_AUTO_ALARM_MINUTES] = s.calendarAutoAlarmMinutesBefore
        this[Keys.GUARDIAN_CONTACT_NAME] = s.guardianContactName
        this[Keys.GUARDIAN_CONTACT_PHONE] = s.guardianContactPhone
        this[Keys.CUSTOM_TYPING_PHRASES] = s.customTypingPhrases
        this[Keys.NIGHT_CLOCK] = s.nightClockEnabled
        this[Keys.SHOW_MOTIVATIONAL_QUOTES] = s.showMotivationalQuotes
        this[Keys.DYNAMIC_COLOR] = s.dynamicColorEnabled
        this[Keys.EXPRESSIVE_MODE] = s.expressiveModeEnabled
        this[Keys.COVER_TO_SNOOZE] = s.coverToSnoozeEnabled
        this[Keys.BEDTIME_CHECKLIST] = s.bedtimeChecklist
        this[Keys.SLEEP_SOUND_TIMER] = s.sleepSoundTimerMinutes
        this[Keys.SLEEP_SOUND_FADE] = s.sleepSoundFadeSeconds
        this[Keys.REPEAT_MISSED_ALARMS] = s.repeatMissedAlarms
        this[Keys.NAP_DEFAULT_MINUTES] = s.napDefaultMinutes
        this[Keys.SHOW_DASHBOARD_TAB] = s.showDashboardTab
        this[Keys.SHOW_TIMER_TAB] = s.showTimerTab
        this[Keys.SHOW_WORLD_CLOCK_TAB] = s.showWorldClockTab
        this[Keys.SHOW_NEWS_TAB] = s.showNewsTab
        this[Keys.SHOW_RADAR_EMBED] = s.showRadarEmbed
        this[Keys.NEWS_FEED_URL] = s.newsFeedUrl
        this[Keys.PAUSE_UNTIL] = s.pauseUntilMillis
        this[Keys.HEALTH_CONNECT_ENABLED] = s.healthConnectEnabled
    }
}
