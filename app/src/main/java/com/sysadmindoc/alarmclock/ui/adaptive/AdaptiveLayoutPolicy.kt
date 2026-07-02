package com.sysadmindoc.alarmclock.ui.adaptive

internal const val ADAPTIVE_TWO_PANE_MIN_WIDTH_DP = 840f

internal fun shouldUseTwoPaneLayout(availableWidthDp: Float): Boolean {
    return availableWidthDp >= ADAPTIVE_TWO_PANE_MIN_WIDTH_DP
}
