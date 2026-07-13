package com.sysadmindoc.alarmclock.data.news

import com.squareup.moshi.JsonClass

/**
 * One parsed RSS/Atom item, normalized down to what the News tab actually
 * renders. We deliberately keep the model thin — anything fancier (categories,
 * authors, enclosures, full HTML body) can be added later without changing
 * the parser contract.
 *
 * Fields:
 *  - [id] stable hash (link or guid). Used as the LazyColumn key.
 *  - [publishedAtMillis] epoch ms or null if the source omitted pubDate. Used
 *    for sorting and the relative-time chip ("3h ago").
 */
@JsonClass(generateAdapter = true)
data class NewsItem(
    val id: String,
    val title: String,
    val link: String,
    val description: String,
    val source: String,
    val publishedAtMillis: Long?,
) {
    /** Only open feed links that are plain web URLs — see [isSafeNewsLink]. */
    val hasOpenableLink: Boolean get() = isSafeNewsLink(link)
}

/**
 * The news feed URL is user-configurable and its item links come verbatim from
 * untrusted RSS/Atom. Restrict what we hand to `ACTION_VIEW` to `http`/`https`
 * so a hostile or compromised feed can't launch `intent:`, `javascript:`,
 * `file:`, or an arbitrary exported deep link when the user taps an article.
 */
fun isSafeNewsLink(url: String): Boolean {
    val trimmed = url.trim()
    // Guard against "  javascript:…" and mixed-case schemes.
    val lower = trimmed.lowercase()
    return (lower.startsWith("http://") || lower.startsWith("https://")) &&
        // Reject embedded control/whitespace that could smuggle a second scheme.
        trimmed.none { it.isWhitespace() || it.isISOControl() }
}
