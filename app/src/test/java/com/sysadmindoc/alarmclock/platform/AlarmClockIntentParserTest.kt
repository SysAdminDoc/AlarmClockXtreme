package com.sysadmindoc.alarmclock.platform

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import java.time.DayOfWeek
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AlarmClockIntentParserTest {
    @Test
    fun setAlarmParsesPlatformDefaultsAndRepeatDays() {
        val parsed = AlarmClockIntentParser.parse(
            Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, 6)
                .putIntegerArrayListExtra(
                    AlarmClock.EXTRA_DAYS,
                    arrayListOf(Calendar.MONDAY, Calendar.FRIDAY)
                )
        ) as AlarmClockParseResult.Valid

        val command = parsed.command as AlarmClockCommand.SetAlarm
        assertEquals(6, command.hour)
        assertEquals(0, command.minute)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), command.repeatDays)
        assertTrue(command.vibrate)
        assertTrue(!command.skipUi)
    }

    @Test
    fun setAlarmWithoutTimeOpensEditor() {
        val parsed = AlarmClockIntentParser.parse(Intent(AlarmClock.ACTION_SET_ALARM))

        assertTrue((parsed as AlarmClockParseResult.Valid).command is AlarmClockCommand.OpenAlarmEditor)
    }

    @Test
    fun malformedAlarmTimeAndRingtoneAreRejected() {
        val invalidTime = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, 24)
        val invalidRingtone = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, 7)
            .putExtra(AlarmClock.EXTRA_RINGTONE, "https://example.test/tone.mp3")

        assertEquals(AlarmClockParseResult.Invalid, AlarmClockIntentParser.parse(invalidTime))
        assertEquals(AlarmClockParseResult.Invalid, AlarmClockIntentParser.parse(invalidRingtone))
    }

    @Test
    fun setTimerHonorsLengthMessageAndSkipUi() {
        val parsed = AlarmClockIntentParser.parse(
            Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, 90)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Tea")
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        ) as AlarmClockParseResult.Valid

        val command = parsed.command as AlarmClockCommand.SetTimer
        assertEquals(90, command.lengthSeconds)
        assertEquals("Tea", command.label)
        assertTrue(command.skipUi)
    }

    @Test
    fun missingTimerLengthOpensTimerAndOutOfRangeDoesNot() {
        val open = AlarmClockIntentParser.parse(Intent(AlarmClock.ACTION_SET_TIMER))
        val invalid = AlarmClockIntentParser.parse(
            Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, 86_401)
        )

        assertTrue((open as AlarmClockParseResult.Valid).command is AlarmClockCommand.OpenTimer)
        assertEquals(AlarmClockParseResult.Invalid, invalid)
    }

    @Test
    fun dismissSupportsDeepLinkLabelAndTwelveHourTimeSearch() {
        val byId = AlarmClockIntentParser.parse(
            Intent(AlarmClock.ACTION_DISMISS_ALARM, Uri.parse("acx://alarm/42"))
        ) as AlarmClockParseResult.Valid
        val byLabel = AlarmClockIntentParser.parse(
            Intent(AlarmClock.ACTION_DISMISS_ALARM)
                .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "school")
        ) as AlarmClockParseResult.Valid
        val byTime = AlarmClockIntentParser.parse(
            Intent(AlarmClock.ACTION_DISMISS_ALARM)
                .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_TIME)
                .putExtra(AlarmClock.EXTRA_HOUR, 7)
                .putExtra(AlarmClock.EXTRA_IS_PM, true)
        ) as AlarmClockParseResult.Valid

        assertEquals(AlarmSearch.ById(42), (byId.command as AlarmClockCommand.DismissAlarm).search)
        assertEquals(AlarmSearch.Label("school"), (byLabel.command as AlarmClockCommand.DismissAlarm).search)
        assertEquals(AlarmSearch.Time(19, null), (byTime.command as AlarmClockCommand.DismissAlarm).search)
    }

    @Test
    fun malformedSnoozeDurationIsRejected() {
        val parsed = AlarmClockIntentParser.parse(
            Intent(AlarmClock.ACTION_SNOOZE_ALARM)
                .putExtra(AlarmClock.EXTRA_ALARM_SNOOZE_DURATION, 0)
        )

        assertEquals(AlarmClockParseResult.Invalid, parsed)
    }
}
