package com.sysadmindoc.alarmclock.service

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi

/** Shared, deterministic timing and style policy for Android 16 countdown Live Updates. */
internal object LiveUpdateCountdown {
    const val PROGRESS_MAX = 1_000

    fun progress(startMillis: Long, endMillis: Long, nowMillis: Long): Int {
        if (endMillis <= startMillis) return PROGRESS_MAX
        val elapsed = (nowMillis - startMillis).coerceIn(0L, endMillis - startMillis)
        return ((elapsed * PROGRESS_MAX) / (endMillis - startMillis))
            .toInt()
            .coerceIn(0, PROGRESS_MAX)
    }

    fun elapsedEndToWallClock(
        endElapsedRealtime: Long,
        nowElapsedRealtime: Long,
        nowWallClockMillis: Long
    ): Long = nowWallClockMillis + (endElapsedRealtime - nowElapsedRealtime).coerceAtLeast(0L)

    @RequiresApi(36)
    fun progressStyle(
        context: Context,
        progress: Int,
        @DrawableRes trackerIcon: Int
    ): Notification.ProgressStyle = Notification.ProgressStyle()
        .setStyledByProgress(true)
        .setProgress(progress.coerceIn(0, PROGRESS_MAX))
        .setProgressSegments(listOf(Notification.ProgressStyle.Segment(PROGRESS_MAX)))
        .setProgressTrackerIcon(Icon.createWithResource(context, trackerIcon))
}
