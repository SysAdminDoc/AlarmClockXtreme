# Research - AlarmClockXtreme

## Executive Summary

AlarmClockXtreme is a mature Android alarm suite for heavy sleepers and privacy-focused users: exact `setAlarmClock()` scheduling, Direct Boot fallback, Play/F-Droid flavors, Wear tile/complication support, 27 dismiss challenges, encrypted backup, local diagnostics, webhooks, Hue, weather/news, Health Connect sleep summaries, Sonar sleep-motion summaries, and a deep dark Compose UI. The current working tree is v1.15.5 / versionCode 107 / DB v17 / backup format v11. Highest-value work now is not adding another broad feature bucket; it is closing trust gaps around alarm-fire verification, release/security gates, and platform readiness that currently need stronger proof. Top opportunities: add a local alarm-fire smoke harness, make dependency/advisory audits part of release proof, verify 16 KB and API 37 readiness, and keep Wear support ahead of the first-party Wear Clock gap.

## Product Map

- Core workflows: create/edit exact alarms, challenge-chain dismiss, snooze/dismiss/wake-confirm, Direct Boot fallback fire, smart wake, bedtime/DND, timers, stopwatch, world clock, weather/news dashboard, backup/restore, shared alarm import, support bundle export.
- User personas: heavy sleeper, F-Droid/privacy user, Wear OS user, smart-home automator, shift worker, caregiver/guardian user, deaf/HoH user, Play-flavor user who wants YouTube sounds.
- Platforms and distribution: Android 8+ phone app, `play` and `fdroid` flavors, Wear companion module, GitHub release APKs, F-Droid metadata, Apache-2.0.
- Key integrations and data flows: Room alarms/events/incidents/actigraphy, DataStore settings, WorkManager recovery, Direct Boot device-encrypted snapshot, Open-Meteo/Nager/NWS/Windy/RSS/Hue/Spotify/YouTube/radio/webhook traffic, Play Health Connect `READ_SLEEP`, Play Wear Data Layer, ML Kit Digital Ink.

## Competitive Landscape

### Sleep as Android
- Does well: sleep tracking depth, wearable integrations, anti-snoring/sound detection, assistant/automation experiments, Hue and Google Home style integration.
- Learn: sleep surfaces need credible data-quality labels and clear fallback paths; wearable breadth is a moat.
- Avoid: cloud-dependent coaching or medical-adjacent claims without disclaimers and store-policy budget.

### Alarmy
- Does well: mission/challenge market validation, multi-mission wake flows, object/photo-style dismiss ideas, strong heavy-sleeper positioning.
- Learn: challenge variety is valuable only when every challenge remains dismissable under denied permissions and accessibility needs.
- Avoid: paywalling basic wake reliability or pushing cloud/ML dependencies into the critical alarm path.

### BlackyHawky Clock
- Does well: fast Android UI modernization, M3 Expressive polish, ringtone/audio UX, manual ordering and customization depth.
- Learn: small interaction polish and APK-size discipline matter even for utility apps.
- Avoid: expanding customization faster than alarm-fire proof and migration tests can cover.

### Google Clock
- Does well: platform-integrated swipe semantics, system-clock trust, Pixel/Wear integration where available.
- Learn: Android users expect alarms to survive Doze, reboot, lock screen, and vendor restrictions without explanation.
- Avoid: platform lock-in; ACX can win by supporting non-Pixel Wear OS watches.

### Turbo Alarm / AMdroid / I Can't Wake Up
- Do well: location-aware alarms, wearables, smart pre-alarms, memory/voice/mini-game dismiss patterns, OEM-specific alarm fixes.
- Learn: ACX already has many parity fields; finishing incomplete UI/execution paths is more valuable than adding another half-wired field.
- Avoid: opaque PRO-only automation or aggressive background behavior that worsens battery-kill risk.

### Fossify Clock / Chrono / QRAlarm / yuriykulikov AlarmClock
- Do well: minimal FOSS clock reliability, QR-specific dismiss, simple code surfaces, low-friction builds.
- Learn: FOSS users value small, explainable permission surfaces and reproducible release artifacts.
- Avoid: large optional dependencies in F-Droid builds; keep Play-only features isolated.

### Apple AlarmKit
- Does well: system-level alarm affordances and App Intent dismiss actions.
- Learn: per-alarm post-dismiss actions are a proven UX pattern, but they must execute reliably and report failure.
- Avoid: advertising a custom action type that only logs.

## Security, Privacy, and Reliability

- Verified: `AlarmService.kt` fires per-alarm dismiss actions, but `HUE_SCENE` currently logs `"Dismiss action: Hue scene ... (stub)"` instead of executing a Hue API call. This is a user-trust bug because `Alarm.kt` exposes `dismissActionType = "HUE_SCENE"`.
- Verified: `SonarSleepService.kt` is declared as a microphone foreground service, Bedtime can start/stop it with microphone-permission recovery, and Statistics reads its compact local movement/restless/still summaries from `actigraphy_sessions`. Raw microphone audio is not retained.
- Verified: `app/build.gradle.kts` already constrains Jackson, Commons Compress, Commons IO, Rhino, and Guava and Play-only isolates youtubedl/NewPipe/Health Connect/ML Kit. `scripts/osv_gradle_audit.py` exists, but release proof should run it for Play runtime, F-Droid runtime, and Wear.
- Verified: Direct Boot is now documented in `docs/DIRECT_BOOT_MINIMUM_ALARM.md` and manifest components are `directBootAware`; the remaining proof gap is a device/emulator fire path before first unlock.
- Likely: Android 16/17 notification and local-network changes make release-time API validation more important than feature expansion. `ACCESS_LOCAL_NETWORK` and `POST_PROMOTED_NOTIFICATIONS` are already declared, but compile/target 37 and runtime validation remain future work.
- Likely: battery optimization resets, OEM restrictions, and Pixel alarm regressions remain the main community reliability complaint class. ACX has wake-readiness UI and an `AlarmHealthWorker`; implementation agents should keep this as a P0 regression surface.

## Architecture Assessment

- Module boundaries are mostly healthy: scheduling in `domain/AlarmScheduler.kt`, calculations in `domain/NextAlarmCalculator.kt`, persistence in Room/DataStore, recovery via WorkManager/receivers, and UI in Compose screens/ViewModels.
- Refactor candidates: `AlarmService.kt` still owns alarm playback, vibration, TTS, flashlight, dismiss actions, incidents, and morning flow; custom dismiss actions should move behind a small executor interface so Hue/webhook/broadcast behavior can be tested without a service instance.
- Refactor candidates: `SonarSleepService.kt` now has the Bedtime/Statistics path; future Sonar work should focus on better data-quality labels and device-specific reliability proof.
- Test gaps: no local alarm-fire-to-dismiss smoke harness; no Direct Boot locked-boot device proof; no release-gate invocation of OSV audit across all runtime graphs; no device-level Sonar audio-path smoke beyond unit-tested local summary math.
- Documentation gaps: release workflow needs a pre-push check that planning files and public version strings agree even though most markdown is gitignored.

## Rejected Ideas

- Cloud account sync: conflicts with no-account/no-tracking product posture; encrypted SAF/WebDAV-style backup is a better fit.
- Full plugin SDK: webhooks, broadcasts, and documented recipes cover most automation value with less maintenance and review burden.
- Cloud sleep coach: conflicts with local-first privacy. On-device summaries remain acceptable if model size and F-Droid packaging are controlled.
- Power-off alarms: requires OEM/privileged firmware support and is not achievable as an ordinary Android app.
- Anti-uninstall/accessibility lock tricks: abusive pattern and Play-policy risk.
- More dismiss challenges before trust gaps close: Alarmy and I Can't Wake Up show demand, but ACX already has 27; reliability, accessibility, and test proof now beat raw challenge count.

## Sources

### Project
- https://github.com/SysAdminDoc/AlarmClockXtreme
- https://github.com/SysAdminDoc/AlarmClockXtreme/releases

### Direct OSS Competitors
- https://github.com/BlackyHawky/Clock
- https://github.com/FossifyOrg/Clock
- https://github.com/yuriykulikov/AlarmClock
- https://github.com/sweakpl/qralarm-android
- https://github.com/trikita/talalarmo
- https://github.com/vicolo-dev/chrono

### Commercial / Platform References
- https://sleep.urbandroid.org/documentation/release-notes/
- https://alar.my/en/blog/alarmy-wake-up-mission
- https://play.google.com/store/apps/details?id=com.turbo.alarm
- https://play.google.com/store/apps/details?id=com.amdroidalarmclock.amdroid
- https://support.google.com/wearos/answer/6300982
- https://developer.apple.com/documentation/AlarmKit

### Android / Dependencies / Security
- https://developer.android.com/about/versions/16/features/progress-centric-notifications
- https://developer.android.com/about/versions/17
- https://developer.android.com/build/releases/gradle-plugin
- https://developer.android.com/jetpack/androidx/releases/work
- https://developer.android.com/jetpack/androidx/releases/room
- https://developer.android.com/jetpack/androidx/releases/glance
- https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions
- https://developers.google.com/ml-kit/vision/digital-ink-recognition
- https://github.com/yt-dlp/yt-dlp/releases
- https://osv.dev/

### Community / Distribution
- https://dontkillmyapp.com/
- https://f-droid.org/en/docs/Reproducible_Builds/
- https://developer.android.com/privacy-and-security/direct-boot
- https://www.home-assistant.io/integrations/sleep_as_android/

## Open Questions

- Does the maintainer want Sonar sleep tracking to become a real product path, or should the manifest permission/service/docs be removed before the next public release?
- Should `HUE_SCENE` dismiss actions recall Hue v2 scenes directly, or should they be represented as webhook/broadcast recipes until Hue scene selection has a real UI?
