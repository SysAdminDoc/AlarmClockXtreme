package com.sysadmindoc.alarmclock.ui.alarmfiring

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sysadmindoc.alarmclock.ui.theme.*

/**
 * F5: Wake confirmation activity.
 * Launched from the wake-confirmation notification. Tapping "I'm Awake" marks
 * the alarm as confirmed so WakeConfirmWorker won't re-fire it.
 */
class WakeConfirmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
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

        setContent {
            AlarmClockXtremeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DismissGreen,
                            modifier = Modifier.size(72.dp)
                        )

                        Text(
                            "Are you awake?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            "Tap below to confirm you're up. If you don't respond, the alarm will re-fire.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )

                        Button(
                            onClick = {
                                if (alarmId != -1L) {
                                    val prefs = getSharedPreferences("wake_confirm", Context.MODE_PRIVATE)
                                    prefs.edit().putBoolean("confirmed_$alarmId", true).apply()
                                }
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DismissGreen),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text("I'm Awake!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
