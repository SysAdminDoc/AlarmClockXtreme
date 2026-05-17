# Direct Boot Minimum Alarm Prototype

Date: 2026-05-17
Version: v1.13.5

Android Direct Boot exposes device-encrypted storage before the user unlocks
after reboot. Android's security documentation lists alarm-clock apps as a
common Direct Boot use case and requires Direct-Boot-aware components to use
device-encrypted storage for data they need before first unlock:
https://developer.android.com/privacy-and-security/direct-boot

## Goal

This prototype keeps AlarmClockXtreme wake-reliable after a reboot without
moving the app's private alarm database, settings, integrations, media URIs, or
challenge data out of credential-encrypted storage.

## Stored Before First Unlock

`DirectBootAlarmCache` stores only the next alarm's minimum fallback snapshot in
device-encrypted SharedPreferences:

- alarm id
- trigger time
- short display time text
- whether the fallback should use the system default alarm sound
- whether vibration should run
- schema/update metadata

The snapshot intentionally does not store alarm labels, custom ringtone/content
URIs, internet radio URLs, Spotify URIs, Hue/webhook details, NFC/barcode/photo
challenge data, location/Wi-Fi values, guardian contacts, Health Connect data,
or any DataStore/Room payload.

## Runtime Flow

1. Normal scheduling writes or refreshes the next-alarm snapshot after
   `AlarmManager.setAlarmClock()` succeeds.
2. `BootReceiver` is Direct-Boot-aware and receives
   `ACTION_LOCKED_BOOT_COMPLETED`.
3. The locked-boot path reads only `DirectBootAlarmCache` and schedules
   `DirectBootAlarmReceiver`; it does not enqueue `BootRescheduleWorker`.
4. If the cached alarm fires before first unlock,
   `DirectBootAlarmService` starts as a foreground service, posts a public alarm
   notification, plays the system default alarm tone when allowed, vibrates when
   allowed, and auto-stops after ten minutes.
5. After unlock, the normal `BOOT_COMPLETED` path runs WorkManager/Room
   rescheduling. If the Direct Boot fallback already fired a one-shot alarm,
   the post-unlock reschedule consumes the fired marker and disables that
   one-shot instead of rolling it to tomorrow.

## Boundaries

- Pre-unlock fallback is not the full alarm-firing UI.
- Snooze, dismiss challenges, custom audio, backup sound escalation, Guardian
  Angel, Hue sunrise, webhooks, Wear Data Layer, and wake-confirm flows remain
  post-unlock behavior backed by credential-encrypted state.
- The fallback uses only the system default alarm or notification tone. Custom
  media stays private and may not be readable before unlock.
- Normal post-unlock scheduling cancels stale fallback PendingIntents to avoid
  duplicate fires if the user unlocks before the cached alarm time.

## Verification

Unit coverage lives in
`app/src/test/java/com/sysadmindoc/alarmclock/directboot/DirectBootAlarmSnapshotTest.kt`.
Release verification should also inspect the APK manifest for
`LOCKED_BOOT_COMPLETED`, `android:directBootAware="true"` on the boot receiver,
and the direct-boot fallback receiver/service entries.
