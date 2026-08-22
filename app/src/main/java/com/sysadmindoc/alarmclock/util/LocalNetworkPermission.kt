package com.sysadmindoc.alarmclock.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Android 17 adds ACCESS_LOCAL_NETWORK as a dangerous runtime permission.
 *
 * The app still compiles against SDK 36, so use the literal permission string
 * until compileSdk moves to API 37.
 */
object LocalNetworkPermission {
    const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    const val ANDROID_17_API_LEVEL = 37

    fun isRuntimeRequired(): Boolean = Build.VERSION.SDK_INT >= ANDROID_17_API_LEVEL

    fun isGranted(context: Context): Boolean {
        if (!isRuntimeRequired()) return true
        return ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun requiresPermissionForUrl(url: String): Boolean =
        isRuntimeRequired() && isLikelyLocalEndpoint(url)

    fun isLikelyLocalEndpoint(url: String): Boolean {
        val host = url.trim().toHttpUrlOrNull()?.host ?: return false
        return isLikelyLocalHost(host)
    }

    /**
     * Suffixes a router hands out for machines that exist only on the LAN.
     * `.local` is mDNS (RFC 6762) and `.home.arpa` is the one the IETF actually
     * reserved for this (RFC 8375); the rest are what consumer routers ship as
     * defaults, so a Hue bridge is routinely reachable as `hue.lan` or
     * `hue.fritz.box` and nothing else.
     */
    private val LOCAL_SUFFIXES = listOf(
        ".local", ".localdomain", ".lan", ".home", ".home.arpa", ".internal",
        ".intranet", ".private", ".fritz.box"
    )

    /**
     * True when [rawHost] can only resolve on the local network: a private or
     * link-local IPv4 literal, a ULA/link-local IPv6 literal, localhost, a
     * single-label name, or a name under one of [LOCAL_SUFFIXES]. Integrations
     * that are supposed to talk to hardware in the house use this to refuse a
     * public hostname outright.
     */
    fun isLikelyLocalHost(rawHost: String): Boolean {
        val host = rawHost.trim().trim('[', ']').lowercase()
        if (host.isBlank()) return false
        if (host == "localhost" || host == "::1") return true
        if (LOCAL_SUFFIXES.any { host.endsWith(it) }) return true
        if (!host.contains('.') && !host.contains(':')) return true
        if (host.contains(':') && (
                host.startsWith("fe80:") ||
                    host.startsWith("fc") ||
                    host.startsWith("fd")
            )
        ) {
            return true
        }

        val octets = host.split('.').mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        return when (octets[0]) {
            10 -> true
            127 -> true
            169 -> octets[1] == 254
            172 -> octets[1] in 16..31
            192 -> octets[1] == 168
            else -> false
        }
    }
}
