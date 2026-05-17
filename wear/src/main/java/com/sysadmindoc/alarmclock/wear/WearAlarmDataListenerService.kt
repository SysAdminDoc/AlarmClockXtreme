package com.sysadmindoc.alarmclock.wear

import android.content.ComponentName
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

class WearAlarmDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        var changed = false
        dataEvents.forEach { event ->
            val item = event.dataItem
            if (item.uri.path != WearAlarmData.PATH_NEXT_ALARM) return@forEach
            if (event.type == DataEvent.TYPE_CHANGED) {
                val snapshot = WearAlarmStore.fromDataMap(
                    DataMapItem.fromDataItem(item).dataMap
                )
                WearAlarmStore.save(applicationContext, snapshot)
                changed = true
            }
        }
        if (changed) {
            TileService.getUpdater(applicationContext)
                .requestUpdate(NextAlarmTileService::class.java)
            ComplicationDataSourceUpdateRequester.create(
                context = applicationContext,
                complicationDataSourceComponent = ComponentName(
                    applicationContext,
                    NextAlarmComplicationDataSourceService::class.java
                )
            ).requestUpdateAll()
        }
    }
}
