package com.sysadmindoc.alarmclock.service

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
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
    fun hearingAidLikeOutputTypesAreSystemManaged() {
        assertTrue(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_HEARING_AID))
        assertTrue(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_BLE_HEADSET))
        assertFalse(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertFalse(AlarmAudioRouting.isSystemManagedHearingAidLikeType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
    }
}
