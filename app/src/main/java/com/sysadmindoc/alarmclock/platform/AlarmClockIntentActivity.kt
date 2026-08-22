package com.sysadmindoc.alarmclock.platform

import android.util.Log
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

    /**
     * The app that sent the intent, only when the system vouches for it.
     *
     * Deliberately not `referrer`: that returns whatever the caller put in
     * EXTRA_REFERRER, so a notice meant to say who added an alarm could be
     * made to name anyone. `callingPackage` is null for a plain
     * startActivity, and an unnamed notice is better than a wrong one.
     */
    private fun callerLabel(): String? = callingPackage

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
                // Reading extras unparcels whatever the caller sent. A
                // malformed bundle throws in getStringExtra and used to
                // take the proxy activity down with it.
                val result = withContext(Dispatchers.IO) {
                    runCatching { handler.handle(request) }
                        .getOrElse { error ->
                            Log.w(TAG, "Rejected a malformed AlarmClock intent", error)
                            AlarmClockHandleResult.Invalid
                        }
                }
                val handled = result as? AlarmClockHandleResult.Handled
                val createdId = handled?.createdAlarmId
                if (handled?.createdSilently == true && createdId != null) {
                    ExternalAlarmNotice.post(
                        this@AlarmClockIntentActivity,
                        callerLabel(),
                        createdId
                    )
                }
                val route = handled?.route
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

private const val TAG = "AlarmClockIntent"
