package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.provider.Settings

/** Shared policy for decorative motion and optional flashing effects. */
object MotionPolicy {
    fun allowsMotion(reduceMotionAndFlashing: Boolean, animatorDurationScale: Float): Boolean =
        !reduceMotionAndFlashing && animatorDurationScale > 0f

    fun animatorDurationScale(context: Context): Float = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }.getOrDefault(1f)

    fun allowsMotion(context: Context, reduceMotionAndFlashing: Boolean): Boolean =
        allowsMotion(reduceMotionAndFlashing, animatorDurationScale(context))
}
