package com.sysadmindoc.alarmclock.platform

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.sysadmindoc.alarmclock.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Permission-protected boundary for the platform android.provider.AlarmClock contract. */
@AndroidEntryPoint
class AlarmClockIntentActivity : ComponentActivity() {
    @Inject lateinit var handler: AlarmClockIntentHandler

    private var inFlightRequests = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRequest(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRequest(intent)
    }

    private fun handleRequest(request: Intent) {
        inFlightRequests += 1
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { handler.handle(request) }
                val route = (result as? AlarmClockHandleResult.Handled)?.route
                if (route != null) {
                    startActivity(
                        Intent(this@AlarmClockIntentActivity, MainActivity::class.java)
                            .setData(route.toUri())
                    )
                }
            } finally {
                inFlightRequests -= 1
                if (inFlightRequests == 0) finish()
            }
        }
    }
}
