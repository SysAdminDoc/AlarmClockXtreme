package com.sysadmindoc.alarmclock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.data.share.AlarmShareCodec
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService
import com.sysadmindoc.alarmclock.ui.alarmfiring.AlarmFiringActivity
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

    private var lastHandledShareToken: String? = null
    private var pendingSharedAlarmToken: String? = null
    private var pendingSharedAlarmDraft by mutableStateOf<Alarm?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lastHandledShareToken = savedInstanceState?.getString(KEY_LAST_HANDLED_SHARE_TOKEN)
        pendingSharedAlarmToken = savedInstanceState?.getString(KEY_PENDING_SHARE_TOKEN)
        pendingSharedAlarmToken?.let { restorePendingSharedAlarm(it) } ?: handleSharedAlarmIntent(intent)

        // v1.5.0: Decide once at launch whether to surface the What's-new
        // dialog; avoid re-checking during recomposition.
        val showWhatsNew = WhatsNewTracker.shouldShow(this, BuildConfig.VERSION_CODE)

        setContent {
            val settings = preferencesManager.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            AlarmClockXtremeTheme(
                accentColorHex = settings.value.accentColor,
                dynamicColor = settings.value.dynamicColorEnabled,
                expressiveMode = settings.value.expressiveModeEnabled
            ) {
                AppNavigation(
                    sharedAlarmDraft = pendingSharedAlarmDraft,
                    onSharedAlarmConsumed = {
                        pendingSharedAlarmDraft = null
                        pendingSharedAlarmToken = null
                    }
                )

                var dialogVisible by remember { mutableStateOf(showWhatsNew) }
                if (dialogVisible) {
                    WhatsNewDialog(
                        version = BuildConfig.VERSION_NAME,
                        highlights = WHATS_NEW_HIGHLIGHTS,
                        onOpenRoadmap = {
                            dialogVisible = false
                            WhatsNewTracker.markShown(this@MainActivity, BuildConfig.VERSION_CODE)
                            openRoadmap()
                        },
                        onDismiss = {
                            dialogVisible = false
                            WhatsNewTracker.markShown(this@MainActivity, BuildConfig.VERSION_CODE)
                        }
                    )
                }
            }
        }
    }

    private fun openRoadmap() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ROADMAP_URL))
        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(
                    this,
                    "Unable to open the roadmap link.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onResume() {
        super.onResume()
        val snapshot = AlarmService.activeAlarm.get() ?: return
        val intent = Intent(this, AlarmFiringActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, snapshot.alarmId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, snapshot.scheduledAt)
            putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, snapshot.fireId)
        }
        startActivity(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedAlarmIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_LAST_HANDLED_SHARE_TOKEN, lastHandledShareToken)
        outState.putString(KEY_PENDING_SHARE_TOKEN, pendingSharedAlarmToken)
        super.onSaveInstanceState(outState)
    }

    private fun handleSharedAlarmIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != AlarmShareCodec.SCHEME || uri.host != AlarmShareCodec.HOST) return

        val token = uri.getQueryParameter(AlarmShareCodec.DATA_PARAM).orEmpty()
        if (token.isBlank() || token == lastHandledShareToken) return
        lastHandledShareToken = token

        queueSharedAlarmDraft(token, showReadyToast = true)
    }

    private fun restorePendingSharedAlarm(token: String) {
        queueSharedAlarmDraft(token, showReadyToast = false)
    }

    private fun queueSharedAlarmDraft(token: String, showReadyToast: Boolean) {
        val decoded = AlarmShareCodec.decodeToken(token)
        decoded.fold(
            onSuccess = { alarm ->
                pendingSharedAlarmToken = token
                pendingSharedAlarmDraft = AlarmShareCodec.prepareImportedAlarm(alarm)
                if (showReadyToast) {
                    Toast.makeText(
                        this,
                        "Review this shared alarm before saving it.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onFailure = {
                pendingSharedAlarmToken = null
                pendingSharedAlarmDraft = null
                Toast.makeText(
                    this,
                    "Unable to import this shared alarm.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    companion object {
        private const val KEY_LAST_HANDLED_SHARE_TOKEN = "last_handled_share_token"
        private const val KEY_PENDING_SHARE_TOKEN = "pending_share_token"
        private const val ROADMAP_URL = "https://github.com/SysAdminDoc/AlarmClockXtreme#roadmap"

        /**
         * Terse highlights for the "What's new" dialog — four concise
         * bullets max, written for users (not devs). Full release notes
         * live in CHANGELOG.md. Refresh on every shipping release so a
         * returning user sees the actual changes since they last opened
         * the app, not stale text from two versions ago.
         */
        private val WHATS_NEW_HIGHLIGHTS = listOf(
            "YouTube downloader errors now use plain recovery copy instead of raw exception text.",
            "Search, preview, update, and download failures now point to the right recovery action.",
            "YouTube search now responds to the keyboard Search action.",
            "Paste-mode sound naming now closes the keyboard cleanly from Done."
        )
    }
}
