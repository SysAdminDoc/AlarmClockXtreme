package com.sysadmindoc.alarmclock.di

import com.sysadmindoc.alarmclock.service.FdroidYouTubeAudioDownloader
import com.sysadmindoc.alarmclock.service.FdroidYouTubeDownloadInitializer
import com.sysadmindoc.alarmclock.service.YouTubeAudioDownloader
import com.sysadmindoc.alarmclock.service.YouTubeDownloadInitializer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * F-Droid flavor - no proprietary dependencies allowed. The yt-dlp downloader
 * is stubbed out; the UI checks `isAvailable()` and hides the entry point.
 */
object FdroidFlavorModule {
    const val FLAVOR = "fdroid"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FdroidFlavorBindings {
    @Binds
    @Singleton
    abstract fun bindDownloader(impl: FdroidYouTubeAudioDownloader): YouTubeAudioDownloader

    @Binds
    @Singleton
    abstract fun bindInitializer(impl: FdroidYouTubeDownloadInitializer): YouTubeDownloadInitializer
}
