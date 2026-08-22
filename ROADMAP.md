# AlarmClockXtreme Roadmap

Actionable work only. Historical and completed roadmap material is archived in CHANGELOG.md; blocked work is kept in Roadmap_Blocked.md.

## Actionable Items

## Audit Findings — 2026-08-22

Baseline at audit time: `./gradlew :app:testPlayDebugUnitTest :wear:testDebugUnitTest` = 546 tests, 0 failures (JDK 21 Temurin; the Android Studio JBR is now JDK 25 and Gradle 8.13 refuses it). `:app:lintPlayDebug` = 0 errors, 209 warnings. `scripts/verify_release_metadata.py` passes (v1.15.33 / 135 / Room 23 / backup 17). `gitleaks`: one hit, a fake value in a unit test (not a secret). `grype`: jsoup 1.22.2 (GHSA-pmhh-3w7g-xqp8) transitively via NewPipe, app never parses HTML itself.

Issue tracker intake (read-only): #47 and #48 reproduced on the API 35 emulator and logged below; #49 split into four findings; #50 traced to the Spotify delegation path. Open PRs #33-#42 are Dependabot and are covered by a single blocked item at the end.

### P2 — UX, i18n and visual

- [ ] P2 — Sonar never listens for its own tone
  Category: correctness
  Where: service/SonarSleepService.kt:263-305 (`runReflectionAnalyzer`, `rms`, `variance`)
  Problem: found by the numerical audit on 2026-08-22. The service emits an 18.75 kHz carrier at 1% amplitude and then decides stillness from the variance of the *broadband* RMS of everything the microphone hears. There is no filter anywhere near 18.75 kHz, so the reflected carrier is a rounding error next to a fan, traffic or a partner breathing. What the feature actually measures is how steady the room's loudness is, which is a reasonable proxy for a still room but is not sonar, and the tone contributes almost nothing to it.
  Fix: either run a Goertzel filter at TONE_HZ over each 50 ms window and take the variance of that magnitude, which makes the name true, or stop emitting the tone and rename the feature to what it measures. The first is a few dozen lines and testable against a synthesised buffer.
  Acceptance: with a synthesised window containing loud broadband noise and a steady carrier, the analyser reports still; with a steady room and a modulated carrier, it reports movement. Neither holds today.
  Confidence: Verified
  Effort: M

### P3

- [ ] P3 — Test gaps for the defects above
  Category: testing
  Where: app/src/test/java/com/sysadmindoc/alarmclock/
  Fix: add (1) `AlarmSchedulerExactPermissionTest` (Robolectric sdk 31/33) for the P0; (2) `AppNavigationRouteResolverTest` for the deep-link allowlist; (3) `SnoozeCapPolicyTest` asserting challenge alarms never auto-dismiss; (4) `buildChallenge` Wi-Fi SSID test; (5) `BackupManagerImportConsentTest` (settings skipped unless opted in, size cap); (6) `HueSunriseRampPlanTest`; (7) `TimerStoreBootCountTest`; (8) `CalendarAutoAlarmWorkerTest` for the single-row invariant; (9) a theme contrast test over the accent presets; (10) a `FireWatchdogPolicyTest` early-fire case; (11) an `AlarmFiringViewModel` harness at all. It takes seven injected dependencies and has no test anywhere, which is why the voice and handwriting `ChallengeNoticeTone` transitions added on 2026-08-22 went in unverified: nothing asserts that a failed match sets PROBLEM or that a successful one sets SUCCESS.
  Acceptance: each test fails on the current code and passes after its fix.
  Confidence: Verified
  Effort: M

