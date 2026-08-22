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
  Evidence: reproduced twice, both times only in a full-suite run. Since then: 5 consecutive green runs of the class in isolation, and 12 consecutive green full-suite runs (2 earlier, plus a 10-run `--rerun-tasks` soak on 2026-08-22), so it is rarer than one in ten and ordering-dependent. 8 more consecutive green full-suite runs close this out.
  Ruled out, do not redo: (a) the `DUPLICATE_WINDOW_MS` theory this item used to carry is wrong, `restartFinished` does not go through `startOrReuse` and has no time window; (b) `TimerPersistence.replace` writing with `apply()` rather than `commit()` is not it either, `apply()` updates the SharedPreferences in-memory map synchronously before it queues the disk write, so read-after-write inside one process is ordered on real Android; (c) `TimerAlarmScheduler.schedule` keys its PendingIntent by `REQUEST_BASE + timerId`, so a repeated schedule for one id replaces rather than adds.
  Fix: the remaining suspect is shadow state surviving between test classes, so instrument it rather than theorise: dump `scheduledAlarms` contents (not just the size) on failure to learn which timer id the second alarm belongs to, and run the suite with a fixed class order to find the neighbour that leaks. Then assert through a fake scheduler instead of the shadow.
  Acceptance: the case passes 20 consecutive full-suite runs.
  Confidence: Needs-repro
  Effort: S

- [ ] P2 — The localisation guard does not match `return "literal"`, so user-facing English survives behind it
  Category: i18n
  Where: ui/alarmlist/AlarmListScreen.kt:1727-1739 (`nextOccurrenceLabel` returns "All alarms paused until X", "Paused until you re-enable this alarm", "Next occurrence: X"); ui/bedtime/BedtimeViewModel.kt:883, :892-894, :900 ("No baseline", "Checks at reminder", "Mic permission needed", "Last X; no audio saved"); service/YouTubeAudioDownloader.kt:38-45 (`YouTubeEngineUpdateResult.userMessage()`, shown by YouTubeDownloadViewModel as the engine-update status); receiver/BedtimeReceiver.kt:215 ("Time to wind down") and service/AlarmService.kt:2176 ("$label at $timeStr was auto-silenced after $autoSilenceMinutes minutes"), both outside the guard's `ui/` tree entirely; wear/ (every runtime string is English in the source; the module's res/values/strings.xml exists but holds only the five manifest labels)
  Also: ui/alarmfiring/AlarmFiringViewModel.kt:597, :603, :623, :642, :659 assign English voice and handwriting status sentences, and ui/alarmfiring/challenges/ChallengeViews.kt:712, :934-935 pick the notice colour by `startsWith("Heard")` / `startsWith("No phrase")` / `endsWith("matched.")` / `startsWith("Checking")`. Those two have to move together: localising the sentences without replacing the colour rule turns every challenge notice the wrong colour.
  Problem: the guard's branch-literal patterns match `->`, `else`, `?` and `if (...)` heads but not a bare `return "literal"`, so these survived the extraction. `userMessage()` also builds the string inside a data class with no Context, so it needs to return a `@StringRes` id plus arguments the way ChallengeReadiness does.
  Fix: add `return` to the guard's branch-literal patterns first so the gap cannot reopen, then extract what it catches, and widen the guard's tree past `ui/` so receiver/ and service/ are covered.
  Acceptance: `:app:verifyLocalizedPrimaryScreens` passes with the `return` pattern added, and no `stringResource`-free English reaches the download dialog or an alarm card.
  Progress 2026-08-22: the `return` pattern, a notification-builder setter pattern and a URL exclusion are in, the scanned tree covers the whole app package rather than `ui/`, and roughly 90 strings are extracted (bedtime, stats, alarm list, challenge statuses, every notification channel and body, the workers and the widget). The alarm-list, bedtime and challenge sites named above are done. Two named in the acceptance are not: `Alarm.repeatLabel` still hands "Once"/"Every day"/"Weekdays" to the alarm card, and `YouTubeEngineUpdateResult.userMessage()` still hands English to the download dialog. Sixteen files remain on `unlocalizedComposeFiles` in app/build.gradle.kts, and that list is the live task list for this item.
  Confidence: Verified
  Effort: M

- [ ] P2 — Eight more screens format the time as 12-hour regardless of the setting
  Category: ux
  Where: data/repository/CalendarRepository.kt:28, :35; ui/alarmlist/AlarmListViewModel.kt:578, :590, :599; ui/dashboard/DashboardViewModel.kt:584; ui/stats/StatsScreen.kt:1266; service/SkipNextAlarmTileService.kt:100 (`"EEE h:mm a"`)
  Problem: each hardcodes `DateTimeFormatter.ofPattern("h:mm a")` with no `is24HourFormat` branch, so a 24-hour phone still sees "6:30 AM" on the calendar rows, the alarm-list snackbars, the stats detail and the quick-settings skip tile. Separate from the shared-alarm/template item below, which has the flag in scope already; these sites do not and need it plumbed from settings (the tile can read `DateFormat.is24HourFormat`).
  Fix: route through `util/AlarmTimeFormatter` and pass the preference in from the caller that already reads settings.
  Acceptance: with 24-hour on, none of the eight sites renders an AM/PM suffix.
  Confidence: Verified
  Effort: S

### P3

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
  Fix: add (1) `AlarmSchedulerExactPermissionTest` (Robolectric sdk 31/33) for the P0; (2) `AppNavigationRouteResolverTest` for the deep-link allowlist; (3) `SnoozeCapPolicyTest` asserting challenge alarms never auto-dismiss; (4) `buildChallenge` Wi-Fi SSID test; (5) `BackupManagerImportConsentTest` (settings skipped unless opted in, size cap); (6) `HueSunriseRampPlanTest`; (7) `TimerStoreBootCountTest`; (8) `CalendarAutoAlarmWorkerTest` for the single-row invariant; (9) a theme contrast test over the accent presets; (10) a `FireWatchdogPolicyTest` early-fire case; (11) an `AlarmFiringViewModel` harness at all. It takes seven injected dependencies and has no test anywhere, which is why the voice and handwriting `ChallengeNoticeTone` transitions added on 2026-08-22 went in unverified: nothing asserts that a failed match sets PROBLEM or that a successful one sets SUCCESS.
  Acceptance: each test fails on the current code and passes after its fix.
  Confidence: Verified
  Effort: M

- [ ] P3 — Documentation drift
  Category: docs
  Where: CHANGELOG.md:5-69 ("Unreleased" lists work that shipped in 1.15.33, e.g. the snooze-until picker, OEM doctor, language picker; the 1.15.33 entry at :70 only has a short "Changed" block); README.md (7 em dashes and 10 spaced-hyphen dashes in prose, against the house style); RESEARCH.md:6 (v1.15.30/132 and "30+" challenges; the enum has exactly 30 plus NONE); PROJECT_CONTEXT.md:15-18, :164 (v1.14.16 / Room 15 / backup v11, untracked but read by agents); Roadmap_Blocked.md (release.yml entry, see the Dependabot item; the two backup items, see the Replace-mode item); CLAUDE.md:8 (says v1.15.32), build command uses the Studio JBR which is now JDK 25 and fails Gradle 8.13 (use the Temurin 21 install), CLAUDE.md:272 claims a build-flagged MediaPlayer path that no variant enables
  Fix: move the Unreleased bullets under 1.15.33, rewrite README prose without dashes, refresh RESEARCH.md/PROJECT_CONTEXT.md version lines, update CLAUDE.md build command to `JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"` and the notification-id table.
  Acceptance: `python scripts/verify_release_metadata.py` still passes and `grep -c "—" README.md` = 0.
  Remaining after the 2026-08-22 pass: only the CLAUDE.md MediaPlayer sentences (:198, :272, :468), which depend on the dead-code item's decision about that backend, and the notification-id table. Everything else in this item is done. The one backup entry left in Roadmap_Blocked.md is the cloud-backup one, which is genuinely blocked; the second one this item expected was already gone.
  Confidence: Verified
  Effort: S

- [ ] P3 — CHANGELOG prose still carries 384 em dashes
  Category: docs
  Where: CHANGELOG.md, mostly in released sections from 1.15.28 and earlier
  Problem: the house style bans em dashes in anything a human reads outside this machine, and the changelog ships with the repo. The 2026-08-22 docs pass fixed README and RESEARCH but left these: rewriting 384 sentences is a mechanical edit that still changes meaning in 384 places, and doing it unreviewed inside a larger commit would bury any damage.
  Fix: rewrite them in one commit of its own, released section by released section, reading each sentence rather than substituting a character.
  Acceptance: `grep -c "—" CHANGELOG.md` = 0 and no bullet changed meaning.
  Confidence: Verified
  Effort: M

### Unaudited — needs a pass

- [ ] Unaudited — Actigraphy / Sonar sleep analysis math (data/actigraphy/*, service/SonarSleepService.kt DSP): not reviewed for numerical correctness. This one is not device-gated: the DSP is pure Kotlin and can be checked against hand-computed cases on the JVM. The other five audits moved to Roadmap_Blocked.md on 2026-08-22 because each needs hardware or a live network.
