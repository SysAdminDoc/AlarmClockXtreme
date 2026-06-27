package com.sysadmindoc.alarmclock.directboot

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.sysadmindoc.alarmclock.R

class DirectBootAlarmActivity : Activity() {
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenWindow()
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    private fun configureLockScreenWindow() {
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun render(intent: Intent) {
        alarmId = intent.getLongExtra(DirectBootAlarmReceiver.EXTRA_ALARM_ID, -1L)
        val timeLabel = intent.getStringExtra(DirectBootAlarmReceiver.EXTRA_TIME_LABEL).orEmpty()
        val title = when {
            timeLabel.isNotBlank() -> timeLabel
            else -> getString(R.string.direct_boot_alarm_title)
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
                setBackgroundColor(0xFF080B12.toInt())
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

                addView(
                    TextView(context).apply {
                        text = title
                        setTextColor(0xFFFFFFFF.toInt())
                        textSize = 44f
                        gravity = Gravity.CENTER
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                addView(
                    TextView(context).apply {
                        text = getString(R.string.direct_boot_alarm_message)
                        setTextColor(0xFFD4D8E5.toInt())
                        textSize = 18f
                        gravity = Gravity.CENTER
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 20
                    }
                )

                addView(
                    Button(context).apply {
                        text = getString(R.string.direct_boot_alarm_stop)
                        isAllCaps = false
                        setOnClickListener {
                            stopDirectBootAlarm()
                        }
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 48
                    }
                )
            }
        )
    }

    private fun stopDirectBootAlarm() {
        val stopIntent = Intent(this, DirectBootAlarmService::class.java).apply {
            action = DirectBootAlarmService.ACTION_STOP
            putExtra(DirectBootAlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        runCatching { startService(stopIntent) }
        finish()
    }

    companion object {
        fun intent(context: android.content.Context, alarmId: Long, timeLabel: String): Intent {
            return Intent(context, DirectBootAlarmActivity::class.java).apply {
                putExtra(DirectBootAlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(DirectBootAlarmReceiver.EXTRA_TIME_LABEL, timeLabel)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
        }

        fun fullScreenIntent(
            context: android.content.Context,
            alarmId: Long,
            timeLabel: String
        ) = android.app.PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            intent(context, alarmId, timeLabel),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }
}
