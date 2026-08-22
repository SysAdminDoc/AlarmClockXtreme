package com.sysadmindoc.alarmclock.service

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AlarmAudioRoutingTest {

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
