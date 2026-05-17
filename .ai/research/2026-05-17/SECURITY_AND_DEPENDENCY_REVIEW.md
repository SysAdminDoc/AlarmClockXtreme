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

## Dependency Recommendations

1. Add Gradle dependency constraints for vulnerable transitive packages and run
   the Play downloader tests against the constrained graph.
2. If constraints break `youtubedl-android`, isolate downloader execution away
   from alarm-critical code and document the risk.
3. Evaluate replacing or augmenting the downloader path with a maintained
   yt-dlp binary/update model for the Play flavor.
4. Add automated dependency update tooling.
5. Add a dependency vulnerability workflow that resolves Gradle classpaths
   before querying OSV or another advisory source.

## Release Security

Current `.github/workflows/release.yml` builds `assembleDebug` and uploads debug
APK outputs on tag pushes. This is not acceptable for a signed release path.

Required repairs:

- Build signed `playRelease` and `fdroidRelease`.
- Build Wear release artifact or explicitly document why it is packaged through
  the phone app.
- Verify signing certificate and APK metadata.
- Upload SHA-256 checksums.
- Avoid uploading debug artifacts on release tags.
- Consider publishing artifact certificate fingerprints in the release notes.

## Privacy And Data Safety

Current `PRIVACY_POLICY.html` accurately describes the Health Connect scaffold,
but older paragraphs still describe network behavior as Open-Meteo/geocoding
only. Current app capabilities include additional optional data flows:

- Nager.Date holidays.
- NWS alerts.
- Windy map embed/forecast surface.
- RSS/news feeds.
- Webhooks configured by the user.
- Philips Hue LAN bridge integration.
- Internet radio.
- YouTube resolution/downloader path in Play flavor.
- Local crash logs.
- Health Connect sleep sessions once the SDK path lands.

Update the policy and store declarations before shipping Health Connect SDK
access.

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
- Direct Boot: not implemented; alarm-clock apps are a listed Direct Boot use
  case, but implementation must separate device-encrypted minimum data from
  credential-encrypted sensitive settings.
- Hue TLS: trust-any-cert and allow-all hostname verification are currently a
  practical LAN bridge workaround but should be replaced with a pinned bridge
  identity or documented manual trust flow.

