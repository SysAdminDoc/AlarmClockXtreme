# Changelog

All notable changes to AlarmClockXtreme will be documented in this file.

## [Unreleased]

### Fixed

- Clearing alarm history says whether it worked instead of taking the app down
  if the database refuses.
- Removing a world clock shows a delete icon rather than an overflow glyph that
  promised a menu.
- Two smart-wake windows that overlap no longer leave the processor awake, and
  a wake-confirmation notification can no longer collide with a Hue sunrise one
  on a long-lived install.
- An NFC, barcode, photo or Wi-Fi dismiss challenge with nothing saved to check
  against now falls back to a maths problem and explains why. It used to accept
  anything, so an imported alarm could be dismissed without solving it.
- Almost every screen's text now comes from the string resources, so the app is
  ready to be translated rather than hardcoded in English. The build refuses a
  new hardcoded line anywhere under the UI.
- A copy pass over the app's own words: no more dashes standing in for
  punctuation, one spelling of the app name, sentence case on notification
  channels, one snooze format, and plain language where permission constants,
  JSON field names and SDK jargon had leaked into things people read.
- Accessibility on the secondary screens: the Bedtime switches are named, the
  wind-down checklist reports whether each item is ticked, the breathing phase
  and the moment a timer runs out are announced, the News feed tabs report
  which one is active, night clock exposes an exit action instead of only a
  long-press gesture, filter chips always say whether they are selected, and
  decorative icons stopped being read twice.
- A Replace-mode restore of a backup that genuinely holds no alarms clears the
  list again, while one whose rows cannot be saved leaves your alarms alone.
- The alarm event history is capped by row count rather than by age, so the
  Stats totals and streak no longer shrink behind your back, and the trim only
  runs when the table is actually oversized.
- Section headings keep their title readable next to a wide action.
- The widget is tall enough for its own controls again.
- The wake-confirmation countdown shows the time the app is really waiting,
  even when you reach it by tapping the notification, and it announces the
  moment it expires without reading the whole sentence every second.
- Previewing a tone that fails to open no longer leaks an audio player.
- Rotating the phone keeps what you had typed and chosen: the Stats filters
  and search, the Bedtime pickers and breathing session, the ringtone search,
  and the YouTube dialog's mode, URL, query and results.
- Resetting the stopwatch or stopping a timer offers Undo. Both actions are
  one tap next to a control you use often and there was no way back.
- Settings descriptions wrap instead of being cut off mid-word, so the
  sentence explaining each toggle is readable.
- A cold launch is dark all the way through. The system splash used to flash
  white before the app appeared.
- Section headings put their action beside the title, which removes the empty
  band that made the Wake readiness card look broken.
- The wake-confirmation countdown survives rotation and matches the real
  deadline, and it announces when the time is up.
- A failed News refresh keeps the headlines you already had and shows an
  inline notice, with a Retry button when there is nothing to show.
- The widget follows the app palette, has readable text sizes and tap targets,
  and says when it could not load rather than claiming you have no alarms.
- Previewing a ringtone no longer freezes the picker while the file opens.
- Removed the "Allow legacy Hue API v1 over HTTP" setting and the code behind
  it. Android blocks plain HTTP for this app, so that path could never connect
  and the toggle only promised something impossible. Hue works over the
  encrypted v2 API as before.
- Internet radio requires an https address, says so in the editor, and flags
  one that is not. An http stream used to sit silently until the alarm fell
  back to the default tone.
- The alarm event history is pruned to a year and indexed, so the Stats screen
  stays quick instead of scanning a table that only ever grew.
- Replace-mode restore saves the new alarms before removing the old ones and
  keeps what you had if the file turns out to contain nothing usable. It used
  to delete everything first, so a crash mid-restore left no alarms at all.
- Backups are stamped v18. A build no longer writes a version it would refuse
  to read, and changing the format now breaks a test until the stamp moves too.
- Pausing all alarms shows the date it ends and a Resume chip on the Alarms
  tab. Each card used to claim it had to be re-enabled by hand, which was
  false, and Resume was three screens away.
- Removed the "Show alarm on lock screen" setting. It was never applied, and an
  alarm you cannot reach without unlocking is a broken alarm. "Hide alarm
  labels on public surfaces" still covers the privacy case.
- Quick alarms use your default snooze and gradual volume.
- Losing a database to a version downgrade now tells you what happened instead
  of quietly starting empty.
- A webhook endpoint that answers with a redirect is reported as such rather
  than retried until the attempts run out.
- When Android will not reveal the connected network name, a Wi-Fi dismiss
  alarm now swaps in a maths problem and says why, instead of showing a
  "continue without Wi-Fi check" button that dismissed the alarm from anywhere.
- The missed-alarm watchdog and the wake-confirmation re-fire now fall back to
  a full-screen notification when the system refuses to start the alarm
  service, which happens without a battery-optimisation exemption. Both used to
  give up silently, which defeated the point of a safety net.
- A smart-wake or Direct Boot alarm that is already ringing is no longer
  re-fired by the watchdog two minutes later.
- The Defaults settings actually apply to new alarms. Default snooze, gradual
  volume and show-on-lock-screen were advertised as defaults but the editor
  ignored them, and the per-alarm lock screen choice did nothing at all.
- An offline weather failure says what went wrong and offers Retry instead of
  telling you to set a location you already set.
- A webhook no longer follows redirects, so a 307 from your endpoint cannot
  forward the signed payload to another host.
- Restoring a database from a newer version of the app starts with an empty
  alarm list instead of crashing every screen that reads it.
- An alarm card shows the whole mission chain, not just the first challenge,
  and shows one even when the single challenge is set to none.
- The alarm editor keeps your place on the overview when you open a category
  and come back.
- The Dismiss page groups snooze duration, limit and progressive snooze
  together, and the mission chain sits directly under the challenge picker.
- Timers left running when the phone restarted are dropped instead of ringing
  by themselves or showing a countdown of days.
- An alarm that hides Dismiss behind a challenge or a location no longer offers
  a one-tap Dismiss from the notification shade, which skipped the challenge
  entirely. Tapping the notification opens the alarm screen as before.
- Flip-to-snooze, cover-to-snooze and the hardware button no longer close the
  alarm screen when the snooze limit is spent, which used to leave the alarm
  ringing with no way back to the challenge.
- The snooze tally is cleared when an alarm is turned off, so the next ring
  starts with a full set of snoozes.
- A location-only alarm counts as challenge-protected for the snooze limit.
- The Hue sunrise is anchored to the alarm time rather than to whenever the
  ramp happens to start, will not brighten the lights hours later if it runs
  late, and stops leaving its progress notification behind when cancelled.
- A Spotify alarm falls back correctly even when other audio was already
  playing. The check now looks for a newly started stream rather than any
  sound at all.
- Restoring a backup keeps per-alarm Guardian numbers when you opt in, and no
  longer installs a custom news feed URL without asking.
- Turning on the first-meeting alarm now asks for calendar access, and the
  Schedule card's "Calendar access" row can be tapped to grant it. Nothing in
  the app ever requested the permission, so the feature could be on and inert.
  Two workers could also insert a second `calendar_auto` alarm at the same
  moment; that is serialised now, and any stray row is cleaned up.
- The per-alarm early dismiss window now controls when the upcoming-alarm
  notification offers Skip. The setting had no effect and Skip was always
  there.
- The per-alarm dismiss action (webhook, Hue scene or broadcast) has an editor
  and survives a save. The field existed but nothing wrote to it, and every
  save silently cleared whatever a backup had put there.
- Restoring a backup no longer installs its webhook endpoint, signing secret,
  Hue key, Routes key or Guardian phone number without asking. The import
  preview names those values and they stay out unless you tick the box, which
  matters because a backup file can come from anywhere. Import also refuses a
  file that is absurdly large instead of running out of memory reading it.
- A Spotify alarm no longer rings silently. Handing playback to Spotify was
  treated as proof that it started, so a logged-out or offline Spotify, or a
  launch the system refused, left nothing playing at all. The alarm now checks
  a few seconds later and falls back to your default tone.
- Hue sunrise now completes for any pre-wake length. The ramp ran as one
  worker that WorkManager stopped after ten minutes, so a 30-minute sunrise
  stalled around a third brightness and restarted from the bottom. Brightness
  is derived from the clock and the ramp hands over between segments.
- The snooze limit no longer turns off an alarm that still has a dismiss
  challenge waiting. Once the limit is spent, snooze is withdrawn and the alarm
  keeps ringing until you solve the challenge.
- The snooze limit is editable per alarm, under Dismiss, and the firing screen
  shows how many snoozes are left.
- Alarms now arm on Android 12 and 12L. The app declared only
  `USE_EXACT_ALARM`, which the platform ignores below API 33, so exact
  scheduling was refused and every alarm, bedtime reminder and snooze
  countdown was cancelled instead of set.
- `acx://navigate` links from other apps are matched against an allowlist
  before they reach the navigation graph. An unknown path used to throw out of
  the navigation host and take the process with it.
- The Wi-Fi dismiss challenge checks the network you saved on the alarm.
  Any connected network used to unlock it.
- Button labels, checked switch thumbs and swatch checkmarks now follow the
  accent colour you picked. On the lighter presets they were near-white on
  near-white, so the Create alarm label was effectively invisible. The violet
  preset was brightened so it also reads as label text on a card.
- The Edit row in an alarm's overflow menu has an icon again, matching the
  other five rows.
- Alarm editor category cards wrap their content instead of reserving a fixed
  height, which removes the empty band under every one-line summary.
- Every screen now shows the time the same way. Fifteen places wrote the
  12-hour and 24-hour split out by hand, three of them spelling "AM" and "PM"
  in English no matter what language the phone is set to, so the alarm card,
  the ringing notification and the bedtime cycle list could each render the
  same alarm differently.
- Notifications, notification channel names, the Wear tile and the widget's
  empty states can be translated. That includes the channel descriptions in
  Android's own settings, which is where you look when an alarm did not make a
  sound.
- The bedtime card stops saying "stopping" when sonar tracking is really off.
  It checked its own status text, which is overwritten a moment later, so it
  gave up after one try.
- The Wear tile is painted in the app's colours instead of the stock Wear
  palette, and its text is joined with a middle dot rather than a hyphen that
  read as a minus sign.
- The challenge picker lists every challenge the app has. It was a
  hand-written list, which is how four challenges shipped in v1.6.0 that you
  could not choose.

## [1.15.33] - 2026-08-12

### Changed

- Drained the actionable roadmap: completed items are archived above, while
  behavior requiring product, safety, visual, or device decisions is recorded
  in `Roadmap_Blocked.md`.

### Internal

- Continued the god-file refactor by extracting Bedtime sleep tracking,
  sleep sounds, sleep-cycle rows, and wind-down checklist sections into focused
  sibling files without changing the screen behavior or public entry point.
- Split the Settings screen's readiness, integrations, personalization, and
  backup/restore categories into focused sibling files while preserving its
  adaptive pane shell and shared controls.
- Split the AlarmEdit screen into focused Overview, Sound, Dismiss, Schedule,
  Wake, Integrations, Advanced, and shared-support files while preserving its
  existing page routing and callbacks.
- Completed the Media3 alarm-audio stall path for both local tones and internet
  radio: bounded READY watchdogs now force eligible speaker/max-volume routing,
  record the failure, and fall back immediately to the legacy player.
- Added a one-off “snooze until” clock-time picker. The selected next
  occurrence is carried through the service contract and persisted as the
  alarm's next trigger, with exact-alarm scheduling and an inexact fallback.
- Added an ongoing snooze countdown notification. Android 16 uses
  `ProgressStyle` live updates, while older versions get a chronometer
  fallback; the notification is cleared when the alarm re-fires or is
  dismissed.
- Added per-OEM battery/autostart settings candidates for Samsung, Xiaomi,
  Oppo/Realme, Vivo/iQOO, and OnePlus, with safe generic fallbacks when a
  vendor activity is unavailable. OS build-fingerprint changes now re-open
  the final wake-readiness checklist after an OTA.
- Added opt-in on-call mode for rotating-shift alarms. With notification-policy
  access, total-silence DND is temporarily moved to Alarms-only during the
  ring and restored afterward; the device-local setting is never copied by
  backup.
- Hardened every AlarmManager registration fallback, including Direct Boot,
  smart-wake, timers, bedtime countdowns, and snoozes, so OEM or permission
  failures are logged without crashing the host path.
- Added a default-on alarm status-icon preference. Users can disable the icon
  while retaining exact idle scheduling, and changing the preference immediately
  re-registers enabled alarms with the selected path.
- Added a configurable firing-screen dismiss hold duration, from 0.5 to 5
  seconds, with backup/restore support and a live preference on the alarm UI.
- Completed the firing-screen TalkBack audit: touch exploration selects button
  controls, dismiss/snooze actions use large targets with explicit semantic
  labels and states, and challenge progress is announced as it changes.
- Verified alarm-related foreground services remain on the `mediaPlayback` type;
  no `shortService` migration is used, avoiding Android 15's short-service timeout.
- Added a bounded 15-second partial wake lock around normal and Direct Boot
  alarm receiver-to-foreground-service handoffs, covering OEM Doze delays without
  extending receiver work or holding a long-lived delivery lock.
- Audited the Android 16 Pixel “missed alarm, unknown reason” report against
  the current QPR notes. ACX now promotes the alarm foreground service
  synchronously, bridges cold-start delivery with a bounded wake lock, and has a
  post-fire watchdog; Pixel/QPR device confirmation remains external.
- Revalidated partial wake-lock use against Play's excessive-wake-lock metric:
  alarm audio stays on the exempt `mediaPlayback` path, delivery is bounded to
  15 seconds, and non-exempt smart wake is capped at 65 minutes per session and
  released during teardown.
- Verified Hue sunrise uses v2 `hue-application-key` authentication over HTTPS
  with TOFU certificate pinning; the legacy v1 HTTP fallback remains explicit
  and disabled by default.
- Added an About-screen “Share crash log” action that creates a locally scrubbed
  text export and opens the system share sheet; crash logs are never uploaded
  automatically.
- Revalidated the local-only release workflow: Play and F-Droid variants share
  the common surface, their unit-test/lint gates pass together, and signing,
  metadata, OSV, reproducibility, and checksum scripts remain local with no
  GitHub Actions workflow added.
- Added a fail-closed signing check to the Play, F-Droid, and Wear release
  artifact tasks, including the Play AAB path, so missing signing material
  cannot produce a publishable unsigned artifact.
- Added a release verification task that builds the F-Droid APK and rejects
  artifacts above the documented 40 MiB size budget.
- Added an Android 13+ app-language picker backed by `LocaleManager`, with a
  system-default reset and English as the first declared bundled locale.
- Expanded `AlarmDatabaseMigrationTest` to exercise every exported schema path
  through the latest version and added a debug-build schema-diff verification
  task so reviewed Room exports cannot be left uncommitted.
- Added an emulator smoke that schedules the onboarding test alarm through
  `AlarmManager`, turns the screen off, and verifies the lock-screen activity
  wakes and resumes with the expected window flag.
- Wired release metadata verification into Gradle release/check tasks and
  extended it to require the latest changelog heading to match the app version.
- Documented the complete webhook contract for Tasker, MacroDroid, and Home
  Assistant, including event coverage, signed payload verification, stable
  retry identities, and delivery semantics.
- Added folder-based ringtone import through the system document-tree picker;
  granted audio folders are persisted as content URIs and their supported
  files appear alongside system tones for preview and selection.
- Switched alarm-edit navigation to Compose's predictive-back handler so an
  unsaved editor keeps its discard guard while participating in the system
  back gesture.
- Closed two roadmap gaps through existing generalized paths: per-alarm
  morning-routine checklists cover dismiss-time pet-feeding chains, and the
  weather-aware scheduler already advances snow/ice alarms by the configured
  early-wake interval.

## [1.15.32] - 2026-07-29

Fix release for GitHub issue #43.

### Fixed

- **Dismiss could bypass the challenge entirely.** The ringing screen's initial
  state has no challenge object yet, which the dismiss gate read as "no challenge
  configured", so Dismiss was live for the whole of the alarm load (a Room read
  plus DataStore, event-stats and weather-cache reads). Tapping Dismiss the instant
  the screen appeared turned the alarm off without ever showing the challenge.
  Dismiss now stays locked until the alarm row has loaded and the challenge
  requirement is known. The accessibility bypass is deliberately outside that gate,
  and a failed load fails open, so an alarm can never become undismissable.
- **Custom typing phrases had no effect.** `Settings → Custom typing phrases` was
  written to DataStore and round-tripped through backup, but the firing screen
  called the challenge generator without it, so Type a Phrase and Voice Phrase
  always drew from the built-in list. The setting is now read at fire time and
  passed through.

### Internal

- Challenge construction moved to a pure top-level `buildChallenge()` so the
  custom-phrase wiring is directly testable.
- Added the missing `guava-parent` 33.3.1-jre dependency-verification entry, which
  was failing every Gradle invocation in the repo.

## [1.15.31] - 2026-07-22

Reliability release: proactive recovery for silently-missed alarms and a
Do-Not-Disturb mute warning.

### Added

- Do Not Disturb mute warning: the wake-readiness screen now flags when the
  device is in total-silence Do Not Disturb, which mutes even alarms, with a
  one-tap link to Do Not Disturb settings. Priority and Alarms-only modes are
  correctly treated as safe (alarm audio bypasses them).
- Proactive fire watchdog: a check is now enqueued two minutes after every
  scheduled alarm and, if AlarmManager silently failed to deliver the fire (no
  `BROADCAST` incident for that occurrence), re-fires the alarm through the same
  service path as wake-confirmation. This catches the failure class the reactive
  on-unlock replay can't see: the alarm that never rang at all (Pixel "missed
  alarm, unknown reason", OEM Doze kills). It shares the existing "repeat missed
  alarms" opt-in and can never double-fire a working alarm, since a delivered
  fire always leaves a broadcast record. Device validation of the live re-fire
  under Doze remains a follow-up.

## [1.15.30] - 2026-07-22

Maintenance release: test-coverage expansion, dead-resource cleanup, and the
start of the god-file refactor. No user-facing behavior changes.

### Changed

- Began splitting the oversized `BedtimeScreen.kt` into focused sibling files:
  extracted the jet-lag planner, chronotype, and guided-breathing sections into
  `BedtimeJetLagSection.kt`, `BedtimeChronotypeSection.kt`, and
  `BedtimeBreathingSection.kt` (screen shrank from ~2.1k to ~1.65k lines with no
  behavior change). Settings and alarm-edit screens are next.

### Removed

- Deleted 61 orphaned string resources (pre-split alarm-edit summaries,
  onboarding copy, unused challenge/nav/notification labels) left unreferenced
  after the localization sweep. Verified zero `R.string`/`@string` references
  across the app and wear modules and no dynamic `getIdentifier` lookups before
  removal; both flavors and the wear module still build clean.

### Tests

- Added `WebhookRetryWorkerTest` covering the `WebhookRetryWorker.doWork()`
  outcome-to-Result mapping (delivered/skipped succeed, failed retries under the
  cap and gives up at it) plus the input-validation fast-fail paths, driven in
  the JVM via `androidx.work:work-testing`.
- Extended `JetLagPlannerTest` with the previously-uncovered AUTO-resolves-to-
  DELAY case, the exact 12-hour advance/delay tie-break, and a user-forced
  direction that opposes the shorter automatic arc.

## [1.15.29] - 2026-07-17

Deep audit pass over the never-audited work since v1.15.27 (timer restart,
fixed-zone alarms, Fossify import, jet-lag planner, the localization sweep)
plus the core alarm and firing paths.

### Fixed

- Overlapping alarms no longer strand one another: an alarm firing while
  another is still ringing now finalizes the preempted alarm (records the
  missed outcome and re-arms its next occurrence) instead of leaving a
  recurring alarm with a stale past trigger and no armed alarm, silently
  losing every future occurrence until the next reboot.
- The repeat-missed-alarm safety net works again. Its unlock/unplug triggers
  were declared only in the manifest, where Android never delivers them on
  modern versions, so the whole feature was dead; the receiver is now
  registered at runtime. Replay also skips alarms disabled after the miss and
  honors the hide-labels-on-public-surfaces setting on its fallback notice.
- A timer created from a notification Restart or a voice assistant can no
  longer be silently destroyed by the in-app timer screen reusing its id;
  timer ids are now allocated under the shared store lock, and the Timer tab
  resyncs external changes when it returns to the foreground.
- A recurring alarm dismissed after a smart-wake early fire no longer rings a
  second time at its original minute.
- A one-shot alarm suppressed by holiday auto-skip no longer resurrects and
  fires on the holiday after a reboot or app update.
- Holiday auto-skip now evaluates the date in a fixed-zone alarm's own time
  zone and can no longer land a skipped occurrence inside a vacation window.
- Saving an alarm inside its snow/ice weather-lead window no longer produces a
  past trigger that fires immediately.
- Snoozing after exact-alarm permission was revoked mid-ring now arms an
  inexact wake-up instead of silently vanishing while the UI shows "snoozed".
- Automatic reschedules (dashboard weather/location refresh, clock changes) no
  longer silently cancel a live snooze.
- Hitting the snooze cap now schedules the same wake-confirmation follow-up a
  manual dismiss gets.
- A ringtone pool combined with dismiss-at-ringtone-end no longer shrinks the
  guaranteed ring to a ~15 second tail.
- The stopwatch no longer drops its running segment when the wall clock is
  adjusted (NTP, travel, manual set); reboot detection now uses the OS boot
  counter.
- Timer alert sound no longer leaks a MediaPlayer when preparation fails, and
  fixed notification ids were moved out of the per-timer id band to stop
  collisions with running-timer notifications.

### Changed

- The alarm editor follows your chosen accent color instead of always showing
  blue for selected values and sliders.
- Timer, bedtime-reminder, and missed-alarm notification copy and channel
  names, jet-lag planner text, and several Fossify-import and webhook strings
  are now localizable resources; count-dependent text uses proper plurals.
- The webhook delivery log shows an empty-state hint and colors success from
  the structured status token rather than a substring match.
- Fossify import previews show localized weekday names, respect the 12/24-hour
  setting, and report failures with calm fixed copy instead of raw errors.

### Accessibility

- Manual alarm reordering is now reachable with TalkBack through Move up / Move
  down actions on the drag handle.
- The numpad time entry announces its readout and validation errors and, when
  it is the sticky entry mode, opens prefilled with the current time instead of
  empty with a disabled Save.
- Night Clock's OLED burn-in drift is no longer disabled by the reduce-motion
  setting (it is a hardware safeguard, not decorative motion) while the glow
  pulse stays gated.

### Added

- Finished timer notifications now offer Restart. The action stops that alert,
  atomically creates one fresh timer with the same duration and label, and
  schedules it without opening the app; duplicate action delivery is ignored.
- Alarm time selection now offers an optional four-digit numpad alongside the
  existing clock picker. It validates 24-hour input and provides explicit
  AM/PM selection in 12-hour mode; the timer duration keypad remains available.
- The Escalating vibration pattern now uses Android 16's hardware-adaptive
  basic haptic envelopes when supported, with a smooth zero-to-peak ramp,
  alarm-class vibration attributes, Gentle/Strong per-alarm strength, and the
  existing repeating waveform as the automatic fallback on older hardware.
- Android 16 running timers, active snoozes, and Hue sunrise ramps now expose
  promoted `ProgressStyle` Live Updates with countdown chips. Timers and Hue
  keep a quiet ongoing chronometer notification on older Android versions,
  reuse stable IDs across updates, and remove the surface on pause, completion,
  cancellation, or failure.
- A new default-off after-dismiss day summary turns the existing morning card
  into a real cached-data handoff: current conditions, today’s high/low and
  precipitation, the next local calendar event, and the alarm routine appear
  without making a network request; closing it returns to the prior home flow.
- Alarms can now either follow the device time zone or stay pinned to a valid
  IANA zone while traveling. Fixed-zone scheduling covers recurring, one-shot,
  shift, solar, and DST-transition cases; the policy survives backup, sharing,
  Direct Boot, Wear sync, widgets, and support diagnostics, while legacy or
  invalid data safely follows the device.
- Settings can now preview and import bounded Fossify Clock JSON exports.
  Time, weekday mask, label, vibration, and readable ringtone URIs are mapped;
  invalid rows and inaccessible sounds are disclosed before confirmation, and
  the entire append is one disabled-by-default Room transaction.
- Commute-aware calendar alarms now retain a bounded, 45-day on-device history
  of successful live route durations and can use a conservative learned
  estimate during transient API failures. Route keys are hashed, sparse/stale
  data falls back normally, Settings can clear the history, and support bundles
  expose aggregate counts only.
- A device-local accessibility preference and Android's system Remove
  animations setting now stop decorative alarm, challenge, timer, weather,
  loading, and Night Clock loops. The same policy prevents optional flashlight
  strobing, which remains off by default and now carries a seizure warning.
- An opt-in accessibility setting can reduce alarm-player volume to 20-65%
  while a dismiss challenge is active. Challenge completion restores the live
  fade level, phone calls still mute completely, and system volume is untouched.
- Ringtone pools now combine their existing per-fire track shuffle with a
  bounded random start offset for clips at least 30 seconds long, while always
  preserving the final 15 seconds and supporting both playback backends.
- The alarm editor now opens as a compact overview with live summaries for
  sound, dismissal, scheduling, wake behavior, integrations, and advanced
  controls. Focused category pages keep one draft across navigation and adapt
  from one-column phone layouts to two-column wide layouts.
- A local release-metadata gate now keeps app/Wear versions, README artifacts,
  F-Droid declarations, the API-37 verifier, Room schemas, and backup format
  declarations in sync without relying on hosted CI.
- Stats now shows a "Wake consistency" score, a 0-100 measure of how steady your
  wake-up times have been, computed entirely on-device from your recent alarm
  dismisses (using circular statistics so times either side of midnight are
  handled correctly). No cloud, no extra permissions.

### Changed

- Settings and alarm editing now render stable keyed sections through
  `LazyColumn`, keeping offscreen controls out of composition while preserving
  pane/page scroll reset and all existing field state.
- Alarm firing, alarm editing, and Settings text and accessibility labels now
  use Android string resources, including localized dynamic summaries and
  locale-aware dates. XML lint and a primary-screen Compose verification task
  now reject new hardcoded UI copy.
- The 2026-07-15 dependency review updated Moshi to 1.15.2, refreshed release
  locks and SHA-256 verification metadata, and added a release-lock gate against
  alpha, beta, and release-candidate dependencies. NewPipe, youtubedl-android,
  and OkHttp were already current; Room/WorkManager remain AGP9/KSP2-blocked.

### Fixed

- Alarm reliability checks now run immediately after boot and app updates as
  well as every six hours, detect Android background restriction alongside
  battery/exact-alarm/notification regressions, and route warnings directly to
  the existing OEM-specific reliability guidance.
- Bedtime now reports Sonar as active only after its foreground service, tone
  output, and microphone recorder are all running; failed or stalled starts
  remain off and surface a retryable status instead of false monitoring.
- Toolbar and system back no longer silently discard alarm-editor changes: a
  full-draft dirty check now presents a keep-editing/discard confirmation while
  save progress and derived forecast state remain outside the dirty boundary.
- Android voice assistants and automation apps now reach a complete,
  permission-protected AlarmClock contract: alarm/timer creation, active-alarm
  snooze/dismiss, warm-intent delivery, malformed-input rejection, duplicate
  coalescing, and the platform's optional show/skip-UI behavior share one path.
- Countdown timers now use one foreground service for every expiry, whether the
  app is visible, backgrounded, or absent. The persisted finish transition is an
  atomic delivery claim, simultaneous expiries share one player/vibration alert,
  and automatic silence preserves the finished timer for later recovery.
- The complete Play unit suite is reliable again: host tests no longer leak
  application-owned Room observers across Robolectric sandboxes, and scheduler
  tests use an isolated application instead of inheriting seeded alarms.

### Security

- The existing public-label privacy control now covers countdown timers: the
  ringing and passive finished notifications keep labels in private content
  while publishing generic lock-screen and screen-sharing versions.
- Play, F-Droid, and Wear release dependencies now resolve against reviewed
  SHA-256 metadata and strict runtime lockfiles. The repeatable refresh also
  exercises release lint/plugin artifacts, and the Play downloader graph now
  constrains Jackson to 2.18.9 to close its remaining OSV advisory.
- Hue connection tests, sunrise workers, and dismiss-scene actions now share one
  trust-on-first-use TLS client and certificate pin. A changed bridge
  certificate is rejected without downgrading to HTTP; legacy API v1 is tried
  only when its explicit warning-backed setting is enabled, and Settings offers
  a confirmed certificate-reset action for verified bridge replacements.
- News feed article links are now opened only when they are plain `http`/`https`
  URLs. Because the feed source is user-configurable and item links come from
  untrusted RSS/Atom, a hostile or compromised feed could previously hand an
  `intent:`, `javascript:`, `file:`, or arbitrary deep-link URI to the system on
  tap; those are now rejected (and non-web links render as non-tappable).

### Fixed

- The reproducible-build gate now works from LF-safe checkouts, reports Gradle
  failures instead of hiding stderr, and discovers the current F-Droid release
  APK name rather than assuming the obsolete `-unsigned` suffix.
- An alarm can no longer ring silently if its ringtone player stalls: if the
  Media3 player never starts within a short grace period (e.g. a hung decoder or
  stuck stream that emits no error), the alarm now falls back to the guaranteed
  default tone instead of staying silent.
- A sleep-tracking (Sonar) session interrupted by an OS kill now persists what it
  captured (sleep-stage summary and snore timeline) instead of silently
  discarding the whole night; previously only an explicit stop saved the session.
- Weather-based early-wake and the alarm firing screen no longer show the wrong
  city's weather: cached forecasts are now only reused when they were fetched
  for the current location, so a recent cache from a previous location can't skew
  alarm timing after you move or change your saved location.
- Holiday auto-skip no longer misses holidays in a future year: the in-memory
  cache fast path now verifies the queried year is actually covered before
  answering, instead of reporting "not a holiday" for a year that was never
  fetched (the scheduler probes up to a year ahead).
- Countdown timers are no longer dropped or resurrected under concurrent writes:
  timer persistence now serializes its read-modify-write across the countdown
  coroutine and the expiry receiver.
- The stopwatch now survives leaving the app: a running or paused stopwatch and
  its laps are restored when you return, instead of resetting on process death.
  (A reboot, which resets the monotonic clock, restores the accumulated time as
  paused rather than guessing the elapsed running segment.)

### Added

- The alarm editor now shows a live "Rings in …" countdown under the time so you
  can see how far away the alarm is without doing the mental math.
- Wake-critical webhook events (`alarm_fired`, `alarm_missed`) are now retried
  with exponential backoff via WorkManager when the fire-time delivery fails, so
  a transient network blip no longer silently breaks a Tasker/automation flow
  that depends on them. The retry carries the same event identity so receivers
  can dedupe. Non-critical events remain fire-and-forget.
- Webhook settings now show a rolling "Recent deliveries" log (last several
  attempts with local timestamps and status) instead of only the single last
  status line.

### Fixed

- The "Use phone speakers" setting now actually forces alarm audio to the
  built-in speaker when accessories are connected, previously the toggle was
  wired to the UI but never applied to playback, so an alarm could still ring
  silently through connected wired/Bluetooth headphones. System-managed
  hearing-aid / BLE routing is deliberately left untouched.
- The auto-silence "missed alarm" outcome (missed event, incident record,
  webhook, broadcast, notification, and repeat-missed state) is now written
  atomically under `NonCancellable`, so a dismiss arriving at the exact
  auto-silence instant can no longer cancel the service mid-write and drop a
  half-recorded miss.
- When a missed-alarm replay can't start its foreground service on
  background-restricted OEMs, the app now posts a high-importance
  full-screen-intent notification as a fallback instead of failing silently.
- Alarms set to a wall-clock time inside the spring-forward DST gap (e.g. 02:30
  on a US spring-forward morning) now fire at the moment the skipped hour ends
  instead of silently drifting a full hour past the requested time. Fall-back
  (overlap) times keep the first occurrence. Policy is now explicit and tested.
- Squat and push-up dismiss challenges are no longer undismissable on devices
  without an accelerometer: the firing screen now surfaces a "Continue without
  sensor" fallback (matching the walk/Wi-Fi challenges) when no motion sensor is
  present. Previously the only escape was force-stopping the app, since the
  challenge-bypass timer is off by default.

### Accessibility

- Challenge progress rings (squats, push-ups, steps, plank seconds, and other
  count-based challenges) now announce each increment to TalkBack via a polite
  live region, so non-visual users get feedback that a rep registered.

## [1.15.28] - 2026-07-06

### Added

- Added a local Bedtime jet-lag planner that shifts wake and bedtime targets
  gradually over the selected number of days.
- Added bright-light and dim-light timing rows for each adjustment day, with
  auto, earlier, and later direction controls.
- Added backup v17 round-trip support for jet-lag planner settings.

### Changed

- Bumped app, Wear, README, release verifier, backup format, and F-Droid
  metadata to `versionName = "1.15.28"`, `versionCode = 130`.

## [1.15.27] - 2026-07-06

### Added

- Added rotating shift-pattern scheduling for DDNNO, 4-on-4-off, Panama,
  DuPont, and Pitman cycles. A selected pattern can act as the recurring
  schedule by itself or narrow existing weekday selections.
- Added Alarm Edit controls for shift pattern and cycle start date, plus alarm
  list chips so shift-gated alarms are visible at a glance.
- Added Room v22 migration coverage and backup v16 round-trip support for
  shift-pattern fields.

### Changed

- Bumped app, Wear, README, release verifier, Room schema, backup format, and
  F-Droid metadata to `versionName = "1.15.27"`, `versionCode = 129`.

## [1.15.26] - 2026-07-05

### Added

- Added Bedtime pre-sleep factor tiles for caffeine, exercise, alcohol, and
  stress with local 30-day retention.
- Added a local pre-sleep correlation chart that compares tagged nights against
  Sonar/smart-wake restlessness summaries without uploading data.
- Added a Room `pre_sleep_tag_entries` table with v21 migration coverage.

### Changed

- Bumped app, Wear, README, release verifier, Room schema, and F-Droid metadata
  to `versionName = "1.15.26"`, `versionCode = 128`.

## [1.15.25] - 2026-07-05

### Added

- Added local Sonar snore/loud-sound event detection: sessions now persist
  compact start/end/duration/peak/average dB metadata for bursts over the local
  threshold and render the recent timeline on the Bedtime tab.
- Added a Room `snore_events` table with v20 migration coverage and 30-day
  retention through the new repository layer.

### Changed

- Updated Sonar sleep copy to clarify that movement and loud sleep-sound
  monitoring runs only while the foreground session is active and retains no
  raw audio.
- Bumped app, Wear, README, release verifier, Room schema, and F-Droid
  metadata to `versionName = "1.15.25"`, `versionCode = 127`.

## [1.15.24] - 2026-07-05

### Added

- Added a local Bedtime chronotype estimate with five compact preference
  prompts, an early/balanced/late type label, and an ideal bedtime/wake window
  derived from the current sleep goal.
- Chronotype answers now persist through app settings and manual JSON backup
  export/import; older backups still import with safe defaults.

### Changed

- Bumped app, Wear, README, release verifier, backup format, and F-Droid
  metadata to `versionName = "1.15.24"`, `versionCode = 126`.

## [1.15.23] - 2026-07-05

### Added

- Added a local bedtime room-noise baseline: if microphone permission is
  already granted, the bedtime reminder samples ambient RMS briefly, stores
  only a quiet/moderate/loud label plus timestamp, excludes that label from
  backup/transfer, and adjusts the reminder copy when the room is noisy.
- Surfaced the last room-noise baseline on the Bedtime tab.

### Changed

- Bumped app, Wear, README, release verifier, and F-Droid metadata to
  `versionName = "1.15.23"`, `versionCode = 125`.

## [1.15.22] - 2026-07-05

### Changed

- Documented that crash logs are capped local files in app-private storage,
  excluded from backups, never uploaded automatically, and leave the device
  only when the user exports a support bundle.
- Added the same crash-log disclosure to F-Droid metadata and clarified that
  crash logs are not part of the optional network-access surface.
- Bumped app, Wear, README, release verifier, and F-Droid metadata to
  `versionName = "1.15.22"`, `versionCode = 124`.

## [1.15.21] - 2026-07-05

### Added

- Added Anti-Sleepyhead location dismissal locks: each alarm can save a place
  and radius, then keep Dismiss disabled until the phone leaves that area.
- The firing screen now shows a dedicated location-lock state while Snooze
  remains available, including distance/status feedback from platform
  LocationManager providers.

### Changed

- Bumped app, Wear, README, release verifier, and F-Droid metadata to
  `versionName = "1.15.21"`, `versionCode = 123`.

## [1.15.20] - 2026-07-05

### Changed

- Repeat missed alarms now re-fire once after a recent auto-silenced alarm when
  the user unplugs the phone, matching the existing unlock recovery path.
- Shared the unlock and power-disconnect recovery triggers behind the same
  stale-state and live-alarm replay guard, with trigger-specific diagnostics.
- Bumped app, Wear, README, release verifier, and F-Droid metadata to
  `versionName = "1.15.20"`, `versionCode = 122`.

## [1.15.19] - 2026-07-05

### Added

- Added a guided Bedtime breathing timer with 4-7-8 and box-breathing modes,
  per-phase countdown cues, and local-only in-screen timing.

### Changed

- Bumped app, Wear, README, release verifier, and F-Droid metadata to
  `versionName = "1.15.19"`, `versionCode = 121`.

## [1.15.18] - 2026-07-05

### Changed

- Wrapped Timer preset chips into a compact multi-row layout so compact phones
  no longer clip the final preset.
- Cleaned News summary text after feed markup removal so adjacent links and HTML
  entities keep readable word spacing.
- Bumped app, Wear, README, release verifier, and F-Droid metadata to
  `versionName = "1.15.18"`, `versionCode = 120`.

## [1.15.17] - 2026-07-02

### Changed

- Polished backup and restore recovery copy, loading announcements, and
  restore-preview decisions so failure and data-safety states read clearly.
- Clarified world-clock search, dismiss-challenge actions, timer keypad
  semantics, quick alarm chips, sleep tracking controls, and ringtone-pool
  validation for stronger accessibility and recovery.
- Bumped app, Wear, README, release verifier, and F-Droid metadata to
  `versionName = "1.15.17"`, `versionCode = 119`.

## [1.15.16] - 2026-07-02

### Added

- Added optional webhook HMAC signing with `X-ACX-Timestamp` and
  `X-ACX-Signature` headers, recent delivery status in Settings, and backup
  v14 round-trip/export-warning coverage for the signing secret.
- Added backup import preview with compatibility, alarm counts, private-value
  disclosure, and append/replace/import-disabled restore choices before any
  Room or DataStore writes occur.
- Added last-good weather and news caches with stale-state banners and retry
  affordances when refresh fails offline.
- Added expanded-width Alarms list/detail and Settings category/detail panes
  for tablets, foldables, Chromebooks, and DeX while preserving the existing
  compact phone flow.

### Changed

- Extracted AlarmService haptic, flashlight, and post-dismiss TTS, briefing,
  and wake-confirm decisions into controller-sized units with focused tests;
  no alarm firing behavior change intended.
- Bumped app, Wear, README, release verifier, and F-Droid metadata to
  `versionName = "1.15.16"`, `versionCode = 118`.

## [1.15.15] - 2026-07-01

### Added

- Added Android 16 `Notification.ProgressStyle` Live Updates for the final
  hour before the bedtime reminder fires, using a low-importance countdown
  notification channel and promoted ongoing request metadata.
- Added bedtime countdown timing tests for final-hour gating, progress math,
  refresh cadence, and daily reschedule rollover.

### Changed

- Bedtime reminder scheduling now pairs the terminal reminder PendingIntent
  with its countdown PendingIntent and cancels both together when bedtime is
  disabled.
- Bumped app, Wear, README, roadmap, release verifier, and F-Droid metadata to
  `versionName = "1.15.15"`, `versionCode = 117`.

## [1.15.14] - 2026-07-01

### Added

- Added a Media3/ExoPlayer alarm playback backend for app-owned alarm tones and
  internet radio, gated by `USE_MEDIA3_ALARM_PLAYER` with the legacy MediaPlayer
  path kept available for platform ringtone-provider fallbacks this release.
- Added Media3 alarm-audio routing tests and build-flag coverage so alarm
  playback continues to use Android's system alarm channel.

### Changed

- Alarm audio startup now records Media3-specific success, legacy-handoff, and
  failure incident reasons for better support diagnostics.
- Raised the Gradle daemon heap to 4 GiB so local release R8 packaging completes
  reliably with the Media3 dependency graph.
- Bumped app, Wear, README, roadmap, release verifier, and F-Droid metadata to
  `versionName = "1.15.14"`, `versionCode = 116`.

## [1.15.13] - 2026-07-01

### Added

- Added Manual order mode for the alarms list with drag handles and persisted
  `Alarm.sortOrder` values.
- Added Room v19 migration, backup v13 round-trip coverage, and deterministic
  reorder helper tests for manual alarm ordering.

### Changed

- New and duplicated alarms are placed at the end of the manual list.
- API 37 release verification now retries transient UI hierarchy dumps while
  driving the onboarding test alarm.
- Bumped app, Wear, README, roadmap, release verifier, and F-Droid metadata to
  `versionName = "1.15.13"`, `versionCode = 115`.

## [1.15.12] - 2026-07-01

### Added

- Added optional per-alarm ringing-screen background images with persisted
  document access, Android 12+ blur, and default-off behavior for existing
  alarms.
- Added Room v18 migration, backup/share round-trip coverage, import stripping,
  and export-warning disclosure for selected background image URIs.

### Changed

- Bumped app, Wear, README, roadmap, release verifier, and F-Droid metadata to
  `versionName = "1.15.12"`, `versionCode = 114`.

## [1.15.11] - 2026-07-01

### Added

- Added commute-aware first-meeting auto-alarms. Calendar events with a location
  can now shift earlier from transit ETA when a user supplies a Google Routes API
  key, while the no-key path still adds a bad-weather buffer for snow, ice,
  storms, or heavy precipitation.
- Added Settings controls, backup round-trip coverage, and unit tests for the
  commute lead-time policy.

### Changed

- Bumped app, Wear, README, roadmap, release verifier, and F-Droid metadata to
  `versionName = "1.15.11"`, `versionCode = 113`.

## [1.15.10] - 2026-07-01

### Changed

- Centralized alarm audio routing attributes so alarm tones, internet radio
  alarm streams, fallback tones, Direct Boot alarms, setup test alarms, ringtone
  previews, and timer-finished tones all use system alarm routing. This keeps
  Android's hearing-aid/speaker alarm route choice in the platform path instead
  of forcing media routing from app code.
- Bumped app, Wear, README, roadmap, release verifier, and F-Droid metadata to
  `versionName = "1.15.10"`, `versionCode = 112`.

## [1.15.9] - 2026-07-01

### Added

- Added three permission-free dismiss challenges: Spot the Difference, Chess
  Mate in 1, and RSVP Speed Reading. They are selectable in the alarm editor,
  work in mission chains, and survive backup/share sanitizer round-trips.

### Changed

- Bumped app, Wear, README, roadmap, and F-Droid metadata to
  `versionName = "1.15.9"`, `versionCode = 111`.

## [1.15.8] - 2026-07-01

### Added

- Added `scripts/verify_api37_release.py`, a local release verifier for 16 KB
  APK zip alignment and API 37 device smoke checks. With an API 37 16 KB device
  serial, it installs the Play release, verifies exact-alarm,
  promoted-notification, notification, and local-network permission state, and
  can drive the built-in test alarm to completion.

### Changed

- Release instructions now include the Android 17 / 16 KB verifier alongside
  signing hygiene and OSV runtime graph auditing.
- The in-app release highlights now describe the current release-hardening work
  instead of the older alarm-fire harness copy.

## [1.15.7] - 2026-07-01

### Changed

- OSV dependency auditing is now a release gate across the Play, F-Droid, and
  Wear release runtime classpaths by default. Findings identify every affected
  runtime graph and fail the release check unless explicitly run with
  `--no-fail`.
- The OSV gate has a narrow resolved-advisory override for
  GHSA-5jmj-h7xm-6q6v when Jackson is on an upstream-patched release line,
  because OSV currently reports the advisory even after 2.18.9.
- Local release documentation now runs signing hygiene and the OSV runtime
  graph audit before publishing signed APKs.
- The new gate exposes GHSA-5jmj-h7xm-6q6v / CVE-2026-54515 as an unresolved
  Play-runtime blocker until Maven Central publishes a compatible patched
  Jackson 2.x artifact or the Play-only downloader graph changes.

### Fixed

- The F-Droid manifest now declares telephony hardware optional alongside its
  SMS permission, keeping ChromeOS and large-screen installs eligible.

## [1.15.6] - 2026-07-01

### Added

- Added a Robolectric alarm fire-to-dismiss smoke harness that verifies the
  local fire intent contract, firing activity launch intent, dismiss action,
  alarm event write, incident records, and active-service cleanup.

### Changed

- Centralized alarm fire, snooze, dismiss, firing-activity, and event payload
  construction through a shared `AlarmFireDismissContract` used by receivers,
  the firing UI, and `AlarmService`.

## [1.15.5] - 2026-07-01

### Added

- Sonar sleep tracking now has a real Bedtime control path: users can start or
  stop the experimental foreground microphone session from the Bedtime tab with
  microphone-permission recovery.
- Sonar sessions save compact local movement/restless/still summaries into the
  existing Statistics sleep-motion view. No raw microphone audio is retained.

### Changed

- Statistics now labels Sonar sleep-motion sessions separately from smart-wake
  phone-actigraphy sessions so the source and privacy posture are clear.

## [1.15.4] - 2026-06-27

### Added

- Handwriting dismiss challenge: users draw a displayed wake word on a Compose
  drawing pad. Play builds use ML Kit Digital Ink recognition with on-demand
  English model download; F-Droid builds keep a typed fallback so alarms remain
  dismissable without proprietary ML Kit dependencies.

### Fixed

- Per-alarm Hue dismiss actions now recall the configured Hue scene instead of
  logging a stub. Dismiss webhooks, broadcasts, and Hue scenes now execute
  through a testable executor with URL, action, local-network, and Hue payload
  validation.

## [1.15.3] - 2026-06-27

### Added

- Voice phrase dismiss challenge: users say the displayed phrase using Android
  SpeechRecognizer with offline preference, microphone permission recovery,
  and a typed fallback so denied-permission or no-recognizer devices stay
  dismissable.

### Fixed

- Alarm edit now exposes the existing PVT, push-up, and plank-hold dismiss
  challenges in the challenge picker instead of leaving them firing-engine-only.
- Wear release signing now resolves the shared `keystore.properties` storeFile
  path the same way the app module does, so the local release artifact build can
  sign `:wear:assembleRelease`.

## [1.15.2] - 2026-06-27

### Added

- Direct Boot fallback alarms now launch a direct-boot-aware full-screen stop
  UI before first unlock after reboot. The fallback notification also uses
  that activity as its full-screen and content intent.

### Changed

- ROADMAP.md now drops completed local actigraphy, smart-wake, lockscreen
  widget, and Direct Boot full-screen entries so it stays actionable-only.

## [1.15.1] - 2026-06-25

### Fixed

- Jackson-databind Play-only constraint bumped from 2.18.6 to 2.18.8 to
  address CVE-2026-54512 (PolymorphicTypeValidator bypass), CVE-2026-54513
  (array type validation bypass), and CVE-2026-54514 (SSRF via
  InetSocketAddress deserialization).
- yt-dlp engine minimum safe version gate added (`2026.06.09`). The
  bundled yt-dlp binary in youtubedl-android 0.18.1 predates fixes for
  CVE-2026-50574, CVE-2026-50023, CVE-2026-50019, and CVE-2026-26331.
  ACX mitigates all four CVEs at the application level (`--get-url` only,
  no file-write/aria2c/netrc/curl paths), but the engine version is now
  flagged in support diagnostics and users are encouraged to update.
- Backup settings drift test updated for `bedtimeStayUpLateUntilMillis`
  (transient field, intentionally excluded from backup round-trip).

### Added

- Per-alarm dismiss action: configurable webhook call, Hue scene, or
  broadcast that fires on successful alarm dismissal. New
  `dismissActionType` and `dismissActionPayload` fields on the Alarm
  entity. DB v16 to v17 migration. Shared-alarm imports strip dismiss
  actions for safety. Inspired by AlarmKit (iOS 26) custom dismiss
  actions pattern.
- Proactive alarm-health monitoring: periodic 6-hour WorkManager check
  detects when battery optimization, notification permission, or exact
  alarm permission has been revoked and warns the user before the next
  alarm silently fails.
- Stay-up-late bedtime override: +1h/+2h/+3h chips on the Bedtime tab
  delay tonight's bedtime reminder. Auto-reverts when the deadline expires.
- Pink noise and violet noise sleep sound presets join the existing
  white/rain/brown/ocean/fan library.
- Bedtime battery warning: yellow banner on the Bedtime tab when battery
  is at 15% or below and bedtime is enabled.
- Lockscreen widget: the next-alarm Glance widget now supports the
  `keyguard` widget category for Android lockscreen placement.
- Broadcast intents for alarm lifecycle events: documented action strings
  for ALARM_FIRED, ALARM_SNOOZED, ALARM_DISMISSED, ALARM_MISSED enable
  local automation via Tasker, Home Assistant Companion, and MacroDroid
  without network overhead.
- Google Assistant alarm intent provider: ACX now handles SET_ALARM,
  DISMISS_ALARM, SNOOZE_ALARM, SET_TIMER, and SHOW_ALARMS intents so
  voice commands route to the app when set as default clock.

### Changed

- Updated RESEARCH.md with 2026-06-25 exhaustive research pass findings
  (4 research agents: competitors, platform, security, community/UX).
- Added 13 new ROADMAP items from the research pass (2 P0 supply-chain,
  4 P1 reliability/security, 5 P2 product, 2 P3 accessibility/polish).

## [1.15.0] - 2026-06-19

### Added

- Push-up dismiss challenge: accelerometer-based push-up detection with the
  phone placed face-down on the floor. Uses Z-axis motion pattern to count
  reps. Shares the same readiness/fallback pattern as the existing squat
  challenge.
- Plank hold dismiss challenge: hold the phone level and face-down for 30
  seconds. Timer counts up while position is held; tap "I broke form" to
  pause and resume. No accelerometer validation in this first version -- the
  timer is honor-system-based with a manual break button.
- Challenge roster now 25 user-facing types.

## [1.14.19] - 2026-06-19

### Added

- Per-alarm squat challenge count: the squat dismiss challenge is now
  configurable (5/10/15/20/30/50) instead of hardcoded to 10 reps.
  DB v15 → v16 with `MIGRATION_15_16`.
- YouTube engine provenance tracking: bundled version, active version, last
  update time/status/source, and last failure reason are persisted to DataStore
  and surfaced in support diagnostics. Engine can be reset to bundled version.
- Settings "Connections and data" transparency panel lists each optional
  network provider with enabled/disabled state, domain, data sent, and offline
  fallback. Support diagnostics include a redacted "Connections" summary.
- Alarm edit progressive disclosure: "Dismiss and wake" and "Extras and
  integrations" sections are now collapsible groups with animated expand/collapse.
  Core alarm fields (time, label, group, sound, vibration, snooze) stay visible.
  Groups auto-expand when they contain active configuration.
- Widget quick-adjust: the home screen Glance widget shows "-10m" and "+10m"
  buttons when the next alarm is within 24 hours. Tapping adjusts the fire
  time without opening the app. Bounds-checked to prevent adjusting into the
  past.

### Changed

- First pass of string-resource extraction: notification titles, action labels,
  and wake-confirmation UI text moved to `strings.xml` (45 new entries). System
  surfaces (lock screen, status bar, notification shade) are now translatable.
  Remaining ~350 hardcoded strings are tracked for future i18n passes.

### Fixed

- Challenge views (Simon Says, Stroop, PVT) now use theme color tokens
  (`AccentRed`, `DismissGreen`, `AccentBlue`, `SnoozeYellow`) instead of raw
  hex values, so they respond correctly to custom accent color and dynamic color.
- AlertDialog surface color in BedtimeScreen and StatsScreen corrected from
  `SurfaceDark` (app background) to `SurfaceMedium` (dialog convention), matching
  YouTubeDownloadDialog and RingtonePickerSheet.
- WakeConfirmWorker now checks `isStopped` during its polling loop so a
  cancelled worker stops promptly and records a diagnostic incident instead of
  spinning until its 60-second deadline.
- Bedtime DND toggle now reverts the persisted preference when the system denies
  notification-policy access, preventing a stuck "on" state on next launch.
- Hue TOFU cert pinning uses `@Volatile` on the observed fingerprint and a
  first-writer-wins DataStore update so concurrent workers cannot overwrite each
  other's pin.
- Onboarding completion key is now versioned (`onboarding_complete_v1`) so a
  future onboarding redesign can re-show the flow to existing users.

### Changed

- Blocked roadmap items separated into `Roadmap_Blocked.md` for clarity.
- CI version-lint now fails if app source links to gitignored markdown files.
- CI version-lint now fails if any manifest permission lacks a README row.
- Added missing `ACCESS_WIFI_STATE` permission to README table.

## [1.14.18] - 2026-06-15

### Fixed

- YouTube downloader engine update now works: the `youtubedl-android` library's
  built-in updater fails because its bare `java.net.URL.openStream()` call to
  the GitHub API gets rejected (no Accept header). The app now falls back to a
  manual OkHttp-based updater that downloads the `yt-dlp` binary directly from
  GitHub releases with proper headers.
- The What's-new "What's next" action now opens a tracked README roadmap anchor
  instead of a local-only `ROADMAP.md` URL that is ignored and not published.
- Wake Readiness now surfaces proof from the real onboarding test alarm:
  scheduled time, fire time, dismissal time, latency, notification permission,
  full-screen request, and direct activity-launch status.
- Wear tile actions no longer claim they were "sent" after merely queuing an
  async message; the tile now waits for the Wear Data Layer send task, reports
  queued/failed status honestly, and blocks alarm controls when phone sync is
  stale.
- YouTube alarm-sound download failures now remain visible inside the active
  dialog instead of being reported only behind the modal.
- The ringtone picker no longer colors a failed YouTube download as a success
  after a previous save, and its limited-device-sound warning no longer points
  F-Droid users toward unavailable YouTube downloads.
- Alarm restore and shared-link imports now bound private-reference fields
  before they reach Room, preventing malformed imports from persisting
  oversized contact strings, URIs, routines, Wi-Fi names, or ringtone pools.
- Shared-alarm deep links no longer store the raw token as Activity duplicate
  state, avoiding oversized saved-state payloads from hostile or malformed
  links.
- Webhook test payloads now respect the "Include alarm labels" setting instead
  of always sending a sample label.
- Backup import failure logs no longer include the skipped alarm's label.

### Changed

- Refined the Compose alarm list, settings, dashboard, bedtime, and statistics
  status surfaces with a reusable inline notice treatment, clearer group/profile
  filter labels, more complete filtered-empty recovery, stronger selected-card
  semantics, and 44dp filter chips for more consistent touch targets.
- Refined first-run onboarding so the primary permission request asks only for
  alarm notifications, setup/test feedback uses the shared inline notice
  treatment, and world-clock city actions announce specific city names for
  screen-reader users.
- Added a tracked public Roadmap section to README and corrected the documented
  backup/restore format to v11.
- Ringtone preview, YouTube save, and device-sound-list warnings now use the
  shared inline notice treatment instead of isolated status text.
- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.14.18"`,
  `versionCode = 100`.

## [1.14.17] - 2026-06-15

### Fixed

- Alarm event analytics now record the actual scheduled fire time instead of the
  next-occurrence trigger time, which was wrong for repeating alarms after
  `handleAlarmFired()` recalculated the next trigger.
- SmartAlarmService `finishMonitoring()` race condition fixed: sensor-thread and
  main-thread callers can no longer double-fire the teardown path because the
  guard is now an AtomicBoolean with compareAndSet.
- AlarmService audio-start reentry guard upgraded from volatile boolean to
  AtomicBoolean, closing a narrow window where concurrent IO coroutines could
  both pass the check and leak MediaPlayer instances.
- NWS tornado/severe-weather alert fetch now checks that the location hasn't
  changed before updating the dashboard, matching the existing air-quality
  stale-read guard.
- Guardian Angel phone sanitizer no longer allows `*` and `#` characters, which
  could be interpreted as USSD codes in `tel:` URIs on some devices.
- AppMetricTile value text now clips with ellipsis instead of overflowing the
  tile on long weather or location strings.

### Changed

- Renamed `SettingsViewModel.refreshBatteryStatus()` to
  `refreshWakeReadiness()` to match what it actually does (refreshes battery
  state AND wake-readiness checks for notifications, exact alarms, standby
  bucket, and full-screen intent access).
- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.14.17"`,
  `versionCode = 99`.

## [1.14.16] - 2026-06-15

### Fixed

- YouTube downloader search, preview, engine update, and download failures now
  show plain recovery copy instead of raw exception messages.
- YouTube search now runs from the keyboard Search action when the query is
  ready.
- Paste-mode YouTube sound naming now closes the keyboard from the IME Done
  action.

### Changed

- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.14.16"`,
  `versionCode = 98`.

## [1.14.15] - 2026-06-15

### Fixed

- News feed errors now show plain recovery copy instead of raw network,
  parser, or HTTP exception messages.
- News cards without usable links no longer expose a tappable dead-end target.
- News refresh cancellation is preserved instead of being converted into a
  stale visible error state.
- Encrypted backup passphrase confirmation now uses inline mismatch feedback
  and closes the keyboard from the IME Done action.

### Changed

- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.14.15"`,
  `versionCode = 97`.

## [1.14.14] - 2026-06-15

### Fixed

- Refreshed the in-app "What's new" dialog so update copy matches the current
  physical-device polish release instead of describing the prior Android 17
  LAN-readiness pass.
- Replaced the remaining non-auto-mirrored volume-off icons in the alarm editor
  with the directional Material variant, clearing the Compose icon warning and
  improving RTL polish.

### Changed

- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.14.14"`,
  `versionCode = 96`.

## [1.14.13] - 2026-06-15

### Fixed

- The final onboarding permission screen now keeps the secondary "Continue
  without permissions" action visible on a real 1080x2316 phone and exposes an
  explicit button label to accessibility services.
- Removed the clipped final-page onboarding pager artifact that could appear
  above the permission-readiness card after compacting the action footer.
- Alarm-list enable switches now expose a single labeled switch target with
  enabled/disabled state instead of an unlabeled clickable wrapper.

### Changed

- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.14.13"`,
  `versionCode = 95`.

## [1.14.12] - 2026-06-15

### Fixed

- Tightened Android 17 local-network endpoint detection so public hostnames
  beginning with `fc` or `fd` are not mistaken for IPv6 ULA/link-local
  endpoints.
- Settings switch rows now expose a single switch semantics node instead of a
  nested row + visual switch announcement.
- Single-line Settings fields now use the keyboard Done action to commit the
  current draft and clear focus, improving IME behavior on small screens.

### Changed

- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.14.12"`,
  `versionCode = 94`.

## [1.14.11] - 2026-06-15

### Added

- Added Android 17 local-network readiness for Hue bridge and LAN webhook
  integrations: the manifest now declares `ACCESS_LOCAL_NETWORK`, Settings can
  request it when a local endpoint is configured, and Hue/webhook test actions
  explain permission denial instead of failing silently.
- Support diagnostics schema v2 now records whether local-network access is
  granted, so LAN integration failures have actionable evidence in support
  bundles.

### Changed

- Standardized existing app `OutlinedTextField` call sites on the shared
  `AppInputShape` token for more consistent form visuals across Settings,
  alarm edit, dashboard, world clock, ringtone, YouTube, and challenge screens.
- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.14.11"`,
  `versionCode = 93`.

### Fixed

- Replaced the stale Android 17 `LOCAL_NETWORK` permission name with
  `ACCESS_LOCAL_NETWORK` in user-facing docs and version lint.
- Updated the public challenge count to 23 after the PVT challenge shipped.

## [1.14.10] - 2026-06-14

### Added

- Added the psychomotor vigilance test (PVT) dismiss challenge.
- Added WCAG timed-challenge accessibility bypass support.

## [1.14.9] - 2026-06-14

### Security

- **Legacy Auto Backup no longer includes DataStore secrets on Android 8-11.**
  `backup_rules.xml` previously included the entire DataStore directory, which
  contains webhook URLs, Hue API keys, guardian phone numbers, custom typing
  phrases, and custom feed URLs. Cloud backup on Android 8-11 now includes only
  the Room alarm database, SharedPreferences, and photo-match reference files,
  matching the Android 12+ `data_extraction_rules.xml` cloud-backup exclusions.
  Device-to-device transfer (Android 12+) continues to include DataStore because
  that path stays on-device.

- **CI/release GitHub Actions pinned to immutable commit SHAs.** All three
  workflow files (`release.yml`, `android-ci.yml`, `version-lint.yml`) now
  reference third-party actions by full-length SHA instead of mutable version
  tags, preventing supply-chain substitution attacks through the release pipeline
  that handles signing secrets and repository write permissions.

## [1.14.8] - 2026-06-14

### Fixed

- The ringtone picker now warns when the device's system sound list can't be
  read, instead of silently showing only "Default Alarm" + "Silent" (which looked
  like a broken picker). Default/Silent and YouTube sounds remain available.

## [1.14.7] - 2026-06-14

Reliability guidance + hardening.

### Added

- Complete background-kill guidance for all aggressive OEMs. Oppo/Vivo/Realme
  (and Redmi/Poco/Honor/iQOO aliases) previously showed "needs guidance" with no
  steps; every flagged OEM now has concrete steps plus a per-vendor
  dontkillmyapp.com link in the Settings reliability card. `needsBatteryGuidance`
  is now derived from the guidance map so the two can't drift (drift-guard test).

### Changed

- Upgraded OkHttp 4.12.0 → 5.4.0 (adds an HTTP/2 total-header-size limit guarding
  against header-based resource exhaustion).
- Enabled `enableOnBackInvokedCallback` so Material 3 modal sheets animate on the
  predictive-back gesture (targetSdk 36).
- `RssParser` caches its date formatters per thread instead of constructing up to
  seven `SimpleDateFormat` instances per feed item.

## [1.14.6] - 2026-06-14

End-to-end engineering, accessibility, and performance audit.

### Fixed

- Smart-wake early fire no longer double-fires a one-shot alarm: the original
  exact-alarm entry, left armed when SmartAlarmService fires early, is now
  cancelled (at fire-time and defensively on dismiss). Regression-tested.

### Changed

- `TextMuted` token brightened (0xFF6A819F → 0xFF7E93AE) so muted helper/body
  copy clears WCAG AA (≥4.5:1) on card and elevated surfaces (it previously
  measured 3.77-4.11:1), fixing ~115 sites at once while staying below
  TextSecondary.
- World Clock no longer rebuilds DateTimeFormatters every second (cached, rebuilt
  only on 12/24-hour change); Timer countdown ticks at 250ms instead of 50ms
  (it only renders seconds + a progress ring), both cut steady background churn.
- Visual consistency: removed a banned 999.dp corner radius (news skeletons),
  off-scale icon sizes (13/15dp → AppIconSize.xs) in shared components, and a
  2.dp outlier (onboarding bullet).

### Accessibility

- Wordle dismiss-challenge tiles expose letter state (correct / in-word /
  not-in-word) to TalkBack and colorblind users instead of relying on tile color.
- Vacation date fields announce as buttons with a merged label:value description.

### UX

- Per-alarm History dialog shows a framed empty state instead of all-zero stats
  for an alarm that hasn't fired yet.
- Clearer shared-import button: "Save inactive" → "Save, keep off".

## [1.14.5] - 2026-06-14

Reliability regression tests (no behavior change).

### Added

- Backup-import resilience tests for `AlarmBackup.toAlarmOrNull()`: malformed
  day-of-week entries are dropped case-insensitively, out-of-range/invalid
  values are sanitized, and invalid challenge-chain entries are filtered.
- Direct Boot fallback cache tests for `DirectBootAlarmCache`: keep-earliest
  across alarms, same-id refresh, past-trigger/invalid-id rejection, clear, and
  one-shot fired-marker consume/ignore invariants.

## [1.14.4] - 2026-06-14

Wear OS test coverage.

### Changed

- Extracted the Wear tile and complication text into a shared, pure
  `WearAlarmText` helper (deduplicating the countdown formatter that was copied
  across both services) and centralized the tile action-routing constants in
  `WearAlarmData`.

### Added

- A Wear unit-test harness (`wear/src/test`) covering snapshot
  save/load/from-DataMap round-trips, tile main/secondary text, complication
  short/long text, content descriptions, countdown formatting, and tile
  action-path routing. CI now runs the `:wear:testDebugUnitTest` lane.

## [1.14.3] - 2026-06-14

Dependency refresh (the AGP-8-compatible subset).

### Changed

- Bumped AndroidX Activity (1.9.3 → 1.13.0), Kotlin Coroutines (1.9.0 → 1.11.0,
  unifying the app and `:wear` modules), and Dagger Hilt (2.53.1 → 2.56.2).
- Room (2.8) and WorkManager (2.11) bumps are deferred to the AGP 8→9 / KSP2
  migration: Room 2.8 fails KSP1 codegen and WorkManager 2.11 changes the
  `Operation` API surface; Hilt 2.59 likewise requires AGP 9.

## [1.14.2] - 2026-06-14

Physical-challenge readiness preflight on the alarm editor.

### Added

- The alarm editor now shows a readiness row for physical dismiss challenges
  (shake, squat, walk-steps, NFC, barcode, photo-match, Wi-Fi), reporting
  whether the device has the required hardware, runtime permission, and
  registered reference. Evaluation also covers chained challenges and surfaces
  the most severe problem first.

### Fixed

- Saving an alarm whose dismiss challenge is missing its required reference (NFC
  tag, barcode, reference photo, or Wi-Fi network) is now blocked with a clear
  message, instead of saving an alarm that could never be dismissed.

## [1.14.1] - 2026-06-14

Sleep insights on the Statistics tab: a duration-only composite sleep score and
a rolling 14-day sleep-debt accumulator, both measured against the user's
configured sleep goal.

### Added

- Per-night composite sleep score (0-100, duration-only v1) scaled against the
  user's nightly sleep goal, surfaced as a chip on the Sleep and wake patterns
  card and averaged across the window.
- Rolling sleep-debt accumulator: accumulated shortfall against the sleep goal
  over the 14-day window, paid down by longer nights, floored at zero and capped
  at one week. Shown as a chip on the Statistics tab.

### Fixed

- F-Droid metadata `CurrentVersionCode` drift (was 81 while the latest build was
  82); all version anchors now agree at 1.14.1 / code 83.

## [1.14.0] - 2026-06-13

Weather-conditional alarms, schedule forecast, profile quick-switch,
cancellation lock, alarm conflict detection, smart snooze suggestion,
backup format v11 (DB v15).

### Changed

- YouTube alarm-sound search now uses feedback-card status and error states,
  a surfaced searching state, a real no-results empty state with retry, and
  clearer result-row save affordances.
- The YouTube alarm-sound dialog no longer shows a disabled primary action in
  search mode; users search from the field and save from a chosen result row.
- YouTube download progress and success copy now uses calmer ASCII-safe
  wording, and the in-app What's New highlights were refreshed for the current
  premium interaction polish pass.
- Upgraded Play-flavor NewPipe Extractor from `v0.24.8` to `v0.26.3` so the
  in-app YouTube alarm-sound search stays on the current parser line with the
  latest 0.26.x YouTube integrity handling.
- Documented why the yt-dlp `--netrc-cmd` CVE path is not reachable from ACX's
  fixed downloader option allow-list and tightened YouTube URL validation
  against whitespace option suffixes.
- Added a manual Play-flavor yt-dlp engine update action to the YouTube alarm
  sound dialog; failed updates keep the installed engine and no update network
  call runs unless the user taps Update.
- AlarmService now activates a platform `MediaSession` for the foreground alarm
  lifecycle, including silent and Spotify-delegated alarm paths, and releases it
  on snooze, dismiss, auto-silence, or service teardown.
- Backup sound escalation now captures the alarm stream volume before boosting
  to max and restores it when alarm playback stops.
- Android CI now runs unsigned Play, F-Droid, and Wear release builds through
  R8 and asserts release APK size budgets.
- Android CI now runs `:app:lintPlayDebug` and `:wear:lintDebug`.
- Added Robolectric/MockK/coroutines test infrastructure plus focused
  AlarmScheduler and BackupManager unit tests covering schedule/cancel/
  reschedule and backup export/import behavior.
- App and Wear modules now target SDK 36; verified Play debug on an API 36.1
  emulator for launch, alarm-editor navigation, back behavior, and crash-free
  edge-to-edge rendering.
- Timers now persist running, paused, and finished state outside the ViewModel,
  schedule AlarmManager-backed expiry callbacks, restore accurate remaining time
  after process death, and show a calm cancellation notice for running timers
  after reboot.
- Bedtime sleep sounds now use continuous procedural `AudioTrack` soundscapes
  for white noise, rain, brown noise, ocean, and fan, replacing the broken
  placeholder WAV assets while preserving fade-out controls.
- Guardian Angel direct emergency SMS is now F-Droid-only; Play builds omit
  `SEND_SMS`, open a prefilled emergency SMS composer instead, and document the
  flavor difference in Settings and README copy.
- Legacy Android Auto Backup on API 26-30 now uses an explicit
  `fullBackupContent` allow-list that keeps only DataStore preferences and
  excludes Room history, crash logs, support bundles, downloaded media, and
  challenge reference files.
- Release shrinking now explicitly keeps the Direct Boot fallback alarm package,
  protecting the pre-unlock receiver, service, cache, and snapshot classes from
  accidental R8 removal or renaming.
- New encrypted backup exports now use 600,000 PBKDF2-HMAC-SHA256 iterations
  while preserving decryption compatibility with existing 210,000-iteration
  backup envelopes.
- First-run onboarding now turns the final setup step into an alarm-readiness
  checklist with real exact-alarm, notification, full-screen alarm, and battery
  protection status plus direct review actions.
- First-run onboarding now includes a non-destructive test alarm that schedules
  a 10-second exact alarm, opens a dedicated dismissal screen, plays/vibrates,
  and marks the checklist complete without creating or changing saved alarms.
- Settings > Wake readiness can reopen the first-run setup checklist for later
  permission review and test-alarm reruns.
- Webhook integrations now use an HTTPS-only v1 payload contract with typed
  alarm event names, event IDs, scheduled-time metadata, optional label
  redaction, missed/skipped event coverage, and clearer Settings validation
  copy.
- Shared `acx://` alarm links now open a pre-save review screen instead of
  writing directly to Room; imported alarms stay disabled and can strip contact,
  media, Wi-Fi, location, and reference-backed challenge fields before saving.
- Release builds now run a signing-hygiene preflight before CI writes
  `keystore.properties`, failing if keystore material is tracked, staged, or
  missing expected ignore coverage.
- Settings can now hide alarm labels on public surfaces, replacing labels with
  neutral alarm text on alarm notifications, wake-confirm prompts, widgets,
  quick settings, and Play-flavor Wear next-alarm snapshots while preserving
  labels inside the unlocked app.
- Settings now surfaces active Guardian Angel readiness in Wake readiness,
  including SMS/composer/call fallback status, direct permission recovery
  actions, and the same redacted state in support bundles.
- Shared chips and surfaces now use sharper 8dp geometry, 48dp tappable chip
  targets for interactive filters/status chips, unified Settings result cards,
  adaptive first-run alarm actions, and clearer News/World Clock empty/error
  state actions.
- Shared alarm import review now uses private-reference status chips, a
  full-row sanitize toggle, feedback-card error state, saving progress, clearer
  inactive-save copy, human-readable challenge labels, and silent-sound
  labeling.
- Settings accent swatches now render as 48dp radio-style controls with
  selected state and contrast-aware checkmarks, while utility shortcuts use the
  same surfaced row treatment as other Settings actions.
- Night clock now drifts content slowly across the screen (±24dp X, ±18dp Y
  on offset 120s/90s cycles) to prevent OLED burn-in during overnight use.
- Home widget now respects the system 24-hour time preference instead of
  hardcoding 12-hour format.
- Alarm edit now warns when NFC, barcode, Wi-Fi, or photo-match challenges
  are selected without the required reference data, and surfaces the Wi-Fi SSID
  field directly in the challenge section instead of only in Advanced.
- Assessed yt-dlp GHSA-69qj-pvh9-c5wg (`--exec` command injection): ACX is
  not affected because it only uses `--get-url` with a fixed option allow-list.
- Assessed Android 17 background-audio exemption: all AlarmService audio paths
  use USAGE_ALARM exclusively and are exempt; no code changes needed for the
  future targetSdk 37 bump.

- The alarm firing screen now shows a weather chip (temperature and
  conditions) when cached weather data is available, using the Today tab's
  last fetch with no network call at fire time.
- Added Dependabot for weekly Gradle and GitHub Actions dependency update PRs,
  with Compose, AndroidX, and Kotlin grouped to reduce noise.
- Cloud backup now excludes DataStore preferences (which contain webhook URLs,
  Hue API keys, guardian contacts, and custom feed URLs) while keeping the Room
  alarm database. Device-transfer still includes DataStore for on-device
  migration. This prevents sensitive integration secrets from traversing
  Google's cloud backup pipeline.
- Added RestoreAlarmAgent so alarms re-arm after Android Auto Backup or
  device-transfer restore. Previously, restored alarms existed in Room but had
  no AlarmManager registrations until the next reboot.
- Release workflow now writes per-APK certificate DN and SHA-256/SHA-1
  fingerprints to APK-CERT-FINGERPRINTS.txt and uploads it alongside APKs to
  GitHub Releases. README documents the apksigner verification command.
- Added launcher shortcuts for "New alarm", "Start timer", and "Bedtime"
  accessible by long-pressing the app icon. Deep links use `acx://navigate/`
  URIs handled by AppNavigation.

- Alarm profiles are now filterable on the alarm list via a chip row,
  mirroring the existing group filter. Profiles like Work, Travel, Weekend
  can be toggled to show only matching alarms.
- Alarm edit now shows the next 7 fire dates as a schedule forecast preview,
  factoring in repeat days, specific dates, solar anchors, holidays, and
  vacation mode. Vacation-skipped dates are flagged.
- Per-alarm weather-early fire: optionally shift the alarm 10-30 minutes
  earlier when Open-Meteo forecasts snow, freezing rain, or ice conditions.
  Uses cached weather data from the Today tab; no network call at schedule
  time. DB v14 to v15 adds the `weatherEarlyMinutes` column.
- Cancellation lock: Settings can prevent disabling alarms within 15/30/60
  minutes of fire time, protecting heavy sleepers from toggling alarms off
  while half-asleep. The lock shows a feedback message and can be overridden.

- Wake confirmation now shows a visible 60-second countdown timer with a
  CircularProgressIndicator that transitions green→red at 10 seconds remaining.
  Re-fires are capped at 3 attempts with the count threaded through the full
  WakeConfirmWorker→AlarmService→WakeConfirmWorker chain. The prompt screen
  shows remaining re-fires and dynamic status copy.
- Philips Hue sunrise simulation now uses TOFU (Trust On First Use) certificate
  pinning for v2 HTTPS bridge connections. The bridge cert SHA-256 fingerprint
  is saved to DataStore on first connect and validated on subsequent runs.
  Legacy v1 plain HTTP is behind an explicit opt-in toggle (default off).
- Support diagnostics now include wake-confirm and Hue integration fields in
  the readiness JSON export.
- Backup now includes `hueBridgeCertFingerprint` and `hueLegacyHttpEnabled`
  settings, fixing a drift-guard test failure where these fields silently reset
  to defaults on restore.

### Fixed

- Wake confirmation now fires for one-shot alarms. Previously, the worker
  checked `alarm.isEnabled` after `handleAlarmFired()` auto-disabled one-shot
  alarms, silently skipping the prompt for the exact use case where heavy
  sleepers need it most.
- Fixed `SupportDiagnosticsFormatterTest` referencing non-existent
  `GuardianSmsPath.AUTO_SEND` enum value (corrected to `DIRECT_SMS`).
- Challenge chains containing NFC, barcode, photo-match, or Wi-Fi challenges
  now warn when reference data is missing, preventing silent challenge skips
  at fire time.
- README Wake Experience table now correctly describes swipe-left-to-dismiss
  and swipe-right-to-snooze, matching the v1.6.2 firing-screen behavior.

## [1.13.14] - 2026-06-11

Fire-scoped alarm incident UI and wake-confirm instrumentation.

### Added

- Backup format v9: `upcomingAlarmMinutes`, `showNoAlarmsWarning`,
  `autoSilenceMinutes`, manual weather location (`locationName`,
  `useManualLocation`, `lastKnownLatitude`/`Longitude`), and the five
  tab/radar visibility toggles now round-trip through export/import. They
  previously reset to defaults on every restore. v1-v8 backups still import
  (missing fields take defaults); readable exports now disclose "Saved
  weather location" alongside the other private-data warnings.
- Reflection-based drift-guard test (`BackupManagerSettingsDriftTest`) fails
  the build if a future `AppSettings` field is added without a
  `SettingsBackup` counterpart or an explicit exemption.
- Alarm firing screen launches now carry the incident fire ID and scheduled time
  through snooze and dismiss service requests.
- Alarm firing UI records privacy-safe activity-open, user snooze/dismiss
  request, and activity-finished diagnostic outcomes.
- Wake confirmation worker/activity now record prompt requested/posted,
  confirmed, keep-checking, timeout, disabled/missing-row skip, and re-fire
  request/failure incident codes.

### Changed

- Wake-confirm re-fire attempts preserve the original incident fire ID in the
  follow-up AlarmService start request.
- Wake-confirm notification IDs moved from the 5000 base to 500000 so a
  long-lived install (alarm ids past 2000) can't collide with the
  timer-finished notification range (7000 + timer id).
- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.13.14"`,
  `versionCode = 79`.

### Fixed

- Incident diagnostic records issued right before an activity `finish()` or
  service `stopSelf()` no longer race their component scope's cancellation
  and silently drop: `AlarmIncidentRepository` now owns an application-lived
  fire-and-forget `recordAsync()` scope used by the firing activity,
  wake-confirm activity, AlarmService, and AlarmScheduler.
- Accessibility: the alarm-search clear button, support-export dismiss
  button, and backup-result dismiss button now announce themselves to screen
  readers; actigraphy session rows announce "Fired early" / "Reached target"
  instead of conveying the outcome by icon tint alone.
- Typing-speed challenge: words typed beyond the target phrase no longer
  inflate the WPM score (padding the input with junk words could defeat the
  speed gate); extra words now count as errors and WPM only credits words up
  to the phrase length.

## [1.13.13] - 2026-06-11

Settings-facing alarm diagnostics and incident retention controls.

### Added

- Added a Settings alarm-diagnostics card that summarizes the latest redacted
  incident event and distinguishes degraded events from normal delivery events.
- Added a clear-diagnostics confirmation that deletes only incident timeline
  rows; alarm statistics, alarms, backups, and crash logs remain separate.
- Added repository coverage for incident retention pruning, latest-row trimming,
  clear-history behavior, and failure-isolated recording.

### Changed

- Support bundle copy now explicitly mentions wake, incident, and alarm
  diagnostics.
- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.13.13"`,
  `versionCode = 78`.

## [1.13.12] - 2026-06-11

Alarm incident timeline foundation and DB v14 diagnostic-event pass.

### Added

- Added DB v14 `alarm_incident_events` storage with migration/schema coverage
  for bounded, redacted alarm-fire diagnostics.
- Added best-effort incident recording across scheduler, receivers, foreground
  promotion, notifications, activity launch, audio/fallback paths, snooze,
  dismiss, auto-silence, and wake-confirm scheduling.
- Added local support diagnostics incident summaries and a redacted
  `incident_timeline.csv` export with whitelist-only event fields.
- Added unit coverage that incident exports strip labels, URLs, and
  free-form secret-like reason text.

### Changed

- README and privacy policy now describe recent incident event codes in the
  user-initiated local support bundle.
- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.13.12"`,
  `versionCode = 77`.

## [1.13.11] - 2026-06-11

Smart Wake v2 scoring and DB v13 decision-evidence pass.

### Added

- Added a pure `SmartWakeDecisionEngine` that evaluates scored one-minute
  actigraphy epochs before allowing conservative early fire.
- Added DB v13 smart-wake decision evidence on `actigraphy_sessions`, including
  decision reason, observed minutes before decision, and decision mode.
- Added smart-wake decision readouts to Statistics and aggregate smart-wake
  fields to local support diagnostics.
- Added migration/schema coverage for the v12->v13 actigraphy decision fields.

### Changed

- Smart-alarm monitoring now records target, timeout, insufficient-data, active,
  still-motion, unstable-light, and light-motion early-fire outcomes.
- Smart-wake windows now align on a 60-minute product cap with a bounded
  service wake lock and Android 15+ foreground-service timeout handling.
- README and privacy policy now describe aggregate support-bundle smart-wake
  metadata without exposing per-minute actigraphy buckets.
- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.13.11"`,
  `versionCode = 76`.

## [1.13.10] - 2026-06-11

Actigraphy validation and smart-wake guard pass.

### Added

- Added classifier boundary and mixed-motion unit coverage for phone-motion
  bucket behavior.
- Added a pure smart-wake observation gate requiring at least 8 minutes and
  one-third of the configured smart window before early firing can be considered.
- Added repository-level coverage that actigraphy recording prunes sessions
  outside the 30-day retention window.

### Changed

- Statistics now labels local actigraphy output as phone-motion buckets with
  awake-motion, light-motion, and still-motion labels instead of stage-like
  wording.
- Support diagnostics now explicitly state that local actigraphy motion buckets
  are omitted from the ZIP.
- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.13.10"`,
  `versionCode = 75`.

## [1.13.9] - 2026-06-11

Android 14+ full-screen alarm readiness pass.

### Added

- Settings Wake readiness now checks Android 14+ full-screen alarm access with a
  platform settings action and an app-details fallback.
- Local support diagnostics now export full-screen alarm access as
  `allowed`, `blocked`, `unknown`, or `not_applicable`.
- Added JVM coverage for the support diagnostics full-screen alarm access field.

### Changed

- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.13.9"`,
  `versionCode = 74`.
- README Data & Reliability now documents Wake readiness and the new
  wake-readiness content in the local support bundle.

## [1.13.8] - 2026-06-11

Release/schema gate pass for the actigraphy DB v12 work.

### Added

- Exported the Room v12 schema with the `actigraphy_sessions` table for compact
  smart-alarm motion-bucket summaries.
- Added an 11->12 Room migration test that validates the actigraphy table and
  updated the latest-schema test constant to 12.
- Added deterministic unit coverage for the actigraphy classifier's empty,
  still-motion, active-motion, scaling, and scored-summary paths.
- Added public README and privacy-policy disclosure for local 30-day actigraphy
  buckets, including the no-raw-accelerometer and non-medical-stage caveats.

### Changed

- Bumped app, Wear, README, and F-Droid metadata to `versionName = "1.13.8"`,
  `versionCode = 73`.
- Version lint now checks tracked Gradle, README, and F-Droid metadata version
  names and version codes instead of ignored local-only roadmap/changelog files.

### Fixed

- Fixed the actigraphy classifier's erased JVM signature clash by renaming the
  scored-epoch summary helper to `summarizeScored(...)`.
- Hardened Wear release shrinking with R8 keep rules for tile, complication,
  Wearable Data Layer, ProtoLayout, and coroutine callback classes.
- `NextAlarmTileService` now cancels its service coroutine job in `onDestroy()`
  so repeated tile bind/unbind cycles do not leave work running.

## [1.13.7] - 2026-05-17

Sleep/wake analytics charts pass (roadmap X6). **DB v11.**

### Added

- Statistics now includes local sleep/wake pattern charts that compare recent
  Health Connect sleep duration with alarm dismiss response time, snoozes, and
  challenge retries.
- Health Connect sleep summaries now retain recent session windows in memory so
  Statistics can pair sleep ending dates with wake behavior without writing
  Health Connect records to Room, DataStore, backups, or support exports.
- Alarm history now persists `challengeRetryCount` for dismissed alarms, using
  the firing screen's wrong-attempt counter.
- Added pure unit coverage for sleep/wake analytics pairing, wake-only
  fallback behavior, and challenge retry aggregation.

### Changed

- Room database bumped to **v11** with `MIGRATION_10_11` adding
  `alarm_events.challengeRetryCount INTEGER NOT NULL DEFAULT 0`.
- The firing screen now passes challenge solve duration and retry count to
  `AlarmService` on user dismiss, so future Statistics views can use both.
- Bumped to `versionName = "1.13.7"`, `versionCode = 72`. README badge,
  install command, roadmap snapshot, Wear module, and F-Droid metadata synced.

## [1.13.6] - 2026-05-17

Local support export pass (roadmap X5).

### Added

- Settings now includes an "Export support bundle" action that creates a
  shareable local ZIP through a FileProvider. The app does not upload it.
- Support bundles include `diagnostics.txt`, `alarms_redacted.csv`, and up to
  10 newest local crash logs when present.
- Diagnostics include app version/build, device/Android version, wake-readiness
  checks, alarm counts, next trigger, and aggregate alarm-history stats.
- Redacted alarm diagnostics omit alarm labels, custom media/content URIs,
  internet-radio URLs, Spotify URIs, Hue/webhook secrets, Wi-Fi/location/contact
  values, challenge reference values, and Health Connect records.
- Added unit coverage for the support diagnostic redaction policy.

### Changed

- `CrashLogger` now exposes newest-first crash log files for local support
  packaging while preserving the existing Settings/debug log read API.
- Bumped to `versionName = "1.13.6"`, `versionCode = 71`. README badge,
  install command, roadmap snapshot, Wear module, and F-Droid metadata synced.

## [1.13.5] - 2026-05-17

Direct Boot minimum-alarm prototype (roadmap X4).

### Added

- Added a device-encrypted next-alarm snapshot for the minimum pre-unlock
  fields: alarm id, trigger time, display time, default-sound flag, and
  vibration flag. Alarm labels, custom ringtone/content URIs, integration URLs,
  challenge data, and settings remain credential-encrypted.
- Added a Direct-Boot-aware boot receiver path that handles
  `LOCKED_BOOT_COMPLETED` without starting Room, DataStore, WorkManager, or
  Hilt worker scheduling before first unlock.
- Added a Direct-Boot-aware fallback receiver and foreground service that can
  ring the next cached alarm with the system default alarm tone and vibration
  before first unlock after reboot.
- Added post-unlock app startup gating so normal crash logging, DataStore,
  WorkManager, Wear bridge, and downloader initialization start only after the
  user credential storage is available.
- Added `docs/DIRECT_BOOT_MINIMUM_ALARM.md` and unit coverage for the
  non-secret Direct Boot snapshot policy.

### Changed

- Normal post-unlock rescheduling now rebuilds the Direct Boot snapshot and
  cancels any stale fallback PendingIntent to avoid duplicate alarm fires.
- Direct-Boot-fired one-shot alarms are consumed during the first post-unlock
  reschedule so they do not roll forward and ring again.
- Bumped to `versionName = "1.13.5"`, `versionCode = 70`. README badge,
  install command, roadmap snapshot, Wear module, and F-Droid metadata synced.

## [1.13.4] - 2026-05-17

Wear next-alarm complication pass (roadmap X3).

### Added

- Wear module now exposes a `NextAlarmComplicationDataSourceService` for
  compatible watch-face complication slots. It supports `SHORT_TEXT` and
  `LONG_TEXT`, uses the same cached next-alarm snapshot as the Wear tile, and
  provides preview data for watch-face pickers.
- Wear Data Layer updates now request both tile and complication refreshes when
  the phone publishes a new next-alarm snapshot.
- Added AndroidX Wear Watchface complication data-source dependency
  `androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0`.

### Changed

- Bumped to `versionName = "1.13.4"`, `versionCode = 69`. README badge,
  install command, roadmap snapshot, Wear module, and F-Droid metadata synced.

## [1.13.3] - 2026-05-17

Backup-export trust pass (roadmap X2).

### Added

- Settings now warns before plain or encrypted backup export when the current
  backup would include configured webhook URLs, Philips Hue bridge details/API
  keys, custom news feed URLs, internet-radio stream URLs, device-local
  ringtone/photo URIs, Wi-Fi/location/contact details, or NFC/barcode challenge
  values.
- `BackupManager.assessExportWarning(...)` provides a unit-tested risk scan for
  backup export disclosures.

### Changed

- Backup format bumped to **v8**. `SettingsBackup.newsFeedUrl` now round-trips
  the selected News feed URL with a default value for older backups.
- Bumped to `versionName = "1.13.3"`, `versionCode = 68`. README badge,
  install command, roadmap snapshot, Wear module, and F-Droid metadata synced.

## [1.13.2] - 2026-05-17

Trust, release, dependency, and Health Connect integration pass (roadmap
R1-R6 + X1).

### Added

- Play-flavor Health Connect sleep-session integration. The app declares and
  requests only `android.permission.health.READ_SLEEP`, reads recent
  `SleepSessionRecord` windows in foreground UI, and shows local-only
  duration/stage summaries in Bedtime and Statistics.
- Flavor-safe `HealthConnectSleepRepository` abstraction with a real Play
  implementation and F-Droid no-op implementation. The F-Droid flavor ships no
  Health Connect SDK and no Health Connect permission.
- Android CI dependency-audit job backed by `scripts/osv_gradle_audit.py`,
  which resolves Gradle runtime classpaths and queries OSV.
- Room migration/schema CI gate and instrumentation tests for exported-schema
  parity, v4-to-current migration, v9-to-v10 defaults, and migration
  contiguity.

### Changed

- Privacy policy, README, F-Droid metadata, Settings copy, and roadmap context
  now describe the actual optional data surfaces, including the Play-only
  Health Connect `READ_SLEEP` path and local-only sleep summaries.
- Release workflow now builds signed Play, F-Droid, and Wear release APKs on
  tags, verifies signatures and badging, generates `SHA256SUMS.txt`, and
  uploads release artifacts.
- Play-only downloader/runtime dependencies are constrained away from OSV-known
  vulnerable transitives: Jackson 2.18.6, Commons Compress 1.28.0, Commons IO
  2.20.0, Rhino 1.8.1, Guava 33.6.0-android, and XZ 1.10 support for release
  shrinking.
- Tracked `PROJECT_CONTEXT.md` is the canonical project context.

### Internal

- Bumped to `versionName = "1.13.2"`, `versionCode = 67`. README badge,
  install command, roadmap snapshot, Wear module, and F-Droid metadata synced.
- Play Console Health Connect health-permissions declaration/approval remains
  an external release gate before distributing this Play build through Play.

## [1.13.1] - 2026-05-16

Health Connect opt-in scaffold + Play health-permissions narrative
(roadmap N12 + N13). DataStore-only; the data-read path itself lands
in a follow-up release after the Play Console health-permissions
declaration is approved.

### Added: Health Connect opt-in toggle

- `AppSettings.healthConnectEnabled` (default false) survives DataStore
  reads/writes and round-trips through the JSON + AES-256 backup paths
  alongside every other setting.
- New Settings → Health Connect card (sits between Philips Hue and
  Personalization). Current copy is intentionally conservative: v1.13.1
  stores only a local opt-in preference and does not request Health Connect
  permission or read sleep data yet.

### Documentation: PRIVACY_POLICY.html

- The privacy-policy reconciliation on 2026-05-17 clarifies that the
  v1.13.1 scaffold does not ship the Health Connect SDK, request health
  permissions, or read `SleepSessionRecord` data. It also documents the
  intended future Play-flavor use case so the follow-up release can go
  through Play health-permissions review before data access ships.

### Internal

- Bumped to `versionName = "1.13.1"`, `versionCode = 66`. README badge,
  install command, and Wear module version synced.

## [1.13.0] - 2026-05-16

Adaptive primary navigation: NavigationBar on phones, NavigationRail on
8" tablets / foldables / Chromebook (roadmap N11). No schema changes;
phone UX is byte-for-byte identical.

### Added: wider-window navigation rail

- `AppNavigation` now reads the current `WindowWidthSizeClass`
  ([Compose stable adaptive APIs](https://android-developers.googleblog.com/2024/09/jetpack-compose-apis-for-building-adaptive-layouts-material-guidance-now-stable.html))
  and renders a `NavigationRail` on the leading edge whenever width is
  `MEDIUM` or `EXPANDED`. The persistent bottom `NavigationBar` only
  renders on `COMPACT` widths.
- `MEDIUM` (~600-839 dp) covers small tablets and most foldables in
  partial-fold posture; `EXPANDED` (~840 dp+) covers 8"+ tablets,
  unfolded foldables, Chromebooks, and Samsung DeX.
- The rail re-uses the visible-tabs filter (Today / Timer / World /
  News can still be hidden by the user) and the same navigation
  callback (single-top, restore-state, pop-to-home behaviour). Tab
  colours and accent treatment match the bar exactly.

### Internal

- New dep `androidx.compose.material3:material3-window-size-class`
  (BOM-managed; no version pin). Adds a single small JAR, the
  underlying WindowSizeClass calculation is stateless math.
- Extracted the per-route `NavHost` definition into a private
  `AppNavHost(...)` composable so the rail branch and bar branch
  share the same navigation graph. ~100 LoC deduplicated.
- Bumped to `versionName = "1.13.0"`, `versionCode = 65`. CI version-
  line lint (N10) enforces consistency across all six touchpoints.

## [1.12.3] - 2026-05-16

CI version-line consistency lint (roadmap N10). No app behavior change.

### Added: `.github/workflows/version-lint.yml`

- New workflow `Version line consistency` runs on every push to `main`
  and on every pull request. Asserts that the version string in all
  six release-touching files agrees with `app/build.gradle.kts`:
  `wear/build.gradle.kts`, the README badge, the README `adb install …`
  snippet, the CHANGELOG top `## [x.y.z]` header, and the ROADMAP
  `## Current snapshot (vX.Y.Z)` header.
- Failure messages identify each drifted file by name and remind the
  contributor of the cross-cutting docs track in ROADMAP.md.
- Pure regex/grep, no Java/Gradle bootstrap needed, so the job runs
  in seconds and never costs build minutes.

### Internal

- Bumped to `versionName = "1.12.3"`, `versionCode = 64`. README badge,
  install command, and Wear module version synced (and now CI-enforced).

## [1.12.2] - 2026-05-16

RingtonePool chip-based editor (roadmap N9). No schema changes; storage
format unchanged.

### Changed: alarm-edit ringtone pool UX

- Replaced the newline-separated `OutlinedTextField` with a horizontally
  scrollable chip row in the Advanced section. Each pool URI renders as
  an `AppFilterChip`; tapping a chip removes that entry. An "Add" chip
  opens a lightweight `AlertDialog` for pasting a new `content://` or
  `file://` URI, with duplicate-protection inline.
- New `ringtoneShortName(uri)` helper picks the trailing path segment
  (e.g. `audio/12345`, `sun.mp3`) for chip labels and truncates to 28
  chars with an ellipsis. We avoid a per-render `ContentResolver`
  lookup to keep alarm-edit scrolling cheap.
- On-disk format stays the comma-separated string already consumed by
  `Alarm.sanitized()` and `AlarmService.startAudio()`, so no migration
  or backup compatibility work needed.

### Internal

- Bumped to `versionName = "1.12.2"`, `versionCode = 63`. README badge,
  install command, and Wear module version synced.

## [1.12.1] - 2026-05-16

Missed-timer notification (roadmap N8). No schema changes; no new
permissions (`POST_NOTIFICATIONS` was already required).

### Added: timer-finished surface

- New `timer_finished_channel` (`CHANNEL_TIMER`) registered alongside
  the alarm channels. `IMPORTANCE_HIGH` so it heads-up the way the
  missed-alarm channel does; channel-level sound and vibration are
  disabled because the timer's own MediaPlayer + vibrator handle the
  foreground experience, this channel is the "user closed the app"
  surface.
- `TimerViewModel` posts a notification when a countdown transitions
  to `FINISHED`. Tapping the notification opens MainActivity at the
  Timer tab; `stop()` and `dismissFinished()` cancel the notification
  alongside the audio.
- One notification per finished timer (id = `TIMER_NOTIFICATION_BASE_ID +
  timer.id`, base 7000) so simultaneous expiries each get their own
  row instead of overwriting each other.
- Notifications intentionally survive `onCleared()` (that's the whole
  point of the feature) and the channel registration runs from
  `AlarmService.createNotificationChannels()` on every process start.

### Internal

- Bumped to `versionName = "1.12.1"`, `versionCode = 62`. README badge,
  install command, and Wear module version synced.

## [1.12.0] - 2026-05-16

Per-alarm vibration start-delay (roadmap N7). **DB v10, backup format v7.**
Schema change is forward-compatible, new field defaults to 0, preserving
prior behaviour.

### Added: gentle-wake building block

- `Alarm.vibrationDelaySeconds: Int = 0` defers haptic onset for the
  configured number of seconds after the alarm fires. Pairs with
  `gradualVolumeSeconds` to build an "audio first, vibration after the
  fade" preset without writing a new alarm profile abstraction.
- Alarm-edit → Vibration section gains a "Start vibration after" picker
  (Immediately / 10s / 30s / 1m / 2m / 5m / 10m). Hidden when
  vibration is disabled.
- `AlarmService` schedules the vibration via a cancellable
  `serviceScope.launch { delay() … startVibration() }`. The launched
  job re-checks `currentAlarmId` before vibrating so a service-restart
  race can't fire haptics for an alarm the user has already dismissed
  or snoozed.

### Changed: schema

- Room database bumped to **v10**. New column
  `vibrationDelaySeconds INTEGER NOT NULL DEFAULT 0` on the `alarms`
  table; new `MIGRATION_9_10` registered in `DatabaseModule`.
- Backup format bumped to **v7**. `AlarmBackup.vibrationDelaySeconds`
  added with default 0 (back-compat read); `BackupData.version`
  default and `MAX_SUPPORTED_BACKUP_VERSION` both moved from 6 → 7.
  v6 backups continue to import correctly, missing field defaults to 0.

### Internal

- `Alarm.sanitized()` coerces `vibrationDelaySeconds` into 0..600
  (10 min cap matches `gradualVolumeSeconds`'s 0..300, leaving room
  to stretch the haptic offset slightly past audio peak).
- Bumped to `versionName = "1.12.0"`, `versionCode = 61`. README badge,
  install command, and Wear module version synced.

## [1.11.6] - 2026-05-16

"Pause alarms" single-tap suspend (roadmap N6). DataStore-only, no DB
schema change. No new permissions.

### Added: Settings → Pause alarms

- New section above Vacation mode lets the user suspend **all** alarms
  (including one-shots, vacation only touched repeating alarms) with a
  single tap. Quick chips for 1 day / 3 days / 7 days / 14 days; the
  active card shows the resume date and a "Resume now" chip to clear
  early. The expiry timestamp lands at end-of-day on the chosen day so
  the next morning's alarm is included in the pause window.
- `AppSettings.pauseUntilMillis` (default 0) controls the state. The
  helper `AppSettings.isPaused(now)` returns true while
  `pauseUntilMillis > now`; stale values lazy-expire (no scheduled
  wake-up needed to clear them).
- `AlarmScheduler.schedule()` and `AlarmScheduler.scheduleAt()` short-
  circuit when paused: cancel any prior PendingIntent and zero the
  alarm's `nextTriggerTime` so widgets, the persistent next-alarm
  notification, and the Wear tile all reflect the suspended state.
  Once the pause expires, the next reschedule pass re-arms everything.
- Snooze + quick-alarm paths honour the pause too, so an alarm fired
  before the pause was set can't sneak past it via the snooze button.

### Changed: backup format

- `SettingsBackup` schema gains `pauseUntilMillis` (default 0,
  backward-compatible read). Round-trips through both the plain JSON
  and the AES-256 encrypted backup paths.

### Internal

- Bumped to `versionName = "1.11.6"`, `versionCode = 60`. README badge,
  install command, and Wear module version synced.

## [1.11.5] - 2026-05-16

Philips Hue API v2 migration (roadmap N5). v1 fallback retained for
~6 months while users update bridge firmware past 1.40. No schema or
permission changes.

### Added: Hue CLIP v2

- `HueSunriseWorker` now probes the bridge for v2 support
  (`GET https://{ip}/clip/v2/resource/light/{rid}` with the
  `hue-application-key` header) on first run and remembers the verdict
  in a `hue_api_capability` SharedPrefs entry keyed on the bridge IP.
  Subsequent runs skip the probe.
- v2 PUTs use the CLIP v2 endpoint
  (`PUT https://{ip}/clip/v2/resource/light/{rid}`), the
  `hue-application-key` header (replacing the v1 username-in-path), and
  the v2 body shape (`{"on":{"on":true},"dimming":{"brightness":0..100},"color_temperature":{"mirek":153..500}}`).
  v1's 0-254 brightness scale is converted to v2's 0-100 percent.
- Settings → Integrations → Hue test now probes v2 first and reports
  the active API version ("Hue bridge reachable (API v2)" or
  "(API v1, bridge firmware is below 1.40)"). The pre-existing v1
  message ("Hue bridge not found, check IP and key") is unchanged
  for unreachable bridges.

### Security notes

- The bridge presents a self-signed cert whose subject CN is the
  bridge ID (a MAC-derived hash), so strict hostname verification
  would reject every connection. The v2 OkHttpClient pairs an
  allow-all `HostnameVerifier` with a permissive `TrustManager`.
  Threat model: traffic stays on the user's LAN; an attacker already
  on that LAN can intercept brightness commands, a trivial
  information disclosure with no escalation path. The risk is the
  same as v1's plain-HTTP path today. A future hardening pass can
  bundle the Signify root CA and pin the bridge ID (left as a
  follow-up, does not block N5).

### Internal

- v1 (deprecated) code path is preserved verbatim and remains the
  default for bridges that fail the v2 probe.
- Bumped to `versionName = "1.11.5"`, `versionCode = 59`. README badge,
  install command, and Wear module version synced.

## [1.11.4] - 2026-05-16

Wake-lock budget compliance audit (roadmap N4). No schema or behavior
changes; documentation + source comments only.

### Documentation: Play wake-lock policy March 2026

- Inline source comments at both `PowerManager.newWakeLock(...)` /
  `acquire(...)` sites (`AlarmService`, `SmartAlarmService`) now document
  whether the wake lock is exempt under the [Play Store March-2026
  wake-lock quality treatment](https://9to5google.com/2026/03/05/google-starts-calling-out-android-apps-that-drain-your-battery-before-you-download-them/).
- `AlarmService`'s 30-minute `PARTIAL_WAKE_LOCK` is **exempt**: it
  wraps a `mediaPlayback` foreground service playing
  `AudioAttributes.USAGE_ALARM` content; both the FGS type and the
  alarm-audio activity are documented exempt categories.
- `SmartAlarmService`'s 90-minute `PARTIAL_WAKE_LOCK` is **non-exempt**
  (`dataSync` FGS, accelerometer-only). Worst-case for a single
  smart-wake alarm = 90 min/day, under the 2 h non-exempt cap; users
  with multiple smart-wake alarms per day could cumulatively exceed,
  if field data shows that pattern, lower the per-window cap or track
  cumulative held time and break monitoring early.
- `SonarSleepService` holds no wake lock; it stays alive via the
  `microphone` FGS.
- Audit results recorded as a "Wake-Lock Budget" table.

### Internal

- Bumped to `versionName = "1.11.4"`, `versionCode = 58`. README badge,
  install command, and Wear module version synced.

## [1.11.3] - 2026-05-16

App Standby bucket surfacing in Settings → Reliability (roadmap N3). No
schema changes; no new permissions.

### Added: App Standby bucket row

- Settings → Reliability → Wake readiness gains a 4th row that reads the
  app's current `UsageStatsManager.getAppStandbyBucket()` value (API 28+)
  and renders the bucket along with a plain-English description of how
  Android is throttling the app. `ACTIVE` and `WORKING_SET` show as
  ready; `FREQUENT`, `RARE`, and `RESTRICTED` show as warnings with an
  action that opens battery settings (the system path that re-promotes
  the app back to `WORKING_SET`).
- The row is hidden entirely on pre-API-28 devices (`AppStandbyBucket.UNKNOWN`)
  and the "X of N ready" pill recomputes accordingly so the count never
  shows "3 of 3" when the system also has a 4th degraded bucket signal.
- The top-level Reliability tile's supporting line ("Review …") now
  includes "standby bucket" when the bucket is degraded, so the user
  sees the throttling state without expanding the section.

### Internal

- No new permission required for the self-query: the system returns the
  calling app's own bucket without `PACKAGE_USAGE_STATS`. Failure paths
  (no `USAGE_STATS_SERVICE` on stripped AOSP, `SecurityException` from
  managed profiles) are swallowed via `runCatching`, the row simply
  hides.
- Bumped to `versionName = "1.11.3"`, `versionCode = 57`. README badge,
  install command, and Wear module version synced.

## [1.11.2] - 2026-05-16

Telephony-aware alarm muting (roadmap N2). No schema changes; no new
permissions.

### Added: call-state observer

- `AlarmService` registers a `TelephonyCallback.CallStateListener` on
  Android 12+ (or the deprecated `PhoneStateListener` on pre-31) for the
  duration of alarm playback. When the system reports `CALL_STATE_OFFHOOK`
  or `CALL_STATE_RINGING` the alarm's MediaPlayer is muted to volume 0;
  on `CALL_STATE_IDLE` it is restored. The listener registers on
  `startAudio()` and unregisters in `stopAlarmPlayback()` /
  `onDestroy()`, so we never observe call state when no alarm is firing.
- Mute is implemented with `MediaPlayer.setVolume(0f, 0f)` (per-player
  attenuation) rather than touching `STREAM_ALARM`, that keeps the
  gradual-volume coroutine, the backup-sound escalation, and the user's
  alarm volume preference intact for after the call ends.
- Vibration, the firing activity, the persistent notification, the
  flashlight strobe, and Guardian Angel scheduling are intentionally
  left running during a call. Tactile and visual wake cues don't
  interrupt the user's call.

### Changed: playback paths honour call state

- Default ringtone path, internet-radio path, and fallback-default path
  all check `callMutedAudio` after `prepare()` / `start()` so an alarm
  fired while a call is already in progress starts silent.
- The gradual-volume fade-in coroutine skips its `setVolume` step while
  a call is active; the configured fade resumes after `IDLE`.
- The backup-sound escalation still raises the system `STREAM_ALARM`
  volume to max (post-call audio will be loud) but no longer fights
  the call-state mute by force-setting the player to 1f.

### Internal

- Bumped to `versionName = "1.11.2"`, `versionCode = 56`. README badge,
  install command, and Wear module version synced.
- No new permissions: `TelephonyCallback.CallStateListener` and the
  legacy `PhoneStateListener.onCallStateChanged` only need
  `READ_PHONE_STATE` to read the incoming phone number, which ACX never
  reads.

## [1.11.1] - 2026-05-16

Fixes the v1.6.0 challenge sanitization regression (roadmap N1). No schema
changes.

### Fixed: dismiss-challenge persistence

- Added `ROCK_PAPER_SCISSORS`, `EMOJI_MEMORY`, `TYPING_SPEED`, and `WORDLE`
  to `Alarm.VALID_CHALLENGE_TYPES`. These four challenges shipped in v1.6.0
  but were missing from the sanitization whitelist, so `Alarm.sanitized()`
  silently rewrote them to `NONE` on every backup export/import, share-link
  round-trip, and DataStore read. Affected alarms now persist correctly.
- The same fix also unblocks these four challenges from appearing inside
  Mission Chain configurations, chains containing them were being stripped.

### Added: regression guard

- Property test `AlarmTest.every ChallengeType survives sanitized round-trip`
  iterates `ChallengeType.entries` and asserts each value round-trips through
  `Alarm.sanitized()`. A companion test exercises the challenge-chain path.
  If a future `ChallengeType` is added without updating
  `VALID_CHALLENGE_TYPES`, the test fails with a directive pointing at the
  whitelist.

### Documentation

- README's "Dismiss Challenges" table now lists 22 user-facing challenges
  (was 18 visible + 4 hidden) and the section header reads "(22 Types)".
- Challenge-types crib now lists all 22 + `NONE` and documents
  the whitelist invariant.

### Internal

- Bumped to `versionName = "1.11.1"`, `versionCode = 55`. README badge,
  install command, and Wear module version synced.

## [1.11.0] - 2026-05-14

Wear OS next-alarm tile. No schema changes.

### Added: wearable control surface

- Added a dedicated Wear OS companion module with a next-alarm Tile provider,
  static preview resource, and matching package/signature setup for Data Layer
  security.
- Added a Play-flavor phone-to-watch Data Layer bridge that publishes the next
  alarm, trigger time, label, and live firing state to the watch.
- Added watch-side skip, snooze, and dismiss message actions. Snooze and dismiss
  are only forwarded when the matching alarm is actively firing, so the tile
  cannot accidentally run post-dismiss flows for a future alarm.

### Changed: flavor and build structure

- Added the Play Services Wearable dependency only to the Play flavor and Wear
  module; the F-Droid app flavor binds a no-op wearable bridge.
- AlarmService now publishes firing and idle transitions to the wearable bridge
  so the tile changes from scheduled skip controls to live alarm controls.

### Internal

- Bumped to `versionName = "1.11.0"`, `versionCode = 54`. README badge,
  install command, roadmap snapshot, and Wear module version synced.

## [1.10.10] - 2026-05-14

Android 16 next-alarm Live Update. No schema changes.

### Added: notification countdown polish

- Added an Android 16 `Notification.ProgressStyle` path for the persistent
  next-alarm notification when the next alarm is inside the final two-hour
  window.
- The live notification now uses the alarm fire time as the `when` countdown
  and chronometer source, so status-bar chips can show time remaining without
  relying only on app-side minute-boundary reposts.
- Requests promoted ongoing treatment through the documented compatibility
  extra and declares `POST_PROMOTED_NOTIFICATIONS`; older devices and far-future
  alarms keep the existing quiet persistent notification.

### Changed: platform target

- Raised `compileSdk` to 36 so the Android 16 ProgressStyle APIs are available
  while leaving `targetSdk` at 35.
- Added unit coverage for the two-hour Live Update eligibility window and
  countdown progress calculation.

### Internal

- Bumped to `versionName = "1.10.10"`, `versionCode = 53`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.9] - 2026-05-14

Material 3 Expressive opt-in. No schema changes.

### Added: expressive personalization

- Added an **Expressive surfaces** toggle in Settings so users can opt into a
  bolder Material 3 shape rhythm without changing the default app look.
- Added theme-level expressive shape tokens for shared cards, chips, tiles,
  loading skeletons, empty-state icon containers, bottom navigation, and the
  main alarm-list cards.

### Changed: platform polish

- Updated the Compose BOM to `2026.05.00`, aligning the app with the stable
  Material 3 1.4.0 release that includes Material 3 Expressive APIs.
- Updated the Android Gradle Plugin to `8.11.1` and the Gradle wrapper to
  `8.13` so the newer Compose lint artifacts run on a compatible lint stack.
- Preserved the expressive setting through DataStore and JSON/encrypted
  backups so visual preferences survive restore flows.

### Internal

- Bumped to `versionName = "1.10.9"`, `versionCode = 52`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.8] - 2026-05-14

Changelog roadmap handoff. No schema changes.

### Added: release follow-through

- Added a **What's next** action to the in-app What's New dialog that opens
  the project roadmap on GitHub.
- Mark the current release dialog as seen when users open the roadmap, avoiding
  repeat prompts after they choose to continue reading.

### Changed: current release copy

- Refreshed the live dialog highlights to cover the current v1.10 work:
  wake-streak badge, first-meeting calendar shifts, Bedtime DND, haptic-only
  alarms, hold-to-dismiss, and exact snooze picking.

### Internal

- Bumped to `versionName = "1.10.8"`, `versionCode = 51`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.7] - 2026-05-14

Wake-streak badge. No schema changes.

### Added: Stats motivation

- Added a dedicated **Wake streak** flame badge card on the Stats tab with the
  active streak, best streak, and next milestone progress.
- Updated Stats hero and mini-card labels so streak state reads as a wake habit
  signal instead of a raw counter.

### Changed: streak correctness

- Kept an active streak alive through yesterday when today's alarm has not
  fired yet, so the badge no longer drops to zero before the user has a chance
  to wake up.
- Added unit coverage for current streaks, best streaks, duplicate dates, and
  malformed stored date rows.

### Internal

- Bumped to `versionName = "1.10.7"`, `versionCode = 50`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.6] - 2026-05-14

First-meeting calendar alarms. No schema changes.

### Changed: calendar auto-alarm

- Moved the Calendar auto-alarm from a slow daily pass to a settings-aware
  15-minute WorkManager refresh, with immediate one-shot refreshes on app start
  and Settings changes.
- Re-target the single reusable Calendar alarm when tomorrow's first timed
  event moves, so wake time follows schedule edits without duplicating alarms.
- Ignore all-day calendar entries so birthday, PTO, and holiday banners do not
  create a midnight wake alarm.

### Added: Settings clarity

- Surfaced the previously hidden **First-meeting auto-alarm** toggle in
  Settings alongside calendar visibility controls.
- Added a lead-time picker for 15-120 minutes, making the automation adjustable
  without editing DataStore-backed defaults manually.

### Internal

- Bumped to `versionName = "1.10.6"`, `versionCode = 49`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.5] - 2026-05-14

Bedtime DND ownership. No schema changes.

### Added: sleep-window DND

- Added an app-owned **Bedtime DND** rule backed by a real
  `ConditionProviderService`, so the app can manage an alarms-only sleep
  window instead of relying on Google Clock or a manually configured system
  rule.
- Added Bedtime-tab controls that clearly show whether DND policy access is
  granted, whether the rule is active, and when it will next start or end.
- Added a direct **Grant DND access** action that opens Android notification
  policy access settings when the user has not approved the app yet.

### Changed: alarm-aware sleep protection

- The DND condition now runs from the configured bedtime until the next alarm's
  local wake time, falling back to the sleep-goal duration when no alarm is set.
- Alarm scheduling, disabling, deletion, boot reschedule, and alarm-fire
  handling now re-sync the Bedtime DND rule so the wake boundary follows the
  current next alarm.
- Backup/export/import now preserves the Bedtime DND opt-in while leaving the
  system rule id device-local.

### Internal

- Bumped to `versionName = "1.10.5"`, `versionCode = 48`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.4] - 2026-05-14

Haptic-only alarm profile. No schema changes.

### Added: quiet alarm profiles

- Added a **Don't wake partner** preset in the alarm editor that applies a
  haptic-only profile using existing per-alarm fields.
- Allowed override-volume alarms to be set to `0%`, showing a clear `Muted`
  value instead of forcing a minimum audible volume.
- Added an active haptic-only status chip so users can see when the quiet
  profile is applied.

### Changed: firing behavior

- Treat `overrideSystemVolume + volume = 0` as a hard mute in `AlarmService`,
  skipping ringtone, Spotify, internet radio, fallback audio, and backup-sound
  escalation.
- Added repeating `VibrationEffect.Composition` haptics on API 30+ devices that
  support primitives, with the existing waveform vibration path as fallback.
- Route vibration through `AudioAttributes.USAGE_ALARM` so the tactile wake
  profile behaves like an alarm channel without waking the room through audio.

### Internal

- Bumped to `versionName = "1.10.4"`, `versionCode = 47`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.3] - 2026-05-14

Hold-to-dismiss alarm safety. DB v9, backup format v6.

### Added: alarm firing controls

- Added a per-alarm **Hold to dismiss** toggle for users who accidentally
  swipe a ready alarm away while half-awake.
- Added a visible firing-screen hold-progress dismiss control that requires a
  1.5-second press after wake-up steps are complete.
- Protected swipe-left dismissal when hold-to-dismiss is enabled, turning the
  swipe into a clear hold prompt instead of a direct destructive action.
- Kept snooze quick and discoverable, including right-swipe snooze and the
  exact-minute snooze picker from v1.10.2.

### Internal

- Added Room `MIGRATION_8_9` with `holdToDismissEnabled` and preserved the
  field in backup/export/import mapping.
- Bumped to `versionName = "1.10.3"`, `versionCode = 46`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.2] - 2026-05-14

Firing-screen snooze picker. No schema changes.

### Added: alarm firing controls

- Added long-press handling on the primary Snooze action to open an inline
  exact-minute picker without changing the alarm's saved default snooze length.
- Added visible preset/exact snooze controls on the firing screen so custom
  minutes are discoverable without relying only on a hidden gesture.
- Added service-side bounds for custom snooze requests, clamping one-off
  snoozes to 1-120 minutes.

### Internal

- Bumped to `versionName = "1.10.2"`, `versionCode = 45`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.1] - 2026-05-14

Weather air-quality context. No schema changes.

### Added: Weather tab air quality

- Added Open-Meteo Air Quality API integration for current U.S. AQI,
  particulate, ozone, and pollen variables.
- Added a Weather tab air-quality companion card with AQI banding, pollutant
  metrics, tree/grass/weed pollen rows, provider attribution, and graceful
  handling when pollen is not reported for the current location.
- Kept the core weather forecast independent from the second air-quality call,
  so an AQI provider outage does not block current conditions, hourly weather,
  radar, or the 3-day forecast.

### Internal

- Bumped to `versionName = "1.10.1"`, `versionCode = 44`. README badge,
  install command, and roadmap snapshot synced.

## [1.10.0] - 2026-05-14

Boot reschedule hardening. No schema changes.

### Fixed: boot reliability

- Moved boot, package-replaced, and clock-change alarm rescheduling out of
  `BootReceiver` and into a unique expedited WorkManager job so large alarm
  libraries are not constrained by the broadcast receiver ANR window.
- Added batched alarm rescheduling with one final widget refresh, avoiding a
  widget update after every individual AlarmManager registration for users with
  dozens of enabled alarms.
- Preserved force-recalculation semantics for time, timezone, and date changes
  while keeping boot/package-replaced paths on the existing future trigger when
  it is still valid.

### Internal

- Bumped to `versionName = "1.10.0"`, `versionCode = 43`. README badge,
  install command, and roadmap snapshot synced.

## [1.9.5] - 2026-05-13

Premium settings trust pass. No schema changes.

### Changed: wake reliability UX

- Added a dedicated **Wake readiness** section at the top of Settings so exact
  alarm access, alarm notifications, and battery protection are visible in one
  calm checklist instead of being scattered across unrelated controls.
- Added direct recovery actions for missing readiness items: notification
  permission, exact-alarm access, and battery optimization settings.
- Updated the Settings overview reliability tile to reflect all wake-critical
  system states, not only battery optimization.
- Split Settings permissions into wake-critical readiness and optional context
  permissions, reducing duplicate notification prompts while keeping calendar
  and weather setup discoverable.

### Internal

- Bumped to `versionName = "1.9.5"`, `versionCode = 42`. README badge,
  install command, and roadmap snapshot synced.

## [1.9.4] - 2026-05-13

Persistent next-alarm notification accuracy fix. No schema changes.

### Fixed: next-alarm notification

- The persistent status-bar notification now refreshes on the minute boundary
  where its "remaining" copy changes, instead of only refreshing when the alarm
  row changes in Room.
- The notification now dismisses at fire time if the database has not yet
  emitted the next schedule, preventing stale "next alarm" copy from lingering.
- The notification title now respects the app's 12/24-hour time setting.

### Internal

- Added focused unit coverage for the notification refresh cadence.
- Bumped to `versionName = "1.9.4"`, `versionCode = 41`. README badge,
  install command, and roadmap snapshot synced.

## [1.9.3] - 2026-05-13

Exact-alarm permission recovery release. No schema changes.

### Fixed: scheduling reliability

- Added a receiver for
  `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` so alarms saved while
  "Alarms & reminders" access was denied are re-armed as soon as the user grants
  the permission.
- Moved the recovery work into a unique expedited WorkManager job, backed by
  the existing Hilt worker setup, so heavy alarm lists are rescheduled outside
  the broadcast receiver timeout window.
- Forced recalculation during permission recovery to repair alarms whose
  `nextTriggerTime` had been cleared while exact scheduling was unavailable.

### Internal

- Bumped to `versionName = "1.9.3"`, `versionCode = 40`. README badge,
  install command, and roadmap snapshot synced.

## [1.9.2] - 2026-05-13

Premium-polish pass across the Compose UI. No schema changes.

### Changed: visual system

- Normalized app shape tokens to a sharper 8-12dp family across cards,
  dialogs, chips, inputs, bottom navigation, challenge panels, and
  secondary surfaces.
- Removed stadium chip/progress backdrops and replaced remaining raw
  Material chip rows with the app's shared `AppFilterChip` primitive.
- Removed negative typography tracking from the clock and headline type
  scale while preserving tabular numerals for stable time displays.
- Reworked circular decorative backdrops into squared premium swatches
  and icon containers where they were not true status dots or icon-only
  controls.

### Changed: UX polish

- Alarm list now distinguishes "all alarms paused" from a truly empty
  schedule and shows schedule feedback when alarms are enabled or paused.
- Settings integration tests now show active progress, lock the test
  buttons while work is running, and color pending state as in-progress
  instead of failure.
- Backup and restore actions now lock while import/export work is running
  and show a clear in-progress state before the result card appears.
- Alarm-edit section labels and solar-relative copy were tightened for
  consistent sentence case and clearer scheduling meaning.
- Final onboarding now keeps the privacy/permissions step readable on
  constrained phones, with shorter permission labels and no clipped copy.
- Bottom navigation selected state now uses color instead of a pill-shaped
  indicator, keeping the tab bar aligned with the sharper app shape system.

### Internal

- Bumped to `versionName = "1.9.2"`, `versionCode = 39`. README badge,
  install command synced.
- Replaced the YouTube downloader availability probe with an explicit
  `remember`/`LaunchedEffect` state loop so Compose lint passes cleanly.

## [1.9.1] - 2026-05-13

End-to-end engineering audit pass on top of v1.9.0. No new features, no
schema changes, six real bugs across the alarm-firing service, the
"Skip next" notification action, the Windy radar embed, the news fetch
identity, and the preferences-store error path.

### Fixed: alarm firing service

- **`AlarmService.isForeground` race fixed** by switching `@Volatile var`
  to an `AtomicBoolean` and gating every `startForeground` /
  `stopForeground` call through `compareAndSet`. The auto-silence job
  and the user's dismiss tap can both reach the foreground-stop path on
  serviceScope's IO dispatcher within microseconds of each other; the
  previous check-then-act was non-atomic, so both could pass `if
  (isForeground)` and call `stopForeground()` twice. Some OEMs (Samsung
  One UI 6) treat the second call as fatal. Touched: `service/AlarmService.kt`.

### Fixed: receivers

- **`SkipNextReceiver` now wraps its `goAsync()` work in
  `withTimeout(8_000L)`**, matching the pattern established for
  `BootReceiver` (v1.5.4) and `MissedAlarmUnlockReceiver` (v1.6.3).
  Previously a corrupt DB or storage lock could have pinned the
  `PendingResult` past the ~10 s ANR ceiling. Now logs a
  `TimeoutCancellationException` on overrun and finishes the
  `PendingResult` either way. Touched: `receiver/SkipNextReceiver.kt`.

### Fixed: Windy radar WebView

- **Mixed-content downgrade attack hardened**: `mixedContentMode` was
  `MIXED_CONTENT_ALWAYS_ALLOW`, which would let a hostile redirect
  downgrade Windy tile loads to plain HTTP. Switched to
  `MIXED_CONTENT_COMPATIBILITY_MODE`, passive resources still load if
  Windy ever needed them, but active mixed content (scripts) is blocked.
- **WebView memory leak fixed** with an `onRelease` block on the
  `AndroidView` that calls `stopLoading()` → `loadUrl("about:blank")` →
  `removeAllViews()` → `destroy()`. Without this, every navigation away
  from and back to the Today tab leaked an entire WebView (~5-15 MB
  including JS engine + GL context) for the lifetime of the process.
- **Defence-in-depth on the WebView surface**: explicitly set
  `allowFileAccess = false` and `allowContentAccess = false` so the
  embed cannot reach `file://` URIs even in a future regression.
  Touched: `ui/components/WindyRadarCard.kt`.

### Fixed: News tab

- **`NewsRepository` User-Agent now uses `BuildConfig.VERSION_NAME`**
  instead of a hardcoded `"1.8.0"` string. Some publishers (NPR's RSS
  bridge in particular) tier-throttle by UA; sending a stale UA from
  every release after 1.8 misrepresents the client. Touched:
  `data/news/NewsRepository.kt`.

### Fixed: preferences store

- **DataStore corruption recovery now logs**: `PreferencesManager`
  silently emitted `emptyPreferences()` on any `IOException`, which on a
  corrupted preferences file effectively factory-resets the user's
  webhook URL, Hue API key, news feed URL, and every other setting with
  no breadcrumb. Now logs a warning so a recurrence is diagnosable.
  Touched: `data/preferences/PreferencesManager.kt`.

### Internal

- Bumped to `versionName = "1.9.1"`, `versionCode = 38`. README badge,
  install command synced.

## [1.9.0] - 2026-04-29

The Today tab is alive. The screen background now renders the actual sky
above your location, interpolated minute-by-minute through a 15-keyframe
table anchored to real sunrise / sunset, and reacts to current weather:
storms swap to overcast blue-gray with lightning flashes at night, and
NWS tornado warnings paint a rotating funnel-cloud silhouette plus a red
warning banner.

### Added: `TimeOfDaySky` engine

- 15 hand-tuned keyframes spanning t = -0.40 (deep night before dawn)
  through t = 1.40 (deep night after dusk), with t = 0 at sunrise and
  t = 1 at sunset. Each keyframe stores a 3-stop gradient (`top`, `mid`,
  `bot`) corresponding to zenith / mid-band / horizon.
- `computeT(now, sunrise, sunset)` maps a clock time to its position
  along the day cycle. With sunrise 06:00 + sunset 20:00 (14h day),
  midnight resolves to t ≈ -0.43 (deep night), 11 PM to t ≈ 1.07 (dusk).
- `gradientForT(t)` linearly interpolates RGB between the two keyframes
  bracketing `t` so every minute reads as its own subtly-different sky.
- Convenience predicates `isDaytime(t)` / `isDeepNight(t)` for downstream
  layers (lightning intensity, content contrast).

### Added: Weather overrides

- `WeatherSkyOverrides.STORM_DAY` (gray-blue overcast) and `STORM_NIGHT`
  (near-black) bypass the time-of-day table when the current Open-Meteo
  weather code is 95-99 (thunderstorm / hail).
- `WeatherSkyOverrides.TORNADO_SKY`, the classic dark-olive ceiling /
  sickly yellow-green horizon, bypasses everything when an active NWS
  tornado warning is detected.

### Added: `WeatherSkyBackground` composable

Stacks five layers behind the Today tab content:

1. **Base sky gradient** (time-of-day or weather override).
2. **Long fade to `SurfaceDark`** so cards below the hero return to the
   app's neutral surface, a vivid sky behind a vivid weather card would
   sap contrast.
3. **Lightning flashes** when the current weather code is a thunderstorm.
   A stochastic 4-9-second loop drives short ramps (60ms up, 220ms decay)
   to white at ~28% alpha; tornado mode boosts the intensity and adds
   ~30%-chance double-strike aftershocks.
4. **Tornado funnel + warning banner** when `tornadoAlertActive` is true.
   The funnel is a Canvas-drawn silhouette with rotation + drift
   animations layered over each other; the banner pins below the status
   bar with a red TORNADO WARNING + cyclone icon.
5. **Actual content**: Today's Column rendered on a transparent column
   so the sky shows through.

### Added: NWS alerts integration

- `WeatherAlertsApi` + `WeatherAlertsRepository` against
  `api.weather.gov/alerts/active`. Free, no key, US-only. Returns empty
  features outside the US, so it's safe to call unconditionally.
- Sends a User-Agent identifying the app + repo URL, required by NWS
  to avoid 403s under their rate-limit policy.
- Distills the response to `WeatherAlertFlags(tornadoActive, severeStorm,
  headline)`, the rest of the app only needs the boolean signal.
- All failures absorbed silently; alerts are bonus context, never the
  critical path.

### Changed

- **`AlarmClockHeroHeader`** gains a `transparent: Boolean = false`
  parameter. When true, the hero skips its own gradient + radial overlay
  so a parent backdrop (the dynamic sky) shows through. Other tabs
  retain the default header treatment unchanged.
- **DashboardUiState** gains `sunriseLocal: LocalTime?`, `sunsetLocal:
  LocalTime?`, `currentWeatherCode: Int?`, `tornadoAlertActive: Boolean`,
  `severeWeatherHeadline: String?` so the dynamic sky has parsed inputs
  rather than re-parsing display strings.
- **DashboardScreen** wraps the entire scrollable content in
  `WeatherSkyBackground`. The hero hosts an inline `Tornado warning`
  status chip when an alert is active, so the signal is visible before
  the user scrolls.
- **NetworkModule**: new `provideWeatherAlertsApi` against
  `api.weather.gov` baseUrl.

### Notes

- **US-only tornado coverage** by design, NWS only issues alerts for
  the United States. International users see the time-of-day sky and
  the storm/lightning visuals; tornado overlay never triggers.
- The keyframe colors were specified by user request and are stored in
  `TimeOfDaySky.KEYFRAMES`. Editing the table (e.g., for a more saturated
  sunset) changes the visual everywhere.

## [1.8.1] - 2026-04-29

Premium-polish pass. No new features, no schema changes, every change in
this release sharpens an interaction or a surface that already worked but
felt rough on close inspection. Driven by a top-to-bottom design audit
(visual hierarchy, component consistency, microcopy, motion, empty/loading
states, accessibility) and verified on a real device.

### Design system

- **`AppIconSize` tokens** (xs=14, sm=18, md=22, lg=32 dp). Replaces the
  ad-hoc 13/15/18/20/22 dp drift that crept across cards, chips, tiles,
  and metric tiles.
- **`AppFilterChip`** primitive that matches `AppStatusChip` geometry,
  same min height (32 dp), same `AppChipShape`, same accent treatment.
  Migrated AlarmList's group filter row + News's feed filter row off raw
  Material `FilterChip` so chip rows hold a single rhythm regardless of
  chip kind.
- **`AppSkeletonBlock`** primitive, a shimmering placeholder block used
  to compose skeleton rows (News list, radar) so first-paint feels
  purposeful instead of presenting a single spinner.

### Bottom navigation (the most visible change)

- `alwaysShowLabel = false`. With six tabs in 1080 px, every label
  truncated ("Weath…" / "Setti…") which read as broken layout. The
  Material 3 idiom for crowded bars is exactly this, the selected tab
  carries its label inside the indicator pill, the rest sit as confident
  icons. The pill becomes the focal affordance.
- "Weather" (7 chars) still got clipped to "Weathe" inside the M3 pill,
  so the tab label is now **Today** (the screen hero still reads
  "Weather"). Pragmatic and accurate, the tab is a daily-overview hub.

### Live radar (Weather)

- **Skeleton + fade-in.** The 360 dp WebView slab used to flash dark for
  1-3 s on cold connections. New `WebViewClient` hooks `onPageStarted` /
  `onPageFinished` to drive a `loaded` flag; a shimmering skeleton fills
  the slot and cross-fades out (240 ms) as the WebView fades in (280 ms).
- "Open in Windy" relocated from a left-aligned `TextButton` under the
  map to a header-aligned `AppStatusChip` that sits next to the title.
  No more orphaned link below a centered map.
- Header retitled "Animated precipitation near $location · Windy", same
  info, half the words.

### News tab

- **Pull-to-refresh** via Material 3 `PullToRefreshBox`, the canonical
  RSS gesture, replacing the icon-only refresh as the primary affordance
  (the icon stays in the hero actions slot for accessibility).
- **Skeleton list** (4 placeholder cards) on first load, replacing the
  single `AppLoadingCard` spinner.
- **`AppFilterChip`** on the feed picker.
- Cleaner microcopy: subtitle "Headlines from your selected feed.",
  section title "Top stories" with no description, error empty-state
  "Pull down to try again, or pick a different source.", empty-state
  "No headlines yet."
- News card title clamped to 3 lines so very long Google News headlines
  don't blow out the card height.
- Hero "Updated just now" badge dropped, the relative-time chip with
  the Schedule icon is enough; "Updated" was redundant.

### Microcopy across the app

Every hero subtitle, section description, and empty-state copy went
through a "≤12 words and only what's true" pass.

- Alarms hero (no alarms) "Tap + New alarm to schedule your first."
  (was "Create, group, and refine alarms from one calm control center.")
- AlarmList "Quick alarms" description "Tap a duration to schedule it
  now." (was "Need a short reminder or power nap? Start one with a
  single tap.")
- AlarmList "Groups" description dropped, title alone is clearer.
- AlarmList empty-state "Create your first wake-up, or start from a
  template." (dropped the "polished head start" marketing tail).
- Today calendar empty-state "Calendar access needed" / "Grant
  permission to surface today's events here." (was three sentences).
- Today calendar empty-events "Nothing scheduled today" / "Events from
  your calendar will appear here." (was "Enjoy the breathing room…").

### Typography rhythm

- In-card section headers ("Next few hours", "Next 3 days", "Today's
  schedule", "Live radar") promoted from `titleSmall` (15 sp Medium)
  to `titleMedium` (17 sp SemiBold) so card-level headers hold a
  distinct tier above the metric-tile values.

### What's new highlights refresh

- `MainActivity.WHATS_NEW_HIGHLIGHTS` had been showing v1.6.0 bullets
  even after v1.7.x and v1.8.0 shipped. Now reflects the actual v1.8.0
  user-visible additions (Weather hub + radar, News tab,
  pull-to-refresh, bottom-nav rework).

## [1.8.0] - 2026-04-29

Two new tabs and a live radar embed. The "Today" tab graduates into a full
**Weather** hub with an animated precipitation radar from Windy, and a brand
new **News** tab pulls public RSS feeds (Google News, BBC, NPR, Hacker News).
Both follow the existing no-account/no-API-key rule, Windy via its public
embed endpoint, news via plain RSS over OkHttp + Android's built-in
XmlPullParser. No new SDKs.

### Added

- **Live radar on the Weather tab.** New `WindyRadarCard` composable, a
  fixed-height (360 dp) `WebView` pointed at `embed.windy.com/embed2.html`
  with `overlay=radar` and `radarRange=-1` for animated playback. The embed
  endpoint serves no `X-Frame-Options` / CSP, so it loads cleanly in WebView
  with `javaScriptEnabled` and `domStorageEnabled`. Auto-centers on the
  user's weather location (lat/lon already plumbed for forecast). A
  secondary "Open full map in Windy" button hands off to the browser via
  `LocalUriHandler` for users who want pan/zoom past what the embed allows.
- **Weather tab.** Renamed bottom-nav label from "Today" → "Weather" and
  retitled the hero. Calendar still lives below the fold but is no longer
  the headline. Hero chips reduced to the active context only.
- **News tab.** New `NewsScreen` + `NewsViewModel` + `NewsRepository`.
  Six pre-configured feeds (Google News Top/World/Tech, BBC, NPR, Hacker
  News) selectable via filter chips; the active feed is persisted to
  DataStore (`newsFeedUrl`). Each headline renders as a tappable card,
  title, 3-line snippet, source chip, relative-time chip ("58m ago"),
  open-in-new icon. Pull-to-refresh button in the hero actions slot.
  External links open in the system browser via `LocalUriHandler`.
- **`RssParser`**: minimal RSS 2.0 / Atom parser using Android's built-in
  `XmlPullParser`. Skipped Rome (~600 KB JAXB-heavy), kept the dep
  footprint at zero. Handles RFC-822 + ISO-8601 dates, falls back to
  channel title for the source field, defensively skips unknown tags so
  vendor extensions don't kill parsing.
- **Settings**: four new toggles, Show News tab, Live radar on Weather
  tab, plus the renamed "Show Weather tab". Updated supporting text on
  the existing Weather/Timer/World toggles.

### Fixed

- **`RssParser` container descent.** Initial implementation walked the
  document with a top-level `else -> skip(parser)` branch, which ate the
  entire `<channel>` (RSS) or `<feed>` (Atom) subtree along with all
  items. Container tags now fall through (`Unit`) so the parser keeps
  walking into them.

### Changed

- **Bottom nav labels** clamped to one line + ellipsis (`maxLines = 1`,
  `softWrap = false`). With 6 visible tabs on a 1080-px phone, "Weather"
  and "Settings" were wrapping to two lines and breaking the row's vertical
  rhythm.
- **`AppSettings`** gained `showNewsTab`, `showRadarEmbed`, `newsFeedUrl`.
  All default to safe values so a fresh install or backup-imported config
  from v1.7.x boots straight into a working Weather + News experience.

### Build

- No new external dependencies. Reuses the existing OkHttp 4.12.0 client
  (15 s timeouts, shared with weather + holiday + webhook calls). The
  News data layer is ~250 lines of Kotlin against the platform XML parser.

## [1.7.5] - 2026-04-29

Visual UX uniformity pass. Touring the app on a real device exposed two
layout regressions where the bottom of a tab read as empty even though
content existed below. Both stem from the same Compose footgun: nesting
a `Card`-with-content inside a `Column` and giving it `Modifier.weight(1f)`.
`Card` (and `AppSurfaceCard`) wraps content height, it doesn't honour
the weight allocation, so on a tall device the area below the wrapped
card stays empty. This release replaces those layouts with scrollable
columns and a manually-positioned FAB so every tab has a consistent,
fully-occupied vertical rhythm.

### Fixed

- **Timer tab: empty space below the hero on devices with no active
  timers.** Switched the parent `Column` to `verticalScroll`, dropped
  the `weight(1f)` on `TimerInputView`, replaced the inner `LazyColumn`
  for active timers with a forEach `Column`. Adds a 24dp Spacer at the
  bottom so the input card breathes above the floating bottom nav.
- **World Clock tab: saved cities not visible despite the "N cities"
  hero chip.** Replaced the inner `Scaffold` (which competed for system
  insets with the outer `AppNavigation` `Scaffold`) with a `Box` that
  hosts the hero + content `Column` and overlays the FAB at
  `BottomEnd`. `LazyColumn` `contentPadding.bottom` set to 96dp so the
  last city card never hides behind the FAB. The hero chip "N cities"
  is now hidden in the empty state for less visual noise.
- **Today tab: duplicate "Now" cells in the hourly strip.** The
  `isFirstFutureSlot` predicate ran a 45-minute window check on every
  cell, so two or three adjacent hours all rendered with the "Now"
  label. Replaced with a single-flag `firstNowAssigned` toggled after
  the first matching cell. (already shipped in 1.7.4 hotfix path,
  consolidated here.)
- **Alarms tab: "Swipe to delete" text bleeding through disabled
  alarm cards.** `AlarmCard` uses `SurfaceCard.copy(alpha = 0.55f)`
  for disabled alarms, so the `SwipeToDismissBox` background (always
  rendered, just transparent when not swiping) showed through any
  disabled foreground. Looked like a stuck swipe gesture. Now the
  delete affordance is gated on `isSwiping = currentValue !=
  Settled || targetValue != Settled` so it only paints during an
  active gesture. Also added a `LaunchedEffect(Unit)` that snaps
  `dismissState` back to `Settled` on first composition, handles
  the rare case where a saved partial-drag offset is restored across
  navigation.

### Polish

- **World Clock hero chip set** trimmed in the empty state, no point
  showing "0 cities" when the empty card already says "No world clocks
  yet".

## [1.7.4] - 2026-04-29

Today-tab weather pass. Centered, denser, and more useful for an alarm
context. Pulls a few well-targeted features from the Aura-stack
companion weather app (~/repos/ZeusWatch).

### Changed

- **Centered weather card.** Location chip, big icon (64dp), big temp,
  condition text, and "feels like" line are now vertically stacked and
  horizontally centered. Edit-location pencil moved to the top-right
  corner so it doesn't fight the hero composition.
- **Removed the "Weather / Current conditions and a short forecast for
  the rest of your day" section title.** The icon + temp + description
  already self-narrate; the title was eating ~50dp of vertical space.
- **Vertical 3-day forecast.** Replaced the horizontal LazyRow with a
  single column. One day per line: day name | weather icon |
  description | rain chip | H / L. Easier to scan; no truncation.

### Added (ported from ZeusWatch)

- **Sunrise / sunset row.** Most useful weather field in an alarm-clock
  context, answers "is the sun up by my alarm time?" Lifted from
  ZeusWatch's GoldenHour card, slimmed to a horizontal pair.
- **UV index** in the metrics grid, with EPA-style band labels
  (low / moderate / high / very high / extreme).
- **Next-few-hours strip.** Horizontal-scrolling 8-cell forecast
  showing time, icon, temp, and rain% per hour. Lifted from
  ZeusWatch's HourlyForecastStrip pattern. The first cell is "Now."
  Cells include rain% only when ≥20%.

### Architecture

- `WeatherApi` now requests `hourly=temperature_2m,weather_code,
  precipitation_probability` and `daily=…sunrise,sunset,uv_index_max`.
  `forecast_hours=12` keeps the response small.
- New `HourlyWeather` model + `HourlyForecast` UI state cell. New
  `HourlyForecast` data class + `formatTimeOfDay()` /
  `formatUv()` / `buildHourly()` helpers in `DashboardViewModel`.
- `ForecastDay` now carries an `icon` field so the vertical row can
  render a glyph next to the description.

### Notes

- All times honour Open-Meteo's `timezone=auto` so the strip and the
  sunrise/sunset row read in the location's local time, not the
  device's.
- F-droid build is unaffected, Open-Meteo is free and unlicensed.

## [1.7.3] - 2026-04-29

### Changed

- **YouTube downloads now show real progress.** The static spinner +
  "Downloading..." text read as "stuck" in user testing. Replaced
  with a determinate `LinearProgressIndicator` paired with a rotating
  status label ("Resolving audio stream…" → "Connecting to YouTube…"
  → "Downloading audio…" → "Almost there…" → "Saving to your alarms…")
  and a live percentage. The bar follows an asymptotic curve that
  reaches ~30% in the first 4 seconds and crawls toward 92%, the
  jump to 100% on actual completion still feels like a finish.

### Why faux

Real progress is hard to surface here: yt-dlp's `--get-url` resolve
step has no progress signal, and OkHttp byte-counting only kicks in
after the stream resolves. Pegging a determinate bar to elapsed time
keeps the UI honest about *something happening* without making up
fake byte counts.

## [1.7.2] - 2026-04-29

Preview YouTube alarm sounds before downloading.

### Added

- **Per-result preview button** in the YouTube search dialog. Tap ▶ on
  any result to stream the lowest-bitrate audio (~1-3 s to start, no
  full download). Tap ⏹ to stop, tap ▶ on another result to switch.
  The downloaded clip lands at full quality only when you tap the row
  body to commit. Mirrors the audition pattern in the Aura/FreeVibe
  app's YouTube tab.

### Architecture

- New `YouTubeAudioDownloader.getPreviewStreamUrl(youtubeUrl)`
  returning a Result<String> with a directly playable URL. Play impl
  uses `yt-dlp -f worstaudio --get-url` (fastest resolution path,
  smallest buffering). F-droid impl returns the standard
  "not available" failure.
- Session-only LRU cache of resolved URLs (64 entries, 3-hour TTL,
  half of YouTube's typical 6-hour signed-URL window). Prevents
  re-resolving when the user previews the same clip twice.
- `MediaPlayer` lifecycle owned by the dialog: switching preview
  stops the previous one, dialog dismissal releases it,
  `setOnCompletionListener` clears state when the clip ends, and a
  `setOnErrorListener` falls through to "couldn't play that preview"
  without leaking the player.
- Tap zones split per row: the play/stop button auditions, the row
  body downloads. Both gestures stay deliberate.

### Notes

- Preview audio plays through the **media** stream (not the alarm
  stream) so the user can audition without competing with their
  alarm volume preference.

## [1.7.1] - 2026-04-29

User-driven on-device polish pass. Visible response to first real-device
testing of v1.7.0.

### Added

- **Hide bottom-nav tabs**: Settings → Bottom navigation lets you turn
  off Today, Timer, and World individually. Alarms and Settings always
  stay. If you're on a tab you just hid, the app bounces you back to
  Alarms automatically.
- **Search YouTube from the download dialog**: paste a URL or search
  by keyword (NewPipe Extractor; same library Aura uses). Tap a result
  to download. Filters to clips ≤4 minutes so 90-minute reaction
  videos don't crowd the list.
- **Prominent "Download alarm sound from YouTube" card** on the Alarms
  screen, top-level, not buried inside "create new alarm." Build up a
  library of tones first, attach them to alarms whenever.

### Fixed

- **Alarms / World screens didn't fill the screen.** Their inner
  `Scaffold` was double-applying system insets on top of the outer
  AppNavigation Scaffold, leaving a visible gap above the floating
  bottom nav. Both now set `contentWindowInsets = WindowInsets(0)`.
- **Alarms screen wasted vertical space.** Removed the redundant
  Sort / gear buttons in the top-right (sort is already a tappable
  chip; the gear duplicated the Settings tab). Dropped the "Saved
  alarms" section title + description (the hero subtitle already says
  the same thing). Tightened hero padding 18dp → 12dp and gap
  14dp → 10dp. Net: ~150dp of vertical real estate reclaimed; both
  alarms now visible above the fold on a 6-inch phone.
- **YouTube downloader was disabled at runtime.** First on-device test
  hit `FileNotFoundException: libpython.zip.so` because AGP 8 packs
  native libs inside the APK by default, and yt-dlp expects them
  extracted to disk. Added `packaging.jniLibs.useLegacyPackaging =
  true` (matching Aura's setup).
- **Battery-optimisation status didn't refresh on return** from the
  system settings page. Added a lifecycle observer so
  `refreshBatteryStatus()` re-runs every time SettingsScreen resumes.
- **Removed marketing-y "Everything important is visible at a glance"**
  subtitle from the Alarms hero, now reads "Tap an alarm to edit it,
  or add a new one below."

### Notes

- F-droid build keeps stub implementations for both download and
  search; entry points stay hidden on that flavor as before.
- yt-dlp init failure is silent: the entry point on Alarms / picker
  just doesn't show up. The init poll re-emits as soon as it
  succeeds, so users don't need to restart the app to see it.

## [1.7.0] - 2026-04-29

Download alarm sounds from YouTube. Ported from the Aura/FreeVibe app.

### Added

- **Download from YouTube button in the alarm-sound picker.** Opens a
  small dialog that takes a YouTube URL plus an optional name, downloads
  the best audio track via yt-dlp, and saves it to the device's Alarms
  folder via MediaStore. The downloaded sound shows up in the picker
  immediately, no extra wiring, because the picker already enumerates
  every alarm-tagged file the system knows about.

### Architecture

- New `YouTubeAudioDownloader` interface in `:main` with two flavor
  implementations:
  - **play**: real `PlayYouTubeAudioDownloader` backed by yt-dlp
    (`io.github.junkfood02.youtubedl-android:library:0.18.1`). Resolves
    `bestaudio` URL via `--get-url`, streams it through OkHttp into
    `MediaStore.Audio` with `IS_ALARM=1` and
    `RELATIVE_PATH=DIRECTORY_ALARMS`. Hard-capped at 60 MB to defend
    against hostile / mis-resolved CDN responses.
  - **fdroid**: stub that returns "not available in this build". The
    yt-dlp library bundles a native Python interpreter that isn't
    F-Droid-compatible, so the entry point is hidden on that flavor.
- New `YouTubeDownloadInitializer` interface, the play impl unpacks
  yt-dlp binaries off the main thread in `AlarmClockApp.onCreate`; the
  f-droid impl no-ops. The UI checks `downloader.isAvailable()` before
  showing the entry point, so init failure (no network, broken unpack)
  cleanly hides the feature instead of crashing it.

### Permissions

- Added `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"`. Required only
  on Android ≤8.x for MediaStore writes; API 29+ uses scoped storage.

### Tests

- `PlayYouTubeAudioDownloaderTest`: URL validation (8 canonical forms
  pass, 6 hostile forms reject), name sanitiser (whitespace, unsafe
  chars, length cap, lowercase).

### Notes

- The yt-dlp library bundles `libpython.so` + `libpython.zip.so` +
  `libqjs.so` natively, so the play APK grows by ~10-15 MB. F-droid
  stays lean.
- Source: ported from `~/repos/Aura` (`YouTubeRepository.kt` for the
  yt-dlp invocation, `SoundApplier.kt` for the MediaStore write
  pattern). NewPipe Extractor (Aura's search backend) was deliberately
  NOT ported, alarm-sound discovery is a paste-URL UX, not a search.
  FFmpeg post-processing (Aura's trim/fade/normalise pipeline) was
  also skipped; downloaded audio plays as-is.

## [1.6.3] - 2026-04-29

End-to-end engineering audit pass. No new user-facing features; targets
real reliability, security, and consistency bugs found across the
service, scheduler, receiver, and import paths.

### Fixed

- **Webhook firing was racing service tear-down.** Dismiss/snooze events
  were dispatched on `serviceScope.launch`, then `stopSelf()` was called
  immediately after, `onDestroy()` cancelled `serviceScope` before the
  5-second OkHttp call could complete, so Tasker integrations missed the
  "dismissed" / "snoozed" events on slow connections. Webhook calls now
  run on an application-lived `SupervisorJob` scope owned by
  `WebhookService`, so service tear-down can no longer kill them.
- **Snooze-cap event/webhook mismatch.** When the user hit
  `maxSnoozeCount`, the alarm event log persisted `ACTION_DISMISSED` but
  the webhook fired the `"snoozed"` event, same physical action, two
  different stories. The webhook event name is now derived from the
  branch that actually executed.
- **`MissedAlarmUnlockReceiver` ANR risk.** Timeout was 25 seconds on a
  receiver running under `goAsync()`, which only extends the
  BroadcastReceiver ANR window to ~10 s on most Android versions,
  guaranteed ANR before the timeout could fire. Tightened to 8 s,
  matching the v1.5.4 fix already applied to `BootReceiver`.
- **`setAlarmClock` not protected from `SecurityException`.**
  `canScheduleExactAlarms()` was checked upstream, but the permission
  can be revoked between the check and the call (race), and some OEM
  builds throw even when the permission appears granted. Wrapped in
  try/catch with a `setAndAllowWhileIdle()` fallback so alarms still
  fire (within the 1-2 minute Doze window) instead of disappearing
  silently.
- **`AlarmShareCodec.decodeToken` had no payload size guard.** A hostile
  `acx://alarm?data=…` deep-link with a multi-megabyte token could OOM
  the app during Base64 decoding. Now hard-caps tokens at 16 KB (real
  alarm payloads are ~1-2 KB).
- **`BackupManager.importFromJson` wasn't actually per-alarm-resilient**
  despite the comment claiming so. A single corrupt alarm row would
  abort the entire import after partially saving earlier rows. Each
  save+schedule now lives in its own try/catch, with bad rows logged
  and skipped while the rest of the backup lands.
- **Stale "What's new" highlights.** The dialog described v1.5.0
  features but the app had since shipped v1.6.0/v1.6.1/v1.6.2.
  Refreshed to the actual changes returning users will see since their
  last open.

### Tests

- Added `AlarmShareCodecTest`: rejection of empty / blank / oversized
  share tokens.

### Why

Several "polish pass" releases had elevated the surface; this pass
elevates the failure paths. The webhook race was a silent
correctness bug for Tasker users; the missed-alarm timeout was a
guaranteed-ANR-on-stress bug; the import resilience and the share-token
size guard were hardening the edges. None of these changes alter normal
operation, they make the unhappy paths quiet and predictable.

## [1.6.2] - 2026-04-29

Easier alarm dismissal, both from the lock-screen notification and via
gestures on the firing screen.

### Changed

- **Tapping the alarm notification now opens the firing screen.** The
  notification used to set only `setFullScreenIntent`, so if the
  full-screen launch was suppressed (e.g. user is mid-call) or the
  notification had collapsed in the shade, tapping the body did
  nothing, only the action buttons were reachable. Added
  `setContentIntent(fullScreenPi)` so the notification body now routes
  to `AlarmFiringActivity`.
- **Swipe LEFT to dismiss, RIGHT to snooze.** The firing-screen swipe
  directions are flipped to match the user's mental model: dismiss is
  the destructive "get this out of my life" action and now lives on the
  left, mirroring swipe-to-delete conventions across Android. Snooze is
  the recoverable "buy me a few more minutes" action on the right.
  Hint copy and the "Alarm controls" status chips updated to match.

### Why

The old swipe direction (right=dismiss / left=snooze) made dismiss feel
like a forward action. In practice, users reach for "make this stop" as
a swipe-away gesture, left works better. And the missing
`setContentIntent` was a real dead-end: a returning notification tap did
absolutely nothing, which is exactly the wrong behaviour for an
ongoing-alarm notification.

## [1.6.1] - 2026-04-29

Premium-polish design-system pass. No new features, no schema changes,
targets the design tokens that ripple across every screen so the product
feels more coherent, intentional, and refined.

### Changed

- **Tabular figures across the clock typography.** `ClockTimeSmall` /
  `ClockTimeLarge` / `ClockTimeDisplay` now request `tnum` + `lnum` font
  features so digits no longer reflow when the clock ticks from `11:11`
  to `12:00`. Letter-spacing tightened to match.
- **Refined surface ladder.** Reworked `Color.kt` with a deliberate
  four-step ladder (`SurfaceDark` → `SurfaceMedium` → `SurfaceCard` →
  `SurfaceLight`), introduced `BorderSubtle` / `BorderStrong` /
  `OverlayHover` tokens, and slightly cooled the primary blue. Cards
  and chips now stack predictably under translucent overlays.
- **Simplified `AppSurfaceCard`.** Dropped the triple-overlay treatment
  (vertical white wash + radial accent + base color) for a single calm
  vertical sheen, a single stroke, and a single container color. The
  result reads as more confident on AMOLED.
- **Refined `AppStatusChip`.** Color-matched border (was hard-coded
  primary alpha), tighter padding, SemiBold label so chips feel like
  deliberate metadata rather than decorative noise.
- **Refined hero header.** Replaced the four-stop vertical gradient and
  nested overlay box with a single deep wash plus one off-center primary
  radial. No more banding on long screens; brand color reads true.
- **Refined bottom navigation.** Removed the redundant outer-container
  radial, tightened indicator alpha, dropped icon size to 22.dp and
  label scale to `labelSmall` for a denser, more premium feel.
- **`AppMetricTile` shared component.** Replaces the ad-hoc translucent
  surfaces scattered across Dashboard / Stats / Bedtime so every "small
  data card" is identical edge-to-edge.
- **Alarm card chip rows unified.** Two separate horizontal-scroll rows
  collapsed into one, with the empty-row case skipped entirely so cards
  don't end with phantom whitespace.
- **Switch styling refined.** Thumb is now `TextPrimary` over a primary
  track for a calmer, more deliberate "on" state instead of the prior
  light-thumb-on-translucent-track look.
- **Forecast / location-result tiles** now use `SurfaceLight` with a
  `BorderSubtle` stroke, matching the metric tile vocabulary.

### Why

Multiple polish passes (v1.5.3, v1.2.1+, v1.3.x) had elevated individual
screens, but the design tokens themselves had drifted: ad-hoc alpha
values, three-layer overlays per card, and hard-coded chip borders. This
pass touches the tokens once and lets every screen inherit the
improvement, the kind of system-level work that makes the product feel
more thoughtfully crafted without changing what anything does.

## [1.6.0] - 2026-04-26

Added 4 new dismiss challenges: **Rock Paper Scissors** (best-of-5 against CPU), **Emoji Memory** (match 8 pairs on a 4×4 face-down grid), **Typing Speed** (type a phrase at ≥15 wpm with ≤2 word errors), and **Wordle** (guess a 5-letter word in ≤6 tries). Each challenge refines the wake-up gauntlet for diverse cognitive and motor preferences.

### Added

- **Rock Paper Scissors (v1.6.0):** Best-of-5 challenge against the computer. Win 3 rounds to dismiss. Round outcomes immediately displayed; loss resets both scores for another attempt.
- **Emoji Memory (v1.6.0):** Classic memory-pairs game on a 4×4 grid. Cards face-up for 3 seconds (customizable) to memorize all 8 distinct emoji types, then face-down. Flip two at a time to find matches; wrong pairs flip back after 1 second.
- **Typing Speed (v1.6.0):** Transcription task with speed and accuracy gates. Phrase appears verbatim; user must type it at ≥15 wpm (customizable) with ≤2 word errors (customizable). Resets input and timer on failure; resets both scores on next submission.
- **Wordle (v1.6.0):** Guess a hidden 5-letter word from a curated 50-word list. Up to 6 guesses (customizable). Letter states color-coded (green=correct, yellow=present, gray=absent). Shows target word for 2.5 seconds on loss, then generates a fresh word; success proceeds immediately.

- **Challenge UI updates:** All four views follow the existing challenge card, support text, icon panel, and notice patterns. Properly integrate with the challenge chain pipeline, state resets, wrong-attempt tracking, and firing-screen dispatch.

## [1.5.4] - 2026-04-22

Reliability-hardening audit pass. No new user features, no schema
changes, targets real bug classes that became visible under Android
14+ foreground-service timing rules and rarer OEM device quirks.

### Fixed

- **`AlarmService.onStartCommand` promotes `startForeground()` out of
  the IO coroutine.** Previously the service did its Room lookup first
  and called `startForeground()` afterward from `Dispatchers.IO`. On a
  cold-start from Doze with heavy IO contention this could miss the
  ~5 second Android 14+ deadline, producing a
  `ForegroundServiceDidNotStartInTimeException` crash. Now a placeholder
  "Alarm ringing" notification is posted synchronously; the labelled
  version replaces it via `NotificationManager.notify()` once the alarm
  row has been fetched and sanitised.

- **`BootReceiver.rescheduleAll` timeout tightened 30s → 8s.** The
  v1.5.1 ceiling was set under the mistaken assumption that `goAsync()`
  extends the BroadcastReceiver ANR window to 30 seconds. In practice
  it caps at ~10 seconds on most Android versions, so a hung
  rescheduleAll would ANR before the timeout fired. 8 seconds leaves
  headroom while still covering realistic schedules.

- **Null-safe `getSystemService(...)` casts across sensor detectors
  and service lifecycle.** `AlarmService` (POWER_SERVICE),
  `SmartAlarmService` (SENSOR_SERVICE, POWER_SERVICE),
  `FlipDetector`, `ShakeDetector`, `SquatDetector`,
  `StepCounterListener`, and `ProximityCoverDetector` now use `as?`
  with graceful no-op fallbacks. Stripped-down AOSP and managed-profile
  devices have been seen to return null for these services; the
  previous hard casts would throw `ClassCastException` at construction
  time and crash the alarm pipeline before it could fall back to the
  default ringtone.

- **`AlarmService.onCreate` wake-lock acquisition guarded.** Rare OEM
  builds throw `SecurityException` from `PowerManager.newWakeLock()`
  when the process is in a restricted state; previously this killed
  the service before it could foreground. Now logged and skipped, the
  alarm still plays with the implicit wake from
  `FLAG_ACTIVITY_TURN_SCREEN_ON` on the firing activity.

## [1.5.3] - 2026-04-19

Premium UX and UI polish pass, no new features, no schema changes.
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
  column, a classic amateur pattern. The Switch itself communicates
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
  from `Type.kt`, a single place to tune the clock face aesthetic.



Follow-up polish pass closing the three "remaining risks" flagged in the
v1.5.1 audit. Still no schema change, no new user features,
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

- `assemblePlayDebug`: green
- `testPlayDebugUnitTest`, all tests green, 15 new unit tests added
  (9 for MissedAlarmReplayPolicy + 6 for ProximityCoverDetector)
- `assemblePlayRelease`: green; signed APK in
  `releases/AlarmClockXtreme-1.5.2-play-release.apk`

## [1.5.1] - 2026-04-18

Production-hardening pass driven by a dedicated audit. Targets real bug
classes identified in v1.5.0, ANR sources, service-restart data loss,
missed-alarm replay races, and sensor-quirk edge cases, without any
new user-facing features.

### Fixed: Critical

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

### Fixed: High

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

### Fixed: Medium

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
  - `SIMON_SAYS`, watch a 4-pad color sequence (length 4-6) and play it
    back. Wrong tap flashes red and restarts the round.
  - `DATE_BACKWARDS`, type today's ISO date reversed character-by-character
    (e.g. `2026-04-17` → `71-40-6202`). Cognitive gate that's easy on
    groggy motor skills but hard without actually reading.
  - `STROOP`, classic interference test; the displayed color-word is
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
  and `ChallengeGenerator` covers each, earlier versions would have
  thrown `IllegalArgumentException` on `valueOf()` for these.

## [1.4.0] - 2026-04-17

### Added (competitive-research pass: features absorbed from Alarmy, Sleep as
Android, BlackyHawky Clock, Fossify Clock, Google Clock, Turbo Alarm)

- **Count-the-Sheep dismiss challenge.** A playful CAPTCHA, sheep and goats
  drift across a starry panel; tap every sheep to a randomised target count
  without catching a goat. Joins the 15-challenge roster as
  `ChallengeType.COUNT_SHEEP`.
- **Quick Settings tile (Skip next alarm).** `SkipNextAlarmTileService`,
  shade tile shows the next alarm's day + time; one tap routes through the
  existing `SkipNextReceiver` so skip semantics match the persistent
  notification action (repeating: recompute; one-shot: disable). Inactive
  state when no alarm is queued.
- **Material You dynamic colors (Android 12+).** Opt-in toggle in Settings →
  Personalization. On Android 12+ the primary/secondary/tertiary palette
  derives from the user's wallpaper (while keeping the app's deep-dark
  surfaces). On older devices the toggle is persisted but no-op, with
  help copy that names the requirement so the setting never feels broken.
- **Cover-to-snooze.** New `ProximityCoverDetector`, hold a hand over the
  proximity sensor for ~1.5 s during an alarm to snooze. Global toggle, pairs
  with flip-to-snooze for phones where face-down accelerometer is flaky
  (e.g. in a phone stand).
- **Hardware-button action per alarm.** `Alarm.hardwareButtonAction`,
  `NONE` / `SNOOZE` / `DISMISS`. Volume Up/Down, Camera, Headset Hook keys
  are intercepted via `dispatchKeyEvent` when the alarm is firing and the
  alarm has opted into a non-NONE action. `NONE` falls through to normal
  system volume control. (Edit-screen UI surfacing tracked on ROADMAP.)
- **Dismiss at ringtone end.** `Alarm.dismissAtRingtoneEnd`, when set, the
  alarm's `MediaPlayer` loops off and an `OnCompletionListener` auto-dismisses
  the alarm once the song / ringtone finishes naturally. Ideal for Spotify
  users or anyone who wants "wake to one song."
- **Random ringtone pool.** `Alarm.ringtonePool`, comma-separated list of
  alarm tones. On each fire the service picks a random URI from the pool
  (supersedes `ringtoneUri`). Anti-habituation: the brain stops tuning out
  a single wake-up sound.
- **Repeat missed alarms safety net.** If an alarm auto-silences and the
  new `repeatMissedAlarms` pref is on, `MissedAlarmUnlockReceiver`
  (listening on `USER_PRESENT`) re-fires that alarm the next time the user
  unlocks within 10 minutes. State is cleared on every re-fire so a single
  miss can only retrigger once.
- **Bedtime wind-down checklist.** Mirror of the morning-routine feature,
  `AppSettings.bedtimeChecklist` (newline-separated items) renders as a
  tappable pre-sleep checklist on the Bedtime tab, with a reset affordance.
- **Configurable sleep-sound timer + fade.** `SleepSoundPlayer.play(...)`
  now accepts a `fadeDurationSeconds` (5-600) and respects
  `AppSettings.sleepSoundTimerMinutes` and `sleepSoundFadeSeconds`, so the
  final taper can be as short as 5 s or as slow as 10 min.
- **Power-nap preset row.** Alarm list → Quick alarms now carries a second
  row with cycle-aware nap lengths (15/20/25/45/90 min) on top of the
  existing reminder durations.
- **Backup format v4.** `AlarmBackup` and `SettingsBackup` extended with
  the v1.4.0 alarm fields and seven new preference fields.
  `MAX_SUPPORTED_BACKUP_VERSION = 4`; v1-v3 backups still import via
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
  `sleepSoundFadeSeconds`, `repeatMissedAlarms`, `napDefaultMinutes`, all
  round-tripped through `toSettings()` / `applySettings()` for drift-free
  persistence.

## [1.3.3] - 2026-04-16

### Fixed (audit pass 4: service lifecycle, worker delays, backup validation)

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
  permission-denied / cancelled SAF intent, wasting work and confusing
  error timing.

## [1.3.2] - 2026-04-16

### Fixed (audit pass 3: workers, widgets, orphan settings, backup integrity)

#### Critical correctness
- **`CalendarAutoAlarmWorker` no longer creates duplicate alarms.** Each daily
  run previously inserted a brand-new `Alarm` row, accumulating to 7+
  duplicates per week. The worker now keeps a single reusable auto-alarm row
  identified by a reserved `profileName`, queries
  `CalendarContract.Instances` (so RRULE-expanded recurring events are
  honoured, `Events` alone missed them), pins the alarm to a `specificDate`
  for tomorrow, and disables (rather than deletes) the row when tomorrow has
  no events so user-edits to time/sound persist.

#### Backup integrity
- **`data_extraction_rules.xml` now includes DataStore preferences.** Cloud
  backup and device-transfer were silently dropping the entire
  `alarm_settings.preferences_pb` file, vacation mode, holiday config,
  Philips Hue creds, accent color, every v1.2.0 personalization setting were
  not migrating. Photo-match reference photos are also included; transient
  crash logs are explicitly excluded. The manifest now references the rules
  file via `android:dataExtractionRules="@xml/data_extraction_rules"`,
  without that attribute the rules file was unused.

#### Reliability
- **`WidgetUpdater` no longer leaks a Job per call.** Replaced the
  per-call `CoroutineScope(Dispatchers.IO)` allocation with a single
  process-scoped `SupervisorJob` so toggling alarms doesn't accumulate
  unrooted jobs.

#### UX: orphan settings finally exposed
- New **Personalization** section in Settings exposes:
  - Accent color picker (six-swatch palette: Default Blue / Violet / Coral /
    Amber / Mint / Mono). Previously the `accentColor` setting was read by
    `MainActivity` but had no UI to change it, users were stuck on the
    factory blue forever.
  - **Show motivational quotes** toggle, which actually gates the quote
    rendering on the firing screen (previously the quote always rendered
    regardless of `showMotivationalQuotes`).
  - **Adaptive challenge difficulty** toggle (the
    `AlarmFiringViewModel` was already reading `snoozeRate` and bumping
    math difficulty, but the user setting that gates the feature was an
    orphan).
  - **Custom typing phrases** multi-line editor, `ChallengeGenerator`
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

### Fixed (audit pass 2: wider net)

#### Correctness
- **`StopwatchViewModel` is now monotonic**: `SystemClock.elapsedRealtime()`
  replaces `System.currentTimeMillis()`, so an NTP sync, DST flip, or
  user-initiated clock change mid-run can no longer rewind or fast-forward
  the stopwatch.
- **`StatsViewModel` keeps aggregates live**: totals/streak/snooze rate now
  recompute every time the recent-events flow ticks, so the screen no longer
  shows stale numbers if an alarm fires while it's open.
- **`WorldClockViewModel` persists user-curated zones**: saved zones are
  written to a SharedPreferences string-list, survive cold-starts, and skip
  any zone the JVM no longer recognises (no more crash from a stale entry).
  Toggling 24-hour format also re-renders immediately instead of waiting
  for the next 1-second tick.
- **`AlarmEditViewModel.save()` is re-entrancy guarded**: a fast double-tap
  on Save no longer creates two alarm rows. The `isSaving` flag now also
  resets in a `finally` so a transient DB/scheduler exception doesn't strand
  the user on a permanently-disabled "Saving..." button.
- **Edit flow tears down old schedules when the alarm is disabled**,
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
    handled, the alarm goes silent gracefully (notification + vibration
    + flashlight still fire) instead of throwing NPE into the catch block.
  - `Uri.parse(alarm.ringtoneUri)` is `runCatching`-wrapped so a corrupt
    custom-ringtone URI no longer crashes setDataSource.

#### Reliability / robustness
- **`ChallengeGenerator.generateMaze()`**: bounded retry (50 attempts) plus
  a guaranteed-solvable empty-walls fallback. The previous `while (true)`
  could in theory deadlock the alarm-firing flow on a pathological RNG
  outcome.
- **`SonarSleepService` audio-write loop** is null-safe and exits cleanly on
  any `write()` exception (e.g. AudioTrack released mid-loop).
- **`SonarSleepService.stopSonarHardware`** rewritten to use explicit blocks
  instead of the brittle `let { if(...) it.stop(); it.release() }` semicolon
  trick, both stop and release branches are now obviously reachable.

#### Security / privacy
- **`SettingsScreen` warns on plain-http webhook URLs**: alarm event
  payloads (label, time, action) were being sent unencrypted without any UI
  surface flagging it.

#### UX
- **Night clock is reachable from Settings**: was previously orphan code
  declared in the manifest with no in-app launcher. New "Night clock" tile
  in the Settings → Utilities section starts the bedside-mode activity.
- **`Theme.kt` is preview-safe**: `view.context as Activity` is now a soft
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
  finish simultaneously, only the first allocates audio and the existing
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
  and degrades gracefully when permissions are missing, `SEND_SMS` is no-op
  if not granted, and `CALL_PHONE` falls back to `ACTION_DIAL`.
- **Permissions declared:** `SEND_SMS`, `CALL_PHONE` (Guardian Angel) and
  `ACCESS_WIFI_STATE` (Wi-Fi dismiss challenge), previously these features
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
- **Alarm fade-in glitch fixed**: without a fade we no longer briefly
  attack at zero volume before snapping to full.
- **`Snooze` cancels Guardian Angel** since the user demonstrably interacted.
  The next fire after snooze re-arms it.
- **Dashboard tolerates malformed weather rows**: a single bad date in the
  Open-Meteo response no longer crashes the whole forecast.
- **`NextAlarmCalculator.formatRemaining`** renders `<1m` for sub-minute
  remainders instead of the misleading `0m` it used to show in the last
  minute before fire.

#### Maintainability
- **`PreferencesManager.update()` deduplicated**: both decode and apply now
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

## Roadmap archive: 2026-08-10, ROADMAP.md

<details>
<summary>Original roadmap snapshot</summary>

```markdown
# AlarmClockXtreme Roadmap

Living feature backlog. Blocked items live in
[Roadmap_Blocked.md](Roadmap_Blocked.md). Completed work lives in git history
and [CHANGELOG.md](CHANGELOG.md). Last research refresh: **2026-06-25**.

**Legend**
- `[ ]` Not started
- `[~]` Design / research stage
- Effort: **S** = single session, **M** = a few days of focused work,
  **L** = multi-phase initiative.
- Tier: **Now** (next release), **Next** (the one after), **Later**
  (kept on the list, not actively scheduled), **UC** (under consideration,
  needs scoping or platform readiness), **Rejected** (explicitly out).


---

## Current snapshot (v1.15.31)

- **Stack:** Kotlin 2.1, AGP 8.11.1 / Gradle 8.13, Compose BOM 2026.06.00 /
  Material 3 (1.4.x), Room 2.6.1 / DB v23, Hilt 2.56.2, Retrofit 2.11 + Moshi (codegen),
  DataStore 1.1.1, Glance 1.1.1, OkHttp 5.4.0, WorkManager 2.9.1, Wear Tiles
  1.6.0 / protolayout 1.4.0, Wear Data Layer, Wear Watchface complications
  data-source 1.3.0, Health Connect client 1.1.0 (Play flavor), ML Kit Digital
  Ink 19.0.0 (Play flavor), Media3 1.10.1, Direct Boot minimum alarm fallback,
  yt-dlp (`youtubedl-android` 0.18.1) + NewPipe Extractor
  0.26.3 (Play flavor only).
- **Targets:** `minSdk 26`, `targetSdk 36`, `compileSdk 36`,
  `versionCode 133`, `versionName 1.15.31`.
- **Surface area:** 186 Kotlin files in `:app` + 4 in `:wear`, two phone
  flavors (`play`, `fdroid`), **30 user-facing dismiss challenges** (all now
  whitelisted by `Alarm.sanitized()` after N1), 50+ alarm fields, 35+
  AppSettings fields, 6 phone tabs (Today, Alarms, Bedtime, Timer, World,
  News) + Settings.
- **What's missing vs. competitors:** standalone-watch story is still thin
  beyond the tile/complication pair; no on-device sleep-stage classifier; no AI sleep coach; no
  foldable/tablet adaptive layout; no full Direct-Boot custom-ringtone/challenge alarm; no
  on-device ML sleep-sound classifier. The good news: the alarm-clock core
  (scheduling, reliability, challenges, weather, bedtime DND, encrypted
  backup) is best-in-class for FOSS Android.

---

## Audit backlog (v1.15.29 deep-audit pass)

Verified findings deliberately NOT fixed in the v1.15.29 pass, each needs
design judgment, a large refactor, or on-device confirmation rather than a
surgical change.

- [ ] **P2/debt, God files.** `SettingsScreen.kt` (~4.1k lines),
  `AlarmEditScreen.kt` (~3.5k), `BedtimeScreen.kt` hold every page /
  pane / dialog. The section enums already give clean seams; extract per-page
  files. Effort: M. **In progress:** `BedtimeScreen.kt` is being drained
  section-by-section (`BedtimeJetLagSection.kt`, `BedtimeChronotypeSection.kt`,
  `BedtimeBreathingSection.kt` extracted so far, ~2.1k → ~1.65k lines).
  Remaining: finish the BedtimeScreen sleep-tracking / sleep-sounds / wind-down
  sections, then split `SettingsScreen.kt` and `AlarmEditScreen.kt`.

---

## LATER: kept on the list

Items revisited every two minor releases. Below are the categories with all
items. New entries from this pass are tagged **NEW**.

### Sleep tracking deepening

| # | Item | Source | Effort |
|---|------|--------|--------|

### Wear OS / wearable depth (beyond X1)

| # | Item | Source | Effort |
|---|------|--------|--------|

### Workplace / shift worker

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-WS3 | On-call rotation mode (override DND silent). | [PagerDuty](https://www.pagerduty.com/) | M |

### Household / relationships

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-H1 | Partner profiles (two users, separate alarms / ringtones). | Sleep as Android couples | M |
| L-H2 | Paired-phone LAN sync (partner-dismiss → you snooze). Strict privacy: LAN-only, no cloud. | none | M |
| L-H3 | Kid-friendly green-light mode. | [OK to Wake](https://www.amazon.com/dp/B003O15A1G), [Hatch](https://www.hatch.co/) | M |
| L-H4 | Pet-feeding reminder chain on dismiss. | none | S |
| L-H5 | Remote parental alarm set. | [Google Family Link](https://families.google.com/familylink/) | L |
| L-H6 | Synchronized alarm groups, edit one, propagate to siblings sharing a label. **NEW.** | [BlackyHawky Clock 2.29](https://github.com/BlackyHawky/Clock/releases) | M |

### Habit / routine integration

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-R1 | Gratitude / journal prompt on dismiss. | [Day One](https://dayoneapp.com/), [Stoic](https://www.getstoic.com/) | S |
| L-R2 | Water-intake quick-log tiles. | [WaterMinder](https://waterminder.com/) | S |
| L-R3 | Mood selfie + emoji tag. | [Daylio](https://daylio.net/) | S |
| L-R4 | Obsidian / Notion / Markdown daily-note append. | [TaskForge.md](https://taskforge.md/android/); [Notelert Obsidian forum](https://forum.obsidian.md/t/notelert-native-android-notification-and-reminders-for-obsidian/109310) | M |
| L-R5 | Health Connect weight / BP / mood quick-entry. | [Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types) | S |
| L-R7 | Badge set: "5 AM club", "no-snooze week", "DDNNO survivor". | [Habitica](https://habitica.com/) | S |
| L-R8 | Share-card screenshot generator (local, no social-feed; matches REJECTED stance). | [Strava](https://www.strava.com/) | S |

### Audio depth

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-A1 | Binaural / isochronic delta (0.5-4 Hz) tone generator. | [Brain.fm](https://brain.fm/), [myNoise](https://mynoise.net/) | M |
| L-A2 | Mathematical-noise synth (brown / pink / violet). | [myNoise](https://mynoise.net/) | S |
| L-A3 | Voice-memo ringtone (in-app 30 s recorder). | iOS-native pattern | S |
| L-A4 | Podcast latest-episode (Podcast Index / AntennaPod URI). | [AntennaPod](https://github.com/AntennaPod/AntennaPod); [AntennaPod alarm-clock feature request](https://forum.antennapod.org/t/alarmclock-function-in-anthennapod/4418) | M |
| L-A5 | Per-alarm Bluetooth sink (specific A2DP / LE Audio device). | [BlackyHawky Clock 2.22 BT routing](https://github.com/BlackyHawky/Clock/releases) | M |
| L-A6 | Chromecast / Nest Hub alarm target. | [Cast SDK](https://developers.google.com/cast/docs/android_sender) | M |
| L-A7 | UPnP / DLNA multi-room cast escalation. | [Cling](https://github.com/4thline/cling) | L |
| L-A8 | Folder-based ringtone import, point at a directory, expose its files in the picker. **NEW.** | [BlackyHawky Clock 2.23](https://github.com/BlackyHawky/Clock/releases) | S |
| L-A9 | System-ringtone preview button parity with the YouTube preview row. **NEW.** | local: [RingtonePickerSheet.kt](app/src/main/java/com/sysadmindoc/alarmclock/ui/ringtone/RingtonePickerSheet.kt) | S |
| L-A10 | Pre-alarm low-volume gentle wake, separate alarm 30 min before main alarm, designed to lift you out of deep sleep. **NEW.** | [yuriykulikov/AlarmClock](https://github.com/yuriykulikov/AlarmClock) signature feature | M |

### Advanced scheduling

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-D1 | Islamic prayer-time Fajr alarm via Aladhan. | [Aladhan API](https://aladhan.com/prayer-times-api); [Al-Azan](https://f-droid.org/packages/com.github.meypod.al_azan/) | M |
| L-D2 | Lunar / Hebrew / Hindu calendar repeat. | none | M |
| L-D3 | Astronomical events (meteor-shower peak, ISS flyover). | [Heavens-Above](https://www.heavens-above.com/) | M |
| L-D4 | Birthday auto-alarm from Contacts. | Android Contacts provider | S |
| L-D5 | Menstrual-cycle aware (softer alarm in luteal phase). | [Health Connect MenstruationFlowRecord](https://developer.android.com/reference/androidx/health/connect/client/records/MenstruationFlowRecord) | M |
| L-D6 | Weather-conditional firing (fire earlier on snow > 2 cm). | [Open-Meteo](https://open-meteo.com/) | M |
| L-D7 | Calendar OOO-aware "skip tomorrow?" suggestion. **NEW.** | inferred from existing CalendarRepository + holiday skip patterns | S |

### Power / reliability

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-P3 | Emergency-escalation call tree (SMS → call → partner → siren). | [Twilio](https://www.twilio.com/) / native | M |
| L-P4 | Location-based escalation (still at home after dismiss → siren). | FusedLocation; partial in code via `locationDismissEnabled` fields | M |
| L-P5 | Car-mode suppression (Android Auto `CarConnection` API; receive Google's new in-car alarm pop-up). | [Android Auto](https://developer.android.com/training/cars); [Android Auto in-car alarm controls 16.8](https://www.autoevolution.com/news/android-auto-is-getting-the-feature-users-first-asked-for-10-years-ago-269408.html) | S |
| L-P6 | Companion-watch autonomous fire if phone battery dies. | none | M |
| L-P7 | Charging-only alarm variant. | none | S |

### Cloud / sync

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-C1 | Google Drive / Nextcloud / WebDAV backup via SAF (opt-in; encryption already exists). | [SAF docs](https://developer.android.com/guide/topics/providers/document-provider); [SeedVault](https://nlnet.nl/project/SeedVault-Integrity/) for inspiration | M |
| L-C2 | End-to-end encrypted paired-phone LAN sync. | none | L |

### Smart home

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-SH1 | Matter 1.6 Dynamic Lighting (DLE) cross-brand sunrise, extends Hue path to any Matter bulb without per-brand workarounds. **NEW.** | [Matter 1.6 DLE 2026](https://mattressmiracle.ca/blogs/mattress-miracle-blog/matter-1-6-dynamic-lighting-sunrise-gradient-bedroom); [Matter Innovations CES 2026](https://matter-smarthome.de/en/products/the-matter-innovations-at-ces-2026/); [Google Home Matter dev docs](https://developers.home.google.com/matter) | L |

### UX polish

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-U1 | Always-On Display-aware Night Clock (uses AOD API rather than full-bright service). | [Android AOD docs](https://developer.android.com/training/wearables/watch-faces/ambient-mode) | S |
| L-U2 | Dynamic color from a specific wallpaper accent rather than the full palette. | none | S |
| L-U3 | Interactive onboarding walkthrough (per-feature highlights). | none | M |
| L-U4 | Predictive-back progress on alarm-edit unsaved-changes dialog (`PredictiveBackHandler`). | [Compose predictive back](https://developer.android.com/develop/ui/compose/system/predictive-back) | S |
| L-U5 | Per-app language picker (`LocaleManager`). Prereq for community translation. | [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages) | S |
| L-U6 | Ultra-HDR sunrise rendering on Android 14+. | [Ultra HDR rendering](https://developer.android.com/about/versions/14/features#ultra-hdr) | S |
| L-U7 | Credential Manager + passkey-gated cloud backup. | [Credential Manager](https://developer.android.com/training/sign-in/passkeys) | M |
| L-U8 | Roman-numeral / additional analog Night Clock face styles. **NEW.** | [BlackyHawky Clock 2.29](https://github.com/BlackyHawky/Clock/releases) | S |

### Accessibility

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-X1 | Screen-flash + camera-flash patterns for deaf users. | [Apple Flash for Alerts](https://support.apple.com/guide/iphone/turn-on-and-customize-led-flash-iph6f30aa5fc/ios); [Android sound notifications](https://support.google.com/accessibility/android/answer/9286728) | S |
| L-X3 | TalkBack audit, large double-tap buttons on firing screen. | [Android accessibility overview](https://support.google.com/accessibility/android/answer/6006564) | S |
| L-X4 | Pure-black / mono-color WCAG AAA high-contrast theme. | [WCAG 2.2 / 2.1 AAA](https://www.w3.org/WAI/WCAG22/quickref/) | S |
| L-X6 | Per-user long-press thresholds on challenge buttons. | Android a11y guidelines | S |

### Documentation

No actionable documentation backlog items remain in this section.

---

## UNDER CONSIDERATION

Items that need scoping or platform readiness before they earn a tier.

| Item | Blocker / scoping question |
|------|---------------------------|
| Android Auto in-car alarm pop-up handler | Wait for Android Auto 16.8 stable release + AAOS API documentation. Currently leaked only via beta teardowns. ([autoevolution](https://www.autoevolution.com/news/android-auto-is-getting-the-feature-users-first-asked-for-10-years-ago-269408.html)) |
| iOS-26 AlarmKit UX pattern adoption (full-screen snooze/stop visuals, App-Intent secondary action) | Study-only, App Intents are iOS-only; port the platform-neutral visual + interaction patterns to ACX firing screen. ([Apple AlarmKit](https://developer.apple.com/documentation/AlarmKit)) |
| Tasker / MacroDroid plugin (true plugin, not just webhook) | Adds API surface to maintain; webhook covers most users. ([Tasker plugin intro](https://tasker.joaoapps.com/plugins-intro.html)) |
| Wear OS standalone app (L-W4) | Build-time, signing, separate Play track; revisit after X1 (complication) proves demand. |
| Cloud LLM sleep-coach | Out of bounds, privacy stance forbids. Local LLM (L-S9) only. |
| Open-Meteo MTG high-resolution solar data | Wait for general availability of the MTG endpoint beyond DWD's Feb 2026 EU/AF launch. ([Open-Meteo seasonal forecast update](https://openmeteo.substack.com/p/seasonal-weather-forecasts)) |
| Custom typeface support per alarm / per app | UX/typography churn risk; revisit when M3 Expressive stabilizes (post v1.13 X17). ([BlackyHawky Clock 2.28](https://github.com/BlackyHawky/Clock/releases)) |
| KMP / Compose-Multiplatform extraction of `NextAlarmCalculator` + `ChallengeGenerator` | Strategic for a future desktop/web Stats companion; L effort, low immediate impact. Defer until at least one cross-platform consumer is concrete. ([Compose Multiplatform 1.11](https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html)) |

## REJECTED: explicit and indefinite

| Item | Reason |
|------|--------|
| Firebase / GA4 / any analytics SDK | Differentiator: "no tracking, no accounts, no data leaves your device." |
| Ad-supported free tier | Same. The app is and will remain ad-free. |
| Public streak / social feed sharing | Privacy trade-off not worth it. Local share-card (L-R8) is the substitute. |
| Sleep-coaching subscription | We remain open-source / donation-based. |
| Collaborative cloud-shared alarms (Ultimate Alarm Clock pattern) | Requires accounts + cloud storage. **NEW.** Local LAN-sync (L-H2) is the boundary we'll consider. ([CCExtractor/ultimate_alarm_clock](https://github.com/CCExtractor/ultimate_alarm_clock)) |
| Anti-uninstall accessibility-service trick (Alarmy "prevent turn off") | Abuse of AccessibilityService; Play policy violation; antithetical to user control. **NEW.** ([Alarmy review on JustUseApp](https://justuseapp.com/en/app/1163786766/alarmy-morning-alarm-clock/reviews)) |
| YouTube alarm-source as a generic feature in F-Droid flavor | Licensing grey zone, `play` flavor only. F-Droid build keeps the strip-out for unencumbered distribution. |
| Cloud LLM for sleep insights | Same privacy stance; only on-device models considered (and only if they fit the APK budget). |
| Power-off alarm without OEM cooperation | Requires privileged partner programs unavailable to indie apps. L-P1 is blocked in `Roadmap_Blocked.md`; non-OEM workarounds remain rejected. |

---

## Cross-cutting tracks (audited every release)

### Platform compatibility

- **`USE_EXACT_ALARM` (install-time grant) instead of `SCHEDULE_EXACT_ALARM` (runtime).** ACX is alarm-clock-category, verify manifest each release. ([FossifyOrg/Calendar #217](https://github.com/FossifyOrg/Calendar/issues/217))
- **Try-catch every `AlarmManager.set*` call.** `setInexactAllowWhileIdle` can still throw if the device's exact-alarm fallback path engages. ([flutter_local_notifications #2248](https://github.com/MaikuB/flutter_local_notifications/issues/2248))
- **Android 15 short-type FGS auto-timeout (3 min cap).** Stay on `mediaPlayback` type, do NOT migrate to `shortService`. ([Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15))
- **Doze defers even `setAlarmClock()` 1-2 min on Redmi/Samsung.** Pair with a 10-15 s `PARTIAL_WAKE_LOCK` in `onReceive`; keep within ANR ceiling. ([Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby))
- **`setAlarmClock()` always shows status-bar icon.** Already mitigated with a settings toggle that falls back to `setExactAndAllowWhileIdle` (with disclaimer); keep the toggle in the UI.
- **`READ_CALENDAR` runtime denial.** `CalendarAutoAlarmWorker` must early-return on denial. Verify each release.
- **`Configuration.Provider` + manifest initializer removal.** WorkManager + Hilt regression vector; CI check exists, keep it.
- **Android 16 "missed alarm, unknown reason" notification regression on Pixel.** Track the QPR fix and confirm ACX's foreground-service start path is not the cause. ([Android Police Pixel alarm bug](https://www.androidpolice.com/pixel-alarm-bug-is-back/))
- **Play wake-lock policy (March 2026).** N4 covers the audit; keep the wake-lock acquisition window inside the 2 h / 24 h non-exempt budget. ([9to5Google March 2026](https://9to5google.com/2026/03/05/google-starts-calling-out-android-apps-that-drain-your-battery-before-you-download-them/))

### Security / privacy

- AES-256-GCM + PBKDF2-HMAC-SHA256 (200k iters) for backup encryption, shipped 1.5.x. Audit iteration count yearly against [OWASP Password Storage cheat sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). Consider Argon2id when JNI dependency budget allows.
- Shareable-alarm import is **disabled by default** until reviewed, keep that. Never silently schedule a received link's alarm.
- Hue v1 username endpoints are deprecated, **migrate `HueSunriseWorker` to v2 `application_key` + HTTPS pinning. Tracked as N5 above.** ([Philips Hue API v2](https://developers.meethue.com/new-hue-api/))
- Webhook URL is user-supplied and never auto-validated, document this as part of the threat model rather than retrofitting validation that won't catch a determined misuse.

### Observability

- Crash logger writes to local files only; we don't ship a remote sink and won't (privacy). Ensure rotation cap remains in place so a runaway loop can't fill storage.
- Add a "share crash log" button on the About screen (does not auto-upload, copies to clipboard or invokes share sheet). **S, not yet tiered.**
- App Standby bucket surfaced in the Reliability Settings group (N3) doubles as observability for the user.

### Distribution / packaging

- Two flavors today: `play` (with YT downloader + Wear Data Layer), `fdroid` (without). Maintain parity on every other surface. Build, test, signing, OSV audit, release artifact creation, and SHA-256 generation happen locally; do not add GitHub Actions.
- F-Droid lint passes, anti-feature flag for the YT downloader is documented in `metadata/`. Re-verify on each release. Crash-log local-file disclosure is documented in README and F-Droid metadata.
- AAB for Play Store, signed APK for GitHub Releases; never ship unsigned artifacts.
- F-Droid users expect APK under **~40 MB**. Any TFLite-model or Matter-SDK work must respect this budget (downloadable models, not bundled).

### i18n / l10n

- English-only today. **Per-app language picker (L-U5) lands first**, THEN community translation. No machine-translation-only strings, better to remain English than ship broken translations.
- `Configuration` change tests when M3 Expressive + per-app locale stack: confirm `Compose` recomposes correctly via `LocalConfiguration`.

### Testing

- Unit tests cover: `NextAlarmCalculator`, `VacationAlarmPolicy`, `MissedAlarmReplayPolicy`, `ProximityCoverDetector`, `AlarmShareCodec`, `EncryptedBackupCodec`, `WakeStreakCalculator`, `WebhookUrl`, `ChallengeGenerator` + maze solver, `StatsFilters`, `NextAlarmNotificationTiming`. **Each new dismiss challenge must come with a unit-tested "valid input" + "invalid input" suite.**
- Room migration tests: every schema bump requires a migration test path in `AlarmDatabaseMigrationTest`; CI also runs `git diff --exit-code -- app/schemas` after debug builds to catch uncommitted exports (whakaara discipline, [ahudson20/whakaara](https://github.com/ahudson20/whakaara)).
- Remaining alarm-fire proof gap: add a device/emulator smoke that fires through AlarmManager/test broadcast and asserts the firing window shows over lock screen. **S, not yet tiered.**
- Add a `sanitized()` round-trip property test that asserts every value in `ChallengeType.entries.map(Enum::name)` is preserved through `Alarm.sanitized()`. Directly prevents the N1 class of regression in the future.

### Documentation

- README, CHANGELOG, ROADMAP, and the version badge must all match on every release. **N10 makes this enforced in CI instead of manual.**
- Add a CONTRIBUTING.md (currently absent), blocked by current markdown hygiene until the repository permits that file.

### Plugin ecosystem

- Webhooks (Tasker / MacroDroid / Home Assistant) cover the integration surface we want to expose. A "real" plugin SDK is rejected (UC) until webhook gaps are documented.
- Recipe library (L-R6 + L-SH2), blocked by current markdown hygiene until the repository permits integration docs.

---

## Research sources (round 5, refreshed 2026-05-16)

### Direct OSS competitors

- **yuriykulikov/AlarmClock**: https://github.com/yuriykulikov/AlarmClock, 612★, AOSP-derived. Signature feature: pre-alarm low-volume gentle wake (L-A10); long-press dismiss; adjustable snooze picker.
- **FossifyOrg/Clock**: https://github.com/FossifyOrg/Clock, beta 1.6.0 (Feb 2026). Switches replacing checkboxes, "About" back in options menu, Android 7 support dropped.
- **BlackyHawky/Clock**: https://github.com/BlackyHawky/Clock, v2.29 (Apr 2026), v2.30 in nightly. Per-version harvest applied to this roadmap pass: pause-alarms (N6), manual drag-reorder (X15), sync alarms (L-H6), Direct-Boot fallback (shipped v1.15.2), BT routing (L-A5), folder ringtones (L-A8), per-alarm background (X14), vibration delay (N7), missed-timer notif (N8), ExoPlayer (X16), custom fonts (UC).
- **LineageOS DeskClock**: https://github.com/LineageOS/android_packages_apps_DeskClock
- **AOSP DeskClock**: https://android.googlesource.com/platform/packages/apps/DeskClock/, gold-standard alarm state machine.
- **ahudson20/whakaara**: https://github.com/ahudson20/whakaara, 51★ (May 2026). Reference for Room migration discipline + Kover code-coverage workflow.
- **yassineAbou/Clock**: https://github.com/yassineAbou/Clock, pure-Compose, single-activity, WorkManager-backed timer/stopwatch persistence.
- **fennifith/Alarmio**: https://github.com/fennifith/Alarmio
- **akshay2211/JetAlarm**: https://github.com/akshay2211/JetAlarm
- **CCExtractor/ultimate_alarm_clock**: https://github.com/CCExtractor/ultimate_alarm_clock (108★, Flutter. Shared cloud alarms (REJECTED) H19), QR-scan dismiss, weather-based alarm.
- **sweakpl/qralarm-android**: https://github.com/sweakpl/qralarm-android, 323★, v2.9.3 (May 2026). Single-purpose QR dismiss.
- **WrichikBasu/ShakeAlarmClock**: https://github.com/WrichikBasu/ShakeAlarmClock
- **meenbeese/Chronos**: https://github.com/meenbeese/Chronos
- **meticha/triggerx**: https://github.com/meticha/triggerx, alarm-execution library, ~101★.
- **lemma-io/vivify**: https://github.com/lemma-io/vivify, open-source Spotify-connected alarm reference for L-A.
- **plusmobileapps/alarm-clock**: https://github.com/plusmobileapps/alarm-clock
- **vicolo-dev/chrono**: https://github.com/vicolo-dev/chrono, Flutter UX study target.
- **kunal-mahatha/Early-Bird-App**: https://github.com/kunal-mahatha/Early-Bird-App
- **giorgosneokleous93/fullscreenintentexample**: https://github.com/giorgosneokleous93/fullscreenintentexample

### Commercial reference

- **Alarmy**: https://alar.my/en/blog/alarmy-wake-up-mission (Multiple Mission feature is parity for our Mission Chain. Photo, Math, Shake, Barcode/QR, Memory, Typing, Steps, Squats (premium). Wake-Up Check feature is paywalled) ACX matches free via existing F5 / N1.
- **Sleep as Android**: https://sleep.urbandroid.org/documentation/release-notes/, 2025 additions: Google Home API (BETA), AI Sleep Assistant (BETA), HRV gain cards, dashboard redesign, wake-up-check automation, Lullabies addon. AI sound detection: https://sleep.urbandroid.org/new-sleep-sound-detection/
- **Sleep Cycle**: https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk, 2026 SDK release; phone mic + accelerometer detects sleep stages and fires alarm in lightest phase within wake window. Algorithm reference for smart-wake logic.
- **Rise**: https://www.risescience.com/, sleep-debt accumulator + composite score reference for X4 / X5.
- **Pillow**: https://www.pillow.app/, actigraphy reference (iOS-only).
- **Turbo Alarm**: https://play.google.com/store/apps/details?id=com.turbo.alarm, Spotify-as-alarm, Wear OS support, talking alarm, sunrise simulation, mini-game dismiss, "Anti-Sleepyhead Security" (L-P10), cloud-sync, Tasker / Macrodroid / Sleepbot integration.
- **Google Clock**: https://play.google.com/store/apps/details?id=com.google.android.deskclock, Pixel-exclusive Sunrise Alarm + Bedtime tab reference.
- **I Can't Wake Up**: Simon-says and voice-phrase reference; voice phrase shipped in v1.15.3.
- **Timeshifter**: https://www.timeshifter.com/, jet-lag re-entrainment reference for L-WS2.
- **Supershift**: https://supershift.app/, shift-pattern reference for L-WS1 (DDNNO / 4-on-4-off / Panama / DuPont / Pitman).
- **Pixel Bedtime mode**: https://support.google.com/pixelphone/answer/9887159, L-S11 reference.
- **Apple AlarmKit (iOS 26 WWDC25)**: https://developer.apple.com/documentation/AlarmKit, cross-platform UX-pattern study (UC).

### Awesome lists / FOSS catalogs

- GitHub topics: https://github.com/topics/alarm-clock?l=kotlin and https://github.com/topics/sleep-tracker
- F-Droid Clocks & Alarms: https://f-droid.org/en/categories/clock/
- IATkachenko/HA-SleepAsAndroid (Home Assistant integration): https://github.com/IATkachenko/HA-SleepAsAndroid
- XADE awesome-android: https://codeberg.org/XADE/awesome-android
- binaryshrey/Awesome-Android-Open-Source-Projects: https://github.com/binaryshrey/Awesome-Android-Open-Source-Projects

### Platform docs / standards / specs

- Android 14 behavior changes: https://developer.android.com/about/versions/14/behavior-changes-14
- Android 15 behavior changes: https://developer.android.com/about/versions/15/behavior-changes-15
- Android 15 features: https://developer.android.com/about/versions/15/features
- Android 16 features: https://developer.android.com/about/versions/16/features
- Android 16 Live Updates / `ProgressStyle`: https://developer.android.com/about/versions/16/features/progress-centric-notifications
- Android 16 article (Wikipedia, install-base): https://en.wikipedia.org/wiki/Android_16
- Android 17 Beta 3 release notes: https://developer.android.com/about/versions/17/release-notes
- Material 3 Expressive: https://m3.material.io/blog/material-3-expressive
- Compose Material 3: https://developer.android.com/jetpack/androidx/releases/compose-material3
- Compose Material 3 Adaptive: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- NavigationSuiteScaffold: https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- Wear OS Tiles API: https://developer.android.com/training/wearables/tiles
- Wear OS Complications API: https://developer.android.com/training/wearables/complications
- Glance: https://developer.android.com/jetpack/androidx/releases/glance
- Glance Wear: https://developer.android.com/jetpack/androidx/releases/glance-wear
- Health Connect Sleep: https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions
- Health Connect Develop Sleep Experiences: https://developer.android.com/health-and-fitness/health-connect/experiences/sleep
- Health Connect get-started: https://developer.android.com/health-and-fitness/health-connect/get-started
- Play Console health permissions FAQ: https://support.google.com/googleplay/android-developer/answer/12991134?hl=en
- Play Console policy April 15 2026: https://support.google.com/googleplay/android-developer/answer/16926792?hl=en
- ML Kit Digital Ink: https://developers.google.com/ml-kit/vision/digital-ink-recognition
- LE Audio (Android 13+): https://source.android.com/docs/core/connect/bluetooth/le_audio
- BLE Audio overview: https://developer.android.com/develop/connectivity/bluetooth/ble-audio/overview
- AutomaticZenRule v2: https://developer.android.com/reference/android/app/AutomaticZenRule
- Predictive back: https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture
- Compose Predictive Back: https://developer.android.com/develop/ui/compose/system/predictive-back
- LocaleManager: https://developer.android.com/about/versions/13/features/app-languages
- Credential Manager: https://developer.android.com/training/sign-in/passkeys
- `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`: https://developer.android.com/reference/android/app/AlarmManager#ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
- App Standby Buckets: https://developer.android.com/topic/performance/appstandby
- Optimize for Doze and App Standby: https://developer.android.com/training/monitoring-device-state/doze-standby
- Telephony state (`EXTRA_STATE_RINGING`): https://developer.android.com/reference/android/telephony/TelephonyManager#EXTRA_STATE_RINGING
- Direct Boot: https://developer.android.com/about/versions/14/direct-boot
- Open-Meteo Weather: https://open-meteo.com/
- Open-Meteo Air Quality + pollen: https://open-meteo.com/en/docs/air-quality-api
- Open-Meteo seasonal forecast 2026: https://openmeteo.substack.com/p/seasonal-weather-forecasts
- NWS Active Alerts: https://www.weather.gov/documentation/services-web-api
- Nager.Date holidays: https://date.nager.at/
- Aladhan prayer times: https://aladhan.com/prayer-times-api
- Matter 1.6 Dynamic Lighting (sunrise gradient): https://mattressmiracle.ca/blogs/mattress-miracle-blog/matter-1-6-dynamic-lighting-sunrise-gradient-bedroom
- Matter Smart Home (CES 2026): https://matter-smarthome.de/en/products/the-matter-innovations-at-ces-2026/
- Google Home Matter dev docs: https://developers.home.google.com/matter
- Philips Hue API v2: https://developers.meethue.com/new-hue-api/
- Apple AlarmKit: https://developer.apple.com/documentation/AlarmKit

### Academic / industry / engineering

- Cole-Kripke 1992: https://pubmed.ncbi.nlm.nih.gov/1455130/
- Roenneberg MEQ: https://www.thewep.org/documentations/mctq
- Horne-Östberg MEQ calculator: https://qxmd.com/calculate/calculator_829/morningness-eveningness-questionnaire-meq
- Springer 2025 (smartwatch IMU OSA): https://link.springer.com/article/10.1007/s11325-025-03255-w
- Apneal 2025 (smartphone OSA prediction): https://link.springer.com/article/10.1007/s11325-025-03441-w
- Samsung × Stanford OSA collab 2025: https://www.samsungmobilepress.com/articles/samsung-announces-collaboration-with-stanford-medicine-to-advance-sleep-apnea-detection-and-beyond
- AASM smartwatch sleep features comparison: https://aasm.org/comparing-sleep-features-of-popular-smartwatches/
- Sleep Cycle SDK announcement: https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk
- Smart alarm tinyML (sleep-stage prediction in embedded systems): https://github.com/cargilgar/Smart-Alarm-using-tinyML
- Smart alarm based on sleep stages prediction (IEEE 2020): https://ieeexplore.ieee.org/document/9176320/
- SlumberNet (Nature Sci. Reports 2024): https://www.nature.com/articles/s41598-024-54727-0
- Edge Impulse snoring on smartphone: https://github.com/edgeimpulse/expert-projects/blob/main/audio-projects/snoring-detection-on-smartphone.md

### Community signal

- r/Android complaints (recurring): missed alarms on Xiaomi/Samsung/Oppo battery-management; subscription fatigue (Alarmy / Sleep as Android Premium); Google Clock missing skip-one-occurrence + mission challenges; Pixel "missed alarm (unknown reason" notification regression (Android 16). Sources: [howtogeek.com) Pixel alarms keep breaking](https://www.howtogeek.com/google-pixel-phone-alarm-app-not-working-again/); [androidpolice.com, Pixel alarm bug is back](https://www.androidpolice.com/pixel-alarm-bug-is-back/); [TechRadar, fix Android alarm clock bug](https://www.techradar.com/how-to/how-to-fix-the-android-alarm-clock-bug-so-you-wake-up-on-time)
- Maker complaints (Hacker News / accessibleandroid.com): mini-game dismiss is poorly TalkBacked across the field; Turbo Alarm called out specifically. Accessibility-first dismiss alternatives (haptic, voice, screen-flash) are differentiators.
- dontkillmyapp.com (https://dontkillmyapp.com/) per-OEM background-execution guidance still actively updated 2026.
- Privacy Guides community (Alarmy permissions discussion) https://discuss.privacyguides.net/t/can-i-mitigate-some-of-the-privacy-issues-of-the-android-app-alarmy-by-removing-network-permission/24492

### Library changelogs to mine each release

- `androidx.work:work-runtime-ktx`: https://developer.android.com/jetpack/androidx/releases/work
- `androidx.glance:glance-appwidget`: https://developer.android.com/jetpack/androidx/releases/glance
- `androidx.glance:glance-wear-tiles`: https://developer.android.com/jetpack/androidx/releases/glance-wear
- `androidx.compose.material3`: https://developer.android.com/jetpack/androidx/releases/compose-material3
- `androidx.compose.material3.adaptive`: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- `androidx.health.connect:connect-client`: https://developer.android.com/jetpack/androidx/releases/health-connect
- `com.patrykandpatrick.vico`: https://github.com/patrykandpatrick/vico/releases
- yt-dlp: https://github.com/yt-dlp/yt-dlp/releases
- NewPipeExtractor: https://github.com/TeamNewPipe/NewPipeExtractor/releases
- OkHttp / Retrofit / Moshi / Hilt / Room, keep current via quarterly audit.

### Legal / compliance flags to budget before touching

- Health Connect (N12 / X1), code and privacy policy now describe Play-only `READ_SLEEP`; Play Console health-permissions declaration/approval must still precede Play Store distribution. ([Play Console policy](https://support.google.com/googleplay/android-developer/answer/16926792?hl=en))
- Apnea event flagging (L-S7), explicit "screening, not a medical device" disclaimer; consider keeping it `play`-flavor only for legal hygiene.
- Power-off alarm (L-P1), per-OEM privileged partner programs; may never be achievable for an indie app.
- Partner-phone / paired-phone sync (L-H2 / L-C2), explicit threat model doc before code.
- LLM sleep-coach (L-S9), bundled model size budget; F-Droid users expect <40 MB APK.
- Matter SDK (L-SH1), adds dependency surface; verify F-Droid compatibility (Google Play Services-free build path).
- Wake-lock budget (N4), Play Store quality treatment policy (March 2026). ([9to5Google](https://9to5google.com/2026/03/05/google-starts-calling-out-android-apps-that-drain-your-battery-before-you-download-them/))

---

*Roadmap owners: add yourself as assignee when picking up an item.
Prefer one-item-per-PR for the S-effort work and phased delivery for
M / L. Update this file on every release alongside CHANGELOG.md.*

---

## Research-Driven Additions (2026-07-12)

New findings from the 2026-07-12 pass (code audit + ecosystem scan). Deduplicated
against all existing ROADMAP.md and Roadmap_Blocked.md items. The prior pass's top
five (signed webhooks, backup import preview, cached stale weather/news, adaptive
wide layouts, AlarmService controller extraction) are done and removed. See
RESEARCH.md for evidence detail.

### P1: Reliability / correctness


### P2: Accessibility / platform / polish

## Research-Driven Additions: Pass 2 (2026-07-12, subsystem audit)

Second 2026-07-12 pass auditing the timer/stopwatch/Sonar/news/restore
subsystems the prior pass skipped, plus net-new ecosystem opportunities.
Deduplicated against all existing ROADMAP.md / Roadmap_Blocked.md items and
against already-shipped features (mission chaining, wake-confirm, holiday
auto-skip, vibrate-only). See RESEARCH.md for evidence detail.

### P1: Reliability / correctness / data-safety

### P2: Correctness / reliability / platform





### P3: Nice-to-have / polish / hygiene

## Research-Driven Additions: Pass 3 (2026-07-12, quality/i18n/performance)

Third 2026-07-12 pass covering the cross-cutting dimensions the prior two
(alarm-firing correctness; subsystem reliability) under-weighted. Both items are
verified against current code and deduplicated against L-U5 (per-app-language
picker), the i18n cross-cutting note, and the blocked Baseline-Profile item.

## Research-Driven Additions

### P0: Now

### P1: Next

### P2: Later

### P3: Under Consideration

## Research-Driven Additions: Pass 4 (2026-07-22, post-v1.15.30 reliability & platform)

All 2026-07-14 RESEARCH.md findings are now fixed (verified against live code
2026-07-22); this pass is grounded in fresh competitor/platform/community
research and current-code verification. Deduplicated against every prior ROADMAP
and Roadmap_Blocked item. Full evidence in RESEARCH.md.

### P2: reliability / platform / UX

- [ ] P2: Media3 alarm-audio stall detection
  Why: the Media3 ring path has no stall/timeout detection, so a stalled ring
  relies only on the delayed backup-sound escalation to recover.
  Evidence: Media3 1.9 `StuckPlayerException` + stalled-ready timeouts
  (developer.android.com/jetpack/androidx/releases/media3); ACX on Media3 1.10.1.
  Touches: `service/AlarmService.kt` audio path, `service/AlarmAudioRouting.kt`.
  Acceptance: a stalled/failed player is detected within a bounded window and
  escalates immediately (built-in speaker + max volume, then legacy fallback)
  rather than waiting for the backup-sound timer; incident reason code recorded.
  Complexity: M.

- [ ] P2: Snooze to a specific time (scheduled snooze)
  Why: snooze is fixed-interval + progressive only; users want to re-fire at a
  chosen clock time (e.g. "again at 07:15").
  Evidence: yuriykulikov/AlarmClock; vicolo-dev/chrono.
  Touches: `service/AlarmService.kt` snooze path, `ui/alarmfiring/AlarmFiringActivity.kt`.
  Acceptance: the firing screen offers a "snooze until…" time picker that arms an
  exact re-fire at the chosen time; round-trips through the existing snooze
  scheduling and survives process death.
  Complexity: M.

- [ ] P2: Extend Live Updates (ProgressStyle) to the snooze countdown
  Why: ACX already uses Android 16 `Notification.ProgressStyle` for the bedtime
  countdown only; the snooze interval is an ideal second start-to-end journey.
  Evidence: developer.android.com/about/versions/16/features/progress-centric-notifications.
  Touches: snooze notification path in `service/AlarmService.kt`, notification builders.
  Acceptance: while snoozed, an ongoing progress notification shows time-until-
  re-fire; clears on re-fire/dismiss; gated to API 36+ with graceful fallback.
  Complexity: M.

- [ ] P2: OEM reliability doctor (per-manufacturer deep-links + post-OTA re-check)
  Why: OEM Doze/autostart kills are the #1 real-world missed-alarm cause; ACX
  surfaces wake-readiness but not per-OEM autostart/battery deep-links or a
  re-prompt after an OTA silently resets permissions.
  Evidence: dontkillmyapp.com; github.com/WrichikBasu/ShakeAlarmClock/discussions/61.
  Touches: wake-readiness settings group, a small per-OEM intent map, an
  OTA/build-fingerprint change detector.
  Acceptance: on Xiaomi/Samsung/Oppo/Vivo/OnePlus/Realme the readiness card deep-
  links to the correct autostart/battery screen; a detected OS build-fingerprint
  change re-surfaces the reliability checklist. Tradeoff (maintenance burden of
  per-OEM intents) accepted and documented inline.
  Complexity: M.

### P3: polish / UX

- [ ] P3: Reduce ring volume while solving a dismiss challenge (opt-in)
  Why: a lower ring during a math/typing/maze mission lets users concentrate;
  Media3 1.10 `mute()`/`unmute()` is now stable, making it cheap.
  Evidence: vicolo-dev/chrono; Media3 1.10 (developer.android.com/jetpack/androidx/releases/media3).
  Touches: `ui/alarmfiring/AlarmFiringActivity.kt`, `service/AlarmService.kt`.
  Acceptance: an opt-in per-alarm/global toggle drops ring volume while a
  challenge is active and restores it on solve/fail; the backup-sound escalation
  still fires so a user cannot fall back asleep in silence. Default off.
  Complexity: S.

- [ ] P3: Random ringtone start position
  Why: starting a ringtone at a random offset each fire keeps long-time users
  from habituating to the same opening seconds.
  Evidence: vicolo-dev/chrono.
  Touches: `service/AlarmService.kt` startAudio, per-alarm setting.
  Acceptance: an opt-in per-alarm flag seeks the ring player to a random valid
  offset at fire time; ignored for streams/short tones; round-trips through backup.
  Complexity: S.
```

</details>
