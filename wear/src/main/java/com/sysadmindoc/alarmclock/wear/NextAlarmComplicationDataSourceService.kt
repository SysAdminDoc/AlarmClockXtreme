package com.sysadmindoc.alarmclock.wear

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class NextAlarmComplicationDataSourceService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val snapshot = WearAlarmStore.load(applicationContext)
        return complicationData(request.complicationType, snapshot)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        complicationData(
            type = type,
            snapshot = WearAlarmSnapshot(
                hasAlarm = true,
                label = "Morning",
                timeLabel = "7:30",
                triggerTime = System.currentTimeMillis() + 3_600_000L,
                updatedAt = System.currentTimeMillis()
            )
        )

    private fun complicationData(
        type: ComplicationType,
        snapshot: WearAlarmSnapshot
    ): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> shortTextData(snapshot)
        ComplicationType.LONG_TEXT -> longTextData(snapshot)
        else -> NoDataComplicationData()
    }

    private fun shortTextData(snapshot: WearAlarmSnapshot): ShortTextComplicationData {
        val text = when {
            snapshot.isFiring -> "Ringing"
            snapshot.hasAlarm && snapshot.timeLabel.isNotBlank() -> snapshot.timeLabel
            snapshot.hasAlarm -> "Alarm"
            else -> "No alarm"
        }
        val title = when {
            snapshot.isFiring -> "ACX"
            snapshot.hasAlarm -> snapshot.label.ifBlank { "Next" }.take(SHORT_TITLE_LIMIT)
            else -> "ACX"
        }

        return ShortTextComplicationData.Builder(
            text = plainText(text),
            contentDescription = plainText(contentDescription(snapshot))
        )
            .setTitle(plainText(title))
            .setMonochromaticImage(icon())
            .build()
    }

    private fun longTextData(snapshot: WearAlarmSnapshot): LongTextComplicationData {
        val text = when {
            snapshot.isFiring -> "Alarm is ringing"
            snapshot.hasAlarm -> listOf(
                snapshot.timeLabel.ifBlank { "Scheduled" },
                snapshot.label,
                formatRemaining(snapshot.triggerTime)
            ).filter { it.isNotBlank() }.joinToString(" - ")
            else -> "No phone alarm synced"
        }

        return LongTextComplicationData.Builder(
            text = plainText(text),
            contentDescription = plainText(contentDescription(snapshot))
        )
            .setTitle(plainText("AlarmClockXtreme"))
            .setMonochromaticImage(icon())
            .build()
    }

    private fun contentDescription(snapshot: WearAlarmSnapshot): String = when {
        snapshot.isFiring -> "AlarmClockXtreme alarm is ringing"
        snapshot.hasAlarm -> "Next AlarmClockXtreme alarm ${snapshot.timeLabel.ifBlank { "scheduled" }}"
        else -> "No AlarmClockXtreme alarm synced from phone"
    }

    private fun formatRemaining(triggerTime: Long): String {
        val diff = triggerTime - System.currentTimeMillis()
        if (diff <= 0L) return "due now"
        val days = diff / 86_400_000L
        val hours = (diff % 86_400_000L) / 3_600_000L
        val minutes = (diff % 3_600_000L) / 60_000L
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    private fun plainText(text: String) = PlainComplicationText.Builder(text).build()

    private fun icon() = MonochromaticImage.Builder(
        Icon.createWithResource(this, R.drawable.ic_alarm)
    ).build()

    private companion object {
        const val SHORT_TITLE_LIMIT = 12
    }
}
