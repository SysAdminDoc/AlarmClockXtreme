package com.sysadmindoc.alarmclock.wear

import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography.BODY_LARGE
import androidx.wear.protolayout.material3.Typography.BODY_MEDIUM
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// DO NOT RENAME: Wear OS identifies tile placements by service class name.
// Renaming this class would orphan every user's carousel slot. When migrating
// to Wear Widgets (Glance + RemoteCompose), keep this exact class name and
// map it via the Wear OS 7 widget `group` config attribute.
class NextAlarmTileService : TileService() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            scope.launch {
                try {
                    val actionStatus = handleActionIfNeeded(
                        clickableId = requestParams.currentState.lastClickableId,
                        snapshot = WearAlarmStore.load(applicationContext)
                    )
                    val snapshot = readLatestSnapshot()
                    completer.set(buildTile(requestParams, snapshot, actionStatus))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to build Wear next-alarm tile", e)
                    completer.set(buildTile(requestParams, WearAlarmStore.load(applicationContext), "Sync delayed"))
                }
            }
            "NextAlarmTileService#onTileRequest"
        }
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> {
        return CallbackToFutureAdapter.getFuture { completer ->
            completer.set(
                Resources.Builder()
                    .setVersion(RESOURCES_VERSION)
                    .build()
            )
            "NextAlarmTileService#onTileResourcesRequest"
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun buildTile(
        requestParams: RequestBuilders.TileRequest,
        snapshot: WearAlarmSnapshot,
        actionStatus: String?,
    ): Tile {
        return Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(60_000L)
            .setTileTimeline(
                Timeline.fromLayoutElement(
                    materialScope(
                        context = this,
                        deviceConfiguration = requestParams.deviceConfiguration,
                        allowDynamicTheme = false,
                    ) {
                        tileLayout(snapshot, actionStatus)
                    }
                )
            )
            .build()
    }

    private fun MaterialScope.tileLayout(
        snapshot: WearAlarmSnapshot,
        actionStatus: String?,
    ): LayoutElementBuilders.LayoutElement {
        return primaryLayout(
            titleSlot = {
                text("AlarmClockXtreme".layoutString, typography = BODY_MEDIUM)
            },
            mainSlot = {
                LayoutElementBuilders.Column.Builder()
                    .addContent(
                        text(
                            if (snapshot.hasAlarm) "Next alarm".layoutString else "No alarm".layoutString,
                            typography = BODY_MEDIUM
                        )
                    )
                    .addContent(
                        text(
                            mainTimeLabel(snapshot).layoutString,
                            typography = BODY_LARGE
                        )
                    )
                    .addContent(
                        text(
                            secondaryLabel(snapshot, actionStatus).layoutString,
                            typography = BODY_MEDIUM
                        )
                    )
                    .build()
            },
            bottomSlot = {
                bottomControls(snapshot)
            },
        )
    }

    private fun MaterialScope.bottomControls(
        snapshot: WearAlarmSnapshot,
    ): LayoutElementBuilders.LayoutElement {
        if (!snapshot.hasAlarm) {
            return textButton(
                shape = shapes.small,
                labelContent = { text("Sync".layoutString) },
                onClick = clickable(id = CLICK_REFRESH, action = loadAction()),
            )
        }

        if (snapshot.isFiring) {
            return buttonGroup {
                buttonGroupItem {
                    textButton(
                        shape = shapes.small,
                        labelContent = { text("Snooze".layoutString) },
                        onClick = clickable(id = WearAlarmData.CLICK_SNOOZE, action = loadAction()),
                    )
                }
                buttonGroupItem {
                    textButton(
                        shape = shapes.small,
                        labelContent = { text("Dismiss".layoutString) },
                        onClick = clickable(id = WearAlarmData.CLICK_DISMISS, action = loadAction()),
                    )
                }
            }
        }

        return textButton(
            shape = shapes.small,
            labelContent = { text("Skip next".layoutString) },
            onClick = clickable(id = WearAlarmData.CLICK_SKIP, action = loadAction()),
        )
    }

    private fun mainTimeLabel(snapshot: WearAlarmSnapshot): String =
        WearAlarmText.mainTimeLabel(snapshot)

    private fun secondaryLabel(snapshot: WearAlarmSnapshot, actionStatus: String?): String =
        WearAlarmText.secondaryLabel(snapshot, actionStatus)

    private fun readLatestSnapshot(): WearAlarmSnapshot {
        val cached = WearAlarmStore.load(applicationContext)
        val buffer = runCatching {
            Tasks.await(
                Wearable.getDataClient(applicationContext).dataItems,
                1_200L,
                TimeUnit.MILLISECONDS
            )
        }.getOrNull() ?: return cached

        try {
            buffer.forEach { item ->
                if (item.uri.path == WearAlarmData.PATH_NEXT_ALARM) {
                    val snapshot = WearAlarmStore.fromDataMap(
                        DataMapItem.fromDataItem(item).dataMap
                    )
                    WearAlarmStore.save(applicationContext, snapshot)
                    return snapshot
                }
            }
        } finally {
            buffer.release()
        }
        return cached
    }

    private fun handleActionIfNeeded(
        clickableId: String,
        snapshot: WearAlarmSnapshot,
    ): String? {
        val path = WearAlarmData.actionPathForClick(clickableId) ?: return null
        if (!snapshot.hasAlarm || snapshot.alarmId <= 0L) return "Phone sync needed"

        val payload = DataMap().apply {
            putLong(WearAlarmData.KEY_ALARM_ID, snapshot.alarmId)
            putLong(WearAlarmData.KEY_UPDATED_AT, System.currentTimeMillis())
        }.toByteArray()

        val nodes = runCatching {
            Tasks.await(
                Wearable.getNodeClient(applicationContext).connectedNodes,
                1_200L,
                TimeUnit.MILLISECONDS
            )
        }.getOrDefault(emptyList())

        if (nodes.isEmpty()) return "Phone unavailable"
        nodes.forEach { node ->
            Wearable.getMessageClient(applicationContext)
                .sendMessage(node.id, path, payload)
        }
        return "Sent to phone"
    }

    companion object {
        private const val TAG = "WearNextAlarmTile"
        private const val RESOURCES_VERSION = "1"
        private const val CLICK_REFRESH = "refresh"
    }
}
