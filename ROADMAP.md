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
  Evidence: reproduced twice, both times only in a full-suite run. Since then: 5 consecutive green runs of the class in isolation and 2 consecutive green full-suite runs, so it is rarer than one in ten and ordering-dependent.
  Ruled out, do not redo: (a) the `DUPLICATE_WINDOW_MS` theory this item used to carry is wrong, `restartFinished` does not go through `startOrReuse` and has no time window; (b) `TimerPersistence.replace` writing with `apply()` rather than `commit()` is not it either, `apply()` updates the SharedPreferences in-memory map synchronously before it queues the disk write, so read-after-write inside one process is ordered on real Android; (c) `TimerAlarmScheduler.schedule` keys its PendingIntent by `REQUEST_BASE + timerId`, so a repeated schedule for one id replaces rather than adds.
  Fix: the remaining suspect is shadow state surviving between test classes, so instrument it rather than theorise: dump `scheduledAlarms` contents (not just the size) on failure to learn which timer id the second alarm belongs to, and run the suite with a fixed class order to find the neighbour that leaks. Then assert through a fake scheduler instead of the shadow.
  Acceptance: the case passes 20 consecutive full-suite runs.
  Confidence: Needs-repro
  Effort: S

### P3

- [ ] P3 — Dead code and duplicated helpers
  Category: maintainability
  Where: ui/components/AppComponents.kt:813-814 (`AppCardBorderColor`, comment claims callers that do not exist); ui/settings/SettingsReadinessSections.kt:501-617 (seven private composables/helpers with zero callers: `standbyBucketDescription`, `testAlarmProofStatusLabel`, `testAlarmProofDescription`, `guardianReadinessDescription`, `guardianReadinessStatusLabel`, `guardianReadinessActionLabel`, `WakeReadinessRow`) plus 27 unused string resources (lint `UnusedResources`, strings.xml:709-878, mostly the same readiness block); ui/timer/TimerViewModel.kt `toggleGradualVolume/toggleKeepScreenOn/toggleOverrideVolume/toggleVibration`; ui/settings/SettingsViewModel.kt `requestDndAccess/requestExactAlarmAccess/requestFullScreenAlarmAccess/updateSleepSoundFade/updateSleepSoundTimer`; ui/alarmlist/AlarmListViewModel.kt `selectAll`; ui/alarmedit/AlarmEditViewModel.kt `addRingtoneToPool/removeRingtoneFromPool`; ui/alarmedit/AlarmEditSupport.kt `CollapsibleGroup`; util/CrashLogger.kt `getLogs/clearLogs`; service/YouTubeAudioDownloader.kt `isEngineOutdated`; ui/timer/TimerScreen.kt:368-383 `TimeUnit`; duplicated 12h/24h time formatting in seven places (service/AlarmService.kt:1917, ui/alarmlist/AlarmListScreen.kt:1565, service/NextAlarmNotifier.kt:164, directboot/DirectBootAlarmCache.kt:255, ui/alarmfiring/AlarmFiringScreen.kt:267, ui/alarmedit/AlarmEditScheduleSections.kt:95, service/BedtimeZenRuleManager.kt:387) with mixed `Locale.US`/default locale; three parallel challenge-label tables (AlarmListScreen.kt:1575, TemplatePickerSheet.kt:245, AlarmEditSupport.kt:863/903/995); `BuildConfig.USE_MEDIA3_ALARM_PLAYER` is `true` for every variant so `startMediaPlayerAudioInternal` (AlarmService.kt:1245-1467) is unreachable in shipped builds
  Fix: delete the dead symbols and resources; introduce `util/AlarmTimeFormatter.format(hour, minute, is24h, locale)` and one localised `ChallengeType.label()`; decide whether to keep the MediaPlayer path (if kept, make the flag a real build-type switch; if not, delete it and the `MEDIA_PLAYER` mapper test).
  Acceptance: lint `UnusedResources` = 0; a single time formatter is referenced from all seven sites.
  Confidence: Verified
  Effort: M

- [ ] P3 — Lint hygiene: 19 `DefaultLocale`, 4 `ApplySharedPref` (`commit()` on the main thread in AlarmService.kt:557, :2048, :2055 and MissedAlarmUnlockReceiver.kt:84), 7 `PluralsCandidate` (strings.xml:386-388, :847, :994, :1133, :1136), `SwitchIntDef` (AlarmService.kt:1189 missing `STATE_BUFFERING`/`STATE_IDLE`), `IconDuplicates` (mipmap-xhdpi ic_launcher == ic_launcher_round), `Overdraw` (layout/widget_loading.xml:6)
  Category: maintainability
  Fix: pass `Locale.getDefault()` explicitly (or use the shared formatter above), replace `commit()` with `apply()` where durability is not needed (keep `commit()` only for the missed-alarm marker and document why), convert the seven candidates to `<plurals>`, handle the two Media3 states, dedupe the icon, drop the hardcoded widget background.
  Acceptance: `./gradlew :app:lintPlayDebug` warning count drops below 150 with no new baseline file.
  Confidence: Verified
  Effort: S

- [ ] P3 — Shared-alarm import and template picker ignore the 24-hour preference
  Category: ux
  Where: ui/share/SharedAlarmImportScreen.kt:328 (`%02d:%02d` in `Locale.US`); ui/templates/TemplatePickerSheet.kt:216-222 (always AM/PM); data/model/AlarmTemplate.kt:51 ("20 min timer" for an alarm template)
  Fix: use the shared formatter with `settings.is24HourFormat`; fix the template description.
  Acceptance: with 24-hour on, both screens show "06:30".
  Confidence: Verified
  Effort: S

- [ ] P3 — BedtimeReceiver samples the microphone from a background broadcast and persists a false "quiet room" baseline
  Category: correctness
  Where: receiver/BedtimeReceiver.kt:211-213 → service/BedtimeNoiseBaselineSampler.kt:89-108
  Problem: on API 30+ a background app receives silence from `AudioRecord`; the sampler stores RMS 0 with a fresh timestamp every bedtime.
  Fix: sample only from the Bedtime screen (foreground) or from the Sonar foreground service; skip when `RECORD_AUDIO` is not held.
  Acceptance: the stored baseline changes only after an in-app measurement.
  Confidence: Likely
  Effort: S

- [ ] P3 — Wear tile renders the stock ProtoLayout palette and joins text with spaced hyphens
  Category: visual
  Where: wear/src/main/java/.../NextAlarmTileService.kt:91 (`allowDynamicTheme = false`, no custom `ColorScheme`); wear/.../WearAlarmData.kt:93, :129 (`" - "`)
  Fix: supply a `ColorScheme` built from the app tokens; use "·" as the separator.
  Acceptance: tile background/accent match the phone app on a Wear emulator.
  Confidence: Verified
  Effort: S

- [ ] P3 — Night Clock exit hint is below 3:1 contrast and hardcoded colours live outside ui/theme
  Category: a11y
  Where: ui/nightclock/NightClockActivity.kt:146-147, :216, :247, :251 (`Color(0xFF02060D)`, `Color.Black`, `Color.White.copy(alpha = 0.04f)`, `TextMuted.copy(alpha = 0.58f)` ≈ 2.7:1)
  Fix: keep the dimmed clock but render the exit hint at ≥3:1 (or fade it in on touch), and move the literals into Color.kt as named night tokens.
  Acceptance: "Long press anywhere to exit" measures ≥3:1 on the night background.
  Confidence: Verified
  Effort: S

- [ ] P3 — Room schema exports for versions 1, 2, 3 and 7 are missing, so migrations from those versions cannot be tested
  Category: testing
  Where: app/schemas/com.sysadmindoc.alarmclock.data.local.AlarmDatabase/ (4-6, 8-23 present); app/src/androidTest/.../AlarmDatabaseMigrationTest.kt:31 (starts at version 4)
  Fix: reconstruct 1.json/2.json/3.json/7.json from the migration SQL (or from git history of the entity) and extend the migration test to start at 1.
  Acceptance: `runMigrationsAndValidate` from 1 and from 7 to 23 passes.
  Confidence: Needs-repro
  Effort: S

- [ ] P3 — Test gaps for the defects above
  Category: testing
  Where: app/src/test/java/com/sysadmindoc/alarmclock/
  Fix: add (1) `AlarmSchedulerExactPermissionTest` (Robolectric sdk 31/33) for the P0; (2) `AppNavigationRouteResolverTest` for the deep-link allowlist; (3) `SnoozeCapPolicyTest` asserting challenge alarms never auto-dismiss; (4) `buildChallenge` Wi-Fi SSID test; (5) `BackupManagerImportConsentTest` (settings skipped unless opted in, size cap); (6) `HueSunriseRampPlanTest`; (7) `TimerStoreBootCountTest`; (8) `CalendarAutoAlarmWorkerTest` for the single-row invariant; (9) a theme contrast test over the accent presets; (10) a `FireWatchdogPolicyTest` early-fire case.
  Acceptance: each test fails on the current code and passes after its fix.
  Confidence: Verified
  Effort: M

- [ ] P3 — Documentation drift
  Category: docs
  Where: CHANGELOG.md:5-69 ("Unreleased" lists work that shipped in 1.15.33, e.g. the snooze-until picker, OEM doctor, language picker; the 1.15.33 entry at :70 only has a short "Changed" block); README.md (7 em dashes and 10 spaced-hyphen dashes in prose, against the house style); RESEARCH.md:6 (v1.15.30/132 and "30+" challenges; the enum has exactly 30 plus NONE); PROJECT_CONTEXT.md:15-18, :164 (v1.14.16 / Room 15 / backup v11, untracked but read by agents); Roadmap_Blocked.md (release.yml entry, see the Dependabot item; the two backup items, see the Replace-mode item); CLAUDE.md:8 (says v1.15.32), build command uses the Studio JBR which is now JDK 25 and fails Gradle 8.13 (use the Temurin 21 install), CLAUDE.md:272 claims a build-flagged MediaPlayer path that no variant enables
  Fix: move the Unreleased bullets under 1.15.33, rewrite README prose without dashes, refresh RESEARCH.md/PROJECT_CONTEXT.md version lines, update CLAUDE.md build command to `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"` and the notification-id table.
  Acceptance: `python scripts/verify_release_metadata.py` still passes and `grep -c "—" README.md` = 0.
  Confidence: Verified
  Effort: S

### Unaudited — needs a pass

- [ ] Unaudited — Direct Boot fallback (directboot/*) end-to-end on a device with file-based encryption: only the cache logic was read, not exercised.
- [ ] Unaudited — Wear module runtime behaviour (tile, complication, Data Layer round trip) on a Wear emulator; only source was read.
- [ ] Unaudited — Health Connect, YouTube engine update, and NewPipe search paths with live network; code-traced only.
- [ ] Unaudited — Actigraphy / Sonar sleep analysis math (data/actigraphy/*, service/SonarSleepService.kt DSP): not reviewed for numerical correctness.
- [ ] Unaudited — Tablet/foldable (NavigationRail, 840 dp list/detail) layouts: adaptive code was read, no wide-window emulator run.
- [ ] Unaudited — Android 12/12L device run (no API 31/32 AVD on this machine); the P0 above is from code and platform semantics.
