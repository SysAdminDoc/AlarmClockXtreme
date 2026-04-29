# Changelog

All notable changes to AlarmClockXtreme will be documented in this file.

## [1.9.0] - 2026-04-29

The Today tab is alive. The screen background now renders the actual sky
above your location — interpolated minute-by-minute through a 15-keyframe
table anchored to real sunrise / sunset — and reacts to current weather:
storms swap to overcast blue-gray with lightning flashes at night, and
NWS tornado warnings paint a rotating funnel-cloud silhouette plus a red
warning banner.

### Added — `TimeOfDaySky` engine

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

### Added — Weather overrides

- `WeatherSkyOverrides.STORM_DAY` (gray-blue overcast) and `STORM_NIGHT`
  (near-black) bypass the time-of-day table when the current Open-Meteo
  weather code is 95-99 (thunderstorm / hail).
- `WeatherSkyOverrides.TORNADO_SKY` — the classic dark-olive ceiling /
  sickly yellow-green horizon — bypasses everything when an active NWS
  tornado warning is detected.

### Added — `WeatherSkyBackground` composable

Stacks five layers behind the Today tab content:

1. **Base sky gradient** (time-of-day or weather override).
2. **Long fade to `SurfaceDark`** so cards below the hero return to the
   app's neutral surface — a vivid sky behind a vivid weather card would
   sap contrast.
3. **Lightning flashes** when the current weather code is a thunderstorm.
   A stochastic 4-9-second loop drives short ramps (60ms up, 220ms decay)
   to white at ~28% alpha; tornado mode boosts the intensity and adds
   ~30%-chance double-strike aftershocks.
4. **Tornado funnel + warning banner** when `tornadoAlertActive` is true.
   The funnel is a Canvas-drawn silhouette with rotation + drift
   animations layered over each other; the banner pins below the status
   bar with a red TORNADO WARNING + cyclone icon.
5. **Actual content** — Today's Column rendered on a transparent column
   so the sky shows through.

### Added — NWS alerts integration

- `WeatherAlertsApi` + `WeatherAlertsRepository` against
  `api.weather.gov/alerts/active`. Free, no key, US-only. Returns empty
  features outside the US, so it's safe to call unconditionally.
- Sends a User-Agent identifying the app + repo URL — required by NWS
  to avoid 403s under their rate-limit policy.
- Distills the response to `WeatherAlertFlags(tornadoActive, severeStorm,
  headline)` — the rest of the app only needs the boolean signal.
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

- **US-only tornado coverage** by design — NWS only issues alerts for
  the United States. International users see the time-of-day sky and
  the storm/lightning visuals; tornado overlay never triggers.
- The keyframe colors were specified by user request and are stored in
  `TimeOfDaySky.KEYFRAMES`. Editing the table (e.g., for a more saturated
  sunset) changes the visual everywhere.

## [1.8.1] - 2026-04-29

Premium-polish pass. No new features, no schema changes — every change in
this release sharpens an interaction or a surface that already worked but
felt rough on close inspection. Driven by a top-to-bottom design audit
(visual hierarchy, component consistency, microcopy, motion, empty/loading
states, accessibility) and verified on a real device.

### Design system

- **`AppIconSize` tokens** (xs=14, sm=18, md=22, lg=32 dp). Replaces the
  ad-hoc 13/15/18/20/22 dp drift that crept across cards, chips, tiles,
  and metric tiles.
- **`AppFilterChip`** primitive that matches `AppStatusChip` geometry —
  same min height (32 dp), same `AppChipShape`, same accent treatment.
  Migrated AlarmList's group filter row + News's feed filter row off raw
  Material `FilterChip` so chip rows hold a single rhythm regardless of
  chip kind.
- **`AppSkeletonBlock`** primitive — a shimmering placeholder block used
  to compose skeleton rows (News list, radar) so first-paint feels
  purposeful instead of presenting a single spinner.

### Bottom navigation (the most visible change)

- `alwaysShowLabel = false`. With six tabs in 1080 px, every label
  truncated ("Weath…" / "Setti…") which read as broken layout. The
  Material 3 idiom for crowded bars is exactly this — the selected tab
  carries its label inside the indicator pill, the rest sit as confident
  icons. The pill becomes the focal affordance.
- "Weather" (7 chars) still got clipped to "Weathe" inside the M3 pill,
  so the tab label is now **Today** (the screen hero still reads
  "Weather"). Pragmatic and accurate — the tab is a daily-overview hub.

### Live radar (Weather)

- **Skeleton + fade-in.** The 360 dp WebView slab used to flash dark for
  1–3 s on cold connections. New `WebViewClient` hooks `onPageStarted` /
  `onPageFinished` to drive a `loaded` flag; a shimmering skeleton fills
  the slot and cross-fades out (240 ms) as the WebView fades in (280 ms).
- "Open in Windy" relocated from a left-aligned `TextButton` under the
  map to a header-aligned `AppStatusChip` that sits next to the title.
  No more orphaned link below a centered map.
- Header retitled "Animated precipitation near $location · Windy" — same
  info, half the words.

### News tab

- **Pull-to-refresh** via Material 3 `PullToRefreshBox` — the canonical
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
- Hero "Updated just now" badge dropped — the relative-time chip with
  the Schedule icon is enough; "Updated" was redundant.

### Microcopy across the app

Every hero subtitle, section description, and empty-state copy went
through a "≤12 words and only what's true" pass.

- Alarms hero (no alarms) "Tap + New alarm to schedule your first."
  (was "Create, group, and refine alarms from one calm control center.")
- AlarmList "Quick alarms" description "Tap a duration to schedule it
  now." (was "Need a short reminder or power nap? Start one with a
  single tap.")
- AlarmList "Groups" description dropped — title alone is clearer.
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
Both follow the existing no-account/no-API-key rule — Windy via its public
embed endpoint, news via plain RSS over OkHttp + Android's built-in
XmlPullParser. No new SDKs.

### Added

- **Live radar on the Weather tab.** New `WindyRadarCard` composable — a
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
  DataStore (`newsFeedUrl`). Each headline renders as a tappable card —
  title, 3-line snippet, source chip, relative-time chip ("58m ago"),
  open-in-new icon. Pull-to-refresh button in the hero actions slot.
  External links open in the system browser via `LocalUriHandler`.
- **`RssParser`** — minimal RSS 2.0 / Atom parser using Android's built-in
  `XmlPullParser`. Skipped Rome (~600 KB JAXB-heavy), kept the dep
  footprint at zero. Handles RFC-822 + ISO-8601 dates, falls back to
  channel title for the source field, defensively skips unknown tags so
  vendor extensions don't kill parsing.
- **Settings**: four new toggles — Show News tab, Live radar on Weather
  tab — plus the renamed "Show Weather tab". Updated supporting text on
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
`Card` (and `AppSurfaceCard`) wraps content height — it doesn't honour
the weight allocation — so on a tall device the area below the wrapped
card stays empty. This release replaces those layouts with scrollable
columns and a manually-positioned FAB so every tab has a consistent,
fully-occupied vertical rhythm.

### Fixed

- **Timer tab — empty space below the hero on devices with no active
  timers.** Switched the parent `Column` to `verticalScroll`, dropped
  the `weight(1f)` on `TimerInputView`, replaced the inner `LazyColumn`
  for active timers with a forEach `Column`. Adds a 24dp Spacer at the
  bottom so the input card breathes above the floating bottom nav.
- **World Clock tab — saved cities not visible despite the "N cities"
  hero chip.** Replaced the inner `Scaffold` (which competed for system
  insets with the outer `AppNavigation` `Scaffold`) with a `Box` that
  hosts the hero + content `Column` and overlays the FAB at
  `BottomEnd`. `LazyColumn` `contentPadding.bottom` set to 96dp so the
  last city card never hides behind the FAB. The hero chip "N cities"
  is now hidden in the empty state for less visual noise.
- **Today tab — duplicate "Now" cells in the hourly strip.** The
  `isFirstFutureSlot` predicate ran a 45-minute window check on every
  cell, so two or three adjacent hours all rendered with the "Now"
  label. Replaced with a single-flag `firstNowAssigned` toggled after
  the first matching cell. (already shipped in 1.7.4 hotfix path,
  consolidated here.)
- **Alarms tab — "Swipe to delete" text bleeding through disabled
  alarm cards.** `AlarmCard` uses `SurfaceCard.copy(alpha = 0.55f)`
  for disabled alarms, so the `SwipeToDismissBox` background (always
  rendered, just transparent when not swiping) showed through any
  disabled foreground. Looked like a stuck swipe gesture. Now the
  delete affordance is gated on `isSwiping = currentValue !=
  Settled || targetValue != Settled` so it only paints during an
  active gesture. Also added a `LaunchedEffect(Unit)` that snaps
  `dismissState` back to `Settled` on first composition — handles
  the rare case where a saved partial-drag offset is restored across
  navigation.

### Polish

- **World Clock hero chip set** trimmed in the empty state — no point
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
  context — answers "is the sun up by my alarm time?" Lifted from
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
- F-droid build is unaffected — Open-Meteo is free and unlicensed.

## [1.7.3] - 2026-04-29

### Changed

- **YouTube downloads now show real progress.** The static spinner +
  "Downloading..." text read as "stuck" in user testing. Replaced
  with a determinate `LinearProgressIndicator` paired with a rotating
  status label ("Resolving audio stream…" → "Connecting to YouTube…"
  → "Downloading audio…" → "Almost there…" → "Saving to your alarms…")
  and a live percentage. The bar follows an asymptotic curve that
  reaches ~30% in the first 4 seconds and crawls toward 92% — the
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
  any result to stream the lowest-bitrate audio (~1–3 s to start, no
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
- Session-only LRU cache of resolved URLs (64 entries, 3-hour TTL —
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

- **Hide bottom-nav tabs** — Settings → Bottom navigation lets you turn
  off Today, Timer, and World individually. Alarms and Settings always
  stay. If you're on a tab you just hid, the app bounces you back to
  Alarms automatically.
- **Search YouTube from the download dialog** — paste a URL or search
  by keyword (NewPipe Extractor; same library Aura uses). Tap a result
  to download. Filters to clips ≤4 minutes so 90-minute reaction
  videos don't crowd the list.
- **Prominent "Download alarm sound from YouTube" card** on the Alarms
  screen — top-level, not buried inside "create new alarm." Build up a
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
  subtitle from the Alarms hero — now reads "Tap an alarm to edit it,
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
  immediately — no extra wiring, because the picker already enumerates
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
- New `YouTubeDownloadInitializer` interface — the play impl unpacks
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
  `libqjs.so` natively, so the play APK grows by ~10–15 MB. F-droid
  stays lean.
- Source: ported from `~/repos/Aura` (`YouTubeRepository.kt` for the
  yt-dlp invocation, `SoundApplier.kt` for the MediaStore write
  pattern). NewPipe Extractor (Aura's search backend) was deliberately
  NOT ported — alarm-sound discovery is a paste-URL UX, not a search.
  FFmpeg post-processing (Aura's trim/fade/normalise pipeline) was
  also skipped; downloaded audio plays as-is.

## [1.6.3] - 2026-04-29

End-to-end engineering audit pass. No new user-facing features; targets
real reliability, security, and consistency bugs found across the
service, scheduler, receiver, and import paths.

### Fixed

- **Webhook firing was racing service tear-down.** Dismiss/snooze events
  were dispatched on `serviceScope.launch`, then `stopSelf()` was called
  immediately after — `onDestroy()` cancelled `serviceScope` before the
  5-second OkHttp call could complete, so Tasker integrations missed the
  "dismissed" / "snoozed" events on slow connections. Webhook calls now
  run on an application-lived `SupervisorJob` scope owned by
  `WebhookService`, so service tear-down can no longer kill them.
- **Snooze-cap event/webhook mismatch.** When the user hit
  `maxSnoozeCount`, the alarm event log persisted `ACTION_DISMISSED` but
  the webhook fired the `"snoozed"` event — same physical action, two
  different stories. The webhook event name is now derived from the
  branch that actually executed.
- **`MissedAlarmUnlockReceiver` ANR risk.** Timeout was 25 seconds on a
  receiver running under `goAsync()`, which only extends the
  BroadcastReceiver ANR window to ~10 s on most Android versions —
  guaranteed ANR before the timeout could fire. Tightened to 8 s,
  matching the v1.5.4 fix already applied to `BootReceiver`.
- **`setAlarmClock` not protected from `SecurityException`.**
  `canScheduleExactAlarms()` was checked upstream, but the permission
  can be revoked between the check and the call (race), and some OEM
  builds throw even when the permission appears granted. Wrapped in
  try/catch with a `setAndAllowWhileIdle()` fallback so alarms still
  fire (within the 1–2 minute Doze window) instead of disappearing
  silently.
- **`AlarmShareCodec.decodeToken` had no payload size guard.** A hostile
  `acx://alarm?data=…` deep-link with a multi-megabyte token could OOM
  the app during Base64 decoding. Now hard-caps tokens at 16 KB (real
  alarm payloads are ~1–2 KB).
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
operation — they make the unhappy paths quiet and predictable.

## [1.6.2] - 2026-04-29

Easier alarm dismissal — both from the lock-screen notification and via
gestures on the firing screen.

### Changed

- **Tapping the alarm notification now opens the firing screen.** The
  notification used to set only `setFullScreenIntent`, so if the
  full-screen launch was suppressed (e.g. user is mid-call) or the
  notification had collapsed in the shade, tapping the body did
  nothing — only the action buttons were reachable. Added
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
a swipe-away gesture — left works better. And the missing
`setContentIntent` was a real dead-end: a returning notification tap did
absolutely nothing, which is exactly the wrong behaviour for an
ongoing-alarm notification.

## [1.6.1] - 2026-04-29

Premium-polish design-system pass. No new features, no schema changes —
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
improvement — the kind of system-level work that makes the product feel
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
changes — targets real bug classes that became visible under Android
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
  the service before it could foreground. Now logged and skipped — the
  alarm still plays with the implicit wake from
  `FLAG_ACTIVITY_TURN_SCREEN_ON` on the firing activity.

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
