# AlarmClockXtreme Project Context

Snapshot: 2026-05-17

This is the canonical project context for future AI and maintainer sessions. It
consolidates current repo state, durable architecture facts, release rules,
known drift, and the active research plan. Tool-specific instructions remain in
`AGENTS.md` and `CLAUDE.md`; this file is the shared project memory.

## Current State

- Repo: `C:\Users\--\repos\AlarmClockXtreme`
- Remote: `https://github.com/SysAdminDoc/AlarmClockXtreme.git`
- Branch at latest update: `main`, tracking `origin/main`
- App version: `versionName = "1.13.7"`, `versionCode = 72`
- Android targets: `compileSdk = 36`, `targetSdk = 35`, `minSdk = 26`
- Database: Room `AlarmDatabase` version 11 with `exportSchema = true`
- Backup format: v8
- Modules: `:app` phone app plus `:wear` Wear OS companion/tile module
- Flavors: `play` includes YouTube downloader and Wear Data Layer; `fdroid`
  excludes Play-specific/proprietary-adjacent dependencies

## Product Stance

AlarmClockXtreme is a privacy-first Android alarm clock for heavy sleepers and
routine-driven users. The differentiator is not a minimal stock clock clone; it
is a wake-reliability system with challenge dismissal, exact scheduling,
wearable affordances, weather/news context, bedtime DND automation, backup, and
transparent F-Droid/Play flavor boundaries.

Preserve these principles:

- No ads, tracking SDKs, accounts, or cloud-first architecture.
- Keep alarm delivery reliable before adding wellness, analytics, or novelty.
- Keep Play-only capabilities behind the `play` flavor and avoid contaminating
  the F-Droid artifact.
- Treat permissions, wake locks, foreground services, full-screen alarms, and
  Health Connect as trust surfaces, not implementation details.
- When schema, backup, or settings fields change, update tests, docs, and
  migration/export artifacts in the same change set.

## Architecture Map

- `app/src/main/java/com/sysadmindoc/alarmclock/data/model/Alarm.kt`
  Room entity and alarm field contract.
- `app/src/main/java/com/sysadmindoc/alarmclock/data/local/AlarmDatabase.kt`
  Room DB version and migrations. Current DB is v11 with `MIGRATION_10_11`.
- `app/src/main/java/com/sysadmindoc/alarmclock/di/DatabaseModule.kt`
  Hilt-provided singleton Room DB and migration registration.
- `app/src/main/java/com/sysadmindoc/alarmclock/service/AlarmService.kt`
  wake-critical alarm firing, foreground service, audio, vibration, challenge
  handoff, snooze/dismiss behavior.
- `app/src/main/java/com/sysadmindoc/alarmclock/directboot/*`
  Direct Boot minimum-alarm fallback. Stores only the next alarm id, trigger
  time, display time, default-sound flag, and vibration flag in
  device-encrypted storage; no labels, custom URIs, integration secrets, or
  challenge data.
- `app/src/main/java/com/sysadmindoc/alarmclock/receiver/*`
  boot, exact-alarm permission, unlock/missed-alarm, and alarm event receivers.
- `app/src/main/java/com/sysadmindoc/alarmclock/data/preferences/PreferencesManager.kt`
  DataStore-backed `AppSettings`, including the Health Connect opt-in flag.
- `app/src/main/java/com/sysadmindoc/alarmclock/data/health/HealthConnectSleepRepository.kt`
  common contract for local sleep summaries and recent session windows; Play
  binds the real Health Connect `READ_SLEEP` implementation and F-Droid binds a
  no-op repository.
- `app/src/main/java/com/sysadmindoc/alarmclock/ui/stats/SleepWakeAnalytics.kt`
  pure local analytics model that pairs Health Connect sleep ending dates with
  Room alarm events for sleep duration, dismiss response, snoozes, and
  challenge retries.
- `app/src/main/java/com/sysadmindoc/alarmclock/data/backup/BackupManager.kt`
  backup/restore contract, backup format v8, and pre-export warning risk scan.
- `app/src/main/java/com/sysadmindoc/alarmclock/data/support/*`
  local support ZIP export. Packages crash logs and redacted app/device/alarm
  diagnostics through a FileProvider without telemetry upload.
- `app/src/main/java/com/sysadmindoc/alarmclock/ui/settings/SettingsScreen.kt`
  high-leverage trust surface: reliability, permissions, Health Connect, Hue,
  backup, and integration controls.
- `wear/src/main/java/...`
  Wear tile, next-alarm complication data source, and companion behavior.
  Maintain version parity with `:app`.

## Release And Verification Commands

Fast docs/change sanity:

```powershell
git diff --check
```

Core app verification:

```powershell
.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain
```

Release verification should use `.github/workflows/release.yml` for tag builds
after the 2026-05-17 R2 repair. The workflow requires signing secrets, builds
signed Play/F-Droid phone APKs plus the Wear release APK, verifies signatures,
and uploads APKs with `SHA256SUMS.txt`. For local release verification, use:

```powershell
.\gradlew.bat :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease --console=plain
apksigner verify --print-certs <release-apk>
aapt2 dump badging <release-apk>
```

## Active Risks Found On 2026-05-17

- Release workflow was repaired on 2026-05-17: tag builds now require signing
  secrets, produce signed Play/F-Droid/Wear release APKs, verify them, and
  upload SHA-256 hashes.
- Privacy/data-safety language was reconciled on 2026-05-17: `PRIVACY_POLICY.html`,
  `README.md`, Settings Health Connect copy, and `metadata/*.yml` now enumerate
  current optional network/data surfaces. After X1, the policy also describes
  the Play-only Health Connect `READ_SLEEP` path and the F-Droid no-SDK stance.
- Room schema discipline was hardened on 2026-05-17: current DB version is 11,
  v11 schema is committed, `AlarmDatabaseMigrationTest` covers migration and
  fresh-install/schema parity, and Android CI fails if `app/schemas` drifts.
- Dependency risk was mitigated on 2026-05-17: the Play flavor constrains the
  downloader graph to `jackson-* 2.18.6`, `commons-compress 1.28.0`,
  `commons-io 2.20.0`, `rhino 1.8.1`, plus Play-only `org.tukaani:xz:1.10`;
  `scripts/osv_gradle_audit.py` and Android CI now query OSV against the
  resolved Play release runtime classpath.
- Instruction drift decision was resolved on 2026-05-17: repo-local
  `AGENTS.md` and `CLAUDE.md` are ignored local tool files. Do not force-add
  them or treat them as durable project truth. Prefer this tracked file,
  `ROADMAP.md`, `README.md`, and `CHANGELOG.md` when local tool notes conflict.
- Backup/export trust was improved on 2026-05-17: backup format v8 round-trips
  the selected news feed URL, and Settings now warns before exporting readable
  or encrypted backups that include configured webhook URLs, Hue details,
  custom feed URLs, stream URLs, local media/photo URIs, Wi-Fi/location/contact
  details, or NFC/barcode challenge values.
- Wear glanceable surfaces expanded on 2026-05-17: the Wear module now exposes
  a modern AndroidX next-alarm complication data source alongside the existing
  tile, and Data Layer updates request both tile and complication refreshes.
- Direct Boot minimum support shipped on 2026-05-17: `BootReceiver` handles
  `LOCKED_BOOT_COMPLETED` through device-encrypted `DirectBootAlarmCache`, a
  direct-boot-aware fallback receiver/service, and post-unlock one-shot cleanup.
  The full Room/DataStore/challenge/custom-audio alarm flow remains
  credential-encrypted and post-unlock.
- Local support export shipped on 2026-05-17: Settings creates a local ZIP with
  crash logs, redacted alarm diagnostics, wake-readiness state, and version/device
  metadata. It is user-initiated sharing only, with no telemetry upload.
- Sleep/wake analytics shipped on 2026-05-17: Statistics renders local charts
  that correlate Health Connect sleep session duration with Room alarm history.
  DB v11 adds `alarm_events.challengeRetryCount`; Health Connect session windows
  remain foreground UI data and are not copied into Room/DataStore/backups.
- Release tagging drift: latest local tag is `v1.9.5`; app is `v1.13.7`.

## Active Roadmap

The active prioritized plan lives in `ROADMAP.md`. The full research packet for
the 2026-05-17 walk-away session is under `.ai/research/2026-05-17/`.

Start the next implementation pass with these top candidates unless newer
evidence changes the order:

1. Add on-device actigraphy buckets and smart-wake follow-up work.
2. Continue challenge/analytics refinements such as response histograms or
   persisted per-challenge solve quality if the roadmap promotes them.

## Research Artifacts

- `.ai/research/2026-05-17/STATE_OF_REPO.md`
- `.ai/research/2026-05-17/MEMORY_CONSOLIDATION.md`
- `.ai/research/2026-05-17/SOURCE_REGISTER.md`
- `.ai/research/2026-05-17/RESEARCH_LOG.md`
- `.ai/research/2026-05-17/COMPETITOR_MATRIX.md`
- `.ai/research/2026-05-17/FEATURE_BACKLOG.md`
- `.ai/research/2026-05-17/PRIORITIZATION_MATRIX.md`
- `.ai/research/2026-05-17/SECURITY_AND_DEPENDENCY_REVIEW.md`
- `.ai/research/2026-05-17/DATASET_MODEL_INTEGRATION_REVIEW.md`
- `.ai/research/2026-05-17/CHANGESET_SUMMARY.md`
