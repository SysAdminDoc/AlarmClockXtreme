package com.sysadmindoc.alarmclock.service

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sysadmindoc.alarmclock.AlarmClockApp
import com.sysadmindoc.alarmclock.BuildConfig
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.model.Alarm
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class AlarmServiceMedia3SmokeTest {

    @Test
    fun alarmServiceStartsWithMedia3PlaybackBackend() = runBlocking {
        assumeTrue(BuildConfig.USE_MEDIA3_ALARM_PLAYER)

        val context = ApplicationProvider.getApplicationContext<Context>()
        grantNotificationPermission(context)

        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            AlarmClockApp.AppEntryPoint::class.java
        )
        val alarmRepository = entryPoint.alarmRepository()
        val incidentRepository = entryPoint.alarmIncidentRepository()
        incidentRepository.clearHistory()

        val scheduledAt = System.currentTimeMillis()
        val fireId = "media3-instrumented-$scheduledAt"
        val media3Tone = writeSmokeTone(context)
        val alarmId = alarmRepository.save(
            Alarm(
                hour = 7,
                minute = 30,
                label = "Media3 instrumented smoke",
                isEnabled = true,
                ringtoneUri = media3Tone.toURI().toString(),
                vibrationEnabled = false,
                overrideSystemVolume = false,
                gradualVolumeSeconds = 0,
                challengeType = "NONE"
            )
        )

        try {
            ContextCompat.startForegroundService(
                context,
                AlarmFireDismissContract.startServiceIntent(context, alarmId, scheduledAt, fireId)
            )

            val events = waitForMedia3PlaybackIncident(fireId) {
                incidentRepository.getRecent(50)
            }.filter { it.fireId == fireId || it.alarmId == alarmId || it.reasonCode.startsWith("MEDIA3_") }

            assertTrue(
                events.joinToString(prefix = "Media3 incidents: ") { it.reasonCode },
                events.any { it.reasonCode == "MEDIA3_PLAYER_STARTED" }
            )
        } finally {
            runCatching {
                context.startService(
                    AlarmFireDismissContract.dismissServiceIntent(context, alarmId, scheduledAt, fireId)
                )
            }
            media3Tone.delete()
            alarmRepository.deleteById(alarmId)
        }
    }

    private fun grantNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private suspend fun waitForMedia3PlaybackIncident(
        fireId: String,
        recentEvents: suspend () -> List<AlarmIncidentEvent>
    ): List<AlarmIncidentEvent> {
        repeat(50) {
            val events = recentEvents()
            val scopedEvents = events.filter { it.fireId == fireId || it.reasonCode.startsWith("MEDIA3_") }
            if (events.any { it.reasonCode == "MEDIA3_PLAYER_STARTED" } ||
                events.any { it.reasonCode == "MEDIA3_DEFAULT_FALLBACK_TO_LEGACY" } ||
                events.any { it.reasonCode.startsWith("MEDIA3_PLAYER_FAILED_") } ||
                events.any { it.reasonCode.startsWith("MEDIA3_DEFAULT_FALLBACK_FAILED_") }
            ) {
                return scopedEvents.ifEmpty { events }
            }
            delay(200)
        }
        return recentEvents()
    }

    private fun writeSmokeTone(context: Context): File {
        val file = File(context.filesDir, "media3_smoke.wav")
        val sampleRate = 8_000
        val samples = sampleRate / 2
        val dataSize = samples * 2

        file.outputStream().use { out ->
            fun ascii(value: String) = out.write(value.toByteArray(Charsets.US_ASCII))
            fun intLe(value: Int) {
                out.write(value and 0xff)
                out.write((value ushr 8) and 0xff)
                out.write((value ushr 16) and 0xff)
                out.write((value ushr 24) and 0xff)
            }
            fun shortLe(value: Int) {
                out.write(value and 0xff)
                out.write((value ushr 8) and 0xff)
            }

            ascii("RIFF")
            intLe(36 + dataSize)
            ascii("WAVE")
            ascii("fmt ")
            intLe(16)
            shortLe(1)
            shortLe(1)
            intLe(sampleRate)
            intLe(sampleRate * 2)
            shortLe(2)
            shortLe(16)
            ascii("data")
            intLe(dataSize)

            for (i in 0 until samples) {
                val angle = 2.0 * PI * 440.0 * i / sampleRate
                val sample = (sin(angle) * Short.MAX_VALUE * 0.2).toInt()
                shortLe(sample)
            }
        }

        return file
    }
}
