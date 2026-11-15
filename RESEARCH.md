# Research: AlarmClockXtreme
Date: 2026-11-14. Replaces all prior research (previous pass: 2026-09-04).

## Executive Summary

AlarmClockXtreme is a local-first Android alarm, bedtime and timer app. `main` is at v1.16.0 (versionCode 138, Room schema 25, backup format 20) and that version hasn't shipped. The last release is v1.15.35 from 2026-09-05, and 70 commits have landed since. The app module is 68,127 lines of Kotlin in 266 files, with 825 `@Test` methods across 149 JVM test files, 3 instrumented test files and no TODO or FIXME markers.

The most important fact about the project right now isn't a missing feature. Every open issue on the tracker (#49, #53, #59, #60, #62, #63, #65) has a fix on `main`, and none of those fixes has reached a user yet. The F-Droid APK is the most downloaded v1.15.35 asset (750 downloads, against 487 for the Play APK and 118 for the AAB), so whatever ships next has to work without Google Play services.

Two user reports have neither a fix nor a reply. Discussion #51 (2026-08-30) points out that the barcode challenge asks you to type the code instead of scanning it. Discussion #52 (same day, same person) asks why Photo Match can't use a picture from the gallery. Both are right about the code. `BarcodeScanChallengeView` (`ui/alarmfiring/challenges/ChallengeViews.kt:1066`) is a text field with a scanning animation drawn above it, and the editor takes a typed value (`ui/alarmedit/AlarmEditDismissSections.kt:399-415`). No barcode library is on either classpath. The README permission table says the camera is used for barcode challenges (`README.md:154`), which isn't true today. Photo Match registers its reference only through `ActivityResultContracts.TakePicturePreview()` (`ui/alarmedit/AlarmEditScreen.kt:290-292`).

Top opportunities, in order:

1. Scan barcodes and QR codes with the camera, both when you register one and when the alarm rings (#51). CameraX 1.6.2 with zxing-cpp 3.1.1 runs offline in both flavors with no Play services.
2. Let Photo Match take its reference from the gallery through the Photo Picker (#52). The picker needs no permission, and the editor already has an `OpenDocument` launcher for the ringing-screen background to copy (`ui/alarmedit/AlarmEditScreen.kt:330-340`).
3. Move the Jackson pin from 2.18.9 to 2.18.11 and Guava to 33.7.2-android. An OSV query over the lockfiles found seven Jackson advisories in the Play build and one HIGH Guava advisory in both builds.
4. Check that the ringing screen really came back after the Spotify fallback. A background launch the system blocks doesn't throw, so the `catch` in `bringFiringScreenBack` (`service/AlarmService.kt:1353-1371`) never sees that failure.
5. Make Photo Match compare shapes as well as colors. `util/PhotoMatcher.kt` scales both images to 64x64 and compares color histograms, nothing else.

One finding changes the order of work already planned. On Android 17, alarm sound played through `MediaPlayer` without an audio focus request was measured muted despite the documented exact alarm waiver, and this app does exactly that in four places. The audio focus item already on ROADMAP.md (P2) now has to land before the targetSdk 37 bump.

Plan by tier (each item is written out in ROADMAP.md under "Research-Driven Additions, 2026-11-14"):

- Now: items 1 to 4 above.
- Next: item 5, and a dependency refresh that doesn't wait on AGP 9.
- Later: honest wording for the smart alarm notification.
- Under consideration: weather on the ringing screen, awesome-list entries, NSD picker discovery for smart-home bridges (see Rejected Ideas).
- Rejected: listed with reasons under Rejected Ideas.

Confidence is Verified unless a line says otherwise. Verified means a tool returned it during this pass (a file read, an API response or a fetched page).

## Product Map

| Area | What's there today | Where |
|---|---|---|
| Scheduling | `setAlarmClock()`, a Direct Boot cache, `USE_EXACT_ALARM` and `SCHEDULE_EXACT_ALARM` both declared, holidays, shift patterns, solar offsets, calendar-tag skip, skip next, global pause and vacation | `domain/AlarmScheduler.kt`, `domain/NextAlarmCalculator.kt`, `domain/VacationAlarmPolicy.kt`, `directboot/` |
| Ringing | `mediaPlayback` foreground service on Media3 1.10.1, gradual volume, backup sound, Spotify with an 8 second watchdog and fallback tone, internet radio with HLS and station search, YouTube audio in the Play build, ringtone pools, TTS, flash and strobe | `service/AlarmService.kt` (2,628 lines) |
| Dismissal | 30 challenge types plus NONE, chains, repeat counts, hold to dismiss, an accessibility bypass after a delay, wake confirmation | `ui/alarmfiring/challenges/`, `worker/WakeConfirmWorker.kt` |
| Integrations | Philips Hue and IKEA DIRIGERA sunrise, webhooks with HMAC, Tasker, Guardian Angel calls and texts, Health Connect in the Play build | `integration/hue/`, `integration/dirigera/`, `worker/` |
| Bedtime | reminders, sleep sounds, Sonar, a smart alarm window driven by phone motion, a chronotype estimate | `ui/bedtime/`, `domain/ChronotypeEstimator.kt` |
| Other tools | concurrent timers with promoted ongoing notifications, stopwatch, world clock, a Today dashboard, Glance widgets | `ui/timer/`, `service/PromotedOngoingNotification.kt`, `widget/` |
| Data | Room schema 25, DataStore, backup format 20 with an encrypted codec | `data/` |
| Languages | English and Japanese. All 2,265 translatable keys have a Japanese entry (2,276 English strings, the rest marked untranslatable) | `res/values/`, `res/values-ja/` |
| Distribution | GitHub Releases only: play, fdroid and wear APKs plus a Play AAB, with `SHA256SUMS.txt` and certificate fingerprints | |

## Competitive Landscape

### Open source

Checked through the GitHub API on 2026-11-14.

| Project | Stars | Last push | Latest release | What matters here |
|---|---|---|---|---|
| BlackyHawky/Clock | 1,191 | 2026-11-14 | 2.34 (2026-10-16) | Most active rival. #202 "Add ability to Pause Alarms (for vacations)" (18 thumbs up) is closed |
| vicolo-dev/chrono | 1,830 | 2025-01-13 | v0.6.0 (2025-01-05) | Dormant. #505 "PLEASE DON'T ABANDONED THIS APP!!!" (9 thumbs up, 2026-02-01) |
| FossifyOrg/Clock | 731 | 2026-11-09 | 1.6.0 (2026-01-30) | Top request #13 "Disable an alarm once" (17 thumbs up, open since 2024-01-12) |
| you-apps/ClockYou | 705 | 2026-11-14 | v12.1 (2026-10-08) | #405 "Pause alarm for some days" still open |
| yuriykulikov/AlarmClock | 622 | 2026-06-03 | no GitHub release | #319 "[Feature-request] Challenge/Puzzle to dismiss the alarm" open since 2020-02-12 |
| sweakpl/qralarm-android | 356 | 2026-11-14 | 2.11.5 (2026-11-09) | The whole app is "scan a QR or barcode to dismiss". #95 "Disable alarm until a given date" is open |
| WrichikBasu/ShakeAlarmClock | 54 | 2026-08-15 | v5.0.0 (2026-08-15) | Shake to dismiss |
| ahudson20/whakaara | 51 | 2026-07-27 | no GitHub release | Plain Compose alarm |
| DarkAspect1/Lucid-Alarm | 29 | 2026-11-11 | 1.5.1 (2026-11-11) | New since the last pass. README lists a Wake Check |
| izumisagirii/silent-alarm | 27 | 2026-10-05 | v2.1 (2026-10-05) | Earphone-only alarm |
| riyadmondol2006/WakeNudge | 10 | 2026-11-09 | v1.0.85 (2026-11-07) | New since the last pass. QR and NFC missions plus a still-awake check |
| RapidAppCoder/mighty-alarm-clock | 7 | 2026-10-12 | 2026.08.001 (2026-08-03) | #1 "QR/barcode alarm dismissal and interval reminders with spoken time" (2026-10-16) |

What this says:

- The most upvoted theme across the field is pausing or skipping alarms: Fossify #13 and #75 "One-time alarm", BlackyHawky #202, ClockYou #405 and qralarm #95. AlarmClockXtreme already has skip next and a global pause. A per-alarm pause is already on ROADMAP.md (P3), and these issues strengthen the case for it.
- Every app that offers a barcode challenge scans it with the camera: qralarm, WakeNudge, Alarmy and Sleep as Android. A typed barcode is the outlier, which is exactly what #51 noticed.
- Both newcomers since 2026-09 (WakeNudge and Lucid-Alarm) lead with a wake check, which AlarmClockXtreme already ships. Nothing in the newer projects is missing here except camera scanning.
- offa/android-foss (11,705 stars) and Psyhackological/AAA (3,159 stars) both list Fossify Clock, Clock You and Chrono. Neither mentions AlarmClockXtreme.

### Commercial

- Alarmy's missions include a QR/Barcode mission, and it sells "Prevent Falling Back Asleep" as a premium feature. Its barcode mission works off something you register in advance, such as a toothpaste barcode or a book's QR code. Its watch-an-ad-to-snooze prompt drew press backlash on 2025-03-23.
- Sleep as Android makes you prove you're up with a QR code, an NFC tag, shaking the phone or morning math. It also has an online radio alarm.
- Sleep Cycle sells a Smart Alarm built on phone sleep tracking.
- Samsung's One UI 8 Clock adds a home screen widget for an alarm group, and SamMobile (2025-05-30) notes the widget "makes it easier to turn it on or off". The One UI 8.5 beta adds weather backgrounds to alarms (Android Authority, 2025-12-08).
- Google Clock on Pixels keeps drawing missed-alarm coverage. Android Police reported the "unknown reason" missed-alarm notification coming back. Android Authority (2026-10-27) covered alarms being "automatically silenced".
- Name collision: Google Play lists "Alarm Clock Xtreme & Timer" by AVG Labs with 50M+ downloads (fetched 2026-11-14). Anyone searching for this project's name lands there first, and reviews or forum posts that mention "Alarm Clock Xtreme" are almost always about AVG's app (Likely). See Open Questions.

### Research on waking

- Snoozing: Sundelin et al. (J Sleep Res, 2024) found 30 minutes of snoozing cost about 6 minutes of sleep and improved or didn't affect cognitive tests on rising, in 31 habitual snoozers. Ogawa et al. (J Physiol Anthropol, 2022) found a snooze alarm prolonged sleep inertia compared with a single alarm, in 10 students. Mattingly et al. (Sleep, 2022) found 57% of participants snoozed, and snoozers didn't feel sleepier or sleep less. The evidence is mixed, which is why the app should keep snooze as a per-alarm choice rather than push either way.
- Sleep inertia can be a real performance hit right after waking (Hilditch and McHill, Nat Sci Sleep, 2019). That supports the wake-confirmation feature the app already has.
- Dawn simulation: Thompson et al. (Eur J Appl Physiol, 2014) found light during the last 30 minutes of sleep raised alertness ratings and performance after waking. Gabel et al. (Sleep, 2020) found no significant effect on subjective or objective alertness in middle-aged and older adults, and the dawn light cut the sleep before waking by 33%. The sunrise features are fine as comfort features. Don't claim they make you sharper.
- Phone sleep tracking: Fino et al. (J Sleep Res, 2020) tested four phone sleep app modes against polysomnography in 21 people. Three of them weren't reliable enough at telling sleep from wake, and the best one still underestimated time awake. This backs the README's "no sleep-stage claims" stance and is why the smart alarm notification shouldn't say it monitors sleep quality.
- Alarm sound: McFarlane et al. (PLOS ONE, 2020) linked melodic alarm sounds with less self-reported grogginess (questionnaire, 50 people). It's weak evidence, but it's a reasonable tiebreaker when picking the bundled fallback tone already on ROADMAP.md.

## Reported Issues

Tracker state on 2026-11-14: 49 stars, 10 forks.

Open issues, all fixed on `main` and waiting for a release:

- #65 "Can you please add radio alarm" (2026-11-13). Station search, playlist links and HLS landed on `main`.
- #63 "Connect app with ikea automation" (2026-11-02). DIRIGERA sunrise landed, and the reply asked the reporter to try it on their Kajplats lamp once it's out. No real DIRIGERA hub has run it yet (Needs live validation).
- #62 "Feature request: test an existing alarm without changing its schedule" (2026-10-19).
- #60 "Feature request: skip an alarm on days with a calendar event containing a tag (e.g. #holiday)" (2026-10-12).
- #59 "App crashes when accessing integrations and safety section" (2026-10-05).
- #53 "Spotify not ringing" (2026-08-30). Shared web links are normalized, the fallback tone plays after 8 seconds of silence, and the ringing screen comes back over Spotify. This was checked on a headless emulator, not yet on a phone.
- #49 "Should be able to turn off Snooze, and other comments" (2026-08-10). Reopened, answered and fixed.

Discussions without an answer:

- #51 "[Question] For the barcode scan challenge , is the barcode scanner not being available intentional?" (2026-08-30). The reporter followed up the same day with "A response would be appreciated." The code confirms the report (see Executive Summary).
- #52 "[Question] Is picture not being allowed from gallery for Photo Scan challenge intentional?" (2026-08-30). Also confirmed in code.

Closed: #64, #61 ("[Withdrawn]"), #50, #48, #47, #44 (a German translation offer that asked for Weblate or Crowdin) and #43.

Patterns in the history: of the 70 commit subjects since v1.15.35, translation and string work shows up in 11, the ringing screen in 7, DIRIGERA in 5, and Spotify and calendar skips in 4 each. Two of the seven open issues (#53 and #63) depend on another app or device, and that's where the remaining live checks are owed.

## Security, Privacy, and Reliability

### Dependency advisories

An OSV batch query over the 270 release coordinates in `app/gradle.lockfile` and `wear/gradle.lockfile` (2026-11-14) returned 9 hits.

| Package | Locked | Flavor | Advisories | Fixed in |
|---|---|---|---|---|
| jackson-databind | 2.18.9 | play | GHSA-cxp5-3px4-pw24, GHSA-gx83-3vf8-gh7j, GHSA-q4xh-88c3-wmh7, GHSA-wjgm-6hv5-3cvf, GHSA-wv8q-qhhj-9h54 | 2.18.11 |
| jackson-core | 2.18.9 | play | GHSA-7hhh-6rmp-j9qf, GHSA-p6pp-m3f8-5c89 | 2.18.11 |
| guava | 33.6.0-android | play | GHSA-xxph-c9ww-hj94 (HIGH, published 2026-10-27) | 33.7.2-android |
| guava | 33.3.1-android | fdroid | GHSA-xxph-c9ww-hj94 | 33.7.2-android |

Jackson only reaches the Play build, through the youtubedl-android and NewPipeExtractor graph that the constraint block in `app/build.gradle.kts:473-505` already pins. The Guava advisory is about Java deserialization of `CompactHashMap`, `CompactHashSet` and `MapMakerInternalMap`. The app doesn't appear to deserialize Java objects from outside input, so real exposure is probably low (Assumption), but the fix costs a version number. The fdroid flavor has no Guava constraint at all, because the existing one is `playImplementation` only. Jackson 2.18.11 and Guava 33.7.2-android are both on Maven Central.

### Reliability

- Blocked background launches are silent. The platform logs "Background activity launch blocked!" and doesn't throw. `bringFiringScreenBack` calls `startActivity` and only records an incident from its `catch`, so a blocked launch over Spotify leaves the fallback tone ringing with nothing on screen to dismiss it, and no incident row. In the #53 emulator checks the screen did come back, and the comment at `service/AlarmService.kt:111-115` says why: the launch lands inside Android's grace period of about 10 seconds after the ringing screen was last visible, which is why the watchdog is held at 8 seconds. Anything that pushes the launch past that window, or an OEM build that shortens it, turns it into a silent no-op, and Android 17 already extends background launch hardening to `IntentSender`.
- Android 17 background audio hardening applies to apps targeting API 37. The requirement is waived for an app that holds the exact alarm permission and changes streams with `USAGE_ALARM`, and the app declares both `USE_EXACT_ALARM` and `SCHEDULE_EXACT_ALARM`. When a call is refused, playback and volume APIs "fail silently without throwing an exception". The page documents a test switch, `adb shell cmd audio set-enable-hardening <enable|disable|throw>`. Note that `enable` and `throw` apply the rule to every app and drop the alarm waiver, so a failure under them is expected and doesn't by itself prove a bug. That makes the existing Android 17 audio checks on ROADMAP.md concrete.
- The waiver isn't as dependable as the page makes it sound. On an API 37 emulator (2026-11-06, AOSP `android-17.0.0_r1`), alarm-stream sound from a `MediaPlayer` in a service started in the background was muted (`mutedState:opControlAudio`) despite the exact alarm permission. The waiver checks a cached permission that only focus and volume calls refresh, and a `MediaPlayer` plays from mediaserver rather than the app's own uid. Requesting audio focus with alarm attributes first fixed it. This app rings through `MediaPlayer` in four places (`service/AlarmService.kt:1515`, `:1606` and `:1691`, `directboot/DirectBootAlarmService.kt:140`, `ui/timer/TimerAlarmService.kt:232`) and never requests audio focus, so the audio focus item already on ROADMAP.md has to land before the targetSdk 37 bump (Likely. This app's own paths still need live validation.).
- Only `dataSync` and `mediaProcessing` foreground services have the 6 hour timeout, so the `mediaPlayback` ringing service isn't affected. Since Android 15, though, a `BOOT_COMPLETED` receiver can't start a `mediaPlayback` service. The ringing service has to keep starting from the alarm delivery itself.
- `USE_EXACT_ALARM` is granted at install and can't be revoked, and alarm clock apps are explicitly allowed to use it. The app already declares it, so the revocation path matters only on Android 12 and 12L (API 31 and 32), where the emulator audit on ROADMAP.md is still owed.
- The Android 17 memory limiter (`MemoryLimiter:AnonSwap` in `ApplicationExitInfo`) is real and documented. The `ApplicationExitInfo` item already on ROADMAP.md (P1) covers it.
- Missed alarms are still the category's top complaint: the Pixel coverage above, a Samsung community thread from 2026-01-04 ("sometimes it rings and sometimes it doesn't"), and dontkillmyapp's Samsung page, which says an app not started in 3 days is put to sleep and "background tasks including alarms will stop working".

### Privacy

- The smart alarm's ongoing notification says "Monitoring sleep quality…" (`res/values/strings.xml:97`), while its own hint says it "Uses conservative phone-motion scoring" (`:612`) and the README FAQ says Smart wake and Sonar are not sleep-stage measurements. The notification promises more than the feature does.
- A camera barcode scanner means a live camera preview at dismiss time. `CAMERA` is already declared for Photo Match. Frames should stay in memory and never be written to disk, and zxing-cpp decodes offline.
- The Photo Picker hands back a single URI with no storage permission, which is the least access possible for #52.

## Architecture Assessment

Largest source files in `app/src/main`:

| File | Lines |
|---|---|
| `ui/alarmfiring/challenges/ChallengeViews.kt` | 2,771 |
| `service/AlarmService.kt` | 2,628 |
| `ui/alarmlist/AlarmListScreen.kt` | 1,916 |
| `ui/settings/SettingsScreen.kt` | 1,849 |
| `ui/alarmfiring/AlarmFiringScreen.kt` | 1,743 |
| `ui/stats/StatsScreen.kt` | 1,604 |
| `ui/alarmfiring/AlarmFiringViewModel.kt` | 1,395 |

- `AlarmService.kt` was 2,400 lines when the audio extraction item went onto ROADMAP.md on 2026-09-04 and is 2,628 now. Seven commits have touched it since then (375 lines in, 147 out), two of them Spotify fixes for #53 in the very region that item wants to move out. The extraction gets more valuable with every Spotify fix.
- `ChallengeViews.kt` holds every challenge's UI in one file. The camera scanner should go in its own file with a small shared camera composable that Photo Match can reuse, instead of adding another few hundred lines here.
- Field drift between preferences and backups is the bug class this repo keeps hitting, and `BackupManagerSettingsDriftTest` plus the ChallengeType whitelist test guard it. Any new alarm field (a gallery reference, a scanned code format) has to go through `Alarm.VALID_CHALLENGE_TYPES`, the Room migration and the backup format together.
- Supply chain: 874 components are pinned in `gradle/verification-metadata.xml`, and both modules have lockfiles. An OSV batch query straight against the lockfiles takes seconds and needs no Gradle run, which is how this pass found the Jackson and Guava advisories.

Dependency currency, checked against official release pages on 2026-11-14:

| Dependency | In the repo | Latest stable | Blocked on AGP 9? |
|---|---|---|---|
| Android Gradle Plugin | 8.11.1 | 9.5.1 (9.5 needs Gradle 9.6.0) | it is the AGP 9 item |
| Kotlin | 2.1.0 | 2.4.21 | yes |
| KSP | 2.1.0-1.0.29 | 2.3.14 (2026-11-13) | yes |
| Dagger and Hilt | 2.56.2 | 2.60.1 (2026-07-06) | yes |
| androidx.hilt | 1.2.0 | 1.4.0 | yes |
| Room | 2.6.1 | 2.8.5 | yes (KSP2) |
| WorkManager | 2.9.1 | 2.12.0 | yes (existing note) |
| Media3 | 1.10.1 | 1.11.1 | no |
| DataStore | 1.1.1 | 1.3.0 (2026-10-21) | unknown (Assumption: no) |
| Glance | 1.1.1 | 1.2.0 | no |
| core | 1.15.0 | 1.19.1 | unknown |
| lifecycle | 2.8.7 | 2.11.0 | unknown |
| OkHttp | 5.4.0 | 5.5.1 | no |
| NewPipeExtractor | v0.26.3 | v0.27.1 (2026-11-10). v0.26.5 is the last before 0.27.0 raised the Java target to 17 and changed `Description` to a record | no |
| Robolectric | 4.14.1 | 4.18 (2026-11-13) | no |
| CameraX (new) | none | 1.6.2 | no |
| zxing-cpp Android (new) | none | 3.1.1 | no |

`activity-compose` 1.13.0 is current. youtubedl-android 0.18.1 is the newest tag, though GitHub marks it a prerelease.

## Rejected Ideas

- ML Kit barcode scanning or Google's code scanner. The code scanner needs Play services, and ML Kit is a proprietary Google library that the fdroid flavor keeps out (ML Kit digital ink is `playImplementation` only, `app/build.gradle.kts:468`). The F-Droid APK is the most downloaded build, and one scanner in both flavors keeps the challenge identical. zxing-cpp is Apache-2.0 and offline.
- Keeping typed entry at dismiss time. If you can type the code at the ringing screen, the barcode is just a password you can recite in bed. Typing stays useful when registering a code you can't scan, and the existing accessibility bypass already covers people who can't do a physical challenge.
- Sleep-stage smart wake. In Fino et al. 2020, three of four phone app modes weren't reliable at telling sleep from wake, and the README already promises no stage claims.
- Making snooze punitive or removing it by default. The snooze studies point in different directions, and the app already lets each alarm turn snooze off or cap it.
- Ad-gated snooze or any monetized wake-up step. Alarmy's version drew press backlash, and the app has no ads by design.
- Store listings of any kind, including the main F-Droid repository, IzzyOnDroid and Google Play. The project ships through GitHub Releases only.
- Wear OS feature work. The `:wear` module stays as it is.
- Headphones-only alarms (yuriykulikov #678, silent-alarm). An alarm that only plays through earbuds fails silently when they disconnect or run flat, and the 2026-09-04 pass rejected it for the same reason.
- Downsampling widget bitmaps for the Android 17 RemoteViews budget. The `widget/` package doesn't use bitmaps, so there's nothing to fix.
- Live Updates for timers. Already shipped through `service/PromotedOngoingNotification.kt`.
- Timer sound in silent or vibrate mode (Fossify #253, 16 thumbs up). The timer already plays with alarm attributes (`ui/timer/TimerAlarmService.kt:234`).
- A one-tap switch for a whole alarm group, as in Samsung's group widget. The list already does it in three taps: pick the group chip, Select all, then Disable (`ui/alarmlist/AlarmListScreen.kt:409-413`), and nobody has asked for less.

### Under consideration (not planned)

- Live weather behind the ringing screen, as in One UI 8.5. The app already has a weather dashboard and custom ringing backgrounds, so it's possible, but nobody has asked for it.
- Proposing AlarmClockXtreme to offa/android-foss and Psyhackological/AAA. These are public pull requests to other people's repos, so it's Matt's call.
- Using the `NsdManager` picker to find a Hue or DIRIGERA bridge without the local network permission. Control still needs the permission, so this only saves a step during setup (Likely).

## Sources

### Open-source competitors
- https://github.com/BlackyHawky/Clock
- https://github.com/vicolo-dev/chrono
- https://github.com/FossifyOrg/Clock
- https://github.com/you-apps/ClockYou
- https://github.com/yuriykulikov/AlarmClock
- https://github.com/sweakpl/qralarm-android
- https://github.com/WrichikBasu/ShakeAlarmClock
- https://github.com/ahudson20/whakaara
- https://github.com/DarkAspect1/Lucid-Alarm
- https://github.com/izumisagirii/silent-alarm
- https://github.com/riyadmondol2006/WakeNudge
- https://github.com/RapidAppCoder/mighty-alarm-clock/issues/1

### Commercial products
- https://alar.my/en
- https://alar.my/en/blog/alarmy-wake-up-mission
- https://futurism.com/alarm-app-advertisement-snooze
- https://sleep.urbandroid.org/
- https://www.sleepcycle.com/
- https://www.sammobile.com/news/samsung-one-ui-8-clock-app-alarm-group-improvements/
- https://www.androidauthority.com/samsung-one-ui-8-5-beta-clock-changes-3623198/
- https://play.google.com/store/apps/details?id=com.alarmclock.xtreme.free&hl=en_US

### Adjacent tools and libraries
- https://github.com/zxing-cpp/zxing-cpp/releases
- https://repo1.maven.org/maven2/io/github/zxing-cpp/android/maven-metadata.xml
- https://developers.google.com/ml-kit/vision/barcode-scanning/android
- https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner
- https://dontkillmyapp.com/samsung

### Curated lists
- https://github.com/offa/android-foss
- https://github.com/Psyhackological/AAA

### Community and press
- https://github.com/SysAdminDoc/AlarmClockXtreme/discussions/51
- https://github.com/SysAdminDoc/AlarmClockXtreme/discussions/52
- https://github.com/FossifyOrg/Clock/issues/13
- https://www.androidauthority.com/google-pixel-alarm-bug-3728689/
- https://www.androidpolice.com/pixel-alarm-bug-is-back/
- https://www.howtogeek.com/google-pixel-phone-alarm-app-not-working-again/
- https://piunikaweb.com/2025/12/02/poll-have-you-ever-experienced-the-missing-alarm-bug-on-a-google-pixel/
- https://community.samsung.com/in/conversations/Galaxy-M/Alarm-not-ringing-consistently-on-my-Samsung-phone/r2_21341516
- https://forum.f-droid.org/t/clocks-timers-and-sound/34973

### Platform and standards
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developer.android.com/about/versions/16/behavior-changes-all
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/about/versions/17/behavior-changes-all
- https://developer.android.com/about/versions/17/behavior-changes-17
- https://developer.android.com/about/versions/17/changes/bg-audio
- https://developer.android.com/about/versions/17/features
- https://developer.android.com/about/versions/17/release-notes
- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/background-work/services/fgs/timeout
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/guide/components/activities/background-starts
- https://developer.android.com/develop/ui/views/notifications/time-sensitive
- https://developer.android.com/develop/ui/views/notifications/live-update
- https://developer.android.com/privacy-and-security/local-network-permission
- https://developer.android.com/training/data-storage/shared/photopicker

### Academic
- https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=37849039&rettype=abstract&retmode=text
- https://research.monash.edu/en/publications/is-snoozing-losing-why-intermittent-morning-alarms-are-used-and-h/
- https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=36587230&rettype=abstract&retmode=text
- https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=35951011&rettype=abstract&retmode=text
- https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=31692489&rettype=abstract&retmode=text
- https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=24509892&rettype=abstract&retmode=text
- https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=32307533&rettype=abstract&retmode=text
- https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=31674096&rettype=abstract&retmode=text
- https://journals.plos.org/plosone/article?id=10.1371%2Fjournal.pone.0215788

### Dependency changelogs
- https://developer.android.com/jetpack/androidx/releases/media3
- https://developer.android.com/jetpack/androidx/releases/room
- https://developer.android.com/jetpack/androidx/releases/work
- https://developer.android.com/jetpack/androidx/releases/datastore
- https://developer.android.com/jetpack/androidx/releases/glance
- https://developer.android.com/jetpack/androidx/releases/core
- https://developer.android.com/jetpack/androidx/releases/lifecycle
- https://developer.android.com/jetpack/androidx/releases/hilt
- https://developer.android.com/jetpack/androidx/releases/camera
- https://developer.android.com/jetpack/androidx/releases/activity
- https://developer.android.com/build/releases/gradle-plugin
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml
- https://kotlinlang.org/docs/releases.html
- https://github.com/google/ksp/releases
- https://github.com/google/dagger/releases
- https://github.com/TeamNewPipe/NewPipeExtractor/releases
- https://github.com/robolectric/robolectric/releases
- https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml

### Security advisories
- https://osv.dev/vulnerability/GHSA-7hhh-6rmp-j9qf
- https://osv.dev/vulnerability/GHSA-p6pp-m3f8-5c89
- https://osv.dev/vulnerability/GHSA-cxp5-3px4-pw24
- https://osv.dev/vulnerability/GHSA-gx83-3vf8-gh7j
- https://osv.dev/vulnerability/GHSA-q4xh-88c3-wmh7
- https://osv.dev/vulnerability/GHSA-wjgm-6hv5-3cvf
- https://osv.dev/vulnerability/GHSA-wv8q-qhhj-9h54
- https://osv.dev/vulnerability/GHSA-xxph-c9ww-hj94
- https://github.com/advisories/GHSA-xxph-c9ww-hj94
- https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/maven-metadata.xml
- https://repo1.maven.org/maven2/com/google/guava/guava/maven-metadata.xml

## Open Questions

1. When does v1.16.0 ship? Every open issue's fix is sitting on `main`, and reporters can't see any of it until a release. Releases happen on Matt's word only.
2. What to do about the name collision with AVG Labs' "Alarm Clock Xtreme & Timer" (50M+ downloads on Google Play)? Options run from a one-line "not affiliated" note in the README to a rename. It's a product and identity call, parked in Roadmap_Blocked.md.
3. Should Discussions #51 and #52 get an answer now, or when the scanner and gallery work lands? Both have waited since 2026-08-30, and a short "you're right, it's coming" would close the gap without promising a version.
4. How strict should Photo Match be? A shape-aware comparison needs a threshold, and only real photos taken in morning light versus the reference can set it (Needs live validation).
