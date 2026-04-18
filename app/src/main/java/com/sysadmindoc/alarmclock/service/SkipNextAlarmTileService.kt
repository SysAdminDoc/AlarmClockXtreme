package com.sysadmindoc.alarmclock.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.receiver.SkipNextReceiver
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * v1.4.0: Quick Settings tile that shows the next scheduled alarm and
 * skips it with one tap from the system shade.
 *
 * Inactive = no upcoming alarm. Active (bell icon highlighted) = alarm is next.
 * Tap delegates the skip logic to [SkipNextReceiver] so skip/dismiss semantics
 * stay consistent with the persistent next-alarm notification action.
 */
class SkipNextAlarmTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val ep = EntryPointAccessors.fromApplication(
                applicationContext,
                AlarmClockApp.AppEntryPoint::class.java
            )
            val next = ep.alarmRepository().getNextAlarm() ?: return@launch
            // Route through SkipNextReceiver so skip semantics stay identical to
            // the persistent notification path (records event, recomputes next
            // occurrence for repeating, disables one-shot).
            val skip = Intent(applicationContext, SkipNextReceiver::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, next.id)
            }
            applicationContext.sendBroadcast(skip)
            // v1.5.1: Refresh twice — once immediately (optimistic "skipped"
            // state) and once after the broadcast has realistically
            // propagated through SkipNextReceiver -> repository -> DAO.
            // Without the second refresh the tile shows the pre-skip time
            // until the user next swipes the shade.
            refreshTile()
            delay(600L)
            refreshTile()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        scope.launch {
            val ep = EntryPointAccessors.fromApplication(
                applicationContext,
                AlarmClockApp.AppEntryPoint::class.java
            )
            val next = ep.alarmRepository().getNextAlarm()
            val label: String
            val subtitleCandidate: String?
            val state: Int
            if (next == null || next.nextTriggerTime <= 0L) {
                label = "No alarm"
                subtitleCandidate = "Tap to re-check"
                state = Tile.STATE_INACTIVE
            } else {
                val time = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(next.nextTriggerTime),
                    ZoneId.systemDefault()
                )
                val timeLabel = time.format(DateTimeFormatter.ofPattern("EEE h:mm a"))
                label = "Skip $timeLabel"
                subtitleCandidate = if (next.label.isBlank()) "Next alarm" else next.label
                state = Tile.STATE_ACTIVE
            }

            tile.icon = Icon.createWithResource(applicationContext, R.drawable.ic_alarm)
            tile.label = label
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = subtitleCandidate
            }
            tile.state = state
            tile.updateTile()
        }
    }
}
