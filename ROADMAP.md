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
  Evidence: one failure in a full-suite run, green on the next full run and on `--tests '*TimerAlarmServiceTest*'`.
  Fix: find the timing dependence. `TimerStore.startOrReuse` coalesces duplicates inside `DUPLICATE_WINDOW_MS`, so the two back-to-back restarts in the test are only deduplicated while the machine is fast enough; inject the clock instead of relying on wall time, and assert on the scheduler through a fake rather than the shadow.
  Acceptance: the case passes 20 consecutive runs of the class.
  Confidence: Needs-repro
  Effort: S

- [ ] P2 — Restoring a backup detaches every alarm from its own history
  Category: correctness
  Where: data/backup/BackupManager.kt:21-40 (`AlarmBackup` has no `id` field); data/backup/AlarmBackupMappers.kt:82-95 (`toAlarmOrNull` builds `Alarm(...)` with the default id 0); consumers keyed by alarm id: data/local/AlarmEventDao.kt:50-60, data/repository/AlarmIncidentRepository.kt, service/AlarmRuntimeState.kt
  Problem: the backup format never stored the alarm id, so every restore inserts fresh rows. `alarm_events`, `alarm_incident_events` and the persisted snooze counts are all keyed by the old ids, so after restoring your own backup on the same device the per-alarm stats panel reads zero fires and adaptive difficulty resets to baseline. The alarms come back; everything the app learned about them does not.
  Evidence: found while checking whether Replace preserved ids — it never could, because the field is absent from the format.
  Fix: add `id` to `AlarmBackup` (bump the backup version with it), carry it through `toAlarmOrNull`, and in Replace mode save over the same row so history stays attached. In Append mode keep allocating new ids and leave the history behind, which is correct there. Add a round-trip test asserting an alarm's events still resolve after a Replace restore.
  Acceptance: record some fires against an alarm, export, Replace-import, and the per-alarm stats panel still shows them.
  Confidence: Verified
  Effort: M

- [ ] P2 — Half of the user-facing strings bypass localisation, so the new language picker has nothing to switch
  Category: ux
  Where: app/build.gradle.kts:195-203 (`verifyLocalizedPrimaryScreens` guards only three files); ~81 literals in ui/alarmfiring/challenges/ChallengeViews.kt (e.g. :122, :165, :486, :694, :1361), ~52 in ui/stats/StatsScreen.kt (:129, :152, :372, :467), ~52 in ui/alarmlist/AlarmListScreen.kt (:1051, :1022, :1033, :1248, :1561), ~40 in ui/dashboard/DashboardScreen.kt, ~37 in ui/bedtime/BedtimeScreen.kt, plus service/AlarmService.kt:2072, service/NextAlarmNotifier.kt:210/249, receiver/BedtimeReceiver.kt:216/296, worker/WakeConfirmWorker.kt:214-254, widget/NextAlarmWidget.kt:243, directboot/DirectBootAlarmService.kt:122/131 (resources `direct_boot_alarm_title`/`_stop` exist but are unused), ui/navigation/AppNavigation.kt:88-93 (tab labels), and the whole wear module (wear strings.xml has 5 entries; NextAlarmTileService.kt:106-242, WearAlarmData.kt:76-130)
  Problem: res/xml/locales_config.xml declares only `en` and util/AppLanguageManager.kt offers only English, so the Android 13 language picker is a two-option no-op; any future translation would leave the firing challenges, stats, list, notifications and watch tile in English.
  Fix: move the literals above to strings.xml (use the existing keys where they already exist), add plurals for `"${n} result${if (n == 1) "" else "s"}"` (RingtonePickerSheet.kt:321) and the degenerate plurals at strings.xml:396/397/1129/1166/1167; extend `primaryComposeScreenFiles` in build.gradle.kts to every `ui/**/*.kt` plus service/receiver/worker/widget notification builders; add the wear module to the guard.
  Acceptance: `./gradlew verifyLocalizedPrimaryScreens` fails on a new `Text("…")` literal anywhere under ui/; a pseudo-locale build (`en-XA`) shows accented text on every screen and notification.
  Confidence: Verified
  Effort: L

- [ ] P2 — A YouTube download is cancelled by a rotation
  Category: ux
  Where: ui/components/YouTubeDownloadDialog.kt:345-360 and :380-395 (`scope.launch` on a `rememberCoroutineScope`, cancelled when the composition is destroyed); ui/ringtone/RingtonePickerSheet.kt:126 (the parent flag now survives rotation, so the dialog itself reopens)
  Problem: the mode, pasted URL, typed query, search results and dialog visibility all survive a rotation now, but a download in flight does not: it runs on the composition's scope, so turning the phone kills it partway with no message. The user sees the dialog reopen with the fields intact and nothing downloading.
  Evidence: the rest of that finding shipped; this piece needs the work hoisted out of composition.
  Fix: move the download (and the engine update, which has the same shape) into a `@HiltViewModel` owned by the ringtone picker, exposing progress and a terminal result as state. The dialog then observes rather than launches.
  Acceptance: start a download, rotate: it keeps going and still reports success or failure into the picker.
  Confidence: Verified
  Effort: M

### P3

- [ ] P3 — NFC / barcode / photo / Wi-Fi challenges silently accept anything when no reference is saved
  Category: ux
  Where: ui/alarmfiring/AlarmFiringViewModel.kt:650-656, :669-672 (blank reference → `proceedToNextChallenge()`); ui/alarmfiring/challenges/ChallengeViews.kt:1050, :1204, :1514 ("Any … will work for now")
  Problem: the editor warns (domain/ChallengeReadiness.kt) but backup/share imports can still land an NFC/barcode/photo challenge with no reference; at fire time the challenge is a no-op with a small grey hint.
  Fix: when the reference is blank at fire time, substitute MATH_MEDIUM (or the next chain step) and show "No tag registered, solving a math problem instead" so the user learns to fix the alarm.
  Acceptance: an imported NFC alarm with no tag id rings with a math challenge.
  Confidence: Verified
  Effort: S

- [ ] P3 — Wake-confirm notification id band (500000 + alarmId) is unbounded and can enter the Hue band (800000-899999)
  Category: maintainability
  Where: worker/WakeConfirmWorker.kt:52 (`NOTIF_ID_BASE + alarmId`, no modulo); worker/HueSunriseNotifications.kt:22-23 (clamped); CLAUDE.md notification-ID table (missing SnoozeCountdown=3003, DirectBoot=1011, OnboardingTestAlarm=1907, Hue=800000+); `alarm.id + 30000` reused by NextAlarmNotifier.kt:157 (broadcast) and MissedAlarmUnlockReceiver.kt:182 (activity)
  Fix: clamp wake-confirm to `500000 + (alarmId % 100000)`, re-band the MissedAlarmUnlockReceiver activity code to its own base, and update the CLAUDE.md table.
  Acceptance: a unit test asserts every notification/request-code band is disjoint for alarmId in 0..1_000_000.
  Confidence: Verified
  Effort: S

- [ ] P3 — `MissedAlarmUnlockReceiver` posts on the live AlarmService notification id without a comment
  Category: maintainability
  Where: receiver/MissedAlarmUnlockReceiver.kt:218 (`NOTIFICATION_ID` 1001)
  Fix: add a comment explaining the intentional replacement, or use a dedicated id so a ringing alarm's foreground notification cannot be overwritten.
  Acceptance: comment present or separate id with a test.
  Confidence: Verified
  Effort: S

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

- [ ] P3 — World clock per-city action uses the overflow glyph for a single "Remove" action
  Category: ux
  Where: ui/worldclock/WorldClockScreen.kt:257-263
  Fix: use `Icons.Default.Delete` (or a real menu) and keep the existing confirmation/undo.
  Acceptance: the icon matches the action.
  Confidence: Verified
  Effort: S

- [ ] P3 — Stats "Clear history" is not guarded against repository exceptions
  Category: reliability
  Where: ui/stats/StatsViewModel.kt:96-102 (`viewModelScope.launch { eventRepository.clearHistory() }` without try/catch; a Room exception crashes the process); no success feedback
  Fix: wrap in `runCatching`, emit a snackbar ("History cleared" / "Couldn't clear history").
  Acceptance: an injected DAO exception shows the error snackbar instead of crashing.
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

- [ ] P3 — SmartAlarmService wake lock is reference-counted across overlapping windows
  Category: reliability
  Where: service/SmartAlarmService.kt:85, :121 (acquire per `onStartCommand`), :277 (single release in `onDestroy`), :125-130 (first session dropped silently)
  Fix: `wakeLock.setReferenceCounted(false)`; log when a second window pre-empts the first.
  Acceptance: two overlapping smart windows leave no held wake lock after `onDestroy` (`adb shell dumpsys power | grep acx`).
  Confidence: Verified
  Effort: S

- [ ] P3 — Crash-log scrubber misses bare hostnames / IPs; yt engine failure reason is exported unscrubbed
  Category: security
  Where: data/support/SupportDiagnosticsFormatter.kt:62-72 (`CrashLogScrubber` patterns), :415-416 (`ytEngineLastFailureReason` printed verbatim); util/CrashLogger.kt:67 (raw stack trace stored)
  Fix: add IPv4 and `Unable to resolve host "…"` patterns, scrub at write time in `CrashLogger.writeLog`, wrap the yt reason in `scrub()`.
  Acceptance: a synthetic `UnknownHostException: Unable to resolve host "radio.example.com"` appears redacted in the support bundle.
  Confidence: Verified
  Effort: S

- [ ] P3 — Hue host field accepts public hostnames while TOFU trust-on-first-use is active; Hue key shown in clear text
  Category: security
  Where: integration/hue/HueBridgeClient.kt:158-162 (host regex), :112 and :186-190 (hostname verifier and trust manager with a blank pin); ui/settings/SettingsIntegrationSections.kt:417-423 (no `PasswordVisualTransformation`, unlike the webhook secret at :201-209); worker/WebhookRetryWorker.kt:96 (label persisted in WorkManager input even when `includeLabel=false`)
  Fix: require a literal IP or `.local` host via `LocalNetworkPermission.isLikelyLocalHost`; password-mask the Hue key; store an empty label when `includeLabel` is false.
  Acceptance: "bridge.example.com" is rejected with an inline error; the Hue key field shows dots.
  Confidence: Verified
  Effort: S

- [ ] P3 — Backup-imported `content://`/`file://` URIs have no scheme allowlist
  Category: security
  Where: data/model/Alarm.kt:289, :305, :342 (`trim().take(2048)` only); consumers service/AlarmService.kt:914-915, ui/alarmfiring/AlarmFiringScreen.kt:257-264, ui/alarmfiring/AlarmFiringActivity.kt:85; data/share/AlarmShareCodec.kt:96-130 (share path already strips)
  Fix: apply the same strip (or an allowlist: `content://media`, `android.resource://`, app-private `file://`) to backup-imported alarms unless the user opts to keep them in the preview.
  Acceptance: a backup with `content://com.other.app/…` ringtone imports with the default tone and a preview warning.
  Confidence: Needs-repro
  Effort: S

- [ ] P3 — `AlarmClockIntentActivity` can dismiss or disable alarms by id, and create alarms silently, for any app holding SET_ALARM
  Category: security
  Where: platform/AlarmClockIntentParser.kt:132-142, :250-262; platform/AlarmClockIntentHandler.kt:56-86, :119-177
  Problem: `DISMISS_ALARM` with `ById`/`All` or `acx://alarm/<id>` needs no confirmation, and `SET_ALARM` with `EXTRA_SKIP_UI` creates an enabled alarm with no visible trace; unparcelling attacker extras is not wrapped in try/catch. This is the platform contract, but it is gated only by a normal permission.
  Fix: route `ById`/`All` through the selection UI unless the alarm is currently firing; post a notification "Alarm added by <caller>" on the skip-UI path; wrap `handler.handle(request)` in `runCatching` and treat failure as `Invalid`.
  Acceptance: an external DISMISS_ALARM by id opens the Alarms list for confirmation; a malformed extras bundle does not crash the proxy activity.
  Confidence: Verified (platform contract; severity low)
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
