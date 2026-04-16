package com.sysadmindoc.alarmclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sysadmindoc.alarmclock.data.preferences.AppSettings
import com.sysadmindoc.alarmclock.data.preferences.PreferencesManager
import com.sysadmindoc.alarmclock.ui.navigation.AppNavigation
import com.sysadmindoc.alarmclock.ui.theme.AlarmClockXtremeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val settings = preferencesManager.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            AlarmClockXtremeTheme(accentColorHex = settings.value.accentColor) {
                AppNavigation()
            }
        }
    }
}
