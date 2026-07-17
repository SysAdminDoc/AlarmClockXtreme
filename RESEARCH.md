# Research — AlarmClockXtreme
Date: 2026-07-14 — replaces all prior research.

## Executive Summary

AlarmClockXtreme is a local-first Android alarm, bedtime, timer, and wake-readiness suite whose strongest shape is its native exact-alarm engine, Direct Boot fallback, unusually deep dismiss/scheduling options, encrypted backup, Play/F-Droid split, and no-account/no-telemetry stance. Recent commits repaired several process-death, cache, and playback failures, so the highest-value direction is to finish eliminating split ownership and exposed trust boundaries before adding more wake features. In priority order: make `TimerAlarmService` the sole expiry-alert owner; complete and permission-protect the Android `AlarmClock` intent contract; reuse production TOFU for the Hue connection test; add dependency verification and release locks; redact timer labels on public lock-screen surfaces; restore release-metadata drift checks as a local gate; split the 2,629-line alarm editor into summarized subscreens; honor reduced-motion/flashing preferences; add a privacy-bounded learned commute fallback; and provide safe external-clock migration plus explicit per-alarm timezone policy. **Confidence: Verified** from live source, history, official platform guidance, and current competitor releases.

## Product Map

- **Core workflows:** create, schedule, skip, snooze, dismiss, and recover alarms; run timers/stopwatch/world clocks; plan bedtime and inspect local sleep/wake statistics; auto-create alarms from calendar, commute, holidays, shifts, weather, and solar time; back up, restore, share, and diagnose locally.
- **User personas:** heavy sleepers and challenge users; shift/on-call workers; privacy-focused F-Droid users; travelers; accessibility users; and power users integrating Wear OS, Hue, webhooks, calendars, Health Connect, or custom audio.
- **Platforms and distribution:** Android 8+ phone/tablet/foldable (`minSdk 26`, `targetSdk 36`) in Play and F-Droid flavors, plus a companion Wear OS tile/complication app; releases and security gates are intentionally local-only.
- **Key integrations:** Android `AlarmManager`, Direct Boot storage, notifications/full-screen intents, Room DB v22, DataStore, Media3, Google Routes, Open-Meteo, Health Connect, Hue, Wear Data Layer, yt-dlp/NewPipe, SAF backup, webhooks, widgets, and Quick Settings.
- **Data flow:** Room owns alarm/history records; DataStore owns preferences; device-protected storage carries the minimal locked-boot alarm snapshot; external data is cached locally; backup/share codecs sanitize before user-confirmed import.

## Competitive Landscape

- **ClockYou:** does rapid, practical clock UX well, including 2026-07-13 timezone adjustment, Fossify JSON import, and timer-notification actions. Learn explicit migration previews and timezone semantics; avoid direct external writes without ACX's disabled-by-default review boundary.
- **Blacky Clock:** does per-timer behavior, audio-state recovery, small-screen/RTL fixes, and frequent OEM regression releases well. Learn its narrow recovery fixes; avoid backup-format changes that invalidate older exports.
- **Fossify Clock:** does a small, private, permission-light baseline well. Preserve ACX's local-first positioning and offer a safe import path; avoid reducing ACX to parity-only clock features.
- **Chrono:** does tags, filtering, date ranges, timers, and responsive Compose presentation well. Learn its compact information hierarchy; avoid its explicit work-in-progress posture for alarm-critical use.
- **Alarmy:** does missions, wake checks, and readiness messaging well, but commercializes restriction and last-minute controls. Keep ACX's already-shipped cancellation lock and safe escape paths; avoid coercive AccessibilityService/device-admin/anti-uninstall patterns.
- **Sleep as Android:** does sleep analysis, wearable/smart-home integration, and staged feature presentation well. Learn progressive disclosure; avoid accounts, subscriptions, and cloud analytics that contradict ACX's privacy model.
- **EarlyBird:** does offline resilience well through local Trip Duration Memory. Learn a bounded historical commute estimate with explicit stale/fallback labels; never present cached history as live traffic.

## Security, Privacy, and Reliability

- **Verified — Hue credential exposure:** `ui/settings/SettingsViewModel.kt:417-470` creates a trust-all `X509TrustManager`, accepts every hostname, sends `hue-application-key`, and automatically falls back to HTTP. Production `worker/HueSunriseWorker.kt:61-124,239-263` already has TOFU pinning and an explicit `hueLegacyHttpEnabled` gate; the test path must share that client and pin state.
- **Verified — exported intent contract is incomplete:** `app/src/main/AndroidManifest.xml:139-154` advertises four `AlarmClock` actions, while `MainActivity.kt:178-213` implements only `SET_ALARM`; dismiss/snooze are no-ops, `SET_TIMER` is absent, and `onNewIntent()` at `:116-120` does not call the handler. Android documents a permission-protected activity for receivers of alarm/timer set actions; the launcher activity cannot safely carry that component permission.
- **Verified — split timer alert ownership:** `ui/timer/TimerExpiryReceiver.kt:20-27` trusts a process-wide UI-alive flag; `TimerViewModel.kt:313-347` and `TimerAlarmService.kt:44-145` each own separate players/vibration. A delayed or cleared UI owner can still produce a silent/cut-off alert, and Android 17 further restricts background audio outside a visible activity or qualifying foreground service.
- **Verified — timer labels are public:** `TimerNotifications.kt:18-33` and `TimerAlarmService.kt:92-125` use `VISIBILITY_PUBLIC` and expose labels on secure lock screens/screen sharing. Alarm label redaction exists elsewhere but is not applied to timers; use private content plus a generic public version.
- **Verified — supply-chain integrity gap:** versions and transitive constraints are pinned, and `scripts/osv_gradle_audit.py` checks known advisories, but no `gradle/verification-metadata.xml` or dependency lock state exists. The Play flavor's JitPack/downloader/native graph makes reviewed checksums and release-classpath locks complementary to OSV scanning.
- **Verified — recovery boundary:** append/replace restore is well reviewed, but atomic replacement remains in `Roadmap_Blocked.md`; do not duplicate or weaken that blocker. External imports should reuse preview, size-limit, disabled-by-default, sanitization, and transactional-write patterns.

## Architecture Assessment

- `ui/alarmedit/AlarmEditScreen.kt` is 2,629 lines with 22 major `SettingsSection` groups. Android's settings guidance favors grouped subscreens for this scale; preserve one `AlarmEditViewModel` draft and make the existing unsaved-change guard a prerequisite, rather than merely virtualizing the same giant form.
- `ui/timer/TimerViewModel.kt`, `TimerExpiryReceiver.kt`, `TimerAlarmService.kt`, and `TimerNotifications.kt` need one persisted expiry state machine and one audible-alert owner. UI countdowns should observe state, not decide whether the alarm-critical service runs.
- `data/model/Alarm.kt` has no timezone policy and scheduling/display paths use `ZoneId.systemDefault()`. A fixed-zone option must migrate as `LOCAL` by default and flow through calculator, scheduler, Direct Boot, backup/share, Wear, widgets, and DST tests.
- `worker/CalendarAutoAlarmWorker.kt:181-210` uses live Google Routes or a manual/weather baseline. A capped, app-private route-duration history can add offline resilience without storing raw itinerary text or claiming live traffic.
- `ui/alarmfiring/AlarmFiringScreen.kt`, `ui/alarmfiring/challenges/ChallengeViews.kt`, `ui/components/WeatherSkyBackground.kt`, `ui/timer/TimerScreen.kt`, and `ui/nightclock/NightClockActivity.kt` contain nonessential infinite motion; no reduced-motion policy was found. Essential progress needs a static equivalent, and optional strobe needs explicit warning/control.
- Tests are concentrated in JVM suites; no tests cover `AlarmClock` intent routing, Hue changed-certificate rejection, timer single-owner behavior, or public timer notification content. The existing full-suite-health roadmap item remains the prerequisite; a local run on 2026-07-14 did not establish suite health because Gradle's ASM transform directory was locked.
- Release/version facts are duplicated across app/Wear Gradle files, README/metadata, verifier constants, DB, and backup declarations. The previous CI guard was removed with all workflows; `build.gradle.kts:1` already says `v1.15.26` while runtime metadata is `1.15.28`, so the equivalent check belongs in the local release gate.
- Coverage disposition: new work below covers security, accessibility, offline resilience, migration, testing, distribution, and upgrade integrity. Existing roadmap tracks cover i18n/l10n, local observability, docs, Wear/mobile validation, and webhook-based integrations; a plugin SDK remains unjustified. Multi-user cloud state remains out of scope, while local partner profiles/LAN sync already sit in Later.

## Rejected Ideas

- **Force-stop/anti-uninstall prevention** — Android challenge-app discussions and Alarmy complaints show demand, but AccessibilityService, overlay, or device-admin coercion is unsafe, policy-sensitive, and contrary to user control.
- **Cloud accounts, social alarm feeds, or cloud AI coaching** — commercial competitors monetize these, but they contradict the repository's no-account/no-telemetry philosophy and add breach/operations burden.
- **Replace the native alarm engine with Flutter or a generic alarm library** — Ultimate Alarm Clock moved wake-critical scheduling toward native Kotlin; ACX's existing engine, Direct Boot path, and migration history are already deeper than reusable libraries.
- **More puzzles, badges, fonts, or weather cosmetics** — competitor scans found no value exceeding the current 30 challenges while reliability, i18n, editor hierarchy, and accessibility remain unfinished.
- **Mandatory live route service or bundled traffic provider** — cost, API-key, privacy, and offline failure modes are worse than a clearly labeled local historical fallback.
- **Per-timer ringtone/vibration/flash/auto-delete matrix now** — Blacky Clock validates the feature, but it should wait until one service owns timer expiry; adding variants first multiplies the split-brain state space.
- **Reproducible-build roadmap item** — already implemented by `scripts/verify-reproducible-build.sh`; dependency verification/locking is the remaining distinct integrity gap.
- **Post-dismiss briefing or last-minute cancellation lock** — both are already shipped (`MorningBriefingActivity` and `cancellationLockMinutes`); re-adding them would duplicate working behavior.

## Sources

### Platform, standards, and security

- https://developer.android.com/reference/android/provider/AlarmClock
- https://developer.android.com/about/versions/17/changes/bg-audio
- https://developer.android.com/media/media3/session/background-playback
- https://developer.android.com/privacy-and-security/risks/unsafe-hostname
- https://developer.android.com/reference/android/app/Notification#VISIBILITY_PUBLIC
- https://developer.android.com/design/ui/mobile/guides/patterns/settings
- https://support.google.com/accessibility/android/answer/16635954
- https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions
- https://www.w3.org/WAI/WCAG22/Understanding/three-flashes-or-below-threshold
- https://docs.gradle.org/current/userguide/dependency_verification.html
- https://docs.gradle.org/current/userguide/dependency_locking.html
- https://osv.dev/

### Dependencies and ecosystem

- https://developer.android.com/jetpack/androidx/releases/room
- https://developer.android.com/jetpack/androidx/releases/work
- https://developer.android.com/jetpack/androidx/releases/media3
- https://github.com/google/dagger/releases
- https://github.com/TeamNewPipe/NewPipeExtractor/releases
- https://github.com/yausername/youtubedl-android/releases

### Competitors, commercial products, and discovery lists

- https://github.com/you-apps/ClockYou/releases
- https://github.com/you-apps/ClockYou/commit/8f03e8bba921bc55014492a0e4a68fbcccf6c21f
- https://github.com/BlackyHawky/Clock/releases
- https://github.com/FossifyOrg/Clock
- https://github.com/vicolo-dev/chrono
- https://alarmy-android.zendesk.com/hc/en-us/articles/900001614846-Let-me-introduce-to-you-Alarmy-Premium-features
- https://sleep.urbandroid.org/docs/general/release_notes.html
- https://www.earlybirdalarm.net/
- https://github.com/offa/android-foss

### Community and research

- https://www.reddit.com/r/androidapps/comments/1lx50rp/i_need_a_better_alarm_app_because_alarmy_has_now/
- https://www.reddit.com/r/androidapps/comments/jn7i82/searching_for_a_alarm_clock_with_extended/
- https://pubmed.ncbi.nlm.nih.gov/40389592/

## Open Questions

None. The roadmap items can be implemented and validated from the inspected code and cited public contracts; device/API-37/credential-dependent work remains in `Roadmap_Blocked.md`.
