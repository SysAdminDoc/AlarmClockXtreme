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
        return ShortTextComplicationData.Builder(
            text = plainText(WearAlarmText.complicationShortText(snapshot)),
            contentDescription = plainText(WearAlarmText.contentDescription(snapshot))
        )
            .setTitle(plainText(WearAlarmText.complicationShortTitle(snapshot)))
            .setMonochromaticImage(icon())
            .build()
    }

    private fun longTextData(snapshot: WearAlarmSnapshot): LongTextComplicationData {
        return LongTextComplicationData.Builder(
            text = plainText(WearAlarmText.complicationLongText(snapshot)),
            contentDescription = plainText(WearAlarmText.contentDescription(snapshot))
        )
            .setTitle(plainText("AlarmClockXtreme"))
            .setMonochromaticImage(icon())
            .build()
    }

    private fun plainText(text: String) = PlainComplicationText.Builder(text).build()

    private fun icon() = MonochromaticImage.Builder(
        Icon.createWithResource(this, R.drawable.ic_alarm)
    ).build()
}
