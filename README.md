# AlarmClockXtreme

![Version](https://img.shields.io/badge/version-0.8.1-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-green)
![Platform](https://img.shields.io/badge/platform-Android%2010+-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)
![Status](https://img.shields.io/badge/status-beta-orange)

> A feature-rich, open-source alarm clock for Android with dismiss challenges, weather dashboard, bedtime tracking, and a deep dark theme. No ads, no tracking, no accounts.

<img width="772" height="568" alt="image" src="https://github.com/user-attachments/assets/01e2e354-3905-4dd2-bb86-112282ae1346" />


## Quick Start

```bash
git clone https://github.com/SysAdminDoc/AlarmClockXtreme.git
cd AlarmClockXtreme
./gradlew assembleDebug
# Install: adb install app/build/outputs/apk/play/debug/app-play-debug.apk
```

**Requirements:** Android Studio Ladybug+, JDK 17, Android SDK 35

## Features

| Feature | Description | Status |
|---------|-------------|--------|
| Alarm Engine | setAlarmClock() for maximum reliability, survives Doze | Stable |
| Dismiss Challenges | Math problems, shake detection, memory sequence | Stable |
| Weather Dashboard | Current conditions, forecast via Open-Meteo (free, no API key) | Stable |
| Calendar Integration | Today's events from device calendar | Stable |
| Bedtime Tracking | Sleep goal, suggested bedtime, daily reminders | Stable |
| Timer & Stopwatch | Countdown timer with notification, stopwatch with laps | Stable |
| Home Widget | Glance-based widget showing next alarm countdown | Stable |
| Vacation Mode | Date range auto-skip for repeating alarms | Stable |
| Templates | Power Nap, Early Bird, Weekday, Weekend presets | Stable |
| Backup/Restore | JSON export/import of all alarms and settings | Stable |
| Statistics | Dismiss streaks, snooze rate, day-of-week breakdown | Stable |
| Skip Next | Skip one occurrence of a repeating alarm | Stable |
| Persistent Notification | Always-visible next alarm countdown in shade | Stable |
| Ringtone Picker | Browse and preview system ringtones | Stable |
| Swipe to Delete | Swipe alarm cards with undo snackbar | Stable |
| Search & Sort | Filter alarms, sort by time/created/enabled | Stable |
| Auto-Silence | Configurable timeout (0/5/10/15/30 min) | Stable |
| Temperature Units | Fahrenheit / Celsius toggle | Stable |
| 24-Hour Format | Respects system or manual override | Stable |

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                     │
│  Screens ← ViewModels ← StateFlow                       │
├─────────────────────────────────────────────────────────┤
│                   Domain Layer                           │
│  AlarmScheduler │ NextAlarmCalculator                    │
├─────────────────────────────────────────────────────────┤
│                    Data Layer                             │
│  Room DB │ DataStore │ Retrofit (Open-Meteo)             │
├─────────────────────────────────────────────────────────┤
│                   Android Platform                       │
│  AlarmManager │ ForegroundService │ BroadcastReceivers   │
└─────────────────────────────────────────────────────────┘
```

**Tech stack:** Kotlin 2.1, Jetpack Compose (Material 3), Room, Hilt, Retrofit + Moshi (codegen), DataStore, Glance widgets, Coroutines/Flow

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
| `SCHEDULE_EXACT_ALARM` | Fire alarms at exact time | Yes |
| `POST_NOTIFICATIONS` | Show alarm alerts | Yes |
| `FOREGROUND_SERVICE` | Reliable alarm playback | Yes |
| `RECEIVE_BOOT_COMPLETED` | Reschedule after reboot | Yes |
| `WAKE_LOCK` | Keep screen on during alarm | Yes |
| `ACCESS_COARSE_LOCATION` | Weather for your area | Optional |
| `READ_CALENDAR` | Today's events on dashboard | Optional |
| `VIBRATE` | Alarm vibration | Yes |

## Privacy

No analytics. No ads. No tracking. No accounts. No data leaves your device except weather API calls to Open-Meteo (latitude/longitude only). Full privacy policy: [PRIVACY_POLICY.html](PRIVACY_POLICY.html)

## FAQ

**Why does the alarm not fire on my Xiaomi/Samsung/Huawei?**
These manufacturers aggressively kill background apps. The app shows a manufacturer-specific warning during onboarding with steps to whitelist it. Generally: Settings > Battery > App Launch > AlarmClockXtreme > Manual > enable all toggles.

**Why does the weather show the wrong temperature?**
Check Settings > Dashboard > Temperature unit. The app defaults to Fahrenheit. You can also set a manual location if GPS isn't available.

**Can I use this without Google Play Services?**
Yes. The app has zero Google dependencies. Weather uses Open-Meteo (free, open-source). The F-Droid build variant excludes any Play-specific code.

## Contributing

Issues and PRs welcome. Please open an issue before starting major work to discuss approach.

## License

Apache License 2.0 - see [LICENSE](LICENSE)
