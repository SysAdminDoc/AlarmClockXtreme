# AlarmClockXtreme Roadmap

Actionable work only. Historical and completed roadmap material is archived in CHANGELOG.md; blocked work is kept in Roadmap_Blocked.md.

## Actionable Items

## Audit Findings — 2026-08-22

Baseline at audit time: `./gradlew :app:testPlayDebugUnitTest :wear:testDebugUnitTest` = 546 tests, 0 failures (JDK 21 Temurin; the Android Studio JBR is now JDK 25 and Gradle 8.13 refuses it). `:app:lintPlayDebug` = 0 errors, 209 warnings. `scripts/verify_release_metadata.py` passes (v1.15.33 / 135 / Room 23 / backup 17). `gitleaks`: one hit, a fake value in a unit test (not a secret). `grype`: jsoup 1.22.2 (GHSA-pmhh-3w7g-xqp8) transitively via NewPipe, app never parses HTML itself.

Issue tracker intake (read-only): #47 and #48 reproduced on the API 35 emulator and logged below; #49 split into four findings; #50 traced to the Spotify delegation path. Open PRs #33-#42 are Dependabot and are covered by a single blocked item at the end.

### P2 — correctness and reliability

- [ ] P2 — "Early dismiss window" dropdown does nothing
  Category: correctness
  Where: ui/alarmedit/AlarmEditAdvancedSection.kt:259-275 (0/15/30/60 picker), res/values/strings.xml:653 hint; service/NextAlarmNotifier.kt:151-157, :220 (Skip action added unconditionally)
  Problem: `earlyDismissMinutes` has no consumer outside the DB migration; the upcoming-alarm notification always offers Skip, so the setting is a no-op.
  Evidence: `grep -rni earlydismiss app/src/main/java` outside alarmedit/backup/model returns only AlarmDatabase.kt:116.
  Fix: in `NextAlarmNotifier.showNotification` add the Skip action only when `alarm.earlyDismissMinutes == 0 || nextTriggerTime - now <= earlyDismissMinutes * 60_000L`, and schedule a notification refresh at the window boundary (the notifier already has a refresh loop). Or remove the row and the column. Test `NextAlarmNotifierTest` for both branches.
  Acceptance: with 15 min selected, the Skip action appears on the notification only inside the last 15 minutes.
  Confidence: Verified
  Effort: S

- [ ] P2 — Calendar auto-alarm can create duplicate auto-alarm rows and never asks for READ_CALENDAR
  Category: reliability
  Where: worker/CalendarAutoAlarmWorker.kt:81-106 (periodic `WORK_NAME` with UPDATE policy and one-shot `REFRESH_WORK_NAME` run the same `doWork()` concurrently), :122-123 (silent `Result.success()` without permission), :135 (unbounded `Result.retry()`), :176-186 (`findExistingAutoAlarm` scan then `save(id = 0)`); AlarmClockApp.kt:123-131 and ui/settings/SettingsViewModel.kt:644-650 (both enqueue back-to-back); ui/dashboard/DashboardScreen.kt:881-887 (calendar-permission row has no click handler); ui/settings/SettingsScreen.kt:569-577 (toggle never checks permission)
  Problem: enabling the feature starts two workers at once; both see "no auto alarm yet" and both insert, producing two enabled `calendar_auto` alarms. Separately, no screen ever requests READ_CALENDAR, so the toggle can be ON and silently inert, and the dashboard row telling the user to grant access cannot be tapped.
  Evidence: code trace above; no `RequestPermission(READ_CALENDAR)` launcher exists in ui/ (grep).
  Fix: give the refresh the same unique name with `ExistingWorkPolicy.REPLACE`, guard `doWork` with a process-wide `Mutex`, bound retries with `runAttemptCount`; add a `rememberLauncherForActivityResult(RequestPermission())` for READ_CALENDAR on the Settings toggle and make the dashboard row tappable; show the existing inline-notice pattern (SettingsReadinessSections.kt:473-491) when enabled without permission.
  Acceptance: toggling the feature on a fresh install prompts for calendar access and creates exactly one `calendar_auto` alarm; `alarms` table never contains two rows with `profileName == "calendar_auto"`.
  Confidence: Likely
  Effort: S

- [ ] P2 — Timer store has no reboot guard: phantom timers ring or show days remaining after a reboot
  Category: reliability
  Where: ui/timer/TimerPersistence.kt:23-31, :69-90, :92-122 (records compared against `elapsedRealtime()` with no boot identity); receiver/BootReceiver.kt:79-80 (the only cleanup, on BOOT_COMPLETED/MY_PACKAGE_REPLACED); ui/timer/TimerViewModel.kt:318-341 `restorePersistedTimers()`; compare ui/stopwatch/StopwatchViewModel.kt:192-207 (uses `Settings.Global.BOOT_COUNT`)
  Problem: if the app was force-stopped (BOOT_COMPLETED not delivered) or opened before BOOT_COMPLETED arrives, RUNNING records with pre-reboot `endElapsedRealtime` are restored against the new uptime: either `newlyFinished` fires `TimerAlarmService` for a phantom timer, or the UI shows a multi-day countdown and re-arms AlarmManager.
  Evidence: the stopwatch solves the same problem with BOOT_COUNT; TimerStore does not.
  Fix: persist `Settings.Global.BOOT_COUNT` in each record and drop RUNNING rows whose boot count differs inside `readStoredRecords()` (post the existing "timers cancelled after restart" notification). Unit test with a fake boot count.
  Acceptance: start a 10-minute timer, reboot the emulator with the app force-stopped, open the app: no timer is shown and nothing rings.
  Confidence: Likely
  Effort: S

- [ ] P2 — Watchdog and wake-confirm re-fires start a foreground service from a background worker and fail silently on API 31+
  Category: reliability
  Where: worker/WakeConfirmWorker.kt:185-208; worker/FireWatchdogWorker.kt:81-102 (`context.startForegroundService` from a non-expedited worker, exception only recorded as an incident); compare receiver/MissedAlarmUnlockReceiver.kt:126-144 (already has a full-screen-intent fallback for `ForegroundServiceStartNotAllowedException`)
  Problem: when the user has not granted the battery-optimisation exemption, the FGS start is refused and the safety net that exists for a silently missed alarm does nothing visible.
  Evidence: code trace; the fallback pattern exists in one receiver but is not reused.
  Fix: on failure, arm an immediate exact alarm through `AlarmReceiver` (exact-alarm delivery is FGS-exempt) or post the same FSI fallback notification used by MissedAlarmUnlockReceiver. Unit test the policy branch.
  Acceptance: with battery optimisation not exempted on an API 34 emulator, a watchdog REFIRE still rings or shows the full-screen fallback.
  Confidence: Likely
  Effort: S

- [ ] P2 — Fire watchdog can re-ring an alarm that already fired through the smart-wake early path
  Category: reliability
  Where: worker/FireWatchdogPolicy.kt:51-58 (`broadcastCount == 0` treated as "never rang"); service/SmartAlarmService.kt:238-261 (early fire starts AlarmService directly, no `TYPE_BROADCAST` incident, watchdog not cancelled); service/AlarmService.kt:273-333 (no same-alarm guard in `ACTION_START_ALARM`)
  Problem: a smart-wake early fire that is still ringing or auto-silenced two minutes past the scheduled minute is re-fired by the watchdog, which resets playback, `alarmFiredAt` and the auto-silence timer. The same gap applies to Direct Boot fires (directboot/DirectBootAlarmReceiver.kt:15-28, no incident write).
  Evidence: grep shows `TYPE_BROADCAST` is written only by the four receivers.
  Fix: have `AlarmService` `ACTION_START_ALARM` record a delivered incident the policy counts (or let the policy count `FOREGROUND_SERVICE/START_COMMAND_RECEIVED` for the occurrence) and cancel `FireWatchdogWorker.uniqueName(id)` in `SmartAlarmService.startAlarmService`. Extend `FireWatchdogPolicyTest` with the early-fire case.
  Acceptance: smart-wake early fire followed by a slow challenge does not produce a second `START_COMMAND_RECEIVED` for the same fireId.
  Confidence: Likely
  Effort: S

- [ ] P2 — Room database restored from a newer app version crashes every DB-backed screen
  Category: reliability
  Where: app/src/main/AndroidManifest.xml:111 (`android:restoreAnyVersion="true"`); di/DatabaseModule.kt:26-32 (no `fallbackToDestructiveMigrationOnDowngrade`); RestoreAlarmAgent.kt:8-15
  Problem: a platform restore of `alarm_clock.db` from a newer build onto an older one makes Room throw on open; `BootRescheduleWorker` retries 3× and fails, and the app crashes on every screen that touches the DB with no user-visible explanation.
  Evidence: grep for `Downgrade|fallbackToDestructive` returns nothing.
  Fix: either set `restoreAnyVersion="false"`, or add `fallbackToDestructiveMigrationOnDowngrade()` plus a one-time notification ("Alarms from a newer version could not be restored") and make `RestoreAlarmAgent` record the event.
  Acceptance: restoring a v24-stamped DB onto the v23 build opens the app with an empty alarm list and the notice, instead of crashing.
  Confidence: Verified (by absence; device repro still needed)
  Effort: S

- [ ] P2 — Mission chain is not shown on the alarm card or in the editor summary when the single challenge is NONE
  Category: ux
  Where: ui/alarmlist/AlarmListScreen.kt:920-925 (chip only when `challengeType != "NONE"`), :1369 (accessibility label, same condition); ui/alarmedit/AlarmEditViewModel.kt:325 and :398 (`challengeType` and `challengeChain` are independent fields)
  Problem: a chain such as MATH_EASY → SHAKE → TYPING with `challengeType == NONE` renders no challenge chip at all, and a chain with a type shows only the first type. The reporter expected the chain to be visible from the list.
  Evidence: code trace; `alarm.challengeChain` is never read in AlarmListScreen.kt.
  Fix: compute a display list = `challengeChain.split(",")` when non-blank else `listOf(challengeType)`; render "Math · Shake · Typing" (or "3 challenges" when more than 3) in the chip and accessibility label, using the localised labels from AlarmEditSupport.kt instead of the hardcoded `challengeTypeLabel` map at :1575.
  Acceptance: an alarm with a three-step chain shows all three names on its card.
  Confidence: Verified
  Effort: S
  Reported: #49 — "If I have a reasonably-sized mission chain, I think it should be displayed in the main Alarms screen. Right now I'm only seeing the first challenge."

- [ ] P2 — Editor scroll position resets to the top when returning from a sub-page
  Category: ux
  Where: ui/alarmedit/AlarmEditScreen.kt:225-227 (`LaunchedEffect(editorPage) { editorScrollState.scrollToItem(0) }`), :195 (single `rememberLazyListState` shared by all pages)
  Problem: opening a category from the overview and pressing Back scrolls the overview to the top, so a user working down the list loses their place every time.
  Evidence: code trace; the overview is a LazyColumn keyed by section, so a saved index can be restored.
  Fix: keep one `LazyListState` per page (`rememberSaveable(saver = LazyListState.Saver)` in a map keyed by `AlarmEditorPage`) and only scroll sub-pages to 0 on entry; restore the overview state on return.
  Acceptance: scroll the overview to "Advanced behavior", open it, go back: the overview is still scrolled to that card.
  Confidence: Verified
  Effort: S
  Reported: #49 — "when I scroll down, click on a sub-menu (like Advanced behavior), then go back, my scroll position should be saved"

- [ ] P2 — Snooze settings are split across two sections and mission chaining sits apart from the challenge picker
  Category: ux
  Where: ui/alarmedit/AlarmEditScreen.kt:92-119 section order (SNOOZE, DISMISS_CHALLENGE, LOCATION, …, CHAIN, ANTI_SNOOZE); ui/alarmedit/AlarmEditDismissSections.kt:91-118 (duration only), :514-530 (progressive snooze in ANTI_SNOOZE)
  Problem: progressive snooze, backup sound and the (future) snooze limit live several cards below the snooze duration, and the chain builder is a separate card from "Dismiss challenge", so users do not discover that a chain replaces the single challenge.
  Fix: move `progressiveSnooze` (and the new snooze-limit row) into the SNOOZE section; render the chain builder inside DISMISS_CHALLENGE as a "Add more challenges" affordance under the type picker and collapse ANTI_SNOOZE to backup-sound only. Update `alarm_edit_section_*_description` strings.
  Acceptance: the Dismiss page shows Snooze (duration, limit, progressive) first, then Challenge (type + chain) as one card.
  Confidence: Verified
  Effort: S
  Reported: #49 — items 2 and 4 ("advanced snooze settings should be up top, by the main snooze setting"; "Mission chaining can be integrated into the Dismiss challenge")

- [ ] P2 — Dashboard weather failure is labelled "Set your location" and offers no retry
  Category: ux
  Where: ui/dashboard/DashboardViewModel.kt:381-407 (any fetch failure → `weatherError = "Weather unavailable"`); ui/dashboard/DashboardScreen.kt:286-321 (every non-null `weatherError` renders the "Set your location / Choose" card; "Retry weather" exists only in the stale-cache branch :331-339)
  Problem: an offline user with a city configured is told to set a location; the error text is never shown and there is no retry.
  Fix: branch on `state.hasLocation`: when true render `AppFeedbackCard` with the error and an `onRetryWeather` button, otherwise the location prompt.
  Acceptance: with airplane mode on and a city set, the Today tab shows "Weather unavailable" with a Retry button.
  Confidence: Verified
  Effort: S

- [ ] P2 — "Pause all alarms" has no banner or Resume on the Alarms tab and the per-card copy is wrong
  Category: ux
  Where: ui/settings/SettingsViewModel.kt:559-576 (`pauseAlarmsForDays`); ui/alarmlist/AlarmListViewModel.kt (never reads `pauseUntilMillis`); ui/alarmlist/AlarmListScreen.kt:1054 (header only says "All alarms paused" because every trigger is 0), :1066 (badge is vacation-only), :1614 ("Paused until you re-enable this alarm")
  Problem: after pausing from Settings, each alarm card claims it must be re-enabled (false), and the only Resume control is three screens away.
  Fix: add `pausedUntilMillis` to `AlarmListUiState`, show a "Paused until <date> · Resume" chip in the hero (reuse the vacation chip at :1066) and use it in `nextOccurrenceLabel`.
  Acceptance: pause for 2 days, open Alarms: hero shows "Paused until Mon, Aug 24 · Resume"; tapping Resume re-arms.
  Confidence: Verified
  Effort: S

- [ ] P2 — Settings defaults never seed new alarms; three settings and five orphan fields are dead
  Category: correctness
  Where: ui/settings/SettingsScreen.kt:472, :493 (`defaultSnoozeDuration`, `defaultGradualVolume`, copy promises they apply to new alarms), :439-444 (`showOnLockScreen`); ui/alarmedit/AlarmEditViewModel.kt:249-258 (new alarm built from `AlarmEditUiState()` defaults 10/60); ui/alarmfiring/AlarmFiringActivity.kt:163 (`setShowWhenLocked(true)` unconditional); data/preferences/PreferencesManager.kt orphans `upcomingAlarmMinutes`, `showNoAlarmsWarning`, `guardianContactName`, `guardianContactPhone`, `nightClockEnabled` (round-tripped by backup only)
  Problem: the Defaults category contains three controls that change nothing, and the settings model carries five fields no screen reads or writes.
  Fix: seed `snoozeDurationMinutes`/`gradualVolumeSeconds` from `preferencesManager.getCachedSettings()` in the new-alarm branch; gate `setShowWhenLocked` on the setting (or delete the row and the per-alarm field); delete the five orphans from `AppSettings`, `Keys`, `SettingsBackup` and the drift test's exemption list (keep JSON tolerant).
  Acceptance: set default snooze to 15 min, tap New alarm: the Snooze card shows 15 min; `BackupManagerSettingsDriftTest` passes after the field removals.
  Confidence: Verified
  Effort: S

- [ ] P2 — Webhook client follows redirects and re-sends the signed payload cross-host
  Category: security
  Where: service/WebhookService.kt:74-77 (OkHttp client with default `followRedirects`/`followSslRedirects`), :301-306 `isAllowedWebhookUrl` (validates the initial URL only)
  Problem: a 307/308 from the configured endpoint forwards the body, `X-ACX-Signature` and `X-ACX-Timestamp` to any HTTPS host, including LAN hosts that bypass the local-network gate.
  Fix: `.followRedirects(false).followSslRedirects(false)` on the webhook client; treat 3xx as a failed delivery in the status log.
  Acceptance: a test server answering 307 to another host sees no second request; delivery log shows "HTTP 307".
  Confidence: Likely
  Effort: S

- [ ] P2 — Hue legacy v1 (HTTP) path and `http://` internet-radio URLs are dead on this targetSdk, yet the UI offers them
  Category: correctness
  Where: integration/hue/HueBridgeClient.kt:140, worker/HueSunriseWorker.kt:159, service/DismissActionExecutor.kt:201 (`http://$bridgeIp/api/$apiKey/...`); ui/settings/SettingsViewModel.kt:471-515 (`hueLegacyHttpEnabled` toggle and "reachable (legacy API v1 over HTTP)" message); service/AlarmService.kt:907-908 (`http://` accepted for radio); res/values/strings.xml:614 ("Stream URL (http://…)")
  Problem: no `usesCleartextTraffic` or network security config exists, so cleartext is blocked platform-wide; the v1 Hue calls throw `UnknownServiceException` and are swallowed, and `http://` radio streams always fall back to the default tone. Users are offered settings that can never work.
  Fix: either add `res/xml/network_security_config.xml` with `cleartextTrafficPermitted="true"` scoped to the Hue bridge host (set at runtime via `domain-config` cannot be dynamic, so prefer removing the v1 path as HueSunriseWorker.kt:34-35 already plans) and reject non-HTTPS radio URLs in `Alarm.sanitized()` with an editor error; update the label to "Stream URL (https://…)".
  Acceptance: the legacy HTTP toggle is gone or works; typing `http://` in the radio field shows a validation error.
  Confidence: Verified
  Effort: S

- [ ] P2 — Backup import is not transactional in Replace mode (already tracked as blocked; unblock it)
  Category: correctness
  Where: Roadmap_Blocked.md "P1 — Make Replace-mode backup restore atomic" and "P2 — Bump backup version stamp"; data/backup/BackupManager.kt:677-713
  Problem: both entries are blocked on "uncommitted in-flight BackupManager changes from a parallel session". The working tree is clean and backup v17 shipped, so the blocker no longer exists.
  Fix: move both items back into this file and implement as written there (insert before delete inside `database.withTransaction`, skip delete when the imported set is empty, surface skipped-row count).
  Acceptance: items live in ROADMAP.md; a crash injected between delete and insert leaves the original alarms intact.
  Confidence: Verified
  Effort: S

- [ ] P2 — `alarm_events` grows without bound and has no indices on the columns the Stats screen filters on
  Category: perf
  Where: data/local/entity/AlarmEvent.kt:10 (no `indices`); data/local/AlarmEventDao.kt:23, :43, :50-60 (filters on `action`, `firedAt`, `alarmId`), :62 (only `deleteAll`); compare data/repository/AlarmIncidentRepository.kt:121-129 (prune on insert)
  Fix: add `deleteOlderThan(firedAt)` and prune to 365 days / 5,000 rows in `AlarmEventRepository.record()`; add indices on `action`, `firedAt`, `(alarmId, firedAt)` via a Room migration 23→24 with exported schema.
  Acceptance: migration test passes; `alarm_events` row count stays bounded after 6,000 synthetic inserts.
  Confidence: Verified
  Effort: S

### P2 — UX, i18n and visual

- [ ] P2 — Half of the user-facing strings bypass localisation, so the new language picker has nothing to switch
  Category: ux
  Where: app/build.gradle.kts:195-203 (`verifyLocalizedPrimaryScreens` guards only three files); ~81 literals in ui/alarmfiring/challenges/ChallengeViews.kt (e.g. :122, :165, :486, :694, :1361), ~52 in ui/stats/StatsScreen.kt (:129, :152, :372, :467), ~52 in ui/alarmlist/AlarmListScreen.kt (:1051, :1022, :1033, :1248, :1561), ~40 in ui/dashboard/DashboardScreen.kt, ~37 in ui/bedtime/BedtimeScreen.kt, plus service/AlarmService.kt:2072, service/NextAlarmNotifier.kt:210/249, receiver/BedtimeReceiver.kt:216/296, worker/WakeConfirmWorker.kt:214-254, widget/NextAlarmWidget.kt:243, directboot/DirectBootAlarmService.kt:122/131 (resources `direct_boot_alarm_title`/`_stop` exist but are unused), ui/navigation/AppNavigation.kt:88-93 (tab labels), and the whole wear module (wear strings.xml has 5 entries; NextAlarmTileService.kt:106-242, WearAlarmData.kt:76-130)
  Problem: res/xml/locales_config.xml declares only `en` and util/AppLanguageManager.kt offers only English, so the Android 13 language picker is a two-option no-op; any future translation would leave the firing challenges, stats, list, notifications and watch tile in English.
  Fix: move the literals above to strings.xml (use the existing keys where they already exist), add plurals for `"${n} result${if (n == 1) "" else "s"}"` (RingtonePickerSheet.kt:321) and the degenerate plurals at strings.xml:396/397/1129/1166/1167; extend `primaryComposeScreenFiles` in build.gradle.kts to every `ui/**/*.kt` plus service/receiver/worker/widget notification builders; add the wear module to the guard.
  Acceptance: `./gradlew verifyLocalizedPrimaryScreens` fails on a new `Text("…")` literal anywhere under ui/; a pseudo-locale build (`en-XA`) shows accented text on every screen and notification.
  Confidence: Verified
  Effort: L

- [ ] P2 — Settings description lines are truncated to one line, hiding what each toggle does
  Category: ux
  Where: ui/settings/SettingsPersonalizationSection.kt toggle rows (observed: "Shows a short quote on the firing screen alo…", "Auto-bumps math challenges (Easy → Medi…", "Blends the app accent with your wallpaper p…") and the shared toggle-row composable they use (search for `maxLines = 1` / `TextOverflow.Ellipsis` on supporting text in ui/settings/*)
  Problem: every helper sentence on the Personalization page is cut off mid-word on a 1080 px phone (screenshot 10-mono-settings), so the explanation the copy was written for is unreadable.
  Fix: allow `maxLines = 2` (or unbounded) on supporting text in the shared settings row; keep titles single-line.
  Acceptance: no ellipsis on Personalization supporting text at 411 dp width with default font scale.
  Confidence: Verified
  Effort: S

- [ ] P2 — Launch shows a white system splash before the dark app
  Category: visual
  Where: res/values/themes.xml:3-7 (`Theme.AlarmClockXtreme` parents `android:Theme.Material.NoActionBar`, sets `windowBackground` #0D1B2A but no `windowSplashScreenBackground`); ui/theme/Color.kt:17 (`SurfaceDark = #070B11`)
  Problem: on API 31+ the system splash uses the theme's `colorBackground` (white for Theme.Material) so a dark-only app flashes a white screen with the icon (screenshot 01-launch), then jumps to #0D1B2A, then to the Compose #070B11.
  Fix: add `<item name="android:windowSplashScreenBackground">#070B11</item>` (values-v31) or adopt `androidx.core:core-splashscreen` with `Theme.SplashScreen`, and align `windowBackground`, `statusBarColor`, `navigationBarColor` and res/layout/widget_loading.xml:6 to `SurfaceDark`.
  Acceptance: cold launch on an API 35 emulator shows a dark splash with no white frame.
  Confidence: Verified
  Effort: S

- [ ] P2 — "Wake readiness" card on the Settings hub has an empty band between the title and the progress row
  Category: visual
  Where: ui/settings/SettingsScreen.kt readiness summary card (the card with "Wake readiness", "Review", "4 of 6 ready" and the progress bar)
  Problem: the card renders the title, then ~40 dp of nothing, then a right-aligned "Review" link, then the counter and bar (screenshot 08-settings). Likely the same min-height/top-align pattern as the editor cards, or an empty slot reserved for a description string (`settings_wake_readiness_description` is unused per lint).
  Fix: either render the unused description under the title or remove the reserved space; put "Review" on the same row as the title.
  Acceptance: no blank band; title and Review on one row, counter and bar directly beneath.
  Confidence: Verified
  Effort: S

- [ ] P2 — Wake-confirm countdown resets on rotation and disagrees with the real deadline
  Category: correctness
  Where: ui/alarmfiring/WakeConfirmActivity.kt:177-183 (`remember { mutableIntStateOf(countdownSeconds) }` + `LaunchedEffect(Unit)` decrement loop)
  Problem: rotating the phone restarts the visible countdown from the full value while `WakeConfirmWorker`'s deadline keeps running, so the user is told they have more time than they do.
  Fix: compute remaining seconds from the intent's `scheduledAt` + `SystemClock.elapsedRealtime()` anchor passed by the worker, and add a `liveRegion` on the "Time's up" transition (:242-246).
  Acceptance: rotate at 5 s left: the screen still shows 5 s.
  Confidence: Verified
  Effort: S

- [ ] P2 — News tab hides already-loaded headlines behind the error card when a refresh fails, and has no retry button
  Category: ux
  Where: ui/news/NewsScreen.kt:165-199 (`errorMessage != null` branch wins over `items`; `AppEmptyState` passed no `footer`); ui/news/NewsViewModel.kt:157-163 (keeps `items`, sets `errorMessage`)
  Fix: when `items` is non-empty show an `AppInlineNotice` above the list instead of replacing it; pass a Retry `footer` to the empty-state (pattern at ui/worldclock/WorldClockScreen.kt:117).
  Acceptance: pull-to-refresh in airplane mode keeps the old headlines visible with an inline "Couldn't refresh" notice and a Retry button.
  Confidence: Verified
  Effort: S

- [ ] P2 — Destructive actions with neither confirmation nor undo: Stopwatch Reset and Timer Stop
  Category: ux
  Where: ui/stopwatch/StopwatchScreen.kt:311-317 → StopwatchViewModel.kt:90-95 (wipes elapsed time and all laps); ui/timer/TimerScreen.kt:237-239 → TimerViewModel.kt:196-208 (removes the timer immediately, Stop sits next to Pause)
  Fix: keep the immediate action (project rule: no confirmation dialogs) but add an Undo snackbar for 5 s that restores the lap list / re-creates the timer with the remaining duration.
  Acceptance: tap Reset then Undo: laps return; tap Stop then Undo: the timer resumes with the same remaining time.
  Confidence: Verified
  Effort: S

- [ ] P2 — Glance widget uses an old palette and sub-12sp text, and reports DB errors as "No alarms set"
  Category: visual
  Where: widget/NextAlarmWidget.kt:127-132 (WidgetBg #0D1B2A, WidgetCardBg #152238, WidgetTextMuted #4A5568 ≈ 2.3:1 on the bg), :184 (10 sp), :196 (11 sp), :203-233 (-10m/+10m targets ≈ 40×24 dp), :84-86 (any exception → null → "No alarms set" at :243)
  Fix: map the widget constants to the ui/theme tokens (SurfaceDark/SurfaceCard/TextMuted), raise text to ≥12 sp, pad the ±10m actions to 48 dp, and render a distinct "Couldn't load alarms" state on exception.
  Acceptance: widget colours match the app; TalkBack targets are 48 dp; an injected DAO exception shows the error state.
  Confidence: Verified
  Effort: S

- [ ] P2 — Microcopy drift and developer jargon in user-facing strings
  Category: ux
  Where: res/values/strings.xml: L73 `Snooze %1$dm` vs seven `%1$d min` strings; L1268 "Snooze Countdown" vs L76 "Snooze countdown"; Title Case channel names L7-L9, L1263, L1266 vs sentence case elsewhere; 13 straight `\'` vs 12 curly `’`; L876 "Alarms-only only" (lint Typos); L988 "SDK available", L991 "READ_SLEEP granted", L986 full permission constant, L984 "F-Droid flavor", L947/L954 JSON field names, L659 "NONE", L671/L670 "content:// URI"; 27 em dashes in in-app copy (house style forbids them in user-facing text); code-side: ui/stats/StatsScreen.kt:726-738, :1141, :1262-1264 ("Motion index 0.42", "Source: sonar RMS…"), ui/bedtime/BedtimeScreen.kt:403, ui/alarmfiring/WakeConfirmActivity.kt:245/:269 ("re-fire"), ui/alarmfiring/MorningBriefingActivity.kt:286 (design rationale shown to the user), ui/share/SharedAlarmImportScreen.kt:219 ("Save, keep off"), ui/components/WhatsNewDialog.kt:56/:85 (two buttons, one outcome), app name written four ways ("AlarmClockXtreme", "ACX", "Alarm Clock Xtreme" in OnboardingScreen.kt:100, wear tile)
  Fix: one copy pass: single snooze format (`%1$d min`), sentence case everywhere except proper nouns, curly apostrophes, no em dashes, one app-name spelling, replace permission/JSON/enum names with plain language ("Sleep data access granted"), delete the MorningBriefing rationale line, make WhatsNew buttons distinct or single.
  Acceptance: `grep -c "—" strings.xml` = 0; lint Typos = 0; no `READ_SLEEP`/`labelIncluded`/`content://` in strings.xml.
  Confidence: Verified
  Effort: M

- [ ] P2 — Accessibility gaps on secondary screens
  Category: a11y
  Where: ui/bedtime/BedtimeScreen.kt:272-276, :367-379 (Switches with no label association); ui/bedtime/BedtimeWindDownSections.kt:82-85 (`clickable(role = Checkbox)` without `toggleable` state); ui/bedtime/BedtimeSleepTrackingSections.kt:70-76 (tag chips without `selectionSemantics = true`); ui/bedtime/BedtimeBreathingSection.kt:91-95 (phase label changes with no `liveRegion`); ui/nightclock/NightClockActivity.kt:151-153 (exit is a raw long-press gesture with no semantics); ui/news/NewsScreen.kt:119-142 (feed tabs convey selection by colour/underline only, no `selected` state); ui/timer/TimerScreen.kt:211-220 and ui/stopwatch/StopwatchScreen.kt:77-100 (state changes not announced); ui/templates/TemplatePickerSheet.kt:193-197 and ui/ringtone/RingtonePickerSheet.kt:477 (duplicated announcements from labelled decorative icons); ui/components/YouTubeDownloadDialog.kt:654-663 (unlabelled progress IconButton)
  Fix: `Modifier.toggleable`/`semantics { selected }` on the listed controls, `liveRegion = Polite` on the listed status labels, `Modifier.combinedClickable(onLongClick)` or a visible Exit button on Night Clock, `contentDescription = null` on decorative icons inside merged rows.
  Acceptance: TalkBack announces "Bedtime reminder, switch, on", "Inhale", "Selected" on tags, and Night Clock exposes an exit action.
  Confidence: Verified
  Effort: M

- [ ] P2 — User input and dialogs lost on rotation across secondary screens
  Category: ux
  Where: `remember` instead of `rememberSaveable` for user state: ui/stats/StatsScreen.kt:102-105, ui/bedtime/BedtimeScreen.kt:117-121 (running breathing session resets), ui/ringtone/RingtonePickerSheet.kt:122-130, ui/components/YouTubeDownloadDialog.kt:119-130 (typed URL, search hits and an in-flight download are dropped; parent flag at RingtonePickerSheet.kt:126 closes the dialog), ui/worldclock/WorldClockScreen.kt:73, ui/onboarding/OnboardingScreen.kt:158-160, ui/share/SharedAlarmImportScreen.kt:73, ui/alarmfiring/WakeConfirmActivity.kt:177
  Fix: switch to `rememberSaveable` (custom Savers for lists) and move the YouTube download into the ViewModel's scope so rotation does not cancel it.
  Acceptance: rotate mid-search in the YouTube dialog: query, results and the running download survive.
  Confidence: Verified
  Effort: M

- [ ] P2 — Ringtone preview prepares MediaPlayer synchronously on the main thread
  Category: perf
  Where: ui/ringtone/RingtonePickerSheet.kt:216-231 (`MediaPlayer().prepare()` on tap); compare ui/components/YouTubeDownloadDialog.kt:195 (`prepareAsync`)
  Fix: use `prepareAsync` with `setOnPreparedListener { start() }` and an error listener that sets `previewError`.
  Acceptance: tapping a SAF/folder ringtone never blocks the UI (no jank in Perfetto / no "Skipped frames" log).
  Confidence: Verified
  Effort: S

- [ ] P2 — Dependabot PRs #33-#42 are open against the repo (blocked: GitHub write needed)
  Category: maintainability
  Where: GitHub PRs 33-42 (Dependabot); repo has no `.github/` directory any more
  Problem: house rule is no Dependabot/Renovate; ten bot PRs sit open and several (AGP/Gradle 9, KSP 2.3, Hilt 2.60) are exactly the coupled upgrades Roadmap_Blocked tracks as a manual migration.
  Fix: human action on GitHub: close the ten PRs, run `gh api repos/SysAdminDoc/AlarmClockXtreme/vulnerability-alerts -X DELETE` to disable Dependabot security updates, and delete the stale "P2 — Configure GitHub Actions signing secrets (release.yml fails on every tag)" entry in Roadmap_Blocked.md (no workflow exists; releases are built locally by rule).
  Acceptance: `gh pr list --state open` is empty; Roadmap_Blocked.md no longer references `.github/workflows/release.yml`.
  Confidence: Verified
  Effort: S

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
