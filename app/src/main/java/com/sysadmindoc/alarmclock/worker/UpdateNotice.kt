package com.sysadmindoc.alarmclock.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.domain.UpdateCheckPolicy

/** Low-key "a newer release is out" notice that opens the release page. Nothing is downloaded. */
object UpdateNotice {
    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 90_310

    /** False when nothing reached the shade, so the caller can try again on the next check. */
    fun post(context: Context, tag: String): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false

        return runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return false
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_app_updates),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            // Android drops a notice for a blocked app or channel without an
            // error, so it must not count as told.
            if (
                !NotificationManagerCompat.from(context).areNotificationsEnabled() ||
                manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
            ) return false
            val openRelease = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                Intent(Intent.ACTION_VIEW, Uri.parse(UpdateCheckPolicy.releasePageUrl(tag)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_alarm)
                    .setContentTitle(
                        context.getString(R.string.notif_update_title, UpdateCheckPolicy.displayVersion(tag))
                    )
                    .setContentText(context.getString(R.string.notif_update_text))
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                    .setContentIntent(openRelease)
                    .setAutoCancel(true)
                    .build()
            )
        }.isSuccess
    }
}
