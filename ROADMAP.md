# AlarmClockXtreme Roadmap

Living feature backlog, refreshed alongside **v1.9.4** (next-alarm notification
refresh accuracy on top of exact-alarm grant recovery; see [CHANGELOG.md](CHANGELOG.md)).

This is the "what's left" companion to [CLAUDE.md](CLAUDE.md). Entries are
ranked by impact-to-effort and grouped by theme.

**Legend**
- `[ ]` Not started
- `[~]` Design / research stage
- `[x]` Implemented (kept briefly for traceability before migrating to CLAUDE.md)
- Effort: **S** = single session, **M** = a few days of focused work,
  **L** = multi-phase initiative.
- Tier: **Now** (next release), **Next** (the one after), **Later**
  (kept on the list, not actively scheduled), **UC** (under consideration —
  needs scoping or platform readiness), **Rejected** (explicitly out).

> **Recently shipped** (from prior tiers, kept here briefly): v1.9.4
> next-alarm notification minute-boundary refresh, v1.9.3 exact-alarm
> permission listener + WorkManager recovery, v1.9.2 premium UI polish
> (sharper shape tokens, unified chip rows, alarm-list feedback,
> settings progress states), time-of-day
> sky engine + weather-aware overrides + NWS tornado overlay (1.9.0),
> design-system polish pass / `AppFilterChip` / skeletons / bottom-nav
> truncation fix (1.8.1), Weather hub + Windy radar + RSS News tab (1.8.0),
> YouTube alarm-sound downloader with preview + faux progress (1.7.0–1.7.3),
> Today-tab weather hero ported from ZeusWatch (1.7.4), R8 Rhino fix +
> Timer/World layout regression fixes (1.7.5), end-to-end engineering audit
> (1.6.3), shareable alarm deep links, encrypted backup with PBKDF2 +
> AES-256-GCM, stats history filters (1.5.x), RPS / Emoji Memory /
> Typing Speed / Wordle / Simon Says / Stroop / Date Backwards challenges
> (1.5.0–1.6.0), solar-relative firing, hardwareButtonAction UI,
> dismissAtRingtoneEnd UI, ringtonePool editor, fade-out taper slider,
> in-app changelog dialog, MissedAlarmUnlockReceiver + ProximityCoverDetector
> tests.

---

## Current snapshot (v1.9.4)

- **Stack:** Kotlin 2.1, Compose / Material 3, Room v8, Hilt, Retrofit +
  Moshi (codegen), DataStore, Glance widgets, OkHttp, WorkManager, yt-dlp +
  NewPipe Extractor (Play flavor only).
- **Targets:** minSdk 26, targetSdk 35, compileSdk 35, versionCode 41.
- **Surface area:** 114 Kotlin source files, two flavors (`play`, `fdroid`),
  19 dismiss challenges, 50+ alarm fields, 35+ AppSettings fields, 6 tabs
  (Today, Alarms, Bedtime, Timer, World, News) + Settings.
- **What's missing vs. competitors:** Wear OS / Health Connect / standalone
  watch story is still zero; no on-device sleep-stage classifier; no AI
  sleep coach; no Live Updates progress notification; no lockscreen-widget
  surface (Pixel-led Android 15+); no air-quality / pollen on the weather
  hub; no Material 3 Expressive components (Android 16+); no
  `AutomaticZenRule` v2 ownership; no foldable/tablet adaptive layout.

---

## NOW — v1.10 candidates

Highest impact-to-effort with the existing stack. All are scoped to land
without breaking schema or flavor parity.

| # | Item | Source | Effort | Rationale |
|---|------|--------|--------|-----------|
| N1 | `BootReceiver.goAsync()` + WorkManager batch reschedule for users with 50+ alarms | [whakaara/AlarmScheduler](https://github.com/ahudson20/whakaara) | S | v1.5.4 tightened to 8 s — still risk of ANR on heavy users. WorkManager has no ceiling. |
| N2 | Air-quality + pollen on the Weather tab via Open-Meteo `air-quality` endpoint (US AQI bands, tree/grass/weed pollen rows) | [Open-Meteo Air Quality](https://open-meteo.com/en/docs/air-quality-api) | S | Same Retrofit + Moshi pipeline as the existing weather call. No key, free. Round-trip from "Today" tab to Weather tab. |
| N3 | Long-press snooze time-picker on the firing screen | [yuriykulikov/AlarmClock](https://github.com/yuriykulikov/AlarmClock) | S | Cycling through 1/3/5/15/30 presets is fine for a tap; long-press should open an inline minute picker. |
| N4 | Long-press dismiss confirmation gesture toggle | [yuriykulikov/AlarmClock](https://github.com/yuriykulikov/AlarmClock) | S | "Hold 1.5 s to dismiss" mode for users who keep accidentally swiping. Per-alarm flag. |
| N5 | Volume / haptic-only "Don't wake partner" alarm profile (`AudioAttributes.USAGE_ALARM` muted + `VibrationEffect.Composition`) | Sleep as Android couples mode, [Apple Bedtime](https://support.apple.com/guide/iphone/wake-up-with-an-alarm-iph59f3ddd0f/ios) | S | Sets the foundation for partner profiles (later) without touching schema. |
| N6 | `AutomaticZenRule` v2 ownership — bedtime DND as a `ConditionProviderService` | [Android 14 ConditionProvider docs](https://developer.android.com/reference/android/service/notification/ConditionProviderService) | M | Right now we depend on Google Clock or system DND — owning the rule lets bedtime/wake transitions toggle DND deterministically per alarm. |
| N7 | Calendar-aware first-meeting shift (alarm shifts earlier when first meeting moves) | Internal | S | Reuses the `CalendarAutoAlarmWorker` that already reads `CalendarContract.Instances`. |
| N8 | Wake-streak flame badge on the Stats tab | [Streaks](https://streaksapp.com/) / Duolingo | S | Stats already records dismiss outcomes — only the surface is missing. |
| N9 | Live changelog dialog gains a "what's next" link to this file | Internal | S | Closes the loop between releases and roadmap. |
| N10 | Material 3 Expressive opt-in once stable on Compose BOM (Android 16) — bolder accent surfaces, expressive shape tokens | [Material 3 Expressive](https://m3.material.io/blog/material-3-expressive) | S | Expressive components are additive — wire behind a flag, ship when the BOM lands. |
| N11 | `Notification.ProgressStyle` "Live Updates" for the persistent next-alarm notification on Android 16+ | [Android 16 Live Updates](https://developer.android.com/about/versions/16/features#progress-centric-notifications) | S | Today's persistent notification is a static line — turning it into a progress bar that ticks down to fire-time is a free upgrade on supported OSes. |

## NEXT — v1.11 candidates

| # | Item | Source | Effort | Rationale |
|---|------|--------|--------|-----------|
| X1 | Wear OS tile (next-alarm + dismiss + snooze) | [Wear OS Tiles API](https://developer.android.com/training/wearables/tiles), Pixel Watch 3 | M | Zero wearable story today. Tiles are glanceable, no full app required. Reuses the QS-tile dismiss pattern. |
| X2 | Wear OS "Next Alarm" complication | [Complications API](https://developer.android.com/training/wearables/complications) | S | Pairs with X1; Pixel Watch users routinely add it to faces. |
| X3 | Health Connect Sleep Sessions (read + write) | [Health Connect Sleep](https://developer.android.com/health-and-fitness/guides/health-connect/data-and-data-types/sleep), [androidx.health.connect:connect-client](https://developer.android.com/jetpack/androidx/releases/health-connect) | M | One API replaces per-vendor SDKs. Required Play Console declaration + privacy policy update. |
| X4 | On-device actigraphy → Awake / Light / Deep buckets (Cole-Kripke) | [Cole-Kripke 1992](https://pubmed.ncbi.nlm.nih.gov/1455130/), [Pillow](https://www.pillow.app/) | L | Existing `SmartAlarmService` already collects accel; bucketize, persist, render. Ships independently of TFLite — TFLite REM stage is L+. |
| X5 | Smart-wake window (accel-based light-sleep firing) | [Sleep Cycle SDK](https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk) | M | Pairs with X4. Fires within user-defined N-min window when motion suggests light sleep. |
| X6 | Composite sleep score (0-100 = duration × efficiency × regularity × stage balance) | [Rise](https://www.risescience.com/), Oura | S | Daily engagement hook. Cheap once X4 lands; can ship a duration-only v1 sooner. |
| X7 | Sleep debt accumulator (rolling 14-day deficit vs. per-user need) | Rise | S | Surfaces naps and bedtime reminders more contextually. |
| X8 | Stats charts — sleep score / snooze rate / streaks (Vico) | [Vico](https://github.com/patrykandpatrick/vico) | M | Stats tab is text-only today; charts move it from "log" to "feedback loop." |
| X9 | Voice-phrase dismiss challenge (offline `SpeechRecognizer`) | [Alarmy](https://alar.my/en/blog/alarmy-wake-up-mission), [I Can't Wake Up](https://play.google.com/store/apps/details?id=com.bartat.android.icwu) | M | Roadmap item from prior pass; Alarmy paywalls it, ACX can ship free. |
| X10 | Handwriting / drawing dismiss challenge (ML Kit Digital Ink) | [ML Kit Digital Ink](https://developers.google.com/ml-kit/vision/digital-ink-recognition), Alarmy | M | Pairs cleanly with the existing `Challenge` sealed-class; no new permission. |
| X11 | Pushup / plank-hold challenge (accelerometer signature, no camera) | Alarmy Premium | M | Reuses squat detector heuristics. |
| X12 | Spot-the-difference / chess-mate-in-1 / RSVP speed reading | indie / NYT Games / research | M | Three more challenges to hit a 22+ roster — Alarmy parity. |
| X13 | Lockscreen-widget surface (next-alarm chip) on Android 15 Pixels | [Android 15 lockscreen widgets](https://developer.android.com/about/versions/15/features) | S | Glance widget is already published — adding the lockscreen receiver intent + size class is a thin wrapper. |
| X14 | Tablet / foldable adaptive layouts (`WindowSizeClass`, dual-pane Alarms / Edit) | [Compose adaptive layouts](https://developer.android.com/develop/ui/compose/layouts/adaptive) | M | Single-pane Compose UI today; foldables and 8" tablets hit medium / expanded width classes. |
| X15 | LE Audio hearing-aid routing (`AudioAttributes.USAGE_ALARM` + `MediaRouter2.RouteCategory.LE_AUDIO`) | [Android 13 LE Audio](https://source.android.com/docs/core/connect/bluetooth/le_audio) | M | Underserved accessibility surface; LE Audio hearing-aids ignore most alarm streams. |
| X16 | Public-transit-aware alarm (shift earlier on weather + commute time) | Google Maps Distance Matrix or open routing | L | Listed Later previously — promote when the weather / calendar plumbing is healthy enough to chain a routing call. Falls back gracefully without API key. |

## LATER — kept on the list

Sleep platform deepening, novel challenges, and platform-level features
that need either OEM cooperation or platform-API stability. Not actively
scheduled; revisited every two minor releases.

### Sleep tracking deepening

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-S1 | TFLite REM-stage classifier (extends X4) | [Pillow](https://www.pillow.app/) / SleepWatch | L |
| L-S2 | Snore recording + timeline (mic ring buffer, save >60 dB bursts) | Sleep as Android | M |
| L-S3 | Pre-sleep tag tiles (caffeine / exercise / alcohol / stress) + correlation chart | Sleep as Android | M |
| L-S4 | Lullaby soundscapes with motion-auto-off | Calm / Headspace / Sleep as Android lullaby pack | M |
| L-S5 | Environmental-noise baseline before bedtime reminder | Pillow | S |
| L-S6 | Apnea event flagging (mic onset detection — explicit "screening, not diagnosis" disclaimer) | [Springer 2025: smartwatch IMU OSA detection](https://link.springer.com/article/10.1007/s11325-025-03255-w), [Samsung × Stanford 2025](https://www.samsungmobilepress.com/articles/samsung-announces-collaboration-with-stanford-medicine-to-advance-sleep-apnea-detection-and-beyond) | L |
| L-S7 | Chronotype quiz (Munich MEQ) → ideal bedtime / wake calc | [Roenneberg MEQ](https://www.thewep.org/documentations/mctq) | S |
| L-S8 | "AI sleep coach" — small on-device LLM summarizing trends and surfacing one suggestion per day. **Strict privacy: model + inference local only.** | Sleep as Android beta, Rise | L |

### Wear OS / wearable depth (beyond X1-X3)

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-W1 | Wearable-only vibration alarm (silent phone) | Sleep as Android | M |
| L-W2 | Bed-exit auto-dismiss via watch motion >60 s | [Withings](https://www.withings.com/) / [Garmin Connect](https://connect.garmin.com/) | M |
| L-W3 | HRV-aware smart wake (watch HRV dip > phone accel for light-sleep detection) | [Sleep as Android wear-data](https://docs.sleep.urbandroid.org/sleep/wearable_devices.html) | L |
| L-W4 | Standalone Wear OS app (alarm fires without phone) | Pixel Watch 3 native alarm | L |

### Workplace / shift worker

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-W5 | Rotating shift patterns (DDNNO / 4-on-4-off / Panama) | [Shyft](https://www.myshyft.com/) | M |
| L-W6 | Jet-lag re-entrainment schedule (gradual shift over N days) | [Timeshifter](https://www.timeshifter.com/), Roenneberg | L |
| L-W7 | On-call rotation mode (override DND silent) | [PagerDuty](https://www.pagerduty.com/) | M |

### Household / relationships

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-H1 | Partner profiles (two users, separate alarms / ringtones) | Sleep as Android couples | M |
| L-H2 | Paired-phone LAN sync (partner-dismiss → you snooze) | — | M |
| L-H3 | Kid-friendly green-light mode | [OK to Wake](https://www.amazon.com/dp/B003O15A1G) / [Hatch](https://www.hatch.co/) | M |
| L-H4 | Pet-feeding reminder chain on dismiss | — | S |
| L-H5 | Remote parental alarm set | [Google Family Link](https://families.google.com/familylink/) | L |

### Habit / routine integration

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-R1 | Gratitude / journal prompt on dismiss | [Day One](https://dayoneapp.com/) / [Stoic](https://www.getstoic.com/) | S |
| L-R2 | Water-intake quick-log tiles | [WaterMinder](https://waterminder.com/) | S |
| L-R3 | Mood selfie + emoji tag | [Daylio](https://daylio.net/) | S |
| L-R4 | Obsidian / Notion / Markdown daily-note append | — | M |
| L-R5 | Health Connect weight / BP quick-entry | [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect) | S |
| L-R6 | Tasker / MacroDroid / Home Assistant recipe library | [Home Assistant Tasker](https://github.com/markadamcik/Home-Assistant-Tasker) | S |
| L-R7 | Badge set: "5 AM club", "no-snooze week", "DDNNO survivor" | [Habitica](https://habitica.com/) | S |
| L-R8 | Share-card screenshot generator | [Strava](https://www.strava.com/) | S |

### Audio depth

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-A1 | Binaural / isochronic delta (0.5-4 Hz) tone generator | [Brain.fm](https://brain.fm/), myNoise | M |
| L-A2 | Mathematical-noise synth (brown / pink / violet) | [myNoise](https://mynoise.net/) | S |
| L-A3 | Voice-memo ringtone (in-app 30 s recorder) | iOS native | S |
| L-A4 | Podcast latest-episode (Podcast Index / AntennaPod URI) | [AntennaPod](https://github.com/AntennaPod/AntennaPod) | M |
| L-A5 | Per-alarm Bluetooth sink (specific A2DP device) | — | M |
| L-A6 | Chromecast / Nest Hub alarm target | [Cast SDK](https://developers.google.com/cast/docs/android_sender) | M |
| L-A7 | UPnP / DLNA multi-room cast escalation | [Cling](https://github.com/4thline/cling) | L |

### Advanced scheduling

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-D1 | Islamic prayer-time Fajr alarm | [Aladhan API](https://aladhan.com/prayer-times-api) | M |
| L-D2 | Lunar / Hebrew / Hindu calendar repeat | — | M |
| L-D3 | Astronomical events (meteor-shower peak, ISS flyover) | [Heavens-Above](https://www.heavens-above.com/) | M |
| L-D4 | Birthday auto-alarm from Contacts | Contacts provider | S |
| L-D5 | Menstrual-cycle aware (softer alarm in luteal phase) | [Health Connect Menstruation](https://developer.android.com/reference/androidx/health/connect/client/records/MenstruationFlowRecord) | M |
| L-D6 | Weather-conditional firing (fire earlier on snow > 2 cm) | Open-Meteo | M |

### Power / reliability

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-P1 | Power-off alarm (Qualcomm / Samsung / Xiaomi HAL) | OEM | L (privileged partner programs — likely never indie-achievable) |
| L-P2 | Direct-boot full-screen alarm audit | Android 14+ | S |
| L-P3 | Emergency-escalation call tree (SMS → call → partner → siren) | Twilio / native | M |
| L-P4 | Location-based escalation (still at home → siren) | FusedLocation | M |
| L-P5 | Car-mode suppression (Android Auto `DrivingStateManager`) | [Android Auto](https://developer.android.com/training/cars) | S |
| L-P6 | Companion-watch autonomous fire if phone battery dies | — | M |
| L-P7 | Charging-only alarm variant | — | S |

### Cloud / sync

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-C1 | Google Drive / Nextcloud / WebDAV backup (SAF-based, opt-in, encryption already exists) | [SAF](https://developer.android.com/guide/topics/providers/document-provider) | M |
| L-C2 | End-to-end encrypted paired-phone sync | — | L |

### UX polish

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-U1 | Always-On Display-aware Night Clock (uses AOD API rather than full-bright service) | AOD API | S |
| L-U2 | Dynamic color from a specific wallpaper accent rather than the full palette | — | S |
| L-U3 | Interactive onboarding walkthrough (per-feature highlights) | — | M |
| L-U4 | Predictive Back / `OnBackInvokedCallback` polish across screens | [Android 14 predictive back](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture) | S |
| L-U5 | Per-app language picker (`LocaleManager`) | Android 13 | S |
| L-U6 | Ultra-HDR sunrise rendering on Android 14+ | [HDR rendering](https://developer.android.com/about/versions/14/features#ultra-hdr) | S |
| L-U7 | Credential Manager + passkey-gated cloud backup | [Credential Manager](https://developer.android.com/training/sign-in/passkeys) | M |

### Accessibility

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-X1 | Screen-flash + camera-flash patterns for deaf users | [Apple Flash for Alerts](https://support.apple.com/guide/iphone/turn-on-and-customize-led-flash-iph6f30aa5fc/ios) | S |
| L-X2 | Haptic-only alarm profile via `VibrationEffect.Composition` | Apple Taptic | S |
| L-X3 | TalkBack audit — large double-tap buttons on firing screen | Android a11y | S |
| L-X4 | Pure-black / mono-color WCAG AAA high-contrast theme | — | S |
| L-X5 | Voice-only dismiss (offline `SpeechRecognizer`) | Voice Access | S |
| L-X6 | Per-user long-press thresholds on challenge buttons | Android a11y | S |

---

## UNDER CONSIDERATION

Items that need scoping or platform readiness before they earn a tier.

| Item | Blocker / scoping question |
|------|---------------------------|
| Material 3 Expressive full migration (vs. opt-in surfaces in N11) | Wait for Compose BOM stable on Android 16; migration cost vs. visual return is unknown. |
| Live-Updates / `ProgressStyle` bedtime countdown | Same as N12 — needs Android 16 install base measurement before becoming a default rather than a flag. |
| TensorFlow Lite REM-stage classifier | Model footprint vs. APK-size budget; users on F-Droid expect <40 MB. Need a downloadable-model strategy that doesn't break offline-first. |
| Tasker / MacroDroid plugin (true plugin, not just webhook) | Adds API surface to maintain; webhook covers most users. |
| Wear OS standalone app (L-W4) | Build-time, signing, separate Play track; revisit after X1-X3 prove demand. |
| Cloud LLM sleep-coach | Out of bounds — privacy stance forbids. Local LLM (L-S8) only. |

## REJECTED — explicit and indefinite

| Item | Reason |
|------|--------|
| Firebase / GA4 / any analytics SDK | Differentiator: "no tracking, no accounts, no data leaves your device." |
| Ad-supported free tier | Same. The app is and will remain ad-free. |
| Public streak / social feed sharing | Privacy trade-off not worth it. Local share-card (L-R8) is the substitute. |
| Sleep-coaching subscription | We remain open-source / donation-based. |
| YouTube alarm-source as a generic feature in F-Droid flavor | Licensing grey zone — `play` flavor only. F-Droid build keeps the strip-out for unencumbered distribution. |
| Cloud LLM for sleep insights | Same privacy stance; only on-device models considered (and only if they fit the APK budget). |
| Power-off alarm without OEM cooperation | Requires privileged partner programs unavailable to indie apps. (L-P1 stays Later as the formal "you'd need OEM" entry — reaffirmed Rejected for any non-OEM workaround.) |

---

## Cross-cutting tracks (audited every release)

### Platform compatibility

- **`USE_EXACT_ALARM` (install-time grant) instead of `SCHEDULE_EXACT_ALARM` (runtime).** ACX is alarm-clock-category — verify manifest each release. ([FossifyOrg/Calendar #217](https://github.com/FossifyOrg/Calendar/issues/217))
- **Try-catch every `AlarmManager.set*` call.** `setInexactAllowWhileIdle` can still throw if the device's exact-alarm fallback path engages. ([flutter_local_notifications #2248](https://github.com/MaikuB/flutter_local_notifications/issues/2248))
- **Android 15 short-type FGS auto-timeout (3 min cap).** Stay on `mediaPlayback` type, do NOT migrate to `shortService`. ([Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15))
- **Doze defers even `setAlarmClock()` 1-2 min on Redmi/Samsung.** Pair with a 10-15 s `PARTIAL_WAKE_LOCK` in `onReceive`; keep within ANR ceiling.
- **`setAlarmClock()` always shows status-bar icon.** Already mitigated with a settings toggle that falls back to `setExactAndAllowWhileIdle` (with disclaimer); keep the toggle in the UI.
- **`READ_CALENDAR` runtime denial.** `CalendarAutoAlarmWorker` must early-return on denial. Verify each release.
- **`Configuration.Provider` + manifest initializer removal.** WorkManager + Hilt regression vector; CI check exists, keep it.

### Security / privacy

- AES-256-GCM + PBKDF2-HMAC-SHA256 (200k iters) for backup encryption — shipped 1.5.x. Audit iteration count yearly against [OWASP Password Storage cheat sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
- Shareable-alarm import is **disabled by default** until reviewed — keep that. Never silently schedule a received link's alarm.
- Hue v1 username endpoints are deprecated — migrate `HueSunriseWorker` to v2 `application_key` + HTTPS pinning. Tracked under L-A.
- Webhook URL is user-supplied and never auto-validated — document this as part of the threat model rather than retrofitting validation that won't catch a determined misuse.

### Observability

- Crash logger writes to local files only; we don't ship a remote sink and won't (privacy). Ensure rotation cap remains in place so a runaway loop can't fill storage.
- Add a "share crash log" button on the About screen (does not auto-upload — copies to clipboard or invokes share sheet). **S, not yet tiered.**

### Distribution / packaging

- Two flavors today: `play` (with YT downloader), `fdroid` (without). Maintain parity on every other surface. CI workflow `release.yml` builds both via `gh release upload --clobber`.
- F-Droid lint passes — anti-feature flag for the YT downloader is documented in `metadata/`. Re-verify on each release.
- AAB for Play Store, signed APK for GitHub Releases; never ship unsigned artifacts.

### i18n / l10n

- English-only today. Per-app language picker (L-U5) lands first; THEN community translation. No machine-translation-only strings — better to remain English than ship broken translations.

### Testing

- Unit tests cover `MissedAlarmUnlockReceiver` window logic, `ProximityCoverDetector` hold threshold, and the share-link filter helper. Each new dismiss challenge must come with a unit-tested "valid input" + "invalid input" suite.
- Room migration tests: every schema bump requires a `testMigration()` block (whakaara discipline).
- Add an instrumented smoke test that fires an alarm via test broadcast and asserts the firing activity launches with `FLAG_SHOW_WHEN_LOCKED`. **S, not yet tiered.**

### Documentation

- README, CHANGELOG, ROADMAP, CLAUDE.md, and the version badge must all match on every release. README + ROADMAP feature list parity is checked on each release pass.

### Plugin ecosystem

- Webhooks (Tasker / MacroDroid / Home Assistant) cover the integration surface we want to expose. A "real" plugin SDK is rejected (UC) until webhook gaps are documented.
- Recipe library (L-R6) — bundle 5-10 ready-made Tasker / Home Assistant flows in `docs/integrations/`.

---

## Research sources (round 4 — refreshed for v1.9.0)

### Direct OSS competitors

- **fennifith/Alarmio** — https://github.com/fennifith/Alarmio
- **FossifyOrg/Clock** — https://github.com/FossifyOrg/Clock — Simple-Tools fork, v1.6.0 (2025) added custom font support, gradual volume, persistent notification.
- **BlackyHawky/Clock** — https://github.com/BlackyHawky/Clock — privacy-first, no INTERNET permission. v2.29 (2026): missed-alarm logic, swipe-to-delete fixes, persistent notification, alarm-to-specific-date, flip/shake gestures, power-off alarm on Snapdragon.
- **LineageOS DeskClock** — https://github.com/LineageOS/android_packages_apps_DeskClock
- **AOSP DeskClock (`AlarmStateManager`)** — https://android.googlesource.com/platform/packages/apps/DeskClock/ — gold-standard alarm state machine.
- **yuriykulikov/AlarmClock** — https://github.com/yuriykulikov/AlarmClock — long-press dismiss UX, AOSP-derived `DismissAlarmActivity` formula.
- **yassineAbou/Clock** — https://github.com/yassineAbou/Clock — pure Compose, single-activity, WorkManager-backed timer/stopwatch persistence.
- **ahudson20/whakaara** — https://github.com/ahudson20/whakaara — Hilt + AlarmScheduler abstraction, Room migration discipline.
- **akshay2211/JetAlarm** — https://github.com/akshay2211/JetAlarm
- **plusmobileapps/alarm-clock** — https://github.com/plusmobileapps/alarm-clock
- **vicolo-dev/chrono** — https://github.com/vicolo-dev/chrono — Flutter, but worth UX study.
- **sweakpl/qralarm-android** — https://github.com/sweakpl/qralarm-android
- **kunal-mahatha/Early-Bird-App** — https://github.com/kunal-mahatha/Early-Bird-App
- **WrichikBasu/ShakeAlarmClock** — https://github.com/WrichikBasu/ShakeAlarmClock
- **meenbeese/Chronos** — https://github.com/meenbeese/Chronos — Kotlin, ~108★ in 2025.
- **meticha/triggerx** — https://github.com/meticha/triggerx — alarm-execution library, ~101★.
- **giorgosneokleous93/fullscreenintentexample** — https://github.com/giorgosneokleous93/fullscreenintentexample
- **Applinx-Tech/Flutter-Alarm-Manager-POC** — https://github.com/Applinx-Tech/Flutter-Alarm-Manager-POC

### Commercial reference

- **Alarmy** — https://alar.my/en/blog/alarmy-wake-up-mission — 2025 Multiple Mission feature (combine up to 3 challenges) is parity for our Mission Chain. Photo, Math, Shake, Barcode/QR, Memory, Typing, Steps (premium), Squats (premium).
- **Sleep as Android** — https://sleep.urbandroid.org/documentation/release-notes/ — 2025 added Google Home API integration (BETA), AI Sleep Assistant (BETA), HRV gain cards, dashboard redesign, wake-up check automation events. 2025 Lullabies addon (Magic / Strings / Fantasy / Megalith).
- **Sleep Cycle** — https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk — 2026 SDK release. Algorithm: phone mic + accelerometer detects sleep stages and fires alarm in lightest phase within wake window.
- **Rise** — https://www.risescience.com/ — sleep-debt accumulator + composite score reference.
- **Pillow** — https://www.pillow.app/
- **Turbo Alarm** — https://play.google.com/store/apps/details?id=com.turbo.alarm — Spotify-as-alarm reference. 2025: Wear OS support, talking alarm with weather, sunrise simulation, mini-game dismiss, "Anti-Sleepyhead Security" (require leaving the house), cloud-sync, Tasker / Macrodroid / Sleepbot integration.
- **Google Clock** — https://play.google.com/store/apps/details?id=com.google.android.deskclock
- **I Can't Wake Up** — Simon-says, voice-phrase reference.
- **Timeshifter** — https://www.timeshifter.com/ — jet-lag re-entrainment reference for L-W6.

### Awesome lists / FOSS catalogs

- GitHub topics: https://github.com/topics/alarm-clock?l=kotlin and https://github.com/topics/sleep-tracker
- F-Droid Clocks & Alarms: https://f-droid.org/en/categories/clock/
- IATkachenko/HA-SleepAsAndroid (Home Assistant integration) — https://github.com/IATkachenko/HA-SleepAsAndroid

### Platform docs / standards / specs

- Android 14 behavior changes — https://developer.android.com/about/versions/14/behavior-changes-14
- Android 15 behavior changes — https://developer.android.com/about/versions/15/behavior-changes-15
- Android 15 features — https://developer.android.com/about/versions/15/features
- Android 16 features — https://developer.android.com/about/versions/16/features
- Android 16 Live Updates / `ProgressStyle` — https://developer.android.com/about/versions/16/features#progress-centric-notifications
- Material 3 Expressive — https://m3.material.io/blog/material-3-expressive
- Wear OS Tiles API — https://developer.android.com/training/wearables/tiles
- Wear OS Complications API — https://developer.android.com/training/wearables/complications
- Health Connect Sleep — https://developer.android.com/health-and-fitness/guides/health-connect/data-and-data-types/sleep
- Health Connect Menstruation — https://developer.android.com/reference/androidx/health/connect/client/records/MenstruationFlowRecord
- ML Kit Digital Ink — https://developers.google.com/ml-kit/vision/digital-ink-recognition
- Glance widgets — https://developer.android.com/jetpack/androidx/releases/glance
- Compose adaptive layouts — https://developer.android.com/develop/ui/compose/layouts/adaptive
- LE Audio (Android 13+) — https://source.android.com/docs/core/connect/bluetooth/le_audio
- AutomaticZenRule v2 — https://developer.android.com/reference/android/app/AutomaticZenRule
- Predictive back — https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture
- LocaleManager — https://developer.android.com/about/versions/13/features/app-languages
- Credential Manager — https://developer.android.com/training/sign-in/passkeys
- `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` — https://developer.android.com/reference/android/app/AlarmManager#ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
- Open-Meteo Weather — https://open-meteo.com/
- Open-Meteo Air Quality + pollen — https://open-meteo.com/en/docs/air-quality-api
- NWS Active Alerts — https://www.weather.gov/documentation/services-web-api
- Nager.Date holidays — https://date.nager.at/
- Aladhan prayer times — https://aladhan.com/prayer-times-api

### Academic / industry / engineering

- Cole-Kripke 1992 — https://pubmed.ncbi.nlm.nih.gov/1455130/
- Roenneberg MEQ — https://www.thewep.org/documentations/mctq
- Springer 2025 (smartwatch IMU OSA) — https://link.springer.com/article/10.1007/s11325-025-03255-w
- Samsung × Stanford OSA collab 2025 — https://www.samsungmobilepress.com/articles/samsung-announces-collaboration-with-stanford-medicine-to-advance-sleep-apnea-detection-and-beyond
- AASM smartwatch sleep features comparison — https://aasm.org/comparing-sleep-features-of-popular-smartwatches/
- Sleep Cycle SDK announcement — https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk

### Community signal

- r/Android complaints (recurring): missed alarms on Xiaomi/Samsung/Oppo battery-management; subscription fatigue (Alarmy / Sleep as Android Premium); Google Clock missing skip-one-occurrence + mission challenges.
- Maker complaints (Hacker News / accessibleandroid.com): mini-game dismiss is poorly TalkBacked across the field; Turbo Alarm called out specifically. Accessibility-first dismiss alternatives (haptic, voice) are differentiators.

### Library changelogs to mine each release

- `androidx.work:work-runtime-ktx` — https://developer.android.com/jetpack/androidx/releases/work
- `androidx.glance:glance-appwidget` — https://developer.android.com/jetpack/androidx/releases/glance
- `androidx.compose.material3` — https://developer.android.com/jetpack/androidx/releases/compose-material3
- `androidx.health.connect:connect-client` — https://developer.android.com/jetpack/androidx/releases/health-connect
- yt-dlp — https://github.com/yt-dlp/yt-dlp/releases
- NewPipeExtractor — https://github.com/TeamNewPipe/NewPipeExtractor/releases
- OkHttp / Retrofit / Moshi / Hilt / Room — keep current via dependency-bot or quarterly audit.

### Legal / compliance flags to budget before touching

- Health Connect (X3) — published privacy-policy update + Play Console declaration.
- Apnea event flagging (L-S6) — explicit "screening, not a medical device" disclaimer; consider keeping it `play`-flavor only for legal hygiene.
- Power-off alarm (L-P1) — per-OEM privileged partner programs; may never be achievable for an indie app.
- Partner-phone / paired-phone sync (L-H2 / L-C2) — explicit threat model doc before code.
- LLM sleep-coach (L-S8) — bundled model size budget; F-Droid users expect <40 MB APK.

---

*Roadmap owners: add yourself as assignee when picking up an item.
Prefer one-item-per-PR for the S-effort work and phased delivery for
M / L. Update this file on every release alongside CHANGELOG.md.*
