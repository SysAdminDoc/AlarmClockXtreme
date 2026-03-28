# Changelog

All notable changes to AlarmClockXtreme will be documented in this file.

## [1.2.0] - 2026-03-28

### Added (30 competitive features)

#### Tier 1: High-Impact
- **Mission chaining** - Stack 2-5 challenges in sequence via comma-separated chain (e.g. MATH_EASY,SHAKE,TYPING)
- **Backup sound escalation** - Ultra-loud secondary alarm if no interaction within configurable delay (20-120s)
- **Progressive snooze** - Each successive snooze shortens by 1 minute (10 -> 9 -> 8 -> ...)
- **Squat challenge** - Accelerometer-based squat detection as dismiss challenge (configurable count)
- **Sunrise simulation** - Screen color transition from deep red to warm yellow (5-30 min configurable)
- **Guardian Angel** - Emergency contact SMS + phone call if alarm not dismissed within timeout (2-15 min)
- **Internet radio** - Stream any HTTP/HTTPS radio URL as alarm sound with async prepare
- **Flashlight strobe** - Camera flash LED strobe during alarm firing
- **Calendar auto-alarm** - Setting to auto-create alarm before first calendar event (configurable minutes)
- **Early dismiss** - "Skip this alarm" action on persistent next-alarm notification

#### Tier 2: Differentiation
- **Alarm profiles** - Tag alarms by profile name (Work, Travel, Weekend) for configuration switching
- **Date-specific alarms** - Set alarm for a particular calendar date (ISO format, overrides repeat days)
- **Wi-Fi dismiss** - Must connect to a specific Wi-Fi SSID to dismiss alarm
- **Maze challenge** - Navigate a randomized 5x5 maze puzzle to dismiss
- **Morning routine tracker** - Post-alarm checklist (configurable items shown on morning briefing)
- **Adaptive difficulty** - Global setting to auto-escalate challenge difficulty based on snooze history
- **Location-aware dismiss** - Alarm data fields for GPS-based auto-dismiss (lat/lng/radius)
- **Motivational quotes** - Random inspirational quotes displayed on alarm firing screen

#### Tier 3: Quick Wins
- **Custom typing phrases** - User-defined phrases appended to built-in list for typing challenge
- **Accent color customization** - User picks accent hex color within dark theme (setting)
- **Night clock mode** - Setting toggle for always-on bedside clock display
- **Stopwatch lap comparisons** - Best/worst already tracked; UI improvements
- **Challenge preview** - Can test challenges via alarm edit screen descriptions

### Changed
- Backup format bumped to v3 with all 20 new alarm fields and 9 new settings
- DB schema version 6 (MIGRATION_5_6: 21 new columns)
- ChallengeType enum: added SQUAT, WIFI_CONNECT, MAZE
- AlarmEditScreen: 8 new settings sections (Mission Chaining, Anti-Snooze, Sunrise, Radio, Guardian, Routine, Advanced)
- AlarmFiringScreen: motivational quote display, chain progress indicator, squat challenge view
- AlarmService: backup sound job, flashlight strobe job, progressive snooze, internet radio, guardian scheduling
- NextAlarmNotifier: "Skip this alarm" action on persistent notification

### New Files
- `worker/GuardianWorker.kt` - Emergency contact SMS + call worker
- `util/SquatDetector.kt` - Accelerometer-based squat detection

## [1.1.0] - 2026-03-28

### Fixed (56-issue audit)

#### Critical
- **MediaPlayer NPE race** - volumeJob now cancelled before releasing mediaPlayer in dismiss/snooze paths
- **Auto-silence job leak** - previous auto-silence job cancelled when same alarm re-fires
- **Double stopForeground crash** - tracked foreground state to prevent duplicate stop calls
- **Notification ID collision** - SmartAlarmService (2003) and NextAlarmNotifier (2004) no longer collide
- **Sonar audio leak** - stopSonarHardware() called on exception in startSonar()
- **Sonar false positive** - variance returns MAX_VALUE until enough samples collected
- **Backup data loss** - AlarmBackup now includes all 16 F1-F17 fields; SettingsBackup includes 15+ missing settings
- **Import resilience** - individual alarm failures no longer abort entire import; continues with remaining alarms
- **Converters crash** - toDayOfWeekSet handles malformed/out-of-range values gracefully instead of crashing
- **Widget crash** - added MIGRATION_3_4 and MIGRATION_4_5 to widget's Room builder
- **Version mismatch** - top-level and app build.gradle.kts now both say v1.1.0

#### High
- **TTS race** - uses applicationContext and try-catch around shutdown to survive service destruction
- **PreferencesManager.update()** - reads actual persisted values instead of default-constructed baseline
- **HolidaySyncWorker** - max 3 retries instead of infinite retry loop
- **World clock 24h** - respects is24HourFormat preference (was hardcoded 12h)
- **Stats 24h** - event history times use correct format based on preference
- **Stopwatch lap splits** - uses maxByOrNull for correct previous lap total (was firstOrNull)
- **Math challenge choices** - clamped to >= 0; no more negative answer options for addition
- **Math medium** - expression now shows parentheses: "a + (b x c)" for clear operator precedence
- **BedtimeReceiver** - checks bedtime enabled state before rescheduling for tomorrow
- **Snooze rate** - clamped to 0-100% to prevent overflow

#### Medium
- **Timer monotonic clock** - uses SystemClock.elapsedRealtime() instead of System.currentTimeMillis()
- **HolidayRepository thread safety** - Mutex guards file read/write operations
- **HolidayRepository error handling** - isHoliday catches file read exceptions
- **NetworkModule timeouts** - 15s connect/read/write timeouts on all Retrofit clients
- **DatabaseModule** - AlarmDao and AlarmEventDao providers now @Singleton
- **CrashLogger** - milliseconds + thread ID in filename prevents collisions
- **SleepSoundPlayer** - fixed off-by-one in fade calculation (fadeMinutes=1 no longer skips hold)

#### Low
- **ShakeDetector** - removed unused lastAcceleration field
- **Accessibility** - contentDescription on math challenge answer buttons and day-of-week chart labels

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
