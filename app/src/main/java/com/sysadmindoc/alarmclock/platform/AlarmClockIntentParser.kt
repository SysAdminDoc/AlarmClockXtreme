@file:Suppress("DEPRECATION")

package com.sysadmindoc.alarmclock.platform

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import androidx.core.net.toUri
import java.time.DayOfWeek
import java.util.Calendar

sealed interface AlarmClockCommand {
    val fingerprint: String

    data class OpenAlarmEditor(override val fingerprint: String) : AlarmClockCommand

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val label: String,
        val repeatDays: Set<DayOfWeek>,
        val ringtoneUri: String,
        val vibrate: Boolean,
        val skipUi: Boolean,
        override val fingerprint: String
    ) : AlarmClockCommand

    data class DismissAlarm(
        val search: AlarmSearch,
        override val fingerprint: String
    ) : AlarmClockCommand

    data class SnoozeAlarm(
        val durationMinutes: Int?,
        override val fingerprint: String
    ) : AlarmClockCommand

    data class OpenTimer(override val fingerprint: String) : AlarmClockCommand

    data class SetTimer(
        val lengthSeconds: Int,
        val label: String,
        val skipUi: Boolean,
        override val fingerprint: String
    ) : AlarmClockCommand
}

sealed interface AlarmSearch {
    data class ById(val id: Long) : AlarmSearch
    data object Active : AlarmSearch
    data object Next : AlarmSearch
    data object All : AlarmSearch
    data class Label(val query: String) : AlarmSearch
    data class Time(val hour: Int?, val minute: Int?) : AlarmSearch
}

sealed interface AlarmClockParseResult {
    data class Valid(val command: AlarmClockCommand) : AlarmClockParseResult
    data object Invalid : AlarmClockParseResult
    data object Unsupported : AlarmClockParseResult
}

object AlarmClockIntentParser {
    private const val MAX_LABEL_CHARS = 120
    private const val MAX_SNOOZE_MINUTES = 120
    private const val ALARM_LINK_SCHEME = "acx"
    private const val ALARM_LINK_HOST = "alarm"

    fun parse(intent: Intent?): AlarmClockParseResult {
        intent ?: return AlarmClockParseResult.Unsupported
        return when (intent.action) {
            AlarmClock.ACTION_SET_ALARM -> parseSetAlarm(intent)
            AlarmClock.ACTION_DISMISS_ALARM -> parseDismiss(intent)
            AlarmClock.ACTION_SNOOZE_ALARM -> parseSnooze(intent)
            AlarmClock.ACTION_SET_TIMER -> parseSetTimer(intent)
            else -> AlarmClockParseResult.Unsupported
        }
    }

    private fun parseSetAlarm(intent: Intent): AlarmClockParseResult {
        val hour = intent.typedInt(AlarmClock.EXTRA_HOUR)
        if (hour is Extra.Malformed) return AlarmClockParseResult.Invalid
        if (hour is Extra.Missing) {
            if (intent.hasExtra(AlarmClock.EXTRA_MINUTES)) return AlarmClockParseResult.Invalid
            return valid(AlarmClockCommand.OpenAlarmEditor("set-alarm:open"))
        }
        val hourValue = (hour as Extra.Present).value
        if (hourValue !in 0..23) return AlarmClockParseResult.Invalid

        val minute = intent.typedInt(AlarmClock.EXTRA_MINUTES)
        if (minute is Extra.Malformed) return AlarmClockParseResult.Invalid
        val minuteValue = (minute as? Extra.Present)?.value ?: 0
        if (minuteValue !in 0..59) return AlarmClockParseResult.Invalid

        val days = parseDays(intent) ?: return AlarmClockParseResult.Invalid
        val label = intent.typedString(AlarmClock.EXTRA_MESSAGE)
            ?: return AlarmClockParseResult.Invalid
        val ringtone = intent.typedString(AlarmClock.EXTRA_RINGTONE)
            ?: return AlarmClockParseResult.Invalid
        if (ringtone.isNotEmpty() &&
            ringtone != AlarmClock.VALUE_RINGTONE_SILENT &&
            ringtone.toUri().scheme != "content"
        ) {
            return AlarmClockParseResult.Invalid
        }
        val vibrate = intent.typedBoolean(AlarmClock.EXTRA_VIBRATE, default = true)
            ?: return AlarmClockParseResult.Invalid
        val skipUi = intent.typedBoolean(AlarmClock.EXTRA_SKIP_UI)
            ?: return AlarmClockParseResult.Invalid
        val normalizedLabel = label.take(MAX_LABEL_CHARS)
        val fingerprint = buildString {
            append("set-alarm:").append(hourValue).append(':').append(minuteValue)
            append(':').append(days.map { it.value }.sorted().joinToString(","))
            append(':').append(normalizedLabel).append(':').append(ringtone)
            append(':').append(vibrate).append(':').append(skipUi)
        }
        return valid(
            AlarmClockCommand.SetAlarm(
                hour = hourValue,
                minute = minuteValue,
                label = normalizedLabel,
                repeatDays = days,
                ringtoneUri = ringtone,
                vibrate = vibrate,
                skipUi = skipUi,
                fingerprint = fingerprint
            )
        )
    }

    private fun parseDismiss(intent: Intent): AlarmClockParseResult {
        parseAlarmLinkId(intent.data)?.let { id ->
            return valid(AlarmClockCommand.DismissAlarm(AlarmSearch.ById(id), "dismiss:id:$id"))
        }
        if (intent.data != null) return AlarmClockParseResult.Invalid

        val mode = intent.typedString(AlarmClock.EXTRA_ALARM_SEARCH_MODE)
            ?: return AlarmClockParseResult.Invalid
        val search = when (mode) {
            "" -> AlarmSearch.Active
            AlarmClock.ALARM_SEARCH_MODE_NEXT -> AlarmSearch.Next
            AlarmClock.ALARM_SEARCH_MODE_ALL -> AlarmSearch.All
            AlarmClock.ALARM_SEARCH_MODE_LABEL -> {
                val label = intent.typedString(AlarmClock.EXTRA_MESSAGE)
                    ?: return AlarmClockParseResult.Invalid
                val query = label.trim().take(MAX_LABEL_CHARS)
                if (query.isEmpty()) return AlarmClockParseResult.Invalid
                AlarmSearch.Label(query)
            }
            AlarmClock.ALARM_SEARCH_MODE_TIME -> parseTimeSearch(intent)
                ?: return AlarmClockParseResult.Invalid
            else -> return AlarmClockParseResult.Invalid
        }
        return valid(AlarmClockCommand.DismissAlarm(search, "dismiss:$search"))
    }

    private fun parseSnooze(intent: Intent): AlarmClockParseResult {
        val duration = intent.typedInt(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION)
        if (duration is Extra.Malformed) return AlarmClockParseResult.Invalid
        val minutes = (duration as? Extra.Present)?.value
        if (minutes != null && minutes !in 1..MAX_SNOOZE_MINUTES) {
            return AlarmClockParseResult.Invalid
        }
        return valid(
            AlarmClockCommand.SnoozeAlarm(
                durationMinutes = minutes,
                fingerprint = "snooze:${minutes ?: "default"}"
            )
        )
    }

    private fun parseSetTimer(intent: Intent): AlarmClockParseResult {
        val length = intent.typedInt(AlarmClock.EXTRA_LENGTH)
        if (length is Extra.Malformed) return AlarmClockParseResult.Invalid
        if (length is Extra.Missing) {
            return valid(AlarmClockCommand.OpenTimer("set-timer:open"))
        }
        val seconds = (length as Extra.Present).value
        if (seconds !in 1..86_400) return AlarmClockParseResult.Invalid
        val label = intent.typedString(AlarmClock.EXTRA_MESSAGE)
            ?: return AlarmClockParseResult.Invalid
        val skipUi = intent.typedBoolean(AlarmClock.EXTRA_SKIP_UI)
            ?: return AlarmClockParseResult.Invalid
        val normalizedLabel = label.take(MAX_LABEL_CHARS)
        return valid(
            AlarmClockCommand.SetTimer(
                lengthSeconds = seconds,
                label = normalizedLabel,
                skipUi = skipUi,
                fingerprint = "set-timer:$seconds:$normalizedLabel:$skipUi"
            )
        )
    }

    private fun parseTimeSearch(intent: Intent): AlarmSearch.Time? {
        val rawHour = intent.typedInt(AlarmClock.EXTRA_HOUR)
        val minute = intent.typedInt(AlarmClock.EXTRA_MINUTES)
        val isPm = intent.typedBooleanExtra(AlarmClock.EXTRA_IS_PM)
        if (rawHour is Extra.Malformed || minute is Extra.Malformed || isPm is Extra.Malformed) return null
        if (rawHour is Extra.Missing && minute is Extra.Missing && isPm is Extra.Missing) return null

        var hour = (rawHour as? Extra.Present)?.value
        val minuteValue = (minute as? Extra.Present)?.value
        if (minuteValue != null && minuteValue !in 0..59) return null
        val pm = (isPm as? Extra.Present)?.value
        if (pm != null) {
            val twelveHour = hour ?: return null
            if (twelveHour !in 1..12) return null
            hour = (twelveHour % 12) + if (pm) 12 else 0
        } else if (hour != null && hour !in 0..23) {
            return null
        }
        return AlarmSearch.Time(hour, minuteValue)
    }

    private fun parseDays(intent: Intent): Set<DayOfWeek>? {
        if (!intent.hasExtra(AlarmClock.EXTRA_DAYS)) return emptySet()
        val raw = intent.extras?.get(AlarmClock.EXTRA_DAYS) as? ArrayList<*> ?: return null
        return raw.map { value ->
            when (value as? Int) {
                Calendar.SUNDAY -> DayOfWeek.SUNDAY
                Calendar.MONDAY -> DayOfWeek.MONDAY
                Calendar.TUESDAY -> DayOfWeek.TUESDAY
                Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
                Calendar.THURSDAY -> DayOfWeek.THURSDAY
                Calendar.FRIDAY -> DayOfWeek.FRIDAY
                Calendar.SATURDAY -> DayOfWeek.SATURDAY
                else -> return null
            }
        }.toSet()
    }

    private fun parseAlarmLinkId(uri: Uri?): Long? {
        if (uri?.scheme != ALARM_LINK_SCHEME || uri.host != ALARM_LINK_HOST) return null
        return uri.pathSegments.singleOrNull()?.toLongOrNull()?.takeIf { it > 0L }
    }

    private fun valid(command: AlarmClockCommand) = AlarmClockParseResult.Valid(command)

    private sealed interface Extra<out T> {
        data object Missing : Extra<Nothing>
        data object Malformed : Extra<Nothing>
        data class Present<T>(val value: T) : Extra<T>
    }

    private fun Intent.typedInt(name: String): Extra<Int> = typedExtra(name)
    private fun Intent.typedBooleanExtra(name: String): Extra<Boolean> = typedExtra(name)

    private inline fun <reified T> Intent.typedExtra(name: String): Extra<T> {
        if (!hasExtra(name)) return Extra.Missing
        val value = extras?.get(name)
        return if (value is T) Extra.Present(value) else Extra.Malformed
    }

    private fun Intent.typedString(name: String): String? {
        if (!hasExtra(name)) return ""
        return extras?.get(name) as? String
    }

    private fun Intent.typedBoolean(name: String, default: Boolean = false): Boolean? {
        if (!hasExtra(name)) return default
        return extras?.get(name) as? Boolean
    }
}
