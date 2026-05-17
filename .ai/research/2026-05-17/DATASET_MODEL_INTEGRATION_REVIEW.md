# Dataset, Model, And Integration Review

Date: 2026-05-17

## Applicability

This project is not an ML-first repository. It has no current model training
pipeline, no cloud analytics stack, and no dataset ingestion layer. However, it
does have strong data and integration angles:

- Health Connect sleep sessions.
- Local alarm/event history.
- Local wake-streak, snooze, missed-alarm, and challenge metrics.
- Weather, holiday, alert, news, radio, Hue, webhook, Wear, and downloader
  integrations.

The file is therefore not thin: the best strategy is local-first data use, not a
new cloud dataset.

## Health Connect

Implemented X1 code-side integration:

- `androidx.health.connect:connect-client:1.1.0` is Play-flavor only.
- The Play manifest declares only `android.permission.health.READ_SLEEP`.
- Settings requests only sleep read access through Health Connect's permission
  contract.
- `PlayHealthConnectSleepRepository` reads recent `SleepSessionRecord` windows
  and summarizes duration/stage data for foreground UI.
- Bedtime and Stats show local-only sleep summaries. Records are not copied into
  Room, DataStore, backups, or a developer service.
- F-Droid binds a no-op repository and has no Health Connect SDK or permission
  request path.

Evidence:

- Android Health Connect sleep-session docs define `SleepSessionRecord` for
  session and stage data.
- Android Health Connect publishing docs require Play Console declarations and
  permission justification for data types.

## Local Analytics

Useful local-only derived metrics:

- Time to dismiss by alarm and challenge.
- Snooze count by alarm.
- Missed alarm count by alarm/time of day.
- Challenge retry count.
- Wake-streak trend.
- Sleep duration vs. alarm success once Health Connect is present.
- Smart-wake window effectiveness.

These can be computed from local Room/DataStore history without any remote
service.

## Candidate Models

Near-term:

- No ML model. Use rule-based Health Connect summaries and local statistics.

Later, only if needed:

- Actigraphy-light smart-wake heuristic using accelerometer motion while the
  phone is charging.
- Tiny on-device audio/noise classifier for snore/noise events, guarded by
  explicit microphone consent and clear non-medical language.
- On-device recommendation heuristics for challenge selection based on dismissal
  history.

Rejected for now:

- Remote AI sleep coach by default.
- Medical diagnosis language for snoring, apnea, or sleep quality.
- Uploading sleep/audio/alarm history to a developer service.

## Integration Inventory

Current or planned external surfaces:

- Open-Meteo weather and geocoding.
- Nager.Date holidays.
- National Weather Service alerts.
- Windy map/forecast surface.
- Google News/RSS-style feed URLs.
- User-configured webhooks.
- Philips Hue LAN bridge.
- Internet radio streams.
- YouTube downloader/resolution in the Play flavor.
- Wear OS Data Layer and tile.
- Health Connect sleep sessions in the Play flavor; F-Droid excludes the SDK.

Privacy note: the policy should describe these surfaces as optional/user-driven
where applicable and distinguish developer collection from third-party network
requests triggered by app features.

## Evaluation Opportunities

- Unit-test Health Connect repository logic with fake client abstractions.
- Add golden fixtures for sleep-session windows and time zones.
- Add DST/time-zone regression cases for smart-wake calculations.
- Add migration tests before any new DB field.
- Add release smoke tests for both flavors to prove Play-only dependencies do
  not leak into F-Droid.
- Add Wear tile/complication render tests or screenshot QA.
