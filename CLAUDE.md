# AlarmClockXtreme - Working Notes

## Tech Stack
- Kotlin 2.1, Jetpack Compose (Material 3), Room, Hilt, Retrofit + Moshi (codegen), DataStore, Glance widgets
- OkHttp 4.12.0 (explicit dep -- WebhookService, HueSunriseWorker, Hue test in SettingsViewModel)
- minSdk 26, targetSdk 35, compileSdk 35
- Build: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && ./gradlew assemblePlayDebug` (play/fdroid flavors)

## Key Paths
- App entry: `AlarmClockApp.kt` (Configuration.Provider for HiltWorkerFactory), `MainActivity.kt`
- Navigation: `ui/navigation/AppNavigation.kt` -- bottom nav: My Day, Alarm, Timer, World Clock, Settings
- Alarm model: `data/model/Alarm.kt` -- Room entity, 47+ fields (F1-F17 + 20 v1.2.0 competitive fields)
- DB: `data/local/AlarmDatabase.kt` -- **version 6**, migrations 1-6 (MIGRATION_5_6 adds 21 v1.2.0 columns)
- Scheduling: `domain/AlarmScheduler.kt` -- `setAlarmClock()`, vacation/holiday/date-specific skip
- Calculator: `domain/NextAlarmCalculator.kt` -- date-specific alarm priority, then repeatDays logic
- Firing: `service/AlarmService.kt` (foreground, `mediaPlayback`) + `ui/alarmfiring/AlarmFiringActivity.kt`
- Challenges: `ui/alarmfiring/challenges/ChallengeGenerator.kt` + `ChallengeViews.kt` -- 15 challenge types
- Preferences: `data/preferences/PreferencesManager.kt` -- DataStore-backed `AppSettings` (34+ fields)
- Backup: `data/backup/BackupManager.kt` -- v3 format, all alarm fields + all settings
- Weather: Open-Meteo API (no key), `data/remote/WeatherApi.kt`
- Holidays: Nager.Date API, `data/remote/HolidayApi.kt`
- Theme: `ui/theme/Theme.kt` -- `LocalAccentColor` compositionLocal, dynamic accent from settings

## Architecture
- UI Layer: Compose screens + ViewModels + StateFlow
- Domain: AlarmScheduler, NextAlarmCalculator
- Data: Room DB v6 + DataStore + Retrofit
- DI: Hilt (DatabaseModule, NetworkModule) -- DAOs are @Singleton
- WorkManager: HiltWorkerFactory via Configuration.Provider, default initializer disabled in manifest
- Network: Shared OkHttpClient with 15s timeouts via NetworkModule

## Feature Map

### Original (F1-F17)
| Feature | Key Files |
|---------|-----------|
| F1: Barcode dismiss | AlarmFiringActivity, ChallengeViews, Alarm.barcodeValue |
| F2: NFC dismiss | AlarmFiringActivity (foreground dispatch), Alarm.nfcTagId |
| F3: Flip-to-snooze | AlarmFiringActivity, FlipDetector, AppSettings.flipToSnoozeEnabled |
| F4: Walk steps dismiss | AlarmFiringActivity, StepCounterListener, Alarm.walkStepsRequired |
| F5: Wake confirmation | AlarmService, WakeConfirmWorker, WakeConfirmActivity |
| F6: Smart alarm window | SmartAlarmService (accelerometer), AlarmScheduler |
| F7: Math challenge | ChallengeViews, ChallengeGenerator (3 difficulties) |
| F8: Webhook / Tasker | WebhookService (fire on fired/snoozed/dismissed/missed/test) |
| F9: Sleep cycle calc | BedtimeViewModel.computeSleepCycles() |
| F10: Sleep sounds | SleepSoundPlayer, BedtimeScreen, res/raw/*.wav (placeholders) |
| F11: TTS announcement | AlarmService.speakMorningAnnouncement() (applicationContext) |
| F12: Morning briefing | MorningBriefingActivity + morning routine checklist |
| F13: Holiday auto-skip | HolidayRepository (Mutex-guarded), HolidaySyncWorker (3 retries max) |
| F14: Spotify ringtone | AlarmService.startAudio() -- Intent + MediaPlayer fallback |
| F15: Philips Hue | HueSunriseWorker, AlarmScheduler enqueue |
| F16: Photo match dismiss | AlarmFiringActivity, PhotoMatcher |
| F17: Sonar sleep tracking | SonarSleepService (18.75 kHz, experimental) |

### Competitive Features (v1.2.0)
| Feature | Key Files |
|---------|-----------|
| Mission chaining | AlarmFiringViewModel.challengeChainTypes, proceedToNextChallenge() |
| Backup sound escalation | AlarmService.backupSoundJob, max volume after delay |
| Progressive snooze | AlarmService.snoozeAlarm() -- duration - snoozeCount |
| Squat challenge | SquatDetector, AlarmFiringActivity, SquatChallengeView |
| Maze challenge | ChallengeGenerator.generateMaze(), MazeChallengeView, tapMazeCell() |
| Wi-Fi dismiss | WifiChallengeView, AlarmFiringActivity Wi-Fi polling |
| Guardian Angel | GuardianWorker (SMS + call), scheduled/cancelled in AlarmService |
| Sunrise simulation | AlarmFiringActivity.startSunriseSimulation() -- window bg gradient |
| Internet radio | AlarmService.startAudio() -- MediaPlayer.setDataSource(url) |
| Flashlight strobe | AlarmService.startFlashlightStrobe() -- CameraManager torch toggle |
| Calendar auto-alarm | CalendarAutoAlarmWorker, scheduled daily in AlarmClockApp |
| Early dismiss | NextAlarmNotifier -- "Skip this alarm" notification action |
| Date-specific alarms | NextAlarmCalculator -- specificDate parsed before repeatDays |
| Alarm profiles | Alarm.profileName tag field |
| Morning routine | MorningBriefingActivity checklist, AlarmService passes EXTRA_ROUTINE |
| Motivational quotes | AlarmFiringViewModel.MOTIVATIONAL_QUOTES, shown on firing screen |
| Accent color | Theme.kt LocalAccentColor, parsed from AppSettings.accentColor hex |
| Adaptive difficulty | AlarmFiringViewModel -- escalates math if snoozeRate > 50% |
| Night clock | NightClockActivity -- dim red-on-black, min brightness |
| Custom typing phrases | ChallengeGenerator.generate(type, customPhrases) merges with built-in |

## DB Schema (version 6)

### `alarms` table (47 columns)
**Core:** id, hour, minute, label, isEnabled, repeatDays, ringtoneUri, vibrationEnabled, vibrationIntensity, volume, overrideSystemVolume, gradualVolumeSeconds, snoozeDurationMinutes, maxSnoozeCount, showOnLockScreen, challengeType, group, flashWake, vibrationPattern, createdAt, nextTriggerTime

**F1-F17:** ttsEnabled, walkStepsRequired, wakeConfirmEnabled, wakeConfirmDelayMinutes, smartAlarmEnabled, smartAlarmWindowMinutes, skipOnHolidays, nfcTagId, barcodeValue, spotifyUri, hueEnabled, huePreWakeMinutes, photoMatchUri

**v1.2.0:** challengeChain, progressiveSnooze, backupSoundEnabled, backupSoundDelaySec, sunriseSimulation, sunriseMinutes, specificDate, profileName, earlyDismissMinutes, guardianEnabled, guardianPhone, guardianDelaySec, locationDismissEnabled, locationDismissLat, locationDismissLng, locationDismissRadius, wifiDismissSsid, internetRadioUrl, flashlightStrobe, morningRoutine

### `alarm_events` table
id, alarmId, alarmLabel, scheduledTime, firedAt, action, actionAt, challengeType, challengeSolveTimeMs, snoozeCount, dayOfWeek

## AppSettings (DataStore -- 34 fields)
**Core:** is24HourFormat, defaultSnoozeDuration, defaultGradualVolume, usePhoneSpeakers, showOnLockScreen, upcomingAlarmMinutes, showNoAlarmsWarning, vacationModeEnabled, vacationStartMillis, vacationEndMillis, showWeatherOnDashboard, showCalendarOnDashboard, lastKnownLatitude, lastKnownLongitude, autoSilenceMinutes, temperatureUnit, locationName, useManualLocation, bedtimeEnabled, bedtimeHour, bedtimeMinute, sleepGoalHours, sleepGoalMinutes, bedtimeReminderMinutes, flipToSnoozeEnabled, webhookEnabled, webhookUrl, holidayAutoSkipEnabled, holidayCountryCode, hueBridgeIp, hueApiKey, hueLightIds

**v1.2.0:** accentColor, adaptiveDifficultyEnabled, calendarAutoAlarmEnabled, calendarAutoAlarmMinutesBefore, guardianContactName, guardianContactPhone, customTypingPhrases, nightClockEnabled, showMotivationalQuotes

## Services & Workers
| Component | Type | Purpose |
|-----------|------|---------|
| AlarmService | Foreground (mediaPlayback) | Alarm firing, audio, vibration, flashlight, backup sound |
| SmartAlarmService | Foreground (dataSync) | Accelerometer motion monitoring |
| SonarSleepService | Foreground (microphone) | Sonar sleep tracking (experimental) |
| HueSunriseWorker | WorkManager | Philips Hue brightness ramp |
| HolidaySyncWorker | WorkManager (weekly) | Holiday data refresh (3 retries max) |
| WakeConfirmWorker | WorkManager | Post-alarm wake confirmation |
| GuardianWorker | WorkManager | Emergency contact SMS + call |
| CalendarAutoAlarmWorker | WorkManager (daily) | Calendar event auto-alarm |

## Notification IDs (no collisions)
AlarmService=1001, MissedNotification=1003, SonarSleep=2002, SmartAlarm=2003, NextAlarmNotifier=2004, BedtimeReminder=3001, WakeConfirm=5000+

## Backup Format (v3)
- AlarmBackup: all 47 alarm fields with defaults for backward compat
- SettingsBackup: all 34 settings fields with defaults
- Backward-compatible: v1 and v2 backups import with defaults for missing fields
- Import resilience: individual alarm failures don't abort the batch

## Challenge Types (15)
NONE, MATH_EASY, MATH_MEDIUM, MATH_HARD, SHAKE, SEQUENCE, MEMORY_PATTERN, TYPING, WALK_STEPS, NFC_SCAN, BARCODE_SCAN, PHOTO_MATCH, SQUAT, WIFI_CONNECT, MAZE

## Gotchas & Non-Obvious Details

### Scheduling
- `AlarmScheduler.schedule()` uses `var triggerTime` -- holiday skip may advance through up to 14 consecutive holidays
- `NextAlarmCalculator.calculate()` checks `alarm.specificDate` FIRST, then falls through to repeatDays/one-shot logic
- SmartAlarm PendingIntent uses `alarmId + 50000` to avoid collision with main alarm PendingIntent
- `cancel()` cancels main + SmartAlarm PendingIntents; SmartAlarm uses `FLAG_NO_CREATE` to safely no-op
- `scheduleExact()` (used by `rescheduleAll()`) skips holiday checking intentionally

### Mission Chaining
- `AlarmFiringViewModel.challengeChainTypes` stores the parsed chain
- `proceedToNextChallenge()` resets all per-challenge state (shake/squat/step counts, tapped indices, etc.)
- `canDismiss` only becomes true after the LAST challenge in the chain is solved
- Adaptive difficulty escalates MATH_EASY->MEDIUM and MEDIUM->HARD when `snoozeRate > 50%`

### AlarmService Jobs
- `backupSoundJob`: fires after `backupSoundDelaySec`, sets volume to max
- `flashlightJob`: toggles CameraManager torch 200ms on / 300ms off
- `autoSilenceJob`: records missed event, shows notification, stops service
- All jobs cancelled in snoozeAlarm(), dismissAlarm(), and onDestroy()
- `isForeground` flag prevents double-stopForeground crashes

### WorkManager + Hilt
- `AlarmClockApp` implements `Configuration.Provider`, injects `HiltWorkerFactory`
- Default `WorkManagerInitializer` removed via `tools:node="remove"` in manifest
- GuardianWorker cancelled on dismiss: `cancelUniqueWork("guardian_${alarm.id}")`

### Audio
- Wake lock: 30 minutes, released in onDestroy()
- Internet radio: `prepareAsync()` + `OnPreparedListener` (non-blocking)
- Spotify: ACTION_VIEW intent with START_PLAYBACK extra, MediaPlayer fallback
- Sleep sounds: placeholder WAVs in res/raw/, replace with real ambient audio

### Theme
- `LocalAccentColor` compositionLocal set from `AppSettings.accentColor` hex
- `AlarmClockXtremeTheme` accepts optional `accentColorHex` parameter
- Overrides primary + secondary in DarkColorScheme

### Notifications
- All icons: `R.drawable.ic_alarm` (only alarm drawable)
- NextAlarmNotifier adds "Skip this alarm" action (DismissReceiver)
- Snooze action label includes duration: `"Snooze ${snoozeDurationMinutes}m"`

### Misc
- HolidayRepository: Mutex-guarded file I/O, IOException-safe reads
- Converters: mapNotNull with try-catch for malformed repeatDays
- Timer: `SystemClock.elapsedRealtime()` (DST/NTP safe)
- Stopwatch: `maxByOrNull { it.number }` for correct previous lap total
- CrashLogger: milliseconds + threadId in filename for uniqueness
- World clock + stats: respect is24HourFormat preference

## Version History
- v1.2.0 (2026-03-28): 30 competitive features, all wired end-to-end. DB v6 (21 new columns). Mission chaining, backup sound, progressive snooze, squat/maze/wifi challenges, guardian angel, sunrise simulation, internet radio, flashlight strobe, night clock, accent color, adaptive difficulty, calendar auto-alarm, date-specific alarms, morning routine, custom typing phrases, motivational quotes, alarm profiles, early dismiss, location dismiss fields. New files: GuardianWorker, SquatDetector, CalendarAutoAlarmWorker, NightClockActivity.
- v1.1.0 (2026-03-28): 56-issue comprehensive audit. Fixed: MediaPlayer NPE race, auto-silence job leak, double-stopForeground crash, notification ID collisions, Sonar audio leak + false-positive, backup data loss (16 alarm fields + 15 settings), widget missing migrations, Converters crash, PreferencesManager update() baseline, TTS race, HolidaySyncWorker infinite retry, 24h format in world clock/stats, stopwatch lap splits, math challenge choices/precedence, timer monotonic clock, HolidayRepository thread safety, network timeouts, DAO singletons, accessibility labels.
- v1.0.0 (2026-03-23): All F1-F17 features complete.
- v0.9.0 (2026-03-20): Groups, duplicate, world clock, timers, vibration patterns, flash wake, swipe gestures.
- v0.8.1 (2026-02-22): Auto-silence, bedtime persistence, widget fixes, temp units.
- v0.8.0: Swipe-to-delete, search/sort, auto-silence, manual location.
- v0.7.0: Onboarding, stats, backup/restore, bedtime.
- v0.6.0: Ringtone picker, templates, widget, persistent notification.
- v0.5.0: Challenges, vacation mode, manufacturer compat.
