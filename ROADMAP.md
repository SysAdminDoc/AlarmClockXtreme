# AlarmClockXtreme Roadmap

Actionable work only. Historical and completed roadmap material is archived in CHANGELOG.md; blocked work is kept in Roadmap_Blocked.md.

## Actionable Items

---

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-H2 | Paired-phone LAN sync (partner-dismiss → you snooze). Strict privacy: LAN-only, no cloud. | — | M |
| L-H3 | Kid-friendly green-light mode. | [OK to Wake](https://www.amazon.com/dp/B003O15A1G), [Hatch](https://www.hatch.co/) | M |
| L-H4 | Pet-feeding reminder chain on dismiss. | — | S |
| L-H5 | Remote parental alarm set. | [Google Family Link](https://families.google.com/familylink/) | L |
| L-H6 | Synchronized alarm groups — edit one, propagate to siblings sharing a label. **NEW.** | [BlackyHawky Clock 2.29](https://github.com/BlackyHawky/Clock/releases) | M |

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-R1 | Gratitude / journal prompt on dismiss. | [Day One](https://dayoneapp.com/), [Stoic](https://www.getstoic.com/) | S |
| L-R2 | Water-intake quick-log tiles. | [WaterMinder](https://waterminder.com/) | S |
| L-R3 | Mood selfie + emoji tag. | [Daylio](https://daylio.net/) | S |
| L-R4 | Obsidian / Notion / Markdown daily-note append. | [TaskForge.md](https://taskforge.md/android/); [Notelert Obsidian forum](https://forum.obsidian.md/t/notelert-native-android-notification-and-reminders-for-obsidian/109310) | M |
| L-R5 | Health Connect weight / BP / mood quick-entry. | [Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types) | S |
| L-R7 | Badge set: "5 AM club", "no-snooze week", "DDNNO survivor". | [Habitica](https://habitica.com/) | S |
| L-R8 | Share-card screenshot generator (local — no social-feed; matches REJECTED stance). | [Strava](https://www.strava.com/) | S |

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-A1 | Binaural / isochronic delta (0.5-4 Hz) tone generator. | [Brain.fm](https://brain.fm/), [myNoise](https://mynoise.net/) | M |
| L-A3 | Voice-memo ringtone (in-app 30 s recorder). | iOS-native pattern | S |
| L-A4 | Podcast latest-episode (Podcast Index / AntennaPod URI). | [AntennaPod](https://github.com/AntennaPod/AntennaPod); [AntennaPod alarm-clock feature request](https://forum.antennapod.org/t/alarmclock-function-in-anthennapod/4418) | M |
| L-A5 | Per-alarm Bluetooth sink (specific A2DP / LE Audio device). | [BlackyHawky Clock 2.22 BT routing](https://github.com/BlackyHawky/Clock/releases) | M |
| L-A6 | Chromecast / Nest Hub alarm target. | [Cast SDK](https://developers.google.com/cast/docs/android_sender) | M |
| L-A7 | UPnP / DLNA multi-room cast escalation. | [Cling](https://github.com/4thline/cling) | L |
| L-A8 | Folder-based ringtone import — point at a directory, expose its files in the picker. **NEW.** | [BlackyHawky Clock 2.23](https://github.com/BlackyHawky/Clock/releases) | S |
| L-A10 | Pre-alarm low-volume gentle wake — separate alarm 30 min before main alarm, designed to lift you out of deep sleep. **NEW.** | [yuriykulikov/AlarmClock](https://github.com/yuriykulikov/AlarmClock) signature feature | M |

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-D1 | Islamic prayer-time Fajr alarm via Aladhan. | [Aladhan API](https://aladhan.com/prayer-times-api); [Al-Azan](https://f-droid.org/packages/com.github.meypod.al_azan/) | M |
| L-D2 | Lunar / Hebrew / Hindu calendar repeat. | — | M |
| L-D3 | Astronomical events (meteor-shower peak, ISS flyover). | [Heavens-Above](https://www.heavens-above.com/) | M |
| L-D4 | Birthday auto-alarm from Contacts. | Android Contacts provider | S |
| L-D5 | Menstrual-cycle aware (softer alarm in luteal phase). | [Health Connect MenstruationFlowRecord](https://developer.android.com/reference/androidx/health/connect/client/records/MenstruationFlowRecord) | M |
| L-D6 | Weather-conditional firing (fire earlier on snow > 2 cm). | [Open-Meteo](https://open-meteo.com/) | M |
| L-D7 | Calendar OOO-aware "skip tomorrow?" suggestion. **NEW.** | inferred from existing CalendarRepository + holiday skip patterns | S |

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-P3 | Emergency-escalation call tree (SMS → call → partner → siren). | [Twilio](https://www.twilio.com/) / native | M |
| L-P4 | Location-based escalation (still at home after dismiss → siren). | FusedLocation; partial in code via `locationDismissEnabled` fields | M |
| L-P5 | Car-mode suppression (Android Auto `CarConnection` API; receive Google's new in-car alarm pop-up). | [Android Auto](https://developer.android.com/training/cars); [Android Auto in-car alarm controls 16.8](https://www.autoevolution.com/news/android-auto-is-getting-the-feature-users-first-asked-for-10-years-ago-269408.html) | S |
| L-P6 | Companion-watch autonomous fire if phone battery dies. | — | M |
| L-P7 | Charging-only alarm variant. | — | S |

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-C1 | Google Drive / Nextcloud / WebDAV backup via SAF (opt-in; encryption already exists). | [SAF docs](https://developer.android.com/guide/topics/providers/document-provider); [SeedVault](https://nlnet.nl/project/SeedVault-Integrity/) for inspiration | M |
| L-C2 | End-to-end encrypted paired-phone LAN sync. | — | L |

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-SH1 | Matter 1.6 Dynamic Lighting (DLE) cross-brand sunrise — extends Hue path to any Matter bulb without per-brand workarounds. **NEW.** | [Matter 1.6 DLE 2026](https://mattressmiracle.ca/blogs/mattress-miracle-blog/matter-1-6-dynamic-lighting-sunrise-gradient-bedroom); [Matter Innovations CES 2026](https://matter-smarthome.de/en/products/the-matter-innovations-at-ces-2026/); [Google Home Matter dev docs](https://developers.home.google.com/matter) | L |

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-U1 | Always-On Display-aware Night Clock (uses AOD API rather than full-bright service). | [Android AOD docs](https://developer.android.com/training/wearables/watch-faces/ambient-mode) | S |
| L-U2 | Dynamic color from a specific wallpaper accent rather than the full palette. | — | S |
| L-U3 | Interactive onboarding walkthrough (per-feature highlights). | — | M |
| L-U4 | Predictive-back progress on alarm-edit unsaved-changes dialog (`PredictiveBackHandler`). | [Compose predictive back](https://developer.android.com/develop/ui/compose/system/predictive-back) | S |
| L-U5 | Per-app language picker (`LocaleManager`). Prereq for community translation. | [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages) | S |
| L-U6 | Ultra-HDR sunrise rendering on Android 14+. | [Ultra HDR rendering](https://developer.android.com/about/versions/14/features#ultra-hdr) | S |
| L-U7 | Credential Manager + passkey-gated cloud backup. | [Credential Manager](https://developer.android.com/training/sign-in/passkeys) | M |
| L-U8 | Roman-numeral / additional analog Night Clock face styles. **NEW.** | [BlackyHawky Clock 2.29](https://github.com/BlackyHawky/Clock/releases) | S |

| # | Item | Source | Effort |
|---|------|--------|--------|
| L-X3 | TalkBack audit — large double-tap buttons on firing screen. | [Android accessibility overview](https://support.google.com/accessibility/android/answer/6006564) | S |
| L-X4 | Pure-black / mono-color WCAG AAA high-contrast theme. | [WCAG 2.2 / 2.1 AAA](https://www.w3.org/WAI/WCAG22/quickref/) | S |
| L-X6 | Per-user long-press thresholds on challenge buttons. | Android a11y guidelines | S |

- [ ] **Try-catch every `AlarmManager.set*` call.** `setInexactAllowWhileIdle` can still throw if the device's exact-alarm fallback path engages. ([flutter_local_notifications #2248](https://github.com/MaikuB/flutter_local_notifications/issues/2248))

- [ ] **Android 15 short-type FGS auto-timeout (3 min cap).** Stay on `mediaPlayback` type, do NOT migrate to `shortService`. ([Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15))

- [ ] **Doze defers even `setAlarmClock()` 1-2 min on Redmi/Samsung.** Pair with a 10-15 s `PARTIAL_WAKE_LOCK` in `onReceive`; keep within ANR ceiling. ([Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby))

- [ ] **`setAlarmClock()` always shows status-bar icon.** Already mitigated with a settings toggle that falls back to `setExactAndAllowWhileIdle` (with disclaimer); keep the toggle in the UI.

- [ ] **Android 16 "missed alarm — unknown reason" notification regression on Pixel.** Track the QPR fix and confirm ACX's foreground-service start path is not the cause. ([Android Police Pixel alarm bug](https://www.androidpolice.com/pixel-alarm-bug-is-back/))

- [ ] **Play wake-lock policy (March 2026).** N4 covers the audit; keep the wake-lock acquisition window inside the 2 h / 24 h non-exempt budget. ([9to5Google March 2026](https://9to5google.com/2026/03/05/google-starts-calling-out-android-apps-that-drain-your-battery-before-you-download-them/))

- [ ] Hue v1 username endpoints are deprecated — **migrate `HueSunriseWorker` to v2 `application_key` + HTTPS pinning. Tracked as N5 above.** ([Philips Hue API v2](https://developers.meethue.com/new-hue-api/))
- [ ] Add a "share crash log" button on the About screen (does not auto-upload — copies to clipboard or invokes share sheet). **S, not yet tiered.**

- [ ] Two flavors today: `play` (with YT downloader + Wear Data Layer), `fdroid` (without). Maintain parity on every other surface. Build, test, signing, OSV audit, release artifact creation, and SHA-256 generation happen locally; do not add GitHub Actions.

- [ ] AAB for Play Store, signed APK for GitHub Releases; never ship unsigned artifacts.

- [ ] F-Droid users expect APK under **~40 MB**. Any TFLite-model or Matter-SDK work must respect this budget (downloadable models, not bundled).

- [ ] English-only today. **Per-app language picker (L-U5) lands first**, THEN community translation. No machine-translation-only strings — better to remain English than ship broken translations.

- [ ] Room migration tests: every schema bump requires a migration test path in `AlarmDatabaseMigrationTest`; CI also runs `git diff --exit-code -- app/schemas` after debug builds to catch uncommitted exports (whakaara discipline — [ahudson20/whakaara](https://github.com/ahudson20/whakaara)).

- [ ] Remaining alarm-fire proof gap: add a device/emulator smoke that fires through AlarmManager/test broadcast and asserts the firing window shows over lock screen. **S, not yet tiered.**

- [ ] README, CHANGELOG, ROADMAP, and the version badge must all match on every release. **N10 makes this enforced in CI instead of manual.**

- [ ] Webhooks (Tasker / MacroDroid / Home Assistant) cover the integration surface we want to expose. A "real" plugin SDK is rejected (UC) until webhook gaps are documented.
