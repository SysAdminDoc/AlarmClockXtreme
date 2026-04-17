package com.sysadmindoc.alarmclock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Core alarm entity stored in Room database.
 * Maps directly to the alarm list UI and scheduling engine.
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int = 9,
    val minute: Int = 0,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val ringtoneUri: String = "",           // Empty = device default
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Int = 2,        // 0=off, 1=gentle, 2=intense
    val volume: Int = 100,                  // 0-100
    val overrideSystemVolume: Boolean = true,
    val gradualVolumeSeconds: Int = 60,     // Fade-in duration in seconds
    val snoozeDurationMinutes: Int = 10,
    val maxSnoozeCount: Int = 3,            // 0 = unlimited
    val showOnLockScreen: Boolean = true,
    val challengeType: String = "NONE",     // ChallengeType enum name
    val group: String = "",                  // User-defined group tag
    val flashWake: Boolean = false,          // Gradually increase screen brightness
    val vibrationPattern: String = "default", // default, gentle, heartbeat, escalating, sos
    val createdAt: Long = System.currentTimeMillis(),
    val nextTriggerTime: Long = 0,          // Epoch millis of next scheduled fire
    // F11: TTS morning announcement
    val ttsEnabled: Boolean = false,
    // F4: Walk-steps dismiss challenge
    val walkStepsRequired: Int = 30,
    // F5: Post-alarm wake confirmation
    val wakeConfirmEnabled: Boolean = false,
    val wakeConfirmDelayMinutes: Int = 10,
    // F7: Smart alarm window (light-sleep detection)
    val smartAlarmEnabled: Boolean = false,
    val smartAlarmWindowMinutes: Int = 30,
    // F13: Public holiday auto-skip
    val skipOnHolidays: Boolean = false,
    // F2: NFC tag dismiss challenge
    val nfcTagId: String = "",
    // F1: Barcode/QR scan dismiss challenge
    val barcodeValue: String = "",
    // F14: Spotify as alarm ringtone
    val spotifyUri: String = "",
    // F15: Philips Hue sunrise
    val hueEnabled: Boolean = false,
    val huePreWakeMinutes: Int = 30,
    // F16: Photo/location match dismiss challenge
    val photoMatchUri: String = "",
    // v1.2.0: Mission chaining (comma-separated ChallengeType names)
    val challengeChain: String = "",
    // v1.2.0: Progressive snooze (each snooze shortens by 1 min)
    val progressiveSnooze: Boolean = false,
    // v1.2.0: Backup sound escalation (ultra-loud after delay)
    val backupSoundEnabled: Boolean = false,
    val backupSoundDelaySec: Int = 40,
    // v1.2.0: Sunrise screen simulation (color transition)
    val sunriseSimulation: Boolean = false,
    val sunriseMinutes: Int = 15,
    // v1.2.0: Date-specific alarm (ISO date, empty = use repeatDays)
    val specificDate: String = "",
    // v1.2.0: Alarm profile tag
    val profileName: String = "",
    // v1.2.0: Early dismiss (minutes before alarm can be cancelled, 0=disabled)
    val earlyDismissMinutes: Int = 0,
    // v1.2.0: Guardian Angel (emergency contact if not dismissed)
    val guardianEnabled: Boolean = false,
    val guardianPhone: String = "",
    val guardianDelaySec: Int = 300,
    // v1.2.0: Location-based auto-dismiss
    val locationDismissEnabled: Boolean = false,
    val locationDismissLat: Double = 0.0,
    val locationDismissLng: Double = 0.0,
    val locationDismissRadius: Int = 100,
    // v1.2.0: Wi-Fi network dismiss challenge
    val wifiDismissSsid: String = "",
    // v1.2.0: Internet radio stream URL as alarm sound
    val internetRadioUrl: String = "",
    // v1.2.0: Flashlight strobe during alarm
    val flashlightStrobe: Boolean = false,
    // v1.2.0: Morning routine checklist (newline-separated items)
    val morningRoutine: String = "",
    // v1.4.0: Hardware-button action during firing ("NONE" / "SNOOZE" / "DISMISS").
    // Volume keys honour the user's choice; NONE leaves volume at system default.
    val hardwareButtonAction: String = "NONE",
    // v1.4.0: Auto-dismiss when the chosen ringtone / track finishes naturally
    // (skips the default infinite loop). Ignored for internet radio.
    val dismissAtRingtoneEnd: Boolean = false,
    // v1.4.0: Random pick from comma-separated ringtone URIs.
    // When set, supersedes [ringtoneUri] on each fire.
    val ringtonePool: String = "",
    // v1.5.0: Sunrise/sunset-relative firing — when non-zero, the alarm's
    // clock time is overridden by solar anchor + offset (minutes, can be
    // negative). Uses last known location. 0 = use [hour]/[minute] directly.
    val solarOffsetMinutes: Int = 0,
    // v1.5.0: Solar anchor — "SUNRISE" or "SUNSET"
    val solarAnchor: String = "SUNRISE"
) {
    val time: LocalTime get() = LocalTime.of(hour, minute)

    val repeatLabel: String get() = when {
        repeatDays.isEmpty() -> "Once"
        repeatDays.size == 7 -> "Every day"
        repeatDays == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY) -> "Weekdays"
        repeatDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekend"
        else -> repeatDays.sortedBy { it.value }
            .joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
    }
}
