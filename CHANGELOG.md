# Changelog

All notable changes to AlarmClockXtreme will be documented in this file.

## [1.5.3] - 2026-04-19

Premium UX and UI polish pass — no new features, no schema changes.
Every change targets feel, clarity, and visual consistency.

### Changed

- **Navigation transitions.** All tab and screen switches now use a
  subtle `slideInHorizontally + fadeIn` / `slideOut + fadeOut` animation
  instead of an instant cut. Feels dramatically more polished on real
  hardware.

- **AlarmCard: Removed redundant "Enabled"/"Paused" chip.** The
  `Switch` toggle already communicates on/off state visually. The
  chip was visual noise and directly contradicted the "Paused by
  vacation" chip when vacation mode was active (both showing
  simultaneously). Removed; the vacation-pause chip is kept.

- **AlarmCard: `animateItem()` on lazy list items.** Alarm cards now
  animate when order changes (after sort, enable/disable, or delete)
  instead of teleporting. Requires no extra API opt-in on Compose 1.7+.

- **AlarmCard: Challenge type chip now shows polished labels.**
  Previously rendered raw enum strings like "Math easy" (from
  `lowercase().replaceFirstChar`). Now uses the same lookup map as the
  edit screen: "Math (Easy)", "Simon Says", "Barcode Scan", etc.

- **AlarmList: Removed redundant "Search alarms" section title** from
  the search card. The field placeholder text ("Try "weekday"…") already
  communicates function; the title above it added vertical height with
  no information gain.

- **Quick alarms: "Power nap" now has a divider.** The label between
  the two chip rows was floating with no visual separator. Added a
  subtle `HorizontalDivider` to clearly delineate the sub-section.

- **AlarmEdit: Removed duplicate Save button from TopAppBar.** There
  was a `TextButton("Save")` in the `TopAppBar` *and* a full-width
  `Button("Create alarm" / "Save changes")` in the `bottomBar`. Two
  save CTAs is confusing. The bottom bar button is the clear primary
  action; the TopAppBar one is removed.

- **AlarmEdit: Group section now shows custom text field only when
  needed.** Previously an `OutlinedTextField` for custom group name was
  always visible below the dropdown, creating two overlapping inputs.
  Now: the dropdown shows preset groups plus a "Custom…" item; the text
  field only appears when a custom (non-preset) group is active.

- **Settings: Removed "On" / "Off" text labels from `SettingsToggle`.**
  The text labels were rendered above the `Switch` widget in a small
  column — a classic amateur pattern. The Switch itself communicates
  state visually by design. Removed the text; layout is now a clean
  label + description row with the Switch on the right.

- **Settings: Fixed "0m 15s" time formatting in volume ramp.** Seconds
  values under one minute were displaying as "0m 15s". Now formats as
  "15s" (no leading "0m"), "2m" (no trailing "0s"), or "1m 30s" for
  combined values.

- **Onboarding: Pager indicator dot width is now animated.** The active
  dot expands from 8 dp to a 28 dp pill. Previously this was an instant
  snap; now uses `animateDpAsState` with a 250 ms tween for a smooth
  morphing transition consistent with modern design patterns.

- **Typography: Added named `TextStyle` constants for large clock
  displays.** Three screens were using hardcoded `fontSize = 40/52/64.sp`
  for alarm time, temperature, and edit-time-preview displays. These
  now reference `ClockTimeSmall`, `ClockTimeDisplay`, and `ClockTimeLarge`
  from `Type.kt` — a single place to tune the clock face aesthetic.



Follow-up polish pass closing the three "remaining risks" flagged in the
v1.5.1 audit. Still no schema change, no new user features —
testability, deprecation cleanup, and one small honesty UX fix.

### Added

- **`MissedAlarmReplayPolicy` pure decision object + 9 unit tests.** The
  10-minute replay window, feature-flag gate, clock-drift tolerance,
  and live-alarm guard all live in a single pure function so they can
  be pinned without BroadcastReceiver / Hilt / DataStore wiring.
  `MissedAlarmUnlockReceiver` now routes through it.
- **`ProximityCoverDetector.computeThreshold(sensorMaxRange)` helper + 6
  unit tests.** Extracted so the clamp behaviour (0 / microscopic /
  negative range → fallback to 5 cm default) is testable on the JVM
  without SensorManager.
- **"Paused by vacation" per-alarm badge on the alarm list.** When an
  alarm's next trigger falls inside the active vacation window, the
  card now shows a yellow `Paused by vacation` chip and the secondary
  line reads "Paused until vacation ends" instead of the misleading
  "Next alarm in 3 days". `AlarmListViewModel` surfaces the current
  `vacationStartMillis` / `vacationEndMillis` bounds for this.

### Fixed

- **`Window.statusBarColor` / `navigationBarColor` deprecation noise in
  `Theme.kt`.** These setters were deprecated on Android 15 (API 35)
  because edge-to-edge is now enforced system-wide (the host
  activities already call `enableEdgeToEdge()`). Guarded with
  `Build.VERSION.SDK_INT < VANILLA_ICE_CREAM` and suppressed the
  deprecation warning; older devices still get the expected bar
  colouring.
- **Dead duplicate "clear missed state" call in
  `MissedAlarmUnlockReceiver`.** The policy now owns state clearing;
  the inline second `store.edit().clear().apply()` after the decision
  was redundant and has been removed.

### Build + test matrix

- `assemblePlayDebug` — green
- `testPlayDebugUnitTest` — all tests green, 15 new unit tests added
  (9 for MissedAlarmReplayPolicy + 6 for ProximityCoverDetector)
- `assemblePlayRelease` — green; signed APK in
  `releases/AlarmClockXtreme-1.5.2-play-release.apk`

## [1.5.1] - 2026-04-18

Production-hardening pass driven by a dedicated audit. Targets real bug
classes identified in v1.5.0 — ANR sources, service-restart data loss,
missed-alarm replay races, and sensor-quirk edge cases — without any
new user-facing features.

### Fixed — Critical

- **Eliminated `runBlocking` ANR risk in `NextAlarmCalculator`.**
  `solarTimeFor()` previously called `runBlocking { preferencesManager
  .getCurrentSettings() }` on the synchronous calculation path. When the
  calculator was invoked from ViewModel `combine` blocks running on
  Dispatchers.Main (e.g., [AlarmListViewModel] status bar updates) this
  could block the main thread if DataStore was slow. Replaced with a
  non-suspend cached snapshot exposed via
  `PreferencesManager.getCachedSettings()`. The cache is kept current by
  the existing `settings` Flow collectors.
- **Alarms are now `sanitized()` before firing.** `AlarmService.startAlarm`,
  `snoozeAlarm` and `dismissAlarm` run every Room row through
  `Alarm.sanitized()` on entry, not just the backup restore path. A
  corrupt `challengeType`, `vibrationPattern`, `ringtonePool` or
  `specificDate` can no longer reach the firing UI.
- **Progressive-snooze count survives service restart.** If the OS killed
  the service between fire and the user tapping Snooze, the next
  `onStartCommand` was starting with `currentSnoozeCount = 0` and
  resetting the progressive-snooze ladder. Entry points now re-read the
  persisted count from `alarm_runtime_state` SharedPrefs when the
  in-memory state is fresh.

### Fixed — High

- **`MissedAlarmUnlockReceiver` no longer stacks on a live alarm.**
  Added `AlarmService.activeAlarmId` volatile flag and the receiver now
  refuses to replay a miss if another alarm is currently firing (prevents
  double-foreground-service / audio conflict). Window widened from
  closed `0..600_000ms` to half-open `0 until 600_000ms` so the boundary
  can't straddle two consecutive alarms.
- **Missed-alarm state cleared on reboot.** `BootReceiver` now wipes
  `missed_alarm_state` on `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` so
  a stale miss from before the reboot can't fire on the user's first
  post-boot unlock.
- **`BootReceiver` has a 30-second timeout** around `rescheduleAll()`.
  A corrupt DB page could previously pin the `PendingResult` until the
  broadcast-receiver ANR watchdog killed the process.
- **Radio-error audio fallback guarded against recursion.** A
  `@Volatile audioStarting` flag prevents `startAudio` from re-entering
  while the previous call is still mid-flight (can happen when the
  internet-radio `OnErrorListener` fires before the radio `MediaPlayer`
  construct returned).
- **`SmartAlarmService` scheduling wraps `startForegroundService` in
  try/catch** with an `AlarmManager` fallback. Android 14+ background
  restrictions can deny the immediate-start path on some edges; the
  fallback runs the service one second later without user impact.
- **`ProximityCoverDetector` clamps `maximumRange`.** Some OEM proximity
  sensors report `0` or microscopic ranges, which made
  `maximumRange * 0.5f` too small to ever trip (or always trip). Floor
  now at a physically plausible 3 cm (with a 5 cm default when the
  driver value is implausible).

### Fixed — Medium

- **`TextToSpeech` constructor try/catch.** On stripped-down AOSP or
  managed-profile devices with no TTS engine, the constructor throws
  and was un-caught; the morning-announcement path now falls through
  cleanly.
- **Flashlight strobe always ends with the torch OFF.** A mid-strobe
  exception could leave the LED stuck on; the coroutine's `finally`
  block now forces `setTorchMode(false)`.
- **All alarm-time formatting honours the 24-hour preference.**
  `AlarmService.buildAlarmNotification`, `formatAlarmTime` and
  `showMissedNotification` shared a manual AM/PM formatter that ignored
  `AppSettings.is24HourFormat`. All three now route through a single
  helper that respects the setting.
- **Quick Settings tile re-refreshes after skip.** The post-click
  broadcast to `SkipNextReceiver` is async, so the tile showed stale
  time until the user next opened the shade. Added a 600 ms follow-up
  refresh.
- **Firing activity finishes on "alarm not found".**
  `AlarmFiringViewModel` now emits a `finishEvents` signal when the
  row disappeared between schedule and fire; the activity observes it
  and closes (instead of rendering a blank screen).
- **Firing activity fx moved out of `collectLatest`.** `flashWake` /
  sunrise simulation are kicked off exactly once when the alarm becomes
  non-null, using `distinctUntilChanged` keyed on alarm id. Previously
  every state emission retriggered the `collectLatest` body (benign
  because of class-field guards, but wasteful).
- **Holiday auto-skip loop extended from 14 to 30 attempts** so back-
  to-back regional 2-week holiday clusters don't fall through to firing
  on a holiday.

### Changed

- `NextAlarmCalculator` constructor split: test-friendly `(AppSettings)`
  and `()` variants keep the unit tests green while production DI
  routes through `(PreferencesManager)`.
- `PreferencesManager.settings` pipes through `onEach { cachedSettings = it }`
  so the snapshot is kept current without any extra wiring.

### Migration

No schema or backup-format change. Existing v1.5.0 installs upgrade
in place.

## [1.5.0] - 2026-04-17

First roadmap-driven release. Closes v1.4.0 follow-up gaps and ships a
batch of small borrowable ideas from Section 3 and Section 9.

### Added

- **Three new dismiss challenges** (19 total):
  - `SIMON_SAYS` — watch a 4-pad color sequence (length 4-6) and play it
    back. Wrong tap flashes red and restarts the round.
  - `DATE_BACKWARDS` — type today's ISO date reversed character-by-character
    (e.g. `2026-04-17` → `71-40-6202`). Cognitive gate that's easy on
    groggy motor skills but hard without actually reading.
  - `STROOP` — classic interference test; the displayed color-word is
    painted in a different ink color and the user taps the INK, not the
    word. Four-color palette.
- **Sunrise/sunset-relative alarm firing** (`solarOffsetMinutes`,
  `solarAnchor`). Alarm edit → Advanced → Solar anchor + offset. When
  set, the alarm fires at sunrise/sunset ± offset at the last known
  location. Uses a compact NOAA solar-position approximation (~1-min
  accuracy). Falls back to the fixed clock time when no location is
  cached or during polar day/night.
- **What's-new dialog** on first launch after update. `WhatsNewTracker`
  records the versionCode we last showed highlights for; fresh installs
  skip the dialog.
- **Alarm-edit UI** for the v1.4.0 fields that previously had no surface:
  - Hardware-button action dropdown (NONE / SNOOZE / DISMISS).
  - "Dismiss when song finishes" toggle.
  - Ringtone pool multi-line editor (one URI per line).
- **Bedtime: seconds-scale final-taper slider** (15s/30s/60s/2m/5m/10m)
  for the sleep-sound fade-out. Lives directly on the Bedtime tab so
  power users don't have to dive into Settings.
- **Power-nap chips highlight the user's default.** `napDefaultMinutes`
  from AppSettings now surfaces in the Quick Alarms row with a distinct
  accent and a " • default" label.

### Changed

- **DB v8.** `MIGRATION_7_8` adds `solarOffsetMinutes` (Int, default 0)
  and `solarAnchor` (String, default "SUNRISE").
- **`NextAlarmCalculator` now injects `PreferencesManager`** so it can
  read the cached location for solar math. Solar time is recomputed per
  candidate day in the repeating-alarm loop (sunrise drifts minutes daily).
- **Backup format v5.** Alarm and settings backups carry the two new
  solar fields. `MAX_SUPPORTED_BACKUP_VERSION = 5`; earlier versions
  still import via Moshi default-filling.
- **`SleepSoundPlayer.scheduleFade()`** takes `fadeDurationSeconds`
  directly (5-600s clamp). BedtimeViewModel persists the choice via a
  new `setSleepSoundFadeSeconds` setter.

### Fixed

- `ChallengeType` enum gains `SIMON_SAYS`, `DATE_BACKWARDS`, `STROOP`
  and `ChallengeGenerator` covers each — earlier versions would have
  thrown `IllegalArgumentException` on `valueOf()` for these.

## [1.4.0] - 2026-04-17

### Added (competitive-research pass — features absorbed from Alarmy, Sleep as
Android, BlackyHawky Clock, Fossify Clock, Google Clock, Turbo Alarm)

- **Count-the-Sheep dismiss challenge.** A playful CAPTCHA — sheep and goats
  drift across a starry panel; tap every sheep to a randomised target count
  without catching a goat. Joins the 15-challenge roster as
  `ChallengeType.COUNT_SHEEP`.
- **Quick Settings tile (Skip next alarm).** `SkipNextAlarmTileService` —
  shade tile shows the next alarm's day + time; one tap routes through the
  existing `SkipNextReceiver` so skip semantics match the persistent
  notification action (repeating: recompute; one-shot: disable). Inactive
  state when no alarm is queued.
- **Material You dynamic colors (Android 12+).** Opt-in toggle in Settings →
  Personalization. On Android 12+ the primary/secondary/tertiary palette
  derives from the user's wallpaper (while keeping the app's deep-dark
  surfaces). On older devices the toggle is persisted but no-op, with
  help copy that names the requirement so the setting never feels broken.
- **Cover-to-snooze.** New `ProximityCoverDetector` — hold a hand over the
  proximity sensor for ~1.5 s during an alarm to snooze. Global toggle, pairs
  with flip-to-snooze for phones where face-down accelerometer is flaky
  (e.g. in a phone stand).
- **Hardware-button action per alarm.** `Alarm.hardwareButtonAction` —
  `NONE` / `SNOOZE` / `DISMISS`. Volume Up/Down, Camera, Headset Hook keys
  are intercepted via `dispatchKeyEvent` when the alarm is firing and the
  alarm has opted into a non-NONE action. `NONE` falls through to normal
  system volume control. (Edit-screen UI surfacing tracked on ROADMAP.)
- **Dismiss at ringtone end.** `Alarm.dismissAtRingtoneEnd` — when set, the
  alarm's `MediaPlayer` loops off and an `OnCompletionListener` auto-dismisses
  the alarm once the song / ringtone finishes naturally. Ideal for Spotify
  users or anyone who wants "wake to one song."
- **Random ringtone pool.** `Alarm.ringtonePool` — comma-separated list of
  alarm tones. On each fire the service picks a random URI from the pool
  (supersedes `ringtoneUri`). Anti-habituation: the brain stops tuning out
  a single wake-up sound.
- **Repeat missed alarms safety net.** If an alarm auto-silences and the
  new `repeatMissedAlarms` pref is on, `MissedAlarmUnlockReceiver`
  (listening on `USER_PRESENT`) re-fires that alarm the next time the user
  unlocks within 10 minutes. State is cleared on every re-fire so a single
  miss can only retrigger once.
- **Bedtime wind-down checklist.** Mirror of the morning-routine feature —
  `AppSettings.bedtimeChecklist` (newline-separated items) renders as a
  tappable pre-sleep checklist on the Bedtime tab, with a reset affordance.
- **Configurable sleep-sound timer + fade.** `SleepSoundPlayer.play(...)`
  now accepts a `fadeDurationSeconds` (5–600) and respects
  `AppSettings.sleepSoundTimerMinutes` and `sleepSoundFadeSeconds`, so the
  final taper can be as short as 5 s or as slow as 10 min.
- **Power-nap preset row.** Alarm list → Quick alarms now carries a second
  row with cycle-aware nap lengths (15/20/25/45/90 min) on top of the
  existing reminder durations.
- **Backup format v4.** `AlarmBackup` and `SettingsBackup` extended with
  the v1.4.0 alarm fields and seven new preference fields.
  `MAX_SUPPORTED_BACKUP_VERSION = 4`; v1–v3 backups still import via
  Moshi's default-filling behaviour.

### Changed

- **DB v7.** `MIGRATION_6_7` adds `hardwareButtonAction`,
  `dismissAtRingtoneEnd`, `ringtonePool`.
- **`AlarmService.startAudio()` refactored.** Split into `startAudio()`
  (pool-pick + silent-mode gate) and `startAudioInternal()` (existing
  Spotify/radio/default paths). Keeps the pool logic at one well-defined
  layer that wins over a static `ringtoneUri`.
- **`AppSettings` gained seven v1.4.0 preferences.** `dynamicColorEnabled`,
  `coverToSnoozeEnabled`, `bedtimeChecklist`, `sleepSoundTimerMinutes`,
  `sleepSoundFadeSeconds`, `repeatMissedAlarms`, `napDefaultMinutes` — all
  round-tripped through `toSettings()` / `applySettings()` for drift-free
  persistence.

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
