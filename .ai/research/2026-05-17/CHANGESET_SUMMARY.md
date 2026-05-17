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
