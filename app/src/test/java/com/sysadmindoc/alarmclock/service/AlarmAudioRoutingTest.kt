package com.sysadmindoc.alarmclock.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.C
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AlarmAudioRoutingTest {

    private fun audioManager(): AudioManager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun attributes(usage: Int): AudioAttributes =
        AudioAttributes.Builder().setUsage(usage).build()

    /**
     * The fallback case: nothing new started after the alarm was handed to
     * Spotify, so the count does not rise and the watchdog rings a tone.
     */
    @Test
    fun otherAppPlaybackStreamsIgnoresOurOwnAlarm() {
        val manager = audioManager()
        shadowOf(manager).setActivePlaybackConfigurationsFor(
            listOf(attributes(AudioAttributes.USAGE_ALARM))
        )

        assertEquals(0, AlarmAudioRouting.otherAppPlaybackStreams(manager))
    }

    /**
     * The confirmed case. Counts by "not our alarm" rather than by USAGE_MEDIA:
     * Android anonymises another app's configuration for callers without
     * MODIFY_AUDIO_ROUTING and the usage does not reliably survive, so a
     * USAGE_MEDIA test can miss Spotify and drop a tone on top of it.
     */
    @Test
    fun otherAppPlaybackStreamsCountsUsagesThatSurviveAnonymisation() {
        val manager = audioManager()
        shadowOf(manager).setActivePlaybackConfigurationsFor(
            listOf(
                attributes(AudioAttributes.USAGE_MEDIA),
                attributes(AudioAttributes.USAGE_UNKNOWN),
                attributes(AudioAttributes.USAGE_ALARM)
            )
        )

        assertEquals(2, AlarmAudioRouting.otherAppPlaybackStreams(manager))
    }

    @Test
    fun otherAppPlaybackStreamsIsZeroWhenNothingIsPlaying() {
        val manager = audioManager()
        shadowOf(manager).setActivePlaybackConfigurationsFor(emptyList())

        assertEquals(0, AlarmAudioRouting.otherAppPlaybackStreams(manager))
    }

    @Test
    fun alarmMusicAttributesUseAlarmRouting() {
        val attributes = AlarmAudioRouting.alarmMusicAttributes()

        assertEquals(AudioAttributes.USAGE_ALARM, attributes.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_MUSIC, attributes.contentType)
    }

    @Test
    fun alarmSonificationAttributesUseAlarmRouting() {
        val attributes = AlarmAudioRouting.alarmSonificationAttributes()

        assertEquals(AudioAttributes.USAGE_ALARM, attributes.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_SONIFICATION, attributes.contentType)
    }

    @Test
    fun media3AlarmAttributesUseAlarmRouting() {
        val musicAttributes = AlarmAudioRouting.media3AlarmMusicAttributes()
        val sonificationAttributes = AlarmAudioRouting.media3AlarmSonificationAttributes()

        assertEquals(C.USAGE_ALARM, musicAttributes.usage)
        assertEquals(C.AUDIO_CONTENT_TYPE_MUSIC, musicAttributes.contentType)
        assertEquals(C.USAGE_ALARM, sonificationAttributes.usage)
        assertEquals(C.AUDIO_CONTENT_TYPE_SONIFICATION, sonificationAttributes.contentType)
    }

    @Test
    fun hearingAidLikeOutputTypesAreSystemManaged() {
        assertTrue(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_HEARING_AID))
        assertTrue(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_BLE_HEADSET))
        assertFalse(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertFalse(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
    }

    @Test
    fun forcesSpeakerOnlyWhenOptedInAndNoHearingAid() {
        val withHeadset = listOf(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        )
        // Opted in with only a normal headset -> force the built-in speaker.
        assertTrue(AlarmAudioRouting.shouldForceBuiltInSpeaker(usePhoneSpeakers = true, outputDeviceTypes = withHeadset))
        // Bluetooth A2DP is still a normal (non-accessibility) sink -> force.
        assertTrue(
            AlarmAudioRouting.shouldForceBuiltInSpeaker(
                usePhoneSpeakers = true,
                outputDeviceTypes = listOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            )
        )
    }

    @Test
    fun doesNotForceSpeakerWhenOptedOut() {
        assertFalse(
            AlarmAudioRouting.shouldForceBuiltInSpeaker(
                usePhoneSpeakers = false,
                outputDeviceTypes = listOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            )
        )
    }

    @Test
    fun doesNotOverrideSystemManagedHearingDevices() {
        // Even opted-in, never fight the system's hearing-aid / BLE routing.
        assertFalse(
            AlarmAudioRouting.shouldForceBuiltInSpeaker(
                usePhoneSpeakers = true,
                outputDeviceTypes = listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_HEARING_AID)
            )
        )
        assertFalse(
            AlarmAudioRouting.shouldForceBuiltInSpeaker(
                usePhoneSpeakers = true,
                outputDeviceTypes = listOf(AudioDeviceInfo.TYPE_BLE_HEADSET)
            )
        )
    }
}
