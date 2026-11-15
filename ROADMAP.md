# AlarmClockXtreme Roadmap

Actionable work only. Historical and completed roadmap material is archived in CHANGELOG.md; blocked work is kept in Roadmap_Blocked.md.

## Research-Driven Additions

Added 2026-09-04 from the research pass recorded in RESEARCH.md.

Cross-reference, not a new item: the Android 17 sleep-audio check in `Roadmap_Blocked.md` ("Confirm non-alarm (sleep) audio") gains a concrete target: `service/SleepSoundPlayer.kt` drives an `AudioTrack` with `USAGE_MEDIA` from a plain coroutine scope with no foreground service behind it, so it is the path Android 17 background-audio hardening will silence first.

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

- [ ] P3: App closures: say when the app was closed mid-ring
  Why: a process death while an alarm is ringing is the worst closure there is, but the card reports "No alarm was set" for it, because the alarm's armed time is already before the exit and it isn't re-armed until it's dismissed.
  Evidence: `data/repository/ProcessExitRepository.kt` `alarmOutcome` keeps only arms with `scheduledAt > exitAt`. AlarmService's START_COMMAND row before the exit with no dismiss, snooze or auto-silence row between it and the exit marks the ringing case.
  Touches: `ProcessExitRepository.kt` (new outcome), `SettingsReadinessSections.kt`, strings en and ja, `ProcessExitReportsTest.kt`.
  Acceptance: an exit between a service start and that fire's dismiss, snooze or auto-silence reads "The %s alarm was ringing", and a unit test covers a ring that ended before the exit.
  Also: `directboot/DirectBootAlarmService` writes no incident rows, so a ring before first unlock reads "no ring recorded"; and an arm the app moved itself (weather or commute lead) reads "changed" rather than owed. Log a start row from the direct-boot service, and treat an app-made re-arm for the same occurrence as the same arm.
  Complexity: S

- [ ] P3: Give generic words their own keys instead of borrowing another feature's
  Why: a translator reads the key's prefix as its context, and fourteen generic words are read by features the prefix doesn't name. Japanese got away with it, but a language that inflects "Paused" or "Ready" by what it describes won't.
  Evidence: a scan of `R.string` reads under `ui/<feature>/` on 2026-11-15 found `settings_paused` (alarm list, stopwatch, timer), `settings_ready` (alarm list, onboarding, stopwatch), `settings_selected` and `settings_not_selected` (alarm list, components, radio, ringtone), `settings_close` (alarm list, dashboard, world clock), `dashboard_retry` (alarm firing, news, radio), `dashboard_use` (radio, ringtone), `dashboard_optional` (bedtime, onboarding), `alarmlist_min` (bedtime, stats), `stopwatch_reset` and `timer_restart` (bedtime), `settings_active`, `settings_never` and `settings_tonight` (bedtime). Keys read by a second folder for the same feature (radio picker in alarm edit, health keys in stats, permission rows in onboarding) are fine.
  Touches: `values/strings.xml`, `values-ja/strings.xml`, the call sites above.
  Acceptance: each of those words is either a `common_` key for a true verb or label used the same way everywhere, or a per-feature key; the same scan finds no feature-prefixed key read from an unrelated feature.
  Complexity: S

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

- [ ] P3: Clear the swipe lock on resume only after two reads
  Why: `AlarmFiringActivity.onResume` clears a lock that needs no PIN on one read of `isKeyguardLocked` and `isDeviceLocked`. SystemUI sets the first a moment before the second, so a quick double press of the power key (the keyguard shows at wake) could read a PIN lock as one that needs none and put the PIN pad over the ringing screen. The NFC poll in `challenges/ChallengeViews.kt` already waits for two reads in a row.
  Touches: `ui/alarmfiring/AlarmFiringActivity.kt` (`onResume`), `LockScreenDismissTest`.
  Acceptance: on resume, a lock is cleared only when a second read about 250 ms later still sees no PIN and the screen is still resumed; a test where `isDeviceLocked` turns true between the reads sees no dismiss.
  Complexity: S

## Toolchain

Moved from Roadmap_Blocked.md on 2026-11-12. Nothing outside the repo blocks these. The Android 17 follow-ons below wait on the compileSdk 37 item.

- [ ] P2: Move to compileSdk 37
  Why: The Android 17 follow-ons below need SDK 37 APIs, and androidx.hilt 1.4.0 and lifecycle 2.11.0 fail `checkAarMetadata` below compileSdk 37, so the repo holds androidx.hilt at 1.3.0. AGP 9.3.3 is in, and the `android-37.0` and `android-37.1` platforms are installed.
  Touches: `compileSdk` in `app/build.gradle.kts` and `wear/build.gradle.kts`, androidx.hilt 1.3.0 to 1.4.0, lifecycle to 2.11.0, both lockfiles, `gradle/verification-metadata.xml`.
  Acceptance: both modules compile against 37 with targetSdk left at 36, all three unit suites green, both release builds shrink under R8, `verifyDependencyIntegrity` passes.
  Complexity: S

- [ ] P2: WorkManager 2.11 or newer
  Why: WorkManager 2.11 broke the test mocks on the old toolchain. Kotlin 2.4 and KSP2 are in now, so what's left is the mocks. 2.12.0 is current, so check its minCompileSdk against the compileSdk 37 item first.
  Complexity: M

- [ ] P3: AGP 9.5 and KSP 2.3.14
  Why: The repo is on AGP 9.3.3 and KSP 2.3.12. AGP 9.5.1 (needs Gradle 9.6.0 or newer) is current, but Kotlin 2.4.21's compatibility table only lists AGP up to 9.3.1. KSP 2.3.13 has a concurrent-execution regression (google/ksp#3286), and 2.3.14 came out on 2026-11-13.
  Acceptance: move once a Kotlin release lists AGP 9.5 and KSP 2.3.14 is at least two weeks old, with all three unit suites green and both release builds shrinking under R8.
  Complexity: S

- [ ] P3: Room 3 SQLiteDriver migration runway
  Complexity: M

### Android 17 follow-ons (after compileSdk 37)

Moved from Roadmap_Blocked.md on 2026-11-14. Its "needs an API 37 emulator" blocker no longer holds (android-37.0 and android-37.1 images are installed). What they really wait on is the compileSdk 37 item above.

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
  Needs: the compileSdk 37 item.
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

Added from the 2026-11-14 research pass in RESEARCH.md. Reported problems come first: the first item answers Discussion #52, which has been waiting since 2026-08-30.

Cross-references, not new items:

- Media3 (P3 "Bump Media3 to 1.11.0" above): aim for 1.11.1 now.
- The two P2 hardcoded-English items above ("Move the remaining hardcoded English into string resources" and "Move the remaining hardcoded English in view models and dialogs into strings.xml") cover the same sweep. Do them as one.
- "Confirm non-alarm (sleep) audio survives Android 17 background-audio rules" says `SleepSoundPlayer` runs under a `mediaPlayback` foreground service. It doesn't: `service/SleepSoundPlayer.kt` sets `USAGE_MEDIA`, never calls `startForeground`, and is only started from `ui/bedtime/BedtimeViewModel.kt`. The note at the top of the 2026-09-04 section has it right. The test switch is `adb shell cmd audio set-enable-hardening enable` on an API 37 emulator. `enable` also drops the waiver for alarm streams, so alarm audio failing under it is expected and not a bug (https://developer.android.com/about/versions/17/changes/bg-audio).
- Audio focus (P2 "Request and monitor audio focus" above) should land before the targetSdk 37 bump, not after it. On an API 37 emulator (2026-11-06, AOSP `android-17.0.0_r1`), alarm-stream sound from a `MediaPlayer` in a service started in the background was muted even though the documentation says the exact alarm permission waives the rule. The waiver reads a cached permission that's refreshed by focus and volume calls, and a `MediaPlayer` plays from mediaserver, so the cache never refreshes on its own. Requesting audio focus with alarm attributes before playing fixed it. This app plays alarm audio through `MediaPlayer` at `service/AlarmService.kt:1515`, `:1606` and `:1691`, `directboot/DirectBootAlarmService.kt:140` and `ui/timer/TimerAlarmService.kt:232`, and never requests focus. Add to that item's acceptance: with a targetSdk 37 build on an API 37 emulator, an alarm fired while the app hasn't been opened since boot logs no `mutedState:opControlAudio` line (Needs live validation for this app's own paths).
- Per-alarm pause (P3 above): FossifyOrg/Clock #13 "Disable an alarm once" has 17 thumbs up and has been open since 2024-01-12. That's more support than any other open request on the rival trackers checked.
- Bundled fallback tone (P3 above): McFarlane et al. 2020 (PLOS ONE, a self-report survey of 50 people) linked melodic alarm sounds with less morning grogginess. That's weak evidence, but it's a fair tiebreaker for picking a melodic tone over a beep.

### P2

- [ ] P2: Make Photo Match compare layout as well as color
  Why: `PhotoMatcher` scales both pictures to 64x64 and compares per-channel color histograms, and the ringing screen accepts a score of 0.65 or more. A histogram knows nothing about layout, so any photo with a similar mix of colors can pass (a beige wall for a beige kitchen), while the right spot in different light can fail. A difference hash (dHash) on a grayscale thumbnail captures layout, and combining the two makes the challenge mean "be at that spot".
  Evidence: `util/PhotoMatcher.kt:12`, `:18`, `:74` (`buildHistogram`), `:96` (`histogramIntersection`); `ui/alarmfiring/AlarmFiringViewModel.kt:1235-1237` (the 0.65 cut-off); no `PhotoMatcher` test exists under `app/src/test`.
  Touches: `util/PhotoMatcher.kt`, `ui/alarmfiring/AlarmFiringViewModel.kt:1235-1245`, a new `PhotoMatcherTest` with fixture images.
  Acceptance: fixture tests show a match for one scene under two light levels and a small shift, and no match for a different scene with a similar color mix. The threshold comes from a handful of real morning photos and is recorded in the test (Needs live validation). The score still reaches the UI as 0 to 100 so the "no match" message keeps working.
  Complexity: M

- [ ] P2: Dependency refresh
  Why: A few libraries are behind: OkHttp 5.4.0 (5.5.1 is out), NewPipeExtractor v0.26.3 (v0.26.4 fixed YouTube playlist continuations, and v0.26.5 is the last 0.26), Robolectric 4.14.1 (4.18, released 2026-11-13), DataStore 1.1.1 (1.3.0) and zxing-cpp 3.0.2 (3.1.1, which needed the Kotlin 2.3.20 stdlib the AGP 9 build now resolves).
  Evidence: versions pinned in `app/build.gradle.kts`; https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml; https://github.com/TeamNewPipe/NewPipeExtractor/releases; https://github.com/robolectric/robolectric/releases; https://developer.android.com/jetpack/androidx/releases/datastore
  Touches: `app/build.gradle.kts`, both lockfiles, `gradle/verification-metadata.xml`.
  Acceptance: each bump is its own commit with both unit suites green. A library that fails `checkAarMetadata` below compileSdk 37 moves into the compileSdk 37 item instead of being forced. NewPipe stays on 0.26.x here, since 0.27.0 raised the Java target to 17 and turned `Description` into a record, so 0.27.x needs its own API pass. A live YouTube search and an internet radio stream still play.
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

- [ ] P3: The next-alarm queries accept a time that has already passed
  Why: `getNextAlarm()` and `observeNextAlarm()` take the enabled alarm with the smallest `nextTriggerTime > 0`, with no check against now. A stored time stays behind us while an alarm rings, while an inexact fallback delivery runs late, and after a failed re-arm whose delivery row is gone (Clear history, or the 100-row incident trim during a ring). While it does, that alarm wins over the real next one. The upcoming-alarm notification for the real next alarm never shows, the Skip tile offers to skip the stale one, widget nudges do nothing, and the Bedtime Do Not Disturb rule is built around a past time. It lasts until that alarm is rescheduled.
  Evidence: `data/local/AlarmDao.kt:28-32`; `data/repository/AlarmRepository.kt:15` and `:19`; `ui/alarmlist/AlarmListViewModel.kt:167`; found while verifying the failed re-arm fix in `domain/AlarmScheduler.kt` (`liveArmTrigger`).
  Touches: `AlarmDao` (a `now` argument or a Kotlin-side filter, keeping in mind a Flow query with a bound `now` doesn't move on by itself), the callers of both queries, and the ringing case, where the alarm on screen may be meant to stay "next".
  Acceptance: with one alarm's stored time in the past and another's ahead, every next-alarm surface (upcoming notification, Skip tile, widget, Bedtime rule, list header) picks the one ahead. A ringing alarm's own screens are unchanged. A unit test covers the query choice.
  Complexity: S

- [ ] P3: Two schedule calls for one alarm can cross
  Why: `AlarmScheduler.schedule()` and `scheduleAt()` read the live arm's time, store the new one, then arm. Nothing serializes them per alarm. If an editor save and a `rescheduleAll` pass run together and the second arm fails, it can restore its stale time onto the arm the first one just set. That alarm then rings as the wrong occurrence, and the dismiss can skip one. Separately, after a failed re-arm that keeps the old arm, `cacheForDirectBoot` and `scheduleSupportingWork` still use the attempted time, so the kept ring loses its smart-wake window and Hue sunrise, and its fire watchdog moves to the attempted time.
  Evidence: `domain/AlarmScheduler.kt` `schedule()`, `scheduleAt()`, `scheduleAlarmClock()` and `scheduleSupportingWork()`; both found while verifying the failed re-arm fix. HEAD had the same race with the attempted time in place of the kept one.
  Touches: `domain/AlarmScheduler.kt` (a per-alarm `Mutex` around the read, store and arm, plus a decision on whether supporting work follows the kept time; the watchdog at the attempted time is what can still ring that occurrence when the arm keeps failing), `AlarmSchedulerTest`.
  Acceptance: two overlapping schedule calls for one alarm leave the PendingIntent's extras and the stored row matching the arm AlarmManager holds. A failed re-arm leaves the smart window and Hue sunrise on the kept time and a watchdog that still covers the attempted time. Unit tests cover both.
  Complexity: S

- [ ] P3: A dismiss for one alarm stops a different alarm that is ringing
  Why: `ACTION_DISMISS` takes the alarm id from the intent but only adopts the intent's time when nothing is ringing. If alarm Y's dismiss arrives while alarm X rings (Y's fire watchdog posted its full-screen fallback, then X started ringing, then the user dismissed Y from that screen or its notification), `dismissAlarm(Y)` stops X's sound and clears the service's state. X goes quiet with nothing on record for it. Y's dismiss row and `finishOccurrence(Y, ...)` both use X's scheduled time, so the post-ring work runs for Y against the wrong occurrence and X's never runs.
  Evidence: `service/AlarmService.kt` `ACTION_DISMISS` branch (`currentAlarmId == -1L` guard) and `dismissAlarm()` (`recordIncident` defaults to `currentScheduledAt`, then `outcomes.finishOccurrence(testRun, alarmId, currentScheduledAt)`); found while verifying the ring-evidence rows for the failed re-arm fix.
  Touches: `service/AlarmService.kt` (a dismiss for an alarm that isn't the one ringing finishes that alarm's own occurrence from the intent's time and leaves the ringing one alone), an `AlarmService` Robolectric test.
  Acceptance: with X ringing, a dismiss for Y leaves X ringing, writes Y's dismiss row at Y's own scheduled time and finishes only Y's occurrence. A dismiss for X still ends X as before.
  Complexity: S

- [ ] P3: A one-shot the scheduler switches off keeps its old snooze count
  Why: when a one-shot's trigger is already past at `schedule()` time, or `rescheduleAll` finds its fired marker after a reboot, the scheduler switches it off through `disarm()`, which leaves `AlarmRuntimeState` alone. A ring that was snoozed and never dismissed (the process died mid-snooze) leaves its count behind, and switching the alarm back on from the list goes through `schedule()`, which doesn't clear it either. The next ring starts with its snoozes already used: a capped snooze limit runs out early and a progressive snooze starts short.
  Evidence: `domain/AlarmScheduler.kt` past-trigger branch in `schedule()` (`repository.setEnabled(..., enabled = false, ...)` after `disarm`) and the `consumeFiredOneShotMarker` branch in `rescheduleAllInBatches`; `disarm()` only cancels entries. `ui/alarmlist/AlarmListViewModel.kt` `toggleAlarm` calls `scheduler.schedule` with no runtime-state clear. Found while checking the calendar auto-alarm's snooze signal.
  Touches: `domain/AlarmScheduler.kt` (both switch-off paths clear the alarm's runtime state when no ring for it is in progress), `AlarmSchedulerTest`.
  Acceptance: a one-shot switched off by either path, then switched back on, rings with a snooze count of 0. A ring in progress for that alarm keeps its count.
  Follow-up: once the count is cleared on these paths, the calendar worker's three-hour cap on a taken snooze (`RING_WINDOW_MS` in `occurrenceUnderway`) can go. Today a smart-wake or weather-early ring snoozed more than three hours ahead loses its hold and the next pass moves the row.
  Complexity: S

- [ ] P3: Calendar auto-alarm misplaces a ring when the commute or weather lead moves it before the day's window
  Why: the worker decides a day is done from history rows placed by the time the ring was set for, with each day's window moved back by the base lead only. A commute-aware ring (up to 120 minutes extra) or a weather-early ring for a meeting just past midnight lands before that day's window. A skip of that ring doesn't hold, and after a dismiss the next pass can ring it again. Changing the lead between a ring and a pass does the same.
  Evidence: `worker/CalendarAutoAlarmWorker.kt` `done()` (window shifted by `minutesBefore` only), `SkipNextReceiver.kt` stamps the weather-shifted trigger, `resolveCommuteAdjustment` adds the route extra. Found while verifying the midnight fix.
  Touches: `worker/CalendarAutoAlarmWorker.kt` (each day's window starts at that day's own full lead: base, commute extra and the row's `weatherEarlyMinutes`), `CalendarAutoAlarmWorkerTest`. Resolving the commute for both days on every pass doubles paid Routes API calls, so the lead the ring was armed with probably needs storing (runtime state keyed by the alarm id) instead of being recomputed.
  Acceptance: a commute-aware alarm for a 00:30 meeting with a 45 minute route extra, skipped at 22:00, stays off through the next pass. A dismissed one doesn't ring again when a later pass sees a shorter route. The previous day's late ring still doesn't count for the next day.
  Complexity: M

- [ ] P3: A snoozed ring's history rows carry the snooze time instead of the occurrence's time
  Why: a snooze re-arm carries the snooze time as the scheduled time, and the service stores it as `currentScheduledAt`, so the SNOOZED, DISMISSED and MISSED rows of a snoozed ring are placed at the last snooze. The calendar auto-alarm reads those rows by time. A late meeting's ring (say 22:40 for a 23:40 meeting) snoozed past 23:00 marks the next day as done, and that day's 9:00 meeting gets no alarm. History and stats also show the snooze time as the occurrence.
  Evidence: `domain/AlarmScheduler.kt` snooze paths (`updateNextTrigger(..., snoozeTime)` and the PendingIntent's `EXTRA_SCHEDULED_AT`), `service/AlarmService.kt` (`currentScheduledAt = scheduledAt` on every fire). Found while verifying the calendar midnight fix.
  Touches: `service/AlarmRuntimeState.kt` (keep the occurrence's first scheduled time next to the snooze count), `service/AlarmService.kt` and `AlarmFireDismissContract` (stamp rows with it), tests for both. Check what the history screen and stats expect before changing the column's meaning.
  Acceptance: a ring snoozed three times and then dismissed writes all its rows at the first ring's scheduled time. The calendar worker's late-meeting case keeps the next day's alarm. The wake-up check and guardian still re-fire for the right occurrence.
  Complexity: M

- [ ] P3: Snoozing a re-ring of a calendar alarm drops the alarm's next time
  Why: after the calendar worker moves its row on to the next meeting, the wake-up check or a missed-alarm replay can still ring for the ring that already ended. Snoozing it goes through `scheduleSnoozeAt` and `scheduleAt`, which store the snooze time in the row and arm it under the row's own request code, so the next meeting's arm is replaced. The snooze then rings with the snooze time as its scheduled time, which isn't the ended occurrence, so ending it switches the row off. Usually the next 15-minute calendar pass arms the row again, so the next meeting is without an alarm until that pass runs, or longer if WorkManager defers it. When the snooze's history row lands in the next day's window, no pass re-arms it and the next meeting gets no alarm at all: a 22:50 ring for a 23:50 meeting, re-rung at 23:00 and snoozed to 23:10, writes a row at 23:10, which marks the next day as done (see the snoozed-ring history item above).
  Evidence: `domain/AlarmScheduler.kt` `scheduleSnoozeAt()`, `scheduleAt()` and `handleAlarmFired()`; the snooze path in `service/AlarmService.kt`; `worker/CalendarAutoAlarmWorker.kt` re-arms a switched-off row on its next pass unless the day reads as done. Found while verifying the calendar midnight fix.
  Touches: `AlarmScheduler` (a snooze of a re-ring keeps the row's newer time, or puts it back when the snooze ends), the `AlarmService` snooze path, tests for both. Fixing the snoozed-ring history item closes the no-alarm case on its own.
  Acceptance: with the calendar row moved on to tomorrow 8:30, snoozing a re-ring of today's ring (from the wake-up check or a missed-alarm replay) and then dismissing the snooze leaves the row on and armed for tomorrow 8:30 with no calendar pass in between.
  Complexity: S

- [ ] P3: Chain-only NFC, barcode, photo and Wi-Fi steps have no field for their saved value
  Why: the NFC tag, barcode value, reference photo and Wi-Fi name fields only show when that challenge is the alarm's main type. Adding one of them only as a chain step shows a warning that its value is missing, but there's nowhere to set it, short of switching the main type over and back. Barcode's new scan button sits in the same gated block.
  Evidence: `ui/alarmedit/AlarmEditDismissSections.kt` (each field is gated on `state.challengeType == ...`, and the chain's `missingRefs` hint names the missing values). Found while verifying the barcode scanner (#51).
  Touches: `ui/alarmedit/AlarmEditDismissSections.kt` (show each reference field when the main type or any chain step needs it), editor tests if any cover the section.
  Acceptance: an alarm with Math as the main type and a Barcode scan chain step shows the barcode field and its scan button. The missing-value warning goes away once the code is scanned. The same holds for NFC, photo and Wi-Fi steps.
  Complexity: S

- [ ] P2: The math problem's lines draw on top of each other when it wraps
  Why: on a narrow screen (or with a larger font size) a longer expression like "47 + (8 x 4) = ?" wraps to two lines, and the second line ("= ?") is drawn over the first, so the problem is hard to read while the alarm is ringing. The barcode camera fallback hands out exactly this kind of problem.
  Evidence: `ui/alarmfiring/challenges/ChallengeViews.kt` MathChallengeView sets `fontSize = 36.sp` on the expression with no `lineHeight`, so it keeps the smaller line height of the inherited body text style. Seen on a 720x1600 320 dpi emulator during the barcode check (#51). Other large single-size texts in the same file (30.sp, 48.sp) may share it once they wrap.
  Touches: `ui/alarmfiring/challenges/ChallengeViews.kt` (give the large texts a matching `lineHeight`, or a typography style that has one).
  Acceptance: on a 720 px wide screen at 320 dpi, and at the largest system font size, a wrapped math expression shows each line clear of the other. No large challenge text overlaps itself when it wraps.
  Complexity: S

- [ ] P2: A finished timer on a locked phone opens the app behind the lock screen
  Why: the timer's full-screen intent opens `MainActivity`, which has no `showWhenLocked`. On a PIN-locked phone the screen wakes and the lock screen shows the "Timer finished" notification (its Stop action works), but there's no timer screen over the lock like the alarm's ringing screen.
  Evidence: `ui/timer/TimerAlarmService.kt:146-153` (`fullScreen` targets `MainActivity`). Headless API 36 emulator, PIN set, screen asleep: "FSI allowed: device is not interactive" for id 6501, `MainActivity` started with `BAL_ALLOW_NON_APP_VISIBLE_WINDOW`, focus stayed on `NotificationShade` with `isKeyguardShowing=true`. Found while checking the ringing-screen fix.
  Touches: `ui/timer/TimerAlarmService.kt` (point the full-screen intent at a small show-when-locked finished-timer screen with Stop and Restart), the manifest, en and ja strings.
  Acceptance: on an API 34+ emulator with a PIN and the screen asleep, a finished timer opens a screen over the lock screen with Stop and Restart, and Stop works without unlocking.
  Complexity: S

- [ ] P2: The full-screen fallback and the wake-up check prompt can still be grouped silent on Android 16
  Why: Android 16 (first release) auto-groups an app's notifications once it has several and marks each grouped one silent, and SystemUI never launches a silent notification's full-screen intent. The ringing notifications avoid it as colorized foreground-service posts, but `AlarmFullScreenFallback` (used when the service can't start) and the wake-up check prompt post with plain `notify()`, so colour can't exempt them.
  Evidence: `service/AlarmFullScreenFallback.kt:92`, `worker/WakeConfirmWorker.kt:308-332`. Raised by the review of the ringing-screen fix. Not yet seen failing on a device.
  Touches: those two posts. Find what keeps a plain post out of the auto-group on Android 16 (an explicit group of its own, or a short-lived foreground service), and check it on an API 36 emulator with several of the app's notifications showing.
  Acceptance: with at least four of the app's notifications showing and the screen asleep on a PIN-locked API 36 emulator, each of the two posts gets an "FSI allowed" decision in SystemUI's NotifInterruptLog and opens its screen.
  Complexity: M

- [ ] P3: Editor row labels are cut off at the largest font size
  Why: at font scale 2.0 the "Challenge type" row in Snooze & dismiss shows only part of its label, because `SettingsRow` gives the label two lines and the value button takes most of the width.
  Evidence: `ui/alarmedit/AlarmEditSupport.kt:483-509` (`maxLines = 2`, `TextOverflow.Ellipsis`, trailing button at full size). Seen on a 720x1600 320 dpi emulator while checking the Photo Match buttons (#52).
  Touches: `ui/alarmedit/AlarmEditSupport.kt` (`SettingsRow`: drop the line cap, or put the value under the label when the label can't fit).
  Acceptance: at font scale 2.0 on a 720 px wide screen, every `SettingsRow` label in the editor reads in full.
  Complexity: S

- [ ] P2: The alarm editor forgets unsaved edits and a new photo when Android closes the app behind the camera
  Why: the editor's draft lives only in the view model. If Android closes the app while the camera or the photo picker is open (common on phones short of memory), the editor comes back with the alarm as saved and every unsaved change gone. The photo result is worse: it arrives before the alarm has loaded, so the reference is saved under a new file name, the screen says "saved", and the load then puts the old photo (or none) back. A cancel in that window says the alarm has no photo even when it has one. The background image picker and the location picker share the same gap.
  Evidence: `ui/alarmedit/AlarmEditViewModel.kt:198-303` (`init` replaces the whole state after an async Room load), `ui/alarmedit/AlarmEditScreen.kt:298-353` (`photoReferenceKey` falls back to the current time while `isEditing` is still false, and the result callbacks read `state` before the load). Raised by the review of the Photo Match gallery pick (#52). Not yet seen on a device.
  Touches: `ui/alarmedit/AlarmEditViewModel.kt` (keep the draft in the `SavedStateHandle`, or hold results that arrive before the load and apply them after it), `ui/alarmedit/AlarmEditScreen.kt` (the result callbacks).
  Acceptance: on an emulator, edit an alarm's time, open the camera from Photo Match, kill the app process with `am kill`, take the photo and return. The editor shows the edited time and the new photo, and saving keeps both. A cancel in the same flow leaves the existing photo and says nothing about a missing one. A Robolectric view model test covers a result that arrives before the load finishes.
  Complexity: M

- [ ] P2: A Spotify alarm never plays Spotify when it rings on a locked or sleeping phone
  Why: the service opens Spotify at the start of the ring, before the ringing screen is up. Android refuses that launch without an error, so Spotify never starts and the backup tone takes over 8 seconds later. A Spotify alarm only gets to Spotify when the phone is already in use, which is the least common way an alarm rings.
  Evidence: `service/AlarmService.kt:1084` (`startActivity(spotifyIntent)` from `startAudioInternal`). Headless API 36 emulator, swipe lock, screen asleep, a real 6:00 ring: "Background activity launch blocked!" for the `spotify:` VIEW at 06:00:00.228, the full-screen intent opened the ringing screen at 06:00:00.433, and the incident log has `SPOTIFY_DELEGATED` and then `SPOTIFY_FALLBACK` 8 seconds later. Spotify was signed out there, so playback itself wasn't tested. Found while closing the Spotify fallback screen check.
  Touches: `service/AlarmService.kt` (hold the Spotify launch until `FiringScreenPresence` reports the ringing screen up, with a short cap, so the launch counts as coming from a visible app), the watchdog's start time. On a PIN-locked phone Spotify's own screen opens under the lock screen, so check it starts playing from there or keep the ringing screen on top.
  Acceptance: on an API 36 emulator with a swipe lock and the screen asleep, a real Spotify alarm logs a START for `com.spotify.music` and no "Background activity launch blocked!" line for it. The same with a PIN either starts Spotify or rings the backup tone within 8 seconds with the ringing screen on top (Needs live validation with a signed-in Spotify).
  Complexity: M

- [ ] P3: The incident log says the ringing screen opened when Android blocked it
  Why: `FIRING_ACTIVITY_LAUNCHED` is recorded as succeeded straight after `startActivity`, which doesn't throw when Android refuses a background launch. With another app on screen the launch is always refused and the heads-up notification is what rings, yet the log says the screen opened. `SPOTIFY_DELEGATED` has the same problem. Anyone reading a reliability report gets the wrong story.
  Evidence: `service/AlarmService.kt:584` and `:1086-1091`. On a headless API 36 emulator, real rings at 6:00 on two days logged `FIRING_ACTIVITY_LAUNCHED` as succeeded while logcat had "Background activity launch blocked!" for the same start. `FIRING_ACTIVITY_CREATED` (`ui/alarmfiring/AlarmFiringActivity.kt:168`) and `SPOTIFY_PLAYBACK_CONFIRMED` are the real proof.
  Touches: `service/AlarmService.kt` (record those two as requested, not succeeded), anything that counts them in diagnostics or the report export.
  Acceptance: a ring on an emulator with another app on screen logs the launch as requested, and the diagnostics don't count it as a screen that opened. A unit test covers the status.
  Complexity: S

- [ ] P2: Calls never mute a ringing alarm on Android 12 and later
  Why: the call-state listener that mutes the alarm during a phone call needs `READ_PHONE_STATE` on Android 12 and later, and the app doesn't hold it. Registration throws, the catch logs a warning, and the alarm keeps playing over the call. The KDoc on `registerCallObserver` says no permission is needed, which was only true before Android 12.
  Evidence: `service/AlarmService.kt:2606-2638`. Headless API 36 emulator, Test now: "Call-state observer registration failed; alarm will not auto-mute on calls" with `SecurityException: listen` from `TelephonyPermissions.checkReadPhoneState`. Found while checking #65.
  Touches: `service/AlarmService.kt`. Check the docs for a route that needs no permission (`AudioManager.addOnModeChangedListener`, API 31, reports `MODE_RINGTONE` and `MODE_IN_CALL`) before adding a permission and a prompt.
  Acceptance: on an API 31+ emulator, a ringing alarm goes quiet when a call comes in (`adb emu gsm call`) and comes back when it ends, and logcat has no registration warning. A unit test covers the mode-to-mute mapping.
  Complexity: S

- [ ] P2: A ring that another alarm takes over while it's still starting keeps starting
  Why: `startAlarm` runs on the IO dispatcher and never checks that its fire is still the current one. When a second alarm's start command preempts it during the alarm row read (two alarms set for the same minute arrive back to back), the first one carries on after the second has stopped playback and taken over `currentFireId`. It then posts its notification under the second fire's id, launches its ringing screen with that id, and starts its own audio on top. Traced in review, not reproduced yet.
  Evidence: `service/AlarmService.kt`, `onStartCommand` ACTION_START (`serviceScope.launch { startAlarm(alarmId, testRun, fireId) }`) and `startAlarm` from the `repository.getById` read on. The first-launch check and the Spotify re-post already return once `currentFireId` isn't the ring's own `fireId`, the older steps don't. A real ring of the same alarm taking over from a test hits this too. Two smaller spots from the same review: `armSpotifyPlaybackWatchdog` still checks `currentAlarmId` and hands on `currentFireId`, so a test ring's late watchdog cancels the real ring's and runs its check, and the Spotify re-post sets `spotifyForcedFullScreen` after its fire-id check, so a takeover landing between the two leaves the flag set for the newer ring.
  Touches: `service/AlarmService.kt` (`startAlarm` already has its `fireId`; return before each side effect once `currentFireId` has moved on, build the ringing screen's intent from `fireId` rather than `currentFireId`, pass `fireId` into `armSpotifyPlaybackWatchdog` and gate it the same way, and re-check the fire id after setting `spotifyForcedFullScreen`).
  Acceptance: on an API 36 emulator, two alarms set for the same minute leave one notification, one ringing screen and one audio stream, all for the alarm that started last, and the incident log shows the first one as preempted. A unit test covers the stale-fire check.
  Complexity: S
