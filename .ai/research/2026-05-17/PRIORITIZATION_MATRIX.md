# Prioritization Matrix

Date: 2026-05-17

Scoring: Impact 1-5, Confidence 1-5, Effort 1-5 where lower is easier, Risk
1-5 where lower is safer. Score = Impact + Confidence - Effort - Risk.

## Now

| ID | Candidate | Impact | Confidence | Effort | Risk | Score | Why Now |
|---|---|---:|---:|---:|---:|---:|---|
| P0-1 | Privacy policy and data-safety reconciliation | 5 | 5 | 2 | 2 | 6 | Existing policy text is stale against current integrations; Play Health Connect work raises scrutiny. |
| P0-2 | Signed release workflow repair | 5 | 5 | 3 | 2 | 5 | Tag workflow currently builds debug APKs; release trust is foundational. |
| P0-3 | F-Droid metadata refresh | 4 | 5 | 2 | 1 | 6 | Metadata is far behind app version and anti-feature story. |
| P0-4 | Room migration/schema gate | 4 | 5 | 2 | 2 | 5 | v10 schema export is added in this changeset; durable migration and CI gates still need implementation. |
| P0-5 | Play downloader dependency risk mitigation | 5 | 4 | 4 | 3 | 2 | OSV finds multiple advisories in Play-only transitive dependencies. Needs careful dependency constraints or isolation. |
| P0-6 | README/CLAUDE factual drift cleanup | 3 | 5 | 1 | 1 | 6 | Cheap memory hygiene prevents future agent errors. |

## Next

| ID | Candidate | Impact | Confidence | Effort | Risk | Score | Why Next |
|---|---|---:|---:|---:|---:|---:|---|
| P1-1 | Health Connect sleep-session SDK integration | 5 | 4 | 4 | 3 | 2 | Scaffold shipped; competitor parity and user value are strong, but policy/permission flow must be correct. |
| P1-2 | Backup-export warning for secrets | 4 | 5 | 2 | 2 | 5 | Trust-critical and smaller than full backup encryption redesign. |
| P1-3 | Wear next-alarm complication | 4 | 4 | 3 | 2 | 3 | Natural follow-up to Wear tile and current Wear OS ecosystem. |
| P1-4 | Direct Boot minimum alarm support | 5 | 3 | 5 | 4 | -1 | Shipped v1.13.5 as a minimum device-encrypted next-alarm fallback with storage separation documented. |
| P1-5 | Crash log export/share | 3 | 5 | 2 | 1 | 5 | Shipped v1.13.6 as a FileProvider-backed local support ZIP with redacted diagnostics and no telemetry. |
| P1-6 | Media3/ExoPlayer audio path audit | 3 | 3 | 4 | 3 | -1 | Useful but less urgent than downloader security and release trust. |

## Later

| ID | Candidate | Impact | Confidence | Effort | Risk | Score | Why Later |
|---|---|---:|---:|---:|---:|---:|---|
| P2-1 | Mission stacking / multi-step challenges | 4 | 4 | 4 | 3 | 1 | Commercially proven, but current challenge surface is already broad. |
| P2-2 | Wake-up check after dismissal | 4 | 4 | 3 | 3 | 2 | Strong heavy-sleeper value; needs careful annoyance and accessibility design. |
| P2-3 | Local sleep trend charts | 3 | 4 | 3 | 2 | 2 | Depends on Health Connect or local sleep data. |
| P2-4 | On-device actigraphy-light smart wake | 4 | 2 | 5 | 4 | -3 | Attractive but needs validation and battery controls. |
| P2-5 | Snore/noise detection | 3 | 2 | 5 | 5 | -5 | Privacy and medical-claim risk; keep experimental. |
| P2-6 | Foldable/tablet two-pane flows | 3 | 4 | 3 | 2 | 2 | Good polish, but less urgent than trust/release fixes. |

## Rejected Or Conditional

| Candidate | Decision |
|---|---|
| Cloud account sync | Not aligned with current privacy-first stance without explicit user direction. |
| AI sleep coach using remote LLM | Conditional; requires explicit opt-in, no sensitive default upload, and a clear local-first alternative. |
| Health Connect weight/BP quick-entry | Weak fit for alarm-clock core; defer until sleep/wake loop is complete. |
| Menstrual-cycle-aware alarms | Sensitive data class; only revisit with clear user value, explicit consent, and policy review. |
