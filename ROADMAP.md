# AlarmClockXtreme Roadmap

Actionable work only. Historical and completed roadmap material is archived in CHANGELOG.md; blocked work is kept in Roadmap_Blocked.md.

## Research-Driven Additions

Added 2026-09-04 from the research pass recorded in RESEARCH.md.

Cross-references, not new items: the AGP 8 to 9 migration (Toolchain, below) is still the keystone for Room 2.8, WorkManager 2.11 and Hilt 2.59, and this pass adds one more reason to do it (CVE-2026-53914 affects `kotlin-gradle-plugin` before 2.4.20-Beta1; the repo is on 2.1.0, build-time only, no runtime exposure). The Android 17 sleep-audio check in `Roadmap_Blocked.md` ("Confirm non-alarm (sleep) audio") gains a concrete target: `service/SleepSoundPlayer.kt` drives an `AudioTrack` with `USAGE_MEDIA` from a plain coroutine scope with no foreground service behind it, so it is the path Android 17 background-audio hardening will silence first.

### P1

- [ ] P1: Capture `ApplicationExitInfo` and surface a user-facing reliability report
  Why: The app records a detailed incident stream but has no access to the OS's own reason for killing the process, which is exactly the "unknown reason" failure the whole category is complaining about. Android 17 adds a memory limiter that kills apps over a RAM threshold regardless of targetSdk, discoverable only through `ApplicationExitInfo.getDescription()` containing `MemoryLimiter:AnonSwap`.
  Evidence: zero matches for `ApplicationExitInfo` across `app/src` and `wear/src`; existing incident infrastructure in `data/repository/AlarmIncidentRepository.kt` and `data/support/SupportDiagnosticsFormatter.kt`; Android 17 behaviour changes for all apps; the recurring Pixel "Missed alarm: Alarm did not fire due to an unknown reason" reports.
  Touches: new `data/repository/ProcessExitRepository.kt` reading `ActivityManager.getHistoricalProcessExitReasons()`, `data/local/entity/AlarmIncidentEvent.kt` (correlate an exit against the surrounding fire window), `data/support/SupportDiagnosticsFormatter.kt`, a new reliability screen under `ui/settings/`.
  Acceptance: after a forced stop or a low-memory kill, the reliability screen names the exit reason, its timestamp, and whether an alarm was scheduled across it, in plain language; the support export carries the same rows; nothing new is stored beyond reason code, timestamp and description.
  Complexity: L

- [ ] P1: In-app update check against the GitHub Releases API
  Why: Distribution is GitHub Releases and sideload only, and the app never tells a user a newer build exists. That is how a reporter ended up filing #53 on 2026-08-30 against a version whose bug was fixed on main on 2026-08-22.
  Evidence: grep for `api.github.com`, `releases/latest`, `checkForUpdate` across `app/src/main` returns nothing; issue #53 filed against 1.15.32.
  Touches: new `data/remote/ReleaseApi.kt` (Retrofit, existing shared OkHttp client from `di/NetworkModule.kt`), a settings toggle in `data/preferences/PreferencesManager.kt` plus its `SettingsBackup` counterpart, `ui/settings/`, a low-priority notification.
  Acceptance: the check is opt-in and off by default, runs no more than daily, compares `versionCode` against the latest release tag, links to the release page rather than downloading anything, and degrades silently offline; a unit test covers newer, same and malformed tag responses; the backup drift-guard test passes with the new settings field.
  Complexity: M

- [ ] P1: Undo for bulk alarm delete
  Why: Multi-select delete is the last irreversible data action in the app, and the confirmation dialog admits it. Single-alarm delete already has undo, so the pattern and the snackbar host exist.
  Evidence: `ui/alarmlist/AlarmListScreen.kt:356` ("This bulk action does not offer per-alarm undo"); working single-delete undo at `ui/alarmlist/AlarmListViewModel.kt:115` and `undoDelete()`; FossifyOrg/Clock #491 shows the same demand elsewhere.
  Touches: `ui/alarmlist/AlarmListViewModel.kt`, `ui/alarmlist/AlarmListScreen.kt`.
  Acceptance: deleting a multi-select set offers one Undo that restores every deleted alarm with its original id and re-arms each one through `AlarmScheduler`; a unit test asserts the restored set matches the deleted set and that a second Undo is a no-op.
  Complexity: S

- [ ] P1: Add a pseudolocale gate and stop sharing the timer's Undo key
  Why: Japanese landed (#55), so a second layout now has to fit translated text, but nothing checks for clipping or hardcoded English before a translator finds it. The alarm list and stopwatch undo actions still read `R.string.timer_undo`, so a translator sees one key for three features.
  Evidence: `app/build.gradle.kts` has no `pseudoLocalesEnabled`; `ui/alarmlist/AlarmListScreen.kt:274` and `ui/stopwatch/StopwatchScreen.kt:91` read `R.string.timer_undo`.
  Touches: `app/build.gradle.kts` (debug `pseudoLocalesEnabled`), new undo keys in `values/strings.xml` and `values-ja/strings.xml`, the two screens above.
  Acceptance: a debug build run under `en-XA` and `ar-XB` shows no clipped or untranslated primary screen; `verifyLocalizedPrimaryScreens` still passes; no string key is shared across two unrelated features.
  Complexity: M

### P2

- [ ] P2: Request and monitor audio focus, and check the alarm stream is audible
  Why: The service never requests audio focus, never learns it lost focus, and never asks whether `STREAM_ALARM` is at zero. It can therefore report an alarm as playing while producing no sound, and it cannot pause a media app the user fell asleep to.
  Evidence: zero matches for `requestAudioFocus`, `AudioFocusRequest`, `OnAudioFocusChange`, `AUDIOFOCUS_` across `app/src/main/java`; `getStreamVolume` used only to save a restore value at `service/AlarmService.kt:583` and `:1226`; `isVolumeFixed` never called; Android 17 background-audio hardening makes focus requests fail with `AUDIOFOCUS_REQUEST_FAILED` outside a valid lifecycle.
  Touches: `service/AlarmService.kt` audio start and teardown, `service/AlarmAudioRouting.kt`.
  Acceptance: the service requests `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` with `USAGE_ALARM` and treats a denial as non-fatal; a stream volume of zero on an alarm that is not deliberately muted raises the stream before playing and records an incident; a test covers the denial path and the zero-volume path.
  Complexity: M

- [ ] P2: Give each notification type its own small icon
  Why: All 27 `setSmallIcon` call sites use `R.drawable.ic_alarm`, so a firing alarm, a running timer, a bedtime reminder, a wake-confirmation, a Hue sunrise and a health warning are indistinguishable in the status bar.
  Evidence: `grep -rho "setSmallIcon(R.drawable.[a-z_]*" app/src/main` returns 27 hits, all `ic_alarm`; `app/src/main/res/drawable/` contains only `ic_alarm.xml` and the two launcher assets; BlackyHawky/Clock #691 is the same open request against a competitor.
  Touches: new monochrome vector drawables under `app/src/main/res/drawable/`, `service/NextAlarmNotifier.kt`, `ui/timer/TimerNotifications.kt`, `ui/timer/TimerAlarmService.kt`, `receiver/BedtimeReceiver.kt`, `worker/WakeConfirmWorker.kt`, `worker/HueSunriseNotifications.kt`, `worker/AlarmHealthWorker.kt`, `service/AlarmService.kt`.
  Acceptance: each notification family renders a distinct status-bar glyph at 24dp in both light and dark system UI; every new drawable is a single-colour vector so Android's tinting applies; no notification loses its channel or id.
  Complexity: S

- [ ] P2: Move Guardian Angel automatic SMS out of the fdroid-only manifest
  Why: `SEND_SMS` lives only in the fdroid flavor and the comment says the play flavor degrades Guardian Angel to a prefilled composer to satisfy a Google Play restricted-permission policy. This project is never listed on any store, and the play flavor is the build the README tells users to install, so the recommended download ships a weaker emergency-escalation feature for a constraint that does not apply to it.
  Evidence: `app/src/fdroid/AndroidManifest.xml:1-9`; the README package table recommends the play APK; `worker/GuardianWorker.kt`.
  Touches: `app/src/main/AndroidManifest.xml` or `app/src/play/AndroidManifest.xml`, `worker/GuardianWorker.kt` flavor branch, `worker/GuardianEscalationPolicy.kt`, README permissions table.
  Acceptance: both flavors can auto-send the Guardian SMS after explicit in-app opt-in and a runtime grant; the README permission table lists `SEND_SMS` with its reason; the manifest-permission-to-README lint still passes.
  Complexity: S

- [ ] P2: Extract the alarm audio cluster out of AlarmService
  Why: `service/AlarmService.kt` is 2400 lines and every residual audio defect in this research pass lives in one 470-line region of it: source selection, two watchdogs, escalation and volume management are interleaved with service lifecycle, so none of it can be tested without Robolectric standing up a service.
  Evidence: `service/AlarmService.kt:841-1310`; the seam-extraction pattern already worked on `SettingsScreen.kt` (4129 to 1744) and on `AlarmHapticController` / `AlarmFlashlightController` / `AlarmPostDismissController`.
  Touches: new `service/AlarmAudioDirector.kt`, `service/AlarmService.kt:841-1310`, new tests under `app/src/test/.../service/`.
  Acceptance: source selection, watchdog arming and escalation decisions are pure functions or a class with an injected player interface, tested on the JVM without a service; `AlarmService` delegates to it; the existing audio tests still pass unchanged.
  Complexity: L

- [ ] P2: Unify `androidx.core:core-ktx` across the two modules
  Why: `:app` resolves 1.15.0 and `:wear` resolves 1.18.0 in two modules that ship under the same `applicationId`. Coroutines were deliberately unified at 1.11.0; core-ktx was missed.
  Evidence: `app/build.gradle.kts:392`, `wear/build.gradle.kts:69`.
  Touches: `app/build.gradle.kts`, `wear/build.gradle.kts`, both lockfiles, `gradle/verification-metadata.xml`.
  Acceptance: both modules declare the same version, both lockfiles regenerate cleanly, `verifyDependencyIntegrity` passes, and both unit suites are green.
  Complexity: S

### P3

- [ ] P3: Per-alarm "pause until a date"
  Why: `pauseUntilMillis` is a single global setting, so pausing one alarm for a fortnight means either disabling it and remembering to re-enable it, or pausing everything. Two competitor trackers carry the same request.
  Evidence: `data/preferences/PreferencesManager.kt:156` (global `pauseUntilMillis`), `domain/VacationAlarmPolicy.kt` (also global and recurring-only); sweakpl/qralarm-android #95, you-apps/ClockYou #405.
  Touches: new `Alarm.pausedUntilMillis`, Room migration, `domain/NextAlarmCalculator.kt`, `domain/AlarmScheduler.kt`, `ui/alarmedit/`, `ui/alarmlist/` card state, `data/backup/BackupManager.kt`.
  Acceptance: a paused alarm shows its resume date on the card, does not fire before it, arms itself automatically afterwards without user action, and survives a reboot; a unit test covers the boundary at the resume instant and the reboot reschedule.
  Complexity: M

- [ ] P3: Bundle a fallback alarm tone in the APK
  Why: If `RingtoneManager` returns null for both the ALARM and NOTIFICATION defaults, which happens on stripped AOSP builds and some managed profiles, the service records `NO_DEFAULT_TONE` and returns. The alarm rings nothing and nothing escalates. `res/raw/` is empty, so there is no in-APK tone to fall back to.
  Evidence: `service/AlarmService.kt:1432-1443` and `:965-978`; `ls app/src/main/res/raw/` is empty.
  Touches: one small looping tone in `app/src/main/res/raw/`, `service/AlarmService.kt:1432-1443`, LICENSE attribution if the asset is third-party.
  Acceptance: with both system defaults stubbed out, the alarm still produces audio from the bundled resource; the incident stream records that the bundled tone was used; the fdroid APK size gate (`verifyFdroidReleaseSize`) still passes.
  Complexity: S

- [ ] P3: Suppress touch on the firing screen while the proximity sensor is covered
  Why: A phone ringing in a pocket or under bedding can register a dismissal from a body press, which turns a safety-critical action into an accident. Cover-to-snooze already reads the proximity sensor, so the plumbing is there.
  Evidence: cover-to-snooze proximity handling in `ui/alarmfiring/AlarmFiringActivity.kt`; BlackyHawky/Clock #687 is the same open request.
  Touches: `ui/alarmfiring/AlarmFiringActivity.kt`, `ui/alarmfiring/AlarmFiringScreen.kt`, a settings toggle in `data/preferences/PreferencesManager.kt` and its `SettingsBackup` counterpart.
  Acceptance: while the proximity sensor reads covered, dismiss and snooze taps are ignored and the screen says why; uncovering restores input within one sensor callback; the setting is off by default and does not interfere with cover-to-snooze when that is enabled.
  Complexity: S

- [ ] P3: Bump Media3 to 1.11.0
  Why: 1.11.0 (2026-08-05) is the current stable and adds a 100ms grace period in the audio renderers that debounces transient underruns during active playout, plus a fixed 500ms default PCM buffer that makes behaviour less device-dependent. Both reduce spurious not-ready transitions on the alarm ring path.
  Evidence: `app/build.gradle.kts:443` pins 1.10.1; https://developer.android.com/jetpack/androidx/releases/media3
  Touches: `app/build.gradle.kts:443`, `app/gradle.lockfile`, `gradle/verification-metadata.xml`.
  Acceptance: both unit suites pass, the play and fdroid release builds shrink cleanly under R8, and a manual ring of a local tone, an internet radio stream and a Spotify alarm all behave as before.
  Complexity: S

## Toolchain

Moved from Roadmap_Blocked.md on 2026-11-12. Nothing outside the repo blocks these; they are one coordinated upgrade, and the Android 17 items still in Roadmap_Blocked.md (compileSdk 37) wait on the first one.

- [ ] P1: AGP 8 to 9 migration
  Why: AGP 8.11.1 is end of life and can't compile against SDK 37 (platforms `android-37.0` and `android-37.1` are installed). AGP 9 needs Hilt 2.56.2 to 2.59.2+, KSP1 to KSP2 (Room 2.8 needs it) and a Gradle wrapper check, all in one pass. Kotlin 2.1.0 also carries CVE-2026-53914 in `kotlin-gradle-plugin` (build time only).
  Touches: `build.gradle.kts` plugin versions, `app/build.gradle.kts`, `wear/build.gradle.kts`, both lockfiles, `gradle/verification-metadata.xml`.
  Acceptance: clean build, both unit suites green, play and fdroid release builds shrink under R8, `verifyDependencyIntegrity` passes.
  Complexity: M

- [ ] P2: Room 2.8 and WorkManager 2.11 (after AGP 9)
  Why: Room 2.8.4 fails KSP1 codegen and WorkManager 2.11 breaks the test mocks, so both wait on KSP2.
  Complexity: M

- [ ] P3: Room 3 SQLiteDriver migration runway (after Room 2.8)
  Complexity: M
