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
Treat it as active local memory, not as the only durable project source.

Shared global rules that mattered in this run:

- Read repo `CLAUDE.md` and recent git history at session start.
- Use `rtk` as a command prefix when available.
- Keep roadmap/state files synchronized with implementation or planning.
- Commit and push after completing autonomous work unless a real blocker stops
  the session.

The consolidated durable context is now `PROJECT_CONTEXT.md`. Future sessions
should read it after tool-specific instructions.

## Stale Or Contradictory Claims

| Claim | Location | Current Evidence | Disposition |
|---|---|---|---|
| Current version around v1.6.3 or v1.9.5 | shared memories | `app/build.gradle.kts`, `wear/build.gradle.kts`, `CHANGELOG.md` are v1.13.1 | Stale memory; use only for historical release workflow. |
| Room DB v6 | `CLAUDE.md` path notes | `AlarmDatabase.kt` is version 10 | Stale local memory. |
| Room DB v8 | `README.md` architecture box | `AlarmDatabase.kt` is version 10 | Tracked doc drift. |
| 19 challenge types/views | `README.md`, `CLAUDE.md` path notes | `ROADMAP.md` and code describe 22 user-facing dismiss challenges | Tracked/local doc drift. |
| Release workflow builds both flavor artifacts | `ROADMAP.md` release hygiene note | `.github/workflows/release.yml` runs `assembleDebug` | Corrected in new roadmap refresh as an active gap. |
| Privacy policy says only Open-Meteo/geocoding network requests | `PRIVACY_POLICY.html` older section | README and source include Nager.Date, NWS, Windy, RSS/news, webhooks, Hue LAN, internet radio, YouTube resolution | Active compliance/doc gap. |
| Latest release tag reflects current app version | tag namespace | latest local tag is `v1.9.5`, app is `v1.13.1` | Release hygiene gap. |

## Durable Facts Extracted To `PROJECT_CONTEXT.md`

- Current version, DB version, backup format, modules, flavors, and branch state.
- Product stance and privacy-first operating principles.
- Architecture map for core files.
- Verification and release commands.
- Active risks from this research pass.
- Pointer to the new research packet.

## Open Conflicts

- `CLAUDE.md` is helpful but ignored. Decide whether to track it, keep it local,
  or move its durable content into `PROJECT_CONTEXT.md` and keep `CLAUDE.md`
  as a local-only operational scratchpad.
- The release automation story is not aligned across roadmap and workflow. The
  workflow is authoritative for actual behavior; roadmap now treats signed
  release automation as unshipped.
- The Play flavor's downloader value is real, but the transitive dependency
  security profile is worse than the privacy-first product posture. Decide
  whether to force safer transitive versions, isolate the downloader, or move
  to a maintained update path.

