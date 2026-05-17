package com.sysadmindoc.alarmclock.di

import com.sysadmindoc.alarmclock.data.health.HealthConnectSleepRepository
import com.sysadmindoc.alarmclock.data.health.PlayHealthConnectSleepRepository
import com.sysadmindoc.alarmclock.service.PlayYouTubeAudioDownloader
import com.sysadmindoc.alarmclock.service.PlayYouTubeDownloadInitializer
import com.sysadmindoc.alarmclock.service.YouTubeAudioDownloader
import com.sysadmindoc.alarmclock.service.YouTubeDownloadInitializer
import com.sysadmindoc.alarmclock.wear.PlayWearNextAlarmBridge
import com.sysadmindoc.alarmclock.wear.WearNextAlarmBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Play Store flavor — wires the real yt-dlp-backed YouTube downloader.
 * The f-droid flavor binds stubs that return "not available in this build".
 */
object PlayFlavorModule {
    const val FLAVOR = "play"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayFlavorBindings {
    @Binds
    @Singleton
    abstract fun bindDownloader(impl: PlayYouTubeAudioDownloader): YouTubeAudioDownloader

    @Binds
    @Singleton
    abstract fun bindInitializer(impl: PlayYouTubeDownloadInitializer): YouTubeDownloadInitializer

    @Binds
    @Singleton
    abstract fun bindWearNextAlarmBridge(impl: PlayWearNextAlarmBridge): WearNextAlarmBridge

    @Binds
    @Singleton
    abstract fun bindHealthConnectSleepRepository(
        impl: PlayHealthConnectSleepRepository
    ): HealthConnectSleepRepository
}
