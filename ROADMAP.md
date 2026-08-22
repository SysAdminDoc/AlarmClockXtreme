# AlarmClockXtreme Roadmap

Actionable work only. Historical and completed roadmap material is archived in CHANGELOG.md; blocked work is kept in Roadmap_Blocked.md.

## Actionable Items

## Audit Findings — 2026-08-22

Baseline at audit time: `./gradlew :app:testPlayDebugUnitTest :wear:testDebugUnitTest` = 546 tests, 0 failures (JDK 21 Temurin; the Android Studio JBR is now JDK 25 and Gradle 8.13 refuses it). `:app:lintPlayDebug` = 0 errors, 209 warnings. `scripts/verify_release_metadata.py` passes (v1.15.33 / 135 / Room 23 / backup 17). `gitleaks`: one hit, a fake value in a unit test (not a secret). `grype`: jsoup 1.22.2 (GHSA-pmhh-3w7g-xqp8) transitively via NewPipe, app never parses HTML itself.

Issue tracker intake (read-only): #47 and #48 reproduced on the API 35 emulator and logged below; #49 split into four findings; #50 traced to the Spotify delegation path. Open PRs #33-#42 are Dependabot and are covered by a single blocked item at the end.

### P2 — UX, i18n and visual

- [ ] P2 — `TimerAlarmServiceTest` restart case is flaky
  Category: testing
  Where: app/src/test/java/com/sysadmindoc/alarmclock/ui/timer/TimerAlarmServiceTest.kt:164-187 (`restart action creates and schedules exactly one fresh timer without ui`)
  Problem: the case failed once during this session with `expected:<1> but was:<2>` on the scheduled-alarm count at line 181, then passed on an immediate rerun and in isolation. `records.single()` on the line above succeeded, so the store held one timer while the shadow AlarmManager held two. A test that fails one run in ten hides real regressions behind a shrug.
  Evidence: reproduced twice, both times only in a full-suite run. Since then, counting only runs whose assertion could actually have caught the suspected leak: 2 with the original count assertion, then 11 with the current one (the summary assertion landed in 87b4649). The 10-run `--rerun-tasks` soak in between used an intermediate assertion that filtered the shadow to timer alarms, so it would have missed an intruder from a neighbouring class and does not count toward the 20. 13 of 20. Roughly 7 more consecutive green full-suite runs close this out; each item drained adds one.
  Ruled out, do not redo: (a) the `DUPLICATE_WINDOW_MS` theory this item used to carry is wrong, `restartFinished` does not go through `startOrReuse` and has no time window; (b) `TimerPersistence.replace` writing with `apply()` rather than `commit()` is not it either, `apply()` updates the SharedPreferences in-memory map synchronously before it queues the disk write, so read-after-write inside one process is ordered on real Android; (c) `TimerAlarmScheduler.schedule` keys its PendingIntent by `REQUEST_BASE + timerId`, so a repeated schedule for one id replaces rather than adds.
  Fix: the remaining suspect is shadow state surviving between test classes, so instrument it rather than theorise: dump `scheduledAlarms` contents (not just the size) on failure to learn which timer id the second alarm belongs to, and run the suite with a fixed class order to find the neighbour that leaks. Then assert through a fake scheduler instead of the shadow.
  Acceptance: the case passes 20 consecutive full-suite runs.
  Confidence: Needs-repro
  Effort: S

### P3

- [ ] P3 — The round and square launcher icons are byte identical at xhdpi
  Category: visual
  Where: app/src/main/res/mipmap-xhdpi/ic_launcher.png and ic_launcher_round.png (lint `IconDuplicates`); the other four densities differ
  Problem: two ways out and both are decisions rather than cleanups, which is why the 2026-08-22 lint pass left this one alone. Either the xhdpi round icon gets a genuinely round render, which is design work, or every raster mipmap goes: minSdk is 26 and `mipmap-anydpi-v26` supplies adaptive icons, so on any supported device the PNGs are already unreachable. Deleting them shrinks the APK and removes the warning, but it also removes the fallback for any launcher that reads the raster anyway.
  Fix: pick one. If the rasters go, verify a launcher install on an emulator still shows the icon.
  Acceptance: `IconDuplicates` is absent from `:app:lintPlayDebug` and the launcher icon still renders after an install.
  Confidence: Verified
  Effort: S

- [ ] P3 — Test gaps for the defects above
  Category: testing
  Where: app/src/test/java/com/sysadmindoc/alarmclock/
  Fix: add (1) `AlarmSchedulerExactPermissionTest` (Robolectric sdk 31/33) for the P0; (2) `AppNavigationRouteResolverTest` for the deep-link allowlist; (3) `SnoozeCapPolicyTest` asserting challenge alarms never auto-dismiss; (4) `buildChallenge` Wi-Fi SSID test; (5) `BackupManagerImportConsentTest` (settings skipped unless opted in, size cap); (6) `HueSunriseRampPlanTest`; (7) `TimerStoreBootCountTest`; (8) `CalendarAutoAlarmWorkerTest` for the single-row invariant; (9) a theme contrast test over the accent presets; (10) a `FireWatchdogPolicyTest` early-fire case; (11) an `AlarmFiringViewModel` harness at all. It takes seven injected dependencies and has no test anywhere, which is why the voice and handwriting `ChallengeNoticeTone` transitions added on 2026-08-22 went in unverified: nothing asserts that a failed match sets PROBLEM or that a successful one sets SUCCESS.
  Acceptance: each test fails on the current code and passes after its fix.
  Confidence: Verified
  Effort: M

### Unaudited — needs a pass

- [ ] Unaudited — Actigraphy / Sonar sleep analysis math (data/actigraphy/*, service/SonarSleepService.kt DSP): not reviewed for numerical correctness. This one is not device-gated: the DSP is pure Kotlin and can be checked against hand-computed cases on the JVM. The other five audits moved to Roadmap_Blocked.md on 2026-08-22 because each needs hardware or a live network.
