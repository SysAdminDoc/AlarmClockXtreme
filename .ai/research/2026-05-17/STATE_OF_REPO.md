# State Of Repo

Date: 2026-05-17

## Git Snapshot

- Repo: `C:\Users\--\repos\AlarmClockXtreme`
- Branch: `main`
- Status at start: clean, `## main...origin/main [ahead 13]`
- Head: `3e9214c feat: Health Connect opt-in scaffold + privacy doc (N12+N13) - v1.13.1`
- Latest local tag: `v1.9.5`
- Remote: `https://github.com/SysAdminDoc/AlarmClockXtreme.git`

Recent commits inspected with `rtk git log -10`:

- `3e9214c` Health Connect opt-in scaffold and privacy doc, v1.13.1
- `3e4b57a` adaptive primary navigation, v1.13.0
- `b086af8` CI version-line consistency lint, v1.12.3
- `daa46ea` chip-based ringtone pool editor, v1.12.2
- `16b5a33` missed-timer notification, v1.12.1
- `5b0bfca` per-alarm vibration start-delay, DB v10, backup v7
- `457cd15` pause alarms for N days, v1.11.6
- `5445100` Philips Hue API v2 with v1 fallback, v1.11.5
- `ed7d750` wake-lock budget compliance audit, v1.11.4
- `39397f3` app standby bucket awareness, v1.11.3

## Repository Shape

- 210 tracked files.
- 146 tracked Kotlin files.
- 34,738 tracked Kotlin lines counted by `Get-Content`.
- Primary modules: `app`, `wear`.
- Primary docs: `README.md`, `CHANGELOG.md`, `ROADMAP.md`, `AGENTS.md`,
  ignored-but-present `CLAUDE.md`, `PRIVACY_POLICY.html`.

Tests present:

- Unit tests under `app/src/test/java`.
- One Play-flavor test under `app/src/testPlay/java`.
- No tracked `app/src/androidTest` tests found during this pass.
- No wear-specific tests found during this pass.

## Build And Dependency Snapshot

Local files:

- `app/build.gradle.kts`: `compileSdk = 36`, `minSdk = 26`,
  `targetSdk = 35`, `versionCode = 66`, `versionName = "1.13.1"`.
- `wear/build.gradle.kts`: same version and SDK targets.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle 8.13.
- Top-level `build.gradle.kts`: AGP 8.11.1, Kotlin 2.1.0, KSP 2.1.0-1.0.29.

Important runtime dependencies:

- Compose BOM 2026.05.00.
- Room 2.6.1.
- Hilt 2.53.1.
- WorkManager 2.9.1.
- DataStore 1.1.1.
- Retrofit 2.11.0, Moshi 1.15.1, OkHttp 4.12.0.
- Glance 1.1.1.
- Wear Tiles 1.6.0, protolayout 1.4.0.
- Play flavor only: `io.github.junkfood02.youtubedl-android:library:0.18.1`
  and `com.github.teamnewpipe:NewPipeExtractor:v0.24.8`.

Dependency commands run:

```powershell
.\gradlew.bat :app:dependencies --configuration playReleaseRuntimeClasspath --console=plain
.\gradlew.bat :app:dependencies --configuration fdroidReleaseRuntimeClasspath --console=plain
```

Both completed. The Play runtime tree includes the downloader and its legacy
transitives. The F-Droid runtime tree excludes the YouTube downloader path.

## Runtime And Policy Surfaces

Manifest evidence from `app/src/main/AndroidManifest.xml`:

- Uses `android.permission.USE_EXACT_ALARM`.
- Uses `android.permission.USE_FULL_SCREEN_INTENT`.
- Uses foreground-service permissions for media playback, data sync, and
  microphone.
- Receives `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- Boot receiver now has a v1.13.5 Direct Boot minimum-alarm path:
  `LOCKED_BOOT_COMPLETED` schedules a device-encrypted fallback snapshot
  through `DirectBootAlarmCache`, `DirectBootAlarmReceiver`, and
  `DirectBootAlarmService`. The full Room/DataStore/custom-audio/challenge
  flow remains credential-encrypted and post-unlock.
- Health Connect permission is not declared yet, which matches the v1.13.1
  scaffold-only state.

Database evidence:

- `AlarmDatabase.kt` declares `@Database(... version = 10, exportSchema = true)`.
- `DatabaseModule.kt` registers `MIGRATION_9_10`.
- `app/schemas/...` lacked the v10 schema export during reconnaissance. The
  verification build generated `app/schemas/com.sysadmindoc.alarmclock.data.local.AlarmDatabase/10.json`,
  and this changeset includes it.

Health Connect evidence:

- `PreferencesManager.kt` adds `AppSettings.healthConnectEnabled`.
- `BackupManager.kt` round-trips the opt-in.
- `SettingsScreen.kt` exposes flavor-aware Health Connect copy.
- No `androidx.health.connect:connect-client` dependency or permission path is
  wired yet.

## Local Drift Findings

1. `README.md` says "Room DB v8" and "19 challenge views"; live code and
   `ROADMAP.md` show DB v10 and 22 user-facing challenge types.
2. `CLAUDE.md` top matter says DB v10, but older path notes still say Room DB
   v6 and 19 challenge types. `CLAUDE.md` is ignored by `.gitignore`.
3. `.github/workflows/release.yml` runs `./gradlew assembleDebug`; this conflicts
   with `ROADMAP.md` language claiming the release workflow builds both flavor
   release artifacts.
4. F-Droid metadata files show `CurrentVersion` values far behind v1.13.1.
5. Room v10 schema export was missing at the start of the run; generated and
   included by this changeset. Future work remains for migration/schema gates.
6. `PRIVACY_POLICY.html` Health Connect section is current, but broad data-flow
   text predates the current integration set.
7. The latest local tag is v1.9.5 while the app is v1.13.1.
