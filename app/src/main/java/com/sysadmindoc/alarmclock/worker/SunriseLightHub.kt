package com.sysadmindoc.alarmclock.worker

import com.sysadmindoc.alarmclock.data.preferences.AppSettings

/** Light hubs the per-alarm sunrise switch can drive. */
enum class SunriseLightHub {
    HUE,
    DIRIGERA;

    companion object {
        /** Hubs with an address, a credential and at least one light picked. */
        fun configured(settings: AppSettings): List<SunriseLightHub> = buildList {
            if (settings.hueBridgeIp.isNotBlank() &&
                settings.hueApiKey.isNotBlank() &&
                settings.hueLightIds.isNotBlank()
            ) {
                add(HUE)
            }
            if (settings.dirigeraHubIp.isNotBlank() &&
                settings.dirigeraAccessToken.isNotBlank() &&
                settings.dirigeraLightIds.isNotBlank()
            ) {
                add(DIRIGERA)
            }
        }
    }
}
