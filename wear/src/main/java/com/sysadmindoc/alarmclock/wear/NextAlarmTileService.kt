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

class NextAlarmTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                        onClick = clickable(id = CLICK_SNOOZE, action = loadAction()),
                    )
                }
                buttonGroupItem {
                    textButton(
                        shape = shapes.small,
                        labelContent = { text("Dismiss".layoutString) },
                        onClick = clickable(id = CLICK_DISMISS, action = loadAction()),
                    )
                }
            }
        }

        return textButton(
            shape = shapes.small,
            labelContent = { text("Skip next".layoutString) },
            onClick = clickable(id = CLICK_SKIP, action = loadAction()),
        )
    }

    private fun mainTimeLabel(snapshot: WearAlarmSnapshot): String {
        return when {
            !snapshot.hasAlarm -> "Open phone app"
            snapshot.timeLabel.isNotBlank() -> snapshot.timeLabel
            else -> "Scheduled"
        }
    }

    private fun secondaryLabel(snapshot: WearAlarmSnapshot, actionStatus: String?): String {
        actionStatus?.let { return it }
        if (!snapshot.hasAlarm) return "Waiting for phone sync"
        if (snapshot.isFiring) return "Alarm is ringing"
        val remaining = formatRemaining(snapshot.triggerTime)
        return listOf(snapshot.label, remaining)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { "Ready on phone" }
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
        val path = when (clickableId) {
            CLICK_SKIP -> WearAlarmData.PATH_ACTION_SKIP
            CLICK_SNOOZE -> WearAlarmData.PATH_ACTION_SNOOZE
            CLICK_DISMISS -> WearAlarmData.PATH_ACTION_DISMISS
            else -> return null
        }
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
        private const val CLICK_SKIP = "skip"
        private const val CLICK_SNOOZE = "snooze"
        private const val CLICK_DISMISS = "dismiss"
    }
}
