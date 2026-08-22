package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.AccentRed
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import com.sysadmindoc.alarmclock.worker.WakeConfirmWorker
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay

class WakeConfirmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_FIRE_ID = "alarm_fire_id"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
        const val EXTRA_COUNTDOWN_SECONDS = "countdown_seconds"

        /**
         * Wall-clock instant the worker will stop waiting. Shared so the screen
         * shows the time that is actually left rather than restarting its own
         * countdown whenever the activity happens to open.
         */
        const val EXTRA_DEADLINE_MILLIS = "deadline_millis"
        const val EXTRA_REFIRE_COUNT = "refire_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L)
        val fireId = intent.getStringExtra(EXTRA_ALARM_FIRE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)
        val countdownSeconds = intent.getIntExtra(
            EXTRA_COUNTDOWN_SECONDS,
            WakeConfirmWorker.CONFIRM_WAIT_SECONDS
        )
        val deadlineMillis = intent.getLongExtra(EXTRA_DEADLINE_MILLIS, 0L)
            .takeIf { it > 0L }
            ?: (System.currentTimeMillis() + countdownSeconds * 1_000L)
        val refireCount = intent.getIntExtra(EXTRA_REFIRE_COUNT, 0)
        val remainingRefires = WakeConfirmWorker.MAX_REFIRES - refireCount

        recordIncidentAsync(
            alarmId = alarmId,
            fireId = fireId,
            scheduledAt = scheduledAt,
            status = AlarmIncidentEvent.STATUS_RECEIVED,
            reasonCode = "WAKE_CONFIRM_ACTIVITY_OPENED"
        )

        setContent {
            AlarmClockXtremeTheme {
                WakeConfirmScreen(
                    countdownSeconds = countdownSeconds,
                    deadlineMillis = deadlineMillis,
                    remainingRefires = remainingRefires,
                    onConfirmAwake = {
                        if (alarmId != -1L) {
                            val prefs = getSharedPreferences("wake_confirm", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("confirmed_$alarmId", true).apply()
                        }
                        recordIncidentAsync(
                            alarmId = alarmId,
                            fireId = fireId,
                            scheduledAt = scheduledAt,
                            status = AlarmIncidentEvent.STATUS_SUCCEEDED,
                            reasonCode = "WAKE_CONFIRM_CONFIRMED_IN_ACTIVITY"
                        )
                        finish()
                    },
                    onKeepChecking = {
                        recordIncidentAsync(
                            alarmId = alarmId,
                            fireId = fireId,
                            scheduledAt = scheduledAt,
                            status = AlarmIncidentEvent.STATUS_SKIPPED,
                            reasonCode = "WAKE_CONFIRM_KEEP_CHECKING"
                        )
                        finish()
                    }
                )
            }
        }
    }

    private fun recordIncidentAsync(
        alarmId: Long,
        fireId: String,
        scheduledAt: Long,
        status: String,
        reasonCode: String
    ) {
        if (alarmId <= 0L) return
        EntryPointAccessors
            .fromApplication(
                applicationContext,
                AlarmClockApp.AppEntryPoint::class.java
            )
            .alarmIncidentRepository()
            .recordAsync(
                alarmId = alarmId,
                fireId = fireId.ifBlank { AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt) },
                scheduledAt = scheduledAt,
                type = AlarmIncidentEvent.TYPE_WAKE_CONFIRM,
                status = status,
                reasonCode = reasonCode,
                source = "WakeConfirmActivity"
            )
    }
}

@Composable
private fun WakeConfirmScreen(
    countdownSeconds: Int,
    deadlineMillis: Long,
    remainingRefires: Int,
    onConfirmAwake: () -> Unit,
    onKeepChecking: () -> Unit
) {
    // Counted against the worker's own deadline. Before this the screen
    // restarted a full countdown on every open and on every rotation, so it
    // could promise a minute while the alarm was seconds from re-firing.
    var secondsLeft by remember { mutableIntStateOf(countdownSeconds) }

    LaunchedEffect(deadlineMillis) {
        while (true) {
            val remainingMs = deadlineMillis - System.currentTimeMillis()
            secondsLeft = ((remainingMs + 999L) / 1_000L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
            if (secondsLeft <= 0) break
            delay(250L)
        }
    }

    val progress = if (countdownSeconds > 0) {
        secondsLeft.toFloat() / countdownSeconds
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DismissGreen.copy(alpha = 0.16f),
                        SurfaceDark
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(96.dp),
                            color = if (secondsLeft > 10) DismissGreen else AccentRed,
                            trackColor = SurfaceDark.copy(alpha = 0.3f),
                            strokeWidth = 6.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = "${secondsLeft}s",
                            color = if (secondsLeft > 10) DismissGreen else AccentRed,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.wake_confirm_question),
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = if (secondsLeft > 0) {
                            "Confirm within ${secondsLeft}s or the alarm will ring again."
                        } else {
                            "Time's up — the alarm is ringing again."
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        // Deliberately NOT a live region: the seconds change
                        // every tick, and announcing the whole sentence sixty
                        // times would bury every other control. The one thing
                        // worth announcing is the flip to "time's up", handled
                        // by the separate live region below.
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = if (secondsLeft > 0) {
                                "Confirm within $secondsLeft seconds or the alarm will ring again."
                            } else {
                                ""
                            }
                        }
                    )
                    // The one transition worth interrupting for.
                    if (secondsLeft <= 0) {
                        Text(
                            text = stringResource(R.string.wake_confirm_ui_time_s_up_alarm_ringing),
                            color = SnoozeYellow,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Assertive
                            }
                        )
                    }
                }

                AppSectionTitle(
                    title = stringResource(R.string.wake_confirm_title),
                    description = stringResource(R.string.wake_confirm_ui_quick_second_check_alarms_need)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppStatusChip(
                        label = stringResource(R.string.wake_confirm_confirm_label),
                        icon = Icons.Default.CheckCircle,
                        color = DismissGreen
                    )
                    if (remainingRefires > 0) {
                        AppStatusChip(
                            label = pluralStringResource(
                                R.plurals.alarmfiring_more_checks_left,
                                remainingRefires,
                                remainingRefires
                            ),
                            icon = Icons.Default.WarningAmber,
                            color = AccentRed
                        )
                    } else {
                        AppStatusChip(
                            label = stringResource(R.string.wake_confirm_final_label),
                            icon = Icons.Default.WarningAmber,
                            color = AccentRed
                        )
                    }
                }

                Button(
                    onClick = onConfirmAwake,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DismissGreen),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = stringResource(R.string.wake_confirm_yes),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onKeepChecking,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.wake_confirm_not_yet)
                    )
                }

                Text(
                    text = stringResource(
                        R.string.wake_confirm_keep_protection,
                        WakeConfirmWorker.MAX_REFIRES
                    ),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
