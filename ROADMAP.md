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
  (kept on the list, not actively scheduled), **UC** (under consideration —
  needs scoping or platform readiness), **Rejected** (explicitly out).


---

## Current snapshot (v1.15.30)

- **Stack:** Kotlin 2.1, AGP 8.11.1 / Gradle 8.13, Compose BOM 2026.06.00 /
  Material 3 (1.4.x), Room 2.6.1 / DB v23, Hilt 2.56.2, Retrofit 2.11 + Moshi (codegen),
  DataStore 1.1.1, Glance 1.1.1, OkHttp 5.4.0, WorkManager 2.9.1, Wear Tiles
  1.6.0 / protolayout 1.4.0, Wear Data Layer, Wear Watchface complications
  data-source 1.3.0, Health Connect client 1.1.0 (Play flavor), ML Kit Digital
  Ink 19.0.0 (Play flavor), Media3 1.10.1, Direct Boot minimum alarm fallback,
  yt-dlp (`youtubedl-android` 0.18.1) + NewPipe Extractor
  0.26.3 (Play flavor only).
- **Targets:** `minSdk 26`, `targetSdk 36`, `compileSdk 36`,
  `versionCode 132`, `versionName 1.15.30`.
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

Verified findings deliberately NOT fixed in the v1.15.29 pass — each needs
design judgment, a large refactor, or on-device confirmation rather than a
surgical change.

- [ ] **P2/debt — God files.** `SettingsScreen.kt` (~4.1k lines),
  `AlarmEditScreen.kt` (~3.5k), `BedtimeScreen.kt` hold every page /
  pane / dialog. The section enums already give clean seams; extract per-page
  files. Effort: M. **In progress:** `BedtimeScreen.kt` is being drained
  section-by-section (`BedtimeJetLagSection.kt`, `BedtimeChronotypeSection.kt`,
  `BedtimeBreathingSection.kt` extracted so far, ~2.1k → ~1.65k lines).
  Remaining: finish the BedtimeScreen sleep-tracking / sleep-sounds / wind-down
  sections, then split `SettingsScreen.kt` and `AlarmEditScreen.kt`.

---

## LATER — kept on the list

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
| L-H2 | Paired-phone LAN sync (partner-dismiss → you snooze). Strict privacy: LAN-only, no cloud. | — | M |
| L-H3 | Kid-friendly green-light mode. | [OK to Wake](https://www.amazon.com/dp/B003O15A1G), [Hatch](https://www.hatch.co/) | M |
| L-H4 | Pet-feeding reminder chain on dismiss. | — | S |
| L-H5 | Remote parental alarm set. | [Google Family Link](https://families.google.com/familylink/) | L |
| L-H6 | Synchronized alarm groups — edit one, propagate to siblings sharing a label. **NEW.** | [BlackyHawky Clock 2.29](https://github.com/BlackyHawky/Clock/releases) | M |

### Habit / routine integration

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-R1 | Gratitude / journal prompt on dismiss. | [Day One](https://dayoneapp.com/), [Stoic](https://www.getstoic.com/) | S |
| L-R2 | Water-intake quick-log tiles. | [WaterMinder](https://waterminder.com/) | S |
| L-R3 | Mood selfie + emoji tag. | [Daylio](https://daylio.net/) | S |
| L-R4 | Obsidian / Notion / Markdown daily-note append. | [TaskForge.md](https://taskforge.md/android/); [Notelert Obsidian forum](https://forum.obsidian.md/t/notelert-native-android-notification-and-reminders-for-obsidian/109310) | M |
| L-R5 | Health Connect weight / BP / mood quick-entry. | [Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types) | S |
| L-R7 | Badge set: "5 AM club", "no-snooze week", "DDNNO survivor". | [Habitica](https://habitica.com/) | S |
| L-R8 | Share-card screenshot generator (local — no social-feed; matches REJECTED stance). | [Strava](https://www.strava.com/) | S |

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
| L-A8 | Folder-based ringtone import — point at a directory, expose its files in the picker. **NEW.** | [BlackyHawky Clock 2.23](https://github.com/BlackyHawky/Clock/releases) | S |
| L-A9 | System-ringtone preview button parity with the YouTube preview row. **NEW.** | local: [RingtonePickerSheet.kt](app/src/main/java/com/sysadmindoc/alarmclock/ui/ringtone/RingtonePickerSheet.kt) | S |
| L-A10 | Pre-alarm low-volume gentle wake — separate alarm 30 min before main alarm, designed to lift you out of deep sleep. **NEW.** | [yuriykulikov/AlarmClock](https://github.com/yuriykulikov/AlarmClock) signature feature | M |

### Advanced scheduling

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-D1 | Islamic prayer-time Fajr alarm via Aladhan. | [Aladhan API](https://aladhan.com/prayer-times-api); [Al-Azan](https://f-droid.org/packages/com.github.meypod.al_azan/) | M |
| L-D2 | Lunar / Hebrew / Hindu calendar repeat. | — | M |
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
| L-P6 | Companion-watch autonomous fire if phone battery dies. | — | M |
| L-P7 | Charging-only alarm variant. | — | S |

### Cloud / sync

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-C1 | Google Drive / Nextcloud / WebDAV backup via SAF (opt-in; encryption already exists). | [SAF docs](https://developer.android.com/guide/topics/providers/document-provider); [SeedVault](https://nlnet.nl/project/SeedVault-Integrity/) for inspiration | M |
| L-C2 | End-to-end encrypted paired-phone LAN sync. | — | L |

### Smart home

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-SH1 | Matter 1.6 Dynamic Lighting (DLE) cross-brand sunrise — extends Hue path to any Matter bulb without per-brand workarounds. **NEW.** | [Matter 1.6 DLE 2026](https://mattressmiracle.ca/blogs/mattress-miracle-blog/matter-1-6-dynamic-lighting-sunrise-gradient-bedroom); [Matter Innovations CES 2026](https://matter-smarthome.de/en/products/the-matter-innovations-at-ces-2026/); [Google Home Matter dev docs](https://developers.home.google.com/matter) | L |

### UX polish

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-U1 | Always-On Display-aware Night Clock (uses AOD API rather than full-bright service). | [Android AOD docs](https://developer.android.com/training/wearables/watch-faces/ambient-mode) | S |
| L-U2 | Dynamic color from a specific wallpaper accent rather than the full palette. | — | S |
| L-U3 | Interactive onboarding walkthrough (per-feature highlights). | — | M |
| L-U4 | Predictive-back progress on alarm-edit unsaved-changes dialog (`PredictiveBackHandler`). | [Compose predictive back](https://developer.android.com/develop/ui/compose/system/predictive-back) | S |
| L-U5 | Per-app language picker (`LocaleManager`). Prereq for community translation. | [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages) | S |
| L-U6 | Ultra-HDR sunrise rendering on Android 14+. | [Ultra HDR rendering](https://developer.android.com/about/versions/14/features#ultra-hdr) | S |
| L-U7 | Credential Manager + passkey-gated cloud backup. | [Credential Manager](https://developer.android.com/training/sign-in/passkeys) | M |
| L-U8 | Roman-numeral / additional analog Night Clock face styles. **NEW.** | [BlackyHawky Clock 2.29](https://github.com/BlackyHawky/Clock/releases) | S |

### Accessibility

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-X1 | Screen-flash + camera-flash patterns for deaf users. | [Apple Flash for Alerts](https://support.apple.com/guide/iphone/turn-on-and-customize-led-flash-iph6f30aa5fc/ios); [Android sound notifications](https://support.google.com/accessibility/android/answer/9286728) | S |
| L-X3 | TalkBack audit — large double-tap buttons on firing screen. | [Android accessibility overview](https://support.google.com/accessibility/android/answer/6006564) | S |
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
| iOS-26 AlarmKit UX pattern adoption (full-screen snooze/stop visuals, App-Intent secondary action) | Study-only — App Intents are iOS-only; port the platform-neutral visual + interaction patterns to ACX firing screen. ([Apple AlarmKit](https://developer.apple.com/documentation/AlarmKit)) |
| Tasker / MacroDroid plugin (true plugin, not just webhook) | Adds API surface to maintain; webhook covers most users. ([Tasker plugin intro](https://tasker.joaoapps.com/plugins-intro.html)) |
| Wear OS standalone app (L-W4) | Build-time, signing, separate Play track; revisit after X1 (complication) proves demand. |
| Cloud LLM sleep-coach | Out of bounds — privacy stance forbids. Local LLM (L-S9) only. |
| Open-Meteo MTG high-resolution solar data | Wait for general availability of the MTG endpoint beyond DWD's Feb 2026 EU/AF launch. ([Open-Meteo seasonal forecast update](https://openmeteo.substack.com/p/seasonal-weather-forecasts)) |
| Custom typeface support per alarm / per app | UX/typography churn risk; revisit when M3 Expressive stabilizes (post v1.13 X17). ([BlackyHawky Clock 2.28](https://github.com/BlackyHawky/Clock/releases)) |
| KMP / Compose-Multiplatform extraction of `NextAlarmCalculator` + `ChallengeGenerator` | Strategic for a future desktop/web Stats companion; L effort, low immediate impact. Defer until at least one cross-platform consumer is concrete. ([Compose Multiplatform 1.11](https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html)) |

## REJECTED — explicit and indefinite

| Item | Reason |
|------|--------|
| Firebase / GA4 / any analytics SDK | Differentiator: "no tracking, no accounts, no data leaves your device." |
| Ad-supported free tier | Same. The app is and will remain ad-free. |
| Public streak / social feed sharing | Privacy trade-off not worth it. Local share-card (L-R8) is the substitute. |
| Sleep-coaching subscription | We remain open-source / donation-based. |
| Collaborative cloud-shared alarms (Ultimate Alarm Clock pattern) | Requires accounts + cloud storage. **NEW.** Local LAN-sync (L-H2) is the boundary we'll consider. ([CCExtractor/ultimate_alarm_clock](https://github.com/CCExtractor/ultimate_alarm_clock)) |
| Anti-uninstall accessibility-service trick (Alarmy "prevent turn off") | Abuse of AccessibilityService; Play policy violation; antithetical to user control. **NEW.** ([Alarmy review on JustUseApp](https://justuseapp.com/en/app/1163786766/alarmy-morning-alarm-clock/reviews)) |
| YouTube alarm-source as a generic feature in F-Droid flavor | Licensing grey zone — `play` flavor only. F-Droid build keeps the strip-out for unencumbered distribution. |
| Cloud LLM for sleep insights | Same privacy stance; only on-device models considered (and only if they fit the APK budget). |
| Power-off alarm without OEM cooperation | Requires privileged partner programs unavailable to indie apps. L-P1 is blocked in `Roadmap_Blocked.md`; non-OEM workarounds remain rejected. |

---

## Cross-cutting tracks (audited every release)

### Platform compatibility

- **`USE_EXACT_ALARM` (install-time grant) instead of `SCHEDULE_EXACT_ALARM` (runtime).** ACX is alarm-clock-category — verify manifest each release. ([FossifyOrg/Calendar #217](https://github.com/FossifyOrg/Calendar/issues/217))
- **Try-catch every `AlarmManager.set*` call.** `setInexactAllowWhileIdle` can still throw if the device's exact-alarm fallback path engages. ([flutter_local_notifications #2248](https://github.com/MaikuB/flutter_local_notifications/issues/2248))
- **Android 15 short-type FGS auto-timeout (3 min cap).** Stay on `mediaPlayback` type, do NOT migrate to `shortService`. ([Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15))
- **Doze defers even `setAlarmClock()` 1-2 min on Redmi/Samsung.** Pair with a 10-15 s `PARTIAL_WAKE_LOCK` in `onReceive`; keep within ANR ceiling. ([Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby))
- **`setAlarmClock()` always shows status-bar icon.** Already mitigated with a settings toggle that falls back to `setExactAndAllowWhileIdle` (with disclaimer); keep the toggle in the UI.
- **`READ_CALENDAR` runtime denial.** `CalendarAutoAlarmWorker` must early-return on denial. Verify each release.
- **`Configuration.Provider` + manifest initializer removal.** WorkManager + Hilt regression vector; CI check exists, keep it.
- **Android 16 "missed alarm — unknown reason" notification regression on Pixel.** Track the QPR fix and confirm ACX's foreground-service start path is not the cause. ([Android Police Pixel alarm bug](https://www.androidpolice.com/pixel-alarm-bug-is-back/))
- **Play wake-lock policy (March 2026).** N4 covers the audit; keep the wake-lock acquisition window inside the 2 h / 24 h non-exempt budget. ([9to5Google March 2026](https://9to5google.com/2026/03/05/google-starts-calling-out-android-apps-that-drain-your-battery-before-you-download-them/))

### Security / privacy

- AES-256-GCM + PBKDF2-HMAC-SHA256 (200k iters) for backup encryption — shipped 1.5.x. Audit iteration count yearly against [OWASP Password Storage cheat sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). Consider Argon2id when JNI dependency budget allows.
- Shareable-alarm import is **disabled by default** until reviewed — keep that. Never silently schedule a received link's alarm.
- Hue v1 username endpoints are deprecated — **migrate `HueSunriseWorker` to v2 `application_key` + HTTPS pinning. Tracked as N5 above.** ([Philips Hue API v2](https://developers.meethue.com/new-hue-api/))
- Webhook URL is user-supplied and never auto-validated — document this as part of the threat model rather than retrofitting validation that won't catch a determined misuse.

### Observability

- Crash logger writes to local files only; we don't ship a remote sink and won't (privacy). Ensure rotation cap remains in place so a runaway loop can't fill storage.
- Add a "share crash log" button on the About screen (does not auto-upload — copies to clipboard or invokes share sheet). **S, not yet tiered.**
- App Standby bucket surfaced in the Reliability Settings group (N3) doubles as observability for the user.

### Distribution / packaging

- Two flavors today: `play` (with YT downloader + Wear Data Layer), `fdroid` (without). Maintain parity on every other surface. Build, test, signing, OSV audit, release artifact creation, and SHA-256 generation happen locally; do not add GitHub Actions.
- F-Droid lint passes — anti-feature flag for the YT downloader is documented in `metadata/`. Re-verify on each release. Crash-log local-file disclosure is documented in README and F-Droid metadata.
- AAB for Play Store, signed APK for GitHub Releases; never ship unsigned artifacts.
- F-Droid users expect APK under **~40 MB**. Any TFLite-model or Matter-SDK work must respect this budget (downloadable models, not bundled).

### i18n / l10n

- English-only today. **Per-app language picker (L-U5) lands first**, THEN community translation. No machine-translation-only strings — better to remain English than ship broken translations.
- `Configuration` change tests when M3 Expressive + per-app locale stack: confirm `Compose` recomposes correctly via `LocalConfiguration`.

### Testing

- Unit tests cover: `NextAlarmCalculator`, `VacationAlarmPolicy`, `MissedAlarmReplayPolicy`, `ProximityCoverDetector`, `AlarmShareCodec`, `EncryptedBackupCodec`, `WakeStreakCalculator`, `WebhookUrl`, `ChallengeGenerator` + maze solver, `StatsFilters`, `NextAlarmNotificationTiming`. **Each new dismiss challenge must come with a unit-tested "valid input" + "invalid input" suite.**
- Room migration tests: every schema bump requires a migration test path in `AlarmDatabaseMigrationTest`; CI also runs `git diff --exit-code -- app/schemas` after debug builds to catch uncommitted exports (whakaara discipline — [ahudson20/whakaara](https://github.com/ahudson20/whakaara)).
- Remaining alarm-fire proof gap: add a device/emulator smoke that fires through AlarmManager/test broadcast and asserts the firing window shows over lock screen. **S, not yet tiered.**
- Add a `sanitized()` round-trip property test that asserts every value in `ChallengeType.entries.map(Enum::name)` is preserved through `Alarm.sanitized()`. Directly prevents the N1 class of regression in the future.

### Documentation

- README, CHANGELOG, ROADMAP, and the version badge must all match on every release. **N10 makes this enforced in CI instead of manual.**
- Add a CONTRIBUTING.md (currently absent) — blocked by current markdown hygiene until the repository permits that file.

### Plugin ecosystem

- Webhooks (Tasker / MacroDroid / Home Assistant) cover the integration surface we want to expose. A "real" plugin SDK is rejected (UC) until webhook gaps are documented.
- Recipe library (L-R6 + L-SH2) — blocked by current markdown hygiene until the repository permits integration docs.

---

## Research sources (round 5 — refreshed 2026-05-16)

### Direct OSS competitors

- **yuriykulikov/AlarmClock** — https://github.com/yuriykulikov/AlarmClock — 612★, AOSP-derived. Signature feature: pre-alarm low-volume gentle wake (L-A10); long-press dismiss; adjustable snooze picker.
- **FossifyOrg/Clock** — https://github.com/FossifyOrg/Clock — beta 1.6.0 (Feb 2026). Switches replacing checkboxes, "About" back in options menu, Android 7 support dropped.
- **BlackyHawky/Clock** — https://github.com/BlackyHawky/Clock — v2.29 (Apr 2026), v2.30 in nightly. Per-version harvest applied to this roadmap pass: pause-alarms (N6), manual drag-reorder (X15), sync alarms (L-H6), Direct-Boot fallback (shipped v1.15.2), BT routing (L-A5), folder ringtones (L-A8), per-alarm background (X14), vibration delay (N7), missed-timer notif (N8), ExoPlayer (X16), custom fonts (UC).
- **LineageOS DeskClock** — https://github.com/LineageOS/android_packages_apps_DeskClock
- **AOSP DeskClock** — https://android.googlesource.com/platform/packages/apps/DeskClock/ — gold-standard alarm state machine.
- **ahudson20/whakaara** — https://github.com/ahudson20/whakaara — 51★ (May 2026). Reference for Room migration discipline + Kover code-coverage workflow.
- **yassineAbou/Clock** — https://github.com/yassineAbou/Clock — pure-Compose, single-activity, WorkManager-backed timer/stopwatch persistence.
- **fennifith/Alarmio** — https://github.com/fennifith/Alarmio
- **akshay2211/JetAlarm** — https://github.com/akshay2211/JetAlarm
- **CCExtractor/ultimate_alarm_clock** — https://github.com/CCExtractor/ultimate_alarm_clock — 108★, Flutter. Shared cloud alarms (REJECTED — H19), QR-scan dismiss, weather-based alarm.
- **sweakpl/qralarm-android** — https://github.com/sweakpl/qralarm-android — 323★, v2.9.3 (May 2026). Single-purpose QR dismiss.
- **WrichikBasu/ShakeAlarmClock** — https://github.com/WrichikBasu/ShakeAlarmClock
- **meenbeese/Chronos** — https://github.com/meenbeese/Chronos
- **meticha/triggerx** — https://github.com/meticha/triggerx — alarm-execution library, ~101★.
- **lemma-io/vivify** — https://github.com/lemma-io/vivify — open-source Spotify-connected alarm reference for L-A.
- **plusmobileapps/alarm-clock** — https://github.com/plusmobileapps/alarm-clock
- **vicolo-dev/chrono** — https://github.com/vicolo-dev/chrono — Flutter UX study target.
- **kunal-mahatha/Early-Bird-App** — https://github.com/kunal-mahatha/Early-Bird-App
- **giorgosneokleous93/fullscreenintentexample** — https://github.com/giorgosneokleous93/fullscreenintentexample

### Commercial reference

- **Alarmy** — https://alar.my/en/blog/alarmy-wake-up-mission — Multiple Mission feature is parity for our Mission Chain. Photo, Math, Shake, Barcode/QR, Memory, Typing, Steps, Squats (premium). Wake-Up Check feature is paywalled — ACX matches free via existing F5 / N1.
- **Sleep as Android** — https://sleep.urbandroid.org/documentation/release-notes/ — 2025 additions: Google Home API (BETA), AI Sleep Assistant (BETA), HRV gain cards, dashboard redesign, wake-up-check automation, Lullabies addon. AI sound detection: https://sleep.urbandroid.org/new-sleep-sound-detection/
- **Sleep Cycle** — https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk — 2026 SDK release; phone mic + accelerometer detects sleep stages and fires alarm in lightest phase within wake window. Algorithm reference for smart-wake logic.
- **Rise** — https://www.risescience.com/ — sleep-debt accumulator + composite score reference for X4 / X5.
- **Pillow** — https://www.pillow.app/ — actigraphy reference (iOS-only).
- **Turbo Alarm** — https://play.google.com/store/apps/details?id=com.turbo.alarm — Spotify-as-alarm, Wear OS support, talking alarm, sunrise simulation, mini-game dismiss, "Anti-Sleepyhead Security" (L-P10), cloud-sync, Tasker / Macrodroid / Sleepbot integration.
- **Google Clock** — https://play.google.com/store/apps/details?id=com.google.android.deskclock — Pixel-exclusive Sunrise Alarm + Bedtime tab reference.
- **I Can't Wake Up** — Simon-says and voice-phrase reference; voice phrase shipped in v1.15.3.
- **Timeshifter** — https://www.timeshifter.com/ — jet-lag re-entrainment reference for L-WS2.
- **Supershift** — https://supershift.app/ — shift-pattern reference for L-WS1 (DDNNO / 4-on-4-off / Panama / DuPont / Pitman).
- **Pixel Bedtime mode** — https://support.google.com/pixelphone/answer/9887159 — L-S11 reference.
- **Apple AlarmKit (iOS 26 WWDC25)** — https://developer.apple.com/documentation/AlarmKit — cross-platform UX-pattern study (UC).

### Awesome lists / FOSS catalogs

- GitHub topics: https://github.com/topics/alarm-clock?l=kotlin and https://github.com/topics/sleep-tracker
- F-Droid Clocks & Alarms: https://f-droid.org/en/categories/clock/
- IATkachenko/HA-SleepAsAndroid (Home Assistant integration) — https://github.com/IATkachenko/HA-SleepAsAndroid
- XADE awesome-android — https://codeberg.org/XADE/awesome-android
- binaryshrey/Awesome-Android-Open-Source-Projects — https://github.com/binaryshrey/Awesome-Android-Open-Source-Projects

### Platform docs / standards / specs

- Android 14 behavior changes — https://developer.android.com/about/versions/14/behavior-changes-14
- Android 15 behavior changes — https://developer.android.com/about/versions/15/behavior-changes-15
- Android 15 features — https://developer.android.com/about/versions/15/features
- Android 16 features — https://developer.android.com/about/versions/16/features
- Android 16 Live Updates / `ProgressStyle` — https://developer.android.com/about/versions/16/features/progress-centric-notifications
- Android 16 article (Wikipedia, install-base) — https://en.wikipedia.org/wiki/Android_16
- Android 17 Beta 3 release notes — https://developer.android.com/about/versions/17/release-notes
- Material 3 Expressive — https://m3.material.io/blog/material-3-expressive
- Compose Material 3 — https://developer.android.com/jetpack/androidx/releases/compose-material3
- Compose Material 3 Adaptive — https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- NavigationSuiteScaffold — https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- Wear OS Tiles API — https://developer.android.com/training/wearables/tiles
- Wear OS Complications API — https://developer.android.com/training/wearables/complications
- Glance — https://developer.android.com/jetpack/androidx/releases/glance
- Glance Wear — https://developer.android.com/jetpack/androidx/releases/glance-wear
- Health Connect Sleep — https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions
- Health Connect Develop Sleep Experiences — https://developer.android.com/health-and-fitness/health-connect/experiences/sleep
- Health Connect get-started — https://developer.android.com/health-and-fitness/health-connect/get-started
- Play Console health permissions FAQ — https://support.google.com/googleplay/android-developer/answer/12991134?hl=en
- Play Console policy April 15 2026 — https://support.google.com/googleplay/android-developer/answer/16926792?hl=en
- ML Kit Digital Ink — https://developers.google.com/ml-kit/vision/digital-ink-recognition
- LE Audio (Android 13+) — https://source.android.com/docs/core/connect/bluetooth/le_audio
- BLE Audio overview — https://developer.android.com/develop/connectivity/bluetooth/ble-audio/overview
- AutomaticZenRule v2 — https://developer.android.com/reference/android/app/AutomaticZenRule
- Predictive back — https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture
- Compose Predictive Back — https://developer.android.com/develop/ui/compose/system/predictive-back
- LocaleManager — https://developer.android.com/about/versions/13/features/app-languages
- Credential Manager — https://developer.android.com/training/sign-in/passkeys
- `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` — https://developer.android.com/reference/android/app/AlarmManager#ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
- App Standby Buckets — https://developer.android.com/topic/performance/appstandby
- Optimize for Doze and App Standby — https://developer.android.com/training/monitoring-device-state/doze-standby
- Telephony state (`EXTRA_STATE_RINGING`) — https://developer.android.com/reference/android/telephony/TelephonyManager#EXTRA_STATE_RINGING
- Direct Boot — https://developer.android.com/about/versions/14/direct-boot
- Open-Meteo Weather — https://open-meteo.com/
- Open-Meteo Air Quality + pollen — https://open-meteo.com/en/docs/air-quality-api
- Open-Meteo seasonal forecast 2026 — https://openmeteo.substack.com/p/seasonal-weather-forecasts
- NWS Active Alerts — https://www.weather.gov/documentation/services-web-api
- Nager.Date holidays — https://date.nager.at/
- Aladhan prayer times — https://aladhan.com/prayer-times-api
- Matter 1.6 Dynamic Lighting (sunrise gradient) — https://mattressmiracle.ca/blogs/mattress-miracle-blog/matter-1-6-dynamic-lighting-sunrise-gradient-bedroom
- Matter Smart Home (CES 2026) — https://matter-smarthome.de/en/products/the-matter-innovations-at-ces-2026/
- Google Home Matter dev docs — https://developers.home.google.com/matter
- Philips Hue API v2 — https://developers.meethue.com/new-hue-api/
- Apple AlarmKit — https://developer.apple.com/documentation/AlarmKit

### Academic / industry / engineering

- Cole-Kripke 1992 — https://pubmed.ncbi.nlm.nih.gov/1455130/
- Roenneberg MEQ — https://www.thewep.org/documentations/mctq
- Horne-Östberg MEQ calculator — https://qxmd.com/calculate/calculator_829/morningness-eveningness-questionnaire-meq
- Springer 2025 (smartwatch IMU OSA) — https://link.springer.com/article/10.1007/s11325-025-03255-w
- Apneal 2025 (smartphone OSA prediction) — https://link.springer.com/article/10.1007/s11325-025-03441-w
- Samsung × Stanford OSA collab 2025 — https://www.samsungmobilepress.com/articles/samsung-announces-collaboration-with-stanford-medicine-to-advance-sleep-apnea-detection-and-beyond
- AASM smartwatch sleep features comparison — https://aasm.org/comparing-sleep-features-of-popular-smartwatches/
- Sleep Cycle SDK announcement — https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk
- Smart alarm tinyML (sleep-stage prediction in embedded systems) — https://github.com/cargilgar/Smart-Alarm-using-tinyML
- Smart alarm based on sleep stages prediction (IEEE 2020) — https://ieeexplore.ieee.org/document/9176320/
- SlumberNet (Nature Sci. Reports 2024) — https://www.nature.com/articles/s41598-024-54727-0
- Edge Impulse snoring on smartphone — https://github.com/edgeimpulse/expert-projects/blob/main/audio-projects/snoring-detection-on-smartphone.md

### Community signal

- r/Android complaints (recurring): missed alarms on Xiaomi/Samsung/Oppo battery-management; subscription fatigue (Alarmy / Sleep as Android Premium); Google Clock missing skip-one-occurrence + mission challenges; Pixel "missed alarm — unknown reason" notification regression (Android 16). Sources: [howtogeek.com — Pixel alarms keep breaking](https://www.howtogeek.com/google-pixel-phone-alarm-app-not-working-again/); [androidpolice.com — Pixel alarm bug is back](https://www.androidpolice.com/pixel-alarm-bug-is-back/); [TechRadar — fix Android alarm clock bug](https://www.techradar.com/how-to/how-to-fix-the-android-alarm-clock-bug-so-you-wake-up-on-time)
- Maker complaints (Hacker News / accessibleandroid.com): mini-game dismiss is poorly TalkBacked across the field; Turbo Alarm called out specifically. Accessibility-first dismiss alternatives (haptic, voice, screen-flash) are differentiators.
- dontkillmyapp.com — https://dontkillmyapp.com/ — per-OEM background-execution guidance still actively updated 2026.
- Privacy Guides community — Alarmy permissions discussion — https://discuss.privacyguides.net/t/can-i-mitigate-some-of-the-privacy-issues-of-the-android-app-alarmy-by-removing-network-permission/24492

### Library changelogs to mine each release

- `androidx.work:work-runtime-ktx` — https://developer.android.com/jetpack/androidx/releases/work
- `androidx.glance:glance-appwidget` — https://developer.android.com/jetpack/androidx/releases/glance
- `androidx.glance:glance-wear-tiles` — https://developer.android.com/jetpack/androidx/releases/glance-wear
- `androidx.compose.material3` — https://developer.android.com/jetpack/androidx/releases/compose-material3
- `androidx.compose.material3.adaptive` — https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- `androidx.health.connect:connect-client` — https://developer.android.com/jetpack/androidx/releases/health-connect
- `com.patrykandpatrick.vico` — https://github.com/patrykandpatrick/vico/releases
- yt-dlp — https://github.com/yt-dlp/yt-dlp/releases
- NewPipeExtractor — https://github.com/TeamNewPipe/NewPipeExtractor/releases
- OkHttp / Retrofit / Moshi / Hilt / Room — keep current via quarterly audit.

### Legal / compliance flags to budget before touching

- Health Connect (N12 / X1) — code and privacy policy now describe Play-only `READ_SLEEP`; Play Console health-permissions declaration/approval must still precede Play Store distribution. ([Play Console policy](https://support.google.com/googleplay/android-developer/answer/16926792?hl=en))
- Apnea event flagging (L-S7) — explicit "screening, not a medical device" disclaimer; consider keeping it `play`-flavor only for legal hygiene.
- Power-off alarm (L-P1) — per-OEM privileged partner programs; may never be achievable for an indie app.
- Partner-phone / paired-phone sync (L-H2 / L-C2) — explicit threat model doc before code.
- LLM sleep-coach (L-S9) — bundled model size budget; F-Droid users expect <40 MB APK.
- Matter SDK (L-SH1) — adds dependency surface; verify F-Droid compatibility (Google Play Services-free build path).
- Wake-lock budget (N4) — Play Store quality treatment policy (March 2026). ([9to5Google](https://9to5google.com/2026/03/05/google-starts-calling-out-android-apps-that-drain-your-battery-before-you-download-them/))

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

### P1 — Reliability / correctness


### P2 — Accessibility / platform / polish

## Research-Driven Additions — Pass 2 (2026-07-12, subsystem audit)

Second 2026-07-12 pass auditing the timer/stopwatch/Sonar/news/restore
subsystems the prior pass skipped, plus net-new ecosystem opportunities.
Deduplicated against all existing ROADMAP.md / Roadmap_Blocked.md items and
against already-shipped features (mission chaining, wake-confirm, holiday
auto-skip, vibrate-only). See RESEARCH.md for evidence detail.

### P1 — Reliability / correctness / data-safety

### P2 — Correctness / reliability / platform





### P3 — Nice-to-have / polish / hygiene

## Research-Driven Additions — Pass 3 (2026-07-12, quality/i18n/performance)

Third 2026-07-12 pass covering the cross-cutting dimensions the prior two
(alarm-firing correctness; subsystem reliability) under-weighted. Both items are
verified against current code and deduplicated against L-U5 (per-app-language
picker), the i18n cross-cutting note, and the blocked Baseline-Profile item.

## Research-Driven Additions

### P0 — Now

### P1 — Next

### P2 — Later

### P3 — Under Consideration

## Research-Driven Additions — Pass 4 (2026-07-22, post-v1.15.30 reliability & platform)

All 2026-07-14 RESEARCH.md findings are now fixed (verified against live code
2026-07-22); this pass is grounded in fresh competitor/platform/community
research and current-code verification. Deduplicated against every prior ROADMAP
and Roadmap_Blocked item. Full evidence in RESEARCH.md.

### P2 — reliability / platform / UX

- [ ] P2 — Media3 alarm-audio stall detection
  Why: the Media3 ring path has no stall/timeout detection, so a stalled ring
  relies only on the delayed backup-sound escalation to recover.
  Evidence: Media3 1.9 `StuckPlayerException` + stalled-ready timeouts
  (developer.android.com/jetpack/androidx/releases/media3); ACX on Media3 1.10.1.
  Touches: `service/AlarmService.kt` audio path, `service/AlarmAudioRouting.kt`.
  Acceptance: a stalled/failed player is detected within a bounded window and
  escalates immediately (built-in speaker + max volume, then legacy fallback)
  rather than waiting for the backup-sound timer; incident reason code recorded.
  Complexity: M.

- [ ] P2 — Snooze to a specific time (scheduled snooze)
  Why: snooze is fixed-interval + progressive only; users want to re-fire at a
  chosen clock time (e.g. "again at 07:15").
  Evidence: yuriykulikov/AlarmClock; vicolo-dev/chrono.
  Touches: `service/AlarmService.kt` snooze path, `ui/alarmfiring/AlarmFiringActivity.kt`.
  Acceptance: the firing screen offers a "snooze until…" time picker that arms an
  exact re-fire at the chosen time; round-trips through the existing snooze
  scheduling and survives process death.
  Complexity: M.

- [ ] P2 — Extend Live Updates (ProgressStyle) to the snooze countdown
  Why: ACX already uses Android 16 `Notification.ProgressStyle` for the bedtime
  countdown only; the snooze interval is an ideal second start-to-end journey.
  Evidence: developer.android.com/about/versions/16/features/progress-centric-notifications.
  Touches: snooze notification path in `service/AlarmService.kt`, notification builders.
  Acceptance: while snoozed, an ongoing progress notification shows time-until-
  re-fire; clears on re-fire/dismiss; gated to API 36+ with graceful fallback.
  Complexity: M.

- [ ] P2 — OEM reliability doctor (per-manufacturer deep-links + post-OTA re-check)
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

### P3 — polish / UX

- [ ] P3 — Reduce ring volume while solving a dismiss challenge (opt-in)
  Why: a lower ring during a math/typing/maze mission lets users concentrate;
  Media3 1.10 `mute()`/`unmute()` is now stable, making it cheap.
  Evidence: vicolo-dev/chrono; Media3 1.10 (developer.android.com/jetpack/androidx/releases/media3).
  Touches: `ui/alarmfiring/AlarmFiringActivity.kt`, `service/AlarmService.kt`.
  Acceptance: an opt-in per-alarm/global toggle drops ring volume while a
  challenge is active and restores it on solve/fail; the backup-sound escalation
  still fires so a user cannot fall back asleep in silence. Default off.
  Complexity: S.

- [ ] P3 — Random ringtone start position
  Why: starting a ringtone at a random offset each fire keeps long-time users
  from habituating to the same opening seconds.
  Evidence: vicolo-dev/chrono.
  Touches: `service/AlarmService.kt` startAudio, per-alarm setting.
  Acceptance: an opt-in per-alarm flag seeks the ring player to a random valid
  offset at fire time; ignored for streams/short tones; round-trips through backup.
  Complexity: S.
