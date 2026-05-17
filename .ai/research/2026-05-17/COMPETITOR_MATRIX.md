# Competitor Matrix

Date: 2026-05-17

## Summary

AlarmClockXtreme is already ahead of many FOSS clocks on heavy-sleeper features,
backup, weather/news context, and wake-readiness UI. The leading gaps are not
basic alarm CRUD; they are sleep data, wearable depth, Direct Boot/reproducible
release discipline, and dependency/release trust.

| Product / Project | Positioning | Notable Features | Lessons For AlarmClockXtreme | Sources |
|---|---|---|---|---|
| Alarmy | Commercial heavy-sleeper / morning wellness app | Missions such as math, shake, photo, barcode, memory, squat; sleep analysis; wake-up enforcement | ACX already has broad challenge coverage. Next differentiation should be mission stacking, wake-up verification after dismissal, and clearer challenge reliability metrics. | https://alar.my/en, https://alarmy-android.zendesk.com/hc/en-us/articles/360004242254--Mission-How-can-I-set-the-Alarm-off-method-math-shake-etc- |
| Sleep as Android | Mature sleep tracker plus smart alarm | Health Connect integration, wearables, sleep tracking, sonar-style concepts, anti-snoring ecosystem | ACX should finish Health Connect read path before building its own classifier. Local-first sleep insights can compete without cloud coaching. | https://docs.sleep.urbandroid.org/services/health_connect.html |
| Sleep Cycle | Commercial smart alarm and sleep coaching | Smart wake window, sound analysis, snore/cough tracking, sleep aid, statistics, coaching | ACX needs a credible smart-wake story: import sleep sessions first, then on-device signals. Avoid unsupported medical claims. | https://sleepcycle.com/features/smart-alarm-clock/, https://support.sleepcycle.com/hc/en-us/articles/7859664023452-Using-the-Sleep-Cycle-app |
| Sleepwave | Commercial contactless smart alarm | Contactless motion/breathing estimates, sleep depth, sound recordings, dream journal, watch taps | Motion/breathing ideas are attractive but high-risk. Treat as experimental and opt-in, with battery/privacy controls. | https://sleepwave.com/ |
| Fossify Clock | FOSS clock/timer/stopwatch baseline | Offline, no ads, widgets, custom alarm settings, timer/stopwatch, themes | ACX has richer alarm features but must match FOSS trust: F-Droid metadata, release reproducibility, and privacy statements need to be current. | https://github.com/FossifyOrg/Clock |
| BlackyHawky Clock | Privacy-first AOSP-derived FOSS clock | Direct Boot, reproducible builds, date-specific alarms, random ringtones, shake/flip actions, permission issue notes | Direct Boot and reproducible-build/release discipline are high-value, concrete ACX roadmap items. | https://github.com/BlackyHawky/Clock |
| QRAlarm | FOSS QR/barcode wake app | QR/barcode dismissal and signed release fingerprint communication | ACX already has QR challenge capability; add signed artifact/fingerprint trust to release docs. | https://github.com/sweakpl/qralarm-android |
| AOSP / DeskClock lineage | Baseline Android clock source family | Stock alarm/timer/clock behavior and integration expectations | Preserve familiar Android alarm behavior while adding heavy-sleeper controls. | https://android.googlesource.com/platform/packages/apps/DeskClock/ |
| yt-dlp ecosystem | Maintained downloader ecosystem | High release cadence for extractor breakage | ACX Play downloader should track a maintained update path or be isolated from alarm-critical runtime. | https://github.com/yt-dlp/yt-dlp |
| NewPipeExtractor | Android-friendly extractor library | Hotfix releases for YouTube/SoundCloud extractor changes | Useful but volatile. Keep Play-only and covered by dependency/security review. | https://github.com/TeamNewPipe/NewPipeExtractor/releases |

## Opportunity Patterns

- Commercial leaders sell outcomes: "wake up during light sleep", "prove you
  are awake", "understand your night". ACX can express the same outcomes while
  staying local-first and transparent.
- FOSS leaders win trust through offline operation, reproducible builds, Direct
  Boot, and clear metadata. ACX needs to close its release/metadata drift to
  match this trust posture.
- Wearables are now a core expectation for sleep/alarm apps. ACX has a tile, but
  should add complications, watch-side state, and physical-device QA.
- Health Connect is the best first data integration because it avoids inventing
  a cloud account or raw sleep-data import format.

