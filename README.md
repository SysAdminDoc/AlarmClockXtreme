# AlarmClockXtreme

![Version](https://img.shields.io/badge/version-1.15.11-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-green)
![Platform](https://img.shields.io/badge/platform-Android%208.0+-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)

> A feature-rich, open-source alarm clock for Android with 50+ alarm fields, 30 dismiss challenges, smart wake intelligence, a built-in YouTube alarm-sound downloader, and a deep dark theme. No ads, no tracking, no accounts.

<p align="center">
  <img src="assets/screenshots/alarm-list.png" width="360" alt="AlarmClockXtreme alarm list with next alarm, alarm cards, quick actions, and bottom navigation" />
</p>

## Download

**Latest signed APK** - [Releases page](https://github.com/SysAdminDoc/AlarmClockXtreme/releases/latest)

```
adb install AlarmClockXtreme-v1.15.11-play.apk
```

The Play-flavor APK includes the YouTube alarm-sound downloader (yt-dlp + NewPipe Extractor), Wear OS Data Layer bridge, Wear next-alarm tile/complication support, optional Health Connect READ_SLEEP integration, and ML Kit Digital Ink handwriting recognition. The F-Droid flavor strips proprietary or Play-distribution-adjacent phone pieces for an unencumbered build.

## Roadmap

Current release-hardening work is focused on public F-Droid inclusion, signed-release automation, API 37 platform readiness, Wear action acknowledgement, localization/pseudo-locale cleanup, and permission/store-declaration drift guards. Local planning details stay in the repository maintainer backlog; public release notes and issues track user-visible changes.

## Build From Source

```bash
git clone https://github.com/SysAdminDoc/AlarmClockXtreme.git
cd AlarmClockXtreme
./gradlew assemblePlayDebug
# Install: adb install app/build/outputs/apk/play/debug/app-play-debug.apk
./gradlew :wear:assembleDebug
# Wear tile: install wear/build/outputs/apk/debug/wear-debug.apk on a paired Wear OS watch
```

**Requirements:** Android Studio Narwhal+ or the included Gradle wrapper, JDK 17, Android SDK 36

## Features

### Core Alarm Engine
| Feature | Description |
|---------|-------------|
| Reliable Scheduling | `setAlarmClock()` for maximum reliability, survives Doze mode |
| Direct Boot Fallback | Re-registers a default-sound/vibration alarm from device-encrypted storage before first unlock after reboot, with a full-screen stop UI |
| Solar-Relative Firing | Fire relative to sunrise/sunset with a configurable offset (NOAA approximation) |
| Alarm Groups | Tag alarms (Work, School, Gym), filter with chips |
| Alarm Profiles | Named configurations (Work, Travel, Weekend) for quick switching |
| Date-Specific Alarms | Set alarm for a particular calendar date (overrides repeat days) |
| Duplicate Alarm | Clone any alarm with all settings via overflow menu |
| Skip Next | Skip one occurrence of a repeating alarm |
| Vacation Mode | Date range auto-skip for all repeating alarms |
| Holiday Auto-Skip | Skip public holidays via Nager.Date API (40+ countries) |
| Templates | Power Nap, Early Bird, Weekday, Weekend presets |
| Shareable Alarms | Share a single alarm as an `acx://alarm?data=` link; imports open a review screen, stay disabled, and can strip private references before saving |
| Early Dismiss | Skip upcoming alarm from the persistent notification |
| Calendar Auto-Alarm | Keeps one reusable alarm shifted before tomorrow's first timed calendar event |
| Commute-Aware Auto-Alarm | Optional event-location commute buffer uses Google Routes transit ETA when you supply a key, and falls back to no-key bad-weather buffers |
| Wear OS Tile + Complication | Shows the next alarm on the watch tile and compatible watch faces; the tile also exposes skip controls before fire and snooze/dismiss controls while ringing and honors the public-label privacy setting |

### Dismiss Challenges (30 Types)
| Challenge | Description |
|-----------|-------------|
| Math (Easy/Medium/Hard) | Solve arithmetic problems with explicit operator precedence |
| Shake Phone | Shake device N times (configurable) |
| Number Sequence | Tap 6 numbers in ascending order |
| Memory Pattern | Memorize and recreate a tile pattern on 3x3 grid |
| Type a Phrase | Type a displayed phrase exactly (custom phrases supported) |
| Voice Phrase | Say the displayed phrase with Android speech recognition; typed fallback protects denied-permission and no-recognizer devices |
| Handwriting | Draw a displayed wake word with Play-flavor ML Kit Digital Ink; typed fallback keeps F-Droid and offline-missing-model devices dismissable |
| Walk Steps | Walk N steps using step counter sensor |
| NFC Tag Scan | Tap a pre-registered NFC tag |
| Barcode/QR Scan | Scan a pre-registered barcode |
| Photo Match | Photograph a registered location (similarity scoring) |
| Squats | Accelerometer-based squat detection |
| Maze Puzzle | Navigate a randomized 5x5 maze |
| Wi-Fi Connect | Connect to a specific Wi-Fi network |
| Count the Sheep | Tap every drifting sheep; avoid the decoy goats (novel CAPTCHA) |
| Simon Says | Watch a 4-pad color sequence and play it back in order |
| Type Date Backwards | Type today's ISO date reversed character-by-character |
| Stroop Test | Tap the INK color of a color-word, not the word itself |
| Rock / Paper / Scissors | Best-of-5 RPS against the computer (first to 3 wins) |
| Emoji Memory | Match 8 emoji pairs on a face-down 4x4 grid |
| Typing Speed | Type a phrase at >= N wpm with limited word mistakes |
| Wordle | Guess the 5-letter target word in <= 6 attempts |
| Reaction Test (PVT) | Tap 5 times when a green stimulus appears; average under 500ms to dismiss |
| Spot the Difference | Compare two color grids and tap the single changed tile |
| Chess Mate in 1 | Pick the checkmate move from a compact board position |
| RSVP Speed Reading | Read a rapid word stream, then identify a word that appeared |
| Push-ups | Accelerometer-based push-up detection with the phone face-down on the floor |
| Plank Hold | Hold the phone level in a plank position for 30 seconds |
| Mission Chaining | Stack 2-5 challenges in sequence (e.g., Math + Shake + Typing) |
| Adaptive Difficulty | Auto-escalates math difficulty based on snooze history |

### Anti-Snooze Arsenal
| Feature | Description |
|---------|-------------|
| Progressive Snooze | Each snooze shortens by 1 minute (10 -> 9 -> 8 -> ...) |
| Backup Sound Escalation | Ultra-loud volume boost if no interaction within configurable delay |
| Max Snooze Count | Auto-dismiss after N snoozes reached |
| Guardian Angel | Emergency-contact escalation if an alarm is not dismissed; F-Droid can auto-send SMS, Play opens a prefilled SMS composer |
| Wake Confirmation | Re-fires alarm if user doesn't confirm they're awake |
| Flashlight Strobe | Camera flash LED strobe during alarm |
| Repeat Missed Alarms | Auto-silenced alarm re-fires briefly on next unlock (configurable) |
| Cover-to-Snooze | Hold a hand over the proximity sensor for ~1.5s to snooze |
| Hardware Button Action | Map volume / headset / camera keys to snooze or dismiss per alarm |

### Wake Experience
| Feature | Description |
|---------|-------------|
| Flash Wake | Gradual screen brightness increase alongside volume |
| Sunrise Simulation | Screen transitions from deep red to warm yellow |
| TTS Announcement | Speaks time, date, and weather after dismissal |
| Morning Briefing | Full-screen good morning card with weather + calendar |
| Morning Routine | Post-alarm checklist (stretch, water, journal, etc.) |
| Motivational Quotes | Random inspirational quotes on alarm firing screen |
| Swipe Gestures | Swipe left to dismiss, right to snooze |
| Custom Snooze | Pick 1/3/5/15/30 minute snooze from firing screen |
| Hold to Dismiss | Optional 1.5-second hold gesture before final dismissal |

### Sound & Vibration
| Feature | Description |
|---------|-------------|
| Ringtone Picker | Browse and preview system ringtones |
| Hearing-Aid Routing | Alarm tones, previews, Direct Boot fallback, and setup test alarms use `AudioAttributes.USAGE_ALARM` so Android's system hearing-aid/speaker routing can apply |
| YouTube Alarm Sounds | Search YouTube or paste a URL, preview before downloading, save the audio straight to your alarm library (Play flavor only) |
| Random Ringtone Pool | Per-alarm pool of URIs; a random one is picked each fire (anti-habituation) |
| Dismiss at Ringtone End | Auto-dismiss when the chosen song/tone finishes (Spotify-friendly) |
| Spotify Integration | Play Spotify tracks/playlists as alarm sound |
| Internet Radio | Stream any HTTP/HTTPS radio station URL |
| Gradual Volume | Configurable fade-in (15s to 5 min) |
| Custom Vibration | 5 patterns: Default, Gentle, Heartbeat, Escalating, SOS |
| Silent Mode | Fire alarm with notification only, no sound |
| Haptic-Only Profile | One-tap "Don't wake partner" preset mutes alarm audio and keeps tactile wake cues |

### Smart Features
| Feature | Description |
|---------|-------------|
| Smart Alarm | Accelerometer-based light sleep detection, fires early during optimal window |
| Sonar Sleep Tracking | Experimental ultrasonic breathing/movement detection with manual Bedtime start/stop and local Statistics summaries; no raw audio is retained |
| Local Actigraphy Buckets | Smart-alarm windows store compact awake-motion, light-motion, and still-motion summaries for 30 days; raw accelerometer samples are not retained |
| Philips Hue Sunrise | Gradually ramp smart lights before alarm fires |
| Webhook / Tasker | POST HTTPS JSON on `alarm_fired`, `alarm_snoozed`, `alarm_dismissed`, `alarm_missed`, and `alarm_skipped` events |
| Dismiss Actions | Trigger a validated webhook, package-scoped broadcast, or configured Philips Hue scene after a successful dismiss |
| Flip-to-Snooze | Place phone face-down to snooze |

### Dashboard & Utilities
| Feature | Description |
|---------|-------------|
| Weather Tab | Centered weather hero, 3-day vertical forecast, sunrise/sunset, UV index with EPA bands, hourly strip with rain probability — Open-Meteo, free, no API key |
| Live Radar | Animated precipitation radar embedded from Windy.com (no key, no SDK) — auto-centers on your weather location |
| Time-of-Day Sky | The Today tab background follows your real sunrise/sunset through a 15-keyframe gradient — deep night, civil dawn, sunrise, midday peak, golden hour, dusk |
| Weather-Aware Theme | Storms swap the sky to overcast blue-gray; night storms drop to near-black with subtle lightning flashes; tornado warnings render a rotating funnel cloud silhouette + warning banner |
| Tornado Alerts (US) | NWS active-alerts integration via api.weather.gov — no key, free, US-only — drives the on-screen warning |
| News Tab | Public RSS reader (Google News, BBC, NPR, Hacker News presets) — pull-to-refresh, tap to open in browser, no accounts |
| Calendar Integration | Today's events from device calendar |
| World Clock | Live time zones with UTC offset, 24h format support, persistent saved cities |
| Multiple Timers | Run several countdown timers concurrently (monotonic clock) |
| Stopwatch | Lap tracking with best/worst marking |
| Bedtime Tracking | Sleep goal, sleep cycle calculator, bedtime reminders, sleep sounds |
| Bedtime DND | App-owned alarms-only Do Not Disturb rule for the sleep window, with clear access/status feedback |
| Health Connect Sleep | Play flavor only: opt-in READ_SLEEP summaries and local sleep/wake trend charts in Bedtime and Statistics |
| Statistics | Wake-streak flame badge, snooze rate, day-of-week breakdown, response times, searchable alarm history |
| Night Clock | Always-on bedside display with minimal brightness |
| Home Widget | Glance-based widget showing next alarm countdown, with optional neutral alarm labels on public surfaces |
| Persistent Notification | Always-visible next alarm countdown in shade, with Android 16 Live Update progress during the final two hours and optional public-label hiding |
| Quick Settings Tile | Skip the next alarm from the system shade with one tap; subtitles can use neutral next-alarm text |
| Tab Visibility Toggles | Hide Weather / Timer / World / News tabs from the bottom nav (Alarms + Settings always visible) |
| Accent Color | Customizable accent color within dark theme |
| Material You | Opt-in dynamic color from wallpaper palette (Android 12+) |
| Expressive Surfaces | Opt-in Material 3 Expressive shape rhythm and accent semantics across shared cards, chips, loading states, and navigation |
| Wind-Down Checklist | Pre-sleep checklist rendered on the Bedtime tab |
| Configurable Sleep Timer | Sleep-sound fade-out with 5s-10min taper and configurable hold |

### Data & Reliability
| Feature | Description |
|---------|-------------|
| Backup/Restore | JSON export/import of all 50+ alarm fields and 59 settings, with optional AES-256 passphrase encryption, custom news-feed round-trip, and pre-export disclosure for secrets/private references (v11 format) |
| Shareable Alarms | Export a single alarm to a copy/paste-able `acx://` link; received links are reviewed and saved disabled |
| Boot Reschedule | All alarms re-registered after device reboot, with a Direct Boot fallback for the next alarm before first unlock |
| Wake Readiness | Settings checks exact alarms, notifications, Android 14+ full-screen alarm access, battery optimization, and App Standby state before bedtime |
| Manufacturer Compat | Onboarding warnings for Xiaomi/Samsung/Huawei battery killers |
| Crash Logger | Automatic crash log files for debugging |
| Local Support Bundle | Settings can package crash logs plus redacted version, device, wake-readiness, smart-wake, incident-timeline, and alarm diagnostics into a shareable ZIP without telemetry |
| Alarm Diagnostics | Settings shows the latest redacted incident event and can clear incident history separately from Statistics |
| Sleep/Wake Analytics | Statistics correlates local alarm history with Health Connect sleep duration, snoozes, dismiss response, and challenge retries |
| Auto-Silence | Configurable timeout (0/5/10/15/30 min), records as missed |
| Webhook Reliability | Application-lived dispatch scope keeps automation events moving even when the alarm service stops mid-flight; payloads use a versioned schema |

## Architecture

```
+---------------------------------------------------------+
|                    UI Layer (Compose)                     |
|  Screens <- ViewModels <- StateFlow                      |
|  30 challenge flows, 8 alarm edit sections               |
+---------------------------------------------------------+
|                   Domain Layer                           |
|  AlarmScheduler | NextAlarmCalculator | SolarCalculator  |
|  Date-specific + holiday + vacation + solar-anchor logic |
+---------------------------------------------------------+
|                    Data Layer                            |
|  Room DB v17 | DataStore | Retrofit (Open-Meteo, Nager, NWS) |
|  HealthConnectSleepRepository (Play READ_SLEEP summaries) |
|  50+ field Alarm entity | 35+ field AppSettings          |
|  YouTubeAudioDownloader (yt-dlp + NewPipe Extractor)     |
+---------------------------------------------------------+
|                   Android Platform                       |
|  AlarmManager | 4 ForegroundServices | 8 Workers         |
|  7 BroadcastReceivers | Direct Boot fallback | Glance/QS Tile |
+---------------------------------------------------------+
```

**Tech stack:** Kotlin 2.1, Jetpack Compose (Material 3), Room, Hilt, Retrofit + Moshi (codegen), DataStore, Glance widgets, OkHttp, Coroutines/Flow, WorkManager, Wear Tiles/Protolayout/Complications, Health Connect client (Play flavor), ML Kit Digital Ink (Play flavor), yt-dlp, NewPipe Extractor (Play flavor)

## Configuration

### Signing

1. Generate a keystore: `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alarm`
2. Copy `keystore.properties.template` to `keystore.properties`
3. Fill in your keystore path and credentials
4. Build: `./gradlew :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease`

Local release builds use `keystore.properties`. If you mirror this setup in
another environment, use either the preferred `ANDROID_*` names or the legacy
short names for the same values:

| Preferred secret | Legacy alias | Purpose |
|------------------|--------------|---------|
| `ANDROID_KEYSTORE_BASE64` | `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `ANDROID_KEYSTORE_PASSWORD` | `KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | `KEY_ALIAS` | Signing key alias |
| `ANDROID_KEY_PASSWORD` | `KEY_PASSWORD` | Signing key password |

Release builds produce signed Play, F-Droid, and Wear APKs locally. Verify
signatures with `apksigner`, write `SHA256SUMS.txt` and
`APK-CERT-FINGERPRINTS.txt`, and attach the artifacts to the GitHub Release.

To verify a sideloaded APK's signing certificate:

```bash
apksigner verify --print-certs AlarmClockXtreme-v1.15.11-play-release.apk
```

Compare the `certificate SHA-256 digest` against the fingerprint published in
the release's `APK-CERT-FINGERPRINTS.txt`.

Before a local release, run:

```bash
bash scripts/check-signing-hygiene.sh
python scripts/osv_gradle_audit.py
python scripts/verify_api37_release.py --device <api37-16kb-serial> --run-test-alarm --fresh-install
```

The signing check fails if keystore files or `keystore.properties` are
tracked/staged, or if common signing-material paths are not ignored. The OSV
gate resolves Play, F-Droid, and Wear release runtime classpaths and exits
non-zero on reported vulnerabilities. The Android 17 verifier checks 16 KB APK
alignment for Play, F-Droid, and Wear release APKs, installs the Play release
on an API 37 16 KB device, verifies exact-alarm, promoted-notification,
notification, and local-network permission state, and can drive the built-in
test alarm.

### Build Variants

| Variant | Description |
|---------|-------------|
| `playDebug` | Google Play flavor, debug signing |
| `playRelease` | Google Play flavor, release signing, R8 minified |
| `fdroidDebug` | F-Droid flavor, debug signing |
| `fdroidRelease` | F-Droid flavor, release signing, R8 minified |

## Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| `USE_EXACT_ALARM` | Fire alarms at exact time | Yes |
| `POST_NOTIFICATIONS` | Show alarm alerts | Yes |
| `POST_PROMOTED_NOTIFICATIONS` | Android 16 promoted alarm updates where available | Optional |
| `USE_FULL_SCREEN_INTENT` | Show the alarm firing screen over the lock screen | Yes |
| `ACCESS_NOTIFICATION_POLICY` | Optional bedtime DND rule access | Optional |
| `FOREGROUND_SERVICE` | Reliable alarm playback | Yes |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Alarm audio | Yes |
| `FOREGROUND_SERVICE_DATA_SYNC` | Smart alarm monitoring | Yes |
| `FOREGROUND_SERVICE_MICROPHONE` | Sonar sleep tracking | Yes |
| `RECEIVE_BOOT_COMPLETED` | Reschedule after reboot | Yes |
| `WAKE_LOCK` | Keep CPU during alarm | Yes |
| `VIBRATE` | Alarm vibration | Yes |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional manufacturer/battery reliability flow | Optional |
| `INTERNET` | Weather, holidays, radar, news, webhooks, radio, Hue, and Play-flavor YouTube downloads | Yes |
| `ACCESS_LOCAL_NETWORK` | Android 17+: LAN access for Philips Hue bridge and local webhook endpoints | Optional |
| `ACCESS_COARSE_LOCATION` | Weather for your area | Optional |
| `ACCESS_FINE_LOCATION` | Wi-Fi challenge matching on Android 12+ | Optional |
| `ACCESS_WIFI_STATE` | Wi-Fi dismiss challenge reads the connected SSID | Optional |
| `READ_CALENDAR` | Dashboard events + auto-alarm | Optional |
| `NFC` | NFC tag dismiss challenge | Optional |
| `CAMERA` | Barcode scan + photo match challenges | Optional |
| `RECORD_AUDIO` | Voice phrase dismiss + Sonar sleep tracking | Optional |
| `android.permission.health.READ_SLEEP` | Health Connect sleep-session summaries in Bedtime and Statistics | Optional (Play flavor only) |
| `MODIFY_AUDIO_SETTINGS` | Alarm audio routing and volume behavior | Yes |
| `ACTIVITY_RECOGNITION` | Walk steps + smart alarm | Optional |
| `SEND_SMS` | Guardian Angel automatic emergency SMS | Optional (F-Droid flavor only) |
| `CALL_PHONE` | Guardian Angel emergency call path; falls back to the dialer when denied | Optional |
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Browse device audio for alarm sounds | Optional |
| `WRITE_EXTERNAL_STORAGE` (<= Android 9) | Save downloaded YouTube alarm sounds to MediaStore | Optional (Play flavor only) |

## Privacy

No analytics. No ads. No tracking. No accounts. No data leaves your device except:
- Weather, air-quality, geocoding, and pollen calls to Open-Meteo for your selected location
- Active-alerts calls to api.weather.gov / NWS for US weather-alert coverage
- Holiday API calls to Nager.Date for the selected country
- Live radar embed from Windy.com on the Weather tab; Windy's privacy policy applies inside the embed
- News RSS calls to your configured feed source, including Google News, BBC, NPR, Hacker News, or a custom URL
- Webhook calls to your configured HTTPS URL, including alarm event metadata. Payloads include `schemaVersion`, `event`, `eventId`, `occurredAt`, `alarmId`, `scheduledFor`, `displayTime`, and `labelIncluded`; alarm labels are included only when the Settings toggle is on.
- Google Routes transit ETA calls for commute-aware calendar auto-alarms only when you enable the feature and provide your own Routes API key; event locations and saved origin coordinates are sent for that lookup.
- Internet radio streaming to your configured station
- Philips Hue commands to your configured bridge on your local network
- ML Kit Digital Ink model download in the Play flavor when you first use handwriting recognition; recognition runs on-device after the model is available
- YouTube search, preview, stream resolution, and download requests in the Play flavor only; F-Droid excludes this feature

Play-flavor Health Connect support is opt-in and requests only `android.permission.health.READ_SLEEP`. Recent sleep-session summaries are used locally in Bedtime and Statistics, including sleep/wake trend charts; they are not copied into Room/DataStore/backups and are never uploaded to the developer. Smart-alarm actigraphy stores only compact local motion-bucket summaries for 30 days; raw accelerometer samples are not retained and the buckets are not medical sleep-stage records. Sonar sleep tracking is manually started from Bedtime, uses the microphone only while its foreground notification is active, stores compact movement/restless/still summaries locally for 30 days, and does not retain raw audio. Support bundles are created only when you choose to export them and include redacted wake-readiness, test-alarm proof timing, aggregate smart-wake decision metadata, and recent alarm incident event codes, not labels or per-minute motion/audio buckets. Crash logs stay in local app storage unless you export them. Plain JSON backups and share links are created only when you choose to export or share, and may contain alarm labels, schedules, settings, integration URLs, webhook URLs, and Hue configuration.

Settings can hide alarm labels on public surfaces. When enabled, alarm notifications, missed-alarm prompts, wake-confirm prompts, the home widget, the quick settings tile, and Play-flavor Wear next-alarm snapshots use neutral alarm text while labels remain visible inside the unlocked app.

Android system Auto Backup is deliberately narrower than manual export. On Android 12+, cloud backup includes the Room alarm database (schedules, labels, challenge configs) but excludes DataStore preferences, which contain webhook URLs, Hue API keys, guardian contacts, and custom feed URLs. Device-transfer (same-device or direct migration) includes DataStore so those settings survive a local upgrade. On Android 8-11, legacy cloud backup includes only DataStore preferences. Crash logs, support bundles, downloaded media, and challenge reference files are excluded from all backup paths. After a cloud restore, enabled alarms are automatically re-armed via a restore agent.

Full privacy policy: [PRIVACY_POLICY.html](PRIVACY_POLICY.html)

## Webhook Integration Recipes

ACX sends an HTTPS POST with a JSON payload on alarm lifecycle events. Enable webhooks in Settings > Integrations > Webhooks and enter your HTTPS endpoint URL.

**Payload schema (v1):**

```json
{
  "schemaVersion": 1,
  "event": "alarm_fired",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-13T07:00:00Z",
  "alarmId": 42,
  "scheduledFor": "2026-06-13T07:00:00Z",
  "displayTime": "7:00 AM",
  "labelIncluded": true,
  "label": "Work alarm",
  "fireId": "42-1718258400000"
}
```

Events: `alarm_fired`, `alarm_snoozed`, `alarm_dismissed`, `alarm_missed`, `alarm_skipped`, `test`. The `label` field is present only when `labelIncluded` is `true` (controlled by Settings > Webhooks > Include alarm labels). `fireId` is present when available.

### Home Assistant

Create a webhook automation in your `automations.yaml`:

```yaml
automation:
  - alias: "ACX alarm fired — turn on lights"
    trigger:
      - platform: webhook
        webhook_id: "your-secret-random-id-here"
        allowed_methods:
          - POST
        local_only: true
    condition:
      - condition: template
        value_template: "{{ trigger.json.event == 'alarm_fired' }}"
    action:
      - service: light.turn_on
        target:
          entity_id: light.bedroom
        data:
          brightness_pct: 80
          color_temp_kelvin: 3000
```

In ACX Settings, enter `https://your-ha-host:8123/api/webhook/your-secret-random-id-here`. Generate a unique random `webhook_id` (e.g., `uuidgen` or `python -c "import uuid; print(uuid.uuid4())"`) — do not reuse example values. Home Assistant webhook triggers do not require an API token when `local_only: true`.

### Tasker

ACX requires HTTPS endpoints. For local Tasker automation, set up a local HTTPS bridge:

1. Install the **Tasker** app and the **AutoRemote** plugin (or another HTTPS-capable receiver).
2. In Tasker, create a Profile > Event > Plugin > AutoRemote > Message Filter = `acx_alarm`.
3. In ACX Settings, enter your AutoRemote personal HTTPS URL.
4. Create a Tasker Task linked to the profile with your desired actions (e.g., enable Wi-Fi, launch music app, set display brightness).

Alternatively, if your network supports it, use a Tasker HTTP Server plugin with a self-signed certificate and configure ACX to POST to `https://your-phone-ip:port/alarm`.

### MacroDroid

1. In MacroDroid, create a Macro with trigger **Webhook (URL)**.
2. Copy the generated HTTPS webhook URL (MacroDroid provides one per macro via cloud relay; requires Play Services).
3. Paste the URL into ACX Settings > Webhooks.
4. In the macro action, use **Set Variable** to capture `{webhook_body}`, then parse with `JSONPath` expressions:
   - Event: `$.event`
   - Alarm ID: `$.alarmId`
   - Display time: `$.displayTime`
5. Add a **Condition** checking the variable against your target event (e.g., `alarm_fired`).

Note: MacroDroid's saved webhook body limit is ~3800 characters. ACX payloads are well under this limit.

## FAQ

**How does the YouTube alarm-sound downloader work?**
On the Alarms tab tap "Download alarm sound from YouTube." A dialog opens with two modes: search YouTube directly (powered by NewPipe Extractor) or paste a video URL. Each result has a play/stop button to preview the audio before committing. Tap the row to download; yt-dlp resolves the best-audio stream with a fixed `--get-url` option allow-list, OkHttp streams it to your alarm library via MediaStore (`IS_ALARM=1`), and the ringtone picker re-enumerates so the new sound is immediately selectable. The Play flavor also includes a manual "Update" action for the local yt-dlp engine; ACX never updates it in the background, and a failed update keeps the existing engine in place. F-Droid builds ship without this feature for licensing reasons.

**Why does the alarm not fire on my Xiaomi/Samsung/Huawei?**
These manufacturers aggressively kill background apps. The app shows a manufacturer-specific warning during onboarding with steps to whitelist it. Generally: Settings > Battery > App Launch > AlarmClockXtreme > Manual > enable all toggles.

**Why does the weather show the wrong temperature?**
Check Settings > Dashboard > Temperature unit. The app defaults to Fahrenheit. You can also set a manual location if GPS isn't available.

**Can I use this without Google Play Services?**
Yes. Core alarm features do not require Google Play Services. Weather uses Open-Meteo, Nager.Date, and api.weather.gov depending on enabled features. The F-Droid build variant excludes Play-specific code, including the YouTube downloader, Wear OS Data Layer bridge, Health Connect SDK path, and ML Kit handwriting recognizer; the handwriting challenge still has a typed fallback.

**How does Mission Chaining work?**
In alarm edit, set the "Challenge chain" field to a comma-separated list of challenge types (e.g., `MATH_EASY,SHAKE,TYPING`). The alarm will require you to solve each challenge in order before dismissing.

**What is Guardian Angel?**
If enabled on an alarm, and you don't dismiss within the configured delay (default 5 minutes), the app escalates to your emergency contact. F-Droid builds can auto-send the emergency SMS when you grant `SEND_SMS`, then open the call path. Play builds do not request `SEND_SMS`; they open a prefilled SMS composer and fall back to call or dialer if texting cannot open. `CALL_PHONE` is optional because the app can always fall back to the dialer. Settings > Wake readiness shows active Guardian alarms, the current SMS/call path, and direct recovery actions for missing SMS or call permission.

## Contributing

Issues and PRs welcome. Please open an issue before starting major work to discuss approach.

## License

Apache License 2.0 - see [LICENSE](LICENSE)
