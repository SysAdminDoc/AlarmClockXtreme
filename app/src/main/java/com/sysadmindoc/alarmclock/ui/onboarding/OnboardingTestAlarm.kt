package com.sysadmindoc.alarmclock.ui.onboarding

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.service.AlarmAudioRouting
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.data.readiness.TestAlarmProof
import com.sysadmindoc.alarmclock.data.readiness.TestAlarmProofStore

object OnboardingTestAlarm {
    const val ACTION_RING = "com.sysadmindoc.alarmclock.ONBOARDING_TEST_ALARM"
    const val EXTRA_TRIGGER_AT = "trigger_at"
    const val NOTIFICATION_ID = 1907
    private const val REQUEST_CODE = 1907
    private const val DELAY_MS = 10_000L

    fun isCompleted(context: Context): Boolean =
        TestAlarmProofStore.isCompleted(context)

    fun lastProof(context: Context): TestAlarmProof =
        TestAlarmProofStore.lastProof(context)

    fun markCompleted(context: Context) {
        TestAlarmProofStore.markCompleted(context)
    }

    internal fun recordFired(
        context: Context,
        scheduledAt: Long,
        firedAt: Long,
        notificationPermissionGranted: Boolean,
        fullScreenIntentRequested: Boolean,
        activityLaunchSucceeded: Boolean
    ) {
        TestAlarmProofStore.recordFired(
            context = context,
            scheduledAt = scheduledAt,
            firedAt = firedAt,
            notificationPermissionGranted = notificationPermissionGranted,
            fullScreenIntentRequested = fullScreenIntentRequested,
            activityLaunchSucceeded = activityLaunchSucceeded
        )
    }

    fun schedule(context: Context): Result<Unit> = runCatching {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
            ?: throw IllegalStateException("Alarm scheduling is unavailable on this device.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            throw IllegalStateException("Review exact alarm access before running the test alarm.")
        }

        val triggerAt = System.currentTimeMillis() + DELAY_MS
        val intent = Intent(context, OnboardingTestAlarmReceiver::class.java).apply {
            action = ACTION_RING
            putExtra(EXTRA_TRIGGER_AT, triggerAt)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        TestAlarmProofStore.recordScheduled(context, triggerAt)
    }
}

class OnboardingTestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != OnboardingTestAlarm.ACTION_RING) return
        AlarmService.createNotificationChannels(context)
        val triggerAt = intent.getLongExtra(OnboardingTestAlarm.EXTRA_TRIGGER_AT, System.currentTimeMillis())
        val activityIntent = Intent(context, OnboardingTestAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(OnboardingTestAlarm.EXTRA_TRIGGER_AT, triggerAt)
        }
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            OnboardingTestAlarm.NOTIFICATION_ID,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, AlarmService.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Test alarm")
            .setContentText("ACX can wake this device. Dismiss the test to finish setup.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(activityPendingIntent, true)
            .setContentIntent(activityPendingIntent)
            .setAutoCancel(true)
            .build()

        val hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (hasNotificationPermission) {
            context.getSystemService(NotificationManager::class.java)
                ?.notify(OnboardingTestAlarm.NOTIFICATION_ID, notification)
        }
        val activityLaunchSucceeded = runCatching { context.startActivity(activityIntent) }.isSuccess
        OnboardingTestAlarm.recordFired(
            context = context,
            scheduledAt = triggerAt,
            firedAt = System.currentTimeMillis(),
            notificationPermissionGranted = hasNotificationPermission,
            fullScreenIntentRequested = true,
            activityLaunchSucceeded = activityLaunchSucceeded
        )
    }
}

class OnboardingTestAlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startTestSignal()
        setContent {
            AlarmClockXtremeTheme {
                TestAlarmContent(onDismiss = { completeAndFinish() })
            }
        }
    }

    override fun onDestroy() {
        stopTestSignal()
        super.onDestroy()
    }

    private fun startTestSignal() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            audioAttributes = AlarmAudioRouting.alarmSonificationAttributes()
            play()
        }
        vibrateForTest()
    }

    private fun vibrateForTest() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        val attributes = AlarmAudioRouting.alarmSonificationAttributes()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1_200L, VibrationEffect.DEFAULT_AMPLITUDE), attributes)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1_200L)
        }
    }

    private fun completeAndFinish() {
        OnboardingTestAlarm.markCompleted(this)
        getSystemService(NotificationManager::class.java)?.cancel(OnboardingTestAlarm.NOTIFICATION_ID)
        stopTestSignal()
        finish()
    }

    private fun stopTestSignal() {
        runCatching { ringtone?.stop() }
        ringtone = null
    }
}

@Composable
private fun TestAlarmContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        SurfaceDark
                    )
                )
            )
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Text(
            text = "Test alarm",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your device can launch the alarm screen, play audio, and vibrate. This did not create or change any saved alarms.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
        )
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DismissGreen)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Text(
                text = "Dismiss test alarm",
                modifier = Modifier.padding(start = 10.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "If you did not hear or feel this test, review volume, Do Not Disturb, and battery settings before relying on an overnight alarm.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp)
        )
    }
}
