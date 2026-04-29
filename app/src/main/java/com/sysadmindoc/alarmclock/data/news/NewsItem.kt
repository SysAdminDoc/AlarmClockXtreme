package com.sysadmindoc.alarmclock.data.news

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
data class NewsItem(
    val id: String,
    val title: String,
    val link: String,
    val description: String,
    val source: String,
    val publishedAtMillis: Long?,
)
