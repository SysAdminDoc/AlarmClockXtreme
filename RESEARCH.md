# Research — AlarmClockXtreme

## Executive Summary
AlarmClockXtreme is a mature, local-first Android wake-reliability suite: exact alarm scheduling, Direct Boot minimum fallback, 30 dismiss challenges, Play/F-Droid flavor separation, Wear tile/complication support, Media3 playback, encrypted backup, local diagnostics, Hue/webhook automation, weather/news, Health Connect sleep summaries, and optional Sonar summaries are already present. The strongest current shape is not raw feature count; it is privacy-preserving alarm reliability for heavy sleepers. Highest-value direction: close trust gaps around automation authenticity, restore safety, offline morning surfaces, adaptive large-screen workflows, and wake-critical service maintainability. Priority opportunities: signed webhooks plus visible delivery status; restore preview before mutation; persistent last-good weather/news states; two-pane tablet/foldable screens beyond the existing navigation rail; continued AlarmService extraction; keep Jackson/AGP/Room/Work upgrades in blocked dependency tracks until compatible; keep sleep-stage/snore/apnea work as later medical-risk-gated work.

## Product Map
- Core workflows: create/edit/reorder alarms, exact alarm fire, challenge-chain dismiss, snooze/wake-confirm, Direct Boot fallback, bedtime/DND, smart wake, timers, stopwatch, world clock, weather/news dashboard, backup/restore, shared alarm import, support bundle export.
- User personas: heavy sleeper, privacy/F-Droid user, Wear OS user, smart-home automator, shift worker, caregiver/guardian user, deaf/HoH user, Play-flavor user who accepts YouTube/Health Connect/ML Kit integrations.
- Platforms and distribution: Android phone app (`minSdk 26`, `targetSdk 36`), `play` and `fdroid` flavors, Wear companion/tile module, GitHub release APKs, F-Droid metadata, Apache-2.0.
- Key integrations and data flows: Room alarms/events/incidents/actigraphy, DataStore settings, WorkManager recovery, device-encrypted Direct Boot cache, Open-Meteo/Nager/NWS/Windy/RSS/Hue/Spotify/YouTube/radio/webhook traffic, Play Health Connect `READ_SLEEP`, Play Wear Data Layer, Play ML Kit Digital Ink.

## Competitive Landscape
### Sleep as Android
- Does well: sleep tracking depth, wearable breadth, Garmin/Pebble/Hue/Google Home style integrations, wake-up check, AI assistant experiments, and frequent release notes.
- Learn: sleep features need data-quality labels, device-specific reliability paths, and clear integration health feedback.
- Avoid: cloud AI dependency and medical-adjacent claims unless the store policy, privacy copy, and disclaimers are explicit.

### Alarmy
- Does well: validates missions as the heavy-sleeper core loop; photo, math, shake, barcode/QR, memory, typing, walking, and squat missions map directly to wake behavior.
- Learn: ACX challenge variety is already competitive; the differentiator is keeping every challenge dismissable under denied permissions, disabled sensors, and accessibility needs.
- Avoid: anti-uninstall or accessibility-service lock patterns; they trade user control for dark-pattern retention.

### BlackyHawky Clock, Fossify Clock, and Chrono
- Do well: privacy-first FOSS positioning, Material 3 polish, small interaction improvements, recent release cadence, offline-only packaging in some variants.
- Learn: ACX should continue Play/F-Droid dependency separation and avoid making F-Droid carry optional large/proprietary-adjacent features.
- Avoid: broad customization before wake-fire, migration, and accessibility regression proof remains green.

### Google/AOSP Clock
- Does well: platform trust, direct system integration, simple mental model, and expected lock-screen alarm behavior.
- Learn: users judge alarm apps by Doze/reboot/lock-screen survival, not feature tables.
- Avoid: platform-only assumptions; ACX can win by supporting non-Pixel devices, Wear watches, and vendor power-management warnings.

### Turbo Alarm and AMdroid
- Do well: heavy-sleeper positioning, Tasker/MacroDroid style automation, cloud sync, folder/music alarms, post-alarm confirmation, and smart wake flows.
- Learn: automation is a paid-tier signal in commercial apps; ACX can keep it local-first by hardening webhooks instead of adding accounts.
- Avoid: opaque cloud sync or PRO-only reliability features that conflict with ACX's no-account stance.

### QRAlarm, Awake, and single-purpose OSS alarm apps
- Do well: one or two focused challenges with simple code and low onboarding friction.
- Learn: ACX needs careful settings hierarchy and restore/import proof because its feature surface is much wider.
- Avoid: adding more challenge types before UI/accessibility smoke coverage catches up.

### Sleep Cycle, Pillow, Apneal, and sleep-research projects
- Do well: smart alarms, snore/audio capture, wearable health metrics, and AI-assisted sleep insights.
- Learn: ACX's actigraphy/Sonar/Health Connect path is credible only with conservative labels, local processing, and non-medical language.
- Avoid: apnea diagnosis or therapy claims; later apnea flags must stay "screening / talk to a clinician" at most.

## Security, Privacy, and Reliability
- Verified: `app/src/main/java/com/sysadmindoc/alarmclock/service/WebhookService.kt` sends HTTPS-only JSON, gates local-network endpoints, and uses an application-lived scope, but outgoing payloads are unsigned and failures are swallowed without user-visible delivery status.
- Verified: `app/src/main/java/com/sysadmindoc/alarmclock/data/backup/BackupManager.kt` rejects unsupported backup versions and warns before export, but import mutates settings/alarms immediately after parsing; there is no dry-run preview, append/replace choice, or conflict-safe disabled-import mode.
- Verified: `Roadmap_Blocked.md` correctly holds API 37, device/emulator, AGP 9, Room/Work, and Jackson-chain blockers; do not duplicate those into the active roadmap.
- Verified: the `Roadmap_Blocked.md` Sonar "dead code" item appears stale against current code and README: `SonarSleepService`, Bedtime start/stop UI, actigraphy summaries, and privacy copy now exist. Reconcile that blocked item in a future docs-hygiene pass when edits outside these two files are allowed.
- Verified: `docs/DIRECT_BOOT_MINIMUM_ALARM.md`, manifest entries, and Android Direct Boot docs support the current minimum fallback boundary: only small device-encrypted alarm metadata should be available before unlock; full custom audio/challenges should remain post-unlock.
- Verified: `app/src/main/java/com/sysadmindoc/alarmclock/ui/alarmfiring/AlarmFiringViewModel.kt` and Settings already implement a timed challenge accessibility bypass, plus typed/fallback paths for several sensor-dependent challenges; remaining accessibility work should be regression smoke, not another generic bypass.
- Likely: Home Assistant-style webhooks treat the webhook ID as the secret; optional HMAC/timestamp headers would mainly help Tasker/MacroDroid/custom receivers, reverse proxies, and users who forward webhooks outside the LAN.
- Likely: weather/news offline resilience is weaker than holidays. `HolidayRepository` keeps stale disk cache, while `WeatherRepository` is memory-only and `NewsRepository` intentionally refetches each time.

## Architecture Assessment
- Module boundaries are mostly healthy: scheduling is in `domain/AlarmScheduler.kt`, calculations in `domain/NextAlarmCalculator.kt`, persistence in Room/DataStore, recovery in WorkManager/receivers, dismiss actions in `service/DismissActionExecutor.kt`, and playback backend abstraction in `service/AlarmPlaybackPlayer.kt`.
- Refactor candidate: `service/AlarmService.kt` is still 1,823 lines and owns vibration, flashlight, TTS, morning briefing, wake-confirm, notification, phone-state, incident, and service lifecycle behavior. Continue extracting controller-sized units with tests rather than changing behavior in place.
- Refactor candidate: `ui/navigation/AppNavigation.kt` already swaps bottom navigation for a rail on wider windows, but high-traffic screens such as `AlarmListScreen.kt`, `AlarmEditScreen.kt`, `SettingsScreen.kt`, and `BedtimeScreen.kt` remain long single-pane surfaces; Material 3 adaptive list/detail guidance is the next large-screen step.
- Refactor candidate: `ui/alarmfiring/challenges/ChallengeViews.kt` is 2,502 lines; future challenge work should separate sensor-backed, text-entry, and game-like challenge components only when paired with Compose/accessibility smoke coverage.
- Test gaps: 54 local test files cover many pure policies, backup drift, direct-boot snapshots, webhooks, and smoke paths, but blocked items still need real device/emulator alarm-fire, Direct Boot, Wear action, Compose UI, and accessibility validation.
- Documentation gaps: `PROJECT_CONTEXT.md` and `LOGO_PROMPTS.md` are stale ignored root docs and conflict with current repo hygiene rules; do not use them as authoritative over live README/CHANGELOG/ROADMAP/code.
- Coverage notes: security/observability, migration/restore, offline resilience, testing, accessibility, mobile/adaptive layout, and maintainability produce new roadmap work; i18n/per-app language, distribution, dependency upgrades, Wear/device validation, plugin ecosystem, and sleep-model bets are already tracked in `ROADMAP.md` or `Roadmap_Blocked.md`; multi-user account sync is rejected below.

## Rejected Ideas
- Full cloud account sync: conflicts with no-accounts/no-tracking posture; encrypted SAF/WebDAV/Drive-style backup remains the acceptable boundary.
- Full plugin SDK: current webhooks, broadcasts, and documented recipes cover most automation value with far less API surface.
- Cloud sleep coach: conflicts with local-first privacy; only on-device summaries remain viable, and only if model size and F-Droid packaging stay controlled.
- Full pre-unlock custom-ringtone/challenge alarm: conflicts with credential-encrypted custom media, challenge secrets, integration URLs, and health data.
- Power-off alarm for ordinary devices: requires OEM/privileged firmware support.
- Anti-uninstall or accessibility-service lock tricks: abusive pattern and Play policy risk.
- Medical sleep-apnea diagnosis: research and commercial products exist, but ACX should not make diagnostic claims without regulatory/legal work.
- More dismiss challenges before proof: ACX already has 30; reliability, restore safety, automation trust, and accessibility regression coverage now have higher value.

## Sources
### Project
- https://github.com/SysAdminDoc/AlarmClockXtreme

### Direct OSS Competitors and Catalogs
- https://github.com/BlackyHawky/Clock/releases
- https://github.com/FossifyOrg/Clock
- https://github.com/vicolo-dev/chrono
- https://github.com/yuriykulikov/alarmclock
- https://github.com/sweakpl/qralarm-android
- https://github.com/adeeteya/Awake-AlarmApp
- https://f-droid.org/en/categories/alarm-clock/

### Commercial and Automation References
- https://sleep.urbandroid.org/docs/general/release_notes.html
- https://alar.my/en/blog/alarmy-wake-up-mission
- https://play.google.com/store/apps/details?id=com.turbo.alarm
- https://sleepcycle.com/sleep-talk/smart-alarm-now-available-in-the-sleep-cycle-sdk
- https://www.home-assistant.io/integrations/sleep_as_android/
- https://www.home-assistant.io/docs/automation/trigger/
- https://ngrok.com/blog/get-webhooks-secure-it-depends-a-field-guide-to-webhook-security

### Android, Dependencies, and Security
- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/privacy-and-security/direct-boot
- https://developer.android.com/identity/data/autobackup
- https://developer.android.com/privacy-and-security/local-network-permission
- https://android-developers.googleblog.com/2024/09/jetpack-compose-apis-for-building-adaptive-layouts-material-guidance-now-stable.html
- https://developer.android.com/jetpack/androidx/releases/compose-material3
- https://developer.android.com/jetpack/androidx/releases/room
- https://developer.android.com/jetpack/androidx/releases/work
- https://github.com/advisories/GHSA-5jmj-h7xm-6q6v

### Academic and Community Signal
- https://www.nature.com/articles/s41598-024-54727-0
- https://docs.edgeimpulse.com/projects/expert-network/snoring-detection-on-smartphone
- https://www.apneal.ai/
- https://dontkillmyapp.com/
- https://discuss.privacyguides.net/t/can-i-mitigate-some-of-the-privacy-issues-of-the-android-app-alarmy-by-removing-network-permission/24492

## Open Questions
None.
