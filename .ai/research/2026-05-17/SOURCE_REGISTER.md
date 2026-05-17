# Source Register

Date: 2026-05-17

This register lists local and external sources used for the 2026-05-17 research
run. Claims in `ROADMAP.md` and sibling research notes should map back here.

## Local Repository Sources

| ID | Source | Used For |
|---|---|---|
| L1 | `git status --short --branch` | Clean worktree and branch ahead state. |
| L2 | `rtk git log -10 --oneline --decorate` | Recent release trajectory. |
| L3 | `git tag --sort=-creatordate` | Tag drift; latest tag `v1.9.5`. |
| L4 | `app/build.gradle.kts` | App SDK targets, app version, dependencies, flavor-specific downloader deps. |
| L5 | `wear/build.gradle.kts` | Wear version parity and Wear dependencies. |
| L6 | `gradle/wrapper/gradle-wrapper.properties` | Gradle version. |
| L7 | `app/src/main/AndroidManifest.xml` | Exact alarm, foreground service, full-screen intent, boot, Health Connect permission state. |
| L8 | `app/src/main/java/com/sysadmindoc/alarmclock/data/local/AlarmDatabase.kt` | DB version and migration definitions. |
| L9 | `app/src/main/java/com/sysadmindoc/alarmclock/di/DatabaseModule.kt` | Migration registration. |
| L10 | `app/schemas/...` | Missing v10 schema export at reconnaissance time; generated v10 schema added by this changeset. |
| L11 | `app/src/main/java/com/sysadmindoc/alarmclock/data/preferences/PreferencesManager.kt` | Health Connect opt-in scaffold. |
| L12 | `app/src/main/java/com/sysadmindoc/alarmclock/data/backup/BackupManager.kt` | Backup v7 Health Connect opt-in round-trip. |
| L13 | `app/src/main/java/com/sysadmindoc/alarmclock/ui/settings/SettingsScreen.kt` | Health Connect settings copy and flavor state. |
| L14 | `app/src/main/java/com/sysadmindoc/alarmclock/worker/HueSunriseWorker.kt` | Hue API v2 HTTPS trust-any-cert implementation. |
| L15 | `app/src/main/java/com/sysadmindoc/alarmclock/ui/settings/SettingsViewModel.kt` | Hue settings/test trust-any-cert implementation. |
| L16 | `.github/workflows/release.yml` | Release workflow debug-artifact drift. |
| L17 | `.github/workflows/version-lint.yml` | Version-line consistency guard. |
| L18 | `README.md` | Public feature list, permission table, stale DB/challenge architecture claims. |
| L19 | `CHANGELOG.md` | Version history, DB/backup bump evidence, Health Connect scaffold evidence. |
| L20 | `ROADMAP.md` | Existing backlog and older source index. |
| L21 | `PRIVACY_POLICY.html` | Health Connect section and stale broader data-flow language. |
| L22 | `metadata/com.sysadmindoc.alarmclock.yml` | Old F-Droid metadata. |
| L23 | `metadata/en-US/fdroid.yml` | Old F-Droid metadata. |
| L24 | `AGENTS.md` | Tool-specific repo instructions. |
| L25 | `CLAUDE.md` | Local working notes and stale memory contradictions. |
| L26 | `.\gradlew.bat :app:dependencies --configuration playReleaseRuntimeClasspath --console=plain` | Play flavor dependency tree. |
| L27 | `.\gradlew.bat :app:dependencies --configuration fdroidReleaseRuntimeClasspath --console=plain` | F-Droid dependency tree. |
| L28 | OSV batch query run from PowerShell | Vulnerability IDs for downloader transitive dependencies. |
| L29 | `scripts/osv_gradle_audit.py` | Repo-local OSV audit implementation for resolved Gradle Maven dependencies. |
| L30 | `.github/workflows/android-ci.yml` | CI unit/build/schema/migration/dependency-audit gates. |
| L31 | `app/src/main/java/com/sysadmindoc/alarmclock/data/health/HealthConnectSleepRepository.kt` | Shared Health Connect summary contract. |
| L32 | `app/src/play/java/com/sysadmindoc/alarmclock/data/health/PlayHealthConnectSleepRepository.kt` | Play-only Health Connect READ_SLEEP implementation. |
| L33 | `app/src/fdroid/java/com/sysadmindoc/alarmclock/data/health/FdroidHealthConnectSleepRepository.kt` | F-Droid no-op Health Connect implementation. |
| L34 | `app/src/play/AndroidManifest.xml` | Play-only Health Connect READ_SLEEP manifest declaration. |
| L35 | `wear/src/main/java/com/sysadmindoc/alarmclock/wear/NextAlarmComplicationDataSourceService.kt` | Wear next-alarm complication data source implementation. |
| L36 | `wear/src/main/AndroidManifest.xml` | Wear tile and complication provider declarations. |
| L37 | `wear/build.gradle.kts` | Wear Tiles, Protolayout, Data Layer, and complication dependencies. |

## Shared Memory And Instruction Sources

| ID | Source | Used For |
|---|---|---|
| M1 | `C:\Users\--\.claude\CLAUDE.md` | Global behavior rules and memory semantics. |
| M2 | `C:\Users\--\CLAUDE.md` | Working protocol, session start, auto-commit/push convention. |
| M3 | `C:\Users\--\.claude\projects\c--Users----repos\memory\MEMORY.md` | Shared memory index and Android stack pointer. |
| M4 | `C:\Users\--\.claude\projects\c--Users----repos\memory\stack-android.md` | Android stack conventions. |
| M5 | `C:\Users\--\.claude\projects\c--Users----repos\memory\android-apk.md` | APK signing/release conventions. |
| M6 | `C:\Users\--\.codex\memories\MEMORY.md` | Codex prior AlarmClockXtreme release memory and autonomous workflow expectations. |
| M7 | `C:\Users\--\.codex\memories\skills\autonomous-roadmap-loop\SKILL.md` | Autonomous roadmap loop procedure. |

## External Platform And Policy Sources

| ID | URL | Used For |
|---|---|---|
| E1 | https://developer.android.com/health-and-fitness/guides/health-connect/develop/sleep-sessions | Health Connect sleep-session API shape. |
| E2 | https://developer.android.com/health-and-fitness/health-connect/experiences/sleep | Health Connect sleep-experience guidance and testing expectations. |
| E3 | https://developer.android.com/health-and-fitness/health-connect/publish | Play publishing requirements for Health Connect data types. |
| E4 | https://support.google.com/googleplay/android-developer/answer/9888170 | Google Play sensitive permissions and exact alarm policy. |
| E5 | https://developer.android.com/about/versions/14/changes/schedule-exact-alarms | Android exact-alarm platform behavior. |
| E6 | https://developer.android.com/about/versions/16/features/progress-centric-notifications | Android 16 `Notification.ProgressStyle`. |
| E7 | https://developer.android.com/develop/ui/views/notifications/live-update | Android live update notification requirements. |
| E8 | https://developer.android.com/develop/background-work/services/fgs/timeout | Android 15 foreground service timeout limits. |
| E9 | https://developer.android.com/privacy-and-security/direct-boot | Direct Boot storage and alarm-clock use case. |
| E10 | https://developer.android.com/topic/performance/vitals/excessive-wakelock | Android vitals excessive wake-lock guidance. |
| E11 | https://developer.android.com/training/wearables/tiles | Wear OS Tiles guidance. |
| E12 | https://developer.android.com/training/wearables/exposing-data-complications | Wear OS complication data-source guidance. |
| E13 | https://developer.android.com/jetpack/androidx/releases/health-connect | AndroidX Health Connect release notes. |
| E14 | https://developer.android.com/jetpack/androidx/releases/room | AndroidX Room release notes. |
| E15 | https://developer.android.com/jetpack/androidx/releases/work | AndroidX WorkManager release notes. |
| E16 | https://developer.android.com/jetpack/androidx/releases/glance | AndroidX Glance release notes. |
| E17 | https://developer.android.com/jetpack/androidx/releases/wear | AndroidX Wear release notes. |
| E18 | https://developer.android.com/build/releases/gradle-plugin | Android Gradle plugin release notes. |
| E19 | https://docs.gradle.org/8.13/release-notes.html | Gradle 8.13 release notes. |
| E20 | https://f-droid.org/en/docs/Anti-Features/ | F-Droid anti-feature definitions. |
| E21 | https://developer.android.com/jetpack/androidx/releases/wear-watchface | AndroidX Wear Watchface / complication data-source 1.3.0 release notes. |

## External Competitor And Ecosystem Sources

| ID | URL | Used For |
|---|---|---|
| C1 | https://alar.my/en | Alarmy missions, wake-up check, sleep analysis positioning. |
| C2 | https://alarmy-android.zendesk.com/hc/en-us/articles/360004242254--Mission-How-can-I-set-the-Alarm-off-method-math-shake-etc- | Alarmy mission configuration. |
| C3 | https://docs.sleep.urbandroid.org/services/health_connect.html | Sleep as Android Health Connect integration. |
| C4 | https://sleepcycle.com/features/smart-alarm-clock/ | Sleep Cycle smart alarm window and light-sleep wake positioning. |
| C5 | https://support.sleepcycle.com/hc/en-us/articles/7859664023452-Using-the-Sleep-Cycle-app | Sleep Cycle feature areas. |
| C6 | https://sleepwave.com/ | Sleepwave contactless motion, breathing, dream journal, sound recordings, watch alarm positioning. |
| C7 | https://github.com/FossifyOrg/Clock | FOSS clock baseline, offline/privacy positioning, widgets/timer/stopwatch. |
| C8 | https://github.com/BlackyHawky/Clock | FOSS AOSP-derived clock, Direct Boot, reproducible builds, permission-readiness lessons. |
| C9 | https://github.com/sweakpl/qralarm-android | QR/barcode dismissal app positioning and signing fingerprint pattern. |
| C10 | https://github.com/yuriykulikov/AlarmClock | Legacy open-source Android alarm baseline. |
| C11 | https://github.com/TeamNewPipe/NewPipeExtractor/releases | NewPipeExtractor release state. |
| C12 | https://github.com/yt-dlp/yt-dlp | yt-dlp release cadence and maintainer surface. |
| C13 | https://github.com/yausername/youtubedl-android/releases | Android downloader library release source. |

## Security Advisory Sources

| ID | URL | Used For |
|---|---|---|
| S1 | https://api.osv.dev/v1/querybatch | Batch vulnerability query against Play-flavor transitive dependencies. |
| S2 | https://osv.dev/vulnerability/GHSA-4g9r-vxhx-9pgx | Commons Compress advisory example; fixed in 1.26.0. |
| S3 | https://osv.dev/vulnerability/GHSA-78wr-2p64-hpwj | Commons IO advisory example; fixed in 2.14.0. |
| S4 | https://osv.dev/vulnerability/GHSA-3w8q-xq97-5j7x | Rhino advisory example. |
| S5 | https://osv.dev/vulnerability/GHSA-72hv-8253-57qq | Jackson Core advisory example. |
