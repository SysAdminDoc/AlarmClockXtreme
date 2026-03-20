# Changelog

All notable changes to AlarmClockXtreme will be documented in this file.

## [0.9.0] - 2026-03-20

### Added
- **Alarm groups** - Tag alarms with groups (Work, School, Gym, etc.) and filter by group with chips
- **Duplicate alarm** - Clone any alarm via the overflow menu, preserving all settings
- **World Clock** - New bottom nav tab with live time zones, search/add cities, remove with long-press
- **Multiple concurrent timers** - Start several timers at once, each with independent controls
- **Custom vibration patterns** - 5 patterns: Default, Gentle, Heartbeat, Escalating, SOS
- **Flash wake** - Gradually brightens screen alongside volume for a natural wake-up
- **Swipe gestures on alarm screen** - Swipe right to dismiss, swipe left to snooze

### Fixed
- Alarm cards now respect 24-hour format setting (was always showing 12h)
- Bedtime time picker now respects 24-hour format setting (was hardcoded to 12h)
- Snooze duration is now editable via dropdown in alarm edit (was display-only)
- Gradual volume is now editable via dropdown in alarm edit (was display-only)
- Alarm edit time display respects 24h format

### Improved
- Bedtime and Stopwatch moved to Settings for cleaner bottom nav
- Group indicator badges on alarm cards
- Undo snackbar when deleting alarms
- Search now also matches alarm group names
- Bottom nav: My Day, Alarm, Timer, World Clock, Settings

## [0.8.1] - 2026-02-22

### Fixed
- Auto-silence setting now actually reads user preference (was hardcoded to 10 minutes)
- Editing a disabled alarm no longer force-enables it
- Power Nap template creates alarm 20 minutes from now instead of at 12:20 AM
- Bedtime settings now persist across app restarts (stored in DataStore)
- Original creation timestamp and max snooze count preserved when editing alarms
- Stats screen no longer crashes when alarm events have invalid day-of-week values
- Calendar events loaded off main thread (prevents ANR)
- Widget reuses singleton database connection instead of creating new one per refresh
- Geocoding search debounced (300ms) to prevent rapid API calls on each keystroke
- Alarm countdown timer now updates every 30 seconds
- Vacation mode validates end date is after start date
- Persistent notification observer guards against duplicate coroutines
- Skip-next survives device reboot (preserved trigger time not recalculated)
- Time picker respects 24-hour format setting
- Backup result messages auto-dismiss after 5 seconds
- Bedtime reminder reschedules itself daily after firing
- Max snooze count now enforced (auto-dismisses after limit reached)

### Improved
- Removed Moshi reflection adapter (~2MB APK size reduction)
- Weather supports Fahrenheit/Celsius toggle in Settings
- Temperature displays now show degree symbol (72°F instead of 72F)
- All icons have accessibility contentDescription for TalkBack
- Snooze/Dismiss receivers use startForegroundService for reliability
- BootReceiver uses SupervisorJob with error logging
- Replaced deprecated onBackPressed with onBackPressedDispatcher
- Hardened ProGuard rules for R8 full mode
- Added crash logger for pre-release debugging
- Added monochrome icon layer for Android 13+ themed icons
- Added round launcher icon variant
- Release signing config reads from keystore.properties

### Added
- Privacy policy (PRIVACY_POLICY.html)
- F-Droid metadata structure
- GitHub README with badges and feature overview
- Play Store listing copy

## [0.8.0] - 2026-02-21

### Added
- Swipe-to-delete alarm cards with undo snackbar
- Auto-silence preference (0/5/10/15/30 minutes)
- Alarm sorting (by time, created, enabled-first)
- Search/filter for 4+ alarms
- Challenge and silent mode indicators on alarm cards
- Battery optimization crash fix (FLAG_ACTIVITY_NEW_TASK)
- Default alarm seeding on first launch
- Settings tab in bottom navigation
- Manual location with geocoding search

## [0.7.0] - 2026-02-21

### Added
- Onboarding flow (permissions, features, battery optimization)
- 24 unit tests for core alarm logic
- Skip next occurrence for repeating alarms
- Alarm history and statistics screen
- Backup/restore (JSON export/import)
- Bedtime reminders with sleep goal tracking

## [0.6.0] - 2026-02-21

### Added
- Ringtone picker with preview playback
- Alarm templates (Power Nap, Early Bird, Weekday, Weekend)
- Glance home screen widget with countdown
- Persistent notification showing next alarm

## [0.5.0] - 2026-02-21

### Added
- Dismiss challenges (math, shake, memory sequence)
- Vacation mode (date range, auto-skip)
- Manufacturer compatibility warnings (Xiaomi, Samsung, etc.)

## [0.4.0] - 2026-02-21

### Added
- Weather dashboard with Open-Meteo API
- Calendar integration (today's events)
- My Day tab with greeting and overview

## [0.3.0] - 2026-02-21

### Added
- Bottom navigation (My Day, Alarm, Timer, Stopwatch, Bedtime)
- Timer with countdown and notification
- Stopwatch with lap tracking

## [0.2.0] - 2026-02-21

### Added
- Alarm editing (label, repeat days, ringtone, vibration, volume)
- Gradual volume increase
- Snooze with configurable duration
- Lock screen alarm display

## [0.1.0] - 2026-02-21

### Added
- Core alarm scheduling with AlarmManager.setAlarmClock()
- Room database with Alarm entity
- Hilt dependency injection
- Material 3 dark theme
- Basic alarm list with enable/disable toggle
