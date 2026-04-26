package com.sysadmindoc.alarmclock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.data.share.AlarmShareCodec
import com.sysadmindoc.alarmclock.ui.components.WhatsNewDialog
import com.sysadmindoc.alarmclock.ui.navigation.AppNavigation
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import com.sysadmindoc.alarmclock.util.WhatsNewTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var alarmRepository: AlarmRepository

    private var lastHandledShareToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lastHandledShareToken = savedInstanceState?.getString(KEY_LAST_HANDLED_SHARE_TOKEN)
        handleSharedAlarmIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedAlarmIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_LAST_HANDLED_SHARE_TOKEN, lastHandledShareToken)
        super.onSaveInstanceState(outState)
    }

    private fun handleSharedAlarmIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != AlarmShareCodec.SCHEME || uri.host != AlarmShareCodec.HOST) return

        val token = uri.getQueryParameter(AlarmShareCodec.DATA_PARAM).orEmpty()
        if (token.isBlank() || token == lastHandledShareToken) return
        lastHandledShareToken = token

        lifecycleScope.launch {
            val decoded = AlarmShareCodec.decodeToken(token)
            decoded.fold(
                onSuccess = { alarm ->
                    val imported = AlarmShareCodec.prepareImportedAlarm(alarm)
                    alarmRepository.save(imported)
                    Toast.makeText(
                        this@MainActivity,
                        "Imported shared alarm. Review it before enabling.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onFailure = {
                    Toast.makeText(
                        this@MainActivity,
                        "Unable to import this shared alarm.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    companion object {
        private const val KEY_LAST_HANDLED_SHARE_TOKEN = "last_handled_share_token"

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
