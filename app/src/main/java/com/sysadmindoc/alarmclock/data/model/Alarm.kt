package com.sysadmindoc.alarmclock.data.model

import androidx.annotation.StringRes
import com.sysadmindoc.alarmclock.R
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

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
    // v1.10.3: Require a deliberate hold gesture before the firing screen can
    // dismiss. This protects users who accidentally swipe a ready alarm away.
    val holdToDismissEnabled: Boolean = false,
    // v1.4.0: Random pick from comma-separated ringtone URIs.
    // When set, supersedes [ringtoneUri] on each fire.
    val ringtonePool: String = "",
    // v1.5.0: Sunrise/sunset-relative firing — when non-zero, the alarm's
    // clock time is overridden by solar anchor + offset (minutes, can be
    // negative). Uses last known location. 0 = use [hour]/[minute] directly.
    val solarOffsetMinutes: Int = 0,
    // v1.5.0: Solar anchor — "SUNRISE" or "SUNSET"
    val solarAnchor: String = "SUNRISE",
    // v1.12.0 (roadmap N7): Delay before vibration starts, in seconds.
    // Pairs with the existing [gradualVolumeSeconds] audio fade-in to let
    // users build a "gentle wake" preset that ramps audio first and only
    // adds haptic intensity after a configurable window. 0 = vibrate as
    // soon as the alarm fires (preserves prior behaviour).
    val vibrationDelaySeconds: Int = 0,
    val weatherEarlyMinutes: Int = 0,
    val requiredSquats: Int = 10,
    // v1.15.1: Per-alarm dismiss action — fires a webhook, Hue scene, or
    // broadcast on successful dismiss. Inspired by AlarmKit (iOS 26).
    // "NONE" / "WEBHOOK" / "HUE_SCENE" / "BROADCAST"
    val dismissActionType: String = "NONE",
    // Payload for the dismiss action: URL for webhook, scene name for Hue,
    // or action string for broadcast. Empty when dismissActionType is NONE.
    val dismissActionPayload: String = "",
    // v1.15.12: Optional per-alarm firing-screen image. Disabled by default
    // and ignored unless a persisted image URI is present.
    val firingBackgroundImageEnabled: Boolean = false,
    val firingBackgroundImageUri: String = "",
    // Blur is applied only on Android 12+ where RenderEffect is available.
    val firingBackgroundBlurEnabled: Boolean = true,
    // v1.15.13: User-defined alarm list order. 0 means "not assigned yet".
    val sortOrder: Int = 0,
    // v1.15.27: Optional rotating shift pattern. When set, the alarm fires
    // only on D/N/W days in the selected preset. Empty = regular schedule.
    val shiftPattern: String = "",
    // ISO local date that marks day 0 of [shiftPattern].
    val shiftPatternStartDate: String = "",
    // LOCAL follows the device wall clock. FIXED keeps the alarm's wall clock
    // anchored to [fixedTimezoneId] while the user travels.
    val timezonePolicy: String = TIMEZONE_POLICY_LOCAL,
    val fixedTimezoneId: String = ""
) {
    companion object {
        const val TIMEZONE_POLICY_LOCAL = "LOCAL"
        const val TIMEZONE_POLICY_FIXED = "FIXED"
        const val MAX_SMART_ALARM_WINDOW_MINUTES = 60
        private const val MAX_LABEL_CHARS = 120
        private const val MAX_GROUP_CHARS = 40
        private const val MAX_PHONE_CHARS = 40
        private const val MAX_SHORT_REFERENCE_CHARS = 128
        private const val MAX_BARCODE_CHARS = 512
        private const val MAX_URI_CHARS = 2_048
        private const val MAX_WIFI_SSID_CHARS = 64
        private const val MAX_ROUTINE_ITEMS = 12
        private const val MAX_ROUTINE_ITEM_CHARS = 80
        private const val MAX_RINGTONE_POOL_ITEMS = 20

        // Must stay in lockstep with ChallengeType (see ChallengeGenerator.kt).
        // Adding a new ChallengeType without listing it here causes sanitized()
        // to silently rewrite it back to "NONE" on every backup/share/DataStore
        // round-trip — that bug shipped between v1.6.0 (when RPS / EmojiMemory
        // / TypingSpeed / Wordle landed) and v1.12 (when this guard was fixed).
        // A ChallengeType round-trip property test guards against regression.
        private val VALID_CHALLENGE_TYPES = setOf(
            "NONE",
            "MATH_EASY",
            "MATH_MEDIUM",
            "MATH_HARD",
            "SHAKE",
            "SEQUENCE",
            "MEMORY_PATTERN",
            "TYPING",
            "VOICE_PHRASE",
            "HANDWRITING",
            "WALK_STEPS",
            "NFC_SCAN",
            "BARCODE_SCAN",
            "PHOTO_MATCH",
            "SQUAT",
            "WIFI_CONNECT",
            "MAZE",
            "COUNT_SHEEP",
            "SIMON_SAYS",
            "DATE_BACKWARDS",
            "STROOP",
            "ROCK_PAPER_SCISSORS",
            "EMOJI_MEMORY",
            "TYPING_SPEED",
            "WORDLE",
            "PVT",
            "SPOT_DIFFERENCE",
            "CHESS_MATE",
            "RSVP_READING",
            "PUSH_UP",
            "PLANK_HOLD"
        )
        private val VALID_VIBRATION_PATTERNS = setOf(
            "default",
            "gentle",
            "heartbeat",
            "escalating",
            "sos"
        )
    }

    val time: LocalTime get() = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    val usesFixedTimezone: Boolean
        get() = timezonePolicy == TIMEZONE_POLICY_FIXED && fixedTimezoneId.isNotBlank()

    /** Resolve the schedule's wall-clock zone, falling back safely for corrupt imports. */
    fun schedulingZone(deviceZone: ZoneId): ZoneId {
        if (!usesFixedTimezone) return deviceZone
        return runCatching { ZoneId.of(fixedTimezoneId) }.getOrDefault(deviceZone)
    }

    val isRecurringSchedule: Boolean
        get() = repeatDays.isNotEmpty() ||
            (ShiftPattern.fromKey(shiftPattern) != null &&
                runCatching { LocalDate.parse(shiftPatternStartDate) }.isSuccess)

    /**
     * The resource naming this schedule, or null when it is an arbitrary set of
     * days and there is nothing to name. Callers fall back to [repeatDayNames].
     *
     * An id rather than a sentence because this is a Room entity: it is read
     * from a ViewModel, a Compose screen and a support bundle, and only two of
     * those have a Context. The alarm card used to show "Once" and "Weekdays"
     * in English on every phone because of it.
     */
    @get:StringRes
    val repeatLabelRes: Int? get() = when {
        repeatDays.isEmpty() -> R.string.alarm_repeat_once
        repeatDays.size == 7 -> R.string.alarm_repeat_every_day
        repeatDays == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY) -> R.string.alarm_repeat_weekdays
        repeatDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> R.string.alarm_repeat_weekend
        else -> null
    }

    /** "Mon, Wed, Fri" in the reader's language, for a schedule with no name. */
    fun repeatDayNames(locale: Locale = Locale.getDefault()): String =
        repeatDays.sortedBy { it.value }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }

    /**
     * A stable, machine-readable form for the support bundle. Deliberately not
     * the display label: a supporter reading a diagnostics dump should see the
     * same token whatever language the phone was set to.
     */
    val repeatWireLabel: String get() = when {
        repeatDays.isEmpty() -> "ONCE"
        repeatDays.size == 7 -> "DAILY"
        else -> repeatDays.sortedBy { it.value }.joinToString(",") { it.name.take(3) }
    }

    /**
     * Defensive normalisation for anything that bypasses the UI layer:
     * backup restore, future migrations, corrupted persistence, or tests that
     * construct alarms manually. Keeps the persisted shape predictable and
     * prevents obviously-invalid values from crashing scheduling.
     */
    fun sanitized(): Alarm {
        val normalizedPool = ringtonePool.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.take(MAX_URI_CHARS) }
            .distinct()
            .take(MAX_RINGTONE_POOL_ITEMS)
            .joinToString(",")
        val normalizedSpecificDate = specificDate.trim().takeIf {
            it.isNotBlank() && runCatching { LocalDate.parse(it) }.isSuccess
        }.orEmpty()
        val normalizedChallengeType = challengeType
            .trim()
            .uppercase(Locale.US)
            .takeIf { it in VALID_CHALLENGE_TYPES }
            ?: "NONE"
        val normalizedChallengeChain = challengeChain.split(",")
            .map { it.trim().uppercase(Locale.US) }
            .filter { it in VALID_CHALLENGE_TYPES && it != "NONE" }
            .distinct()
            .joinToString(",")
        val normalizedVibrationPattern = vibrationPattern
            .trim()
            .lowercase(Locale.US)
            .takeIf { it in VALID_VIBRATION_PATTERNS }
            ?: "default"
        val normalizedFiringBackgroundImageUri = firingBackgroundImageUri.trim().take(MAX_URI_CHARS)
        val normalizedShiftPatternKey = ShiftPattern.normalizedKey(shiftPattern)
        val normalizedShiftPatternStartDate = shiftPatternStartDate.trim().takeIf {
            normalizedShiftPatternKey.isNotBlank() &&
                it.isNotBlank() &&
                runCatching { LocalDate.parse(it) }.isSuccess
        }.orEmpty()
        val normalizedShiftPattern = normalizedShiftPatternKey.takeIf {
            normalizedShiftPatternStartDate.isNotBlank()
        }.orEmpty()
        val normalizedFixedTimezoneId = fixedTimezoneId.trim().takeIf {
            it.isNotBlank() && runCatching { ZoneId.of(it) }.isSuccess
        }.orEmpty()
        val normalizedTimezonePolicy = if (
            timezonePolicy.equals(TIMEZONE_POLICY_FIXED, ignoreCase = true) &&
            normalizedFixedTimezoneId.isNotBlank()
        ) TIMEZONE_POLICY_FIXED else TIMEZONE_POLICY_LOCAL

        return copy(
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            label = label.trim().take(MAX_LABEL_CHARS),
            ringtoneUri = ringtoneUri.trim().take(MAX_URI_CHARS),
            vibrationIntensity = vibrationIntensity.coerceIn(0, 2),
            volume = volume.coerceIn(0, 100),
            gradualVolumeSeconds = gradualVolumeSeconds.coerceIn(0, 300),
            snoozeDurationMinutes = snoozeDurationMinutes.coerceIn(1, 180),
            maxSnoozeCount = maxSnoozeCount.coerceIn(0, 20),
            challengeType = normalizedChallengeType,
            group = group.trim().take(MAX_GROUP_CHARS),
            vibrationPattern = normalizedVibrationPattern,
            walkStepsRequired = walkStepsRequired.coerceIn(1, 10_000),
            wakeConfirmDelayMinutes = wakeConfirmDelayMinutes.coerceIn(1, 180),
            smartAlarmWindowMinutes = smartAlarmWindowMinutes.coerceIn(0, MAX_SMART_ALARM_WINDOW_MINUTES),
            nfcTagId = nfcTagId.trim().take(MAX_SHORT_REFERENCE_CHARS),
            barcodeValue = barcodeValue.trim().take(MAX_BARCODE_CHARS),
            spotifyUri = spotifyUri.trim().take(MAX_URI_CHARS),
            huePreWakeMinutes = huePreWakeMinutes.coerceIn(0, 180),
            photoMatchUri = photoMatchUri.trim().take(MAX_URI_CHARS),
            challengeChain = normalizedChallengeChain,
            backupSoundDelaySec = backupSoundDelaySec.coerceIn(5, 900),
            sunriseMinutes = sunriseMinutes.coerceIn(0, 120),
            specificDate = normalizedSpecificDate,
            profileName = profileName.trim().take(MAX_GROUP_CHARS),
            earlyDismissMinutes = earlyDismissMinutes.coerceIn(0, 180),
            guardianPhone = guardianPhone.trim().take(MAX_PHONE_CHARS),
            guardianDelaySec = guardianDelaySec.coerceIn(30, 3600),
            locationDismissRadius = locationDismissRadius.coerceIn(25, 5_000),
            wifiDismissSsid = wifiDismissSsid.trim().take(MAX_WIFI_SSID_CHARS),
            internetRadioUrl = internetRadioUrl.trim().take(MAX_URI_CHARS),
            morningRoutine = morningRoutine.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.take(MAX_ROUTINE_ITEM_CHARS) }
                .take(MAX_ROUTINE_ITEMS)
                .joinToString("\n"),
            hardwareButtonAction = when (hardwareButtonAction.uppercase(Locale.US)) {
                "SNOOZE", "DISMISS" -> hardwareButtonAction.uppercase(Locale.US)
                else -> "NONE"
            },
            ringtonePool = normalizedPool,
            solarOffsetMinutes = solarOffsetMinutes.coerceIn(-720, 720),
            solarAnchor = if (solarAnchor.equals("SUNSET", ignoreCase = true)) "SUNSET" else "SUNRISE",
            // v1.12.0 (roadmap N7): 0..600 s (10 min hard cap matches the
            // ceiling on gradualVolumeSeconds so the two can pair cleanly).
            vibrationDelaySeconds = vibrationDelaySeconds.coerceIn(0, 600),
            weatherEarlyMinutes = weatherEarlyMinutes.coerceIn(0, 60),
            requiredSquats = requiredSquats.coerceIn(1, 100),
            dismissActionType = when (dismissActionType.uppercase(Locale.US)) {
                "WEBHOOK", "HUE_SCENE", "BROADCAST" -> dismissActionType.uppercase(Locale.US)
                else -> "NONE"
            },
            dismissActionPayload = dismissActionPayload.trim().take(MAX_URI_CHARS),
            firingBackgroundImageEnabled = firingBackgroundImageEnabled &&
                normalizedFiringBackgroundImageUri.isNotBlank(),
            firingBackgroundImageUri = normalizedFiringBackgroundImageUri,
            sortOrder = sortOrder.coerceIn(0, Int.MAX_VALUE),
            shiftPattern = normalizedShiftPattern,
            shiftPatternStartDate = normalizedShiftPatternStartDate,
            timezonePolicy = normalizedTimezonePolicy,
            fixedTimezoneId = if (normalizedTimezonePolicy == TIMEZONE_POLICY_FIXED) {
                normalizedFixedTimezoneId
            } else {
                ""
            }
        )
    }
}
