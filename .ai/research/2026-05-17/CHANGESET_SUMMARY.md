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
