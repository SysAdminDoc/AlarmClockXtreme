# Feature Backlog

Date: 2026-05-17

This is the raw harvested backlog before final prioritization. Scored items are
in `PRIORITIZATION_MATRIX.md`; active roadmap order is in `ROADMAP.md`.

## Release, Trust, And Documentation

- Fix `.github/workflows/release.yml` so tag builds produce signed release
  artifacts for Play, F-Droid, and Wear instead of debug APKs.
- Refresh F-Droid metadata to current version/code and current anti-feature
  language.
- Update privacy policy and data-safety narrative for every network/data
  surface, not only Health Connect.
- Add signed artifact SHA-256 and certificate fingerprint publication pattern.
- Add SBOM/dependency inventory artifact for releases.
- Add Dependabot or Renovate for Gradle dependencies and GitHub Actions.
- Add OSV/dependency vulnerability check that works with Gradle Kotlin DSL.
- Add release checklist covering build, sign, verify, tag, GitHub release,
  install smoke, and metadata sync.
- Add `PROJECT_CONTEXT.md` as canonical memory and keep it in version control.

## Reliability And Android Platform

- Enforce Room schema export checks and migration tests. The missing v10 schema
  export was generated and added by this research changeset.
- Add migration tests for every schema bump and verify fresh-install parity.
- Add Direct Boot support for the minimum alarm data required before first
  unlock.
- Add full-screen intent / notification permission diagnostic for Android 14+ and
  vendor-specific settings.
- Add Android 15 foreground-service timeout audit for `dataSync` and
  media-processing paths.
- Add app standby/battery optimization troubleshooting export.
- Add crash log export/share from Settings.
- Tighten Hue v2 TLS by pinning bridge identity or Signify trust material.
- Add backup-export warning when optional secrets/integration URLs are present.

## Health, Sleep, And Analytics

- Complete Play-flavor Health Connect SDK integration for sleep sessions.
- Surface last-night sleep duration/stages in Bedtime and Stats.
- Use Health Connect sleep windows to choose a smart-wake point inside a user
  configured window.
- Add local-only sleep trend charts.
- Add local sleep quality correlations with wake-streak, snooze count, missed
  alarms, and alarm difficulty.
- Add optional manual sleep/mood note after dismissal.
- Add actigraphy-light experimental mode using accelerometer while charging.
- Add local-only snore/noise event detection behind explicit microphone consent.
- Add Vico or equivalent charting library for trend views.

## Wear OS

- Add next-alarm complication data source.
- Add Wear tile refresh QA and physical-device verification notes.
- Add watch-side quick actions for skip/snooze/dismiss state with sync failure
  recovery.
- Add haptic-only watch alarm profile.
- Add standalone watch fallback investigation for users without phone nearby.
- Add Wear screenshot/test harness for tile/complication outputs.

## Challenges And Wake Enforcement

- Add mission stacking / multi-step dismissal.
- Add wake-up check after dismissal with configurable grace period.
- Add typing phrase challenge.
- Add brightness/light-sensor challenge.
- Add NFC tag challenge.
- Add floor/room QR challenge templates with recovery codes.
- Add challenge effectiveness analytics: dismiss time, retries, false starts,
  snooze loops.
- Add per-alarm recommended challenge profiles.

## Audio, Media, And Integrations

- Evaluate Media3/ExoPlayer for internet radio/alarm audio path.
- Isolate or replace `youtubedl-android` Play dependency path.
- Add safer update path for extractor/downloader dependencies.
- Add per-alarm audio fallback tests for local URI, ringtone URI, stream, and
  downloaded audio.
- Add local network integration test stubs for Hue and webhook delivery.
- Add NWS/Open-Meteo/Nager/Windy/RSS privacy and failure-mode docs.

## UX And Product Polish

- Update README architecture/version facts.
- Add first-run trust walkthrough that explains exact alarms, notifications,
  battery, full-screen alarm, and Health Connect separately.
- Add empty/error/recovery states for every integration settings card.
- Add tablet/foldable two-pane Settings and alarm edit layout.
- Add home-screen/lock-screen widget roadmap once Android platform APIs are
  stable enough.
- Add user-visible release notes source from `CHANGELOG.md` or "What's New".

## Research-Only Or Conditional

- AI sleep coach: only after local sleep/session data exists and only if local
  model or explicit opt-in remote model is viable.
- Menstrual-cycle aware alarms: only if Health Connect read/write policy and
  user value justify the sensitivity.
- Blood pressure/weight/mood quick-entry: weak product fit for an alarm app.
- Cloud sync/account: conflicts with current privacy-first stance unless the
  user explicitly chooses a self-hosted or BYO backend model.
