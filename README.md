# AlarmClockXtreme

![Version](https://img.shields.io/badge/version-1.5.4-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-green)
![Platform](https://img.shields.io/badge/platform-Android%2010+-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)

> A feature-rich, open-source alarm clock for Android with 47 features, 15 dismiss challenges, smart wake intelligence, and a deep dark theme. No ads, no tracking, no accounts.

<img width="772" height="568" alt="image" src="https://github.com/user-attachments/assets/01e2e354-3905-4dd2-bb86-112282ae1346" />

## Quick Start

```bash
git clone https://github.com/SysAdminDoc/AlarmClockXtreme.git
cd AlarmClockXtreme
./gradlew assemblePlayDebug
# Install: adb install app/build/outputs/apk/play/debug/app-play-debug.apk
```

**Requirements:** Android Studio Ladybug+, JDK 17, Android SDK 35

## Features

### Core Alarm Engine
| Feature | Description |
|---------|-------------|
| Reliable Scheduling | `setAlarmClock()` for maximum reliability, survives Doze mode |
| Solar-Relative Firing | Fire relative to sunrise/sunset with a configurable offset (NOAA approximation) |
| Alarm Groups | Tag alarms (Work, School, Gym), filter with chips |
| Alarm Profiles | Named configurations (Work, Travel, Weekend) for quick switching |
| Date-Specific Alarms | Set alarm for a particular calendar date (overrides repeat days) |
| Duplicate Alarm | Clone any alarm with all settings via overflow menu |
| Skip Next | Skip one occurrence of a repeating alarm |
| Vacation Mode | Date range auto-skip for all repeating alarms |
| Holiday Auto-Skip | Skip public holidays via Nager.Date API (40+ countries) |
| Templates | Power Nap, Early Bird, Weekday, Weekend presets |
| Shareable Alarms | Share a single alarm as an `acx://alarm?data=` link; imports are disabled until reviewed |
| Early Dismiss | Skip upcoming alarm from the persistent notification |
| Calendar Auto-Alarm | Auto-create alarm before first calendar event daily |

### Dismiss Challenges (19 Types)
| Challenge | Description |
|-----------|-------------|
| Math (Easy/Medium/Hard) | Solve arithmetic problems with explicit operator precedence |
| Shake Phone | Shake device N times (configurable) |
| Number Sequence | Tap 6 numbers in ascending order |
| Memory Pattern | Memorize and recreate a tile pattern on 3x3 grid |
| Type a Phrase | Type a displayed phrase exactly (custom phrases supported) |
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
| Mission Chaining | Stack 2-5 challenges in sequence (e.g., Math + Shake + Typing) |
| Adaptive Difficulty | Auto-escalates math difficulty based on snooze history |

### Anti-Snooze Arsenal
| Feature | Description |
|---------|-------------|
| Progressive Snooze | Each snooze shortens by 1 minute (10 -> 9 -> 8 -> ...) |
| Backup Sound Escalation | Ultra-loud volume boost if no interaction within configurable delay |
| Max Snooze Count | Auto-dismiss after N snoozes reached |
| Guardian Angel | Emergency contact SMS + phone call if alarm not dismissed |
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
| Swipe Gestures | Swipe right to dismiss, left to snooze |
| Custom Snooze | Pick 1/3/5/15/30 minute snooze from firing screen |

### Sound & Vibration
| Feature | Description |
|---------|-------------|
| Ringtone Picker | Browse and preview system ringtones |
| Random Ringtone Pool | Per-alarm pool of URIs; a random one is picked each fire (anti-habituation) |
| Dismiss at Ringtone End | Auto-dismiss when the chosen song/tone finishes (Spotify-friendly) |
| Spotify Integration | Play Spotify tracks/playlists as alarm sound |
| Internet Radio | Stream any HTTP/HTTPS radio station URL |
| Gradual Volume | Configurable fade-in (15s to 5 min) |
| Custom Vibration | 5 patterns: Default, Gentle, Heartbeat, Escalating, SOS |
| Silent Mode | Fire alarm with notification only, no sound |

### Smart Features
| Feature | Description |
|---------|-------------|
| Smart Alarm | Accelerometer-based light sleep detection, fires early during optimal window |
| Sonar Sleep Tracking | Experimental ultrasonic breathing/movement detection |
| Philips Hue Sunrise | Gradually ramp smart lights before alarm fires |
| Webhook / Tasker | POST JSON on alarm fire/snooze/dismiss/miss events |
| Flip-to-Snooze | Place phone face-down to snooze |

### Dashboard & Utilities
| Feature | Description |
|---------|-------------|
| Weather Dashboard | Current conditions + 3-day forecast via Open-Meteo (free, no API key) |
| Calendar Integration | Today's events from device calendar |
| World Clock | Live time zones with UTC offset, 24h format support |
| Multiple Timers | Run several countdown timers concurrently (monotonic clock) |
| Stopwatch | Lap tracking with best/worst marking |
| Bedtime Tracking | Sleep goal, sleep cycle calculator, bedtime reminders, sleep sounds |
| Statistics | Dismiss streaks, snooze rate, day-of-week breakdown, response times, searchable alarm history |
| Night Clock | Always-on bedside display with minimal brightness |
| Home Widget | Glance-based widget showing next alarm countdown |
| Persistent Notification | Always-visible next alarm countdown in shade |
| Quick Settings Tile | Skip the next alarm from the system shade with one tap |
| Accent Color | Customizable accent color within dark theme |
| Material You | Opt-in dynamic color from wallpaper palette (Android 12+) |
| Wind-Down Checklist | Pre-sleep checklist rendered on the Bedtime tab |
| Configurable Sleep Timer | Sleep-sound fade-out with 5s-10min taper and configurable hold |

### Data & Reliability
| Feature | Description |
|---------|-------------|
| Backup/Restore | JSON export/import of all 47+ alarm fields and 34+ settings, with optional AES-256 passphrase encryption (v5 format) |
| Boot Reschedule | All alarms re-registered after device reboot |
| Manufacturer Compat | Onboarding warnings for Xiaomi/Samsung/Huawei battery killers |
| Crash Logger | Automatic crash log files for debugging |
| Auto-Silence | Configurable timeout (0/5/10/15/30 min), records as missed |

## Architecture

```
+---------------------------------------------------------+
|                    UI Layer (Compose)                     |
|  Screens <- ViewModels <- StateFlow                      |
|  15 challenge views, 8 alarm edit sections               |
+---------------------------------------------------------+
|                   Domain Layer                           |
|  AlarmScheduler | NextAlarmCalculator                    |
|  Date-specific + holiday + vacation skip logic           |
+---------------------------------------------------------+
|                    Data Layer                            |
|  Room DB v6 | DataStore | Retrofit (Open-Meteo, Nager)  |
|  47-field Alarm entity | 34-field AppSettings            |
+---------------------------------------------------------+
|                   Android Platform                       |
|  AlarmManager | 3 ForegroundServices | 5 Workers         |
|  5 BroadcastReceivers | Glance Widget                   |
+---------------------------------------------------------+
```

**Tech stack:** Kotlin 2.1, Jetpack Compose (Material 3), Room, Hilt, Retrofit + Moshi (codegen), DataStore, Glance widgets, OkHttp, Coroutines/Flow, WorkManager

## Configuration

### Signing

1. Generate a keystore: `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias alarm`
2. Copy `keystore.properties.template` to `keystore.properties`
3. Fill in your keystore path and credentials
4. Build: `./gradlew assembleRelease`

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
| `FOREGROUND_SERVICE` | Reliable alarm playback | Yes |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Alarm audio | Yes |
| `FOREGROUND_SERVICE_DATA_SYNC` | Smart alarm monitoring | Yes |
| `FOREGROUND_SERVICE_MICROPHONE` | Sonar sleep tracking | Yes |
| `RECEIVE_BOOT_COMPLETED` | Reschedule after reboot | Yes |
| `WAKE_LOCK` | Keep CPU during alarm | Yes |
| `VIBRATE` | Alarm vibration | Yes |
| `INTERNET` | Weather, holidays, webhooks, radio | Yes |
| `ACCESS_COARSE_LOCATION` | Weather for your area | Optional |
| `READ_CALENDAR` | Dashboard events + auto-alarm | Optional |
| `NFC` | NFC tag dismiss challenge | Optional |
| `CAMERA` | Barcode scan + photo match challenges | Optional |
| `RECORD_AUDIO` | Sonar sleep tracking | Optional |
| `ACTIVITY_RECOGNITION` | Walk steps + smart alarm | Optional |
| `SEND_SMS` / `CALL_PHONE` | Guardian Angel emergency contact | Optional |

## Privacy

No analytics. No ads. No tracking. No accounts. No data leaves your device except:
- Weather API calls to Open-Meteo (latitude/longitude only)
- Holiday API calls to Nager.Date (country code only)
- Webhook calls to your own configured URL
- Internet radio streaming to your configured station

Full privacy policy: [PRIVACY_POLICY.html](PRIVACY_POLICY.html)

## FAQ

**Why does the alarm not fire on my Xiaomi/Samsung/Huawei?**
These manufacturers aggressively kill background apps. The app shows a manufacturer-specific warning during onboarding with steps to whitelist it. Generally: Settings > Battery > App Launch > AlarmClockXtreme > Manual > enable all toggles.

**Why does the weather show the wrong temperature?**
Check Settings > Dashboard > Temperature unit. The app defaults to Fahrenheit. You can also set a manual location if GPS isn't available.

**Can I use this without Google Play Services?**
Yes. The app has zero Google dependencies. Weather uses Open-Meteo (free, open-source). The F-Droid build variant excludes any Play-specific code.

**How does Mission Chaining work?**
In alarm edit, set the "Challenge chain" field to a comma-separated list of challenge types (e.g., `MATH_EASY,SHAKE,TYPING`). The alarm will require you to solve each challenge in order before dismissing.

**What is Guardian Angel?**
If enabled on an alarm, and you don't dismiss within the configured delay (default 5 minutes), the app sends an SMS and attempts to call your emergency contact. Requires SEND_SMS and CALL_PHONE permissions.

## Contributing

Issues and PRs welcome. Please open an issue before starting major work to discuss approach.

## License

Apache License 2.0 - see [LICENSE](LICENSE)
