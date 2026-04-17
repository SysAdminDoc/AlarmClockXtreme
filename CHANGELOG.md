# Changelog

All notable changes to AlarmClockXtreme will be documented in this file.

## [1.3.3] - 2026-04-16

### Fixed (audit pass 4 — service lifecycle, worker delays, backup validation)

- **`AlarmService.speakMorningAnnouncement` no longer leaks the TTS engine.**
  The cleanup hook was a coroutine launched in `serviceScope` with `delay(8000)`;
  on the common path (alarm dismissed → service stops → scope cancelled within
  ~200 ms) the cleanup never ran and `TextToSpeech.shutdown()` was skipped.
  Replaced with an `UtteranceProgressListener.onDone/onError/onStop` cleanup,
  plus a 30 s safety net on a daemon `ScheduledExecutorService` independent
  of the service scope.
- **`scheduleWakeConfirmation` floors `wakeConfirmDelayMinutes` at 1.** A
  corrupt or zero value would otherwise have raced the wake-confirm prompt
  against the morning briefing animation in the same instant.
- **`AlarmService` Guardian Angel scheduling floors `guardianDelaySec` at 30.**
  Prevents an emergency-contact alert from firing before the user has any
  reasonable chance to interact with the alarm if the per-alarm delay is
  somehow zero or negative.
- **`BackupManager.importFromUri` validates the backup version.** A random
  JSON file (or a future-format export) used to be silently parsed as an
  empty backup with all defaults; we now reject `version > 3` with a clear
  error and accept `version 1..3` (Moshi tolerates older formats by filling
  defaults for missing fields).
- **`BackupManager.exportToUri` opens the output stream first.** Previously
  the entire DB was queried and the JSON serialised before discovering a
  permission-denied / cancelled SAF intent — wasting work and confusing
  error timing.

## [1.3.2] - 2026-04-16

### Fixed (audit pass 3 — workers, widgets, orphan settings, backup integrity)

#### Critical correctness
- **`CalendarAutoAlarmWorker` no longer creates duplicate alarms.** Each daily
  run previously inserted a brand-new `Alarm` row, accumulating to 7+
  duplicates per week. The worker now keeps a single reusable auto-alarm row
  identified by a reserved `profileName`, queries
  `CalendarContract.Instances` (so RRULE-expanded recurring events are
  honoured — `Events` alone missed them), pins the alarm to a `specificDate`
  for tomorrow, and disables (rather than deletes) the row when tomorrow has
  no events so user-edits to time/sound persist.

#### Backup integrity
- **`data_extraction_rules.xml` now includes DataStore preferences.** Cloud
  backup and device-transfer were silently dropping the entire
  `alarm_settings.preferences_pb` file — vacation mode, holiday config,
  Philips Hue creds, accent color, every v1.2.0 personalization setting were
  not migrating. Photo-match reference photos are also included; transient
  crash logs are explicitly excluded. The manifest now references the rules
  file via `android:dataExtractionRules="@xml/data_extraction_rules"` —
  without that attribute the rules file was unused.

#### Reliability
- **`WidgetUpdater` no longer leaks a Job per call.** Replaced the
  per-call `CoroutineScope(Dispatchers.IO)` allocation with a single
  process-scoped `SupervisorJob` so toggling alarms doesn't accumulate
  unrooted jobs.

#### UX — orphan settings finally exposed
- New **Personalization** section in Settings exposes:
  - Accent color picker (six-swatch palette: Default Blue / Violet / Coral /
    Amber / Mint / Mono). Previously the `accentColor` setting was read by
    `MainActivity` but had no UI to change it — users were stuck on the
    factory blue forever.
  - **Show motivational quotes** toggle, which actually gates the quote
    rendering on the firing screen (previously the quote always rendered
    regardless of `showMotivationalQuotes`).
  - **Adaptive challenge difficulty** toggle (the
    `AlarmFiringViewModel` was already reading `snoozeRate` and bumping
    math difficulty, but the user setting that gates the feature was an
    orphan).
  - **Custom typing phrases** multi-line editor — `ChallengeGenerator`
    already merges these with the built-in list.
- **Flip-to-snooze chip on the firing screen** is hidden when the user
  hasn't enabled the global setting (the chip was previously a lie).
- **`StatsScreen` honours the 24-hour preference.** The screen took a
  defaultable parameter the nav graph never passed, so event timestamps
  always rendered in 12-hour format. The `StatsViewModel` now collects the
  setting itself.

#### Hardening
- **`SettingsViewModel.updateAccentColor` validates the hex string** through
  `android.graphics.Color.parseColor` before persisting, so a bad value
  (or someone editing the settings file by hand) can't blank out the theme.

## [1.3.1] - 2026-04-16

### Fixed (audit pass 2 — wider net)

#### Correctness
- **`StopwatchViewModel` is now monotonic** — `SystemClock.elapsedRealtime()`
  replaces `System.currentTimeMillis()`, so an NTP sync, DST flip, or
  user-initiated clock change mid-run can no longer rewind or fast-forward
  the stopwatch.
- **`StatsViewModel` keeps aggregates live** — totals/streak/snooze rate now
  recompute every time the recent-events flow ticks, so the screen no longer
  shows stale numbers if an alarm fires while it's open.
- **`WorldClockViewModel` persists user-curated zones** — saved zones are
  written to a SharedPreferences string-list, survive cold-starts, and skip
  any zone the JVM no longer recognises (no more crash from a stale entry).
  Toggling 24-hour format also re-renders immediately instead of waiting
  for the next 1-second tick.
- **`AlarmEditViewModel.save()` is re-entrancy guarded** — a fast double-tap
  on Save no longer creates two alarm rows. The `isSaving` flag now also
  resets in a `finally` so a transient DB/scheduler exception doesn't strand
  the user on a permanently-disabled "Saving..." button.
- **Edit flow tears down old schedules when the alarm is disabled** —
  previously, editing an enabled alarm into a disabled one left the prior
  AlarmManager registration armed.
- **`AlarmService` audio path hardening:**
  - Internet-radio URL is restricted to http(s) and gets a real
    `OnErrorListener` that falls back to the device default ringtone on
    stream failure (DNS, 404, codec). Previously a failing stream produced
    a silent alarm.
  - Spotify ringtone is restricted to the canonical `spotify:` /
    `https://open.spotify.com/` schemes, package-targeted at
    `com.spotify.music`, and `resolveActivity()`-checked before launch so a
    typo'd URI can't accidentally open the browser. The package is also
    declared in `<queries>` so this works on Android 11+.
  - Both `RingtoneManager.getDefaultUri()` calls returning null is now
    handled — the alarm goes silent gracefully (notification + vibration
    + flashlight still fire) instead of throwing NPE into the catch block.
  - `Uri.parse(alarm.ringtoneUri)` is `runCatching`-wrapped so a corrupt
    custom-ringtone URI no longer crashes setDataSource.

#### Reliability / robustness
- **`ChallengeGenerator.generateMaze()`** — bounded retry (50 attempts) plus
  a guaranteed-solvable empty-walls fallback. The previous `while (true)`
  could in theory deadlock the alarm-firing flow on a pathological RNG
  outcome.
- **`SonarSleepService` audio-write loop** is null-safe and exits cleanly on
  any `write()` exception (e.g. AudioTrack released mid-loop).
- **`SonarSleepService.stopSonarHardware`** rewritten to use explicit blocks
  instead of the brittle `let { if(...) it.stop(); it.release() }` semicolon
  trick — both stop and release branches are now obviously reachable.

#### Security / privacy
- **`SettingsScreen` warns on plain-http webhook URLs** — alarm event
  payloads (label, time, action) were being sent unencrypted without any UI
  surface flagging it.

#### UX
- **Night clock is reachable from Settings** — was previously orphan code
  declared in the manifest with no in-app launcher. New "Night clock" tile
  in the Settings → Utilities section starts the bedside-mode activity.
- **`Theme.kt` is preview-safe** — `view.context as Activity` is now a soft
  `as?` cast, so the theme can be hosted in any non-Activity Compose preview
  or wrapped context without `ClassCastException`.

#### Tests
- **+4 unit tests** covering `ChallengeGenerator.generateMaze` solvability,
  bounds invariants, walk-step minimum, and math-choice integrity.

## [1.3.0] - 2026-04-16

### Fixed (production hardening pass)

#### Critical correctness
- **"Skip this alarm" notification action no longer triggers post-fire flow.**
  Previously the persistent next-alarm notification routed "Skip this alarm"
  through `DismissReceiver` -> `AlarmService.ACTION_DISMISS`, which fired the
  morning briefing, scheduled the wake-confirmation worker, sent a `dismissed`
  webhook event and recorded a `DISMISSED` stat with `firedAt = 0`. A new
  `SkipNextReceiver` records a proper `SKIPPED` event and just re-arms the
  next occurrence (or disables one-shot alarms).
- **Wake-confirmation worker actually prompts the user now.** It previously
  polled SharedPreferences for a confirmation token that no UI ever wrote,
  causing every wake-confirm cycle to re-fire the alarm. The worker now posts
  a high-priority full-screen-intent notification opening `WakeConfirmActivity`
  and waits up to 60 s before re-firing if still unconfirmed.
- **`AlarmScheduler.schedule()` is null-safe against `getLaunchIntentForPackage`
  returning null** on stripped/system-rebuilt installs (would NPE the show-info
  PendingIntent on every schedule).
- **`AlarmDao.observeNextAlarm`/`getNextAlarm` now exclude `nextTriggerTime = 0`**
  so the persistent notification, widget, and dashboard "next alarm" surfaces
  no longer latch onto an unscheduled alarm.
- **Defensive finish in `AlarmFiringActivity`** when launched without an alarm
  id (rare stale full-screen-intent path).

#### Race conditions / leaks
- **`TimerViewModel` no longer leaks MediaPlayers** when multiple timers
  finish simultaneously — only the first allocates audio and the existing
  tone covers all finished timers.
- **`HolidayRepository` cache reads are now mutex-guarded** and parsed dates
  are kept in memory so repeating-alarm holiday probes (up to 14 candidates
  per schedule call) hit the disk at most once.
- **`AlarmFiringActivity` Wi-Fi polling loop** now respects coroutine
  cancellation (`while (isActive)` instead of `while (true)`) and tolerates
  `SecurityException` from `WifiManager.connectionInfo`.
- **Flip-to-snooze sensor** is only registered when the user has explicitly
  enabled the global setting (it was previously registered for every alarm,
  which both wasted battery and could snooze for users who never opted in).
- **`SonarSleepService`** audio-write loop catches release-during-write
  exceptions; resource cleanup branches are no longer dependent on the prior
  brittle `let { if(...) it.stop(); it.release() }` semicolon trick.

#### Security / data safety
- **`HueSunriseWorker` validates the bridge IP, API key, and light IDs**
  against strict character sets before interpolating them into the URL,
  preventing `..` traversal or scheme smuggling from a malformed user value.
- **`WebhookService.isAllowedWebhookUrl`** rejects non-http(s) schemes
  (`javascript:`, `file://`, etc.) and malformed input before they reach
  OkHttp's URL parser. Both `fire()` and `test()` call it.
- **`GuardianWorker` sanitises the phone number** to legal `tel:` characters
  and degrades gracefully when permissions are missing — `SEND_SMS` is no-op
  if not granted, and `CALL_PHONE` falls back to `ACTION_DIAL`.
- **Permissions declared:** `SEND_SMS`, `CALL_PHONE` (Guardian Angel) and
  `ACCESS_WIFI_STATE` (Wi-Fi dismiss challenge) — previously these features
  silently failed with `SecurityException`.
- **`AlarmScheduler.cancel()`** now also cancels guardian and wake-confirm
  workers in addition to the Hue sunrise worker, so disabling/deleting an
  alarm cleans up every related background task.

#### UX / polish
- **Bedtime reminder no longer reschedules itself forever after disable.**
  `BedtimeReceiver` checks a SharedPreferences mirror that the
  `BedtimeViewModel` writes whenever the user toggles bedtime.
- **`MainActivity` handles `ACTION_SHOW_ALARMS`** so the system clock's
  upcoming-alarm chip and Google Assistant can open the app's alarm list.
- **Alarm fade-in glitch fixed** — without a fade we no longer briefly
  attack at zero volume before snapping to full.
- **`Snooze` cancels Guardian Angel** since the user demonstrably interacted.
  The next fire after snooze re-arms it.
- **Dashboard tolerates malformed weather rows** — a single bad date in the
  Open-Meteo response no longer crashes the whole forecast.
- **`NextAlarmCalculator.formatRemaining`** renders `<1m` for sub-minute
  remainders instead of the misleading `0m` it used to show in the last
  minute before fire.

#### Maintainability
- **`PreferencesManager.update()` deduplicated** — both decode and apply now
  go through `Preferences.toSettings()` / `MutablePreferences.applySettings()`
  so adding a new field can no longer accidentally reset every existing one.
- **`Converters.kt`** sanitises corrupt `repeatDays` cells (whitespace,
  empties, out-of-range integers, nulls) and guarantees a stable serialised
  ordering so observers can de-dupe.
- **`NextAlarmWidget`** uses Hilt `EntryPointAccessors` to share the app's
  singleton Room database instead of constructing a second
  `AlarmDatabase` instance with `allowMainThreadQueries()`. Resolves a
  long-standing dual-connection corruption risk.

#### Tests
- **+9 unit tests** covering `NextAlarmCalculator` specific-date precedence,
  expired-date fall-through, malformed input, sub-minute formatting, and the
  new `WebhookService.isAllowedWebhookUrl` allow-list.

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
