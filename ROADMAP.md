# AlarmClockXtreme Roadmap

Actionable work only. Historical and completed roadmap material is archived in CHANGELOG.md; blocked work is kept in Roadmap_Blocked.md.

## Research-Driven Additions

Added 2026-09-04 from the research pass recorded in RESEARCH.md.

Cross-references, not new items: the AGP 8 to 9 migration (Toolchain, below) is still the keystone for Room 2.8, WorkManager 2.11 and Hilt 2.59, and this pass adds one more reason to do it (CVE-2026-53914 affects `kotlin-gradle-plugin` before 2.4.20-Beta1; the repo is on 2.1.0, build-time only, no runtime exposure). The Android 17 sleep-audio check in `Roadmap_Blocked.md` ("Confirm non-alarm (sleep) audio") gains a concrete target: `service/SleepSoundPlayer.kt` drives an `AudioTrack` with `USAGE_MEDIA` from a plain coroutine scope with no foreground service behind it, so it is the path Android 17 background-audio hardening will silence first.

### P1

- [ ] P1: In-app update check against the GitHub Releases API
  Why: Distribution is GitHub Releases and sideload only, and the app never tells a user a newer build exists. That is how a reporter ended up filing #53 on 2026-08-30 against a version whose bug was fixed on main on 2026-08-22.
  Evidence: grep for `api.github.com`, `releases/latest`, `checkForUpdate` across `app/src/main` returns nothing; issue #53 filed against 1.15.32.
  Touches: new `data/remote/ReleaseApi.kt` (Retrofit, existing shared OkHttp client from `di/NetworkModule.kt`), a settings toggle in `data/preferences/PreferencesManager.kt` plus its `SettingsBackup` counterpart, `ui/settings/`, a low-priority notification.
  Acceptance: the check is opt-in and off by default, runs no more than daily, compares `versionCode` against the latest release tag, links to the release page rather than downloading anything, and degrades silently offline; a unit test covers newer, same and malformed tag responses; the backup drift-guard test passes with the new settings field.
  Complexity: M

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

- [ ] P2: Move the remaining hardcoded English into string resources
  Why: With the app set to Japanese, about 90 lines still show in English because they are Kotlin literals in shapes `verifyLocalizedPrimaryScreens` does not catch: `if`/`else` arms assigned to a text val, data-class fields, function arguments, ViewModel state defaults and string templates. The tab bar, quotes, onboarding highlights and ringing-screen messages were moved on 2026-11-12; the rest were not.
  Evidence: a prose-literal sweep of `app/src/main/java` on 2026-11-12. Biggest clusters: `domain/ChronotypeEstimator.kt` (questions and answers), `domain/PreSleepTagAnalytics.kt` (tag names), `ui/bedtime/` (BedtimeScreen, BedtimeViewModel defaults, BedtimeSleepTrackingSections, BedtimeChronotypeSection), `ui/dashboard/` (weather and calendar status), `ui/alarmlist/` (bulk-delete dialog, feedback toasts), `ui/permissions/PermissionHelper.kt`, `ui/ringtone/RingtonePickerSheet.kt`, `ui/share/` (import screen), `ui/stats/StatsScreen.kt`, `ui/worldclock/WorldClockScreen.kt`, `ui/components/YouTubeDownloadDialog.kt`, `util/AlarmPublicText.kt` ("Alarm", "Next alarm", "Are you awake?" in notifications), `util/ManufacturerCompat.kt` (battery steps), `MainActivity.kt` (What's new lines), `data/backup/BackupManager.kt` (excluded-field labels). Typing-challenge phrases in `ChallengeGenerator.kt` stay English on purpose until there is a way to type Japanese answers without IME conversion getting in the way.
  Touches: those files, `values/strings.xml` and `values-ja/strings.xml` (both CRLF), and the guard in `app/build.gradle.kts` (teach it the `if`/`else` arm and data-class shapes so the gap can't reopen).
  Acceptance: the sweep finds no user-visible English outside the typing phrases, a Robolectric `qualifiers = "ja"` test per moved cluster passes, and a walk of every tab on a phone set to Japanese shows no English lines.
  Complexity: M

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

- [ ] P2: Move the remaining hardcoded English in view models and dialogs into strings.xml
  Why: With the app in Japanese, toasts, status lines and some screen-reader labels still come out in English. `verifyLocalizedPrimaryScreens` only scans composable text arguments, so strings built in view models, workers and `contentDescription` positional arguments slip past it.
  Evidence: about 90 hits for an English sentence literal under `ui/`, among them the `emitFeedback(...)` toasts in `ui/alarmlist/AlarmListViewModel.kt`, the Bedtime defaults and sonar status in `ui/bedtime/BedtimeViewModel.kt:89-108` and `ui/bedtime/BedtimeSleepTrackingSections.kt:191-251`, the weather and calendar errors in `ui/dashboard/DashboardViewModel.kt:293-524`, the YouTube dialog status lines in `ui/components/YouTubeDownloadDialog.kt:288-296` and `:879-883`, the wake check countdown in `ui/alarmfiring/WakeConfirmActivity.kt:276-290` and `worker/WakeConfirmWorker.kt:306`, the location-challenge status in `ui/alarmfiring/AlarmFiringViewModel.kt:880-882`, the sequence and grid challenge labels in `ui/alarmfiring/challenges/ChallengeViews.kt:278-280` and `:2564-2566`, and the bulk-delete dialog and search labels in `ui/alarmlist/AlarmListScreen.kt:363-482`.
  Touches: those files, `res/values/strings.xml`, `res/values-ja/strings.xml`; view models take an `@ApplicationContext` or return string resource IDs.
  Acceptance: none of those lines holds an English literal; the Japanese locale shows Japanese for each toast and status; `verifyLocalizedPrimaryScreens` also flags `emitFeedback("...")` and `contentDescription` literals so new ones can't land.
  Complexity: M

- [ ] P3: Stop the Bedtime screen from opening the microphone on every visit
  Why: Each time Bedtime comes to the foreground, `refreshNoiseBaselineStatus()` records 700 ms of room audio, so Android's green microphone dot flashes whenever someone just looks at their bedtime. That reads as the app listening without being asked.
  Evidence: `ui/bedtime/BedtimeViewModel.kt:600-611` calls `BedtimeNoiseBaselineSampler.sampleAndPersist(context)` from the ON_RESUME refresh.
  Touches: `ui/bedtime/BedtimeViewModel.kt`, `ui/bedtime/BedtimeScreen.kt` (noise baseline card).
  Acceptance: opening Bedtime records nothing; the baseline is taken when the user taps a "Measure room" action or when the bedtime reminder fires with the app in the foreground; the card says when the last sample was taken.
  Complexity: S

- [ ] P3: Restart a ringing alarm quickly after a background kill
  Why: AlarmService returns `START_REDELIVER_INTENT` while an alarm rings. With the ringing screen on top Android restarts it within a few seconds, but when the app is in the background the restart waits about twice as long as the service had been running (seen on the API 36 emulator, 2026-11-14). `START_STICKY` plus a persisted "currently ringing" alarm id read in `onStartCommand(null)` would bring it back without that backoff.
  Complexity: M

## Toolchain

Moved from Roadmap_Blocked.md on 2026-11-12. Nothing outside the repo blocks these; they are one coordinated upgrade, and the Android 17 follow-ons below (compileSdk 37) wait on the first one.

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

### Android 17 follow-ons (after AGP 9 and compileSdk 37)

Moved from Roadmap_Blocked.md on 2026-11-14. Its "needs an API 37 emulator" blocker no longer holds (android-37.0 and android-37.1 images are installed); what they really wait on is the AGP 9 item above.

- [ ] P3: AdvancedProtectionManager awareness (privacy hardening)
  Why: when the device is in Advanced Protection Mode, ACX should tighten up. Gate webhook egress and unknown-source ringtone imports, to match its privacy-first stance.
  Needs: compileSdk 37 to build against (`AdvancedProtectionManager` is an Android 17 API), then an API 37 emulator to verify. Implement as a reflective/no-op capability check when the compile SDK reaches 37.
  Where: `service/WebhookService.kt`, ringtone import path
  Complexity: S

- [ ] P1: Android 17 local-network runtime permission migration (smoke only)
  Status: v1.14.11 declared `ACCESS_LOCAL_NETWORK`, added Settings request/recovery UI, gates local webhook/Hue tests. Keep open for targetSdk 37 / API 37 device smoke.
  Complexity: S (verification only)

- [ ] P2: Android 17 native-loading smoke for Play YouTube engine
  Why: Android 17 hardens native dynamic loading. Needs API 37 smoke test before targetSdk bump.
  Complexity: S

- [ ] P2: Android 17 semantic notification colors for alarm urgency
  Why: `SEMANTIC_STYLE_SAFE/CAUTION/DANGER` notification spans need compileSdk 37.
  Evidence: https://developer.android.com/about/versions/17/features
  Needs: the AGP 9 and compileSdk 37 migration.
  Complexity: S

- [ ] P2: Android 17 Metric Style notification for timer countdown
  Why: `Notification.MetricStyle` needs compileSdk 37.
  Complexity: M

- [ ] P3: Confirm non-alarm (sleep) audio survives Android 17 background-audio rules
  Why: `SleepSoundPlayer` uses `USAGE_MEDIA` under a `mediaPlayback` FGS. Likely fine, but verify it before the targetSdk 37 bump.
  Complexity: S

## Emulator-run work

Moved from Roadmap_Blocked.md on 2026-11-14. Each was parked as "needs a device or emulator", and that doesn't hold: this machine has API 32, 36 and 37 images, tablet and foldable AVDs, and google_apis images with Play services and built-in Health Connect. Run them on a headless `-no-audio` emulator.

- [ ] Audit: Health Connect, YouTube engine update and NewPipe search against a live network
  Why: all three are code-traced. Each depends on a remote service answering, and two of them are play-flavor only. Jackson 2.18.11 and Guava 33.7.2-android (moved 2026-11-15, OSV clean, both releases shrink under R8) ride the YouTube and NewPipe paths, so this run is also that bump's live check.
  Needs: a google_apis emulator with Play services and network (Health Connect is built in from API 34).
  Complexity: M

- [ ] Audit: Tablet and foldable layouts (NavigationRail, the 840 dp list/detail split)
  Why: `ui/adaptive/AdaptiveLayoutPolicy.kt` was read and its threshold is unit tested, but no wide window has ever rendered the two-pane arrangement.
  Needs: a run on the tablet or foldable AVD.
  Complexity: S

- [ ] Audit: Android 12 / 12L behaviour
  Why: the exact-alarm permission work is reasoned from code and platform semantics, not observed. API 31 and 32 changed exactly that area.
  Needs: a run on an API 32 AVD (one is installed).
  Complexity: S

- [ ] P3: Room schema exports for versions 1, 2, 3 and 7
  Why: `app/schemas/` holds 4-6 and 8-25, so `AlarmDatabaseMigrationTest` starts at 4 and the three oldest upgrade paths plus 7 have never been validated.
  Needs two things, not one. The obvious one is that `runMigrationsAndValidate` is an instrumented test and needs a device or emulator. The one that actually stops the work is that a Room schema JSON carries an `identityHash` the annotation processor derives from the entity definition, and `runMigrationsAndValidate` compares against it. Hand-writing those four files from the migration SQL cannot produce a correct hash, and a file with a wrong one fails in a way that looks like a broken migration. The only sound route is to check out the commit where the database was at each of those versions and let KSP emit the export, which means building a 2026-03-era source tree with the current toolchain.
  Note: the item's original acceptance said "to 23"; the database is at 25 now.
  Acceptance: `runMigrationsAndValidate` from 1 and from 7 to the current version passes.
  Complexity: M

- [ ] P1: Alarm fire reliability smoke matrix
  Why: No automated on-device proof that exact alarm reaches receiver, FGS, FSI/notification fallback. Needs emulator.
  Complexity: L

- [ ] P2: Critical edge-case regression test expansion (instrumented paths)
  Why: Permission-denied alarm flows, reboot/timezone/date reschedule behavior, Guardian flavor behavior and timer/process-death recovery all need an emulator.
  Complexity: M

- [ ] P2: Baseline Profile for app startup
  Why: No Baseline Profile exists. Needs `connectedBenchmarkAndroidTest` on device.
  Complexity: M

- [ ] P2: Compose UI and accessibility regression smoke suite
  Why: No rendered UI test suite. Needs instrumentation + emulator.
  Complexity: M

- [ ] P3: Glance 1.2.0 widget picker preview (`providePreview()`)
  Why: Needs Glance 1.2.0 dep bump + device verification.
  Complexity: S

- [ ] P3: Geofence "don't fire here" alarm suppression (opt-in, risk-gated)
  Why: L effort, complex risk gating, needs device testing.
  Complexity: L

## Research-Driven Additions, 2026-11-14

Added from the 2026-11-14 research pass in RESEARCH.md. Reported problems come first: the first two items answer Discussions #51 and #52, which have been waiting since 2026-08-30.

Cross-references, not new items:

- Media3 (P3 "Bump Media3 to 1.11.0" above): aim for 1.11.1 now.
- AGP 8 to 9 (Toolchain): concrete targets as of 2026-11-14 are AGP 9.5.1 with Gradle 9.6.0, Kotlin 2.4.21, KSP 2.3.14, Dagger and Hilt 2.60.1, Room 2.8.5 and WorkManager 2.12.0. androidx.hilt 1.4.0 belongs in the same batch, since it needs KGP 2.2.0 or newer and AGP 9.2.0 or newer with Compose, and 1.3.0 moved `hiltViewModel()` into `androidx.hilt:hilt-lifecycle-viewmodel-compose`. AGP 9 also brings built-in Kotlin (https://developer.android.com/build/migrate-to-built-in-kotlin).
- The two P2 hardcoded-English items above ("Move the remaining hardcoded English into string resources" and "Move the remaining hardcoded English in view models and dialogs into strings.xml") cover the same sweep. Do them as one.
- "Confirm non-alarm (sleep) audio survives Android 17 background-audio rules" says `SleepSoundPlayer` runs under a `mediaPlayback` foreground service. It doesn't: `service/SleepSoundPlayer.kt` sets `USAGE_MEDIA`, never calls `startForeground`, and is only started from `ui/bedtime/BedtimeViewModel.kt`. The note at the top of the 2026-09-04 section has it right. The test switch is `adb shell cmd audio set-enable-hardening enable` on an API 37 emulator. `enable` also drops the waiver for alarm streams, so alarm audio failing under it is expected and not a bug (https://developer.android.com/about/versions/17/changes/bg-audio).
- Audio focus (P2 "Request and monitor audio focus" above) should land before the targetSdk 37 bump, not after it. On an API 37 emulator (2026-11-06, AOSP `android-17.0.0_r1`), alarm-stream sound from a `MediaPlayer` in a service started in the background was muted even though the documentation says the exact alarm permission waives the rule. The waiver reads a cached permission that's refreshed by focus and volume calls, and a `MediaPlayer` plays from mediaserver, so the cache never refreshes on its own. Requesting audio focus with alarm attributes before playing fixed it. This app plays alarm audio through `MediaPlayer` at `service/AlarmService.kt:1515`, `:1606` and `:1691`, `directboot/DirectBootAlarmService.kt:140` and `ui/timer/TimerAlarmService.kt:232`, and never requests focus. Add to that item's acceptance: with a targetSdk 37 build on an API 37 emulator, an alarm fired while the app hasn't been opened since boot logs no `mutedState:opControlAudio` line (Needs live validation for this app's own paths).
- Per-alarm pause (P3 above): FossifyOrg/Clock #13 "Disable an alarm once" has 17 thumbs up and has been open since 2024-01-12. That's more support than any other open request on the rival trackers checked.
- Bundled fallback tone (P3 above): McFarlane et al. 2020 (PLOS ONE, a self-report survey of 50 people) linked melodic alarm sounds with less morning grogginess. That's weak evidence, but it's a fair tiebreaker for picking a melodic tone over a beep.

### P1

- [ ] P1: Scan barcodes and QR codes with the camera (Discussion #51)
  Why: The barcode challenge asks you to type the code. The editor stores a typed `barcodeValue` and the ringing screen shows a text field under a scanning animation, so anyone who remembers the code can pass it from bed. No barcode library is on either classpath, yet the README's permission table lists barcode among the camera uses. QRAlarm, WakeNudge, Alarmy and Sleep as Android all scan with the camera. ML Kit and Google's code scanner are out because they need Play services or proprietary code, and the fdroid APK is the most downloaded build (750 downloads of v1.15.35 against 487 for the Play APK).
  Evidence: `ui/alarmedit/AlarmEditDismissSections.kt:399-415`; `ui/alarmfiring/challenges/ChallengeViews.kt:1066-1130` (`BarcodeScanChallengeView`); `ui/alarmfiring/AlarmFiringViewModel.kt:788-799` (`onBarcodeDetected`); `README.md:154`; no `camera` or `zxing` match in `app/build.gradle.kts`; https://github.com/SysAdminDoc/AlarmClockXtreme/discussions/51; https://repo1.maven.org/maven2/io/github/zxing-cpp/android/maven-metadata.xml (3.1.1); https://developer.android.com/jetpack/androidx/releases/camera (1.6.2).
  Touches: `app/build.gradle.kts` (`androidx.camera:camera-camera2`, `camera-lifecycle` and `camera-view` 1.6.2 plus `io.github.zxing-cpp:android` 3.1.1 as plain `implementation`, so both flavors get them), both lockfiles, `gradle/verification-metadata.xml`; a new `ui/alarmfiring/challenges/BarcodeScanner.kt` composable (preview plus analyzer) rather than more code in `ChallengeViews.kt`; a "Scan to register" button beside the typed field in `AlarmEditDismissSections.kt`; `BarcodeScanChallengeView`; the camera permission flow `AlarmFiringActivity` already runs for Photo Match; strings in en and ja.
  Acceptance: scanning in the editor stores the decoded text in `barcodeValue`, and typing stays available there for codes that won't scan. At the ringing screen the alarm moves on only when the camera reads that exact value, with no text entry for the code (the accessibility bypass remains the way out). Existing alarms with a typed value keep working because the comparison is the same text. Frames stay in memory and are never written to disk. A denied camera permission shows a clear message and points at the bypass. `verifyFdroidReleaseSize` still passes. A unit test covers the match rule (trimmed, exact), and a headless emulator run proves the decode path with a test image fed to the analyzer.
  Complexity: M

- [ ] P1: Let Photo Match take its reference from the gallery (Discussion #52)
  Why: The reference photo can only come from `TakePicturePreview()`, so you have to be standing at the spot when you set the alarm up. The Photo Picker needs no storage permission (activity 1.7.0 or newer, and the app is on 1.13.0), and the editor already has an `OpenDocument` launcher for the ringing-screen background to copy from.
  Evidence: `ui/alarmedit/AlarmEditScreen.kt:290-292` (camera only) and `:330-340` (background image launcher); `util/PhotoMatcher.kt:52` (`saveReference` copies a bitmap into app storage); https://github.com/SysAdminDoc/AlarmClockXtreme/discussions/52; https://developer.android.com/training/data-storage/shared/photopicker
  Touches: `ui/alarmedit/AlarmEditScreen.kt` (a `PickVisualMedia` launcher with `ImageOnly`), `util/PhotoMatcher.kt` (decode the picked URI with downsampling and EXIF orientation, then `saveReference`), the Photo Match editor buttons and their en and ja strings.
  Acceptance: a picked image is copied through `saveReference`, so the alarm keeps working after the original is deleted. A very large image decodes without running out of memory. A phone photo saved with an EXIF rotation matches the same scene shot upright at dismiss time. The manifest gains no storage permission. Unit tests cover the decode and save path.
  Complexity: S

- [ ] P1: Confirm the ringing screen really came back after the Spotify fallback
  Why: When Spotify never plays, `bringFiringScreenBack` relaunches the ringing screen over it. Android doesn't throw when it blocks a background launch, it only logs "Background activity launch blocked!", so the `catch` never runs and nothing records that the user has a fallback tone and no Dismiss button. The launch works today only because it lands inside the grace period of about 10 seconds after the screen was last visible (the comment at `service/AlarmService.kt:111-115`), and Android 17 is extending background launch hardening to `IntentSender`.
  Evidence: `service/AlarmService.kt:1342` and `:1353-1371`; `ui/alarmfiring/AlarmFiringActivity.kt:345` (`onResume` reports nothing back to the service); https://developer.android.com/guide/components/activities/background-starts; https://developer.android.com/about/versions/17/behavior-changes-17
  Touches: `service/AlarmService.kt` (`bringFiringScreenBack` plus a short follow-up check), `ui/alarmfiring/AlarmFiringActivity.kt` (`onResume` and `onStop` publish whether the ringing screen is showing, for example through a small `StateFlow` next to `ActiveAlarmSnapshot`), the incident reason codes.
  Acceptance: about 2 seconds after the relaunch the service checks that the ringing screen is resumed. If it isn't, it re-posts the ringing notification so it alerts again (the full-screen intent is built at `:774-837`) and records `SPOTIFY_FALLBACK_SCREEN_BLOCKED`. A Robolectric test covers both outcomes. A headless `-no-audio` emulator run with Spotify signed out shows the screen on top, and a run with the launch forced to fail shows the notification and the incident row (Needs live validation on how the heads-up looks over Spotify).
  Complexity: S

### P2

- [ ] P2: Make Photo Match compare layout as well as color
  Why: `PhotoMatcher` scales both pictures to 64x64 and compares per-channel color histograms, and the ringing screen accepts a score of 0.65 or more. A histogram knows nothing about layout, so any photo with a similar mix of colors can pass (a beige wall for a beige kitchen), while the right spot in different light can fail. A difference hash (dHash) on a grayscale thumbnail captures layout, and combining the two makes the challenge mean "be at that spot".
  Evidence: `util/PhotoMatcher.kt:12`, `:18`, `:74` (`buildHistogram`), `:96` (`histogramIntersection`); `ui/alarmfiring/AlarmFiringViewModel.kt:1235-1237` (the 0.65 cut-off); no `PhotoMatcher` test exists under `app/src/test`.
  Touches: `util/PhotoMatcher.kt`, `ui/alarmfiring/AlarmFiringViewModel.kt:1235-1245`, a new `PhotoMatcherTest` with fixture images.
  Acceptance: fixture tests show a match for one scene under two light levels and a small shift, and no match for a different scene with a similar color mix. The threshold comes from a handful of real morning photos and is recorded in the test (Needs live validation). The score still reaches the UI as 0 to 100 so the "no match" message keeps working.
  Complexity: M

- [ ] P2: Dependency refresh that doesn't wait for AGP 9
  Why: A few libraries are behind and don't depend on the Kotlin or KSP upgrade: OkHttp 5.4.0 (5.5.1 is out), NewPipeExtractor v0.26.3 (v0.26.4 fixed YouTube playlist continuations, and v0.26.5 is the last 0.26), Robolectric 4.14.1 (4.18, released 2026-11-13) and DataStore 1.1.1 (1.3.0).
  Evidence: versions pinned in `app/build.gradle.kts`; https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml; https://github.com/TeamNewPipe/NewPipeExtractor/releases; https://github.com/robolectric/robolectric/releases; https://developer.android.com/jetpack/androidx/releases/datastore
  Touches: `app/build.gradle.kts`, both lockfiles, `gradle/verification-metadata.xml`.
  Acceptance: each bump is its own commit with both unit suites green. A library built with a Kotlin version the 2.1.0 compiler can't read (the error names the metadata version) moves into the AGP 9 item instead of being forced. NewPipe stays on 0.26.x here, since 0.27.0 raised the Java target to 17 and turned `Description` into a record, so 0.27.x needs its own API pass. A live YouTube search and an internet radio stream still play.
  Complexity: S

### P3

- [ ] P3: Make the smart alarm notification say what it actually does
  Why: While the smart window is open, the ongoing notification says "Monitoring sleep quality…". The feature scores phone motion, the editor hint says so, and the README FAQ says Smart wake isn't a sleep-stage measurement. In Fino et al. 2020, three of four phone sleep app modes weren't reliable at telling sleep from wake, so the copy shouldn't promise more.
  Evidence: `res/values/strings.xml:97` and `:612`; `res/values-ja/strings.xml:68`; `service/SmartAlarmService.kt:108`; `README.md:194-196`.
  Touches: `notif_smart_alarm_text` in en and ja.
  Acceptance: the notification says something like "Watching for movement before your alarm", with a matching Japanese line. Nothing else shown for the smart window mentions sleep quality.
  Complexity: XS

- [ ] P3: Undo of a delete loses the calendar-skip mark
  Why: Deleting an alarm runs `AlarmScheduler.cancel`, which clears `CalendarSkipMarks` and `AlarmRuntimeState`. Undo re-arms an alarm whose stored trigger is still ahead through `scheduleAt`, which never puts the mark back. An alarm a calendar tag had moved comes back without its "Skipped" badge, and deleting the tagged event no longer restores the original ring.
  Evidence: `domain/AlarmScheduler.kt` `cancel()` (clears the mark) and `scheduleAt()`; `ui/alarmlist/AlarmListViewModel.kt` `undoDelete()`; `calendarSkipTagOf()` in the same file needs the mark's origin to show the badge.
  Touches: `ui/alarmlist/AlarmListViewModel.kt` (capture each deleted alarm's mark origin before `cancel`, restore it in `undoDelete`), or `AlarmScheduler` if the restore belongs there.
  Acceptance: WHEN an alarm whose next ring a calendar tag moved is deleted and then restored with Undo, the system SHALL show the same calendar-skip badge as before the delete, and removing the tagged event SHALL bring back the original ring. A unit test covers the round trip.
  Complexity: XS
