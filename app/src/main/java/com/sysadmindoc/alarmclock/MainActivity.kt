package com.sysadmindoc.alarmclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.ui.components.WhatsNewDialog
import com.sysadmindoc.alarmclock.ui.navigation.AppNavigation
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import com.sysadmindoc.alarmclock.util.WhatsNewTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // v1.5.0: Decide once at launch whether to surface the What's-new
        // dialog; avoid re-checking during recomposition.
        val showWhatsNew = WhatsNewTracker.shouldShow(this, BuildConfig.VERSION_CODE)

        setContent {
            val settings = preferencesManager.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            AlarmClockXtremeTheme(
                accentColorHex = settings.value.accentColor,
                dynamicColor = settings.value.dynamicColorEnabled
            ) {
                AppNavigation()

                var dialogVisible by remember { mutableStateOf(showWhatsNew) }
                if (dialogVisible) {
                    WhatsNewDialog(
                        version = BuildConfig.VERSION_NAME,
                        highlights = WHATS_NEW_HIGHLIGHTS,
                        onDismiss = {
                            dialogVisible = false
                            WhatsNewTracker.markShown(this@MainActivity, BuildConfig.VERSION_CODE)
                        }
                    )
                }
            }
        }
    }

    companion object {
        /**
         * v1.5.0: Terse highlights for the "What's new" dialog — a
         * ~half-dozen bullets maximum, written for users (not devs).
         * Full release notes live in CHANGELOG.md.
         */
        private val WHATS_NEW_HIGHLIGHTS = listOf(
            "Three new wake-up challenges: Simon Says, type today's date backwards, and the Stroop color test.",
            "Alarms can now fire relative to sunrise or sunset with a configurable offset.",
            "Alarm-edit screen exposes the v1.4.0 settings: hardware-button action, dismiss-at-ringtone-end, and a random ringtone pool.",
            "Bedtime tab has a seconds-scale slider for how long the sleep-sound fade-out should take.",
            "Power-nap quick-alarm chips now highlight your default nap length."
        )
    }
}
