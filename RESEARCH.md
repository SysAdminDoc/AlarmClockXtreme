# Research: AlarmClockXtreme
Date: 2026-09-04. Delivery status updated 2026-09-05. Replaces all prior research (previous pass: 2026-07-22).

## Executive Summary

AlarmClockXtreme is a local-first Android alarm, bedtime, and timer suite at
v1.15.35 (versionCode 137, DB v24, backup v19, 675 JVM tests across 125 test
files). The engine is mature: `setAlarmClock()` + Direct Boot fallback, a
post-fire watchdog (`worker/FireWatchdogWorker.kt`), an 8-second in-ring stall
watchdog, a redacted incident stream (`alarm_incident_events`), 30 dismiss
challenges, Android 16 Live Updates already wired into three notification
surfaces, and 860 components pinned in `gradle/verification-metadata.xml`. There
are zero TODO/FIXME markers in 113k lines of Kotlin. The technical debt is not
in the code.

The immediate delivery gap was resolved in v1.15.35. Signed Play, F-Droid, and
Wear APKs now carry the fixes that had been sitting on `main`, with checksums
and certificate fingerprints. The README names a real release asset and shows
current app screens. The app still has **no in-app update check** (grep for
`api.github.com` / `releases/latest` across `app/src/main` returns nothing), so
sideloaded installs have no built-in way to learn that a newer release exists.

Top opportunities in priority order:

1. Ship an in-app GitHub-Releases update check. Sideload distribution without
   one guarantees a permanently fragmented install base.
2. Normalise pasted Spotify web links to `spotify:` URIs. A pasted
   `https://open.spotify.com/playlist/...?si=...` is handed to Spotify verbatim
   (`service/AlarmService.kt:893-908`), which is the most likely reason #50/#53
   saw Spotify open and play nothing.
3. Fix the Spotify playback watchdog's detection predicate
   (`AlarmService.kt:1179-1186`) so the fallback tone does not fire over working
   playback.
4. Make backup-sound escalation audible when the service owns no player
   (`AlarmService.kt:575` promises a behaviour `:580-592` does not implement).
5. Give users a real "no snooze" option. Issue #49 asked for it; today
   `maxSnoozeCount = 0` means *unlimited*, the exact inverse
   (`domain/SnoozeCapPolicy.kt:41`).
6. Capture `ApplicationExitInfo` and surface a user-facing reliability report.
   The app records rich incidents but can never say *why* the OS killed it,
   which is precisely the "unknown reason" failure the whole Android ecosystem
   is complaining about.
7. Land the first translation locale. 2136 strings and 43 plurals are extracted,
   the build guard is in place, and `values-*` contains **zero** locales.
8. Add undo to bulk alarm delete. It is the one irreversible data action left
    (`ui/alarmlist/AlarmListScreen.kt:356` states the gap explicitly).

Confidence is **Verified** unless marked otherwise.

## Product Map

- **Core workflows:** create / schedule / skip / snooze / dismiss / recover
  alarms; timers, stopwatch, world clocks; bedtime planning and local sleep and
  wake statistics; auto-created alarms from calendar, commute, holiday, shift,
  weather and solar inputs; backup, restore, share, and local diagnostics.
- **Personas:** heavy sleepers and challenge users; shift and on-call workers;
  privacy-focused users who refuse accounts and telemetry; travellers;
  accessibility users; power users wiring Hue, Spotify, webhooks, calendars,
  Health Connect and custom audio.
- **Platforms and distribution:** Android 8+ phone / tablet / foldable
  (`minSdk 26`, `targetSdk 36`, `compileSdk 36`) in `play` and `fdroid` flavors,
  plus a `:wear` companion module. Distribution is GitHub Releases and direct
  sideload only. No store listing, by standing policy.
- **Key integrations and data flow:** `AlarmManager`, Direct Boot storage,
  full-screen intents, Room DB v24, DataStore, Media3 1.10.1, Open-Meteo,
  Nager.Date, Google Routes (optional key), Health Connect (play), Hue
  (TOFU-pinned), yt-dlp and NewPipeExtractor (play), ML Kit Digital Ink (play),
  SAF backup, HMAC-signed webhooks, Glance widgets, Quick Settings tiles. Room
  owns records, DataStore owns preferences, device-protected storage carries the
  locked-boot snapshot, and backup/share codecs sanitise before user-confirmed
  import.

## Competitive Landscape

- **FossifyOrg/Clock** (691 stars, pushed 2026-09-01, 92 open issues). Best at
  clean AOSP-derived clock UX with a maintained widget set. Learn from its open
  tracker, which reads as a to-do list ACX has half-solved: undo a just-deleted
  alarm (#491), name the specific alarm in a missed-alarm notification (#465),
  "alarm randomly fails with custom sound" (#447, the same dead-content-URI
  class ACX now handles through `escalateMedia3PlaybackFailure`). Avoid its
  widget-configuration sprawl.
- **BlackyHawky/Clock** (1083 stars, pushed 2026-07-28, 34 open issues).
  Strongest at narrow OEM-regression fixes and per-timer behaviour. Learn:
  distinct notification icons per notification type (#691) is a direct hit on
  ACX, where all 27 `setSmallIcon` call sites use `R.drawable.ic_alarm` and
  `res/drawable/` contains exactly one non-launcher icon; disabling touch on the
  full-screen alert while the proximity sensor is covered (#687) prevents
  in-pocket dismissal. Avoid its habit of shipping format changes that break
  older exports.
- **you-apps/ClockYou** (648 stars, pushed 2026-09-03). Learn: gradual volume
  ramping over a much longer window (#392) and "new alarm inherits the last-used
  values" (#396). Nothing else is net-new against ACX.
- **yuriykulikov/AlarmClock** (618 stars, 84 open issues). Signature gentle
  pre-alarm, already tracked as L-A10. Its #774 ("Full-screen alarm and alarm
  banner not working since Android 16") is the platform regression worth
  watching; ACX verified full-screen firing on a physical Galaxy S22 Ultra at
  API 36 in July 2026, so it is likely unaffected, but it is a live risk class.
  Its #741 (alarm skip does not persist through reboot) is a bug ACX already
  fixed in v1.15.29.
- **sweakpl/qralarm-android** (346 stars, pushed 2026-08-26). Learn two feature
  shapes ACX lacks: "disable this alarm until a given date" (#95) and "delay the
  next occurrence once" (#98). ACX has a global `pauseUntilMillis` setting
  (`data/preferences/PreferencesManager.kt:156`) and per-alarm skip-next, but no
  per-alarm pause-until-date. Avoid its paid-tier gating of core dismissal
  behaviour.
- **vicolo-dev/chrono** (1759 stars, last pushed 2025-01-13, 191 open issues).
  Effectively unmaintained. Its ringtone-shuffle and reduce-volume-during-task
  ideas remain interesting; ACX already ships a random ringtone pool. Avoid
  treating its star count as a signal of current health.
- **WrichikBasu/ShakeAlarmClock** (51 stars, pushed 2026-08-15). Small, but its
  stated design principle is the right one to copy in marketing terms: the alarm
  is a service with almost no dependency on the UI, so a frozen UI cannot lose
  the alarm. ACX's architecture already matches this; nothing says so anywhere a
  user reads.
- **Alarmy (commercial).** Ad-gated snooze extension drew visible backlash in
  March 2025 (Futurism). Reviewers also report missions becoming repetitive.
  Learn: ACX's free equivalents are a genuine differentiator, and mission
  *variety* matters more than mission count. Avoid the subscription model and
  the ad-gated snooze pattern outright.
- **Sleep as Android (commercial).** Deepest sleep-tracking feature set,
  characterised by reviewers as "feature hungry". Learn its integration breadth;
  avoid letting bedtime and sleep features crowd out the alarm.
- **Google Clock / Pixel.** The single largest acquisition opportunity in the
  category, and it is a reliability story, not a feature story. The "Missed
  alarm: Alarm did not fire due to an unknown reason" bug has recurred in waves
  since at least 2021, with fresh coverage in March 2025, July 2025 and
  December 2025, and Google reportedly cannot reproduce it. Its Spotify alarm
  integration has also broken repeatedly with a "You are not logged into
  Spotify" error, and the official workaround was to stop using Spotify as the
  alarm source (Confidence: Likely, source thread is undated).

## Reported Issues

The repo tracker holds **one open issue** and six closed ones. Discussions are
enabled and empty. All 42 PRs are closed Dependabot bumps from June 2026; nine
stale `dependabot/*` branches still exist on the remote.

**Open**

- **#53 "Spotify not ringing" (bug, Android 11, v1.15.32, filed 2026-08-30).**
  Spotify playlist set as the alarm sound, alarm screen appears, no audio, no
  backup alarm. **Almost certainly already fixed on `main`.** On 1.15.32 the
  Spotify branch fired an `ACTION_VIEW` intent, recorded
  `SPOTIFY_DELEGATED / STATUS_SUCCEEDED` purely because the intent was sent,
  and returned without creating a player. `2c17a85` (2026-08-22, "Fixes #50")
  added `armSpotifyPlaybackWatchdog` (`AlarmService.kt:1188-1212`), which falls
  back to the default tone after `PLAYBACK_START_TIMEOUT_MS = 8_000`
  (`AlarmService.kt:111`). The reporter cannot have that fix because it was
  never released. Resolving #53 is a release action, not a code action, plus the
  three residual Spotify defects below.

**Closed, correctly**

- **#50 "Spotify integration" (2026-08-14).** Same symptom as #53, from the same
  reporter. Fixed by `2c17a85`, unreleased.
- **#47 "'Edit' icon missing".** Fixed by `4cc5580`; the row now carries
  `Icons.Default.Edit` at `ui/alarmlist/AlarmListScreen.kt:1411`.
- **#48 "Menu boxes have extra blank space on the bottom".** Fixed by `12738b5`;
  the `.heightIn(min = 104.dp)` floor at `ui/alarmedit/AlarmEditSupport.kt:390`
  became `.fillMaxHeight()` with an `IntrinsicSize.Min` row.
- **#43 "Custom dismiss phrase … challenge not blocking dismiss".** Both parts
  fixed in v1.15.32 and released.

**Closed, but the request is still open in the code**

- **#49 "Should be able to turn off Snooze, and other comments" (5 items).**
  Items 3, 4 and 5 shipped in v1.15.34 (editor keeps scroll position, snooze
  settings grouped, alarm card shows the whole mission chain). **Item 1 did
  not.** There is no snooze-off toggle anywhere: `Alarm.snoozeDurationMinutes`
  is clamped to `1..180` (`data/model/Alarm.kt:321`), the picker offers
  `1, 3, 5, 10, 15, 20, 30` (`ui/alarmedit/AlarmEditDismissSections.kt:104`),
  the limit picker offers `0, 1, 2, 3, 5, 10`
  (`AlarmEditDismissSections.kt:133`), and `0` is rendered "Unlimited" because
  `SnoozeCapPolicy.isCapped` is `maxSnoozeCount > 0`
  (`domain/SnoozeCapPolicy.kt:41`). A user reaching for "off" gets infinite
  snoozes. The Snooze button is only ever disabled, never hidden
  (`ui/alarmfiring/AlarmFiringScreen.kt:1091`), and with a minimum cap of 1 it
  is always live on the first ring.
- **#43 follow-up.** The maintainer publicly promised a "use only my phrases"
  toggle in the closing comment. It was never built. `ChallengeGenerator.kt:368-381`
  still unions custom phrases onto `TYPING_PHRASES` with no exclusive branch,
  and no `customPhrasesOnly` symbol exists anywhere in the repo.
- **#44 "Translations".** A contributor offered German and the maintainer laid
  out the exact path (a plain `values-de/strings.xml` PR). Nothing landed, and
  there are still zero locale directories.

**Not acted on**

- Nothing in the tracker was judged stale or unreproducible. The tracker is
  small, high signal, and every report maps to real code.

## Security, Privacy, and Reliability

- **Verified, actionable: `org.jsoup:jsoup:1.22.2` carries CVE-2026-71497**
  (GHSA-pmhh-3w7g-xqp8, moderate, CVSS 4.7, published 2026-07-30, affects
  1.14.3 through 1.23.0, fixed in 1.23.1). An OSV `querybatch` over all 269
  components in `app/gradle.lockfile` and `wear/gradle.lockfile` on 2026-09-04
  returned this as the **only** hit. It arrives transitively through
  NewPipeExtractor and resolves in `playReleaseRuntimeClasspath` only
  (`app/gradle.lockfile:228`). **Not exploitable here**: the advisory requires
  custom jsoup Safelists permitting raw-text elements, no app code imports jsoup
  (zero `Jsoup` references under `app/src`), and the only WebView
  (`ui/components/WindyRadarCard.kt:153`) loads Windy's own URL, not parsed
  markup. It still belongs in the constraint block beside the eight existing
  CVE-driven pins (`app/build.gradle.kts:468-497`), which is this repo's
  established pattern.
- **Verified: no audio focus handling anywhere.** `requestAudioFocus`,
  `AudioFocusRequest`, `OnAudioFocusChange` and `AUDIOFOCUS_` return zero
  matches across `app/src/main/java`. The service cannot pause a media app that
  is playing, cannot learn it lost focus, and cannot detect being ducked.
  Android 17's background-audio hardening makes focus requests fail with
  `AUDIOFOCUS_REQUEST_FAILED` outside a valid lifecycle, so any adoption has to
  be written defensively.
- **Verified: no audibility check.** `getStreamVolume` appears twice
  (`AlarmService.kt:583`, `:1226`) and only to save a value for later restore.
  Nothing ever asks whether `STREAM_ALARM` is at zero, and `isVolumeFixed` is
  never called. An alarm can be reported as playing while producing no sound.
- **Verified: the Spotify success predicate is probably unreachable.**
  `mediaStreamsBefore()` (`AlarmService.kt:1179-1186`) filters
  `activePlaybackConfigurations` on `audioAttributes.usage == USAGE_MEDIA`.
  AOSP's `AudioPlaybackConfiguration.anonymizedCopy()` sanitises configurations
  belonging to other apps for callers without `MODIFY_AUDIO_ROUTING`, preserving
  system usages conditionally. If `USAGE_MEDIA` does not survive anonymisation,
  the watchdog always concludes Spotify failed and layers the fallback tone over
  working playback. Confidence: **Likely, needs device validation.** A second,
  independent defect in the same function: the baseline is sampled *after*
  `startActivity` (`:926`), so a warm Spotify that starts inside the sampling
  window is counted into the baseline and reads as a failure.
- **Verified: backup-sound escalation cannot rescue a delegated alarm.** The
  comment at `AlarmService.kt:575` says the job will switch to the system alarm
  tone. The body (`:580-592`) raises `STREAM_ALARM` (Spotify plays on
  `STREAM_MUSIC`), records an incident, and calls `applyPlaybackGain()`, which is
  `alarmPlayback?.setVolume(...)` at `:2385` and a no-op when the Spotify branch
  returned at `:927` without a player. Compounding this,
  `Alarm.backupSoundEnabled` defaults to `false` (`data/model/Alarm.kt:70`), so
  the safety net is off for every alarm nobody configured.
- **Verified: no `ApplicationExitInfo` usage.** The app records a detailed
  incident stream but has no access to the OS's own reason for killing the
  process. Android 17 adds a memory limiter that kills apps over a RAM threshold
  **regardless of targetSdk**, discoverable through
  `ApplicationExitInfo.getDescription()` containing `MemoryLimiter:AnonSwap`.
  For an alarm app, an OS kill is a missed alarm, and this is the one signal
  that turns "unknown reason" into a specific, reportable cause.
- **Verified: bulk delete is irreversible.** Single-alarm delete offers undo
  (`ui/alarmlist/AlarmListViewModel.kt:115`, `undoDelete()`), but the
  multi-select path says so in its own confirmation copy: "This bulk action does
  not offer per-alarm undo" (`ui/alarmlist/AlarmListScreen.kt:356`).
- **Verified: the last silent-alarm path with no escalation.** If
  `RingtoneManager` returns null for both ALARM and NOTIFICATION defaults
  (stripped AOSP builds, some managed profiles), `AlarmService.kt:1432-1443`
  records `NO_DEFAULT_TONE` and returns. The alarm rings nothing, by design,
  with no fallback tone bundled in the APK. `res/raw/` is empty.
- **Verified, low: CVE-2026-53914 (GHSA-r937-wjx7-w2jp)** affects
  `org.jetbrains.kotlin:kotlin-gradle-plugin` before 2.4.20-Beta1; the repo is on
  2.1.0. Moderate (6.7), local, build-time only, no runtime exposure in shipped
  APKs. It is one more reason to unblock the AGP 9 chain, not a standalone item.
- **Verified: `verification-metadata.xml` is current.** 860 pinned components,
  `verify-metadata = true`, `verify-signatures = false`, last touched `80127d8`
  with the v1.15.35 audit adding jsoup 1.23.2. Dependency locking is
  `LockMode.STRICT` over all three release runtime classpaths, and
  `verifyStableReleaseDependencies` rejects any alpha, beta or rc.
- **Verified: signing material sits in the working tree.** `keystore.properties`
  and `alarmclock-release.jks` are at the repo root and are covered by
  `.gitignore` (`*.jks`, `keystore.properties`). No leak, but any tooling that
  copies the tree (the documented `robocopy` to `C:\ab` build workaround) carries
  the release key with it.

## Architecture Assessment

- **God files, still the top refactor candidates:**
  `ui/alarmfiring/challenges/ChallengeViews.kt` (2766),
  `service/AlarmService.kt` (2400), `ui/alarmlist/AlarmListScreen.kt` (1810),
  `ui/settings/SettingsScreen.kt` (1744), `ui/alarmfiring/AlarmFiringScreen.kt`
  (1693), `ui/stats/StatsScreen.kt` (1561). `SettingsScreen.kt` came down from
  4129 and `AlarmService.kt` from 2164 was already the target of seam
  extraction, so the pattern works. `AlarmService.kt` is where every residual
  audio defect in this report lives, and the audio start / watchdog / escalation
  cluster (`:841-1310`) is the natural next seam: a `AlarmAudioDirector` that
  owns source selection, watchdogs and escalation, testable without the service.
- **The play/fdroid split now costs more than it buys.** The `play` flavor is
  the feature-rich one (YouTube downloader, Health Connect, ML Kit handwriting,
  Wear bridge) and is what the README tells users to install. Yet
  `app/src/fdroid/AndroidManifest.xml` holds `SEND_SMS` and the comment states
  the Play flavor degrades Guardian Angel to a prefilled SMS composer "to comply
  with Google Play restricted SMS permission policy". This project is never
  listed on Play. The recommended build ships a weaker emergency-escalation
  feature to satisfy a constraint that does not apply to it.
- **`androidx.core:core-ktx` is split-brained:** `:app` on 1.15.0
  (`app/build.gradle.kts:392`) against `:wear` on 1.18.0
  (`wear/build.gradle.kts:69`), in two modules sharing one `applicationId`.
  Coroutines were deliberately unified at 1.11.0; core-ktx was missed.
- **The dependency chain is the keystone blocker.** AGP 8.11.1, Gradle 8.13,
  Kotlin 2.1.0, KSP 2.1.0-1.0.29, Room 2.6.1, WorkManager 2.9.1, Hilt 2.56.2,
  Retrofit 2.11.0, Media3 1.10.1. Room and WorkManager are pinned back on
  purpose (`app/build.gradle.kts:401-405`). Everything downstream, including the
  Kotlin plugin advisory above, waits on the AGP 8 to 9 migration already
  tracked in `Roadmap_Blocked.md`. There is no version catalog, so roughly sixty
  hard-coded versions are bumped by hand.
- **Test posture is strong but flat.** 125 test files, 675 `@Test` functions,
  3 androidTest files. The JVM discipline (Robolectric, drift guards, pure
  policy objects like `SnoozeCapPolicy`, `FireWatchdogPolicy`,
  `LocationDismissPolicy`) is genuinely good. The uncovered surface is exactly
  the audio cluster: nothing tests the Spotify branch's watchdog outcome, the
  backup-sound job against a null player, or a stream-volume-zero alarm.
- **i18n groundwork is complete and unspent.** 2136 strings, 43 plurals, a
  build-time guard (`verifyLocalizedPrimaryScreens`) covering the whole app
  package with a two-file exemption list, and **zero** `values-*` locales. One
  latent correctness bug in the extraction: `ui/alarmlist/AlarmListScreen.kt:274`
  renders alarm-delete undo using `R.string.timer_undo`, so a translator giving
  that key a timer-specific wording produces wrong text on the alarm screen.
- **README drift was resolved in v1.15.35.** The download example now matches
  a published asset, package differences are explicit, and the feature claims
  reflect the current code. Five current phone captures and a real-screen hero
  replaced the single old screenshot.
- **`Roadmap_Blocked.md` holds items that standing policy forbids ever doing:**
  F-Droid submission (`:292`), developer verification readiness (`:298`),
  reproducible-build badge (`:307`). They should be deleted, not left blocking.

**Coverage disposition.** The roadmap items below cover security, reliability,
observability, offline resilience, i18n, docs, distribution, packaging, data
safety, migration and upgrade strategy. Three categories are deliberately not
carried forward, with reasons. *Accessibility*: the 2026-08-22 pass swept the
secondary screens (named Bedtime switches, checklist state announcements,
breathing-phase and timer-expiry announcements, News tab selection, a Night
Clock exit action, filter-chip selection state, decorative icons no longer read
twice) and what remains needs a physical device with TalkBack, which is already
tracked at `Roadmap_Blocked.md:488`. *Wear OS*: excluded by standing policy; the
existing `:wear` module is maintained as-is. *Multi-user and cloud sync*: the
app is deliberately account-free and local-first, and the two sync shapes that
would fit that stance (paired-phone LAN sync, encrypted paired-phone sync)
already sit at `Roadmap_Blocked.md:67` awaiting a human design decision.

## Rejected Ideas

- **Adopt the Spotify App Remote SDK instead of the intent.** Requires a
  registered Spotify developer app with a client ID and redirect URI baked into
  the build, plus Spotify Premium for playback control. An OSS app cannot ship
  usable credentials, and the current Spotify Developer Policy (effective
  2025-05-15) is tightening third-party access. Fixing the URI form and the
  watchdog gets most of the value at none of the cost. Source: spotify.github.io
  android-sdk, developer.spotify.com/policy.
- **TFLite REM-stage classification from phone accelerometer** (already L-S1 in
  the blocked list; this pass reinforces the rejection). 2025 validation work
  shows phone accelerometry identifies bed and rise times well (86.6% true
  positive, 4.0% false positive) but that even wrist wearables score wake epochs
  at roughly 48-52% specificity against polysomnography. Promising sleep-stage
  accuracy from a phone on a mattress would be a claim the data does not
  support. Source: JMIR Formative Research 2025;9:e67455, SLEEP Advances
  6(2):zpaf021.
- **Ring only when headphones are connected** (BlackyHawky #631). Directly
  contradicts `service/AlarmAudioRouting.shouldForceBuiltInSpeaker`, which exists
  so a headset cannot silently swallow the alarm. Previously rejected, still
  rejected.
- **Power-off guard or accessibility-service uninstall lock.** Coercive and
  contrary to user control. Source: qralarm-android.
- **Ad-gated or subscription-gated snooze extension.** The single clearest
  negative signal in the commercial category. Source: Futurism, 2025-03-23.
- **A plugin SDK / third-party mission API.** No demand signal found in any
  competitor tracker; webhooks plus the `SET_ALARM` / `DISMISS_ALARM` /
  `SNOOZE_ALARM` intent contract already cover the automation cases people
  actually ask for.
- **Anything store-shaped:** F-Droid submission, reproducible-build badges,
  developer verification, listing metadata, Play permission eligibility. Out of
  bounds by standing policy; distribution is GitHub Releases and sideload only.
- **Wear OS feature work.** Same standing policy. The existing `:wear` module is
  maintained as-is; no new Wear items are proposed.
- **Replacing `AlarmManager` with a WorkManager scheduler.** `setAlarmClock()`
  is the correct wake-critical primitive. The watchdog shape already shipped.

## Sources

### Platform, standards, security
- https://developer.android.com/about/versions/17/behavior-changes-all
- https://developer.android.com/about/versions/17/behavior-changes-17
- https://developer.android.com/about/versions/17/changes/bg-audio
- https://developer.android.com/about/versions/16/features/progress-centric-notifications
- https://source.android.com/docs/core/permissions/fsi-limits
- https://developer.android.com/reference/android/app/ApplicationExitInfo
- https://github.com/aosp-mirror/platform_frameworks_base/blob/master/media/java/android/media/AudioPlaybackConfiguration.java
- https://developer.android.com/jetpack/androidx/releases/media3
- https://github.com/advisories/GHSA-pmhh-3w7g-xqp8
- https://github.com/advisories/GHSA-r937-wjx7-w2jp
- https://github.com/advisories/GHSA-7gcm-g887-7qv7
- https://api.osv.dev/v1/querybatch
- https://spotify.github.io/android-sdk/app-remote-lib/
- https://developer.spotify.com/policy/

### Competitors
- https://github.com/FossifyOrg/Clock/issues
- https://github.com/BlackyHawky/Clock/issues/691
- https://github.com/BlackyHawky/Clock/issues/687
- https://github.com/you-apps/ClockYou/issues
- https://github.com/yuriykulikov/AlarmClock/issues/774
- https://github.com/sweakpl/qralarm-android/issues
- https://github.com/vicolo-dev/chrono
- https://github.com/WrichikBasu/ShakeAlarmClock
- https://sleep.urbandroid.org/docs/general/release_notes.html
- https://alternativeto.net/software/free-alarm-clock/?license=opensource&platform=android

### Community and press
- https://www.howtogeek.com/pixel-alarms-not-going-off-problem/
- https://www.androidpolice.com/pixel-alarm-bug-is-back/
- https://piunikaweb.com/2025/12/02/poll-have-you-ever-experienced-the-missing-alarm-bug-on-a-google-pixel/
- https://www.androidauthority.com/google-pixel-missing-alarms-poll-3575639/
- https://www.bgr.com/tech/alarms-arent-working-on-some-pixel-phones-and-nobody-knows-why/
- https://support.google.com/pixelphone/thread/318525822/missed-alarm-alarm-did-not-fire-due-to-an-unknown-reason
- https://www.reddit.com/r/GooglePixel/comments/1jg2nz3/my_alarm_did_not_go_off_today/
- https://www.reddit.com/r/GooglePixel/comments/1jir4tz/all_of_my_alarms_turned_themselves_off_over_the/
- https://futurism.com/alarm-app-advertisement-snooze
- https://www.phonearena.com/news/Past-Pixel-problem-resurfaces-again-creating-chaos-in-users-lives_id172087
- https://dontkillmyapp.com/

### Research
- https://formative.jmir.org/2025/1/e67455
- https://academic.oup.com/sleepadvances/article/6/2/zpaf021/8090472
- https://www.nature.com/articles/s41746-024-01016-9
- https://aasm.org/staying-current-with-actigraphy-devices-for-sleep-wake-monitoring/

### This repo's tracker
- https://github.com/SysAdminDoc/AlarmClockXtreme/issues/53
- https://github.com/SysAdminDoc/AlarmClockXtreme/issues/50
- https://github.com/SysAdminDoc/AlarmClockXtreme/issues/49
- https://github.com/SysAdminDoc/AlarmClockXtreme/issues/44
- https://github.com/SysAdminDoc/AlarmClockXtreme/issues/43

## Open Questions

1. **Should the recommended download stop being the `play` flavor, or should
   `SEND_SMS` move out of the fdroid-only manifest?** Both fix the degraded
   Guardian Angel, and the choice is a product call, not a technical one. The
   roadmap item below assumes moving the permission, which keeps one recommended
   build.
2. **Does `USAGE_MEDIA` survive `AudioPlaybackConfiguration` anonymisation on
   the target devices?** This decides whether the Spotify watchdog needs a new
   predicate or only a baseline-ordering fix. Answerable only on a device with
   Spotify installed; the roadmap item specifies a device check first.
