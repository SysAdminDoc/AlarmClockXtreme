# AlarmClockXtreme Roadmap

Living feature backlog, refreshed **2026-04-17** alongside **v1.5.0**.

This is the "what's left" side of [CLAUDE.md](CLAUDE.md). Entries are ranked
by impact-to-effort and grouped by theme. Effort column: **S** = a session,
**M** = a few days of focused work, **L** = multi-phase initiative.

**Legend**
- `[ ]` Not started
- `[~]` Design / research stage
- `[x]` Implemented (kept briefly for traceability before migrating to CLAUDE.md)

---

## 0. v1.4.0 Follow-ups (mostly closed in v1.5.0)

Small gaps created by the v1.4.0 batch. Most shipped in v1.5.0.

| # | Item | Effort | Status |
|---|------|--------|--------|
| 0.1 | Alarm-edit UI for `hardwareButtonAction` | S | ✅ v1.5.0 |
| 0.2 | Alarm-edit UI for `dismissAtRingtoneEnd` | S | ✅ v1.5.0 |
| 0.3 | Alarm-edit UI for `ringtonePool` | M | ✅ v1.5.0 (multi-line editor, not a file-picker — power-user feature) |
| 0.4 | Alarm share via deep link (`acx://alarm?data=`) | M | ✅ v1.5.x (share-sheet export + disabled-by-default deep-link import) |
| 0.5 | Nap-mode duration chip respects `napDefaultMinutes` | S | ✅ v1.5.0 |
| 0.6 | Sleep-sound timer / fade sliders in BedtimeScreen | S | ✅ v1.5.0 (fade taper slider; whole-timer lives on Bedtime too) |
| 0.7 | Unit tests for `MissedAlarmUnlockReceiver` window logic | S | ✅ v1.5.2 |
| 0.8 | Unit tests for `ProximityCoverDetector` hold threshold | S | ✅ v1.5.2 |
| 0.9 | Lint / data-extraction-rules doc comment (fixed in build) | — | ✅ v1.4.0 build |

---

## 1. Sleep Tracking State-of-the-Art

The feature most likely to push this from "power-user alarm" to "best-in-class
sleep platform." The existing SonarSleepService, smart-alarm service, and
accel sensors are all pieces of the machine — we just haven't wired them into
a coherent tracker yet.

| # | Item | Source | Effort | Rationale |
|---|------|--------|--------|-----------|
| 1.1 | **Smart-wake window (accel-based)** | Sleep Cycle | M | Fire within a user-defined N-min window when motion suggests light sleep. SmartAlarmService already exists — needs a scoring algorithm + scheduler hook. |
| 1.2 | **Composite sleep score (0-100)** | Rise / Oura | S | Blend duration × efficiency × regularity × stage balance. Daily engagement hook for stats. |
| 1.3 | **On-device actigraphy → sleep stages** | Pillow / SleepWatch | L | TensorFlow Lite / on-device Cole-Kripke. Awake / light / deep / REM bucketing. |
| 1.4 | **Snore recording + timeline** | Sleep as Android | M | Ring buffer in mic service, save >60 dB bursts, playback in stats. Mic permission already present. |
| 1.5 | **Sleep debt accumulator** | Rise | S | Running 14-day deficit vs. per-user need; surfaces naps more contextually. |
| 1.6 | **Apnea / cough event flagging** | Sleep as Android Premium | L | Onset-detection on mic buffer. Health flag, not diagnosis — legal copy required. |
| 1.7 | **Chronotype quiz + ideal bedtime calc** | Rise / MEQ | S | Onboarding step. Writes `bedtimeHour/Minute` from quiz output. |
| 1.8 | **Pre-sleep tag tiles (caffeine/exercise/alcohol)** | Sleep as Android | M | Extend `AlarmEvent` with tags; show correlation chart in stats. |
| 1.9 | **Environmental-noise baseline** | Pillow | S | One-off sample before bedtime reminder; warns if bedroom >45 dB. |
| 1.10 | **Lullaby soundscapes with motion-auto-off** | Calm / Headspace | M | Extend SleepSoundPlayer to stop when accel reports no movement for N min. |
| 1.11 | **Stats charts (Vico / MPAndroidChart)** | Custom | M | Visualise the sleep score, snooze-rate history, streaks. Today stats are text-only. |

## 2. Wear OS & Companion Devices

A gap identified in v1.4.0 research — zero wearable story today.

| # | Item | Source | Effort | Rationale |
|---|------|--------|--------|-----------|
| 2.1 | **Wear OS tile + complication (next alarm)** | Google Clock | M | Dismiss / snooze from wrist. Re-uses Quick-Settings tile pattern. |
| 2.2 | **Health Connect read/write (Sleep Sessions, HR)** | Android 14+ | M | One API replaces per-vendor Fitbit/Samsung/Garmin SDKs. Requires Play declaration + privacy policy update. |
| 2.3 | **Wearable-only vibration alarm** | Sleep as Android | M | Silent phone, watch-only buzz. "Don't wake partner" path. |
| 2.4 | **Bed-exit auto-dismiss via watch** | Withings / Garmin | M | Watch motion >60 s → dismiss. Elegant "prove you're up." |
| 2.5 | **HRV-aware smart wake** | Sleep as Android | L | Watch HRV dip is better than phone accel for light-sleep detection. |
| 2.6 | **Standalone Pixel Watch / Galaxy Watch app** | Samsung Health | L | Alarm works without phone. Biggest effort bump. |

## 3. Novel Dismiss Challenges

Extend the current 16-challenge roster. These fit the existing Challenge
sealed class pattern; most are S-tier and fun to ship.

| # | Item | Source | Effort |
|---|------|--------|--------|
| 3.1 | Handwriting / drawing (ML Kit digital ink) | Alarmy | M |
| 3.2 | Voice-phrase repeat (SpeechRecognizer offline) | Alarmy / I Can't Wake Up | M |
| 3.3 | Pushup / plank hold | Alarmy Premium | M |
| 3.4 | Chess mate-in-1 puzzle | indie | M |
| 3.5 | Simon-says color sequence ✅ v1.5.0 | I Can't Wake Up | S |
| 3.6 | Rock-paper-scissors best-of-5 | indie | S |
| 3.7 | Emoji memorisation grid | I Can't Wake Up | S |
| 3.8 | Anki / vocab flashcard | — | M |
| 3.9 | Speed-reading comprehension (RSVP) | — | M |
| 3.10 | Typing-speed gate (N wpm, <2 errors) | indie | S |
| 3.11 | Type today's date backwards ✅ v1.5.0 | indie | S |
| 3.12 | Wordle-style 5-letter guess | NYT Games | S |
| 3.13 | Stroop color-name test ✅ v1.5.0 | Cognitive research | S |
| 3.14 | Spot-the-difference | indie | M |
| 3.15 | QR scavenger with location verify | Alarmy | S |

## 4. Accessibility

Currently table-stakes compliance. Worth a dedicated pass.

| # | Item | Source | Effort |
|---|------|--------|--------|
| 4.1 | Screen-flash + camera-flash patterns for deaf users | Apple "Flash for Alerts" | S |
| 4.2 | Haptic-only alarm profile via VibrationEffect.Composition | Apple Taptic | S |
| 4.3 | TalkBack audit — large double-tap buttons on firing | Android a11y | S |
| 4.4 | Pure-black / mono-color high-contrast theme (WCAG AAA) | — | S |
| 4.5 | Voice-only dismiss (offline SpeechRecognizer) | Voice Access | S |
| 4.6 | Per-user long-press thresholds on challenge buttons | Android a11y | S |
| 4.7 | LE Audio hearing-aid routing for alarm stream | Android 13 LE Audio | M |

## 5. Workplace / Shift Worker

Underserved niche. "Alarm clock for shift workers" is a searchable category
with very few open-source options.

| # | Item | Source | Effort |
|---|------|--------|--------|
| 5.1 | Rotating shift patterns (DDNNO / 4-on-4-off / Panama) | Shyft | M |
| 5.2 | Jet-lag re-entrainment schedule | Timeshifter | L |
| 5.3 | **Commute-aware alarm** (shift earlier on traffic) | Google Maps Distance Matrix | L |
| 5.4 | On-call rotation mode (override DND-silent) | PagerDuty | M |
| 5.5 | AutomaticZenRule v2 (bedtime DND owner) | Android 14 | S |
| 5.6 | Focus-mode handoff at bedtime / dismiss | Android 14 Focus | S |
| 5.7 | Meeting-shift awareness (first-meeting moved → alarm moved) | Calendar | S |

## 6. Relationships / Household

| # | Item | Source | Effort |
|---|------|--------|--------|
| 6.1 | Partner profiles (two users, separate alarms/ringtones) | Sleep as Android couples | M |
| 6.2 | "Don't wake partner" vibration-only mode | Apple Bedtime | M |
| 6.3 | Paired-phone LAN sync (partner-dismiss → you snooze) | — | M |
| 6.4 | Kid-friendly green-light mode | OK to Wake / Hatch | M |
| 6.5 | Pet-feeding reminder chain on dismiss | — | S |
| 6.6 | Remote parental alarm set (Family Link) | Google Family Link | L |

## 7. Habit / Routine Integration

Builds on morning-routine / motivational-quotes features.

| # | Item | Source | Effort |
|---|------|--------|--------|
| 7.1 | Wake-streak flame badge | Streaks / Duolingo | S |
| 7.2 | Gratitude / journal prompt on dismiss | Day One / Stoic | S |
| 7.3 | Water-intake quick-log tiles | WaterMinder | S |
| 7.4 | Mood selfie + emoji tag | Daylio | S |
| 7.5 | Obsidian / Notion / Markdown daily-note append | — | M |
| 7.6 | Health Connect weight / BP quick-entry | Android 14+ | S |
| 7.7 | Tasker / Macrodroid / Home Assistant recipe library | Existing webhook | S |
| 7.8 | Badge set: "5am club", "no-snooze week", "DDNNO survivor" | Habitica | S |
| 7.9 | Local-LAN leaderboard (household) | — | S |
| 7.10 | Share-card screenshot generator | Strava | S |

## 8. Audio Deep Features

| # | Item | Source | Effort |
|---|------|--------|--------|
| 8.1 | Binaural / isochronic tone generator (0.5-4 Hz delta) | Brain.fm / myNoise | M |
| 8.2 | Mathematical noise synth (brown/pink/violet) | myNoise | S |
| 8.3 | ASMR trigger pack | — | S (content only) |
| 8.4 | Voice-memo ringtone (in-app 30 s recorder) | iOS | S |
| 8.5 | YouTube URL as alarm (NewPipeExtractor) | indie | M (legal grey) |
| 8.6 | Podcast "latest episode" (Podcast Index / AntennaPod URI) | AntennaPod | M |
| 8.7 | Per-alarm Bluetooth sink (BluetoothA2dp) | — | M |
| 8.8 | UPnP / DLNA multi-room cast escalation | Cling | L |
| 8.9 | Chromecast / Nest Hub alarm target | Cast SDK | M |
| 8.10 | Spatial-audio sunrise panning (Dolby Atmos) | — | M (novelty) |

## 9. Advanced Scheduling

| # | Item | Source | Effort |
|---|------|--------|--------|
| 9.1 | Islamic prayer-time Fajr alarm | Aladhan API | M |
| 9.2 | Lunar / Hebrew / Hindu calendar repeat | — | M |
| 9.3 | Sunrise/sunset-relative firing ✅ v1.5.0 | Custom math | S |
| 9.4 | Astronomical events (meteor shower peak, ISS flyover) | Heavens-Above | M |
| 9.5 | Birthday auto-alarm from Contacts | Contacts provider | S |
| 9.6 | Menstrual-cycle aware (softer alarm in luteal phase) | Health Connect Menstruation | M |
| 9.7 | Weather-conditional firing (fire earlier on snow > 2 cm) | OpenWeatherMap / Open-Meteo | M |

## 10. Power / Reliability

| # | Item | Source | Effort |
|---|------|--------|--------|
| 10.1 | Power-off alarm (Qualcomm / Samsung / Xiaomi HAL) | OEM | L (OEM privileges) |
| 10.2 | Boot-locked (direct-boot) full-screen alarm | Android 14+ | S (audit) |
| 10.3 | Emergency-escalation call tree (SMS → call → partner) | Twilio / native | M |
| 10.4 | Location-based escalation (still at home → siren) | FusedLocation | M |
| 10.5 | Car-mode suppression (Android Auto DrivingStateManager) | Android Auto | S |
| 10.6 | Companion-watch autonomous fire if phone battery dies | — | M |
| 10.7 | Charging-only alarm variant | — | S |

## 11. Emerging Android Platform (14 → 16)

Wiring to keep the app "modern" against each platform bump.

| # | Item | Android | Effort |
|---|------|---------|--------|
| 11.1 | Live Updates / progress-style alarm notification | 15 | S |
| 11.2 | Predictive Back / OnBackInvokedCallback polish | 14 | S |
| 11.3 | Lockscreen widgets (next-alarm chip) | 17 preview | S (blocked on API) |
| 11.4 | Device Controls API publish ("Turn off alarm") | 11+ | S |
| 11.5 | AutomaticZenRule v2 (ConditionProviderService) | 14 | M |
| 11.6 | Monochrome adaptive icon ✅ (already shipped in v1.3.x) | 13+ | S |
| 11.7 | Per-app language picker (LocaleManager) | 13 | S |
| 11.8 | Credential Manager + passkey-gated cloud backup | 14 | M |
| 11.9 | Ultra-HDR sunrise rendering | 14 | S |

## 12. Cloud / Sync

Stubbed v1.4.0 research touched on this but didn't ship.

| # | Item | Source | Effort |
|---|------|--------|--------|
| 12.1 | Google Drive / Nextcloud backup (SAF-based) | — | M |
| 12.2 | AES-256 passphrase encryption over BackupManager output | — | S |
| 12.3 | Nextcloud WebDAV sync (self-host crowd) | — | M |
| 12.4 | End-to-end encrypted paired-phone sync | — | L |

## 13. UX Polish

| # | Item | Source | Effort |
|---|------|--------|--------|
| 13.1 | Landscape / tablet layouts (every screen) | — | M |
| 13.2 | Always-On Display-aware night clock | AOD API | S |
| 13.3 | Dynamic color from specific wallpaper color (not full palette) | — | S |
| 13.4 | Share button on alarm card overflow (ties with 0.4) ✅ v1.5.x | — | S |
| 13.5 | Interactive onboarding walkthrough (beyond current steps) | — | M |
| 13.6 | In-app changelog dialog on first launch after update ✅ v1.5.0 | — | S |
| 13.7 | Search / filter in stats by tag, day, or alarm ✅ v1.5.x | — | S |

---

## Research Notes

**Sources consulted** for v1.4.0 + this roadmap (abbreviated):

- Open source: [fennifith/Alarmio](https://github.com/fennifith/Alarmio), [FossifyOrg/Clock](https://github.com/FossifyOrg/Clock), [BlackyHawky/Clock](https://github.com/BlackyHawky/Clock), [LineageOS DeskClock](https://github.com/LineageOS/android_packages_apps_DeskClock), [sweakpl/qralarm-android](https://github.com/sweakpl/qralarm-android), [kunal-mahatha/Early-Bird-App](https://github.com/kunal-mahatha/Early-Bird-App)
- Commercial reference: Alarmy, Sleep as Android, Sleep Cycle, Pillow, Rise, Turbo Alarm, Google Clock, I Can't Wake Up
- Platform docs: Android 12-15 release notes, Health Connect SDK, ML Kit Digital Ink, Wear OS Tiles API, CredentialManager, AutomaticZenRule v2, Cast SDK, Google Maps Distance Matrix

**Deliberately not pursued** (for now):

- YouTube alarm source — licensing grey zone; would block Play Store acceptance.
- Full Firebase analytics — the privacy stance ("no tracking, no accounts, no data leaves") is a differentiator we keep.
- Ad-supported free tier — same reasoning; the app is and will remain ad-free.
- Social feed / public streak sharing — privacy trade-off not worth it.
- Sleep-coaching subscription model — we remain open-source / donation-based.

**Legal / compliance flags** to budget before touching:

- Health Connect (2.2) requires a published privacy policy update and a
  Play Console declaration.
- Apnea flagging (1.6) must carry "not a medical device" disclaimers.
- Power-off alarm (10.1) needs per-OEM privileged partner programs; may
  never be achievable for an indie app.
- Partner-phone sync (6.3 / 12.4) needs an explicit threat model doc.

---

*Roadmap owners: add yourself as assignee when picking up an item. Prefer
one-item-per-PR for the S-effort work and phased delivery for M/L.*

## Open-Source Research (Round 2)

### Related OSS Projects
- **yuriykulikov/AlarmClock** — https://github.com/yuriykulikov/AlarmClock — AOSP-derived, long-click dismiss UX, adjustable snooze time-picker.
- **yassineAbou/Clock** — https://github.com/yassineAbou/Clock — Pure Jetpack Compose, single-activity + bottom-nav + WorkManager for timer/stopwatch integration.
- **ahudson20/whakaara** — https://github.com/ahudson20/whakaara — Compose + Room + Hilt reference architecture; minimal and idiomatic.
- **akshay2211/JetAlarm** — https://github.com/akshay2211/JetAlarm — MVVM Compose alarm, Apache-2.0.
- **plusmobileapps/alarm-clock** — https://github.com/plusmobileapps/alarm-clock — Jetpack + Kotlin, small and readable.
- **hanihashemi/Alarm-Clock-App-Tutorial** — https://github.com/hanihashemi/Alarm-Clock-App-Tutorial — Tutorial repo; handy for the exact AlarmManager + fullscreen-intent wiring that LTS Android versions keep regressing.
- **AlarmIT** (alarm-clock topic) — https://github.com/topics/alarm-clock — shake + math dismiss reference (older but still cited).

### Applied in this pass
- Implemented the shareable-alarm workflow from items 0.4 and 13.4: card overflow -> Android share sheet -> `acx://alarm?data=` link -> import through `MainActivity`.
- Imported shared alarms are intentionally disabled until reviewed so a received link cannot silently schedule a live alarm.
- Shared payloads reuse the backup alarm mapper, keeping future alarm-field additions in one portable serialization path.
- Implemented item 13.7: Stats history now supports text search plus outcome/day filters, with empty-filter feedback and a pure filter helper covered by unit tests.

### Features to Borrow
- **Long-click dismiss + long-press snooze time-picker** (yuriykulikov/AlarmClock) — prevents accidental-dismiss complaints; long-press snooze opens an inline picker rather than cycling presets.
- **Single-activity + Compose nav for clock/timer/stopwatch/alarm tabs** (yassineAbou/Clock) — consolidates what AlarmClockXtreme currently splits, and exposes a Timer/Stopwatch path without growing the APK.
- **WorkManager-backed stopwatch/timer persistence** (yassineAbou/Clock) — survives process death on Android 15+ cold-kill aggressive OEMs (Xiaomi/Oppo/Samsung battery managers).
- **Hilt DI boundaries around AlarmScheduler** (whakaara) — clean swap of `AlarmManager` vs `WorkManager` vs test-fake for CI tests.
- **Room migration discipline** (whakaara) — canonical single-source-of-truth schema per release and testMigration() blocks to avoid user-loss regressions.
- **Fullscreen-intent + SCREEN_ON fallback matrix** (hanihashemi tutorial) — explicit table of which Android versions need which flags (POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT, SCHEDULE_EXACT_ALARM) with runtime feature-detection.
- **"Nudge" alarms that gradually ramp volume** (not yet in ACX per feature list) — common in commercial alarm apps; use `AudioFocus` + Compose LaunchedEffect timer.
- **Widget-driven "create alarm" shortcut** — Glance widget that opens ACX to the create screen with preset duration params (borrowing the Compose Glance pattern surfacing in several 2025 alarm forks).

### Patterns & Architectures Worth Studying
- **Compose bottom-nav single-activity + per-destination ViewModel scoping** (yassineAbou/Clock) — reduces StateFlow leakage between timer/stopwatch/alarm and halves cold start.
- **Hilt + AlarmScheduler abstraction** (whakaara) — lets the dismiss-challenge engine and alarm scheduler be unit-tested without the Android runtime.
- **AOSP-derived dismiss UX** (yuriykulikov/AlarmClock) — `DismissAlarmActivity` run on top of lockscreen with `FLAG_SHOW_WHEN_LOCKED` + `FLAG_TURN_SCREEN_ON` + `setTurnScreenOn(true)` is the battle-tested formula.
- **Per-alarm `setAlarmClock()` with `setInexactRepeating` fallback** (yuriykulikov/AlarmClock) — graceful-degradation path when SCHEDULE_EXACT_ALARM permission is revoked post-install on Android 14+.

## Implementation Deep Dive (Round 3)

### Reference Implementations to Study
- **yuriykulikov/AlarmClock/app/src/main/java/com/better/alarm/services/AlarmsService.kt** — https://github.com/yuriykulikov/AlarmClock — canonical `startForeground()` called synchronously in `onStartCommand` before any IO; good pattern for comparing against ACX's v1.5.4 fix.
- **yassineAbou/Clock/app/src/main/java/com/yassineabou/clock/ui/timer/TimerViewModel.kt** — https://github.com/yassineAbou/Clock — WorkManager-backed timer that survives process death on Xiaomi/Oppo/Samsung. Direct blueprint for ACX's Timer tab persistence.
- **ahudson20/whakaara/app/src/main/java/com/app/whakaara/domain/AlarmScheduler.kt** — https://github.com/ahudson20/whakaara — Hilt-injectable `AlarmScheduler` abstraction with test fake. Replaces ACX's untestable `domain/AlarmScheduler.kt` directly.
- **FossifyOrg/Clock/app/src/main/kotlin/org/fossify/clock/receivers/AlarmReceiver.kt** — https://github.com/FossifyOrg/Clock/blob/main/app/src/main/kotlin/org/fossify/clock/receivers/AlarmReceiver.kt — lightweight wake lock (10s) + startForegroundService pattern proven across 100k+ users. Template for DST/timezone edge case handling ACX hasn't hit yet.
- **Applinx-Tech/Flutter-Alarm-Manager-POC/android/app/src/main/java/...AlarmActivity** — https://github.com/Applinx-Tech/Flutter-Alarm-Manager-POC — `showWhenLocked="true"` + `turnScreenOn="true"` full-screen intent wiring + all permission declarations in one manifest. Reference for verifying ACX's `AlarmFiringActivity` manifest flags.
- **giorgosneokleous93/fullscreenintentexample/app/src/main/AndroidManifest.xml** — https://github.com/giorgosneokleous93/fullscreenintentexample — minimal-complete full-screen intent example; compare `USE_FULL_SCREEN_INTENT` permission declaration + notification channel importance (`IMPORTANCE_HIGH` required).
- **androidx/media3/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/util/EventLogger.java** — https://github.com/androidx/media — clean MediaPlayer/ExoPlayer listener pattern; ACX's internet-radio `prepareAsync()` listener could move to ExoPlayer for HLS support.
- **AOSP DeskClock (packages/apps/DeskClock)** — https://android.googlesource.com/platform/packages/apps/DeskClock/ — Google's reference DeskClock app; `AlarmStateManager.kt` is the gold-standard alarm state machine (PENDING → SNOOZED → FIRING → MISSED → DISMISSED) with persisted StateHolder.

### Known Pitfalls from Similar Projects
- **Android 14 USE_EXACT_ALARM is auto-granted, SCHEDULE_EXACT_ALARM is not** — FossifyOrg/Calendar#217 — alarm-clock-category apps should declare `USE_EXACT_ALARM` (install-time grant) instead of `SCHEDULE_EXACT_ALARM` (runtime). Check ACX manifest. https://github.com/FossifyOrg/Calendar/issues/217
- **SecurityException when inexact alarm internally falls back to exact** — flutter_local_notifications#2248 — reporter only used `inexactAllowWhileIdle` but `AlarmManager.setImpl` still threw. Always surround `set*` with try-catch-fallback-to-inexact. https://github.com/MaikuB/flutter_local_notifications/issues/2248
- **Five-second window to call `startForeground()` after `startForegroundService()`** — androidx/media#2412 — ACX already fixed this in v1.5.4 but the spec is Media3-relevant: if audio is started off the main thread, the service timeout fires. https://github.com/androidx/media/issues/2412
- **Doze mode defers even `setAlarmClock` on Redmi/Samsung by 1-2 min** — Microsoft Q&A — the only fix is to pair with a 10-15s `PARTIAL_WAKE_LOCK` in `onReceive`. ACX already has the wake lock but verify the timing is <15s so ANR watchdog doesn't kill it. https://learn.microsoft.com/en-us/answers/questions/5566005/
- **`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` broadcast must be registered to reschedule alarms after permission-grant** — FossifyOrg/Calendar#217 — otherwise alarms set before permission grant remain inexact forever. ACX doesn't currently listen for this.
- **`BootReceiver.goAsync()` 10s ANR ceiling** — ACX v1.5.4 tightened timeout 30s→8s — but if a user has 50+ alarms, `rescheduleAll` can still exceed 8s. Consider batching via WorkManager.
- **Android 15 preparatory changes: Foreground service short-type 3-minute cap** — starting Android 15 (API 35), short-type FGS auto-timeouts. ACX uses `mediaPlayback` type (no timeout) — good. Do not migrate to `shortService` type.
- **`setAlarmClock()` always shows clock icon in status bar** — can't be hidden; users complain. Workaround: offer a user setting "Show alarm icon in status bar" that falls back to `setExactAndAllowWhileIdle()` if disabled (with UX disclaimer about 1-2min drift). https://learn.microsoft.com/en-us/answers/questions/5566005/
- **`CalendarContract.Instances` RRULE expansion requires `READ_CALENDAR`** — runtime permission, not declared in all manifests. ACX `CalendarAutoAlarmWorker` must handle denial gracefully (ACX v1.3.2 already rewrites this worker — verify permission check).
- **WorkManager + Hilt requires `Configuration.Provider` on Application AND default initializer removed in manifest** — easy to regress on gradle plugin upgrades. Lock in a CI check. https://github.com/ahudson20/whakaara

### Library Integration Checklist
- **AndroidX WorkManager (timer persistence + Guardian + Calendar workers)** — `androidx.work:work-runtime-ktx:2.10.0` (ACX already has) — entry: `CoroutineWorker.doWork()` + `Data.Builder().putLong("alarmId", id)`. Gotcha: `setInitialDelay(minimumOf(1m))` because `WorkManager` floors delays under 1 minute to 10 minutes on some OEMs — ACX v1.3.3 fixes this.
- **AndroidX Glance (widgets)** — `androidx.glance:glance-appwidget:1.1.1` — entry: `class NextAlarmWidget : GlanceAppWidget() { override provideGlance() { ... } }`. Gotcha: Glance state updates require `updateAppWidgetState(context, glanceId)` — just calling `update()` on ViewModel does not refresh the widget.
- **Philips Hue v2 API (HueSunriseWorker upgrade path)** — no Maven; REST client via OkHttp. Entry: `PUT https://{bridge}/api/{username}/lights/{id}/state {"bri": N, "transitiontime": deciseconds}`. Gotcha: Hue v1 `username` endpoints are deprecated; migrate to Hue v2 `application_key` header + HTTPS certificate pinning (self-signed cert — pin via `OkHttpClient.Builder().hostnameVerifier`). ACX's `HueSunriseWorker` currently uses v1.
