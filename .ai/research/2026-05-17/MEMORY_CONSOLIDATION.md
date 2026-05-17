# Memory Consolidation

Date: 2026-05-17

## Files Inventoried

Repo-local:

- `AGENTS.md`
- `CLAUDE.md`
- `README.md`
- `CHANGELOG.md`
- `ROADMAP.md`
- `PRIVACY_POLICY.html`
- `.github/workflows/release.yml`
- `.github/workflows/version-lint.yml`
- `metadata/com.sysadmindoc.alarmclock.yml`
- `metadata/en-US/fdroid.yml`

No nested `.claude`, `.cursor`, `GEMINI.md`, `COPILOT_INSTRUCTIONS.md`,
`ARCHITECTURE*`, `CONTRIBUTING*`, or existing `.ai` files were present before
this research run.

Shared instruction and memory files inspected:

- `C:\Users\--\.claude\CLAUDE.md`
- `C:\Users\--\CLAUDE.md`
- `C:\Users\--\.claude\projects\c--Users----repos\memory\MEMORY.md`
- `C:\Users\--\.claude\projects\c--Users----repos\memory\stack-android.md`
- `C:\Users\--\.claude\projects\c--Users----repos\memory\android-apk.md`
- `C:\Users\--\.codex\memories\MEMORY.md`
- `C:\Users\--\.codex\memories\skills\autonomous-roadmap-loop\SKILL.md`

## Instruction Reconciliation

`AGENTS.md` delegates repo-specific details to `CLAUDE.md` and shared global
files. It should remain tool-specific.

`CLAUDE.md` is useful as living working notes, but it is ignored by `.gitignore`.
`AGENTS.md` is also ignored by the user's global Git ignore file. Treat both as
local tool scratchpads, not as durable project sources.

Shared global rules that mattered in this run:

- Read repo `CLAUDE.md` and recent git history at session start.
- Use `rtk` as a command prefix when available.
- Keep roadmap/state files synchronized with implementation or planning.
- Commit and push after completing autonomous work unless a real blocker stops
  the session.

The consolidated durable context is now `PROJECT_CONTEXT.md`. Future sessions
should read it after tool-specific instructions, and should prefer tracked
`PROJECT_CONTEXT.md`, `ROADMAP.md`, `README.md`, and `CHANGELOG.md` when ignored
local tool notes conflict with tracked project evidence.

## Stale Or Contradictory Claims

| Claim | Location | Current Evidence | Disposition |
|---|---|---|---|
| Current version around v1.6.3 or v1.9.5 | shared memories | `app/build.gradle.kts`, `wear/build.gradle.kts`, `CHANGELOG.md` are v1.13.1 | Stale memory; use only for historical release workflow. |
| Room DB v6 | `CLAUDE.md` path notes | `AlarmDatabase.kt` is version 10 | Stale ignored local memory; tracked docs now state DB v10. |
| Room DB v8 | `README.md` architecture box | `AlarmDatabase.kt` is version 10 | Corrected in README / tracked context. |
| 19 challenge types/views | `README.md`, `CLAUDE.md` path notes | `ROADMAP.md` and code describe 22 user-facing dismiss challenges | Corrected in README / tracked context; stale only in ignored local notes. |
| Release workflow builds both flavor artifacts | `ROADMAP.md` release hygiene note | `.github/workflows/release.yml` previously ran `assembleDebug`; R2 repaired the workflow | Corrected; release workflow now builds signed Play/F-Droid/Wear release APKs. |
| Privacy policy says only Open-Meteo/geocoding network requests | `PRIVACY_POLICY.html` older section | README and source include Nager.Date, NWS, Windy, RSS/news, webhooks, Hue LAN, internet radio, YouTube resolution | Corrected in privacy policy, README, metadata, and tracked context. |
| Latest release tag reflects current app version | tag namespace | latest local tag is `v1.9.5`, app is `v1.13.1` | Release hygiene gap. |

## Durable Facts Extracted To `PROJECT_CONTEXT.md`

- Current version, DB version, backup format, modules, flavors, and branch state.
- Product stance and privacy-first operating principles.
- Architecture map for core files.
- Verification and release commands.
- Active risks from this research pass.
- Pointer to the new research packet.

## Open Conflicts

- Latest release tag still does not reflect current app version: latest local
  tag is `v1.9.5`, app is `v1.13.1`. This is a release-management gap, not a
  source-of-truth conflict.

## Resolved Conflicts

- `CLAUDE.md` is helpful but ignored, and `AGENTS.md` is ignored globally.
  Decision from the v1.13.2 R6 doc-policy pass: keep both as local-only
  operational scratchpads and do not force-add them. Move durable facts into
  tracked `PROJECT_CONTEXT.md`, `ROADMAP.md`, `README.md`, and `CHANGELOG.md`.
- The release automation story is now aligned: `.github/workflows/release.yml`
  builds signed Play/F-Droid/Wear release artifacts and uploads SHA-256 hashes
  on tags.
- The Play flavor downloader dependency risk was mitigated in R5 with Play-only
  constraints and a CI OSV audit against `playReleaseRuntimeClasspath`.
