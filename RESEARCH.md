# Research: AlarmClockXtreme
Research date: 2026-07-22, replacing all prior research. Version facts refreshed 2026-08-22.

## Executive Summary

AlarmClockXtreme (v1.15.34, versionCode 136) is a local-first Android alarm /
bedtime / timer / wake-readiness suite whose strongest shape is a native
`setAlarmClock()` engine with Direct Boot fallback, 30 dismiss challenges plus
"none",
mission chaining, encrypted backup, a Play/F-Droid split, and a strict
no-account/no-telemetry stance. **Every finding from the 2026-07-14 research pass
is now fixed** (Hue TOFU test path, the four-action `AlarmClock` intent contract
behind a permission-protected activity, single-owner timer alerts, redacted
public timer labels, `gradle/verification-metadata.xml` with 857 pinned
components, a real reduced-motion policy, and a per-alarm timezone policy),
verified against live code on 2026-07-22. The remaining high-value direction is
**reliability-first differentiation**: the incumbents (including Google's own
Pixel Clock) are visibly failing to fire alarms, and community signal in 2026 is
dominated by "the alarm didn't go off." The top opportunities, in order:
(1) a proactive post-fire confirmation watchdog that catches silent misses;
(2) Media3 stall detection so a stalled ring escalates instead of going silent;
(3) DND / OEM-bedtime-schedule conflict detection; (4) fix the one new leak in
`SkipNextAlarmTileService`; (5) snooze-to-a-specific-time; (6) extend Android 16
Live Updates to the snooze countdown; (7) an OEM "reliability doctor" of
per-manufacturer autostart/battery deep-links with post-OTA re-checks.
**Confidence: Verified** unless a specific item is marked otherwise.

## Product Map

- **Core workflows:** create/schedule/skip/snooze/dismiss/recover alarms; run
  timers/stopwatch/world clocks; plan bedtime and view local sleep/wake stats;
  auto-create alarms from calendar/commute/holiday/shift/weather/solar inputs;
  back up, restore, share, and locally diagnose.
- **Personas:** heavy sleepers & challenge users; shift/on-call workers;
  privacy-focused F-Droid users; travelers; accessibility users; power users
  wiring Wear OS/Hue/webhooks/calendars/Health Connect/custom audio.
- **Platforms & distribution:** Android 8+ phone/tablet/foldable (`minSdk 26`,
  `targetSdk 36`) in Play and F-Droid flavors + a companion Wear OS
  tile/complication; releases and security gates are intentionally local-only.
- **Key integrations & data flow:** `AlarmManager`, Direct Boot storage, FSI/
  notifications, Room DB v23, DataStore, Media3 1.10.1, Google Routes,
  Open-Meteo, Health Connect (Play), Hue (TOFU-pinned), Wear Data Layer,
  yt-dlp/NewPipe (Play), SAF backup, webhooks (HMAC-signed), widgets, Quick
  Settings. Room owns records; DataStore owns prefs; device-protected storage
  carries the minimal locked-boot snapshot; external data is cached locally;
  backup/share codecs sanitize before user-confirmed import.

## Competitive Landscape

- **BlackyHawky/Clock (v2.30):** strong at per-timer behavior, OEM-regression
  fixes, label-synced alarms, ring-only-when-headset. Learn its narrow recovery
  fixes; avoid backup-format changes that break older exports.
- **you-apps/ClockYou:** clean, fast clock UX with multi-select, numpad entry,
  timezone auto-adjust, Fossify import. Learn nothing net-new here, because ACX already
  ships multi-select (`ui/alarmlist/AlarmListScreen.kt`), numpad, timezone policy,
  and Fossify import.
- **vicolo-dev/chrono:** date-range/every-N-day recurrence, ringtone shuffle /
  random start offset, reduce-volume-during-task, max-snooze. Learn the
  volume-during-challenge and random-start ideas; avoid its explicit WIP posture
  for wake-critical use.
- **yuriykulikov/AlarmClock:** signature gentle pre-alarm (already tracked as
  L-A10) and snooze-to-specific-time. Learn scheduled snooze.
- **Alarmy / Sleep as Android / Turbo Alarm (commercial):** paywall physical-
  activity missions, smart-light sunrise/dismiss, meditation, Wear complication
  control, sleep-stage smart-wake. Keep ACX's shipped equivalents free; avoid
  their subscription model and the accessibility complaints their gated missions
  attract.
- **Google Clock / Pixel:** watch-sync + screen-brightening Sunrise Alarm are now
  platform table-stakes, but Pixel's unresolved "missed alarm, unknown reason"
  bug is the single biggest acquisition opportunity for a reliability-first app.

## Security, Privacy, and Reliability

- **Verified, new coroutine-scope leak:** `service/SkipNextAlarmTileService.kt:34`
  creates `CoroutineScope(Dispatchers.IO + SupervisorJob())` and launches DB
  reads at `:43` and `:69` but never cancels it. No `onStopListening()` /
  `onDestroy()` override. TileService instances churn as the QS shade opens; the
  scope leaks. Low severity, real. All other services cancel correctly.
- **Verified, no proactive fire verification:** the engine survives process
  death (`setAlarmClock()` + Direct Boot), and missed alarms replay reactively on
  `USER_PRESENT`/`POWER_DISCONNECTED`, but nothing confirms shortly *after* a
  scheduled fire time that the alarm actually rang. This is exactly the failure
  class of the Pixel "unknown reason" bug and OEM Doze kills.
- **Verified, alarm audio can stall silently:** the Media3 ring path has no
  stall/timeout detection. Media3 1.9 exposes `StuckPlayerException` and stalled-
  ready timeouts; a stalled ring currently relies only on the delayed
  backup-sound escalation to recover.
- **Verified safe, protobuf CVE-2026-0994 (GHSA-7gcm-g887-7qv7, CVSS 8.2):** the
  transitive protobuf (via Glance/Wear/DataStore) resolves through
  `protobuf-bom-4.35.0` in `gradle/verification-metadata.xml`, past the fix line.
  No action; keep the OSV gate watching it.
- **Verified, Android 17 background-audio exemption holds:** every ring path
  uses `AudioAttributes.USAGE_ALARM` and the app holds exact-alarm permission, so
  the API 37 while-in-use FGS requirement is waived (already documented in
  CLAUDE.md). Re-verify on an API 37 device at targetSdk bump (tracked, blocked).
- **Verified, DND/Zen self-management only:** `service/BedtimeZenRuleManager.kt`
  sets its own `INTERRUPTION_FILTER_ALARMS` rule but does not detect a
  *conflicting* user or OEM bedtime/DND schedule that could mute the alarm.

## Architecture Assessment

- **God files (tracked, in progress):** `ui/settings/SettingsScreen.kt` (4129),
  `ui/alarmedit/AlarmEditScreen.kt` (3487), `ui/alarmfiring/challenges/ChallengeViews.kt`
  (2736), `service/AlarmService.kt` (2164), `ui/alarmlist/AlarmListScreen.kt`
  (1767). `BedtimeScreen.kt` is being drained section-by-section (now ~1652).
  Continue the seam-extraction pattern; the ROADMAP "Audit backlog" item covers it.
- **Reliability layering:** a proactive fire-confirmation watchdog and Media3
  stall detection add to the existing reactive
  replay and backup-sound escalation. Frame them as post-fire verification and
  in-ring stall recovery respectively.
- **Testing gaps:** no tests cover the TileService lifecycle, a simulated silent
  miss + watchdog re-fire, or DND-conflict detection. New items below carry their
  own acceptance tests; the JVM-suite discipline (Robolectric + drift guards)
  already exists.
- **Coverage disposition:** items below cover security, reliability, offline
  resilience, accessibility, platform, and UX. i18n/l10n (L-U5), local
  observability, docs, Wear/mobile validation, and webhook integrations are
  tracked elsewhere; a plugin SDK stays unjustified; multi-user cloud stays out
  of scope (local partner profiles/LAN sync remain in Later).

## Rejected Ideas

- **Multi-select bulk alarm ops**: already shipped (`ui/alarmlist/AlarmListScreen.kt`
  `isSelectionMode`/`selectMany`/bulk delete). Source: you-apps/ClockYou.
- **Media-button / Bluetooth dismiss-snooze**: already shipped via per-alarm
  `hardwareButtonAction` handling `KEYCODE_HEADSETHOOK`/volume/camera in
  `AlarmFiringActivity.onKeyDown` (`:629-660`). Source: BlackyHawky #642.
- **Headphone-unplug re-routing (AudioBecomingNoisy)**: already handled proactively:
  `service/AlarmAudioRouting.shouldForceBuiltInSpeaker` forces the built-in
  speaker so a headset can't swallow the alarm. Source: Media3 1.9.
- **Ring-only-when-headphones-connected**: directly contradicts the above
  reliability guarantee (ACX intentionally forces the speaker so alarms can't be
  silently swallowed); niche silent-partner use case not worth reversing it.
  Source: BlackyHawky #631.
- **Power-off guard / accessibility anti-uninstall lock**: coercive, Play-policy
  sensitive, contrary to user control; already Rejected. Source: qralarm-android.
- **protobuf CVE-2026-0994 remediation item**: transitive protobuf already
  resolves to 4.35.0, past the fix line. Source: GHSA-7gcm-g887-7qv7.
- **Replace AlarmManager with a WorkManager periodic scheduler**: `setAlarmClock()`
  is the correct wake-critical primitive; a WorkManager *watchdog* (see roadmap)
  is the right shape, not a replacement. Source: WorkManager release notes.
- **Material3 1.5 Expressive TimePicker adoption now**: UX-only, needs a Compose
  BOM bump entangled with the blocked AGP 8→9 chain; low value vs. the existing
  dial + numpad. Revisit post-AGP9. Source: compose-material3 release notes.

## Sources

### Platform, standards, security
- https://developer.android.com/about/versions/16/features/progress-centric-notifications
- https://developer.android.com/about/versions/17/changes/bg-audio
- https://developer.android.com/about/versions/17/behavior-changes-17
- https://developer.android.com/develop/ui/compose/system/predictive-back-progress
- https://developer.android.com/health-and-fitness/health-connect/experiences/sleep
- https://support.google.com/googleplay/android-developer/answer/16926792
- https://developer.android.com/guide/practices/page-sizes
- https://github.com/advisories/GHSA-7gcm-g887-7qv7

### Dependencies / ecosystem
- https://developer.android.com/jetpack/androidx/releases/media3
- https://developer.android.com/jetpack/androidx/releases/work
- https://developer.android.com/jetpack/androidx/releases/glance
- https://developer.android.com/jetpack/androidx/releases/compose-material3

### Competitors, commercial, community
- https://github.com/BlackyHawky/Clock/releases
- https://github.com/BlackyHawky/Clock/issues/631
- https://github.com/BlackyHawky/Clock/issues/642
- https://github.com/you-apps/ClockYou/releases
- https://github.com/vicolo-dev/chrono
- https://github.com/yuriykulikov/AlarmClock
- https://github.com/sweakpl/qralarm-android
- https://play.google.com/store/apps/details?id=com.turbo.alarm
- https://sleep.urbandroid.org/docs/general/release_notes.html
- https://www.androidpolice.com/pixel-alarm-bug-is-back/
- https://support.google.com/pixelphone/thread/318525822/missed-alarm-alarm-did-not-fire-due-to-an-unknown-reason
- https://github.com/WrichikBasu/ShakeAlarmClock/discussions/61
- https://dontkillmyapp.com/
- https://alarmy-android.zendesk.com/hc/en-us/articles/4592128972313--Xiaomi

## Open Questions

None that block prioritization. The watchdog and stall-detection items need
device/emulator validation (marked Likely) and should land their instrumented
checks in the already-blocked device-test tracks; all other items are
implementable from inspected code and cited public contracts.
