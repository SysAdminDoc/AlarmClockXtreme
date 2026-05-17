# Security And Dependency Review

Date: 2026-05-17

## Dependency Snapshot

Resolved Play release runtime classpath was inspected with:

```powershell
.\gradlew.bat :app:dependencies --configuration playReleaseRuntimeClasspath --console=plain
```

Resolved F-Droid release runtime classpath was inspected with:

```powershell
.\gradlew.bat :app:dependencies --configuration fdroidReleaseRuntimeClasspath --console=plain
```

Play flavor includes:

- `io.github.junkfood02.youtubedl-android:library:0.18.1`
- `com.github.teamnewpipe:NewPipeExtractor:v0.24.8`

F-Droid flavor excludes the downloader path.

## OSV Findings

`osv-scanner scan source -r .` did not produce useful results for this Gradle
Kotlin DSL repository. A manual OSV batch query was run against resolved
Play-flavor transitive dependencies.

| Package | Version | OSV Result |
|---|---:|---|
| `com.fasterxml.jackson.core:jackson-databind` | 2.11.1 | 4 advisories: `GHSA-3x8x-79m2-3w2w`, `GHSA-57j2-w4cx-62h2`, `GHSA-jjjh-jjxp-wpff`, `GHSA-rgv9-q543-rqg4` |
| `com.fasterxml.jackson.core:jackson-core` | 2.11.1 | 3 advisories: `GHSA-72hv-8253-57qq`, `GHSA-h46c-h94j-95f3`, `GHSA-wf8f-6423-gfxg` |
| `org.apache.commons:commons-compress` | 1.12 | 7 advisories: `GHSA-4g9r-vxhx-9pgx`, `GHSA-7hfm-57qf-j43q`, `GHSA-crv7-7245-f45f`, `GHSA-h436-432x-8fvx`, `GHSA-hrmr-f5m6-m9pq`, `GHSA-mc84-pj99-q6hh`, `GHSA-xqfj-vm6h-2x34` |
| `commons-io:commons-io` | 2.5 | 2 advisories: `GHSA-78wr-2p64-hpwj`, `GHSA-gwrp-pvrq-jmwv` |
| `org.mozilla:rhino` | 1.8.0 | 1 advisory: `GHSA-3w8q-xq97-5j7x` |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | No advisories returned in the queried batch. |
| `org.jsoup:jsoup` | 1.21.1 | No advisories returned in the queried batch. |

Important source examples:

- OSV `GHSA-4g9r-vxhx-9pgx` says Commons Compress versions through 1.25.0 are
  affected and recommends upgrading to 1.26.0.
- OSV `GHSA-78wr-2p64-hpwj` says Commons IO before 2.14.0 is affected.

## R5 Mitigation Implemented

The 2026-05-17 dependency-hardening pass added Play-only dependency constraints
and an OSV audit script:

- `com.fasterxml.jackson.core:jackson-databind/core/annotations:2.18.6`
- `org.apache.commons:commons-compress:1.28.0`
- `commons-io:commons-io:2.20.0`
- `org.mozilla:rhino` and `org.mozilla:rhino-engine:1.8.1`
- `org.tukaani:xz:1.10` as a Play-only support dependency required by the
  constrained Commons Compress release during Android release shrinking.

`scripts/osv_gradle_audit.py` resolves a Gradle configuration, extracts Maven
coordinates, queries `https://api.osv.dev/v1/querybatch`, and fails when OSV
returns vulnerabilities. `.github/workflows/android-ci.yml` now runs it against
`playReleaseRuntimeClasspath`.

Verification after the constraints:

```powershell
python scripts\osv_gradle_audit.py --configuration playDebugRuntimeClasspath
python scripts\osv_gradle_audit.py --configuration playReleaseRuntimeClasspath
.\gradlew.bat :app:assemblePlayRelease --console=plain
.\gradlew.bat :app:testPlayDebugUnitTest :app:assemblePlayDebug :app:assembleFdroidDebug :wear:assembleDebug --console=plain
```

Both OSV audits reported no vulnerabilities for the resolved Maven dependency
sets. The Play release build passed with a temporary local signing key after
adding the XZ support dependency and suppressing the unused optional Commons
Compress Zstandard warning.

## Dependency Recommendations

1. Keep the OSV dependency-audit job green on every pull request and push to
   `main`.
2. If future constraints break `youtubedl-android`, isolate downloader
   execution away from alarm-critical code and document the risk.
3. Evaluate replacing or augmenting the downloader path with a maintained
   yt-dlp binary/update model for the Play flavor.
4. Add automated dependency update tooling.

## Release Security

`.github/workflows/release.yml` was repaired on 2026-05-17. Tag builds now
require signing secrets, build signed `playRelease`, `fdroidRelease`, and Wear
release APKs, verify signatures and APK badging, upload workflow artifacts, and
attach APKs plus `SHA256SUMS.txt` to GitHub Releases.

Remaining release-security recommendation: consider publishing artifact
certificate fingerprints in release notes.

## Privacy And Data Safety

`PRIVACY_POLICY.html`, README privacy text, Settings Health Connect copy, and
F-Droid metadata were reconciled on 2026-05-17. Current public language now
distinguishes developer collection from direct optional user-triggered
third-party requests and documents these app surfaces:

- Nager.Date holidays.
- NWS alerts.
- Windy map embed/forecast surface.
- RSS/news feeds.
- Webhooks configured by the user.
- Philips Hue LAN bridge integration.
- Internet radio.
- YouTube resolution/downloader path in Play flavor.
- Local crash logs. v1.13.6 adds a user-triggered support ZIP export from
  Settings. The bundle stays local until shared by the user and redacts alarm
  labels, raw URIs, integration secrets, contact/location/Wi-Fi values,
  challenge reference values, and Health Connect records.
- Health Connect sleep sessions once the SDK path lands.

X1 update: the Play flavor now ships Health Connect SDK access for
`android.permission.health.READ_SLEEP` only. `PRIVACY_POLICY.html` and README
were refreshed to state that sleep-session summaries stay local, are not copied
into Room/DataStore/backups, and are not uploaded. Play Console
health-permission declaration/approval remains an external release gate before
Play Store distribution.

Dependency note: adding `androidx.health.connect:connect-client:1.1.0` pulled
`com.google.guava:guava:31.1-android`, which OSV reported as vulnerable. The
Play dependency graph is now constrained to `com.google.guava:guava:33.6.0-android`;
the resolved Play release runtime classpath passed the OSV audit after that
constraint.

X3 dependency note: the Wear complication pass added
`androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0`.
Official AndroidX Wear Watchface release notes list 1.3.0 as the stable release
and state that complication APIs remain supported even as watchface APIs move
toward Watch Face Format. The provider service is protected with
`com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER` in the Wear
manifest and is `directBootAware` per current API reference guidance.

## Android Platform Security

- Exact alarms: current manifest uses `USE_EXACT_ALARM`, which matches a core
  alarm-clock use case under Play policy, but the app must keep the user-facing
  alarm/timer purpose clear.
- Full-screen intent: keep Settings diagnostics current because alarm apps rely
  on time-sensitive notifications and device/vendor behavior differs.
- Wake locks: prior audit is documented; keep smart-alarm monitoring under
  Android vitals limits.
- Foreground service timeouts: review `dataSync`/monitoring services as Android
  15+ limits evolve.
- Direct Boot: v1.13.5 now implements the minimum alarm prototype for the next
  alarm only. Device-encrypted storage contains id, trigger time, display time,
  default-sound flag, vibration flag, and schema/update metadata. Labels,
  custom media/content URIs, integration URLs/secrets, challenge values,
  Health Connect data, Room, and DataStore remain credential-encrypted.
- Hue TLS: trust-any-cert and allow-all hostname verification are currently a
  practical LAN bridge workaround but should be replaced with a pinned bridge
  identity or documented manual trust flow.
