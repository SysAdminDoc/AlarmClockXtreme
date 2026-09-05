# AlarmClockXtreme

[![Version](https://img.shields.io/badge/version-1.15.35-5CA6FF)](https://github.com/SysAdminDoc/AlarmClockXtreme/releases/latest)
[![License](https://img.shields.io/badge/license-Apache%202.0-65DDB9)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://github.com/SysAdminDoc/AlarmClockXtreme/releases/latest)
[![Stars](https://img.shields.io/github/stars/SysAdminDoc/AlarmClockXtreme?style=flat&color=FFD166)](https://github.com/SysAdminDoc/AlarmClockXtreme/stargazers)
[![Downloads](https://img.shields.io/github/downloads/SysAdminDoc/AlarmClockXtreme/total?color=8EA8C9)](https://github.com/SysAdminDoc/AlarmClockXtreme/releases)

<p align="center">
  <img src="assets/marketing/hero.png" width="100%" alt="AlarmClockXtreme, an Android alarm clock with exact scheduling and wake-up challenges" />
</p>

AlarmClockXtreme is an open-source Android alarm clock built for heavy sleepers and shift workers. It also suits anyone who wants more control than the stock clock provides. Exact scheduling handles the wake-up. Thirty dismiss challenges help make sure you stay awake.

**No ads or account. Nothing tracks you.**

<p align="center">
  <a href="assets/screenshots/alarm-list.png"><img src="assets/screenshots/alarm-list.png" width="18%" alt="Alarm list with three weekday alarms and a math challenge" /></a>
  <a href="assets/screenshots/today.png"><img src="assets/screenshots/today.png" width="18%" alt="Today dashboard with New York weather and air quality" /></a>
  <a href="assets/screenshots/alarm-editor.png"><img src="assets/screenshots/alarm-editor.png" width="18%" alt="Alarm editor with weekday schedule and math challenge" /></a>
  <a href="assets/screenshots/timer.png"><img src="assets/screenshots/timer.png" width="18%" alt="Running timer alongside a new timer keypad" /></a>
  <a href="assets/screenshots/settings.png"><img src="assets/screenshots/settings.png" width="18%" alt="Settings overview with wake readiness and preference categories" /></a>
</p>

## Get AlarmClockXtreme

Download the signed APKs and matching checksums from the [latest release](https://github.com/SysAdminDoc/AlarmClockXtreme/releases/latest).

| Package | Best for | What it includes |
|---|---|---|
| Play APK | Most phones | YouTube alarm sounds, Health Connect, handwriting recognition, and the Wear bridge |
| F-Droid APK | Fully open-source installs | The complete alarm engine without Play-specific libraries |
| Wear APK | Paired Wear OS watches | Next-alarm tile, complication, skip, snooze, and dismiss controls |

Install the Play build over ADB:

```bash
adb install AlarmClockXtreme-v1.15.35-play-release.apk
```

Each release includes `SHA256SUMS.txt` and certificate fingerprints so you can verify the file before installing it.

## Why People Choose It

### Alarms that take reliability seriously

AlarmClockXtreme uses Android's exact alarm path, restores schedules after reboot, and keeps a minimal Direct Boot fallback for the next alarm. A wake-readiness check shows which system settings could get in the way before bedtime.

- Smart wake can watch for light movement before the scheduled time.
- Wake confirmation rings again if you dismiss an alarm and fall back asleep.
- Rotating shifts support DDNNO, 4-on-4-off, Panama, DuPont, and Pitman cycles.
- Vacation mode and public-holiday skips pause the right alarms without deleting them.

### Thirty ways to prove you are awake

Choose a quick math problem or build a chain of challenges. The list also includes typing, memory, walking, squats, push-ups, NFC, barcode, Wi-Fi, photo match, a maze, Wordle, and more. An accessibility bypass can unlock dismissal after a chosen delay when a physical challenge is not suitable.

### Sound and automation that fit your routine

Use a system ringtone, an HTTPS radio stream, Spotify, or a random pool of saved sounds. The Play build can search YouTube or accept a link and save the audio to the phone's alarm library. Webhooks, Tasker broadcasts, and Philips Hue scenes can run when an alarm fires or is dismissed.

### Useful after the alarm stops

The Today tab combines local weather with air quality and sunrise timing. You also get concurrent timers, a stopwatch, world clocks, bedtime reminders, guided breathing, local sleep summaries, widgets, and Wear OS controls.

## Highlights

| Area | What you can do |
|---|---|
| Scheduling | Exact alarms, date-specific alarms, solar offsets, shift patterns, profiles, groups, manual order, and shareable alarm links |
| Wake-up | Thirty challenge types, mission chains, adaptive math, progressive snooze, hold-to-dismiss, wake confirmation, and a guardian contact |
| Sound | Gradual volume, vibration patterns, ringtone pools, Spotify, HTTPS radio, YouTube audio in the Play build, and haptic-only alarms |
| Context | Weather, air quality, NWS alerts in the US, calendar events, public holidays, sunrise and sunset, plus optional RSS news |
| Bedtime | Sleep goal, wind-down checklist, breathing timer, night clock, chronotype estimate, local Sonar summaries, and Health Connect in the Play build |
| Reliability | Reboot restore, Direct Boot fallback, missed-alarm watchdog, wake-readiness checks, local diagnostics, and encrypted backup |
| Devices | Phones, tablets, foldables, Chromebooks, DeX, home-screen widgets, Quick Settings, and Wear OS |

## Privacy

The alarm engine, challenge state, schedules, statistics, and settings stay on your device. No analytics SDK or ad network is included, and the app has no account system.

Network access is optional and tied to features you choose. Weather uses Open-Meteo. Public-holiday skips use Nager.Date, US alerts use the National Weather Service, and the radar card loads Windy. RSS feeds, internet radio, webhooks, Philips Hue, and the Play-only YouTube downloader contact the service you configure.

Sonar tracking keeps compact movement and loud-sound summaries for 30 days. It does not save raw audio. Crash logs are capped, app-private files. They are never uploaded automatically and only leave the phone when you export a support bundle.

See the full [privacy policy](PRIVACY_POLICY.html) for the exact data and network boundaries.

## Build From Source

Requirements:

- Android Studio or the included Gradle wrapper
- JDK 17 or 21
- Android SDK 36

```bash
git clone https://github.com/SysAdminDoc/AlarmClockXtreme.git
cd AlarmClockXtreme
./gradlew :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug
```

Debug outputs:

```text
app/build/outputs/apk/play/debug/app-play-debug.apk
app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
wear/build/outputs/apk/debug/wear-debug.apk
```

The Play and F-Droid phone builds share the same alarm engine. The F-Droid flavor omits the YouTube downloader, Health Connect SDK path, Play Data Layer bridge, and ML Kit handwriting recognizer.

## Release Signing

Copy `keystore.properties.template` to `keystore.properties`, point it at your release keystore, then provide the password and alias values. Release tasks fail closed when the signing material is missing.

```bash
./gradlew verifyReleaseMetadata verifyDependencyIntegrity :app:verifyRoomSchemaExports
python scripts/osv_gradle_audit.py
./gradlew :app:bundlePlayRelease :app:assemblePlayRelease :app:assembleFdroidRelease :wear:assembleRelease
```

Verify an APK against the certificate fingerprint published with the release:

```bash
apksigner verify --print-certs AlarmClockXtreme-v1.15.35-play-release.apk
```

For Android 17 and 16 KB page-size testing, run:

```bash
python scripts/verify_api37_release.py --device <emulator-serial> --run-test-alarm --fresh-install
```

## Permissions

AlarmClockXtreme asks only when a feature needs access.

| Permission or access | Used for |
|---|---|
| Alarms and reminders | Exact wake-up scheduling |
| Notifications and full-screen alarms | Showing the ringing screen and alarm controls |
| Battery optimization exemption | Reducing missed alarms on aggressive Android builds |
| Calendar and location | Optional Today dashboard, commute timing, weather, and location dismissal |
| Microphone, camera, NFC, and activity recognition | Optional voice, Sonar, photo, barcode, tag, walking, and exercise challenges |
| Do Not Disturb | Optional bedtime and on-call alarm rules |
| Health Connect | Optional Play-build sleep summaries |
| SMS or phone | Optional guardian escalation, with behavior that differs by flavor |

Basic alarms, local challenges, timers, and backups work without enabling the optional network or sensor features.

## Webhooks and Tasker

Webhooks can fire on `alarm_fired`, `alarm_snoozed`, `alarm_dismissed`, `alarm_missed`, or `alarm_skipped`. HTTPS is required. Optional HMAC-SHA256 signing adds `X-ACX-Timestamp` and `X-ACX-Signature` headers.

Example payload:

```json
{
  "schemaVersion": 1,
  "event": "alarm_dismissed",
  "alarmId": 42,
  "label": "Work Alarm",
  "time": "7:00 AM",
  "timestamp": 1788591600000
}
```

For local automation, choose a package-scoped broadcast dismiss action and target your Tasker or MacroDroid package. The app validates the package and action before saving it.

## FAQ

### Will it work offline?

Yes. Scheduling, local sounds, challenges, timers, backups, and most bedtime tools work offline. Weather, alerts, RSS, radio, Hue, webhooks, and YouTube audio need a connection.

### Why does Android show several reliability settings?

Phone makers can restrict background work differently. Wake readiness puts the relevant switches in one place and explains which ones matter on the current device.

### Can I move my alarms to another phone?

Yes. Export a JSON backup, or protect it with AES-256 encryption and a passphrase. Import previews the file before changing your alarms.

### Does it claim to measure sleep stages?

No. Smart wake and Sonar provide local motion or sound summaries. They are not medical sleep-stage measurements.

### Is the F-Droid build reduced?

It keeps the full scheduling, challenge, weather, bedtime, timer, backup, and automation core. Only libraries tied to Play distribution or proprietary SDKs are removed.

## Contributing

Bug reports and focused pull requests are welcome. Please include the Android version, phone model, app flavor, and clear reproduction steps for alarm-delivery issues.

## License

[Apache License 2.0](LICENSE)
