# AlarmClockXtreme - Working Notes

## Tech Stack
- Kotlin 2.1, Jetpack Compose (Material 3), Room, Hilt, Retrofit + Moshi (codegen), DataStore, Glance widgets
- minSdk 26, targetSdk 35, compileSdk 35
- Build: `./gradlew assembleDebug` (play/fdroid flavors)

## Key Paths
- App entry: `AlarmClockApp.kt`, `MainActivity.kt`
- Navigation: `ui/navigation/AppNavigation.kt` - bottom nav: My Day, Alarm, Timer, World Clock, Settings
- Alarm model: `data/model/Alarm.kt` - Room entity with group, flashWake, vibrationPattern fields
- DB: `data/local/AlarmDatabase.kt` - version 4, migrations 1-4
- Scheduling: `domain/AlarmScheduler.kt` - uses `setAlarmClock()` for reliability
- Firing: `service/AlarmService.kt` (foreground service) + `ui/alarmfiring/AlarmFiringActivity.kt`
- Preferences: `data/preferences/PreferencesManager.kt` - DataStore-backed AppSettings
- Weather: Open-Meteo API (no key), `data/remote/WeatherApi.kt`

## Architecture
- UI Layer: Compose screens with ViewModels + StateFlow
- Domain: AlarmScheduler, NextAlarmCalculator
- Data: Room DB + DataStore + Retrofit
- DI: Hilt (DatabaseModule, NetworkModule)

## Version History
- v1.0.0 (2026-03-20): Custom snooze durations on firing screen, multi-select with batch delete/enable/disable, memory challenge sequence fix, media playback fallback to default ringtone
- v0.9.0 (2026-03-20): Alarm groups, duplicate, world clock, multiple timers, vibration patterns, flash wake, swipe gestures, 24h format fixes
- v0.8.1 (2026-02-22): Bug fix release - auto-silence, bedtime persistence, widget fixes, temp units
- v0.8.0: Swipe-to-delete, search/sort, auto-silence, manual location
- v0.7.0: Onboarding, stats, backup/restore, bedtime
- v0.6.0: Ringtone picker, templates, widget, persistent notification
- v0.5.0: Challenges, vacation mode, manufacturer compat

## DB Schema
- `alarms` table: id, hour, minute, label, isEnabled, repeatDays, ringtoneUri, vibrationEnabled, vibrationIntensity, volume, overrideSystemVolume, gradualVolumeSeconds, snoozeDurationMinutes, maxSnoozeCount, showOnLockScreen, challengeType, group, flashWake, vibrationPattern, createdAt, nextTriggerTime
- `alarm_events` table: id, alarmId, alarmLabel, scheduledTime, firedAt, action, actionAt, challengeType, challengeSolveTimeMs, snoozeCount, dayOfWeek

## Gotchas
- Vibration patterns defined in AlarmService.startVibration() - patterns are longArray + intArray pairs
- Timer supports multiple concurrent instances now - TimerViewModel tracks List<TimerInstance>
- World clock uses Java ZoneId for timezone data (no external API needed)
- Flash wake ramps screen brightness in AlarmFiringActivity via window.attributes.screenBrightness
- Bottom nav changed in v0.9.0: Bedtime moved to Settings, World Clock added
- AlarmCard and AlarmEditScreen both check is24HourFormat from PreferencesManager
