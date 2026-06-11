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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import dagger.hilt.android.EntryPointAccessors

/**
 * F5: Wake confirmation activity.
 * Launched from the wake-confirmation notification. Tapping "I'm Awake" marks
 * the alarm as confirmed so WakeConfirmWorker won't re-fire it.
 */
class WakeConfirmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_FIRE_ID = "alarm_fire_id"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
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
        // Repository-owned scope: records here precede finish(), which would
        // cancel lifecycleScope mid-write and drop the event.
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
    onConfirmAwake: () -> Unit,
    onKeepChecking: () -> Unit
) {
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
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = DismissGreen,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "Are you actually up?",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Confirming here stops the follow-up alarm check. If you skip this, the app can ring again to make sure you did not drift back to sleep.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                AppSectionTitle(
                    title = "Wake confirmation",
                    description = "A quick second check for alarms that need extra accountability."
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppStatusChip(
                        label = "Confirm to stop re-checks",
                        icon = Icons.Default.CheckCircle,
                        color = DismissGreen
                    )
                    AppStatusChip(
                        label = "Skip and the alarm may ring again",
                        icon = Icons.Default.WarningAmber,
                        color = AccentRed
                    )
                }

                Button(
                    onClick = onConfirmAwake,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DismissGreen),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = "Yes, I'm up",
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
                        text = "Check again later"
                    )
                }

                Text(
                    text = "Choosing \"Check again later\" simply closes this screen and keeps the follow-up protection active.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
