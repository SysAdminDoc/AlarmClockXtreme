package com.sysadmindoc.alarmclock.service

import android.media.AudioAttributes
import android.media.AudioDeviceInfo

/**
 * Alarm playback must be classified as system alarm audio, not media audio.
 * Android 17's hearing-aid routing is user/system managed for alarms, so ACX
 * deliberately sets AudioAttributes.USAGE_ALARM and does not force a preferred
 * output device from app code.
 */
object AlarmAudioRouting {
    fun alarmMusicAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    fun alarmSonificationAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun isSystemManagedHearingAidLikeType(type: Int): Boolean = when (type) {
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_HEADSET -> true
        else -> false
    }
}
