# Changeset Summary

Date: 2026-05-17

## Files Added

- `PROJECT_CONTEXT.md` - canonical consolidated project context for future AI
  and maintainer sessions.
- `.ai/research/2026-05-17/STATE_OF_REPO.md` - local repository reconnaissance
  memo.
- `.ai/research/2026-05-17/MEMORY_CONSOLIDATION.md` - instruction/memory
  inventory and reconciliation.
- `.ai/research/2026-05-17/SOURCE_REGISTER.md` - local, memory, external,
  competitor, and security source register.
- `.ai/research/2026-05-17/RESEARCH_LOG.md` - search strategies, commands,
  failed searches, and saturation notes.
- `.ai/research/2026-05-17/COMPETITOR_MATRIX.md` - competitor and adjacent
  project matrix.
- `.ai/research/2026-05-17/FEATURE_BACKLOG.md` - raw harvested ideas before
  prioritization.
- `.ai/research/2026-05-17/PRIORITIZATION_MATRIX.md` - scored candidate matrix.
- `.ai/research/2026-05-17/SECURITY_AND_DEPENDENCY_REVIEW.md` - dependency,
  OSV, release, privacy, and platform hardening review.
- `.ai/research/2026-05-17/DATASET_MODEL_INTEGRATION_REVIEW.md` - data,
  model, Health Connect, integration, and evaluation review.
- `app/schemas/com.sysadmindoc.alarmclock.data.local.AlarmDatabase/10.json` -
  generated Room v10 schema export discovered missing during reconnaissance.

## Files Modified

- `ROADMAP.md` - added a 2026-05-17 deep-research refresh and corrected release
  workflow drift language.
- `app/src/main/java/com/sysadmindoc/alarmclock/ui/alarmedit/AlarmEditScreen.kt`
  - restored the missing `AppFilterChip` import discovered during verification.
- `app/src/main/java/com/sysadmindoc/alarmclock/ui/settings/SettingsScreen.kt`
  - restored the missing `AppFilterChip` import discovered during verification.

## Verification

- `git diff --check` passed. Git reported the existing line-ending normalization
  warning that `ROADMAP.md` will be converted to CRLF the next time Git touches
  it.
- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain` passed after the import repair. Gradle still reports existing deprecation warnings for `PhoneStateListener`, `Vibrator.vibrate(...)`, `Icons.Filled.VolumeOff`, `rememberSwipeToDismissBoxState(confirmValueChange)`, `CrashLogger` package info, and Moshi KAPT codegen.

## Autonomous Roadmap Pass: R1 / R3 / R6 Partial

Date: 2026-05-17

### Files Modified

- `PRIVACY_POLICY.html` - replaced the older Open-Meteo-only policy with
  current data-flow disclosures for weather, air quality, geocoding, NWS,
  Nager.Date, Windy, RSS/news, user webhooks, internet radio, Hue LAN, Play-only
  YouTube resolution/downloads, local crash logs, backups, and the current
  Health Connect scaffold.
- `README.md` - corrected 22 challenge count, Room DB v10, backup format v7,
  network/privacy disclosures, F-Droid spelling, and Google Play Services
  wording.
- `app/src/main/java/com/sysadmindoc/alarmclock/ui/settings/SettingsScreen.kt`
  - changed Health Connect settings copy to say v1.13.1 stores only a local
  preference and does not request permission or read sleep data yet.
- `metadata/com.sysadmindoc.alarmclock.yml` and `metadata/en-US/fdroid.yml` -
  refreshed current version/code and optional network anti-feature language.
- `PROJECT_CONTEXT.md` and `ROADMAP.md` - marked R1/R3 complete, kept R6 partial
  because ignored local `CLAUDE.md` still has stale tool notes, and moved release
  automation to the next active priority.

### Verification

- `git diff --check` passed with only existing line-ending normalization
  warnings.
- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain`
  passed after the Health Connect copy change.

## Autonomous Roadmap Pass: R2 Release Automation

Date: 2026-05-17

### Files Modified

- `.github/workflows/release.yml` - replaced the tag-triggered debug APK
  workflow with a signed release workflow for Play, F-Droid, and Wear APKs. The
  workflow validates signing secrets, writes `keystore.properties`, rejects
  tag/version mismatches, builds release variants, verifies signatures and APK
  badging, generates `SHA256SUMS.txt`, uploads workflow artifacts, and attaches
  APKs plus hashes to GitHub Releases.
- `wear/src/main/java/com/sysadmindoc/alarmclock/wear/NextAlarmTileService.kt` -
  removed the release-only dependency on `com.google.common.util.concurrent.Futures`
  by returning resources through `CallbackToFutureAdapter`, matching the existing
  `androidx.concurrent` dependency.
- `README.md` - documented local release tasks and the preferred/legacy GitHub
  signing-secret names.
- `PROJECT_CONTEXT.md` and `ROADMAP.md` - marked R2 complete and moved the next
  active implementation priority to the Room migration/schema gate.

### Verification

- `python -c "import yaml, pathlib; yaml.safe_load(...)"` parsed
  `.github/workflows/release.yml`.
- Extracted all 9 shell `run:` blocks from `.github/workflows/release.yml` and
  checked them with `bash -n`.
- `git diff --check` passed with only existing line-ending normalization
  warnings.
- `.\gradlew.bat :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease --console=plain`
  passed using a temporary local test keystore. The pre-existing ignored
  `keystore.properties` was restored afterward.
- `apksigner verify --verbose --print-certs` and `aapt2 dump badging` passed for
  `app-play-release.apk`, `app-fdroid-release.apk`, and `wear-release.apk`;
  all report `versionCode=66` and `versionName=1.13.1`.
- `.\gradlew.bat :wear:assembleDebug --console=plain` passed after the Wear
  tile future fix.

## Autonomous Roadmap Pass: R4 Room Schema Gate

Date: 2026-05-17

### Files Modified

- `app/src/main/java/com/sysadmindoc/alarmclock/data/local/AlarmDatabase.kt` -
  added `ALL_MIGRATIONS` so production database setup, tests, and future schema
  work share the same ordered migration list.
- `app/src/main/java/com/sysadmindoc/alarmclock/di/DatabaseModule.kt` - switched
  the Room builder to `AlarmDatabase.ALL_MIGRATIONS`.
- `app/build.gradle.kts` - exposes `app/schemas` as androidTest assets and adds
  Room migration-test dependencies.
- `app/src/androidTest/java/com/sysadmindoc/alarmclock/data/local/AlarmDatabaseMigrationTest.kt`
  - added migration/fresh-install tests for earliest exported schema to latest,
  v9 to v10 defaulting, latest exported schema parity, and contiguous migration
  registration.
- `.github/workflows/android-ci.yml` - added push/PR CI for unit tests, debug
  builds, schema-export drift detection, and emulator-backed Room migration
  tests.
- `PROJECT_CONTEXT.md` and `ROADMAP.md` - marked R4 complete and moved the next
  active priority to Play-only downloader dependency risk.

### Verification

- Workflow YAML parsing passed for all `.github/workflows/*.yml`.
- Extracted and `bash -n` checked all 15 shell `run:` blocks across GitHub
  workflows.
- `git diff --check` passed with only existing line-ending normalization
  warnings.
- `.\gradlew.bat :app:compileFdroidDebugAndroidTestKotlin --console=plain`
  passed, proving the Room migration instrumentation test source compiles.
- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain`
  passed.

## Autonomous Roadmap Pass: X4 Direct Boot Minimum Alarm Prototype

Date: 2026-05-17

### Files Modified

- `app/src/main/java/com/sysadmindoc/alarmclock/directboot/DirectBootAlarmSnapshot.kt`
  - added the pure minimum snapshot model and policy for pre-unlock alarm
  fallback data.
- `app/src/main/java/com/sysadmindoc/alarmclock/directboot/DirectBootAlarmCache.kt`
  - added device-encrypted SharedPreferences storage, earliest-next-alarm
  cache maintenance, locked-boot scheduling, stale fallback cancellation, and
  one-shot fired-marker handoff for post-unlock reschedule.
- `app/src/main/java/com/sysadmindoc/alarmclock/directboot/DirectBootAlarmReceiver.kt`
  and `DirectBootAlarmService.kt` - added the Direct-Boot-aware fallback alarm
  path that posts a foreground notification, plays only the system default
  alarm/notification tone, vibrates when allowed, and auto-stops after ten
  minutes.
- `BootReceiver.kt` and `AndroidManifest.xml` - added
  `LOCKED_BOOT_COMPLETED`, `android:directBootAware="true"` declarations, and
  isolated locked-boot behavior from normal WorkManager rescheduling.
- `AlarmClockApp.kt` - deferred normal crash logger, DataStore, WorkManager,
  Wear bridge, downloader, and default-alarm seeding until user credential
  storage is unlocked.
- `AlarmScheduler.kt` - writes/rebuilds the Direct Boot snapshot during normal
  scheduling, cancels stale fallback PendingIntents after unlock, and consumes
  one-shot alarms that fired through the fallback.
- `DirectBootAlarmSnapshotTest.kt` - covers schedulability and the no-label /
  no-custom-URI snapshot policy.
- `docs/DIRECT_BOOT_MINIMUM_ALARM.md` - documents the design, stored fields,
  runtime flow, boundaries, and verification expectations.
- `CHANGELOG.md`, `README.md`, `ROADMAP.md`, `PROJECT_CONTEXT.md`,
  F-Droid metadata, and research notes - bumped/synced version lines to
  `1.13.5` / code `70` and marked roadmap X4 complete.

### Verification

- Official Android Direct Boot documentation checked during implementation:
  alarm-clock apps are listed as a Direct Boot use case; Direct-Boot-aware
  components must use device-encrypted storage before unlock and register
  `ACTION_LOCKED_BOOT_COMPLETED`.
- `.\gradlew.bat :app:compilePlayDebugKotlin --console=plain` passed after
  adding the new direct-boot package.
- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain`
  passed.
- `python scripts\osv_gradle_audit.py --configuration playReleaseRuntimeClasspath`
  passed: 207 Maven dependencies resolved; no OSV vulnerabilities reported.
- Version consistency check passed for app, Wear, README badge, README install
  command, changelog top entry, and roadmap current snapshot: all report
  `1.13.5`.
- `git diff --check` passed with only existing line-ending normalization
  warnings.
- `git diff --exit-code -- app/schemas` passed after the Gradle build.
- `.\gradlew.bat :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease --console=plain`
  passed with a temporary verification keystore created at the ignored local
  `keystore.properties` path and removed afterward.
- `apksigner verify --verbose` and `aapt2 dump badging` passed for Play,
  F-Droid, and Wear release APKs: all report `versionCode=70` and
  `versionName=1.13.5`; all verify with APK Signature Scheme v2.
- `aapt2 dump xmltree --file AndroidManifest.xml` confirmed Play and F-Droid
  release manifests include `LOCKED_BOOT_COMPLETED`, direct-boot-aware
  `BootReceiver`, `DirectBootAlarmReceiver`, and `DirectBootAlarmService`.
- `aapt2 dump permissions` confirmed `android.permission.health.READ_SLEEP`
  remains Play-only.

## Autonomous Roadmap Pass: X5 Local Support Export

Date: 2026-05-17

### Files Modified

- `SupportExportManager.kt` - added a user-triggered FileProvider-backed ZIP
  export from `cacheDir/support_exports`, containing diagnostics, redacted
  alarm metadata, and newest local crash logs.
- `SupportDiagnosticsFormatter.kt` - added pure formatting for
  `diagnostics.txt` and `alarms_redacted.csv`, intentionally omitting labels,
  custom media URIs, integration URLs/secrets, contact/location/Wi-Fi values,
  challenge references, and Health Connect records.
- `CrashLogger.kt` - exposed newest-first crash log files for support packaging
  while keeping existing log text APIs.
- `AndroidManifest.xml` and `res/xml/file_paths.xml` - registered the
  non-exported `FileProvider` used to share generated support ZIPs.
- `SettingsViewModel.kt` and `SettingsScreen.kt` - added the Settings "Export
  support bundle" action, busy/result state, share sheet launch, and failure
  feedback.
- `SupportDiagnosticsFormatterTest.kt` - added redaction coverage for private
  alarm labels, raw URIs, integration values, contact, Wi-Fi, and NFC data.
- `CHANGELOG.md`, `README.md`, `ROADMAP.md`, `PROJECT_CONTEXT.md`,
  F-Droid metadata, and research notes - bumped/synced version lines to
  `1.13.6` / code `71` and marked roadmap X5 complete.

### Verification

- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain`
  passed after the support export implementation.
- `git diff --exit-code -- app/schemas` passed; no Room schema drift.
- `git diff --check` passed with only line-ending normalization warnings.
- Version consistency check passed for app, Wear, README, CHANGELOG, ROADMAP,
  and PROJECT_CONTEXT: all report `1.13.6` / code `71`.
- `python scripts\osv_gradle_audit.py --configuration playReleaseRuntimeClasspath`
  passed: 207 Maven dependencies resolved; no OSV vulnerabilities reported.
- `.\gradlew.bat :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease --console=plain`
  passed with a temporary local signing key removed after verification.
- `apksigner verify --verbose` and `aapt2 dump badging` passed for Play,
  F-Droid, and Wear release APKs: all report `versionCode=71` and
  `versionName=1.13.6`; all verify with APK Signature Scheme v2.
- `aapt2 dump xmltree --file AndroidManifest.xml` confirmed the release
  manifest includes the non-exported
  `com.sysadmindoc.alarmclock.fileprovider` provider and preserves the Direct
  Boot receivers/services.
- `aapt2 dump permissions` confirmed `android.permission.health.READ_SLEEP`
  remains Play-only.

## Autonomous Roadmap Pass: X6 Sleep/Wake Analytics Charts

Date: 2026-05-17

### Files Modified

- `HealthConnectSleepRepository.kt` and
  `PlayHealthConnectSleepRepository.kt` - extended the foreground summary model
  with recent sleep-session windows for Statistics-only correlation.
- `SleepWakeAnalytics.kt` - added a pure local analytics model that pairs sleep
  ending dates with Room alarm events for sleep duration, dismiss response,
  snoozes, challenge solve time, and challenge retries.
- `AlarmEvent.kt`, `AlarmDatabase.kt`, `AlarmEventDao.kt`,
  `AlarmEventRepository.kt`, and `app/schemas/.../11.json` - bumped Room to
  v11 and persisted `challengeRetryCount` with a default-zero migration.
- `AlarmFiringViewModel.kt`, `AlarmFiringActivity.kt`, and `AlarmService.kt` -
  counted wrong challenge attempts, passed retry/solve metrics on dismiss, and
  stored them with dismissed alarm events.
- `StatsViewModel.kt` and `StatsScreen.kt` - added 14-day sleep/wake and
  wake-friction chart cards without adding a new chart dependency.
- `SleepWakeAnalyticsTest.kt` and `AlarmDatabaseMigrationTest.kt` - added unit
  coverage for correlation and migration coverage for the v11 event column.
- `CHANGELOG.md`, `README.md`, `ROADMAP.md`, `PROJECT_CONTEXT.md`,
  F-Droid metadata, and research notes - bumped/synced version lines to
  `1.13.7` / code `72`, marked roadmap X6 complete, and documented the DB v11
  privacy boundary.

### Verification

- `.\gradlew.bat :app:testPlayDebugUnitTest --tests "com.sysadmindoc.alarmclock.ui.stats.SleepWakeAnalyticsTest" --tests "com.sysadmindoc.alarmclock.ui.stats.StatsFiltersTest" --console=plain`
  passed after the analytics implementation and Room v11 schema export.
- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain`
  passed for the full Play/F-Droid/Wear debug gate.
- `.\gradlew.bat :app:assemblePlayDebugAndroidTest --console=plain` passed,
  compiling the Room migration instrumentation tests including
  `MIGRATION_10_11`.
- `git diff --check` passed with only line-ending normalization warnings.
- Schema check confirmed the only Room schema change is the expected new
  `app/schemas/com.sysadmindoc.alarmclock.data.local.AlarmDatabase/11.json`.
- Version consistency check passed for app, Wear, README, CHANGELOG, ROADMAP,
  PROJECT_CONTEXT, and F-Droid metadata: all report `1.13.7` / code `72`.
- `python scripts\osv_gradle_audit.py --configuration playReleaseRuntimeClasspath`
  passed: 207 Maven dependencies resolved; no OSV vulnerabilities reported.
- `.\gradlew.bat :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease --console=plain`
  passed with a temporary local signing key removed after verification.
- `apksigner verify --verbose` and `aapt2 dump badging` passed for Play,
  F-Droid, and Wear release APKs: all report `versionCode=72` and
  `versionName=1.13.7`; all verify with APK Signature Scheme v2.
- `aapt2 dump permissions` confirmed `android.permission.health.READ_SLEEP`
  remains Play-only, and `aapt2 dump xmltree --file AndroidManifest.xml`
  confirmed the FileProvider and Direct Boot manifest entries remain present.

## Autonomous Roadmap Pass: X2 Backup Export Warning

Date: 2026-05-17

### Files Modified

- `app/src/main/java/com/sysadmindoc/alarmclock/data/backup/BackupManager.kt` -
  added `BackupExportWarning`, `assessExportWarning(...)`,
  `inspectExportWarning()`, backup format v8, and `SettingsBackup.newsFeedUrl`
  round-trip support.
- `app/src/main/java/com/sysadmindoc/alarmclock/data/preferences/PreferencesManager.kt`
  - extracted the default news feed URL constant so DataStore defaults and
  backup warning classification stay aligned.
- `app/src/main/java/com/sysadmindoc/alarmclock/ui/settings/SettingsViewModel.kt`
  - exposed the backup export warning scan to Settings.
- `app/src/main/java/com/sysadmindoc/alarmclock/ui/settings/SettingsScreen.kt`
  - added confirmation dialogs before plain/encrypted export when configured
  webhook URLs, Hue details/API keys, custom feed URLs, stream URLs,
  device-local media/photo URIs, Wi-Fi/location/contact details, or NFC/barcode
  challenge values would be included.
- `app/src/test/java/com/sysadmindoc/alarmclock/data/backup/BackupExportWarningTest.kt`
  - added pure unit coverage for clean, settings-risk, and alarm-risk export
  warning classifications.
- `app/build.gradle.kts`, `wear/build.gradle.kts`, `CHANGELOG.md`, README,
  ROADMAP, `PROJECT_CONTEXT.md`, and F-Droid metadata - bumped/synced version
  lines to `1.13.3` / code `68` and marked X2 complete.

### Verification

- `.\gradlew.bat :app:testPlayDebugUnitTest --tests "com.sysadmindoc.alarmclock.data.backup.BackupExportWarningTest" --console=plain`
  passed.
- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain`
  passed.
- Version consistency check passed for app, Wear, README badge, README install
  command, changelog top entry, and roadmap current snapshot: all report
  `1.13.3`.
- Workflow YAML/shell syntax check passed for all 17 GitHub Actions shell
  `run:` blocks.
- `git diff --exit-code -- app/schemas` passed.
- `python scripts\osv_gradle_audit.py --configuration playReleaseRuntimeClasspath`
  passed: 207 Maven dependencies resolved; no OSV vulnerabilities reported.
- `git diff --check` passed with only existing line-ending normalization
  warnings.
- `.\gradlew.bat :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease --console=plain`
  passed with a temporary local signing key. The ignored local
  `keystore.properties` was restored afterward.
- `aapt2 dump permissions`, `aapt2 dump badging`, and
  `apksigner verify --verbose` passed for Play, F-Droid, and Wear release APKs:
  all report `versionCode=68` and `versionName=1.13.3`; only the Play APK
  declares `android.permission.health.READ_SLEEP`.

## Autonomous Roadmap Pass: X3 Wear Next-Alarm Complication

Date: 2026-05-17

### Files Modified

- `wear/build.gradle.kts` - added
  `androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0` and
  bumped Wear to version `1.13.4` / code `69`.
- `wear/src/main/java/com/sysadmindoc/alarmclock/wear/NextAlarmComplicationDataSourceService.kt`
  - added a `SuspendingComplicationDataSourceService` that serves `SHORT_TEXT`
  and `LONG_TEXT` next-alarm complications from the existing cached Wear alarm
  snapshot and provides watch-face picker preview data.
- `wear/src/main/java/com/sysadmindoc/alarmclock/wear/WearAlarmDataListenerService.kt`
  - requests both tile and complication updates when the phone publishes a new
  Data Layer next-alarm snapshot.
- `wear/src/main/AndroidManifest.xml` and `wear/src/main/res/values/strings.xml`
  - registered the complication provider with supported types, update period,
  direct-boot awareness, system bind permission, label, and description.
- `app/build.gradle.kts`, `CHANGELOG.md`, README, ROADMAP,
  `PROJECT_CONTEXT.md`, F-Droid metadata, and research notes - bumped/synced
  version lines to `1.13.4` / code `69` and marked X3 complete.

### Verification

- `.\gradlew.bat :wear:compileDebugKotlin --console=plain` passed after adding
  the complication API dependency and provider implementation.
- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain`
  passed.
- `python scripts\osv_gradle_audit.py --configuration playReleaseRuntimeClasspath`
  passed: 207 Maven dependencies resolved; no OSV vulnerabilities reported.
- `python scripts\osv_gradle_audit.py --project :wear --configuration releaseRuntimeClasspath`
  passed: 70 Maven dependencies resolved; no OSV vulnerabilities reported.
- `.\gradlew.bat :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease --console=plain`
  passed with a temporary local signing key. The ignored local
  `keystore.properties` was restored afterward.
- `aapt2 dump permissions`, `aapt2 dump badging`, and
  `apksigner verify --verbose` passed for Play, F-Droid, and Wear release APKs:
  all report `versionCode=69` and `versionName=1.13.4`; only the Play APK
  declares `android.permission.health.READ_SLEEP`.
- `aapt2 dump xmltree --file AndroidManifest.xml wear-release.apk` confirmed
  `NextAlarmComplicationDataSourceService`,
  `BIND_COMPLICATION_PROVIDER`, `SUPPORTED_TYPES`, and
  `UPDATE_PERIOD_SECONDS` are present in the Wear release manifest.
- Version consistency check passed for app, Wear, README badge, README install
  command, changelog top entry, and roadmap current snapshot: all report
  `1.13.4`.
- Workflow YAML/shell syntax check passed for all 17 GitHub Actions shell
  `run:` blocks.
- `git diff --exit-code -- app/schemas` passed.
- `git diff --check` passed with only existing line-ending normalization
  warnings.

## Autonomous Roadmap Pass: R6 Documentation Drift Policy

Date: 2026-05-17

### Files Modified

- `CHANGELOG.md` - corrected the v1.13.1 Health Connect release notes to match
  the current no-SDK/no-permission/no-read scaffold and the 2026-05-17 privacy
  reconciliation.
- `PROJECT_CONTEXT.md` - recorded the decision that ignored local `AGENTS.md`
  and `CLAUDE.md` remain local scratchpads; tracked context files are
  authoritative when those ignored files conflict.
- `ROADMAP.md` - marked R6 complete and documented the ignored-tool-file
  decision.
- `.ai/research/2026-05-17/MEMORY_CONSOLIDATION.md` - moved the CLAUDE/AGENTS,
  release workflow, privacy, and downloader conflicts into resolved status.

### Verification

- Evidence checks: `git ls-files` confirmed `AGENTS.md` and `CLAUDE.md` are not
  tracked; `git check-ignore -v` showed `AGENTS.md` is ignored by the user's
  global Git ignore and `CLAUDE.md` by repo `.gitignore`.
- No code changed in this pass; verification was limited to documentation
  consistency and `git diff --check`.

## Autonomous Roadmap Pass: X1 Health Connect READ_SLEEP Integration

Date: 2026-05-17

### Files Modified

- `app/build.gradle.kts` - added `androidx.health.connect:connect-client:1.1.0`
  as a Play-flavor-only dependency and constrained the new Play-only Guava
  transitive to `33.6.0-android` after OSV flagged `31.1-android`.
- `app/src/play/AndroidManifest.xml` - declared only
  `android.permission.health.READ_SLEEP` for the Play flavor.
- `app/src/main/java/com/sysadmindoc/alarmclock/data/health/HealthConnectSleepRepository.kt`
  - added the common flavor-safe contract and local summary model.
- `app/src/play/java/com/sysadmindoc/alarmclock/data/health/PlayHealthConnectSleepRepository.kt`
  - added the real Health Connect implementation: SDK status, permission
  status, recent `SleepSessionRecord` reads, and stage/duration summarization.
- `app/src/fdroid/java/com/sysadmindoc/alarmclock/data/health/FdroidHealthConnectSleepRepository.kt`
  - added a no-op F-Droid implementation with no SDK or permission path.
- `PlayFlavorModule.kt` and `FdroidFlavorModule.kt` - bound the flavor-specific
  Health Connect repositories through Hilt.
- `SettingsViewModel.kt` / `SettingsScreen.kt` - wired status refresh,
  permission request, READ_SLEEP result handling, and Settings summaries.
- `BedtimeViewModel.kt` / `BedtimeScreen.kt` - surfaced recent local sleep
  summaries in the Bedtime planning flow.
- `StatsViewModel.kt` / `StatsScreen.kt` - surfaced local sleep context beside
  alarm history.
- `PRIVACY_POLICY.html`, `README.md`, `ROADMAP.md`, `PROJECT_CONTEXT.md`, and
  research notes - updated policy and roadmap state for the actual Play-only
  READ_SLEEP behavior.
- `app/build.gradle.kts`, `wear/build.gradle.kts`, `CHANGELOG.md`, README,
  ROADMAP, and F-Droid metadata - bumped/synced version lines to `1.13.2` /
  code `67` so the new user-facing integration is not misreported as v1.13.1.

### Verification

- Official Android docs checked during implementation:
  Health Connect sleep sessions require `android.permission.health.READ_SLEEP`
  for reads, `PermissionController.createRequestPermissionResultContract()`
  requests Health permissions, and `connect-client:1.1.0` is the latest stable
  release noted in AndroidX release notes.
- `.\gradlew.bat :app:compilePlayDebugKotlin :app:compileFdroidDebugKotlin --console=plain`
  passed after the flavor split and Guava constraint were wired.
- `python scripts\osv_gradle_audit.py --configuration playDebugRuntimeClasspath`
  passed: 212 Maven dependencies resolved; no OSV vulnerabilities reported.
- `python scripts\osv_gradle_audit.py --configuration playReleaseRuntimeClasspath`
  passed after the Guava constraint: 207 Maven dependencies resolved; no OSV
  vulnerabilities reported.
- F-Droid release runtime dependency inspection passed with no
  `androidx.health.connect` or `connect-client` dependency present.
- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain`
  passed.
- Version consistency check passed for app, Wear, README badge, README install
  command, changelog top entry, and roadmap current snapshot: all report
  `1.13.2`.
- Workflow YAML/shell syntax check passed for all 17 GitHub Actions shell
  `run:` blocks.
- `git diff --check` passed with only existing line-ending normalization
  warnings.
- `.\gradlew.bat :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease --console=plain`
  passed with a temporary local signing key. The ignored local
  `keystore.properties` was restored afterward.
- `aapt2 dump permissions`, `aapt2 dump badging`, and
  `apksigner verify --verbose` passed for Play, F-Droid, and Wear release APKs:
  all report `versionCode=67` and `versionName=1.13.2`; only the Play APK
  declares `android.permission.health.READ_SLEEP`.
- `git diff --exit-code -- app/schemas` passed after the Gradle build.
- `adb devices` reported no attached device, so
  `connectedFdroidDebugAndroidTest` was not runnable locally; the new GitHub
  Actions emulator job is the execution gate for those tests.

## Autonomous Roadmap Pass: R5 Play Downloader Dependency Hardening

Date: 2026-05-17

### Files Modified

- `app/build.gradle.kts` - added Play-only constraints for stale downloader
  transitives (`jackson-* 2.18.6`, `commons-compress 1.28.0`,
  `commons-io 2.20.0`, `rhino 1.8.1`) and added Play-only `org.tukaani:xz:1.10`
  to satisfy the constrained Commons Compress release during release shrinking.
- `app/proguard-rules.pro` - documented and suppressed the unused optional
  Commons Compress Zstandard class warning instead of shipping unnecessary
  native Zstd code.
- `scripts/osv_gradle_audit.py` - added a stdlib-only OSV batch-query audit for
  resolved Gradle Maven dependencies.
- `.github/workflows/android-ci.yml` - added a dependency-audit job that runs
  the OSV script against `playReleaseRuntimeClasspath`.
- `ROADMAP.md`, `PROJECT_CONTEXT.md`,
  `.ai/research/2026-05-17/SECURITY_AND_DEPENDENCY_REVIEW.md`,
  `.ai/research/2026-05-17/SOURCE_REGISTER.md`, and
  `.ai/research/2026-05-17/RESEARCH_LOG.md` - recorded the R5 resolution and
  current verification posture.

### Verification

- `python scripts\osv_gradle_audit.py --configuration playDebugRuntimeClasspath`
  passed: 204 Maven dependencies resolved; no OSV vulnerabilities reported.
- `python scripts\osv_gradle_audit.py --configuration playReleaseRuntimeClasspath`
  passed: 199 Maven dependencies resolved; no OSV vulnerabilities reported.
- `.\gradlew.bat :app:assemblePlayRelease --console=plain` passed with a
  temporary local signing key after the release-shrinker fix. The ignored local
  `keystore.properties` was restored afterward.
- `.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain`
  passed.
