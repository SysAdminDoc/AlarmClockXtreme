package com.sysadmindoc.alarmclock.service

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes as Media3AudioAttributes

/**
 * Alarm playback must be classified as system alarm audio, not media audio.
 * Android 17's hearing-aid routing is user/system managed for alarms, so ACX
 * deliberately sets AudioAttributes.USAGE_ALARM and, by default, does not force
 * a preferred output device from app code. The one exception is the explicit
 * "use phone speakers" opt-in, which forces the built-in speaker so a connected
 * wired/BT headset can't silently swallow the alarm — see [shouldForceBuiltInSpeaker].
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

    fun media3AlarmMusicAttributes(): Media3AudioAttributes = Media3AudioAttributes.Builder()
        .setUsage(C.USAGE_ALARM)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    fun media3AlarmSonificationAttributes(): Media3AudioAttributes = Media3AudioAttributes.Builder()
        .setUsage(C.USAGE_ALARM)
        .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
        .build()

    fun isSystemManagedHearingAidLikeType(type: Int): Boolean = when (type) {
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_HEADSET -> true
        else -> false
    }

    /**
     * When the user has opted into "use phone speakers", force alarm audio to
     * the built-in speaker so a connected wired/BT headset can't swallow the
     * alarm and leave a heavy sleeper in silence. Deliberately does NOT override
     * system-managed hearing-aid / BLE routing (accessibility), matching the
     * default no-force philosophy above for those device classes.
     */
    fun shouldForceBuiltInSpeaker(usePhoneSpeakers: Boolean, outputDeviceTypes: List<Int>): Boolean =
        usePhoneSpeakers && outputDeviceTypes.none { isSystemManagedHearingAidLikeType(it) }

    fun builtInSpeaker(audioManager: AudioManager): AudioDeviceInfo? =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
}
