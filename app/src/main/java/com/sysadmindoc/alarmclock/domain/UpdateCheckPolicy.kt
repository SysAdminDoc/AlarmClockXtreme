package com.sysadmindoc.alarmclock.domain

/**
 * Decides whether a GitHub release tag is newer than the running build.
 *
 * Release tags are `v` plus the versionName (`v1.15.35`), so the comparison
 * is numeric part by part. Anything that doesn't parse counts as "no update":
 * a bad tag must never nag.
 */
object UpdateCheckPolicy {
    private const val RELEASES_URL = "https://github.com/SysAdminDoc/AlarmClockXtreme/releases/tag/"
    private val VERSION = Regex("""^v?(\d{1,6}(?:\.\d{1,6}){0,3})$""")

    fun parse(version: String?): List<Int>? {
        val match = VERSION.matchEntire(version?.trim().orEmpty()) ?: return null
        return match.groupValues[1].split('.').map { it.toInt() }
    }

    fun isNewer(latestTag: String?, currentVersionName: String): Boolean {
        val latest = parse(latestTag) ?: return false
        val current = parse(currentVersionName) ?: return false
        val width = maxOf(latest.size, current.size)
        for (i in 0 until width) {
            val a = latest.getOrElse(i) { 0 }
            val b = current.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** "1.16.0" for the notification, from a tag [parse] already accepted. */
    fun displayVersion(tag: String): String = tag.trim().removePrefix("v")

    /**
     * The tag exactly as published, since GitHub's page path is the tag name.
     * Only a tag that parses as a version is used, so the notification can
     * only ever open a page under this repo's releases.
     */
    fun releasePageUrl(tag: String): String =
        RELEASES_URL + (tag.trim().takeIf { parse(it) != null } ?: "")
}
