# AlarmClockXtreme Roadmap

Actionable work only. Historical and completed roadmap material is archived in CHANGELOG.md; blocked work is kept in Roadmap_Blocked.md.

## Actionable Items

## Audit Findings — 2026-08-22

Baseline at audit time: `./gradlew :app:testPlayDebugUnitTest :wear:testDebugUnitTest` = 546 tests, 0 failures (JDK 21 Temurin; the Android Studio JBR is now JDK 25 and Gradle 8.13 refuses it). `:app:lintPlayDebug` = 0 errors, 209 warnings. `scripts/verify_release_metadata.py` passes (v1.15.33 / 135 / Room 23 / backup 17). `gitleaks`: one hit, a fake value in a unit test (not a secret). `grype`: jsoup 1.22.2 (GHSA-pmhh-3w7g-xqp8) transitively via NewPipe, app never parses HTML itself.

Issue tracker intake (read-only): #47 and #48 reproduced on the API 35 emulator and logged below; #49 split into four findings; #50 traced to the Spotify delegation path. Open PRs #33-#42 are Dependabot and are covered by a single blocked item at the end.

### P2 — UX, i18n and visual

- [ ] P2 — Wire the sonar carrier detector into the stillness decision, and raise the carrier
  Category: correctness
  Where: service/SonarSleepService.kt:263-305 (`runReflectionAnalyzer`); service/SonarCarrier.kt
  Problem: the service emits an 18.75 kHz carrier and then decides stillness from the variance of the *broadband* RMS of everything the microphone hears. There is no filter near the carrier, so a fan or traffic dwarfs a tone emitted at 1% amplitude. What it measures is how steady the room's loudness is; the tone contributes almost nothing.
  Done 2026-08-22: `SonarCarrier` is the Goertzel filter that reads the carrier bin, tested and measured. Its analysis length is 2058 samples because that is the largest multiple of 294 inside the 50 ms read, which puts the carrier exactly on bin 875 with no leakage.
  What the measurements say, and why it is not wired in yet. Against a synthesised loud room (broadband RMS 0.42), the carrier bin reads about 0.01, so the filter rejects the room by a factor of roughly 40. But the emitter uses 1% amplitude, which reads 0.01 as well: in a loud room the carrier is the same size as the room's own energy in that bin. A 5% carrier reads cleanly through it, a 10% one more so. So the wiring needs two things this machine cannot supply: a carrier amplitude chosen against real rooms, and a stillness threshold calibrated for carrier magnitude rather than the current broadband RMS variance constant (`DEEP_SLEEP_THRESHOLD = 0.004f`).
  Fix: raise the carrier until it clears a real room's noise floor, keeping it inaudible, then switch the stillness variance onto `SonarCarrier.magnitude` and recalibrate the threshold from a night of recorded data. Keep the broadband RMS for snore detection, which genuinely wants loudness.
  Acceptance: on a real overnight recording, stillness tracks movement rather than room noise, with the two separable.
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

