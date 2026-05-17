# Research Log

Date: 2026-05-17

## Local Reconnaissance

Commands used:

```powershell
git status --short --branch
rtk git log -10 --oneline --decorate
git tag --sort=-creatordate
rg -n "versionCode|versionName|compileSdk|targetSdk|minSdk|Room|Database\(|MIGRATION_9_10|Health Connect|USE_EXACT_ALARM|SCHEDULE_EXACT_ALARM|FOREGROUND_SERVICE|USE_FULL_SCREEN_INTENT|android:directBootAware|trustAll|X509TrustManager|HostnameVerifier|youtubedl|NewPipeExtractor|commons-compress|jackson|assembleDebug|gh release upload|antiFeatures|CurrentVersion|Room DB v8|19 challenge" -S ...
.\gradlew.bat :app:dependencies --configuration playReleaseRuntimeClasspath --console=plain
.\gradlew.bat :app:dependencies --configuration fdroidReleaseRuntimeClasspath --console=plain
```

Findings:

- Current source is v1.13.1, DB v10, backup v7.
- Worktree was clean at the start.
- Local branch was ahead of origin by 13 commits.
- Latest tag is v1.9.5, which lags current app version.
- Play dependency graph includes downloader transitive dependencies with known
  OSV advisories.
- F-Droid dependency graph excludes downloader-specific dependencies.
- Release workflow uploads debug artifacts on tag builds.

## Instruction And Memory Search

Searched for:

- `AGENTS.md`
- `CLAUDE.md`
- `.claude/**`
- `.claude-instructions`
- `.cursor/rules/**`
- `.cursorrules`
- `.windsurfrules`
- `GEMINI.md`
- `COPILOT_INSTRUCTIONS.md`
- `.github/copilot-instructions.md`
- `.ai/**`
- `memory*.md`
- `context*.md`
- `project*.md`
- `notes*.md`
- `TODO*`
- `ROADMAP*`
- `CHANGELOG*`
- `ARCHITECTURE*`
- `CONTRIBUTING*`

Only root `AGENTS.md`, root `CLAUDE.md`, root `ROADMAP.md`, root
`CHANGELOG.md`, and `.github/workflows/*.yml` matched before this research run.

## Web Research Passes

Platform/policy queries:

- `Android Health Connect sleep session record official documentation SleepSessionRecord`
- `Android 16 ProgressStyle notification official Android Developers`
- `Android exact alarms special app access Android 14 official documentation`
- `Android foreground service timeout Android 15 official documentation`
- `Google Play Health Connect permissions policy official Android app sleep data`
- `Android Direct Boot alarm clock official documentation`
- `Wear OS Tiles complications official Android developers Glance Wear Tiles`

Competitor queries:

- `Sleep as Android changelog Health Connect Wear OS sonar CAPTCHA release notes`
- `Alarmy alarm app missions features official math barcode squat memory photo typing wake up check`
- `Sleep Cycle app features sleep tracking smart alarm official`
- `Fossify Clock GitHub alarm clock Android features`
- `BlackyHawky Clock GitHub Android alarm app releases`
- `GitHub Android alarm clock open source QR alarm math challenge`

Dependency/security queries:

- `AndroidX Health Connect release notes 1.1.0 official`
- `AndroidX Room release notes 2.7.0 2.8.0 official`
- `Android Gradle Plugin release notes official`
- `youtubedl-android 0.18.1 GitHub releases commons-compress jackson-databind`
- `NewPipeExtractor v0.24.8 GitHub releases`
- `yt-dlp releases GitHub latest 2026 Android library replacement`
- `F-Droid metadata anti-features NonFreeNet official documentation`

## Vulnerability Research

`osv-scanner scan source -r .` did not find package source manifests in this
Gradle Kotlin DSL repo and produced no useful dependency results. The dependency
tree was therefore queried directly with the OSV batch API for the relevant
resolved Maven coordinates.

OSV batch result for selected Play-flavor transitives:

- `jackson-databind@2.11.1`: 4 advisories.
- `jackson-core@2.11.1`: 3 advisories.
- `commons-compress@1.12`: 7 advisories.
- `commons-io@2.5`: 2 advisories.
- `rhino@1.8.0`: 1 advisory.
- `okhttp@4.12.0`: no advisories returned.
- `jsoup@1.21.1`: no advisories returned.

R5 implementation pass:

- Added `scripts/osv_gradle_audit.py`, which resolves a Gradle configuration
  and queries OSV batch API for every Maven coordinate in the resolved output.
- Re-ran the script against `playDebugRuntimeClasspath` and
  `playReleaseRuntimeClasspath` after applying Play-only dependency constraints.
- Both constrained graphs returned "OSV: no vulnerabilities reported for
  resolved dependencies."
- Re-ran the Play release build because Android release shrinking is the path
  most likely to expose optional transitive-class breakage.

## Saturation Notes

Source saturation was tested across four classes:

1. Local code/docs/workflows.
2. Official Android, Play, AndroidX, Gradle, and F-Droid references.
3. Direct and adjacent competitor products.
4. Vulnerability/advisory data.

The same themes repeated after multiple passes: Health Connect/sleep analytics,
Wear glanceable surfaces, Direct Boot/reliability, mission/challenge depth,
privacy-policy/data-safety alignment, release automation, dependency scanning,
and migration/schema rigor.

Thin areas:

- No public AlarmClockXtreme issue tracker was inspected through GitHub API in
  this run; local repo evidence dominated the plan.
- No connected device was used, so wake/fire behavior was not re-validated on
  hardware.
- No Play Console or F-Droid submission portal state was available; metadata
  findings are based on repo files.
