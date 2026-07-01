package com.sysadmindoc.alarmclock.service

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import androidx.media3.common.C
import com.sysadmindoc.alarmclock.BuildConfig
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
    fun playbackBackendFollowsBuildFlag() {
        assertEquals(AlarmPlaybackBackend.MEDIA3, AlarmPlaybackBackend.fromBuildFlag(true))
        assertEquals(AlarmPlaybackBackend.MEDIA_PLAYER, AlarmPlaybackBackend.fromBuildFlag(false))
    }

    @Test
    fun media3PlaybackGateIsEnabledForCurrentRelease() {
        assertTrue(BuildConfig.USE_MEDIA3_ALARM_PLAYER)
    }

    @Test
    fun hearingAidLikeOutputTypesAreSystemManaged() {
        assertTrue(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_HEARING_AID))
        assertTrue(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_BLE_HEADSET))
        assertFalse(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertFalse(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
    }
}
