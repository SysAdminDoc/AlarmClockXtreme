# AlarmClockXtreme Roadmap

Actionable work only. Historical and completed roadmap material is archived in CHANGELOG.md; blocked work is kept in Roadmap_Blocked.md.

## Research-Driven Additions

Added 2026-09-04 from the research pass recorded in RESEARCH.md.

Cross-references, not new items: the AGP 8 to 9 migration in `Roadmap_Blocked.md:546` is still the keystone for Room 2.8, WorkManager 2.11 and Hilt 2.59, and this pass adds one more reason to unblock it (CVE-2026-53914 affects `kotlin-gradle-plugin` before 2.4.20-Beta1; the repo is on 2.1.0, build-time only, no runtime exposure). The Android 17 sleep-audio check at `Roadmap_Blocked.md:353` gains a concrete target: `service/SleepSoundPlayer.kt` drives an `AudioTrack` with `USAGE_MEDIA` from a plain coroutine scope with no foreground service behind it, so it is the path Android 17 background-audio hardening will silence first.

### P0

- [ ] P0 — Publish the v1.15.34 release with signed play and fdroid APKs
  Why: The last published release is v1.15.32 (2026-07-29). Two versions of fixes sit on `main` and have never reached a user, including the Spotify silent-alarm fix (`2c17a85`), the dismiss-gate hardening, the whole i18n extraction, and the fixes for #47, #48 and #49. README.md:20 already tells users to install `AlarmClockXtreme-v1.15.34-play-release.apk`, which does not exist.
  Evidence: `gh release list` shows v1.15.32 as Latest; `git log v1.15.32..HEAD` contains `2c17a85`, `4cc5580`, `12738b5`, `8c41583`; issue #53 (2026-08-30) reports against 1.15.32 a bug fixed on main 2026-08-22.
  Touches: local release build (`assemblePlayRelease`, `assembleFdroidRelease`), `scripts/verify_release_metadata.py`, `scripts/check-signing-hygiene.sh`, `gh release create v1.15.34`, GitHub issues #53 and #50.
  Acceptance: `gh release view v1.15.34` lists both release-signed APKs plus SHA256SUMS, the filename matches the README install line exactly, and #53 is answered with the fix commit and closed.
  Complexity: S

### P1

- [ ] P1 — Normalise pasted Spotify web links to `spotify:` URIs before delegating
  Why: A pasted `https://open.spotify.com/playlist/<id>?si=<token>` link is passed to `Uri.parse` verbatim and handed to Spotify with `START_PLAYBACK`. The `spotify:playlist:<id>` form is the one that reliably autoplays. This is the most likely reason #50 and #53 saw Spotify open and play nothing.
  Evidence: `service/AlarmService.kt:893-908`; no converter exists anywhere (`data/model/Alarm.kt:331` only trims and truncates); issues #50 and #53.
  Touches: new pure `domain/SpotifyUriNormalizer.kt`, `service/AlarmService.kt:893-908`, `ui/alarmedit/AlarmEditIntegrationSections.kt:88-103`, `data/model/Alarm.kt` `sanitized()`.
  Acceptance: a unit test converts `https://open.spotify.com/{track,album,playlist}/<id>?si=...`, `https://spotify.link/<id>`, and a bare `open.spotify.com/...` to the matching `spotify:` URI, leaves an already-canonical `spotify:` URI untouched, and rejects a non-Spotify host; the editor shows an inline error for a rejected value instead of accepting it silently.
  Complexity: S

- [ ] P1 — Fix the Spotify playback watchdog predicate and its baseline ordering
  Why: `mediaStreamsBefore()` counts only `AudioPlaybackConfiguration`s whose usage reads `USAGE_MEDIA`, but AOSP anonymises other apps' configurations for callers without `MODIFY_AUDIO_ROUTING`, so the check may never confirm and the fallback tone plays over working Spotify audio. Separately the baseline is sampled after `startActivity`, so a warm Spotify that starts inside the sampling window is counted into the baseline and reads as a failure.
  Evidence: `service/AlarmService.kt:1179-1186` (predicate), `:926` (baseline sampled after launch), `:1188-1212` (watchdog); `AudioPlaybackConfiguration.anonymizedCopy()` in AOSP `frameworks/base`.
  Touches: `service/AlarmService.kt:1179-1212`, new test in `app/src/test/.../service/`.
  Acceptance: the baseline is captured before `startActivity`; the predicate treats any new non-alarm playback configuration as started rather than requiring `USAGE_MEDIA`; a Robolectric test with a shadow `AudioManager` proves both a confirmed case and a fallback case; a device check with Spotify installed confirms `SPOTIFY_PLAYBACK_CONFIRMED` is actually reachable.
  Complexity: M

- [ ] P1 — Make backup-sound escalation audible when the service owns no player
  Why: The comment at `AlarmService.kt:575` says the job switches to the system alarm tone. The body only raises `STREAM_ALARM` (Spotify plays on `STREAM_MUSIC`), records an incident, and calls `applyPlaybackGain()`, which is `alarmPlayback?.setVolume(...)` and a no-op after the Spotify branch returned without a player. The escalation cannot rescue a delegated alarm.
  Evidence: `service/AlarmService.kt:575` versus `:580-592`; `applyPlaybackGain` at `:2385`; the Spotify branch returns at `:927`.
  Touches: `service/AlarmService.kt:571-594`.
  Acceptance: when the escalation job fires and `alarmPlayback` is null, it starts the default tone through `startMedia3DefaultFallback`; a unit test drives the job with a null player and asserts a tone is started; the comment matches the code.
  Complexity: S

- [ ] P1 — Default `backupSoundEnabled` to true for new alarms
  Why: The backup-sound safety net is off unless the user finds and enables it, so the escalation that exists to catch a silent ring never runs for a default alarm. Both Spotify reports describe having no backup alarm at all.
  Evidence: `data/model/Alarm.kt:70` (`backupSoundEnabled: Boolean = false`); issues #50 and #53 both say "no backup alarm sound".
  Touches: `data/model/Alarm.kt:70`, `data/backup/BackupManager.kt`, `ui/alarmedit/`.
  Acceptance: a newly created alarm has the escalation armed by default; alarms already in the database keep the value they were saved with, so no Room migration rewrites existing rows; restoring a backup written before this change preserves whatever the file recorded rather than being silently flipped; a test creates one alarm through the editor default and one through a v18 backup import and asserts the two differ as expected.
  Complexity: S

- [ ] P1 — Add a real "no snooze" option
  Why: Issue #49 asked to turn snooze off. It is impossible today, and the control that looks like "off" does the opposite: `maxSnoozeCount = 0` means unlimited. The minimum duration is 1 minute and the minimum cap is 1, so the Snooze button is always live on the first ring.
  Evidence: `domain/SnoozeCapPolicy.kt:41` (`isCapped = maxSnoozeCount > 0`), `data/model/Alarm.kt:321-322`, `ui/alarmedit/AlarmEditDismissSections.kt:104,133`, `ui/alarmfiring/AlarmFiringScreen.kt:1091`; issue #49 item 1.
  Touches: new `Alarm.snoozeEnabled` boolean (do not remap 0, which would flip every existing unlimited alarm to no-snooze on upgrade), Room migration, `domain/SnoozeCapPolicy.kt`, `ui/alarmedit/AlarmEditDismissSections.kt`, `ui/alarmfiring/AlarmFiringScreen.kt`, `service/AlarmService.kt` snooze handling, `data/backup/BackupManager.kt`.
  Acceptance: an alarm with snooze off hides the Snooze button on the firing screen, ignores flip-to-snooze, cover-to-snooze and the hardware button snooze mapping, and shows no snooze action in the notification; existing alarms with `maxSnoozeCount = 0` still get unlimited snoozes after migration, proven by a migration test.
  Complexity: M

- [ ] P1 — Constrain `org.jsoup:jsoup` to 1.23.1
  Why: An OSV batch query over both lockfiles on 2026-09-04 returned exactly one hit: jsoup 1.22.2 in `playReleaseRuntimeClasspath`, CVE-2026-71497 (GHSA-pmhh-3w7g-xqp8, moderate, published 2026-07-30, fixed in 1.23.1). It arrives transitively through NewPipeExtractor. Not exploitable here (no app code imports jsoup, no custom Safelist, the only WebView loads Windy's own URL), but the repo's convention is to pin every OSV hit in the transitive Play graph.
  Evidence: `app/gradle.lockfile:228`; the eight existing CVE-driven pins at `app/build.gradle.kts:468-497`; https://github.com/advisories/GHSA-pmhh-3w7g-xqp8
  Touches: `app/build.gradle.kts` constraints block, `gradle/verification-metadata.xml`, `app/gradle.lockfile`.
  Acceptance: `python scripts/osv_gradle_audit.py` reports zero findings against the play release runtime classpath, the lockfile records jsoup 1.23.1, and the release build passes `verifyDependencyIntegrity`.
  Complexity: S

- [ ] P1 — Capture `ApplicationExitInfo` and surface a user-facing reliability report
  Why: The app records a detailed incident stream but has no access to the OS's own reason for killing the process, which is exactly the "unknown reason" failure the whole category is complaining about. Android 17 adds a memory limiter that kills apps over a RAM threshold regardless of targetSdk, discoverable only through `ApplicationExitInfo.getDescription()` containing `MemoryLimiter:AnonSwap`.
  Evidence: zero matches for `ApplicationExitInfo` across `app/src` and `wear/src`; existing incident infrastructure in `data/repository/AlarmIncidentRepository.kt` and `data/support/SupportDiagnosticsFormatter.kt`; Android 17 behaviour changes for all apps; the recurring Pixel "Missed alarm: Alarm did not fire due to an unknown reason" reports.
  Touches: new `data/repository/ProcessExitRepository.kt` reading `ActivityManager.getHistoricalProcessExitReasons()`, `data/local/entity/AlarmIncidentEvent.kt` (correlate an exit against the surrounding fire window), `data/support/SupportDiagnosticsFormatter.kt`, a new reliability screen under `ui/settings/`.
  Acceptance: after a forced stop or a low-memory kill, the reliability screen names the exit reason, its timestamp, and whether an alarm was scheduled across it, in plain language; the support export carries the same rows; nothing new is stored beyond reason code, timestamp and description.
  Complexity: L

- [ ] P1 — In-app update check against the GitHub Releases API
  Why: Distribution is GitHub Releases and sideload only, and the app never tells a user a newer build exists. That is how a reporter ended up filing #53 on 2026-08-30 against a version whose bug was fixed on main on 2026-08-22.
  Evidence: grep for `api.github.com`, `releases/latest`, `checkForUpdate` across `app/src/main` returns nothing; issue #53 filed against 1.15.32.
  Touches: new `data/remote/ReleaseApi.kt` (Retrofit, existing shared OkHttp client from `di/NetworkModule.kt`), a settings toggle in `data/preferences/PreferencesManager.kt` plus its `SettingsBackup` counterpart, `ui/settings/`, a low-priority notification.
  Acceptance: the check is opt-in and off by default, runs no more than daily, compares `versionCode` against the latest release tag, links to the release page rather than downloading anything, and degrades silently offline; a unit test covers newer, same and malformed tag responses; the backup drift-guard test passes with the new settings field.
  Complexity: M

- [ ] P1 — Undo for bulk alarm delete
  Why: Multi-select delete is the last irreversible data action in the app, and the confirmation dialog admits it. Single-alarm delete already has undo, so the pattern and the snackbar host exist.
  Evidence: `ui/alarmlist/AlarmListScreen.kt:356` ("This bulk action does not offer per-alarm undo"); working single-delete undo at `ui/alarmlist/AlarmListViewModel.kt:115` and `undoDelete()`; FossifyOrg/Clock #491 shows the same demand elsewhere.
  Touches: `ui/alarmlist/AlarmListViewModel.kt`, `ui/alarmlist/AlarmListScreen.kt`.
  Acceptance: deleting a multi-select set offers one Undo that restores every deleted alarm with its original id and re-arms each one through `AlarmScheduler`; a unit test asserts the restored set matches the deleted set and that a second Undo is a no-op.
  Complexity: S

- [ ] P1 — Land the first translation locale and add a pseudolocale gate
  Why: 2136 strings and 43 plurals are extracted, the build guard covers the whole app package, and there are zero `values-*` locale directories, so none of that work reaches a user. A contributor offered German in #44 and the maintainer published the exact path; nothing landed.
  Evidence: `ls app/src/main/res/values*` returns only `values`; `grep -c "<string name=" app/src/main/res/values/strings.xml` = 2136; issue #44 and its closing comment.
  Touches: new `app/src/main/res/values-de/strings.xml`, `app/build.gradle.kts` (`resConfigs`, pseudolocale build type), `ui/alarmlist/AlarmListScreen.kt:274` (the alarm undo action reads `R.string.timer_undo`, so it needs its own key before a translator sees it).
  Acceptance: the app renders in German end to end with no missing-format-argument crashes; a build run against `en-XA`/`ar-XB` pseudolocales produces no clipped or untranslated primary screen; `verifyLocalizedPrimaryScreens` still passes; no string key is shared across two unrelated features.
  Complexity: L

### P2

- [ ] P2 — Request and monitor audio focus, and check the alarm stream is audible
  Why: The service never requests audio focus, never learns it lost focus, and never asks whether `STREAM_ALARM` is at zero. It can therefore report an alarm as playing while producing no sound, and it cannot pause a media app the user fell asleep to.
  Evidence: zero matches for `requestAudioFocus`, `AudioFocusRequest`, `OnAudioFocusChange`, `AUDIOFOCUS_` across `app/src/main/java`; `getStreamVolume` used only to save a restore value at `service/AlarmService.kt:583` and `:1226`; `isVolumeFixed` never called; Android 17 background-audio hardening makes focus requests fail with `AUDIOFOCUS_REQUEST_FAILED` outside a valid lifecycle.
  Touches: `service/AlarmService.kt` audio start and teardown, `service/AlarmAudioRouting.kt`.
  Acceptance: the service requests `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` with `USAGE_ALARM` and treats a denial as non-fatal; a stream volume of zero on an alarm that is not deliberately muted raises the stream before playing and records an incident; a test covers the denial path and the zero-volume path.
  Complexity: M

- [ ] P2 — Ship the "use only my phrases" toggle promised in #43
  Why: The maintainer publicly committed to it in the closing comment on #43. It was never built, so a user with three custom phrases still mostly draws from the built-in pool.
  Evidence: `ui/alarmfiring/challenges/ChallengeGenerator.kt:368-381` unions custom phrases onto `TYPING_PHRASES` with no exclusive branch; no `customPhrasesOnly` symbol exists in the repo; the closing comment on issue #43.
  Touches: new `AppSettings.customPhrasesOnly` in `data/preferences/PreferencesManager.kt` plus its `SettingsBackup` counterpart and a backup format bump, `ui/settings/SettingsPersonalizationSection.kt:371-383`, `ChallengeGenerator.kt:368-381`, `ui/alarmfiring/AlarmFiringViewModel.kt`.
  Acceptance: with the toggle on and at least one custom phrase saved, TYPING and VOICE_PHRASE draw only from the user's list; with it on and the list empty, the built-in pool is used rather than producing an unsolvable challenge; the drift-guard test passes.
  Complexity: S

- [ ] P2 — Give each notification type its own small icon
  Why: All 27 `setSmallIcon` call sites use `R.drawable.ic_alarm`, so a firing alarm, a running timer, a bedtime reminder, a wake-confirmation, a Hue sunrise and a health warning are indistinguishable in the status bar.
  Evidence: `grep -rho "setSmallIcon(R.drawable.[a-z_]*" app/src/main` returns 27 hits, all `ic_alarm`; `app/src/main/res/drawable/` contains only `ic_alarm.xml` and the two launcher assets; BlackyHawky/Clock #691 is the same open request against a competitor.
  Touches: new monochrome vector drawables under `app/src/main/res/drawable/`, `service/NextAlarmNotifier.kt`, `ui/timer/TimerNotifications.kt`, `ui/timer/TimerAlarmService.kt`, `receiver/BedtimeReceiver.kt`, `worker/WakeConfirmWorker.kt`, `worker/HueSunriseNotifications.kt`, `worker/AlarmHealthWorker.kt`, `service/AlarmService.kt`.
  Acceptance: each notification family renders a distinct status-bar glyph at 24dp in both light and dark system UI; every new drawable is a single-colour vector so Android's tinting applies; no notification loses its channel or id.
  Complexity: S

- [ ] P2 — Move Guardian Angel automatic SMS out of the fdroid-only manifest
  Why: `SEND_SMS` lives only in the fdroid flavor and the comment says the play flavor degrades Guardian Angel to a prefilled composer to satisfy a Google Play restricted-permission policy. This project is never listed on any store, and the play flavor is the build the README tells users to install, so the recommended download ships a weaker emergency-escalation feature for a constraint that does not apply to it.
  Evidence: `app/src/fdroid/AndroidManifest.xml:1-9`; `README.md:20,23` recommends the play APK; `worker/GuardianWorker.kt`.
  Touches: `app/src/main/AndroidManifest.xml` or `app/src/play/AndroidManifest.xml`, `worker/GuardianWorker.kt` flavor branch, `worker/GuardianEscalationPolicy.kt`, README permissions table.
  Acceptance: both flavors can auto-send the Guardian SMS after explicit in-app opt-in and a runtime grant; the README permission table lists `SEND_SMS` with its reason; the manifest-permission-to-README lint still passes.
  Complexity: S

- [ ] P2 — Retrofit README.md against the code it describes
  Why: README.md is the only git-tracked markdown and therefore the only thing a user reads, and three of its claims are wrong.
  Evidence: `README.md:20` names a v1.15.34 APK that was never published; the Sound and Vibration table describes "a build-flagged legacy MediaPlayer fallback", but `USE_MEDIA3_ALARM_PLAYER` and the backend mapper were deleted on 2026-08-22 while the fallback survived; `README.md:27` advertises F-Droid inclusion and store-declaration work that standing policy rules out.
  Touches: `README.md`.
  Acceptance: the download block matches the newest published release asset name; the Media3 row describes the live behaviour (Media3 first, `startMedia3DefaultFallback` drops into MediaPlayer on failure) with no reference to a flag; the roadmap paragraph describes work that is actually planned; no em dashes or spaced-hyphen dashes anywhere in the file.
  Complexity: S

- [ ] P2 — Cut the store-gated items out of Roadmap_Blocked.md
  Why: Three blocked items can never be worked because standing policy forbids store distribution entirely, so they sit in the blocked list forever and dilute it.
  Evidence: `Roadmap_Blocked.md:292` (F-Droid submission), `:298` (developer verification readiness), `:307` (reproducible-build badge).
  Touches: `Roadmap_Blocked.md`.
  Acceptance: the three items are deleted, and the file's "Requires External Credentials / Actions" section contains only work that could actually start if a credential appeared.
  Complexity: S

- [ ] P2 — Extract the alarm audio cluster out of AlarmService
  Why: `service/AlarmService.kt` is 2400 lines and every residual audio defect in this research pass lives in one 470-line region of it: source selection, two watchdogs, escalation and volume management are interleaved with service lifecycle, so none of it can be tested without Robolectric standing up a service.
  Evidence: `service/AlarmService.kt:841-1310`; the seam-extraction pattern already worked on `SettingsScreen.kt` (4129 to 1744) and on `AlarmHapticController` / `AlarmFlashlightController` / `AlarmPostDismissController`.
  Touches: new `service/AlarmAudioDirector.kt`, `service/AlarmService.kt:841-1310`, new tests under `app/src/test/.../service/`.
  Acceptance: source selection, watchdog arming and escalation decisions are pure functions or a class with an injected player interface, tested on the JVM without a service; `AlarmService` delegates to it; the existing audio tests still pass unchanged.
  Complexity: L

- [ ] P2 — Unify `androidx.core:core-ktx` across the two modules
  Why: `:app` resolves 1.15.0 and `:wear` resolves 1.18.0 in two modules that ship under the same `applicationId`. Coroutines were deliberately unified at 1.11.0; core-ktx was missed.
  Evidence: `app/build.gradle.kts:392`, `wear/build.gradle.kts:69`.
  Touches: `app/build.gradle.kts`, `wear/build.gradle.kts`, both lockfiles, `gradle/verification-metadata.xml`.
  Acceptance: both modules declare the same version, both lockfiles regenerate cleanly, `verifyDependencyIntegrity` passes, and both unit suites are green.
  Complexity: S

### P3

- [ ] P3 — Per-alarm "pause until a date"
  Why: `pauseUntilMillis` is a single global setting, so pausing one alarm for a fortnight means either disabling it and remembering to re-enable it, or pausing everything. Two competitor trackers carry the same request.
  Evidence: `data/preferences/PreferencesManager.kt:156` (global `pauseUntilMillis`), `domain/VacationAlarmPolicy.kt` (also global and recurring-only); sweakpl/qralarm-android #95, you-apps/ClockYou #405.
  Touches: new `Alarm.pausedUntilMillis`, Room migration, `domain/NextAlarmCalculator.kt`, `domain/AlarmScheduler.kt`, `ui/alarmedit/`, `ui/alarmlist/` card state, `data/backup/BackupManager.kt`.
  Acceptance: a paused alarm shows its resume date on the card, does not fire before it, arms itself automatically afterwards without user action, and survives a reboot; a unit test covers the boundary at the resume instant and the reboot reschedule.
  Complexity: M

- [ ] P3 — Bundle a fallback alarm tone in the APK
  Why: If `RingtoneManager` returns null for both the ALARM and NOTIFICATION defaults, which happens on stripped AOSP builds and some managed profiles, the service records `NO_DEFAULT_TONE` and returns. The alarm rings nothing and nothing escalates. `res/raw/` is empty, so there is no in-APK tone to fall back to.
  Evidence: `service/AlarmService.kt:1432-1443` and `:965-978`; `ls app/src/main/res/raw/` is empty.
  Touches: one small looping tone in `app/src/main/res/raw/`, `service/AlarmService.kt:1432-1443`, LICENSE attribution if the asset is third-party.
  Acceptance: with both system defaults stubbed out, the alarm still produces audio from the bundled resource; the incident stream records that the bundled tone was used; the fdroid APK size gate (`verifyFdroidReleaseSize`) still passes.
  Complexity: S

- [ ] P3 — Suppress touch on the firing screen while the proximity sensor is covered
  Why: A phone ringing in a pocket or under bedding can register a dismissal from a body press, which turns a safety-critical action into an accident. Cover-to-snooze already reads the proximity sensor, so the plumbing is there.
  Evidence: cover-to-snooze proximity handling in `ui/alarmfiring/AlarmFiringActivity.kt`; BlackyHawky/Clock #687 is the same open request.
  Touches: `ui/alarmfiring/AlarmFiringActivity.kt`, `ui/alarmfiring/AlarmFiringScreen.kt`, a settings toggle in `data/preferences/PreferencesManager.kt` and its `SettingsBackup` counterpart.
  Acceptance: while the proximity sensor reads covered, dismiss and snooze taps are ignored and the screen says why; uncovering restores input within one sensor callback; the setting is off by default and does not interfere with cover-to-snooze when that is enabled.
  Complexity: S

- [ ] P3 — Bump Media3 to 1.11.0
  Why: 1.11.0 (2026-08-05) is the current stable and adds a 100ms grace period in the audio renderers that debounces transient underruns during active playout, plus a fixed 500ms default PCM buffer that makes behaviour less device-dependent. Both reduce spurious not-ready transitions on the alarm ring path.
  Evidence: `app/build.gradle.kts:443` pins 1.10.1; https://developer.android.com/jetpack/androidx/releases/media3
  Touches: `app/build.gradle.kts:443`, `app/gradle.lockfile`, `gradle/verification-metadata.xml`.
  Acceptance: both unit suites pass, the play and fdroid release builds shrink cleanly under R8, and a manual ring of a local tone, an internet radio stream and a Spotify alarm all behave as before.
  Complexity: S
