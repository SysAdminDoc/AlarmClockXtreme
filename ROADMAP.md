# AlarmClockXtreme Roadmap

Living feature backlog, refreshed alongside **v1.13.1** (Health Connect
opt-in scaffold + privacy-policy update; see [CHANGELOG.md](CHANGELOG.md)).
Deep research refresh: **2026-05-17**. Durable research artifacts live in
[.ai/research/2026-05-17](.ai/research/2026-05-17), and consolidated project
memory lives in [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md).

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

> **Recently shipped** (rolled up from prior tiers): **v1.13.1 Health Connect
> opt-in scaffold + privacy-policy update (N12 + N13 scaffold)**; **v1.13.0
> adaptive NavigationRail on MEDIUM/EXPANDED widths (N11)**; **v1.12.3 CI
> version-line consistency lint (N10)**; **v1.12.2 RingtonePool chip-based
> editor (N9)**; **v1.12.1 missed-timer notification (N8)**; **v1.12.0 per-alarm vibration start-delay (N7) —
> DB v10**; **v1.11.6 "Pause alarms" single-tap suspend (N6)**; **v1.11.5 Philips Hue API v2 migration (N5)**; **v1.11.4
> Play wake-lock policy audit (N4)**; **v1.11.3 App Standby bucket awareness
> (N3)**; **v1.11.2 telephony-aware alarm muting (N2)**; **v1.11.1 challenge
> sanitization regression fix (N1)**; v1.11.0 Wear OS
> next-alarm tile with skip / snooze / dismiss actions; v1.10.10 Android 16
> next-alarm Live Update (`Notification.ProgressStyle`); v1.10.9 Material 3
> Expressive opt-in (Compose BOM 2026.05.00); v1.10.8 What's New roadmap
> handoff; v1.10.7 wake-streak flame badge; v1.10.6 first-meeting Calendar
> auto-alarm refreshes; v1.10.5 app-owned Bedtime DND rule
> (`ConditionProviderService`); v1.10.4 haptic-only "Don't wake partner"
> profile; v1.10.3 per-alarm hold-to-dismiss; v1.10.2 long-press firing-screen
> snooze picker; v1.10.1 Weather tab Open-Meteo air-quality + pollen; v1.10.0
> BootReceiver handoff to batched WorkManager reschedule.

---

## 2026-05-17 Deep Research Refresh

This section supersedes the older v1.12/v1.13 candidate queue below. The older
queue is retained for traceability because it records shipped items and source
links.

### Hard Findings From Local Evidence

- `.github/workflows/release.yml` was repaired in the v1.13.2 automation pass:
  tag builds now require signing secrets, build signed Play/F-Droid/Wear release
  APKs, verify signatures/metadata, and upload SHA-256 hashes.
- `PRIVACY_POLICY.html`, README privacy text, Settings Health Connect copy, and
  F-Droid metadata were reconciled in the v1.13.2 docs pass. Current public
  language now distinguishes developer collection from direct optional
  third-party requests and states that Health Connect is only a local opt-in
  preference until the SDK path ships.
- Room is v10 with `exportSchema = true`; the v1.13.2 schema-gate pass added
  migration instrumentation tests and CI schema-drift checks.
- The Play flavor downloader dependency tree was constrained in the v1.13.2
  dependency-hardening pass: Jackson 2.18.6, Commons Compress 1.28.0, Commons
  IO 2.20.0, Rhino 1.8.1, and Play-only XZ support now resolve cleanly through
  the local OSV audit script and CI dependency-audit job. The F-Droid flavor
  still excludes this path.
- The Health Connect feature is an opt-in scaffold only: DataStore + settings +
  backup copy exist, but the SDK dependency, manifest permission, permission
  prompt, repository, and Bedtime/Stats reads are not wired.
- Latest local tag is `v1.9.5`; current app version is `v1.13.1`.

### NOW - v1.13.2 Trust, Release, And Data-Safety Gate

| # | Item | Evidence | Effort | Rationale |
|---|------|----------|--------|-----------|
| R1 | [x] Privacy policy and data-safety reconciliation — **shipped v1.13.2 docs pass.** `PRIVACY_POLICY.html`, README privacy text, and Settings Health Connect copy now enumerate optional network/data surfaces, distinguish developer collection from user-triggered third-party requests, and state that v1.13.1 stores only a local Health Connect opt-in preference until the SDK path ships. | local: `PRIVACY_POLICY.html`, `README.md`, `SettingsScreen.kt`; [Play Health Connect publishing](https://developer.android.com/health-and-fitness/health-connect/publish), [Play sensitive permissions policy](https://support.google.com/googleplay/android-developer/answer/9888170) | S | This closes the highest-trust documentation gap before requesting Health Connect review. |
| R2 | [x] Repair release automation — **shipped v1.13.2 automation pass.** Tag builds now require signing secrets, build signed `playRelease`, `fdroidRelease`, and `:wear:assembleRelease`, reject tag/version mismatches, verify APK signatures and badging, upload workflow artifacts, and attach APKs plus `SHA256SUMS.txt` to GitHub Releases. | local: `.github/workflows/release.yml`; prior release memory; [Android build release docs](https://developer.android.com/build/releases/gradle-plugin) | M | Release tags no longer publish debug APKs. |
| R3 | [x] Refresh F-Droid metadata and anti-feature declarations — **shipped v1.13.2 docs pass.** Both metadata files now point to version `1.13.1` / code `66`, describe the F-Droid flavor exclusions, and disclose optional network surfaces under `NonFreeNet`. | local: `metadata/com.sysadmindoc.alarmclock.yml`, `metadata/en-US/fdroid.yml`; [F-Droid Anti-Features](https://f-droid.org/en/docs/Anti-Features/) | S | Current repo metadata no longer advertises old app versions or Open-Meteo-only network language. |
| R4 | [x] Add Room migration/schema gate — **shipped v1.13.2 schema-gate pass.** Added `AlarmDatabase.ALL_MIGRATIONS`, Android instrumentation tests for earliest-exported-schema→latest, 9→10 defaulting, fresh-install/schema parity, and migration contiguity. Added `.github/workflows/android-ci.yml` to run unit tests/debug builds, fail on uncommitted `app/schemas` drift, and run the Room migration tests on an emulator. | local: `AlarmDatabase.kt`, `DatabaseModule.kt`, `AlarmDatabaseMigrationTest.kt`, `app/schemas`, `.github/workflows/android-ci.yml`; [Room release notes](https://developer.android.com/jetpack/androidx/releases/room) | S | Future DB bumps now have both migration validation and an exported-schema commit gate. |
| R5 | [x] Mitigate Play-only downloader dependency risk - **shipped v1.13.2 dependency-hardening pass.** Added Play-only constraints for stale downloader transitives (`jackson-* 2.18.6`, `commons-compress 1.28.0`, `commons-io 2.20.0`, `rhino 1.8.1`), added Play-only `org.tukaani:xz:1.10` support for release shrinking, documented the optional Commons Compress Zstandard warning in R8 rules, and added `scripts/osv_gradle_audit.py` plus a CI dependency-audit job. | local: `app/build.gradle.kts`, `app/proguard-rules.pro`, `scripts/osv_gradle_audit.py`, `.github/workflows/android-ci.yml`; [OSV API](https://api.osv.dev/v1/querybatch), [NewPipeExtractor releases](https://github.com/TeamNewPipe/NewPipeExtractor/releases), [yt-dlp](https://github.com/yt-dlp/yt-dlp) | M | `playDebugRuntimeClasspath` and `playReleaseRuntimeClasspath` now report no OSV vulnerabilities for resolved Maven dependencies; the release R8 path passes with a temporary signing key. |
| R6 | [x] Clean factual doc drift in README/CLAUDE and release notes - **shipped v1.13.2 doc-policy pass.** README, `PROJECT_CONTEXT.md`, and `CHANGELOG.md` now reflect DB v10, 22 challenge flows, backup format v7, the current Health Connect no-read scaffold, and the resolved ignored-tool-file policy. Decision: do not force-add ignored local `AGENTS.md` / `CLAUDE.md`; tracked context files are authoritative when local tool notes conflict. | local: `README.md`, ignored `AGENTS.md`, ignored `CLAUDE.md`, `PROJECT_CONTEXT.md`, `CHANGELOG.md`, `MEMORY_CONSOLIDATION.md` | S | Reduces future agent mistakes and keeps public docs aligned with live code without committing local scratch instruction files. |

### NEXT - Product Differentiation After The Trust Gate

| # | Item | Evidence | Effort | Rationale |
|---|------|----------|--------|-----------|
| X1 | [ ] Complete Health Connect sleep-session SDK integration behind the Play flavor. Request only `READ_SLEEP`, read recent `SleepSessionRecord` windows, and surface local-only summaries in Bedtime/Stats. | [Track sleep sessions](https://developer.android.com/health-and-fitness/guides/health-connect/develop/sleep-sessions), [Develop sleep experiences](https://developer.android.com/health-and-fitness/health-connect/experiences/sleep) | M | Best next user-value feature after policy/docs are accurate. |
| X2 | [ ] Backup-export warning when configured secrets or private integration URLs are present. | local: `BackupManager.kt`, `SettingsScreen.kt` | S | Trust-critical and small. Warn before exporting webhooks, Hue keys, feeds, or local paths. |
| X3 | [ ] Wear OS next-alarm complication data source. | [Wear complications](https://developer.android.com/training/wearables/exposing-data-complications), [Wear Tiles](https://developer.android.com/training/wearables/tiles) | M | Builds on the existing Wear tile and fills a common glanceable-surface expectation. |
| X4 | [ ] Direct Boot minimum alarm support design and prototype. Keep only the minimum schedule/ringtone/defaults in device-encrypted storage. | [Direct Boot docs](https://developer.android.com/privacy-and-security/direct-boot) | L | High reliability value, but requires careful storage separation and recovery UX. |
| X5 | [ ] Local crash/support export. Add a Settings action to package local crash logs and version/device alarm diagnostics without telemetry. | local: `CrashLogger.kt`, Settings reliability cards | S | Improves support while preserving no-tracking posture. |
| X6 | [ ] Sleep/wake analytics charts after Health Connect lands. Correlate sleep duration, snooze count, dismiss time, wake-streak, and challenge retries locally. | Health Connect docs; local Room/DataStore history | M | Competes with commercial insight surfaces without cloud upload. |

### Research Packet

- State of repo: [.ai/research/2026-05-17/STATE_OF_REPO.md](.ai/research/2026-05-17/STATE_OF_REPO.md)
- Memory reconciliation: [.ai/research/2026-05-17/MEMORY_CONSOLIDATION.md](.ai/research/2026-05-17/MEMORY_CONSOLIDATION.md)
- Sources: [.ai/research/2026-05-17/SOURCE_REGISTER.md](.ai/research/2026-05-17/SOURCE_REGISTER.md)
- Competitors: [.ai/research/2026-05-17/COMPETITOR_MATRIX.md](.ai/research/2026-05-17/COMPETITOR_MATRIX.md)
- Raw backlog: [.ai/research/2026-05-17/FEATURE_BACKLOG.md](.ai/research/2026-05-17/FEATURE_BACKLOG.md)
- Prioritization: [.ai/research/2026-05-17/PRIORITIZATION_MATRIX.md](.ai/research/2026-05-17/PRIORITIZATION_MATRIX.md)
- Security/dependencies: [.ai/research/2026-05-17/SECURITY_AND_DEPENDENCY_REVIEW.md](.ai/research/2026-05-17/SECURITY_AND_DEPENDENCY_REVIEW.md)
- Data/model/integrations: [.ai/research/2026-05-17/DATASET_MODEL_INTEGRATION_REVIEW.md](.ai/research/2026-05-17/DATASET_MODEL_INTEGRATION_REVIEW.md)

---

## Current snapshot (v1.13.1)

- **Stack:** Kotlin 2.1, AGP 8.11.1 / Gradle 8.13, Compose BOM 2026.05.00 /
  Material 3 (1.4.x), Room v10, Hilt 2.53.1, Retrofit 2.11 + Moshi (codegen),
  DataStore 1.1.1, Glance 1.1.1, OkHttp 4.12.0, WorkManager 2.9.1, Wear Tiles
  1.6.0 / protolayout 1.4.0, Wear Data Layer, yt-dlp (`youtubedl-android`
  0.18.1) + NewPipe Extractor 0.24.8 (Play flavor only).
- **Targets:** `minSdk 26`, `targetSdk 35`, `compileSdk 36`,
  `versionCode 66`, `versionName 1.13.1`.
- **Surface area:** 120 Kotlin files in `:app` + 3 in `:wear`, two phone
  flavors (`play`, `fdroid`), **22 user-facing dismiss challenges** (all now
  whitelisted by `Alarm.sanitized()` after N1), 50+ alarm fields, 35+
  AppSettings fields, 6 phone tabs (Today, Alarms, Bedtime, Timer, World,
  News) + Settings.
- **What's missing vs. competitors:** Health Connect / standalone-watch story
  is still zero; no on-device sleep-stage classifier; no AI sleep coach; no
  lockscreen-widget surface (Pixel-led Android 15+); no foldable/tablet
  adaptive layout; no Direct-Boot alarm; no ExoPlayer audio path; no
  on-device snore detection. The good news: the alarm-clock core
  (scheduling, reliability, challenges, weather, bedtime DND, encrypted
  backup) is best-in-class for FOSS Android.

---

## Historical NOW - v1.12/v1.13 Shipped Cycle

This older queue is retained for traceability. Use the 2026-05-17 section above
for active priority order.

Highest impact-to-effort with the existing stack. Each is scoped to land
without breaking schema or flavor parity; schema-changing items call that out
in their notes. **Order is the recommended landing order.**

| # | Item | Source | Effort | Rationale |
|---|------|--------|--------|-----------|
| N1 | [x] Fix v1.6.0 challenge sanitization regression — **shipped v1.11.1.** Added `ROCK_PAPER_SCISSORS`, `EMOJI_MEMORY`, `TYPING_SPEED`, `WORDLE` to `Alarm.VALID_CHALLENGE_TYPES`; added `AlarmTest.every ChallengeType survives sanitized round-trip` property test that iterates `ChallengeType.entries` and fails fast if the whitelist drifts; README dismiss-challenges table updated to 22 types. | local: [app/src/main/java/com/sysadmindoc/alarmclock/data/model/Alarm.kt:114](app/src/main/java/com/sysadmindoc/alarmclock/data/model/Alarm.kt) vs. [challenges/ChallengeGenerator.kt:5](app/src/main/java/com/sysadmindoc/alarmclock/ui/alarmfiring/challenges/ChallengeGenerator.kt) | S | Latent data-loss bug; affected the 4 v1.6.0 challenges on every backup/share/DataStore round-trip. |
| N2 | [x] Telephony-aware alarm muting — **shipped v1.11.2.** `AlarmService` registers `TelephonyCallback.CallStateListener` (API 31+) / legacy `PhoneStateListener` (pre-31) on `startAudio()` and unregisters in `stopAlarmPlayback()` / `onDestroy()`. On `OFFHOOK` / `RINGING` the MediaPlayer is muted via `setVolume(0f, 0f)`; restored on `IDLE`. All three creation paths + gradual-volume coroutine + backup-sound escalation honour the flag. Vibration / Guardian / flashlight intentionally continue. | [Android telephony state reference](https://developer.android.com/reference/android/telephony/TelephonyManager#EXTRA_STATE_RINGING) | S | Zero new permissions — only the optional incoming-number argument requires `READ_PHONE_STATE`, which is never read. |
| N3 | [x] App-Standby bucket awareness banner in Settings → Reliability — **shipped v1.11.3.** Added a 4th Wake-readiness row that reads `UsageStatsManager.getAppStandbyBucket()` on API 28+ and shows a plain-English description of the throttling state. `FREQUENT`/`RARE`/`RESTRICTED` get a warning with an action that opens battery settings. The "X of N ready" pill and the Reliability tile's supporting line both recompute. | [App Standby Buckets docs](https://developer.android.com/topic/performance/appstandby) | S | No new permission — self-query works without `PACKAGE_USAGE_STATS`. Pre-API-28 hides the row. |
| N4 | [x] Play Store wake-lock budget compliance audit — **shipped v1.11.4.** Both wake-lock acquisition sites annotated. AlarmService (30 min, `mediaPlayback` + USAGE_ALARM) is exempt. SmartAlarmService (90 min, `dataSync` + accelerometer) is non-exempt and stays under the 2 h / 24 h cap for typical use; documented mitigation if multiple smart-wake alarms cumulate beyond. Wake-Lock Budget table added to CLAUDE.md. | [Play wake-lock policy March 2026](https://9to5google.com/2026/03/05/google-starts-calling-out-android-apps-that-drain-your-battery-before-you-download-them/) | S | Audit-only; no behavior change. |
| N5 | [x] Migrate `HueSunriseWorker` to Hue API v2 + HTTPS — **shipped v1.11.5.** Probe-on-first-run (`GET https://{ip}/clip/v2/resource/light/{rid}` with `hue-application-key` header) with verdict cached per bridge IP in SharedPrefs. v2 PUTs use CLIP v2 endpoint shape + body (`{"on":{"on":true},"dimming":{"brightness":0..100},"color_temperature":{"mirek":153..500}}`); v1 0–254 brightness scale converted to v2 percent. v1 HTTP fallback retained for ~6 months. Settings → Hue test now reports active API version. | [Philips Hue API v2](https://developers.meethue.com/new-hue-api/) | M | Bridge cert is self-signed with non-matching CN (bridge ID, not IP) — v2 client uses allow-all hostname verifier + trust-any-cert TrustManager. Threat model: LAN-only, same risk profile as v1 HTTP today. Future hardening: bundle Signify root CA + pin bridge ID. |
| N6 | [x] "Pause alarms for N days" single-tap action — **shipped v1.11.6.** New Settings → Pause alarms card with 1/3/7/14-day chips and Resume-now. `AppSettings.pauseUntilMillis` + `isPaused()` extension. `AlarmScheduler.schedule()` and `scheduleAt()` short-circuit while paused (cancel PendingIntent, zero `nextTriggerTime`). Backup format round-trips the field; stale values lazy-expire. | [BlackyHawky Clock 2.30 nightly](https://github.com/BlackyHawky/Clock/releases) | S | Distinct from vacation: applies to one-shots too; no date range required. |
| N7 | [x] Per-alarm vibration start-delay — **shipped v1.12.0.** New `Alarm.vibrationDelaySeconds: Int = 0` (clamped 0..600). DB v9→v10 with `MIGRATION_9_10`; backup format v6→v7 with default-0 back-compat read. Alarm-edit Vibration section shows the new "Start vibration after" picker. AlarmService schedules a cancellable delay job that re-checks `currentAlarmId` before vibrating. | [BlackyHawky Clock 2.25](https://github.com/BlackyHawky/Clock/releases) | S | Pairs with `gradualVolumeSeconds` for a "gentle wake" preset. |
| N8 | [x] Missed-timer notification — **shipped v1.12.1.** New `CHANNEL_TIMER` (`IMPORTANCE_HIGH`, channel sound/vibration off) registered in `AlarmService.createNotificationChannels()`. TimerViewModel posts a notification per finished timer (id 7000+timer.id) when the countdown transitions to `FINISHED`; tap opens MainActivity. `stop()` and `dismissFinished()` cancel it. Notifications intentionally survive `onCleared()` so the user-closed-the-app case is covered. | [BlackyHawky Clock 2.29](https://github.com/BlackyHawky/Clock/releases) | S | Symmetry with missed-alarm path; reuses already-granted POST_NOTIFICATIONS. |
| N9 | [x] RingtonePool chip-based editor — **shipped v1.12.2.** Replaced the textarea with a horizontally scrollable chip row in the Advanced section. Each pool URI renders as an `AppFilterChip` (tap to remove); an "Add" chip opens an `AlertDialog` for pasting a new URI with duplicate-protection. New `ringtoneShortName(uri)` helper trims content://media/external/audio/media/12345 → "audio/12345" without a per-render ContentResolver lookup. Storage format unchanged. | local: [AlarmEditScreen.kt:1247](app/src/main/java/com/sysadmindoc/alarmclock/ui/alarmedit/AlarmEditScreen.kt) | S | UX polish, no schema change. |
| N10 | [x] CI version-line consistency lint — **shipped v1.12.3.** New `.github/workflows/version-lint.yml` runs on every push/PR. Asserts six touchpoints agree with `app/build.gradle.kts`: the `:wear` module's `versionName`, the README badge, the README `adb install` snippet, the CHANGELOG top `## [x.y.z]` header, and the ROADMAP `## Current snapshot (vX.Y.Z)` header. Pure regex/grep — no JDK/Gradle bootstrap. | local: [.github/workflows/version-lint.yml](.github/workflows/version-lint.yml) | S | Prevents the recurring "README still says vX.Y.Z" post-release mismatch. |
| N11 | [x] Adaptive primary navigation — **shipped v1.13.0.** Used the lower-level `material3-window-size-class` API rather than `NavigationSuiteScaffold` so the existing custom `BottomNavContainer` treatment + accent colors carry through unchanged on phones. AppNavigation branches on width class: COMPACT keeps the bottom NavigationBar verbatim, MEDIUM/EXPANDED render a `NavigationRail` on the leading edge inside a Row. Extracted the per-route NavHost into a private `AppNavHost` to dedupe. | [Android Developers — adaptive navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation) | M | Phone UX is byte-for-byte identical. Defers dual-pane Alarms/Edit (X14 follow-on) to a later release. |
| N12 | [~] Health Connect Sleep Sessions — **scaffold shipped v1.13.1.** DataStore opt-in toggle (`healthConnectEnabled`), Settings → Health Connect card with flavor-aware copy, backup round-trip. Actual `androidx.health.connect:connect-client` SDK integration + Bedtime/Stats data surfacing deferred to a follow-up release pending Play Console health-permissions review. | [androidx.health.connect:connect-client docs](https://developer.android.com/health-and-fitness/health-connect); [Track sleep sessions](https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions) | M | Toggle survives backup so opted-in users light up immediately when the data path lands. |
| N13 | [~] Play Store data-safety + health-permission declaration refresh — **policy corrected in v1.13.2 docs pass.** `PRIVACY_POLICY.html` now accurately states that v1.13.1 stores only a local Health Connect opt-in preference and does not request permission or read data yet. Final Play Console health-permission wording remains tied to X1 when the SDK path actually ships. | [Play Health Connect publishing](https://developer.android.com/health-and-fitness/health-connect/publish), [Play Console policy April 15 2026](https://support.google.com/googleplay/android-developer/answer/16926792?hl=en) | S | Avoids overclaiming current Health Connect behavior while preserving the review prerequisite for the follow-up release that wires the SDK. |

## NEXT — v1.13 candidates

| # | Item | Source | Effort | Rationale |
|---|------|--------|--------|-----------|
| X1 | [ ] Wear OS "Next Alarm" complication (`androidx.wear.watchface.complications`) — pairs with the shipped Tile. | [Complications API](https://developer.android.com/training/wearables/complications); Pixel Watch users routinely add it | S | Same Data Layer payload as the tile; just adds a complication data source. |
| X2 | [ ] On-device actigraphy → Awake / Light / Deep buckets (Cole-Kripke 1992). Existing `SmartAlarmService` already collects accel — bucketize, persist, render. | [Cole-Kripke 1992 (PubMed)](https://pubmed.ncbi.nlm.nih.gov/1455130/); [Pillow app](https://www.pillow.app/); [Smart Alarm using tinyML on GitHub](https://github.com/cargilgar/Smart-Alarm-using-tinyML) | L | Ships independently of TFLite REM-stage classifier (that stays Later). Pure-Kotlin algorithm; no model file. |
| X3 | [ ] Smart-wake window (accel-based light-sleep firing within user-defined N-min window). | [Sleep Cycle SDK 2026](https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk); existing `Alarm.smartAlarmEnabled` field | M | Builds on X2. UI scaffolding already exists. |
| X4 | [ ] Composite sleep score (0–100 = duration × efficiency × regularity × stage balance). Ship a duration-only v1 sooner. | [Rise app](https://www.risescience.com/); Oura | S | Daily engagement hook. |
| X5 | [ ] Sleep-debt accumulator (rolling 14-day deficit vs. per-user need). | [Rise app](https://www.risescience.com/) | S | Surfaces naps + bedtime reminders more contextually. |
| X6 | [ ] Stats charts via Vico (`com.patrykandpatrick.vico` 2.x) — sleep score, snooze rate, streak, response-time histograms. | [Vico chart library](https://github.com/patrykandpatrick/vico) — last release 2026-05-04 | M | Stats tab is text-only today; charts move it from "log" to "feedback loop." |
| X7 | [ ] Voice-phrase dismiss challenge (offline `SpeechRecognizer`). | [Alarmy wake-up missions](https://alar.my/en/blog/alarmy-wake-up-mission); [I Can't Wake Up](https://play.google.com/store/apps/details?id=com.bartat.android.icwu) | M | Alarmy paywalls this; ACX can ship it free. Existing `RECORD_AUDIO` permission already requested for Sonar — make conditional. |
| X8 | [ ] Handwriting / drawing dismiss challenge (ML Kit Digital Ink). | [ML Kit Digital Ink Recognition](https://developers.google.com/ml-kit/vision/digital-ink-recognition); [googlesamples/mlkit](https://github.com/googlesamples/mlkit/tree/master/android/digitalink) | M | Pairs cleanly with the existing `Challenge` sealed-class; ML Kit on-device only. |
| X9 | [ ] Push-up / plank-hold accelerometer challenge (no camera). | [Alarmy premium missions](https://alar.my/en) | M | Reuses `SquatDetector` heuristics. |
| X10 | [ ] Spot-the-difference / chess-mate-in-1 / RSVP speed-reading — three more challenges to push the roster to 25+. | indie / NYT Games / research | M | Continues the differentiator over Alarmy's paywalled "Multiple Mission" tier. |
| X11 | [ ] Lockscreen-widget surface — Pixel Tablet today, Pixel phones next. Wraps the existing Glance widget with the lockscreen receiver intent + a smaller size class. | [Android 15 lockscreen widgets](https://developer.android.com/about/versions/15/features); [Widgets on lock screen FAQ](https://android-developers.googleblog.com/2025/03/widgets-on-lock-screen-faq.html) | S | Glance Widget is already published; this is a thin wrapper. |
| X12 | [ ] LE Audio hearing-aid routing (`AudioAttributes.USAGE_ALARM` + `MediaRouter2`). Android 17 adds [system-level granular hearing-aid routing](https://www.androidpolice.com/android-15-hearing-aid-support-le-audio/) — verify ACX honors it. | [Android Bluetooth LE Audio overview](https://developer.android.com/develop/connectivity/bluetooth/ble-audio/overview); [Hearing aid audio support via Bluetooth LE](https://source.android.com/docs/core/connect/bluetooth/asha) | M | Underserved accessibility surface; LE Audio hearing aids ignore most alarm streams today. |
| X13 | [ ] Public-transit-aware alarm (shift earlier when commute time grows or weather degrades). | open routing — Google Maps Distance Matrix or [OpenRouteService](https://openrouteservice.org/) | L | Listed Later previously — promote now that the weather/calendar plumbing can chain a routing call. Falls back gracefully without a key. |
| X14 | [ ] Per-alarm background image with Android-12+ blur — drop-in to `AlarmFiringActivity`. Behind a per-alarm toggle, default off. | [BlackyHawky Clock 2.28](https://github.com/BlackyHawky/Clock/releases) | M | UX-only; no permissions; opt-in. |
| X15 | [ ] Manual drag-to-reorder of alarms list (currently sorted by time / next-fire). Persists order via `Alarm.sortOrder: Int`. | [BlackyHawky Clock 2.29](https://github.com/BlackyHawky/Clock/releases) | M | DB v11 + backup v8 schema bump. Use Reorderable-Compose patterns. |
| X16 | [ ] Migrate alarm audio playback from `MediaPlayer` to ExoPlayer / Media3. Improves LE Audio routing, gapless internet-radio, error handling. | [BlackyHawky Clock 2.22](https://github.com/BlackyHawky/Clock/releases) — proven precedent in the FOSS space | M | Carries regression risk — gate behind a build flag for one release. Pairs with X12 (LE Audio). |
| X17 | [ ] Material 3 Expressive — flip the v1.10.9 opt-in toggle (`AppSettings.expressiveModeEnabled`) to default once Compose BOM 2026.06+ ships the stable M3 1.4.x APIs without opt-in annotations. Keep a "Classic" fallback toggle for one release. | [Material 3 Expressive docs](https://m3.material.io/blog/material-3-expressive); [Compose BOM 2026.05.00](https://developer.android.com/jetpack/androidx/releases/compose-material3) | S | Promotion of previously-Under-Consideration "full M3 Expressive migration" entry. |
| X18 | [ ] Bedtime countdown `Notification.ProgressStyle` Live Update (mirrors v1.10.10 next-alarm Live Update during the final hour before bedtime reminder fires). | [Android 16 ProgressStyle](https://developer.android.com/about/versions/16/features/progress-centric-notifications) | S | Reuses next-alarm Live Update plumbing. Was UC; promote with Android 16 install base ~21% of devices per [Wikipedia Android 16 share, March 2026](https://en.wikipedia.org/wiki/Android_16). |
| X19 | [ ] Direct-Boot full-screen alarm support — register a Direct-Boot-aware copy of the alarm-firing flow so alarms can fire before first unlock after reboot. | [Android 14+ Direct Boot](https://developer.android.com/about/versions/14/direct-boot); [BlackyHawky Clock 2.25](https://github.com/BlackyHawky/Clock/releases) — proven OSS implementation | S | Was L-P2 (Later). BlackyHawky's release proves the platform path is viable on stock and Snapdragon; ACX can match without OEM cooperation. |

## LATER — kept on the list

Items revisited every two minor releases. Below are the categories with all
items. New entries from this pass are tagged **NEW**.

### Sleep tracking deepening

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-S1 | TFLite REM-stage classifier (extends X2). | [Pillow](https://www.pillow.app/), SleepWatch, [SlumberNet 2024](https://www.nature.com/articles/s41598-024-54727-0) | L |
| L-S2 | Snore recording + timeline (mic ring buffer, save >60 dB bursts). | [Sleep as Android sound detection](https://sleep.urbandroid.org/new-sleep-sound-detection/); [britig/SnoreDetection](https://github.com/britig/SnoreDetection) | M |
| L-S3 | On-device ML snore/sound-detection (cough / sleep-talk / baby) via TFLite micro. **NEW.** | [Edge Impulse snoring on smartphone](https://github.com/edgeimpulse/expert-projects/blob/main/audio-projects/snoring-detection-on-smartphone.md); [Sleep as Android sound detection blog](https://sleep.urbandroid.org/new-sleep-sound-detection/) | L |
| L-S4 | Pre-sleep tag tiles (caffeine / exercise / alcohol / stress) + correlation chart. | Sleep as Android | M |
| L-S5 | Lullaby soundscapes with motion-auto-off — **downloadable add-on**, not bundled, to respect F-Droid 40 MB budget. | [Sleep as Android lullaby pack](https://sleep.urbandroid.org/documentation/release-notes/); [Calm](https://www.calm.com/) | M |
| L-S6 | Environmental-noise baseline before bedtime reminder. | [Pillow](https://www.pillow.app/) | S |
| L-S7 | Apnea event flagging (mic onset detection — explicit "screening, not diagnosis" disclaimer; `play` flavor only). | [Apneal app smartphone OSA paper 2025](https://link.springer.com/article/10.1007/s11325-025-03441-w); [Springer 2025 IMU OSA](https://link.springer.com/article/10.1007/s11325-025-03255-w); [Samsung × Stanford 2025](https://www.samsungmobilepress.com/articles/samsung-announces-collaboration-with-stanford-medicine-to-advance-sleep-apnea-detection-and-beyond) | L |
| L-S8 | Chronotype quiz (Munich MEQ / Horne-Östberg) → ideal bedtime / wake calc. | [Roenneberg MEQ](https://www.thewep.org/documentations/mctq); [Horne-Östberg MEQ calculator](https://qxmd.com/calculate/calculator_829/morningness-eveningness-questionnaire-meq) | S |
| L-S9 | "AI sleep coach" — small on-device LLM summarising trends and surfacing one suggestion per day. **Strict privacy: model + inference local only.** | [Sleep as Android AI assistant beta](https://sleep.urbandroid.org/documentation/release-notes/); [Rise](https://www.risescience.com/) | L |
| L-S10 | Pre-sleep guided 4-7-8 / box breathing timer on the Bedtime tab. **NEW.** | [Headspace breathing techniques](https://www.headspace.com/) | S |
| L-S11 | "Stay-up-late-tonight" override — single-tap delay tonight's bedtime reminder by 1/2/3h with auto-revert. **NEW.** | [Pixel Bedtime mode](https://support.google.com/pixelphone/answer/9887159) | S |

### Wear OS / wearable depth (beyond X1)

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-W1 | Wearable-only vibration alarm (silent phone). | [Sleep as Android wear](https://docs.sleep.urbandroid.org/sleep/wearable_devices.html) | M |
| L-W2 | Bed-exit auto-dismiss via watch motion >60 s. | [Withings](https://www.withings.com/), [Garmin Connect](https://connect.garmin.com/) | M |
| L-W3 | HRV-aware smart wake (watch HRV dip > phone accel for light-sleep detection). | [Sleep as Android wear-data](https://docs.sleep.urbandroid.org/sleep/wearable_devices.html) | L |
| L-W4 | Standalone Wear OS app (alarm fires without phone). | [Pixel Watch 3 alarms](https://support.google.com/wearos/answer/6300982?hl=en); [1Smart WakeUp](https://play.google.com/store/apps/details?id=com.onesmart.wakeup) | L |
| L-W5 | Migrate Wear tile from `protolayout-material3` to `androidx.glance:glance-wear-tiles` once stable. **NEW (tech-debt).** | [Glance Wear](https://developer.android.com/jetpack/androidx/releases/glance-wear) — currently 1.0.0-alpha07 | M |

### Workplace / shift worker

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-WS1 | Rotating shift patterns (DDNNO / 4-on-4-off / Panama / DuPont / Pitman). | [Supershift](https://supershift.app/), [Shyft](https://www.myshyft.com/), [Work Shift Calendar](https://www.lrhsoft.com/wsc.html) | M |
| L-WS2 | Jet-lag re-entrainment schedule (gradual shift over N days, light-exposure timing). | [Timeshifter](https://www.timeshifter.com/), [Roenneberg MEQ](https://www.thewep.org/documentations/mctq) | L |
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
| L-R6 | Tasker / MacroDroid / Home Assistant recipe library — bundle 5-10 ready-made flows under `docs/integrations/`. | [HA Sleep as Android integration](https://www.home-assistant.io/integrations/sleep_as_android/); [IATkachenko/HA-SleepAsAndroid](https://github.com/IATkachenko/HA-SleepAsAndroid) | S |
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
| L-P1 | Power-off alarm (Qualcomm / Samsung / Xiaomi HAL). | OEM | L — privileged partner programs only; likely never indie-achievable |
| L-P2 | ~~Direct-boot full-screen alarm audit~~ → promoted to **NEXT X19** for v1.13. | Android 14+; [BlackyHawky Clock 2.25](https://github.com/BlackyHawky/Clock/releases) | (moved) |
| L-P3 | Emergency-escalation call tree (SMS → call → partner → siren). | [Twilio](https://www.twilio.com/) / native | M |
| L-P4 | Location-based escalation (still at home after dismiss → siren). | FusedLocation; partial in code via `locationDismissEnabled` fields | M |
| L-P5 | Car-mode suppression (Android Auto `CarConnection` API; receive Google's new in-car alarm pop-up). | [Android Auto](https://developer.android.com/training/cars); [Android Auto in-car alarm controls 16.8](https://www.autoevolution.com/news/android-auto-is-getting-the-feature-users-first-asked-for-10-years-ago-269408.html) | S |
| L-P6 | Companion-watch autonomous fire if phone battery dies. | — | M |
| L-P7 | Charging-only alarm variant. | — | S |
| L-P8 | Charge-disconnect missed-alarm re-fire (proxy for "user picked up phone"). **NEW.** | adjacent to MissedAlarmUnlockReceiver | S |
| L-P9 | Bedtime battery-state warning ("phone is at 14% — plug in to avoid alarm failure"). **NEW.** | [dontkillmyapp.com strategies](https://dontkillmyapp.com/) | S |
| L-P10 | "Anti-Sleepyhead Security" — alarm only dismissable when GPS confirms you've left a configured geofence. **NEW.** Fields already in DB (`locationDismissEnabled`, `locationDismissRadius`); finish UI. | [Turbo Alarm](https://play.google.com/store/apps/details?id=com.turbo.alarm) | S |

### Cloud / sync

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-C1 | Google Drive / Nextcloud / WebDAV backup via SAF (opt-in; encryption already exists). | [SAF docs](https://developer.android.com/guide/topics/providers/document-provider); [SeedVault](https://nlnet.nl/project/SeedVault-Integrity/) for inspiration | M |
| L-C2 | End-to-end encrypted paired-phone LAN sync. | — | L |

### Smart home

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-SH1 | Matter 1.6 Dynamic Lighting (DLE) cross-brand sunrise — extends Hue path to any Matter bulb without per-brand workarounds. **NEW.** | [Matter 1.6 DLE 2026](https://mattressmiracle.ca/blogs/mattress-miracle-blog/matter-1-6-dynamic-lighting-sunrise-gradient-bedroom); [Matter Innovations CES 2026](https://matter-smarthome.de/en/products/the-matter-innovations-at-ces-2026/); [Google Home Matter dev docs](https://developers.home.google.com/matter) | L |
| L-SH2 | Home Assistant blueprint kit — 5-10 ready-made flows in `docs/integrations/`. | [HA Sleep as Android integration](https://www.home-assistant.io/integrations/sleep_as_android/); [HA-SleepAsAndroid](https://github.com/IATkachenko/HA-SleepAsAndroid) | S |

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
| L-X2 | Haptic-only alarm profile — **shipped v1.10.4.** Kept here as the audit anchor; verify each release. | [Apple Taptic engine](https://support.apple.com/en-us/111880); v1.10.4 in CHANGELOG | (shipped) |
| L-X3 | TalkBack audit — large double-tap buttons on firing screen. | [Android accessibility overview](https://support.google.com/accessibility/android/answer/6006564) | S |
| L-X4 | Pure-black / mono-color WCAG AAA high-contrast theme. | [WCAG 2.2 / 2.1 AAA](https://www.w3.org/WAI/WCAG22/quickref/) | S |
| L-X5 | Voice-only dismiss (offline `SpeechRecognizer`). Pairs with X7. | [Voice Access](https://support.google.com/accessibility/android/answer/6151848) | S |
| L-X6 | Per-user long-press thresholds on challenge buttons. | Android a11y guidelines | S |

### Documentation

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-DOC1 | Document the VMware-shared-folder MAX_PATH build gotcha in CONTRIBUTING.md (already in CLAUDE.md). **NEW.** | local: [CLAUDE.md:184](CLAUDE.md) | S |
| L-DOC2 | F-Droid anti-feature flag for crash-log writes — document explicitly that crash logs stay local and are never uploaded. **NEW.** | local: [util/CrashLogger.kt](app/src/main/java/com/sysadmindoc/alarmclock/util/CrashLogger.kt); [metadata/com.sysadmindoc.alarmclock.yml](metadata/com.sysadmindoc.alarmclock.yml) | S |

---

## UNDER CONSIDERATION

Items that need scoping or platform readiness before they earn a tier.

| Item | Blocker / scoping question |
|------|---------------------------|
| Android Auto in-car alarm pop-up handler | Wait for Android Auto 16.8 stable release + AAOS API documentation. Currently leaked only via beta teardowns. ([autoevolution](https://www.autoevolution.com/news/android-auto-is-getting-the-feature-users-first-asked-for-10-years-ago-269408.html)) |
| iOS-26 AlarmKit UX pattern adoption (full-screen snooze/stop visuals, App-Intent secondary action) | Study-only — App Intents are iOS-only; port the platform-neutral visual + interaction patterns to ACX firing screen. ([Apple AlarmKit](https://developer.apple.com/documentation/AlarmKit)) |
| TensorFlow Lite REM-stage classifier (L-S1) | Model footprint vs. APK-size budget; F-Droid users expect <40 MB. Need a downloadable-model strategy that doesn't break offline-first. |
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
| Power-off alarm without OEM cooperation | Requires privileged partner programs unavailable to indie apps. (L-P1 stays Later as the formal "you'd need OEM" entry — reaffirmed Rejected for any non-OEM workaround.) |

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
- Backup-export warning when `webhookUrl` / `hueApiKey` is non-empty — surface a "your backup contains secrets" confirmation before export. **NEW audit item — S, not yet tiered.**

### Observability

- Crash logger writes to local files only; we don't ship a remote sink and won't (privacy). Ensure rotation cap remains in place so a runaway loop can't fill storage.
- Add a "share crash log" button on the About screen (does not auto-upload — copies to clipboard or invokes share sheet). **S, not yet tiered.**
- App Standby bucket surfaced in the Reliability Settings group (N3) doubles as observability for the user.

### Distribution / packaging

- Two flavors today: `play` (with YT downloader + Wear Data Layer), `fdroid` (without). Maintain parity on every other surface. 2026-05-17 audit note: `.github/workflows/release.yml` now builds signed Play/F-Droid/Wear release artifacts and uploads SHA-256 hashes on tags; `.github/workflows/android-ci.yml` runs an OSV audit against the resolved Play release runtime classpath.
- F-Droid lint passes — anti-feature flag for the YT downloader is documented in `metadata/`. Re-verify on each release. Add explicit F-Droid anti-feature note for crash-log local files (L-DOC2).
- AAB for Play Store, signed APK for GitHub Releases; never ship unsigned artifacts.
- F-Droid users expect APK under **~40 MB**. Any TFLite-model or Matter-SDK work must respect this budget (downloadable models, not bundled).

### i18n / l10n

- English-only today. **Per-app language picker (L-U5) lands first**, THEN community translation. No machine-translation-only strings — better to remain English than ship broken translations.
- `Configuration` change tests when M3 Expressive + per-app locale stack: confirm `Compose` recomposes correctly via `LocalConfiguration`.

### Testing

- Unit tests cover: `NextAlarmCalculator`, `VacationAlarmPolicy`, `MissedAlarmReplayPolicy`, `ProximityCoverDetector`, `AlarmShareCodec`, `EncryptedBackupCodec`, `WakeStreakCalculator`, `WebhookUrl`, `ChallengeGenerator` + maze solver, `StatsFilters`, `NextAlarmNotificationTiming`. **Each new dismiss challenge must come with a unit-tested "valid input" + "invalid input" suite.**
- Room migration tests: every schema bump requires a migration test path in `AlarmDatabaseMigrationTest`; CI also runs `git diff --exit-code -- app/schemas` after debug builds to catch uncommitted exports (whakaara discipline — [ahudson20/whakaara](https://github.com/ahudson20/whakaara)).
- Add an instrumented smoke test that fires an alarm via test broadcast and asserts the firing activity launches with `FLAG_SHOW_WHEN_LOCKED`. **S, not yet tiered.**
- Add a `sanitized()` round-trip property test that asserts every value in `ChallengeType.entries.map(Enum::name)` is preserved through `Alarm.sanitized()`. Directly prevents the N1 class of regression in the future.

### Documentation

- README, CHANGELOG, ROADMAP, CLAUDE.md, and the version badge must all match on every release. **N10 makes this enforced in CI instead of manual.**
- Add a CONTRIBUTING.md (currently absent) — covers the VMware MAX_PATH gotcha (L-DOC1) and the new contributor's first PR loop.

### Plugin ecosystem

- Webhooks (Tasker / MacroDroid / Home Assistant) cover the integration surface we want to expose. A "real" plugin SDK is rejected (UC) until webhook gaps are documented.
- Recipe library (L-R6 + L-SH2) — bundle 5-10 ready-made Tasker / Home Assistant flows in `docs/integrations/`.

---

## Research sources (round 5 — refreshed 2026-05-16)

### Direct OSS competitors

- **yuriykulikov/AlarmClock** — https://github.com/yuriykulikov/AlarmClock — 612★, AOSP-derived. Signature feature: pre-alarm low-volume gentle wake (L-A10); long-press dismiss; adjustable snooze picker.
- **FossifyOrg/Clock** — https://github.com/FossifyOrg/Clock — beta 1.6.0 (Feb 2026). Switches replacing checkboxes, "About" back in options menu, Android 7 support dropped.
- **BlackyHawky/Clock** — https://github.com/BlackyHawky/Clock — v2.29 (Apr 2026), v2.30 in nightly. Per-version harvest applied to this roadmap pass: pause-alarms (N6), manual drag-reorder (X15), sync alarms (L-H6), Direct-Boot (X19), BT routing (L-A5), folder ringtones (L-A8), per-alarm background (X14), vibration delay (N7), missed-timer notif (N8), ExoPlayer (X16), custom fonts (UC).
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
- **Sleep Cycle** — https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk — 2026 SDK release; phone mic + accelerometer detects sleep stages and fires alarm in lightest phase within wake window. Algorithm reference for X3.
- **Rise** — https://www.risescience.com/ — sleep-debt accumulator + composite score reference for X4 / X5.
- **Pillow** — https://www.pillow.app/ — actigraphy reference (iOS-only).
- **Turbo Alarm** — https://play.google.com/store/apps/details?id=com.turbo.alarm — Spotify-as-alarm, Wear OS support, talking alarm, sunrise simulation, mini-game dismiss, "Anti-Sleepyhead Security" (L-P10), cloud-sync, Tasker / Macrodroid / Sleepbot integration.
- **Google Clock** — https://play.google.com/store/apps/details?id=com.google.android.deskclock — Pixel-exclusive Sunrise Alarm + Bedtime tab reference.
- **I Can't Wake Up** — Simon-says, voice-phrase reference (X7).
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

- Health Connect (N12 / X3) — published privacy-policy update + Play Console health-permissions declaration **must precede shipping** (April 2026 policy). ([Play Console policy](https://support.google.com/googleplay/android-developer/answer/16926792?hl=en))
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
