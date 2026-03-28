# AlarmClockXtreme - Working Notes

## Tech Stack
- Kotlin 2.1, Jetpack Compose (Material 3), Room, Hilt, Retrofit + Moshi (codegen), DataStore, Glance widgets
- OkHttp 4.12.0 (explicit dep — used by WebhookService, HueSunriseWorker, Hue test in SettingsViewModel)
- minSdk 26, targetSdk 35, compileSdk 35
- Build: `./gradlew assembleDebug` (play/fdroid flavors)

## Key Paths
- App entry: `AlarmClockApp.kt` (implements `Configuration.Provider` for HiltWorkerFactory), `MainActivity.kt`
- Navigation: `ui/navigation/AppNavigation.kt` — bottom nav: My Day, Alarm, Timer, World Clock, Settings
- Alarm model: `data/model/Alarm.kt` — Room entity, 27+ fields including all F1-F17 feature columns
- DB: `data/local/AlarmDatabase.kt` — **version 5**, migrations 1→5 (MIGRATION_4_5 adds 13 feature columns)
- Scheduling: `domain/AlarmScheduler.kt` — uses `setAlarmClock()` for reliability
- Firing: `service/AlarmService.kt` (foreground, `mediaPlayback`) + `ui/alarmfiring/AlarmFiringActivity.kt`
- Preferences: `data/preferences/PreferencesManager.kt` — DataStore-backed `AppSettings`
- Weather: Open-Meteo API (no key), `data/remote/WeatherApi.kt`
- Holidays: Nager.Date API (`https://date.nager.at/`), `data/remote/HolidayApi.kt`
- Backup: `data/backup/BackupManager.kt` — v2 format with all F1-F17 fields + full settings (backward-compatible with v1)

## Architecture
- UI Layer: Compose screens with ViewModels + StateFlow
- Domain: AlarmScheduler, NextAlarmCalculator
- Data: Room DB + DataStore + Retrofit
- DI: Hilt (DatabaseModule, NetworkModule)
- WorkManager: requires `HiltWorkerFactory` — default initializer disabled via manifest `tools:node="remove"` on `WorkManagerInitializer`

## Feature Map (F1-F17, all implemented)
| Feature | Key Files |
|---------|-----------|
| F1: Barcode dismiss | AlarmFiringActivity, ChallengeViews, Alarm.barcodeValue |
| F2: NFC dismiss | AlarmFiringActivity (foreground dispatch), Alarm.nfcTagId |
| F3: Flip-to-snooze | AlarmFiringActivity, FlipDetector, AppSettings.flipToSnoozeEnabled |
| F4: Walk steps dismiss | AlarmFiringActivity, StepCounterListener, Alarm.walkStepsRequired |
| F5: Wake confirmation | AlarmService.scheduleWakeConfirmation(), WakeConfirmWorker, WakeConfirmActivity |
| F6: Smart alarm window | SmartAlarmService (accelerometer), AlarmScheduler (schedules via AlarmManager) |
| F7: Math challenge | ChallengeViews, ChallengeGenerator |
| F8: Webhook / Tasker | WebhookService (fire on fired/snoozed/dismissed/missed/test) |
| F9: Sleep cycle calc | BedtimeViewModel.computeSleepCycles() — 90-min cycles, 15-min offset |
| F10: Sleep sounds | SleepSoundPlayer, BedtimeScreen, res/raw/*.wav |
| F11: TTS announcement | AlarmService.speakMorningAnnouncement() — fires after dismiss |
| F12: Morning briefing | MorningBriefingActivity (launched from AlarmService after dismiss) |
| F13: Holiday auto-skip | HolidayRepository, HolidaySyncWorker (weekly), AlarmScheduler |
| F14: Spotify ringtone | AlarmService.startAudio() — Intent(ACTION_VIEW, spotifyUri); falls back to MediaPlayer |
| F15: Philips Hue | HueSunriseWorker (WorkManager), AlarmScheduler enqueue with setInitialDelay |
| F16: Photo match dismiss | AlarmFiringActivity, PhotoMatcher |
| F17: Sonar sleep tracking | SonarSleepService (AudioTrack 18.75 kHz + AudioRecord, `microphone` service type) |

## DB Schema (version 5)
### `alarms` table
id, hour, minute, label, isEnabled, repeatDays, ringtoneUri, vibrationEnabled, vibrationIntensity,
volume, overrideSystemVolume, gradualVolumeSeconds, snoozeDurationMinutes, maxSnoozeCount,
showOnLockScreen, challengeType, group, flashWake, vibrationPattern, createdAt, nextTriggerTime,
ttsEnabled, walkStepsRequired, wakeConfirmEnabled, wakeConfirmDelayMinutes, smartAlarmEnabled,
smartAlarmWindowMinutes, skipOnHolidays, nfcTagId, barcodeValue, spotifyUri, hueEnabled,
huePreWakeMinutes, photoMatchUri

### `alarm_events` table
id, alarmId, alarmLabel, scheduledTime, firedAt, action (DISMISSED/SNOOZED/SKIPPED/MISSED),
actionAt, challengeType, challengeSolveTimeMs, snoozeCount, dayOfWeek

## AppSettings (DataStore — PreferencesManager)
is24HourFormat, defaultSnoozeDuration, defaultGradualVolume, usePhoneSpeakers, showOnLockScreen,
vacationModeEnabled, vacationStartMillis, vacationEndMillis, showWeatherOnDashboard,
showCalendarOnDashboard, lastKnownLatitude, lastKnownLongitude, autoSilenceMinutes, temperatureUnit,
bedtimeEnabled, bedtimeHour, bedtimeMinute, sleepGoalHours, sleepGoalMinutes, bedtimeReminderMinutes,
flipToSnoozeEnabled, webhookEnabled, webhookUrl, holidayAutoSkipEnabled, holidayCountryCode,
hueBridgeIp, hueApiKey, hueLightIds

## Services & Workers
| Component | Foreground Type | Purpose |
|-----------|----------------|---------|
| AlarmService | mediaPlayback | Alarm firing, audio, vibration |
| SmartAlarmService | dataSync | Accelerometer motion monitoring (F6) |
| SonarSleepService | microphone | Sonar sleep tracking (F17, experimental) |
| HueSunriseWorker | WorkManager | Philips Hue brightness ramp (F15) |
| HolidaySyncWorker | WorkManager | Weekly holiday data refresh (F13) |
| WakeConfirmWorker | WorkManager | Post-alarm wake confirmation (F5) |

## DB Schema (version 6)
Note: DB version bumped from 5 to 6 in v1.2.0. MIGRATION_5_6 adds 21 columns for competitive features.

## Version History
- v1.2.0 (2026-03-28): 30 competitive features. Mission chaining, backup sound escalation, progressive snooze, squat/maze/wifi challenges, guardian angel, sunrise simulation, internet radio, flashlight strobe, morning routine tracker, early dismiss, date-specific alarms, alarm profiles, motivational quotes, accent color, adaptive difficulty, night clock, custom typing phrases, calendar auto-alarm, location dismiss. DB v6. 2 new files (GuardianWorker, SquatDetector). 8 new AlarmEditScreen sections.
- v1.1.0 (2026-03-28): 56-issue comprehensive audit. Fixed: MediaPlayer NPE race, auto-silence job leak, double-stopForeground crash, notification ID collision (SmartAlarm 2003, NextAlarmNotifier 2004), Sonar audio leak + false-positive deep sleep, backup data loss (16 alarm fields + 15 settings), widget missing migrations, Converters crash on malformed data, PreferencesManager update() baseline, TTS service destroy race, HolidaySyncWorker infinite retry, world clock/stats 24h format, stopwatch lap split math, math challenge negative choices + precedence, timer monotonic clock, HolidayRepository thread safety, network timeouts, DAO singletons, CrashLogger uniqueness, SleepSoundPlayer fade off-by-one, BedtimeReceiver settings check, accessibility labels.
- v1.0.0 (2026-03-23): All F1-F17 features complete. Bug fixes: SmartAlarmService action, holiday skip for repeating alarms, TTS race condition, wake lock duration, webhook JSON, SonarSleepService drawable. Improvements: Skip Next snackbar feedback, snooze duration in notification, sleep sound placeholder WAVs.
- v1.0.0 (2026-03-20): Custom snooze on firing screen, multi-select batch ops, memory challenge fix, media fallback
- v0.9.0 (2026-03-20): Groups, duplicate, world clock, timers, vibration patterns, flash wake, swipe gestures
- v0.8.1 (2026-02-22): Auto-silence, bedtime persistence, widget fixes, temp units
- v0.8.0: Swipe-to-delete, search/sort, auto-silence, manual location
- v0.7.0: Onboarding, stats, backup/restore, bedtime
- v0.6.0: Ringtone picker, templates, widget, persistent notification
- v0.5.0: Challenges, vacation mode, manufacturer compat

## Gotchas & Non-Obvious Details

### Scheduling
- `AlarmScheduler.schedule()` uses `var triggerTime` (not val) — holiday skip logic may advance it through consecutive holidays (up to 14 iterations).
- SmartAlarm PendingIntent uses `alarmId + 50000` as request code to avoid collision with the main alarm PendingIntent (which uses `alarmId`).
- `cancel()` must cancel both the main alarm PendingIntent AND the SmartAlarm PendingIntent — the SmartAlarm uses `FLAG_NO_CREATE` to safely no-op if not scheduled.
- SmartAlarmService `onStartCommand` returns `START_NOT_STICKY` immediately if `intent.action != ACTION_START_SMART` — always set the action on the intent before starting/scheduling.
- `scheduleExact()` (used by `rescheduleAll()`) skips holiday checking intentionally — it preserves existing valid future triggers.

### WorkManager + Hilt
- `AlarmClockApp` implements `Configuration.Provider` and injects `HiltWorkerFactory` — this replaces the default WorkManager initializer.
- The default `WorkManagerInitializer` is removed in the manifest via `tools:node="remove"` on the `InitializationProvider` meta-data. Without this, WorkManager ignores the custom factory and `@HiltWorker` injection fails silently.

### TTS
- `speakMorningAnnouncement()` uses `AtomicReference<TextToSpeech?>` + `OnInitListener` callback because TTS init is async. Never call `speak()` synchronously after construction.
- Minute formatting: 0 → "o'clock", 1-9 → "oh N", 10+ → "N" (verbatim).

### Audio
- Wake lock in `AlarmService.onCreate()` is set to 30 minutes (covers max auto-silence). It is always released in `onDestroy()`.
- `SleepSoundPlayer` uses `MediaPlayer.create(context, rawResId)` — returns null if resource ID is 0, so missing raw files don't crash; the BedtimeScreen tiles just appear disabled.
- Sleep sound files in `res/raw/` are **silent placeholder WAVs** (8000 Hz, mono, 8-bit, 1 second). Replace with real ambient audio.
- Spotify integration: `Intent(ACTION_VIEW, spotifyUri)` with `FLAG_ACTIVITY_NEW_TASK` + `android.intent.extra.START_PLAYBACK`. Falls back to MediaPlayer if Spotify not installed or URI invalid.

### Webhook
- `WebhookService.buildJson()` uses `org.json.JSONObject` (built-in Android) — not string interpolation. Required for correct escaping of control characters in alarm labels.

### Notifications
- All notification icons use `R.drawable.ic_alarm` (only alarm icon drawable that exists).
- Snooze action label includes duration: `"Snooze ${alarm.snoozeDurationMinutes}m"`.
- **Notification IDs**: AlarmService=1001, MissedNotification=1003, SonarSleep=2002, SmartAlarm=2003, NextAlarmNotifier=2004, BedtimeReminder=3001, WakeConfirm=5000+. No collisions.

### Skip Next
- `AlarmListViewModel.skipNextOccurrence()` only works for repeating alarms (`repeatDays.isNotEmpty()`).
- After skip, emits to `skipFeedbackEvents: SharedFlow<String>` — collected in `AlarmListScreen` via `LaunchedEffect(Unit)` to show a snackbar with the next scheduled date.
- Uses `nextTriggerTime + 60_000` (1 minute) as the `fromTime` for `calculator.calculate()` so the next occurrence lands after the skipped one.

### Vibration
- Patterns defined in `AlarmService.startVibration()` as `longArray` (timing) + `intArray` (amplitudes) pairs.
- Pattern names: "default", "gentle", "heartbeat", "escalating", "sos".

### Misc
- Flash wake ramps screen brightness in `AlarmFiringActivity` via `window.attributes.screenBrightness`.
- World clock uses Java `ZoneId` for timezone data — no external API.
- Timer supports multiple concurrent instances — `TimerViewModel` tracks `List<TimerInstance>`.
- `AlarmCard` and `AlarmEditScreen` both respect `is24HourFormat` from `PreferencesManager`.
- HolidayRepository caches dates in `filesDir/holiday_cache.txt` as newline-delimited ISO-8601 strings. Meta file at `holiday_cache_meta.txt` stores `"countryCode|timestampMillis"`.
