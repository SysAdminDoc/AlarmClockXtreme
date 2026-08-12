package com.sysadmindoc.alarmclock.domain

/** Safe user-configurable hold duration for the firing-screen dismiss button. */
object LongPressThreshold {
    const val DEFAULT_MILLIS = 1_500
    const val MIN_MILLIS = 500
    const val MAX_MILLIS = 5_000

    fun coerceMillis(value: Int): Int = value.coerceIn(MIN_MILLIS, MAX_MILLIS)
}
